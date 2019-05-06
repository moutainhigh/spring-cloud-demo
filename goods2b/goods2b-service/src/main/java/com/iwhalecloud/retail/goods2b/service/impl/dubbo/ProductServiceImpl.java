package com.iwhalecloud.retail.goods2b.service.impl.dubbo;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.goods2b.common.FileConst;
import com.iwhalecloud.retail.goods2b.common.GoodsConst;
import com.iwhalecloud.retail.goods2b.common.ProductConst;
import com.iwhalecloud.retail.goods2b.dto.ProdFileDTO;
import com.iwhalecloud.retail.goods2b.dto.ProductDTO;
import com.iwhalecloud.retail.goods2b.dto.req.*;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductPageResp;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductResourceResp;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductResp;
import com.iwhalecloud.retail.goods2b.dto.resp.QueryProductInfoResqDTO;
import com.iwhalecloud.retail.goods2b.entity.ProdFile;
import com.iwhalecloud.retail.goods2b.entity.Product;
import com.iwhalecloud.retail.goods2b.entity.Tags;
import com.iwhalecloud.retail.goods2b.manager.ProdFileManager;
import com.iwhalecloud.retail.goods2b.manager.ProductManager;
import com.iwhalecloud.retail.goods2b.manager.TagTelManager;
import com.iwhalecloud.retail.goods2b.manager.TagsManager;
import com.iwhalecloud.retail.goods2b.service.dubbo.ProductBaseService;
import com.iwhalecloud.retail.goods2b.service.dubbo.ProductService;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.dto.req.MerchantGetReq;
import com.iwhalecloud.retail.partner.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductManager productManager;

    @Autowired
    private ProdFileManager fileManager;

    @Reference
    private MerchantService merchantService;

    @Autowired
    private ProductBaseService productBaseService;

    @Autowired
    private TagsManager tagsManager;

    @Autowired
    private TagTelManager tagTelManager;

    @Override
    public ResultVO<String> getMerchantByProduct(MerChantGetProductReq req) {
        return ResultVO.success(productManager.getMerChantByProduct(req.getProductId()));
    }

    @Override
    public ResultVO<ProductResp> getProduct(ProductGetByIdReq req) {
        return ResultVO.success(productManager.getProduct(req.getProductId()));
    }


    @Override
    @Transactional(isolation= Isolation.DEFAULT,propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public ResultVO<Integer> addProduct(ProductAddReq req){
        Product t = new Product();
        BeanUtils.copyProperties(req, t);
        Date now = new Date();
        t.setCreateDate(now);
        t.setUpdateDate(now);
        t.setIsDeleted(ProductConst.IsDelete.NO.getCode());
        //默认未提交审核
        if(StringUtils.isEmpty(t.getAuditState())){
             t.setAuditState(ProductConst.AuditStateType.UN_SUBMIT.getCode());
        }

        Integer num = productManager.insert(t);

        // 添加图片
        String productId = t.getProductId();
        String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
        List<FileAddReq> fileAddReqs = req.getFileAddReqs();
        if (null != fileAddReqs && !fileAddReqs.isEmpty()) {
            for (FileAddReq fReq : fileAddReqs) {
                ProdFile file = new ProdFile();
                BeanUtils.copyProperties(fReq, file);
                file.setTargetType(targetType);
                file.setTargetId(productId);
                fileManager.addFile(file);
            }
        }
        return ResultVO.success(num);
    }

    @Override
    public ResultVO<ProductResp> getProductBySn(String sn){
        return ResultVO.success(productManager.getProductBySn(sn));
    }

    @Override
    public ResultVO<String> addProductByZT(ProductAddReq req){
        Product t = new Product();
        BeanUtils.copyProperties(req, t);
        Date now = new Date();
        t.setCreateDate(now);
        t.setUpdateDate(now);
        t.setIsDeleted(ProductConst.IsDelete.NO.getCode());
        //默认未提交审核
        if(StringUtils.isEmpty(t.getAuditState())){
            t.setAuditState(ProductConst.AuditStateType.UN_SUBMIT.getCode());
        }

        Integer num = productManager.insert(t);

        // 添加图片
        String productId = t.getProductId();
        String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
        List<FileAddReq> fileAddReqs = req.getFileAddReqs();
        if (null != fileAddReqs && !fileAddReqs.isEmpty()) {
            for (FileAddReq fReq : fileAddReqs) {
                ProdFile file = new ProdFile();
                BeanUtils.copyProperties(fReq, file);
                file.setTargetType(targetType);
                file.setTargetId(productId);
                fileManager.addFile(file);
            }
        }
        return ResultVO.success(productId);
    }


    @Override
    public ResultVO<Integer> deleteProdProduct(PrdoProductDeleteReq req) {
        ProductResp product = productManager.getProduct(req.getProductId());
        if(product==null){
            return ResultVO.error("产品已经不存在");
        }
        String uuditState = product.getAuditState();
        //审核中，审核通过不可删除
        if(ProductConst.AuditStateType.AUDITING.getCode().equals(uuditState)
                ||ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(uuditState)){
            return ResultVO.error("审核状态为:"+ProductConst.AuditStateType.getAuditStateTypeByCode(uuditState).getValue()+"的产品不可删除");
        }
        return ResultVO.success(productManager.deleteProdProduct(req.getProductId()));
    }

    @Override
    public ResultVO<Integer> batchDeleteProdProduct(List<PrdoProductDeleteReq> req) {
        int num = 0;
        if(!CollectionUtils.isEmpty(req)){
            for(PrdoProductDeleteReq prdoProductDeleteReq :req){
                ResultVO<Integer> resultVO = this.deleteProdProduct(prdoProductDeleteReq);
                if(resultVO.isSuccess() && resultVO.getResultData()!=null){
                    num += resultVO.getResultData();
                }else if(!resultVO.isSuccess()){
                    return resultVO;
                }
            }
        }
        return ResultVO.success(num);
    }

    @Override
    public ResultVO<Integer> updateProdProductDelete(PrdoProductDeleteReq req) {
        return ResultVO.success(productManager.updateProdProductDelete(req.getProductId()));
    }

    @Override
    @Transactional(isolation= Isolation.DEFAULT,propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public ResultVO<Integer> updateProdProduct(ProductUpdateReq req){
        ResultVO checkResltVO = checkUpdateProdProduct(req);
        if(!checkResltVO.isSuccess()){
            return checkResltVO;
        }
        List<FileAddReq> updateReqs = req.getFileAddReqs();
        Date now = new Date();
        if (null != updateReqs && !updateReqs.isEmpty()) {
            // 先删除原有图片，再添加图片
            String productId = req.getProductId();
            String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
            Integer num = fileManager.deleteFile(req.getProductId(), targetType, null);
            for (FileAddReq updateReq : updateReqs) {
                ProdFile file = new ProdFile();
                BeanUtils.copyProperties(updateReq, file);
                file.setTargetType(targetType);
                file.setTargetId(productId);
                Integer num2 = fileManager.addFile(file);
            }
        }
        req.setUpdateDate(now);
        return ResultVO.success(productManager.updateProdProduct(req));
    }

    @Override
    public ResultVO<Integer> batchUpdateProdProduct(List<ProductUpdateReq> req) {
        int num = 0;
        if(!CollectionUtils.isEmpty(req)){
            for(ProductUpdateReq productUpdateReq:req){
                ResultVO<Integer> resultVO = this.updateProdProduct(productUpdateReq);
                if(resultVO.isSuccess() && resultVO.getResultData()!=null){
                    num += resultVO.getResultData();
                }
            }
        }

        return ResultVO.success(num);
    }

    private ResultVO checkUpdateProdProduct(ProductUpdateReq req){
        String productId = req.getProductId();
        ProductResp productResp = this.productManager.getProduct(productId);
        if(productResp==null){
            ResultVO.error("产品不存在");
        }
        String oldStatue = productResp.getStatus();
        String newStatue = req.getStatus();
        String auditState = productResp.getAuditState();
        //修改成已挂网
        if(ProductConst.StatusType.EFFECTIVE.getCode().equals(newStatue)&&!ProductConst.StatusType.EFFECTIVE.getCode().equals(oldStatue)){
            //审核未通过的不可进行挂网
            if(!ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(auditState)){
                return ResultVO.error("未审核通过的产品不可进行挂网");
            }

        }else if(ProductConst.StatusType.INEFFECTIVE.getCode().equals(newStatue)&&!ProductConst.StatusType.INEFFECTIVE.getCode().equals(oldStatue)){
            //审核未通过的不可进行退市
            if(!ProductConst.AuditStateType.AUDIT_PASS.getCode().equals(auditState)){
                return ResultVO.error("未审核通过的产品不可进行退市");
            }
        }

        return ResultVO.success();
    }

    /**
     * 通用查询
     * @param req
     * @return
     */
    @Override
    public ResultVO<Page<ProductDTO>> selectProduct(ProductGetReq req){
        Page<ProductDTO> page = productManager.selectProduct(req);
        List<ProductDTO> list = page.getRecords();
        if (!CollectionUtils.isEmpty(list)) {
            for (ProductDTO product : list) {
                String productId = product.getProductId();
                // 查询默认图片
                String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
                List<ProdFileDTO> fileList = fileManager.getFile(productId, targetType, null);
                product.setProdFiles(fileList);
                String specName = this.getSpecName(product);
                product.setSpecName(specName);
            }
        }
        log.info("ProductServiceImpl.selectProduct req={}, resp={}", JSON.toJSONString(req), JSON.toJSONString(list));
        return ResultVO.success(page);
    }

    @Override
    public ResultVO<Page<ProductPageResp>> selectPageProductAdmin(ProductsPageReq req) {
        Page<ProductPageResp> page = productManager.selectPageProductAdmin(req);
        List<ProductPageResp> respList = page.getRecords();
        for (ProductPageResp resp : respList){
            MerchantGetReq merchantGetReq = new MerchantGetReq();
            merchantGetReq.setMerchantId(resp.getManufacturerId());
            ResultVO<MerchantDTO> result = merchantService.getMerchant(merchantGetReq);
            MerchantDTO dto = result.getResultData();
            resp.setManufacturerName(null!=dto? dto.getMerchantName():null);
            // 设置规格名
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(resp, productDTO);
            String specName = this.getSpecName(productDTO);
            resp.setSpecName(specName);

            // 查询缩略图图片
            String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
            String nailType = FileConst.ProductSubType.NAIL_SUB.getType();
            List<ProdFileDTO> nailImages =  fileManager.getFile(resp.getProductId(), targetType,nailType);
            if(null == nailImages || nailImages.isEmpty()){
                // // 没有缩略图图片，查询默认图片
                String defaultType = FileConst.ProductSubType.DEFAULT_SUB.getType();
                nailImages =  fileManager.getFile(resp.getProductId(), targetType,defaultType);
            }

            if(null == nailImages || nailImages.isEmpty()){
                continue;
            }
            StringBuilder url = new StringBuilder();
            for (int i = 0;  i< nailImages.size(); i++){
                ProdFileDTO file = nailImages.get(i);
                if (i == (nailImages.size() - 1)){
                    url.append(file.getFileUrl());
                }else {
                    url.append(file.getFileUrl()).append(",");
                }
            }
            resp.setDefaultImages(url.toString());
        }
        page.setRecords(respList);
        log.info("ProductServiceImpl.selectPageProductAdmin req={}, resp={}", JSON.toJSONString(req), JSON.toJSONString(respList));
        return ResultVO.success(page);
    }

    @Override
    public ResultVO<Page<ProductPageResp>> selectPageProductAdminAll(ProductsPageReq req) {
        Page<ProductPageResp> page = productManager.selectPageProductAdminAll(req);
        List<ProductPageResp> respList = page.getRecords();
        for (ProductPageResp resp : respList){
            MerchantGetReq merchantGetReq = new MerchantGetReq();
            merchantGetReq.setMerchantId(resp.getManufacturerId());
            ResultVO<MerchantDTO> result = merchantService.getMerchant(merchantGetReq);
            MerchantDTO dto = result.getResultData();
            resp.setManufacturerName(null!=dto? dto.getMerchantName():null);
            // 设置规格名
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(resp, productDTO);
            String specName = this.getSpecName(productDTO);
            resp.setSpecName(specName);

            // 查询缩略图图片
            String targetType = FileConst.TargetType.PRODUCT_TARGET.getType();
            String nailType = FileConst.ProductSubType.NAIL_SUB.getType();
            List<ProdFileDTO> nailImages =  fileManager.getFile(resp.getProductId(), targetType,nailType);
            if(null == nailImages || nailImages.isEmpty()){
                // // 没有缩略图图片，查询默认图片
                String defaultType = FileConst.ProductSubType.DEFAULT_SUB.getType();
                nailImages =  fileManager.getFile(resp.getProductId(), targetType,defaultType);
            }

            if(null == nailImages || nailImages.isEmpty()){
                continue;
            }
            StringBuilder url = new StringBuilder();
            for (int i = 0;  i< nailImages.size(); i++){
                ProdFileDTO file = nailImages.get(i);
                if (i == (nailImages.size() - 1)){
                    url.append(file.getFileUrl());
                }else {
                    url.append(file.getFileUrl()).append(",");
                }
            }
            resp.setDefaultImages(url.toString());
        }
        page.setRecords(respList);
        log.info("ProductServiceImpl.selectPageProductAdminAll req={}, resp={}", JSON.toJSONString(req), JSON.toJSONString(respList));
        return ResultVO.success(page);
    }

    @Override
    public ResultVO<List<ProductResourceResp>> getProductResource(ProductResourceInstGetReq req){
        List<ProductResourceResp> respList = productManager.getProductResource(req);
        if (CollectionUtils.isEmpty(respList)) {
            return ResultVO.success(respList);
        }

        for (ProductResourceResp resp : respList) {
            // 设置规格名
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(resp, productDTO);
            String specName = this.getSpecName(productDTO);
            resp.setSpecName(specName);
        }
        return ResultVO.success(respList);
    }

    @Override
    public ResultVO<Integer> getProductCountByManufId(ProductCountGetReq req) {
        log.info("ProductServiceImpl.getProductCountByManufId(), 入参manufacturerId ", req.getManufacturerId());
        int count = productManager.getProductCountByManufId(req.getManufacturerId());
        log.info("ProductServiceImpl.getProductCountByManufId(), 出参int ", count);
        return ResultVO.success(count);
    }

    @Override
    public ResultVO<List<ProductDTO>> getProductListByIds(ProductListGetByIdsReq req) {
        List<Product> products = productManager.getProductListByIds(req.getProductIdList());
        List<ProductDTO> productDTOList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(products)) {
            for (Product product : products) {
                ProductDTO productDTO = new ProductDTO();
                BeanUtils.copyProperties(product,productDTO);
                productDTOList.add(productDTO);
            }
        }
        return ResultVO.success(productDTOList);
    }

    @Override
    public ResultVO<QueryProductInfoResqDTO> getProductInfo(QueryProductInfoReqDTO queryProductInfoReqDTO) {
        QueryProductInfoResqDTO queryProductInfoResqDTO = new QueryProductInfoResqDTO();
        ProductPageResp productInfo = productManager.getProductInfo(queryProductInfoReqDTO);
        log.info("ProductServiceImpl.getProductInfo productManager.getProductInfo productInfo={}", JSON.toJSON(productInfo));
        if(productInfo != null){
            BeanUtils.copyProperties(productInfo,queryProductInfoResqDTO);
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(productInfo, productDTO);
            String specName = this.getSpecName(productDTO);
            queryProductInfoResqDTO.setSpecName(specName);
        }
        return ResultVO.success(queryProductInfoResqDTO);
    }

    @Override
    @Transactional(isolation= Isolation.DEFAULT,propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public ResultVO<Boolean> addProductTags(ProductTagsAddReq req) {
        log.info("ProductServiceImpl.addProductTags req={}", req);
        try {
            Tags tags = new Tags();
            tags.setTagName(req.getTagName());
            tags.setTagType(GoodsConst.TagTypeEnum.PRODUCT_TAG.getCode());
            tags.setCreateStaff(req.getCreateStaff());
            tags.setUpdateStaff(req.getUpdateStaff());
            tagsManager.addProdTags(tags);
            TagTelAddReq tagTelAddReq = new TagTelAddReq();
            tagTelAddReq.setProductId(req.getProductId());
            tagTelAddReq.setTagId(tags.getTagId());
            int rspNum = tagTelManager.addTagTel(tagTelAddReq);
            if(rspNum > 0){
                return ResultVO.success(true);
            }
            return ResultVO.success(false);
        } catch (Exception e) {
            log.error("ProductServiceImpl.addProductTags error={}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error(e.getMessage());
        }
    }

    @Override
    public ResultVO updateAuditState(ProductAuditStateUpdateReq req){
        log.info("ProductServiceImpl.updateAuditState productBaseId={},auditState={},updateStaff={}", req.getProductBaseId(), req.getAuditState(),req.getUpdateStaff());
        int result = productManager.updateAuditStateByProductBaseId(req);
        if(result > 0){
            return ResultVO.success();
        }

        return ResultVO.error("审核失败");
    }

    @Override
    public ResultVO updateAttrValue10(ProductAuditStateUpdateReq req) {
        log.info("ProductServiceImpl.updateAttrValue10 productIds={},attrValue10={},updateStaff={}", req.getProductIds(), req.getAttrValue10(),req.getUpdateStaff());
        return ResultVO.success(productManager.updateAttrValue10(req));
    }

    @Override
    public ResultVO<List<ProductResp>> getProductByProductIdsAndBrandIds(ProductAndBrandGetReq req){
        return ResultVO.success(productManager.getProductByProductIdsAndBrandIds(req));
    }

    private String getSpecName(ProductDTO resp){
        StringBuilder specName = new StringBuilder();
        if (StringUtils.isNotBlank(resp.getColor())) {
            specName.append(resp.getColor()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getMemory())) {
            specName.append(resp.getMemory()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getInch())) {
            specName.append(resp.getInch()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue1())) {
            specName.append(resp.getAttrValue1()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue2())) {
            specName.append(resp.getAttrValue2()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue3())) {
            specName.append(resp.getAttrValue3()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue4())) {
            specName.append(resp.getAttrValue4()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue5())) {
            specName.append(resp.getAttrValue5()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue6())) {
            specName.append(resp.getAttrValue6()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue7())) {
            specName.append(resp.getAttrValue7()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue8())) {
            specName.append(resp.getAttrValue8()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue9())) {
            specName.append(resp.getAttrValue9()).append(" ");
        }
        if (StringUtils.isNotBlank(resp.getAttrValue10())) {
            specName.append(resp.getAttrValue10()).append(" ");
        }

        return specName.toString();
    }

    @Override
    public ResultVO<Integer> getDuplicate(ProductGetDuplicateReq req) {
        Integer num=0;
        // 产品编码
        if (StringUtils.isNotBlank(req.getSn()) || StringUtils.isNotBlank(req.getUnitName())) {
            Boolean bothNotNull = StringUtils.isNotBlank(req.getSn()) && StringUtils.isNotBlank(req.getUnitName());
            ProductGetDuplicateReq dto = new ProductGetDuplicateReq();
            dto.setSn(req.getSn());
            dto.setUnitName(req.getUnitName());
            dto.setBothNotNull(bothNotNull);
            num = productManager.getDuplicate(dto);
        }
        return ResultVO.success(num);
    }
}