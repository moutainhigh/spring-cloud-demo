package com.iwhalecloud.retail.goods2b.service.impl.dubbo;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultCodeEnum;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.exception.RetailTipException;
import com.iwhalecloud.retail.goods2b.common.GoodsConst;
import com.iwhalecloud.retail.goods2b.common.ProductConst;
import com.iwhalecloud.retail.goods2b.dto.GoodsSaleNumDTO;
import com.iwhalecloud.retail.goods2b.dto.ProductDTO;
import com.iwhalecloud.retail.goods2b.dto.req.*;
import com.iwhalecloud.retail.goods2b.dto.resp.*;
import com.iwhalecloud.retail.goods2b.entity.Brand;
import com.iwhalecloud.retail.goods2b.entity.ProductBase;
import com.iwhalecloud.retail.goods2b.manager.BrandManager;
import com.iwhalecloud.retail.goods2b.manager.ProdFileManager;
import com.iwhalecloud.retail.goods2b.manager.ProductBaseManager;
import com.iwhalecloud.retail.goods2b.manager.TagRelManager;
import com.iwhalecloud.retail.goods2b.service.dubbo.*;
import com.iwhalecloud.retail.goods2b.utils.DateUtil;
import com.iwhalecloud.retail.goods2b.utils.GenerateCodeUtil;
import com.iwhalecloud.retail.goods2b.utils.ReflectUtils;
import com.iwhalecloud.retail.goods2b.utils.ZopClientUtil;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.dto.req.MerchantGetReq;
import com.iwhalecloud.retail.partner.service.MerchantService;
import com.iwhalecloud.retail.system.dto.PublicDictDTO;
import com.iwhalecloud.retail.system.service.PublicDictService;
import com.iwhalecloud.retail.workflow.common.WorkFlowConst;
import com.ztesoft.zop.common.message.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ProductBaseServiceImpl implements ProductBaseService {

    @Autowired
    private ProductBaseManager productBaseManager;
    @Autowired
    private ProdFileManager fileManager;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductExtService productExtService;
    @Reference
    private MerchantService merchantService;

    @Autowired
    private ProductFlowService productFlowService;

    @Autowired
    private TagRelService tagRelService;

    @Autowired
    private TypeService typeService;
    @Autowired
    private BrandManager brandManager;
    @Reference
    private PublicDictService publicDictService;
    @Autowired
    private TagRelManager tagRelManager;

    @Autowired
    private GoodsSaleNumService goodsSaleNumService;

    @Value("${zop.secret}")
    private String zopSecret;

    @Value("${zop.url}")
    private String zopUrl;

    @Override
    public ResultVO<ProductBaseGetResp> getProductBase(ProductBaseGetByIdReq req){
        return ResultVO.success(productBaseManager.getProductBase(req.getProductBaseId()));
    }

    @Override
    public ResultVO<Page<ProductBaseGetResp>> getProductBaseList(ProductBaseListReq req){
        Page<ProductBaseGetResp> pageResp = productBaseManager.getProductBaseList(req);
        List<ProductBaseGetResp> respList = pageResp.getRecords();
        for(ProductBaseGetResp dto : respList){
            MerchantGetReq merchantGetReq = new MerchantGetReq();
            merchantGetReq.setMerchantId(dto.getManufacturerId());
            ResultVO<MerchantDTO> result = merchantService.getMerchant(merchantGetReq);
            MerchantDTO merchantDTO = result.getResultData();
            dto.setManufacturerName(null != merchantDTO ? merchantDTO.getMerchantName() : null);

            Integer snCount = 0;
            ProductGetReq productGetReq = new ProductGetReq();
            productGetReq.setProductBaseId(dto.getProductBaseId());
            ResultVO<Page<ProductDTO>> selectProductVO = productService.selectProduct(productGetReq);
            if (selectProductVO.isSuccess() && null != selectProductVO.getResultData() && !CollectionUtils.isEmpty(selectProductVO.getResultData().getRecords())) {
                snCount = selectProductVO.getResultData().getRecords().size();
            }
            dto.setSnCount(snCount);
        }
        return ResultVO.success(pageResp);
    }


    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public ResultVO<String> addProductBase(ProductBaseAddReq req) {
        log.info("ProductBaseServiceImpl.addProductBase,req={}", JSON.toJSONString(req));
        String unitType = req.getUnitType();
        if (StringUtils.isNotEmpty(unitType)) {
            ProductBaseGetReq productBaseGetReq = new ProductBaseGetReq();
            productBaseGetReq.setUnitType(unitType);
            List<ProductBaseGetResp> productBaseGetRespList = productBaseManager.selectProductBase(productBaseGetReq);
            if (!CollectionUtils.isEmpty(productBaseGetRespList)) {
                throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "同一型号只能创建一个产品，已经存在该型号产品");
            }
        }
        ProductBase t = new ProductBase();
        BeanUtils.copyProperties(req, t);
        Date now = new Date();
        t.setCreateDate(now);
        t.setUpdateDate(now);
        t.setIsDeleted(ProductConst.IsDelete.NO.getCode());
        // 产品编码后端生成
        t.setProductCode(GenerateCodeUtil.generateCode());
        if (null == t.getEffDate()) {
            t.setEffDate(now);
        }
        // 默认有效期三年
        if (null == t.getExpDate()) {
            t.setExpDate(DateUtil.getNextYearTime(now, 3));
        }
        //固网产品需要提交串码到itms
        String isInspection = req.getIsInspection();
        String isItms = req.getIsItms();
        if (StringUtils.isNotEmpty(isInspection) && ProductConst.isInspection.YES.getCode().equals(isInspection) &&
                StringUtils.isNotEmpty(isItms) && !ProductConst.isItms.NOPUSH.equals(isItms)) {
            String serialCode = req.getParam20(); //串码  xxxx-1234556612
            String params = req.getParam19(); //附加参数  city_code=731# warehouse=12#source=1#factory=厂家
            String userName = req.getParam18();  //login_name
            if (StringUtils.isNotEmpty(serialCode) && StringUtils.isNotEmpty(params) && StringUtils.isNotEmpty(userName)) {
                if (serialCode.indexOf(",") > -1) {
                    String[] serialCodes = serialCode.split(",");
                    for (int i = 0; i < serialCodes.length; i++) {
                        this.pushItms(serialCodes[i], userName, "ITMS_ADD", params);
                    }
                } else {
                    this.pushItms(serialCode, userName, "ITMS_ADD", params);
                }
            }
        }
        if (StringUtils.isEmpty(req.getPriceLevel())) {
            Double minCost = 0.0;
            List<ProductAddReq> productAddReqs = req.getProductAddReqs();
            if (null != productAddReqs && !productAddReqs.isEmpty()) {
                for (ProductAddReq par : productAddReqs) {
                    String cost = String.valueOf(par.getCost());
                    if (StringUtils.isEmpty(cost) || "null".equals(cost)) {
                        continue;
                    }
                    if (minCost < 0.01) {
                        minCost = par.getCost();
                    } else {
                        minCost = (par.getCost() - minCost) < 0 ? par.getCost() : minCost;
                    }
                }
                log.info("ProductBaseServiceImpl.addProductBase minCost={}", minCost);
                if (minCost > 0.01) {
                    t.setPriceLevel(this.getPriceLevel(minCost));
                    log.info("ProductBaseServiceImpl.addProductBase PriceLevel={}", t.getPriceLevel());
                }
            }
        }
        Integer num = productBaseManager.addProductBase(t);
        String productBaseId = t.getProductBaseId();
        // 添加产品拓展属性
        ProductExtAddReq pear = new ProductExtAddReq();
        BeanUtils.copyProperties(req, pear);
        pear.setProductBaseId(productBaseId);
        productExtService.addProductExt(pear);
        // 添加产品
        List<ProductAddReq> productAddReqs = req.getProductAddReqs();
        String status = "";
        Boolean addResult = true;
        TypeSelectByIdReq typeSelectByIdReq = new TypeSelectByIdReq();
        typeSelectByIdReq.setTypeId(req.getTypeId());
        ResultVO<TypeDetailResp> respResultVO = typeService.getDetailType(typeSelectByIdReq);
        TypeDetailResp type = new TypeDetailResp();
        if (respResultVO.isSuccess() && null != respResultVO.getResultData()) {
            type = respResultVO.getResultData();
        }
        Brand brand = brandManager.getBrandByBrandId(req.getBrandId());
        //易购网下线固网规格同步CRM开关,默认是关闭
        String syncSpecCrm = "";
        List<PublicDictDTO> publicDictDTOs = publicDictService.queryPublicDictListByType("SYNC_SPEC_CRM");
        log.info("ProductBaseServiceImpl.addProductBase publicDictDTOs={}", publicDictDTOs);
        if (!CollectionUtils.isEmpty(publicDictDTOs)) {
            PublicDictDTO publicDictDTO = publicDictDTOs.get(0);
            if (null != publicDictDTO) {
                syncSpecCrm = publicDictDTO.getCodeb();
            }
        }
        if (StringUtils.isEmpty(syncSpecCrm)) {
            syncSpecCrm = "F";
        }
        if (null != productAddReqs && !productAddReqs.isEmpty()) {
            for (ProductAddReq par : productAddReqs) {
                //
                if (StringUtils.isEmpty(req.getIsFixedLine()) || (StringUtils.isNotEmpty(req.getIsFixedLine()) &&
                        !"1".equals(req.getIsFixedLine()))) {
                    String sn = par.getSn();
                    String purchaseString = sn.substring(sn.length() - 3);
                    if ("100".equals(purchaseString)) {
                        par.setPurchaseType(ProductConst.purchaseType.COLLECTIVE.getCode());
                    } else if ("300".equals(purchaseString)) {
                        par.setPurchaseType(ProductConst.purchaseType.SOCIOLOGY.getCode());
                    }
                }
                status = par.getStatus();
                String auditState = ProductConst.AuditStateType.UN_SUBMIT.getCode();
                //除了待提交，都是审核中
                if (!ProductConst.StatusType.SUBMIT.getCode().equals(status)) {
                    auditState = ProductConst.AuditStateType.AUDITING.getCode();
                    par.setStatus(ProductConst.StatusType.AUDIT.getCode());

                    // 如果是: 非厂商添加(一般是管理员）  直接挂网 zhongwenlong
                    if (!req.isManufacturerType()) {
                        auditState = ProductConst.AuditStateType.AUDIT_PASS.getCode();
                        par.setStatus(ProductConst.StatusType.EFFECTIVE.getCode());
                    }

                }
                par.setProductBaseId(productBaseId);
                par.setCreateStaff(t.getCreateStaff());
                par.setAuditState(auditState);
                String productId = productService.addProduct(par);
                //是固网产品并且开发打开才同步CRM
                if (StringUtils.isNotEmpty(req.getIsFixedLine()) && "1".equals(req.getIsFixedLine()) &&
                        "T".equals(syncSpecCrm) && null != type && null != brand) {
                    Map request = new HashMap<>();
                    request.put("Good_id", productId);
                    request.put("u_kind_id", type.getCrmResKind());
                    request.put("u_kind_name", type.getDetailName());
                    request.put("GOOD_BAND", brand.getName());
                    request.put("sales_resource_id", par.getSn());
                    request.put("terminal_type", type.getCrmResType());
                    request.put("sales_resource_name", par.getUnitName());
                    request.put("Good_price", par.getCost());
                    request.put("terminal_type_name", type.getTypeName());
                    this.pushCrm(request);
                }
                if (!CollectionUtils.isEmpty(req.getTagList())) {
                    TagRelBatchAddReq relBatchAddReq = new TagRelBatchAddReq();
                    relBatchAddReq.setProductId(productId);
                    relBatchAddReq.setTagList(req.getTagList());
                    tagRelService.batchAddTagRelProductId(relBatchAddReq);
                }
            }
        }

//         添加零售商标签
        List<String> tagList = req.getTagList();
        if (!CollectionUtils.isEmpty(tagList)) {
            TagRelBatchAddReq relBatchAddReq = new TagRelBatchAddReq();
            relBatchAddReq.setProductBaseId(productBaseId);
            relBatchAddReq.setTagList(tagList);
            tagRelService.batchAddTagRel(relBatchAddReq);
        }

        // 如果是:非厂商添加(一般是管理员）不走审核流程 zhongwenlong
        if (!req.isManufacturerType()) {
            return  ResultVO.success(productBaseId);
        }

        //除了待提交，都是审核中,都要提交审核
        if (!ProductConst.StatusType.SUBMIT.getCode().equals(status) && addResult) {
            StartProductFlowReq startProductFlowReq = new StartProductFlowReq();
            startProductFlowReq.setProductBaseId(productBaseId);
            startProductFlowReq.setDealer(t.getCreateStaff());
            startProductFlowReq.setProductName(req.getProductName());
            startProductFlowReq.setProcessId(ProductConst.APP_PRODUCT_FLOW_PROCESS_ID);
            //如果是固网产品
            if (StringUtils.isNotEmpty(req.getIsFixedLine()) && "1".equals(req.getIsFixedLine())) {
//                String isItms = req.getIsItms();
                if (StringUtils.isNotEmpty(isItms) && (ProductConst.isItms.PUSHIPTV.getCode().equals(isItms) ||
                        ProductConst.isItms.PUSHIPTVMODEL.getCode().equals(isItms))) {
                    startProductFlowReq.setProcessId(ProductConst.ITV_PRODUCT_FLOW_PROCESS_ID);
                } else if (StringUtils.isNotEmpty(isItms) && ProductConst.isItms.PUSHMODEL.getCode().equals(isItms)) {
                    startProductFlowReq.setProcessId(ProductConst.MODEL_PRODUCT_FLOW_PROCESS_ID);
                } else {
                    startProductFlowReq.setProcessId(ProductConst.INTELLIGENCE_PRODUCT_FLOW_PROCESS_ID);
                }
            }
            startProductFlowReq.setParamsType(WorkFlowConst.TASK_PARAMS_TYPE.STRING_PARAMS.getCode());
            startProductFlowReq.setParamsValue(t.getBrandId());
            ResultVO flowResltVO = productFlowService.startProductFlow(startProductFlowReq);
            if (!flowResltVO.isSuccess()) {
                throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), flowResltVO.getResultMsg());
            }
        }
        return ResultVO.success(productBaseId);
    }

    private void pushItms(String deviceId,String userName,String code,String params){
        String b = "";
        String callUrl = "ord.operres.OrdInventoryChange";
        Map request = new HashMap<>();
        request.put("deviceId",deviceId);
        request.put("userName",userName);
        request.put("code",code);
        request.put("params",params);
        try {
            b = this.zopService(callUrl,zopUrl,request,zopSecret);
            if(StringUtils.isNotEmpty(b)){
                Map parseObject = JSON.parseObject(b, new TypeReference<HashMap>(){});
                String body = String.valueOf(parseObject.get("Body"));
                Map parseObject2 = JSON.parseObject(body, new TypeReference<HashMap>(){});
                String inventoryChangeResponse = String.valueOf(parseObject2.get("inventoryChangeResponse"));
                Map parseObject3 = JSON.parseObject(inventoryChangeResponse, new TypeReference<HashMap>(){});
                String inventoryChangeReturn = String.valueOf(parseObject3.get("inventoryChangeReturn"));
                if("-1".equals(inventoryChangeReturn)){
                    throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "串码推送ITMS(新增)失败");
                }else if("1".equals(inventoryChangeReturn)){
                    throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "串码推送ITMS(新增)已经存在");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

//        例子：
//       {"content":{"Good_id":"3033","u_kind_id":"27325","u_kind_name":"泛智能终端","GOOD_BAND":"中维世纪","sales_resource_id":"16297","terminal_type":"5014","sales_resource_name":"JVS-T-BC8WH2M-A0(室外摄像头)","Good_price":"299","terminal_type_name":"智能监控(室外机)"},"method":"ord.res.BandTerminalInDB","access_token":"a1a683ad0c7230088f3d51e215cd275b","version":"1.0"}
//       {"res_code":"00000","res_message":"Success","result":{"Result":{"resultCode":"0","resultMsg":"数据同步成功"},"Message":"成功","ExchangeId":"HNYIGW201905081012191211332008","Code":"0000"}}

    private void pushCrm(Map request){
        String b = "";
        String callUrl = "ord.res.BandTerminalInDB";
        try {
            log.info("ProductBaseServiceImpl.addProductBase pushCrm.req={}",request);
            b = this.zopService(callUrl,zopUrl,request,zopSecret);
            log.info("ProductBaseServiceImpl.addProductBase pushCrm.resp={}",b);
            if(StringUtils.isNotEmpty(b)){
                Map parseObject = JSON.parseObject(b, new TypeReference<HashMap>(){});
                String res_code = String.valueOf(parseObject.get("res_code"));
                if("00000".equals(res_code)){
                    String body = String.valueOf(parseObject.get("result"));
                    Map result = JSON.parseObject(body, new TypeReference<HashMap>(){});
                    String code = String.valueOf(result.get("Code"));
                    if("0000".equals(code)){
                        String crmResultStr = String.valueOf(parseObject.get("result"));
                        Map crmResult = JSON.parseObject(crmResultStr, new TypeReference<HashMap>(){});
                        String resultCode = String.valueOf(crmResult.get("resultCode"));
                        if(!"0".equals(resultCode)){
                            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "推送固网规格到CRM报错："+String.valueOf(crmResult.get("resultMsg")));
                        }
                    }else{
                        throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "推送固网规格到CRM报错："+String.valueOf(result.get("Message")));
                    }
                }else{
                    throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "推送固网规格到CRM报错："+String.valueOf(parseObject.get("res_message")));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String getPriceLevel(Double cost){
        String priceLevel = "";
        if(cost>0.01 && (cost-60000)<=0.01){
            priceLevel = "0-600";
        }
        if((cost-60000) >0.01 && (cost-99000)<=0.01){
            priceLevel = "600-990";
        }
        if((cost-99000) >0.01 && (cost-159000)<=0.01){
            priceLevel = "990-1590";
        }
        if((cost-159000) >0.01 && (cost-300000)<=0.01){
            priceLevel = "1590-3000";
        }
        if((cost-300000) >0.01){
            priceLevel = "3000-∞";
        }

        return priceLevel;
    }

    @Override
    @Transactional(isolation= Isolation.DEFAULT,propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public ResultVO<Integer> updateProductBase(ProductBaseUpdateReq req) {
        log.info("ProductBaseServiceImpl.updateProductBase,req={}", JSON.toJSONString(req));

        final long startTime = System.currentTimeMillis();
        ProductBaseGetByIdReq req1 = new ProductBaseGetByIdReq();
        req1.setProductBaseId(req.getProductBaseId());
        ResultVO<ProductBaseGetResp> product = this.getProductBase(req1);
        if (product==null||product.getResultData() == null) {
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "产品不存在");
        }
        List<String> tagList = req.getTagList();
        if (!CollectionUtils.isEmpty(tagList)) {
            TagRelDeleteByGoodsIdReq relDeleteByGoodsIdReq = new TagRelDeleteByGoodsIdReq();
            relDeleteByGoodsIdReq.setProductBaseId(req.getProductBaseId());
            tagRelService.deleteTagRelByGoodsId(relDeleteByGoodsIdReq);
            TagRelBatchAddReq relBatchAddReq = new TagRelBatchAddReq();
            relBatchAddReq.setTagList(tagList);
            relBatchAddReq.setProductBaseId(req.getProductBaseId());
            tagRelService.batchAddTagRel(relBatchAddReq);
        }
        ProductExtUpdateReq extUpdateReq = req.getProductExtUpdateReq();
        String notCheckField = "productBaseId";
        Boolean isAllFieldNull = ReflectUtils.isAllFieldNull(extUpdateReq, notCheckField);
        ProductExtGetReq productExtGetReq = new ProductExtGetReq();
        productExtGetReq.setProductBaseId(req.getProductBaseId());
        ResultVO<ProductExtGetResp> productExtVO = productExtService.getProductExt(productExtGetReq);
        if (!isAllFieldNull && productExtVO.isSuccess() && null != productExtVO.getResultData()) {
            ResultVO<Integer>  updateVO = productExtService.updateProductExt(extUpdateReq);
            log.info("ProductBaseServiceImpl.updateProductBase productExtService.updateProductExt,req={}", JSON.toJSONString(extUpdateReq));
        }else if(!isAllFieldNull && productExtVO.isSuccess() && null == productExtVO.getResultData()){
            ProductExtAddReq productExtAddReq = new ProductExtAddReq();
            BeanUtils.copyProperties(extUpdateReq, productExtAddReq);
            ResultVO<Integer> addVO = productExtService.addProductExt(productExtAddReq);
            log.info("ProductBaseServiceImpl.updateProductBase productExtService.addProductExt,req={}", JSON.toJSONString(productExtAddReq));
        }
        //原审核状态
        String oldAuditState = "";
        List<ProductUpdateReq> productUpdateReqs = req.getProductUpdateReqs();
        //获取产品
        ProductGetReq productGetReq = new ProductGetReq();
        productGetReq.setProductBaseId(req.getProductBaseId());
        productGetReq.setPageNo(1);
        productGetReq.setPageSize(Integer.MAX_VALUE);
        ResultVO<Page<ProductDTO>> productListResult =  productService.selectProduct(productGetReq);
        if(productListResult==null||!productListResult.isSuccess()||productListResult.getResultData()==null){
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "原产品为空无法获取审核状态");
        }
        Page<ProductDTO> page = productListResult.getResultData();
        if(page==null||page.getRecords()==null||page.getRecords().isEmpty()){
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "原产品为空无法获取审核状态");
        }
        oldAuditState = page.getRecords().get(0).getAuditState();
        if(StringUtils.isEmpty(oldAuditState)){
            oldAuditState = ProductConst.AuditStateType.UN_SUBMIT.getCode();
        }
        String newState = "";
        for (ProductUpdateReq productUpdateReq : productUpdateReqs) {
            String productId = productUpdateReq.getProductId();
            String isDeleted = productUpdateReq.getIsDeleted();
            String state = productUpdateReq.getStatus();
            newState = state;
            if(ProductConst.IsDelete.NO.getCode().equals(isDeleted) && (StringUtils.isEmpty(req.getIsFixedLine()) ||
                    (StringUtils.isNotEmpty(req.getIsFixedLine()) && !"1".equals(req.getIsFixedLine())))){
                String sn = productUpdateReq.getSn();
                String purchaseString = sn.substring(sn.length() - 3);
                if ("100".equals(purchaseString)){
                    productUpdateReq.setPurchaseType(ProductConst.purchaseType.COLLECTIVE.getCode());
                }else if("300".equals(purchaseString)){
                    productUpdateReq.setPurchaseType(ProductConst.purchaseType.SOCIOLOGY.getCode());
                }
            }
            if (ProductConst.IsDelete.YES.getCode().equals(isDeleted) && StringUtils.isNotBlank(productId)) {
                PrdoProductDeleteReq prdoProductDeleteReq = new PrdoProductDeleteReq();
                prdoProductDeleteReq.setProductId(productId);
                productService.updateProdProductDelete(prdoProductDeleteReq);
                continue;
            }
            if(ProductConst.IsDelete.NO.getCode().equals(isDeleted) && StringUtils.isNotBlank(productId)){
                productUpdateReq.setAuditState(ProductConst.AuditStateType.UN_SUBMIT.getCode());
                productUpdateReq.setStatus(ProductConst.StatusType.SUBMIT.getCode());
                if(!ProductConst.StatusType.SUBMIT.getCode().equals(state)){
                    productUpdateReq.setAuditState(ProductConst.AuditStateType.AUDITING.getCode());
                    productUpdateReq.setStatus(ProductConst.StatusType.AUDIT.getCode());

                    // 如果是非厂商添加(一般是管理员）并且之前的是已审核通过状态  直接挂网 zhongwenlong
                    if (!req.isManufacturerType()
                            && ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(oldAuditState)) {
                        productUpdateReq.setStatus(ProductConst.StatusType.EFFECTIVE.getCode());
                        productUpdateReq.setAuditState(ProductConst.AuditStateType.AUDIT_PASS.getCode());
                    }

                }
                if (!CollectionUtils.isEmpty(tagList)) {
                    TagRelDeleteByGoodsIdReq relDeleteByGoodsIdReq = new TagRelDeleteByGoodsIdReq();
                    relDeleteByGoodsIdReq.setProductId(productId);
                    tagRelService.deleteTagRelByProductId(relDeleteByGoodsIdReq);
                    TagRelBatchAddReq relBatchAddReq = new TagRelBatchAddReq();
                    relBatchAddReq.setTagList(tagList);
                    relBatchAddReq.setProductId(productId);
                    tagRelService.batchAddTagRelProductId(relBatchAddReq);
                }
                productService.updateProdProduct(productUpdateReq);
                log.info("ProductBaseServiceImpl.updateProductBase productService.updateProdProduct,req={}", JSON.toJSONString(productUpdateReq));
                continue;
            }
            if(StringUtils.isBlank(productId)){
                ProductAddReq par = new ProductAddReq();
                BeanUtils.copyProperties(productUpdateReq, par);
                par.setProductBaseId(req.getProductBaseId());
                par.setCreateStaff(req.getUpdateStaff());
                par.setAuditState(ProductConst.AuditStateType.UN_SUBMIT.getCode());
                par.setStatus(ProductConst.StatusType.SUBMIT.getCode());
                if(!ProductConst.StatusType.SUBMIT.getCode().equals(state)){
                    par.setAuditState(ProductConst.AuditStateType.AUDITING.getCode());
                    par.setStatus(ProductConst.StatusType.AUDIT.getCode());

                    // 如果是: 非厂商添加(一般是管理员）  直接挂网 zhongwenlong
                    if (!req.isManufacturerType()) {
                        par.setStatus(ProductConst.StatusType.EFFECTIVE.getCode());
                        par.setAuditState(ProductConst.AuditStateType.AUDIT_PASS.getCode());
                    }
                }
                productService.addProduct(par);
                continue;
            }
        }

        // 添加产品
        List<ProductAddReq> productAddReqs = req.getProductAddReqs();
        String status = "";
        Boolean addResult = true;
        if (null != productAddReqs && !productAddReqs.isEmpty()){
            for (ProductAddReq par : productAddReqs){
                //
                if(StringUtils.isEmpty(req.getIsFixedLine()) || (StringUtils.isNotEmpty(req.getIsFixedLine()) &&
                        !"1".equals(req.getIsFixedLine()))){
                    String sn = par.getSn();
                    String purchaseString = sn.substring(sn.length() - 3);
                    if ("100".equals(purchaseString)){
                        par.setPurchaseType(ProductConst.purchaseType.COLLECTIVE.getCode());
                    }else if("300".equals(purchaseString)){
                        par.setPurchaseType(ProductConst.purchaseType.SOCIOLOGY.getCode());
                    }
                }
                status = par.getStatus();
                String auditState = ProductConst.AuditStateType.UN_SUBMIT.getCode();
                //除了待提交，都是审核中
                if(!ProductConst.StatusType.SUBMIT.getCode().equals(status)){
                    auditState =ProductConst.AuditStateType.AUDITING.getCode();
                    par.setStatus(ProductConst.StatusType.AUDIT.getCode());

                    // 如果是非厂商添加(一般是管理员）  直接挂网 zhongwenlong
                    if (!req.isManufacturerType()) {
                        auditState =ProductConst.AuditStateType.AUDIT_PASS.getCode();
                        par.setStatus(ProductConst.StatusType.EFFECTIVE.getCode());
                    }

                }
                par.setProductBaseId(req.getProductBaseId());
                par.setCreateStaff(req.getUpdateStaff());
                par.setAuditState(auditState);
                String productId = productService.addProduct(par);
                if (!CollectionUtils.isEmpty(tagList)) {
                    TagRelBatchAddReq relBatchAddReq = new TagRelBatchAddReq();
                    relBatchAddReq.setTagList(tagList);
                    relBatchAddReq.setProductId(productId);
                    tagRelService.batchAddTagRelProductId(relBatchAddReq);
                }
            }
        }
        req.setUpdateDate(new Date());
        if(StringUtils.isEmpty(req.getPriceLevel())){
            Double minCost = 0.0;
            List<ProductUpdateReq> productUpdateReqs2 = req.getProductUpdateReqs();
            if (null != productUpdateReqs2 && !productUpdateReqs2.isEmpty()) {
                for (ProductUpdateReq par : productUpdateReqs2) {
                    String cost = String.valueOf(par.getCost());
                    if(StringUtils.isEmpty(cost) || "null".equals(cost)){
                        continue;
                    }
                    if(minCost < 0.01){
                        minCost = par.getCost();
                    }else{
                        minCost = (par.getCost()-minCost)<0 ? par.getCost():minCost;
                    }
                }
                log.info("ProductBaseServiceImpl.updateProductBase minCost={}",minCost);
                if(minCost > 0.01){
                    req.setPriceLevel(this.getPriceLevel(minCost));
                    log.info("ProductBaseServiceImpl.updateProductBase PriceLevel={}",req.getPriceLevel());
                }
            }
        }
        int index = productBaseManager.updateProductBase(req);

        if (!req.isManufacturerType()) {
            // 如果是 非厂商修改(一般是管理员）不走审核流程 zhongwenlong
            return  ResultVO.success(index);
        }

        //修改成功并且非审核中
        if(index>0&&!ProductConst.AuditStateType.AUDITING.getCode().equals(oldAuditState)){

            //非待提交的都是待审核，需要启动流程
             if(!ProductConst.StatusType.SUBMIT.getCode().equals(newState)){
                 //审核不通过的则重启流程
                 if(ProductConst.AuditStateType.AUDIT_UN_PASS.getCode().equals(oldAuditState)){
                     StartProductFlowReq startProductFlowReq = new StartProductFlowReq();
                     startProductFlowReq.setProductBaseId(req.getProductBaseId());
                     startProductFlowReq.setDealer(req.getUpdateStaff());
                     ResultVO flowResltVO =productFlowService.reStartProductFlow(startProductFlowReq);
                     if(!flowResltVO.isSuccess()){
                         throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), flowResltVO.getResultMsg());
                     }
                 }else if(ProductConst.AuditStateType.UN_SUBMIT.getCode().equals(oldAuditState)
                         || ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(oldAuditState)){
                     //原审核状态为待提交，且新状态为非待提交
                     String processId =ProductConst.APP_PRODUCT_FLOW_PROCESS_ID;
                     if(StringUtils.isNotEmpty(req.getIsFixedLine()) && "1".equals(req.getIsFixedLine())){
                         String isItms = req.getIsItms();
                         if(StringUtils.isNotEmpty(isItms) && (ProductConst.isItms.PUSHIPTV.getCode().equals(isItms) ||
                                 ProductConst.isItms.PUSHIPTVMODEL.getCode().equals(isItms))){
                             processId =ProductConst.ITV_PRODUCT_FLOW_PROCESS_ID;
                         }else if(StringUtils.isNotEmpty(isItms) && ProductConst.isItms.PUSHMODEL.getCode().equals(isItms)){
                             processId =ProductConst.MODEL_PRODUCT_FLOW_PROCESS_ID;
                         }else{
                             processId =ProductConst.INTELLIGENCE_PRODUCT_FLOW_PROCESS_ID;
                         }
                     }
                     if(ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(oldAuditState)){
//                         processId =ProductConst.UPDATE_PRODUCT_FLOW_PROCESS_ID;
                         processId = this.updateProductFlow(req);
                         log.info("ProductBaseServiceImpl.updateProductBase processId={}",processId);
                     }
                     //没有修改则不需走流程
                     if(StringUtils.isNotEmpty(processId)){
                         StartProductFlowReq startProductFlowReq = new StartProductFlowReq();
                         startProductFlowReq.setProductBaseId(req.getProductBaseId());
                         startProductFlowReq.setDealer(req.getUpdateStaff());
                         startProductFlowReq.setProductName(product.getResultData().getProductName());
                         startProductFlowReq.setProcessId(processId);
                         startProductFlowReq.setParamsValue(req.getBrandId());
                         ResultVO flowResltVO =productFlowService.startProductFlow(startProductFlowReq);
                         if(!flowResltVO.isSuccess()){
                             throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), flowResltVO.getResultMsg());
                         }
                     }
                 }
             }
        }
        return  ResultVO.success(index);
    }

    private String updateProductFlow(ProductBaseUpdateReq req){
        String updateProductFlow="";
        OldProductBaseUpdateReq oldReq = req.getOldProductBaseUpdateReq();

        List<ProductUpdateReq> productUpdateReqs = req.getProductUpdateReqs();
        if(null == oldReq){
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "修改产品必须传入老产品产品信息");
        }
        List<ProductUpdateReq> OldProductUpdateReqs = oldReq.getProductUpdateReqs();
        if(CollectionUtils.isEmpty(productUpdateReqs) || CollectionUtils.isEmpty(OldProductUpdateReqs)){
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "修改产品必须传入新老产品产品信息");
        }
        if(StringUtils.isEmpty(req.getProductName()) || StringUtils.isEmpty(oldReq.getProductName())){
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), "修改产品必须传入新老产品名称");
        }

        //流程1
        if(!CollectionUtils.isEmpty(req.getProductAddReqs())){
            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
        }
        //流程2
        if(!req.getProductName().equals(oldReq.getProductName())){
            if(ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
            }else{
                updateProductFlow = ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID;
            }
        }
        //流程2
        if(!StringUtils.isEmpty(req.getSallingPoint()) && !StringUtils.isEmpty(oldReq.getSallingPoint()) &&
                !req.getSallingPoint().equals(oldReq.getSallingPoint())){
            if(ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
            }else{
                updateProductFlow = ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID;
            }
        }

        for(ProductUpdateReq newUpdateReq: productUpdateReqs){
            for(ProductUpdateReq oldUpdateReq:OldProductUpdateReqs){
                if(newUpdateReq.getProductId().equals(oldUpdateReq.getProductId())){
                    //流程1
                    if( ( StringUtils.isNotEmpty(newUpdateReq.getSn()) && StringUtils.isNotEmpty(oldUpdateReq.getSn()) && !newUpdateReq.getSn().equals(oldUpdateReq.getSn()) ) ||
                            ( StringUtils.isNotEmpty(newUpdateReq.getIsDeleted()) && StringUtils.isNotEmpty(oldUpdateReq.getIsDeleted()) && !newUpdateReq.getIsDeleted().equals(oldUpdateReq.getIsDeleted()))){
                        if(ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                    //流程1
                    if((StringUtils.isNotEmpty(newUpdateReq.getAttrValue1()) && StringUtils.isEmpty(oldUpdateReq.getAttrValue1())) ||
                            (StringUtils.isNotEmpty(oldUpdateReq.getAttrValue1()) && StringUtils.isEmpty(newUpdateReq.getAttrValue1())) ||
                            (StringUtils.isNotEmpty(newUpdateReq.getAttrValue1()) && StringUtils.isNotEmpty(oldUpdateReq.getAttrValue1())) &&
                                    !newUpdateReq.getAttrValue1().equals(oldUpdateReq.getAttrValue1())){
                        if(ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                    //流程1
                    if((StringUtils.isNotEmpty(newUpdateReq.getAttrValue2()) && StringUtils.isEmpty(oldUpdateReq.getAttrValue2())) ||
                            (StringUtils.isNotEmpty(oldUpdateReq.getAttrValue2()) && StringUtils.isEmpty(newUpdateReq.getAttrValue2())) ||
                            (StringUtils.isNotEmpty(newUpdateReq.getAttrValue2()) && StringUtils.isNotEmpty(oldUpdateReq.getAttrValue2())) &&
                                    !newUpdateReq.getAttrValue2().equals(oldUpdateReq.getAttrValue2())){
                        if(ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                    //流程1
                    if((StringUtils.isNotEmpty(newUpdateReq.getAttrValue3()) && StringUtils.isEmpty(oldUpdateReq.getAttrValue3())) ||
                            (StringUtils.isNotEmpty(oldUpdateReq.getAttrValue3()) && StringUtils.isEmpty(newUpdateReq.getAttrValue3())) ||
                            (StringUtils.isNotEmpty(newUpdateReq.getAttrValue3()) && StringUtils.isNotEmpty(oldUpdateReq.getAttrValue3())) &&
                                    !newUpdateReq.getAttrValue3().equals(oldUpdateReq.getAttrValue3())){
                        if(ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                    //流程1
                    if( ProductConst.IsDelete.NO.getCode().equals(newUpdateReq.getIsDeleted()) &&
                            ((null!=newUpdateReq.getCost() && null!=oldUpdateReq.getCost() && newUpdateReq.getCost() - oldUpdateReq.getCost() !=0) ||
                            (null!=newUpdateReq.getLocalSupplyFeeLower() && null!=oldUpdateReq.getLocalSupplyFeeLower() && newUpdateReq.getLocalSupplyFeeLower() - oldUpdateReq.getLocalSupplyFeeLower() != 0) ||
                            (null!=newUpdateReq.getLocalSupplyFeeUpper() && null!=oldUpdateReq.getLocalSupplyFeeUpper() && newUpdateReq.getLocalSupplyFeeUpper() - oldUpdateReq.getLocalSupplyFeeUpper() != 0) ||
                            (null!=newUpdateReq.getSupplyFeeLower() && null!=oldUpdateReq.getSupplyFeeLower() && newUpdateReq.getSupplyFeeLower() - oldUpdateReq.getSupplyFeeLower() !=0) ||
                            (null!=newUpdateReq.getSupplyFeeUpper() && null!=oldUpdateReq.getSupplyFeeUpper() && newUpdateReq.getSupplyFeeUpper() - oldUpdateReq.getSupplyFeeUpper() !=0))){
                        if(ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                    //流程2
                    if(this.compareProdFile(newUpdateReq.getFileAddReqs(),oldUpdateReq.getFileAddReqs())){
                        if(ProductConst.BRANDEDIT_PRODUCT_FLOW_PROCESS_ID.equals(updateProductFlow)){
                            updateProductFlow = ProductConst.EDIT_PRODUCT_FLOW_PROCESS_ID;
                        }else{
                            updateProductFlow = ProductConst.OPEREDIT_PRODUCT_FLOW_PROCESS_ID;
                        }
                    }
                }
            }
        }
        return updateProductFlow;
    }

    private boolean compareProdFile(List<FileAddReq> newFileAddReqs,List<FileAddReq> oldFileAddReqs){
        if(CollectionUtils.isEmpty(newFileAddReqs) && CollectionUtils.isEmpty(oldFileAddReqs)){
            return false;
        }
        if(!CollectionUtils.isEmpty(newFileAddReqs) && CollectionUtils.isEmpty(oldFileAddReqs)){
            return true;
        }
        if(!CollectionUtils.isEmpty(oldFileAddReqs) && CollectionUtils.isEmpty(newFileAddReqs)){
            return true;
        }
        if(newFileAddReqs.size()!=oldFileAddReqs.size()){
            return true;
        }
        for(int i = 0;i<newFileAddReqs.size();i++){
            FileAddReq newFile = newFileAddReqs.get(i);
            FileAddReq oldFile = oldFileAddReqs.get(i);
            if(StringUtils.isNotEmpty(newFile.getFileUrl()) && StringUtils.isNotEmpty(oldFile.getFileUrl()) &&
                    newFile.getFileUrl().equals(oldFile.getFileUrl())){
            }else if(StringUtils.isEmpty(newFile.getFileUrl()) && StringUtils.isEmpty(oldFile.getFileUrl())){
            }else {
                return true;
            }
            if(StringUtils.isNotEmpty(newFile.getThumbnailUrl()) && StringUtils.isNotEmpty(oldFile.getThumbnailUrl()) &&
                    newFile.getThumbnailUrl().equals(oldFile.getThumbnailUrl())){
            }else if(StringUtils.isEmpty(newFile.getThumbnailUrl()) && StringUtils.isEmpty(oldFile.getThumbnailUrl())) {
            }else {
                return true;
            }
            if(StringUtils.isNotEmpty(newFile.getThreeDimensionsUrl()) && StringUtils.isNotEmpty(oldFile.getThreeDimensionsUrl()) &&
                    newFile.getThreeDimensionsUrl().equals(oldFile.getThreeDimensionsUrl())){
            }else if(StringUtils.isEmpty(newFile.getThreeDimensionsUrl()) && StringUtils.isEmpty(newFile.getThreeDimensionsUrl())){
            }else{
                return true;
            }
        }
        return false;
    }

    @Override
    public ResultVO<Integer> deleteProdProductBase(ProdProductBaseDeleteReq req){
        return ResultVO.success(productBaseManager.deleteProdProductBase(req.getProductBaseId()));
    }


    @Override
    public ResultVO<Integer> softDelProdProductBase(ProdProductBaseSoftDelReq req){
        return ResultVO.success(productBaseManager.softDelProdProductBase(req.getProductBaseId()));
    }

    @Override
    public ResultVO<List<ProductBaseGetResp>> selectProductBase(ProductBaseGetReq req){
        return ResultVO.success(productBaseManager.selectProductBase(req));
    }

    /**
     * 产品详情
     * @param req
     * @return
     */
    @Override
    public ResultVO<ProductDetailResp> getProductDetail(ProductDetailGetByBaseIdReq req){
        ProductDetailResp productDetail = productBaseManager.getProductDetail(req.getProductBaseId());
        log.info("ProductBaseServiceImpl.getProductDetail ProductDetailResp={}",productDetail);
        if (null == productDetail){
            return ResultVO.success(productDetail);
        }
        ProductGetReq productGetReq = new ProductGetReq();
        productGetReq.setProductBaseId(req.getProductBaseId());
        ResultVO<Page<ProductDTO>> resultVO = productService.selectProduct(productGetReq);
        Page<ProductDTO> page = resultVO.getResultData();
        List<ProductDTO> list = page.getRecords();
        productDetail.setProductAddReqs(list);
        if (!CollectionUtils.isEmpty(list)) {
            for(ProductDTO productDTO : list){
                productDTO.setIsSale(this.isSaleByProductId(productDTO.getProductId()));
            }
        }

//        List<String> listTagRelId = new ArrayList<>();
//        if (!CollectionUtils.isEmpty(list)) {
//            for (ProductDTO productDTO : list) {
//                TagRelListReq tagRelListReq = new TagRelListReq();
//                tagRelListReq.setProductId(productDTO.getProductId());
//                List<TagRelListResp> listTagRel = tagRelManager.listTagRel(tagRelListReq);
//                log.info("ProductBaseServiceImpl.getProductDetail tagRelManager.listTagRel req={}, resp={}", JSON.toJSONString(tagRelListReq), JSON.toJSONString(listTagRel));
//                List<String> relIdList = listTagRel.stream().map(TagRelListResp::getTagId).collect(Collectors.toList());
//                if (!CollectionUtils.isEmpty(relIdList)) {
//                    listTagRelId.addAll(relIdList);
//                }
//            }
//        }
//        if(CollectionUtils.isEmpty(productDetail.getTagList())){
//            productDetail.setTagList(listTagRelId);
//        } else {
//            List<String> tagList = productDetail.getTagList();
//            if (!CollectionUtils.isEmpty(listTagRelId)) {
//                tagList.addAll(listTagRelId);
//            }
//        }

        // zhongwenlong 获取标签ID集合
        List<String> listTagRelId = new ArrayList<>();
        TagRelListReq tagRelListReq = new TagRelListReq();
        tagRelListReq.setProductBaseId(req.getProductBaseId());
        List<TagRelListResp> listTagRel = tagRelManager.listTagRel(tagRelListReq);
        log.info("ProductBaseServiceImpl.getProductDetail tagRelManager.listTagRel req={}, resp={}", JSON.toJSONString(tagRelListReq), JSON.toJSONString(listTagRel));
        List<String> relTagIdList = listTagRel.stream().map(TagRelListResp::getTagId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(relTagIdList)) {
            // 去重
            HashSet<String> relTagIdHashSet = new HashSet<>(relTagIdList);
            productDetail.setTagList(Lists.newArrayList(relTagIdHashSet));
        }

        ProductExtGetReq productExtGetReq = new ProductExtGetReq();
        productExtGetReq.setProductBaseId(req.getProductBaseId());
        ResultVO<ProductExtGetResp> productExtVO = productExtService.getProductExt(productExtGetReq);
        if (null != productExtVO && productExtVO.isSuccess() && null != productExtVO.getResultData()) {
            ProductExtGetResp productExtGetResp = productExtVO.getResultData();
            BeanUtils.copyProperties(productExtGetResp, productDetail);
        }

        MerchantGetReq merchantGetReq = new MerchantGetReq();
        merchantGetReq.setMerchantId(productDetail.getManufacturerId());
        ResultVO<MerchantDTO> result = merchantService.getMerchant(merchantGetReq);
        MerchantDTO dto = result.getResultData();
        productDetail.setManufacturerName(null!=dto? dto.getMerchantName():null);

        return ResultVO.success(productDetail);
    }

    @Override
    public ResultVO<ExchangeObjectGetResp> getExchangeObject(ProductExchangeObjectGetReq req) {
        ProductGetByIdReq req1 = new ProductGetByIdReq();
        req1.setProductId(req.getProductId());
        ResultVO<ProductResp> respResultVO = productService.getProduct(req1);
        if (!respResultVO.isSuccess() || respResultVO.getResultData() == null) {
            return ResultVO.error("查询产品信息为空");
        }
        ProductResp productResp = respResultVO.getResultData();
        ProductBaseGetResp productBaseGetResp = productBaseManager.getProductBase(productResp.getProductBaseId());
        if (productBaseGetResp == null) {
            return ResultVO.error("查询产品基本信息为空");
        }
        ExchangeObjectGetResp resp = new ExchangeObjectGetResp();
        resp.setExchangeObject(productBaseGetResp.getExchangeObject());
        resp.setManufacturerId(productBaseGetResp.getManufacturerId());
        return ResultVO.success(resp);
    }

    @Override
    public ResultVO<Boolean> updateAvgApplyPrice(ProductBaseUpdateReq req) {
        String productBaseId = req.getProductBaseId();
        if (productBaseId == null || productBaseId == "") {
            return ResultVO.error(ResultCodeEnum.LACK_OF_PARAM);
        }
        Double avgSupplyPrice = productBaseManager.getAvgSupplyPrice(productBaseId);
        log.info("ProductBaseServiceImpl.updateAvgApplyPrice productBaseId={},avgSupplyPrice={}",productBaseId,avgSupplyPrice);
        if (avgSupplyPrice == null) {
            return ResultVO.success(true);
        }
        ProductBaseUpdateReq productBaseUpdateReq = new ProductBaseUpdateReq();
        productBaseUpdateReq.setProductBaseId(productBaseId);
        productBaseUpdateReq.setAvgSupplyPrice(avgSupplyPrice);
        productBaseManager.updateProductBase(productBaseUpdateReq);
        return ResultVO.success(true);
    }

    @Override
    public ResultVO<List<String>> getSeq(int i) {
        List<String> list = new ArrayList<>();
        if(i==0){
            ResultVO.success(list);
        }
        for(int j=0;j<i;j++){
            list.add(productBaseManager.getSeq());
        }
        return ResultVO.success(list);
    }

    @Override
    public Boolean isSaleByProductId(String productId) {
        boolean flag = false;
        ResultVO<List<GoodsSaleNumDTO>> resultVO = goodsSaleNumService.getProductSaleOrder(GoodsConst.CACHE_NAME_PRODUCT_SALE_ORDER_WHOLE);
        if(resultVO.isSuccess() && null!=resultVO.getResultData()){
            List<GoodsSaleNumDTO> goodsSaleNumDTOs = resultVO.getResultData();
            if(!CollectionUtils.isEmpty(goodsSaleNumDTOs)){
                for(GoodsSaleNumDTO goodsSaleNumDTO: goodsSaleNumDTOs){
                    if(productId.equals(goodsSaleNumDTO.getProductId())) {
                        return true;
                    }
                }
            }
        }
        ResultVO<List<GoodsSaleNumDTO>> resultVO1 = goodsSaleNumService.queryProductSaleOrderByProductId(productId);
        if(resultVO1.isSuccess() && null!=resultVO1.getResultData()){
            List<GoodsSaleNumDTO> goodsSaleNumDTOs = resultVO1.getResultData();
            if(!CollectionUtils.isEmpty(goodsSaleNumDTOs)){
                for(GoodsSaleNumDTO goodsSaleNumDTO: goodsSaleNumDTOs){
                    if(productId.equals(goodsSaleNumDTO.getProductId())) {
                        return true;
                    }
                }
            }
        }
        return flag;
    }

    public String zopService(String method, String zopUrl, Object request, String zopSecret) {
        String version = "1.0";
        ResponseResult responseResult = ZopClientUtil.callRest(zopSecret, zopUrl, method, version, request);
        String resCode = "00000";
        String returnStr = null;
        if (resCode.equals(responseResult.getRes_code())) {
            Object result = responseResult.getResult();
            returnStr = String.valueOf(result);
        }else{
            log.info("能开请求失败：method：" +method+"，zopUrl:"+ zopUrl+"，resCode:"
                    +responseResult.getRes_code()+"，msg:"+responseResult.getRes_message());
        }
        return returnStr;
    }

    @Override
    public ResultVO<List<String>> getDistinctUnitType(String typeId, String brandId){
        return ResultVO.success(productBaseManager.getDistinctUnitType(typeId, brandId));
    }

    @Override
    public ResultVO<ProductBaseLightResp> getProductBaseByProductId(String productId){
        return ResultVO.success(productBaseManager.getProductBaseByProductId(productId));
    }

    @Override
    public ResultVO updateCrmType(ProductBaseUpdateReq req) {

        return ResultVO.success(productBaseManager.updateProductBase(req));
    }
}