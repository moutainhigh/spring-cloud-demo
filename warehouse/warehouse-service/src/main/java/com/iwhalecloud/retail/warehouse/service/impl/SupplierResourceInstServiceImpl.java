package com.iwhalecloud.retail.warehouse.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultCodeEnum;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.exception.RetailTipException;
import com.iwhalecloud.retail.goods2b.dto.req.ProductGetByIdReq;
import com.iwhalecloud.retail.goods2b.dto.req.ProductResourceInstGetReq;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductResourceResp;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductResp;
import com.iwhalecloud.retail.goods2b.service.dubbo.ProductService;
import com.iwhalecloud.retail.partner.common.PartnerConst;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.service.MerchantService;
import com.iwhalecloud.retail.system.service.CommonRegionService;
import com.iwhalecloud.retail.warehouse.busiservice.*;
import com.iwhalecloud.retail.warehouse.common.ResourceConst;
import com.iwhalecloud.retail.warehouse.constant.Constant;
import com.iwhalecloud.retail.warehouse.dto.ResouceInstTrackDTO;
import com.iwhalecloud.retail.warehouse.dto.ResouceStoreDTO;
import com.iwhalecloud.retail.warehouse.dto.ResourceInstDTO;
import com.iwhalecloud.retail.warehouse.dto.ResourceReqDetailDTO;
import com.iwhalecloud.retail.warehouse.dto.request.*;
import com.iwhalecloud.retail.warehouse.dto.response.ResourceInstAddResp;
import com.iwhalecloud.retail.warehouse.dto.response.ResourceInstCheckResp;
import com.iwhalecloud.retail.warehouse.dto.response.ResourceInstListPageResp;
import com.iwhalecloud.retail.warehouse.dto.response.ResourceRequestResp;
import com.iwhalecloud.retail.warehouse.manager.*;
import com.iwhalecloud.retail.warehouse.runable.QueryResourceInstRunableTask;
import com.iwhalecloud.retail.warehouse.runable.SupplierRunableTask;
import com.iwhalecloud.retail.warehouse.service.*;
import com.iwhalecloud.retail.workflow.common.WorkFlowConst;
import com.iwhalecloud.retail.workflow.dto.req.ProcessStartReq;
import com.iwhalecloud.retail.workflow.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service("supplierResourceInstService")
public class SupplierResourceInstServiceImpl implements SupplierResourceInstService {

    @Autowired
    private ResourceInstManager resourceInstManager;

    @Autowired
    private ResouceEventManager resouceEventManager;

    @Autowired
    private ResourceChngEvtDetailManager detailManager;

    @Autowired
    private ResourceBatchRecManager batchRecManager;

    @Autowired
    private ResourceInstStoreManager resourceInstStoreManager;

    @Autowired
    private ResouceStoreManager resouceStoreManager;

    @Reference
    private ProductService productService;

    @Reference
    private MerchantService merchantService;

    @Autowired
    private ResourceInstService resourceInstService;

    @Reference
    private TaskService taskService;

    @Reference
    private ResourceRequestService resourceRequestService;

    @Reference
    private ResouceStoreService resouceStoreService;

    @Reference
    private ResourceInstStoreService resourceInstStoreService;

    @Reference
    private MarketingResStoreService marketingResStoreService;

    @Reference
    private CommonRegionService commonRegionService;

    @Reference
    private ResourceRequestService requestService;

    @Autowired
    private ResourceReqDetailManager resourceReqDetailManager;

    @Autowired
    private ResouceInstTrackService resouceInstTrackService;

    @Autowired
    private Constant constant;

    @Autowired
    private ResourceInstLogService resourceInstLogService;

    @Autowired
    private ResourceInstCheckService resourceInstCheckService;

    @Autowired
    private SupplierRunableTask runableTask;

    @Autowired
    private ResourceUploadTempManager resourceUploadTempManager;

    @Autowired
    private QueryResourceInstRunableTask queryResourceInstRunableTask;

    @Autowired
    private ResourceBatchRecService resourceBatchRecService;

    @Autowired
    private ResouceInstTrackDetailManager resouceInstTrackDetailManager;


    @Override
    public ResultVO addResourceInst(ResourceInstAddReq req) {
        log.info("SupplierResourceInstServiceImpl.addResourceInst req={}", JSON.toJSONString(req));
        String merchantId = req.getMerchantId();
        ResultVO<MerchantDTO> merchantResultVO = merchantService.getMerchantById(req.getMerchantId());
        log.info("SupplierResourceInstServiceImpl.addResourceInst merchantService.getMerchantById merchantId={},resp={}", req.getMerchantId(), JSON.toJSONString(merchantResultVO));
        if (!merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO merchantDTO = merchantResultVO.getResultData();
        ResultVO<MerchantDTO> sourceMerchantResultVO = merchantService.getMerchantById(req.getMerchantId());
        log.info("SupplierResourceInstServiceImpl.addResourceInst merchantService.getMerchantById merchantId={},resp={}", req.getSourcemerchantId(), JSON.toJSONString(sourceMerchantResultVO));
        if (!sourceMerchantResultVO.isSuccess() || null == sourceMerchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO sourceMerchantDTO = sourceMerchantResultVO.getResultData();
        // 获取仓库
        StoreGetStoreIdReq storeGetStoreIdReq = new StoreGetStoreIdReq();
        storeGetStoreIdReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        storeGetStoreIdReq.setMerchantId(merchantId);
        String mktResStoreId = null;
        String destStoreId = req.getDestStoreId();
        try{
            mktResStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
            log.info("SupplierResourceInstServiceImpl.addResourceInst resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storeGetStoreIdReq), mktResStoreId);
            if (StringUtils.isBlank(mktResStoreId)) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
        }catch (Exception e){
            return ResultVO.error(constant.getGetRepeatStoreMsg());
        }
        req.setMktResStoreId(destStoreId);
        req.setMerchantType(merchantDTO.getMerchantType());
        req.setDestStoreId(mktResStoreId);
        req.setLanId(merchantDTO.getLanId());
        req.setRegionId(merchantDTO.getCity());
        req.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
        req.setStorageType(ResourceConst.STORAGETYPE.TRANSACTION_WAREHOUSING.getCode());
        req.setCreateStaff(merchantId);
        req.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
        req.setEventType(ResourceConst.EVENTTYPE.PUT_STORAGE.getCode());
        Boolean addNum = resourceInstService.addResourceInst(req);
        if (!addNum) {
            return ResultVO.error(constant.getAddNbrFail());
        }
        ResourceInstUpdateReq resourceInstUpdateReq = new ResourceInstUpdateReq();
        resourceInstUpdateReq.setDestStoreId(destStoreId);
        resourceInstUpdateReq.setMktResInstNbrs(req.getMktResInstNbrs());
        resourceInstUpdateReq.setMktResStoreId(mktResStoreId);
        resourceInstUpdateReq.setEventType(ResourceConst.EVENTTYPE.PUT_STORAGE.getCode());
        resourceInstUpdateReq.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
        resourceInstUpdateReq.setCheckStatusCd(Lists.newArrayList(
                ResourceConst.STATUSCD.AUDITING.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONED.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONING.getCode(),
                ResourceConst.STATUSCD.RESTORAGEING.getCode(),
                ResourceConst.STATUSCD.RESTORAGED.getCode(),
                ResourceConst.STATUSCD.DELETED.getCode()));
        resourceInstUpdateReq.setStatusCd(ResourceConst.STATUSCD.SALED.getCode());
        resourceInstUpdateReq.setTypeId(req.getTypeId());
        resourceInstUpdateReq.setMktResId(req.getMktResId());
        resourceInstUpdateReq.setMerchantId(req.getSourcemerchantId());
        ResultVO updateResourceresultVO = resourceInstService.updateResourceInst(resourceInstUpdateReq);
        if (!updateResourceresultVO.isSuccess()) {
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), updateResourceresultVO.getResultMsg());
        }
        return ResultVO.success();
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO delResourceInst(AdminResourceInstDelReq req) {
        AdminResourceInstDelReq delReq = new AdminResourceInstDelReq();
        BeanUtils.copyProperties(req, delReq);
        // 获取仓库
        StoreGetStoreIdReq storeGetStoreIdReq = new StoreGetStoreIdReq();
        storeGetStoreIdReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        storeGetStoreIdReq.setMerchantId(req.getMerchantId());
        String mktResStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
        log.info("SupplierResourceInstServiceImpl.delResourceInst resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storeGetStoreIdReq), mktResStoreId);
        req.setMktResStoreId(ResourceConst.NULL_STORE_ID);
        req.setDestStoreId(mktResStoreId);
        req.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
        return resourceInstService.updateResourceInstByIds(req);
    }


    @Override
    public ResultVO<Page<ResourceInstListPageResp>> getResourceInstList(ResourceInstListPageReq req) {
        log.info("SupplierResourceInstServiceImpl.getResourceInstList req={}", JSON.toJSONString(req));
        return this.resourceInstService.getResourceInstList(req);
    }

    /**
     * 判断是否需要审核环节：
     * 1.本地市内可调拨，地市管理员审核
     * 2.跨地市调拨，调出和调入双方管理员审核"
     */
    @Override
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO allocateResourceInst(SupplierResourceInstAllocateReq req) {
        //根据仓库查使用对象
        ResultVO<MerchantDTO> sourceMerchantVO = resouceStoreService.getMerchantByStore(req.getMktResStoreId());
        log.info("SupplierResourceInstServiceImpl.allocateResourceInst resouceStoreService.getMerchantByStore req={},resp={}", req.getMktResStoreId(), JSON.toJSONString(sourceMerchantVO));
        MerchantDTO sourceMerchantDTO = sourceMerchantVO.getResultData();
        if (null == sourceMerchantDTO) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        ResultVO<MerchantDTO> destMerchantVO = resouceStoreService.getMerchantByStore(req.getDestStoreId());
        log.info("SupplierResourceInstServiceImpl.allocateResourceInst resouceStoreService.getMerchantByStore req={},resp={}", req.getDestStoreId(), JSON.toJSONString(destMerchantVO));
        MerchantDTO destMerchantDTO = destMerchantVO.getResultData();
        if (null == destMerchantDTO) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        ResourceInstsGetByIdListAndStoreIdReq selectReq = new ResourceInstsGetByIdListAndStoreIdReq();
        selectReq.setMktResInstIdList(req.getMktResInstIds());
        selectReq.setMktResStoreId(req.getMktResStoreId());
        List<ResourceInstDTO> resourceInstDTOList = resourceInstService.selectByIds(selectReq);

        Boolean sameLanId = sourceMerchantDTO.getLanId() != null && destMerchantDTO.getLanId() != null && sourceMerchantDTO.getLanId().equals(destMerchantDTO.getLanId());
        // step1 判断是否跨地市跨地市
        String successMessage = constant.getAllocateAudit();
        String reqCode = resourceInstManager.getPrimaryKey();

        String processId = WorkFlowConst.PROCESS_ID.PROCESS_07.getTypeCode();
        String taskSubType = WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_1010.getTaskSubType();
        if (!sameLanId) {
            processId = WorkFlowConst.PROCESS_ID.PROCESS_12.getTypeCode();
            taskSubType = WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_2040.getTaskSubType() ;
        }
        // step2 新增申请单
        List<ResourceRequestAddReq.ResourceRequestInst> resourceRequestInsts = new ArrayList<>();
        for (ResourceInstDTO resourceInstDTO : resourceInstDTOList) {
            ResourceRequestAddReq.ResourceRequestInst resourceRequestInst = new ResourceRequestAddReq.ResourceRequestInst();
            BeanUtils.copyProperties(resourceInstDTO, resourceRequestInst);
            resourceRequestInsts.add(resourceRequestInst);
        }
        ResourceRequestAddReq resourceRequestAddReq = new ResourceRequestAddReq();
        resourceRequestAddReq.setReqName(constant.getAllocateRequestItem());
        resourceRequestAddReq.setReqType(ResourceConst.REQTYPE.ALLOCATE_APPLYFOR.getCode());
        resourceRequestAddReq.setMktResStoreId(req.getMktResStoreId());
        resourceRequestAddReq.setCreateStaff(req.getCreateStaff());
        resourceRequestAddReq.setDestStoreId(req.getDestStoreId());
        resourceRequestAddReq.setStatusCd(ResourceConst.MKTRESSTATE.PROCESSING.getCode());
        resourceRequestAddReq.setChngType(ResourceConst.PUT_IN_STOAGE);
        resourceRequestAddReq.setInstList(resourceRequestInsts);
        resourceRequestAddReq.setLanId(sourceMerchantDTO.getLanId());
        resourceRequestAddReq.setRegionId(sourceMerchantDTO.getCity());
        resourceRequestAddReq.setReqCode(reqCode);
        resourceRequestAddReq.setDetailStatusCd(ResourceConst.DetailStatusCd.STATUS_CD_1009.getCode());
        ResultVO<String> resultVOInsertResReq = resourceRequestService.insertResourceRequest(resourceRequestAddReq);
        log.info("SupplierResourceInstServiceImpl.allocateResourceInst resourceRequestService.insertResourceRequest req={},resp={}", JSON.toJSONString(resourceRequestAddReq), JSON.toJSONString(resultVOInsertResReq));
        if (resultVOInsertResReq == null || resultVOInsertResReq.getResultData() == null) {
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), constant.getAddRequestItemError());
        }

        // step3 发起流程到目标商家，由目标商家决定是否接受
        String createStaff = req.getCreateStaff();
        ProcessStartReq processStartDTO = new ProcessStartReq();
        processStartDTO.setTitle(constant.getAllocateWorkFlow());
        processStartDTO.setApplyUserId(createStaff);
        processStartDTO.setApplyUserName(sourceMerchantDTO.getMerchantName());
        processStartDTO.setProcessId(processId);
        processStartDTO.setFormId(resultVOInsertResReq.getResultData());
        processStartDTO.setTaskSubType(taskSubType);
        processStartDTO.setExtends1(sourceMerchantDTO.getCityName());
        processStartDTO.setParamsType(WorkFlowConst.TASK_PARAMS_TYPE.JSON_PARAMS.getCode());
        Map map=new HashMap();
        String secondStepFlag = "1";
        map.put(sourceMerchantDTO.getLanId(), sourceMerchantDTO.getLanId());
        map.put(destMerchantDTO.getLanId() + secondStepFlag, destMerchantDTO.getLanId() + secondStepFlag);
        processStartDTO.setParamsValue(JSON.toJSONString(map));
        ResultVO startResultVO = taskService.startProcess(processStartDTO);
        log.info("SupplierResourceInstServiceImpl.allocateResourceInst taskService.startProcess req={}, resp={}", JSON.toJSONString(processStartDTO), JSON.toJSONString(startResultVO));
        if (!startResultVO.getResultCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), constant.getStartWorkFlowError());
        }
        // step4 修改源仓库串码为调拨中
        AdminResourceInstDelReq updateReq = new AdminResourceInstDelReq();
        BeanUtils.copyProperties(req, updateReq);
        // 设置状态校验条件
        List<String> checkStatusCd = Lists.newArrayList(
                ResourceConst.STATUSCD.DELETED.getCode(),
                ResourceConst.STATUSCD.AUDITING.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONED.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONING.getCode(),
                ResourceConst.STATUSCD.RESTORAGEING.getCode(),
                ResourceConst.STATUSCD.RESTORAGED.getCode(),
                ResourceConst.STATUSCD.SALED.getCode());
        updateReq.setCheckStatusCd(checkStatusCd);
        updateReq.setStatusCd(ResourceConst.STATUSCD.ALLOCATIONING.getCode());
        updateReq.setUpdateStaff(createStaff);
        updateReq.setEventType(ResourceConst.EVENTTYPE.ALLOT.getCode());
        updateReq.setEventStatusCd(ResourceConst.EVENTSTATE.PROCESSING.getCode());
        updateReq.setObjType(ResourceConst.EVENT_OBJTYPE.PUT_STORAGE.getCode());
        updateReq.setObjId(resultVOInsertResReq.getResultData());
        updateReq.setMktResStoreId(req.getDestStoreId());
        updateReq.setDestStoreId(req.getMktResStoreId());
        updateReq.setMktResInstIdList(req.getMktResInstIds());
        updateReq.setOrderId(resultVOInsertResReq.getResultData());
        ResultVO updateResultVO = resourceInstService.updateResourceInstByIds(updateReq);
        log.info("SupplierResourceInstServiceImpl.allocateResourceInst resourceInstService.updateResourceInstByIds req={},resp={}", JSON.toJSONString(updateReq), JSON.toJSONString(updateResultVO));
        if (!updateResultVO.isSuccess()) {
            throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), constant.getUpdateNbrFail());
        }
        return ResultVO.success(successMessage+reqCode);
    }


    @Override
    public ResultVO deliveryOutResourceInst(DeliveryResourceInstReq req) {
        StoreGetStoreIdReq storePageReq = new StoreGetStoreIdReq();
        storePageReq.setMerchantId(req.getBuyerMerchantId());
        storePageReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        String sourceStoreId = null;
        String destStoreId = null;
        try{
            sourceStoreId = resouceStoreService.getStoreId(storePageReq);
            if (StringUtils.isEmpty(sourceStoreId)) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
            log.info("SupplierResourceInstServiceImpl.deliveryOutResourceInst resouceStoreService.getStoreId req={},storeId={}", JSON.toJSONString(storePageReq), sourceStoreId);
            storePageReq.setMerchantId(req.getSellerMerchantId());
            destStoreId = resouceStoreService.getStoreId(storePageReq);
            if (StringUtils.isEmpty(destStoreId)) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
            log.info("SupplierResourceInstServiceImpl.deliveryOutResourceInst resouceStoreService.getStoreId req={},destStoreId={}", JSON.toJSONString(storePageReq), destStoreId);
        }catch (Exception e){
            return ResultVO.error(constant.getGetRepeatStoreMsg());
        }
        ResultVO<MerchantDTO> merchantVO = merchantService.getMerchantById(req.getBuyerMerchantId());
        if (!merchantVO.isSuccess() || null == merchantVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO merchantDTO = merchantVO.getResultData();
        log.info("SupplierResourceInstServiceImpl.deliveryOutResourceInst resouceStoreService.getMerchantById req={},destStoreId={}", req.getBuyerMerchantId(), JSON.toJSONString(merchantVO));
        for (DeliveryResourceInstItem item : req.getDeliveryResourceInstItemList()) {
            ResourceInstUpdateReq resourceInstUpdateReq = new ResourceInstUpdateReq();
            resourceInstUpdateReq.setMktResInstNbrs(item.getMktResInstNbrs());
            resourceInstUpdateReq.setCheckStatusCd(Lists.newArrayList(ResourceConst.STATUSCD.DELETED.getCode()));
            resourceInstUpdateReq.setMktResStoreId(sourceStoreId);
            resourceInstUpdateReq.setDestStoreId(destStoreId);
            resourceInstUpdateReq.setStatusCd(ResourceConst.STATUSCD.SALED.getCode());
            resourceInstUpdateReq.setEventType(ResourceConst.EVENTTYPE.SALE_TO_ORDER.getCode());
            resourceInstUpdateReq.setObjType(ResourceConst.EVENT_OBJTYPE.ALLOT.getCode());
            resourceInstUpdateReq.setObjId(req.getOrderId());
            if (PartnerConst.MerchantTypeEnum.PARTNER.getType().equals(merchantDTO.getMerchantType())) {
                resourceInstUpdateReq.setMerchantName(merchantDTO.getMerchantName());
                resourceInstUpdateReq.setMerchantCode(merchantDTO.getMerchantCode());
            }
            resourceInstUpdateReq.setMktResId(item.getProductId());
            resourceInstUpdateReq.setOrderId(req.getOrderId());
            resourceInstUpdateReq.setCreateTime(new Date());
            resourceInstUpdateReq.setMerchantId(req.getSellerMerchantId());
            ResultVO delRS = resourceInstService.updateResourceInstForTransaction(resourceInstUpdateReq);
            log.info("SupplierResourceInstServiceImpl.deliveryOutResourceInst resourceInstService.delResourceInst req={},resp={}", JSON.toJSONString(resourceInstUpdateReq), JSON.toJSONString(delRS));
            if (delRS == null || !delRS.isSuccess()) {
                return ResultVO.error(delRS.getResultMsg());
            }
        }
        // 下单是增加了在途数量，发货时减去
        if (null != req.getUpdateStockReq()) {
            resourceInstStoreService.updateStock(req.getUpdateStockReq());
        }
        return ResultVO.success(true);
    }

    @Override
    public ResultVO deliveryInResourceInst(DeliveryResourceInstReq req) {
        ResultVO<MerchantDTO> merchantResultVO = merchantService.getMerchantById(req.getSellerMerchantId());
        if (!merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO merchantDTO = merchantResultVO.getResultData();
        log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst merchantService.getMerchantById req={},resp={}", JSON.toJSONString(req), JSON.toJSONString(merchantResultVO));

        ResultVO<MerchantDTO> buyerResultVO = merchantService.getMerchantById(req.getBuyerMerchantId());
        if (!buyerResultVO.isSuccess() || null == buyerResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO buyer = buyerResultVO.getResultData();
        log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst merchantService.getMerchantById req={},resp={}", JSON.toJSONString(req), JSON.toJSONString(merchantResultVO));
        // 源仓库
        StoreGetStoreIdReq storeGetStoreIdReq = new StoreGetStoreIdReq();
        storeGetStoreIdReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        storeGetStoreIdReq.setMerchantId(req.getSellerMerchantId());
        String mktResStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
        log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storeGetStoreIdReq), mktResStoreId);
        // 目标仓库
        storeGetStoreIdReq.setMerchantId(req.getBuyerMerchantId());
        String destStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
        log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storeGetStoreIdReq), destStoreId);

        for (DeliveryResourceInstItem item : req.getDeliveryResourceInstItemList()) {
            if (StringUtils.isEmpty(item.getProductId())) {
                return ResultVO.error(constant.getNoProductIdMsg());
            }
            ResourceInstAddReq resourceInstAddReq = new ResourceInstAddReq();
            resourceInstAddReq.setMktResInstNbrs(item.getMktResInstNbrs());
            resourceInstAddReq.setCreateStaff(req.getBuyerMerchantId());
            resourceInstAddReq.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
            resourceInstAddReq.setEventType(ResourceConst.EVENTTYPE.SALE_TO_ORDER.getCode());
            resourceInstAddReq.setSourceType(ResourceConst.SOURCE_TYPE.MERCHANT.getCode());
            resourceInstAddReq.setStorageType(ResourceConst.STORAGETYPE.TRANSACTION_WAREHOUSING.getCode());
            resourceInstAddReq.setMktResInstType(ResourceConst.MKTResInstType.TRANSACTION.getCode());
            resourceInstAddReq.setObjType(ResourceConst.EVENT_OBJTYPE.ALLOT.getCode());
            resourceInstAddReq.setObjId(req.getOrderId());
            resourceInstAddReq.setMktResId(item.getProductId());
            resourceInstAddReq.setOrderId(req.getOrderId());
            resourceInstAddReq.setCreateTime(new Date());
            resourceInstAddReq.setMktResStoreId(mktResStoreId);
            resourceInstAddReq.setDestStoreId(destStoreId);
            resourceInstAddReq.setSupplierName(merchantDTO.getMerchantName());
            resourceInstAddReq.setSupplierCode(merchantDTO.getMerchantCode());
            resourceInstAddReq.setMerchantId(req.getBuyerMerchantId());
            resourceInstAddReq.setLanId(buyer.getLanId());
            resourceInstAddReq.setRegionId(buyer.getCity());
            resourceInstAddReq.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
            resourceInstAddReq.setMerchantType(buyer.getMerchantType());
            ProductGetByIdReq productReq = new ProductGetByIdReq();
            productReq.setProductId(item.getProductId());
            ResultVO<ProductResp> productRespResultVO = productService.getProductInfo(productReq);
            log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst productService.getProductInfo productId={},resp={}", item.getProductId(), JSON.toJSONString(productRespResultVO));
            if (productRespResultVO.isSuccess() && null != productRespResultVO.getResultData()) {
                resourceInstAddReq.setTypeId(productRespResultVO.getResultData().getTypeId());
            }
            ResultVO addRS = this.resourceInstService.addResourceInstForTransaction(resourceInstAddReq);
            log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst resourceInstService.addResourceInst req={},resp={}", JSON.toJSONString(resourceInstAddReq), JSON.toJSONString(addRS));
            if (addRS == null || !addRS.isSuccess()) {
                return ResultVO.error(addRS.getResultMsg());
            }
        }
        return ResultVO.success(true);
    }

    @Override
    public ResultVO backDeliveryOutResourceInst(DeliveryResourceInstReq req) {
        ResultVO<MerchantDTO> merchantResultVO = merchantService.getMerchantById(req.getBuyerMerchantId());
        log.info("SupplierResourceInstServiceImpl.backDeliveryOutResourceInst merchantService.getMerchantById req={},resp={}", JSON.toJSONString(req), JSON.toJSONString(merchantResultVO));
        if (null == merchantResultVO || !merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        // 源仓库(退库 卖家仓库)
        StoreGetStoreIdReq storePageReq = new StoreGetStoreIdReq();
        storePageReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        storePageReq.setMerchantId(req.getSellerMerchantId());
        String mktResStoreId = resouceStoreService.getStoreId(storePageReq);
        log.info("SupplierResourceInstServiceImpl.deliveryInResourceInst resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storePageReq), mktResStoreId);
        // 目标仓库(退库 买家仓库)
        storePageReq.setMerchantId(req.getBuyerMerchantId());
        String destStoreId = resouceStoreService.getStoreId(storePageReq);
        log.info("SupplierResourceInstServiceImpl.backDeliveryOutResourceInst resourceInstService  destStoreId={}",destStoreId);
        for (DeliveryResourceInstItem item : req.getDeliveryResourceInstItemList()) {

            ResourceInstUpdateReq resourceInstUpdateReq = new ResourceInstUpdateReq();
            resourceInstUpdateReq.setMktResInstNbrs(item.getMktResInstNbrs());
            // 设置状态校验条件
            List<String> checkStatusCd = Lists.newArrayList(ResourceConst.STATUSCD.DELETED.getCode());
            resourceInstUpdateReq.setCheckStatusCd(checkStatusCd);
            resourceInstUpdateReq.setStatusCd(ResourceConst.STATUSCD.DELETED.getCode());
            resourceInstUpdateReq.setEventType(ResourceConst.EVENTTYPE.BUY_BACK.getCode());
            resourceInstUpdateReq.setObjType(ResourceConst.EVENT_OBJTYPE.ALLOT.getCode());
            resourceInstUpdateReq.setObjId(req.getOrderId());
            resourceInstUpdateReq.setDestStoreId(destStoreId);
            resourceInstUpdateReq.setUpdateStaff(merchantResultVO.getResultData().getUserId());
            resourceInstUpdateReq.setMerchantId(req.getBuyerMerchantId());
            resourceInstUpdateReq.setMktResId(item.getProductId());
            resourceInstUpdateReq.setOrderId(req.getOrderId());
            resourceInstUpdateReq.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
            resourceInstUpdateReq.setMktResStoreId(mktResStoreId);
            ResultVO updateResultVO = resourceInstService.updateResourceInstForTransaction(resourceInstUpdateReq);
            log.info("SupplierResourceInstServiceImpl.backDeliveryOutResourceInst resourceInstService.updateResourceInstForTransaction req={},resp={}", JSON.toJSONString(resourceInstUpdateReq), JSON.toJSONString(updateResultVO));
            if (updateResultVO == null || !updateResultVO.isSuccess()) {
                return ResultVO.error(updateResultVO.getResultMsg());
            }
        }
        return ResultVO.success(true);
    }

    @Override
    public ResultVO backDeliveryInResourceInst(DeliveryResourceInstReq req) {
        ResultVO<MerchantDTO> merchantResultVO = merchantService.getMerchantById(req.getSellerMerchantId());
        log.info("SupplierResourceInstServiceImpl.backDeliveryInResourceInst merchantService.getMerchantById req={},resp={}", JSON.toJSONString(req), JSON.toJSONString(merchantResultVO));
        if (null == merchantResultVO || !merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO merchantDTO = merchantResultVO.getResultData();
        // 仓库
        StoreGetStoreIdReq storeGetStoreIdReq = new StoreGetStoreIdReq();
        storeGetStoreIdReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
        storeGetStoreIdReq.setMerchantId(req.getSellerMerchantId());
        String destStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
        log.info("SupplierResourceInstServiceImpl.backDeliveryOutResourceInst resourceInstService req={} destStoreId={}", JSON.toJSONString(storeGetStoreIdReq), destStoreId);
        storeGetStoreIdReq.setMerchantId(req.getBuyerMerchantId());
        String sourceStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
        log.info("SupplierResourceInstServiceImpl.backDeliveryOutResourceInst resourceInstService req={} storeId={}",JSON.toJSONString(storeGetStoreIdReq), sourceStoreId);
        for (DeliveryResourceInstItem item : req.getDeliveryResourceInstItemList()) {
            ResourceInstsGetReq resourceInstsGetReq = new ResourceInstsGetReq();
            resourceInstsGetReq.setMktResInstNbrs(item.getMktResInstNbrs());
            resourceInstsGetReq.setMktResStoreId(destStoreId);
            resourceInstsGetReq.setStatusCd(ResourceConst.STATUSCD.SALED.getCode());
            List<ResourceInstDTO> resourceInstDTOList = resourceInstManager.getResourceInsts(resourceInstsGetReq);
            log.info("SupplierResourceInstServiceImpl.backDeliveryInResourceInst resourceInstManager.getResourceInsts req={},resp={}", JSON.toJSONString(resourceInstsGetReq), JSON.toJSONString(resourceInstDTOList));
            if (CollectionUtils.isEmpty(resourceInstDTOList)) {
                continue;
            }
            AdminResourceInstDelReq adminResourceInstDelReq = new AdminResourceInstDelReq();
            adminResourceInstDelReq.setMktResInstIdList(resourceInstDTOList.stream().map(ResourceInstDTO::getMktResInstId).collect(Collectors.toList()));
            // 设置状态校验条件
            List<String> checkStatusCd = Lists.newArrayList(
                    ResourceConst.STATUSCD.DELETED.getCode(),
                    ResourceConst.STATUSCD.AUDITING.getCode(),
                    ResourceConst.STATUSCD.ALLOCATIONED.getCode(),
                    ResourceConst.STATUSCD.ALLOCATIONING.getCode());
            adminResourceInstDelReq.setCheckStatusCd(checkStatusCd);
            adminResourceInstDelReq.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
            adminResourceInstDelReq.setEventType(ResourceConst.EVENTTYPE.BUY_BACK.getCode());
            adminResourceInstDelReq.setObjType(ResourceConst.EVENT_OBJTYPE.ALLOT.getCode());
            adminResourceInstDelReq.setObjId(req.getOrderId());
            adminResourceInstDelReq.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
            //目标仓库
            adminResourceInstDelReq.setDestStoreId(destStoreId);
            adminResourceInstDelReq.setMktResStoreId(sourceStoreId);
            adminResourceInstDelReq.setMerchantId(merchantDTO.getMerchantId());
            adminResourceInstDelReq.setOrderId(req.getOrderId());
            ResultVO updateResultVO = resourceInstService.updateResourceInstByIdsForTransaction(adminResourceInstDelReq);
            log.info("SupplierResourceInstServiceImpl.backDeliveryInResourceInst resourceInstService.updateResultVO req={},resp={}", JSON.toJSONString(adminResourceInstDelReq), JSON.toJSONString(updateResultVO));
            if (updateResultVO == null || !updateResultVO.isSuccess()) {
                return ResultVO.error(updateResultVO.getResultMsg());
            }
        }
        return ResultVO.success(true);
    }

    @Override
    public ResultVO<List<ResourceInstListPageResp>> getBatch(ResourceInstBatchReq req) {
        List<ResourceInstListPageResp> list = resourceInstManager.getBatch(req);
        log.info("SupplierResourceInstServiceImpl.getBatch req={}", JSON.toJSONString(req));
        // 添加产品信息
        for (ResourceInstListPageResp resp : list) {
            String productId = resp.getMktResId();
            ProductResourceInstGetReq queryReq = new ProductResourceInstGetReq();
            queryReq.setProductId(productId);
            ResultVO<List<ProductResourceResp>> resultVO = productService.getProductResource(queryReq);
            log.info("SupplierResourceInstServiceImpl.getBatch productService.getProductResource req={},resp={}", JSON.toJSONString(queryReq), JSON.toJSONString(resultVO));
            List<ProductResourceResp> prodList = resultVO.getResultData();
            if (null != prodList && !prodList.isEmpty()) {
                ProductResourceResp prodResp = prodList.get(0);
                BeanUtils.copyProperties(prodResp, resp);
            }
        }
        return ResultVO.success(list);
    }

    @Override
    public ResultVO<Boolean> updateInstState(ResourceInstUpdateReq req) {
        log.info("SupplierResourceInstServiceImpl.updateInstState req={}", JSON.toJSONString(req));
        req.setCheckStatusCd(Lists.newArrayList(
                ResourceConst.STATUSCD.DELETED.getCode(),
                ResourceConst.STATUSCD.AUDITING.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONED.getCode(),
                ResourceConst.STATUSCD.ALLOCATIONING.getCode(),
                ResourceConst.STATUSCD.DELETED.getCode()));
        return this.resourceInstService.updateInstState(req);
    }

    @Override
    public ResultVO confirmReciveNbr(ConfirmReciveNbrReq req) {
        // step1 申请单状态改为完成
        String resReqId = req.getResReqId();
        ResourceRequestItemQueryReq resourceRequestReq = new ResourceRequestItemQueryReq();
        resourceRequestReq.setMktResReqId(resReqId);
        ResultVO<ResourceRequestResp> resourceRequestRespVO = requestService.queryResourceRequest(resourceRequestReq);
        String statusCdReviewed = ResourceConst.MKTRESSTATE.REVIEWED.getCode();
        Boolean statusNotRight = !resourceRequestRespVO.isSuccess() || resourceRequestRespVO.getResultData() == null || !statusCdReviewed.equals(resourceRequestRespVO.getResultData().getStatusCd());
        if (statusNotRight) {
            return ResultVO.error(ResultCodeEnum.ERROR.getCode(), constant.getRequestItemInvalid());
        }
        ResourceRequestResp resourceRequestResp = resourceRequestRespVO.getResultData();
        ResourceRequestUpdateReq reqUpdate = new ResourceRequestUpdateReq();
        reqUpdate.setMktResReqId(resReqId);
        reqUpdate.setStatusCd(ResourceConst.MKTRESSTATE.DONE.getCode());
        ResultVO<Boolean> updateResourceRequestStateVO = requestService.updateResourceRequestState(reqUpdate);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr requestService.updateResourceRequestState req={},resp={}", JSON.toJSONString(reqUpdate),JSON.toJSONString(updateResourceRequestStateVO));

        // step1 找到串码明细
        ResourceReqDetailQueryReq queryReq = new ResourceReqDetailQueryReq();
        queryReq.setMktResReqId(req.getResReqId());
        List<ResourceReqDetailDTO> list = resourceReqDetailManager.listDetail(queryReq);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resourceReqDetailManager.listDetail req={},resp={}", JSON.toJSONString(queryReq),JSON.toJSONString(list));
        if (null == list || list.isEmpty()) {
            return ResultVO.error(constant.getCannotGetRequestItemMsg());
        }
        ResourceReqDetailDTO detailDTO = list.get(0);
        ResourceReqDetailUpdateReq detailUpdateReq = new ResourceReqDetailUpdateReq();
        detailUpdateReq.setMktResReqItemIdList(Lists.newArrayList(detailDTO.getMktResReqItemId()));
        detailUpdateReq.setStatusCd(ResourceConst.DetailStatusCd.STATUS_CD_1003.getCode());
        detailUpdateReq.setUpdateStaff(req.getUpdateStaff());
        Integer detailNum = resourceReqDetailManager.updateResourceReqDetailStatusCd(detailUpdateReq);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resourceReqDetailManager.updateResourceReqDetailStatusCd detailUpdateReq={}, resp={}", JSON.toJSONString(detailUpdateReq), detailNum);

        ResultVO<MerchantDTO> merchantDTOResultVO = resouceStoreService.getMerchantByStore(detailDTO.getDestStoreId());
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resouceStoreService.getMerchantByStore req={},resp={}", list.get(0).getDestStoreId(),JSON.toJSONString(merchantDTOResultVO));
        if (!merchantDTOResultVO.isSuccess() || null == merchantDTOResultVO.getResultData() ) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO destMerchant = merchantDTOResultVO.getResultData();
        List<String> mktResInstIds = list.stream().map(ResourceReqDetailDTO::getMktResInstId).collect(Collectors.toList());

        // step2 把状态改为已调拨
        AdminResourceInstDelReq updateReq = new AdminResourceInstDelReq();
        List<String> checkStatusCd = Lists.newArrayList(
                ResourceConst.STATUSCD.AUDITING.getCode(),
                ResourceConst.STATUSCD.AVAILABLE.getCode(),
                ResourceConst.STATUSCD.RESTORAGEING.getCode(),
                ResourceConst.STATUSCD.RESTORAGED.getCode(),
                ResourceConst.STATUSCD.SALED.getCode(),
                ResourceConst.STATUSCD.DELETED.getCode());
        updateReq.setMktResInstIdList(mktResInstIds);
        updateReq.setCheckStatusCd(checkStatusCd);
        updateReq.setStatusCd(ResourceConst.STATUSCD.ALLOCATIONED.getCode());
        updateReq.setEventType(ResourceConst.EVENTTYPE.NO_RECORD.getCode());
        updateReq.setObjType(ResourceConst.EVENT_OBJTYPE.PUT_STORAGE.getCode());
        updateReq.setObjId(resReqId);
        updateReq.setUpdateStaff(req.getUpdateStaff());
        updateReq.setDestStoreId(resourceRequestResp.getMktResStoreId());
        updateReq.setMktResStoreId(resourceRequestResp.getDestStoreId());
        updateReq.setMerchantId(resourceRequestResp.getMerchantId());
        ResultVO<List<String>> updateVO = resourceInstService.updateResourceInstByIds(updateReq);
        log.info("SupplierResourceInstServiceImpl.confirmReciveNbr resourceInstService.updateResourceInstByIds req={}, resp={}", JSON.toJSONString(updateReq), JSON.toJSONString(updateVO));

        // step3 领用方入库
        ResourceInstsGetByIdListAndStoreIdReq selectReq = new ResourceInstsGetByIdListAndStoreIdReq();
        selectReq.setMktResInstIdList(mktResInstIds);
        selectReq.setMktResStoreId(resourceRequestResp.getMktResStoreId());
        List<ResourceInstDTO> insts = resourceInstManager.selectByIds(selectReq);
        log.info("SupplierResourceInstServiceImpl.confirmReciveNbr resourceInstManager.selectByIds req={}, resp={}", JSON.toJSONString(selectReq), JSON.toJSONString(insts));
        // 按产品维度组装数据
        Map<String, List<ResourceInstDTO>> map = insts.stream().collect(Collectors.groupingBy(t -> t.getMktResId()));
        ResourceInstPutInReq instPutInReq = new ResourceInstPutInReq();
        instPutInReq.setInsts(map);
        instPutInReq.setCreateStaff(req.getUpdateStaff());
        instPutInReq.setStorageType(ResourceConst.STORAGETYPE.ALLOCATION_AND_WAREHOUSING.getCode());
        instPutInReq.setEventType(ResourceConst.EVENTTYPE.ALLOT.getCode());
        instPutInReq.setMktResStoreId(resourceRequestResp.getMktResStoreId());
        instPutInReq.setDestStoreId(resourceRequestResp.getDestStoreId());
        instPutInReq.setMerchantId(req.getMerchantId());
        instPutInReq.setEventStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
        instPutInReq.setObjType(ResourceConst.EVENT_OBJTYPE.PUT_STORAGE.getCode());
        instPutInReq.setObjId(resReqId);
        instPutInReq.setLanId(destMerchant.getLanId());
        instPutInReq.setRegionId(destMerchant.getCity());
        ResultVO resultResourceInstPutIn = resourceInstService.resourceInstPutIn(instPutInReq);
        log.info("SupplierResourceInstServiceImpl.confirmReciveNbr resourceInstService.resourceInstPutIn req={}, resp={}", JSON.toJSONString(instPutInReq), JSON.toJSONString(resultResourceInstPutIn));
        return ResultVO.success(instPutInReq.getUnUse());
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO confirmRefuseNbr(ConfirmReciveNbrReq req) {
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr req={}, resp={}", JSON.toJSONString(req));
        // step1 申请单修改状态
        ResourceRequestUpdateReq reqUpdate = new ResourceRequestUpdateReq();
        reqUpdate.setMktResReqId(req.getResReqId());
        reqUpdate.setStatusCd(ResourceConst.MKTRESSTATE.CANCEL.getCode());
        requestService.updateResourceRequestState(reqUpdate);
        //step2 根据申请单表保存的源仓库和申请单明细找到对应的串码
        ResourceReqDetailQueryReq queryReq = new ResourceReqDetailQueryReq();
        queryReq.setMktResReqId(req.getResReqId());
        List<ResourceReqDetailDTO> list = resourceReqDetailManager.listDetail(queryReq);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resourceReqDetailManager.listDetail req={}, resp={}", JSON.toJSONString(queryReq), JSON.toJSONString(list));
        if (CollectionUtils.isEmpty(list)) {
            return ResultVO.error(constant.getCannotGetRequestItemMsg());
        }
        ResourceReqDetailDTO detailDTO = list.get(0);
        ResourceReqDetailUpdateReq detailUpdateReq = new ResourceReqDetailUpdateReq();
        detailUpdateReq.setMktResReqItemIdList(Lists.newArrayList(detailDTO.getMktResReqItemId()));
        detailUpdateReq.setStatusCd(ResourceConst.DetailStatusCd.STATUS_CD_1004.getCode());
        detailUpdateReq.setUpdateStaff(req.getUpdateStaff());
        Integer detailNum = resourceReqDetailManager.updateResourceReqDetailStatusCd(detailUpdateReq);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resourceReqDetailManager.updateResourceReqDetailStatusCd detailUpdateReq={}, resp={}", JSON.toJSONString(detailUpdateReq), detailNum);

        ResultVO<MerchantDTO> merchantResultVO = resouceStoreService.getMerchantByStore(list.get(0).getMktResStoreId());
        if (!merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        List<String> mktResInstIds = list.stream().map(ResourceReqDetailDTO::getMktResInstId).collect(Collectors.toList());
        // step3 修改源仓库串码为可用
        AdminResourceInstDelReq updateReq = new AdminResourceInstDelReq();
        List<String> checkStatusCd = new ArrayList<String>(7);
        checkStatusCd.add(ResourceConst.STATUSCD.AUDITING.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.AVAILABLE.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.ALLOCATIONED.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.RESTORAGEING.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.RESTORAGED.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.SALED.getCode());
        checkStatusCd.add(ResourceConst.STATUSCD.DELETED.getCode());
        updateReq.setCheckStatusCd(checkStatusCd);
        updateReq.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
        updateReq.setMktResInstIdList(mktResInstIds);
        updateReq.setEventType(ResourceConst.EVENTTYPE.ALLOT.getCode());
        updateReq.setObjType(ResourceConst.EVENT_OBJTYPE.PUT_STORAGE.getCode());
        updateReq.setObjId(req.getResReqId());
        updateReq.setMerchantId(merchantResultVO.getResultData().getMerchantId());
        updateReq.setMktResStoreId(detailDTO.getDestStoreId());
        updateReq.setDestStoreId(detailDTO.getMktResStoreId());
        updateReq.setEventStatusCd(ResourceConst.EVENTSTATE.CANCEL.getCode());
        ResultVO resp = resourceInstService.updateResourceInstByIds(updateReq);
        log.info("SupplierResourceInstServiceImpl.confirmRefuseNbr resourceInstService.resourceInstPutIn req={}, resp={}", JSON.toJSONString(updateReq), JSON.toJSONString(resp));
        return ResultVO.success();
    }

    @Override
    public ResultVO validResourceInst(DeliveryValidResourceInstReq req) {
        try{
            ResouceStoreDTO storeDTO = resouceStoreManager.getStore(req.getMerchantId(), ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
            log.info("SupplierResourceInstServiceImpl.validResourceInst resouceStoreManager.getStore req={}, resp={}", JSON.toJSONString(req), JSON.toJSONString(storeDTO));
            if (null == storeDTO) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
            req.setMktResStoreId(storeDTO.getMktResStoreId());
            return ResultVO.success(resourceInstManager.validResourceInst(req));
        }catch (Exception e){
            return ResultVO.error(constant.getGetRepeatStoreMsg());
        }
    }

    @Override
    public ResultVO validNbr(ResourceInstValidReq req){
        // 集采前端会传入库仓库id,其他类型根据当前登陆用户去获取仓库
        String mktResStoreId = req.getMktResStoreId();
        if (!ResourceConst.MKTResInstType.NONTRANSACTION.getCode().equals(req.getMktResInstType())) {
            StoreGetStoreIdReq storeGetStoreIdReq = new StoreGetStoreIdReq();
            storeGetStoreIdReq.setStoreSubType(ResourceConst.STORE_SUB_TYPE.STORE_TYPE_TERMINAL.getCode());
            storeGetStoreIdReq.setMerchantId(req.getMerchantId());
            mktResStoreId = resouceStoreService.getStoreId(storeGetStoreIdReq);
            log.info("MerchantResourceInstServiceImpl.validNbr resouceStoreService.getStoreId req={},resp={}", JSON.toJSONString(storeGetStoreIdReq), mktResStoreId);
            if (StringUtils.isBlank(mktResStoreId)) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
        }
        ResourceInstValidReq resourceInstValidReq = new ResourceInstValidReq();
        BeanUtils.copyProperties(req, resourceInstValidReq);
        resourceInstValidReq.setMktResStoreId(mktResStoreId);
        String batchId = runableTask.exceutorValidForSupplier(resourceInstValidReq);
        return ResultVO.success(batchId);
    }

    @Override
    public ResultVO<Page<ResourceInstListPageResp>> getResourceInstListForTask(ResourceInstListPageReq req) {
        log.info("SupplierResourceInstServiceImpl.getResourceInstList req={}", JSON.toJSONString(req));
        // 多线程没跑完，返回空
        if (runableTask.addNbrForSupplierHasDone()) {
            return this.resourceInstService.getResourceInstList(req);
        } else{
            return ResultVO.success();
        }
    }

    @Override
    public ResultVO listResourceUploadTemp(ResourceUploadTempListPageReq req) {
        // 多线程没跑完，返回空
        if (runableTask.validForSupplierHasDone()) {
            return ResultVO.success(runableTask.exceutorListResourceUploadTemp(req));
        } else{
            return ResultVO.success();
        }
     }

    @Override
    public ResultVO exceutorAddNbrForSupplier(ResourceInstAddReq req) {
        runableTask.exceutorAddNbrForSupplier(req);
        return ResultVO.success();
    }

    @Override
    public ResultVO addResourceInstByAdmin(ResourceInstAddReq req) {
        log.info("SupplierResourceInstServiceImpl.addResourceInstByAdmin req={}", JSON.toJSONString(req));
        List<String> validNbrList = req.getMktResInstNbrs();
        ResourceInstsTrackGetReq resourceInstsTrackGetReq = new ResourceInstsTrackGetReq();
        CopyOnWriteArrayList<String> mktResInstNbrList = new CopyOnWriteArrayList<String>(validNbrList);
        resourceInstsTrackGetReq.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
        resourceInstsTrackGetReq.setTypeId(req.getTypeId());
        resourceInstsTrackGetReq.setMktResInstNbrList(mktResInstNbrList);
        ResultVO<List<ResouceInstTrackDTO>> trackListVO = resouceInstTrackService.listResourceInstsTrack(resourceInstsTrackGetReq);
        log.info("SupplierResourceInstServiceImpl addResourceInstByAdmin resouceInstTrackService.listResourceInstsTrack req={},resp={}", JSON.toJSONString(resourceInstsTrackGetReq), JSON.toJSONString(trackListVO));
        if (trackListVO.isSuccess() && null != trackListVO.getResultData()) {
            return ResultVO.error(constant.getNoResInstInMerchant());
        }

        ResultVO<MerchantDTO> merchantResultVO = resouceStoreService.getMerchantByStore(req.getMktResStoreId());
        log.info("SupplierResourceInstServiceImpl.addResourceInstByAdmin resouceStoreService.getMerchantByStore req={} resp={}", req.getMktResStoreId(), JSON.toJSONString(merchantResultVO));
        if (!merchantResultVO.isSuccess() || null == merchantResultVO.getResultData()) {
            return ResultVO.error(constant.getCannotGetMerchantMsg());
        }
        MerchantDTO merchantDTO = merchantResultVO.getResultData();
        req.setDestStoreId(req.getMktResStoreId());
        req.setLanId(merchantDTO.getLanId());
        req.setRegionId(merchantDTO.getCity());
        req.setMerchantType(merchantDTO.getMerchantType());
        req.setCreateStaff(merchantDTO.getMerchantId());

        // 串码来源是空的为厂商串码
        List<ResouceInstTrackDTO> trackList = trackListVO.getResultData();
        List<ResouceInstTrackDTO> merchantTrackList = trackList.stream().filter(t -> StringUtils.isBlank(t.getSourceType())).collect(Collectors.toList());
        List<ResouceInstTrackDTO> supplierTrackList = trackList.stream().filter(t -> StringUtils.isNotBlank(t.getSourceType())).collect(Collectors.toList());
        List<String> existNbrs = supplierTrackList.stream().map(ResouceInstTrackDTO::getMktResInstNbr).collect(Collectors.toList());
        ResourceInstAddResp resourceInstAddResp = new ResourceInstAddResp();
        resourceInstAddResp.setExistNbrs(existNbrs);

        Map<String, List<ResouceInstTrackDTO>> map = merchantTrackList.stream().collect(Collectors.groupingBy(t -> t.getMktResStoreId()));
        for (Map.Entry<String, List<ResouceInstTrackDTO>> entry : map.entrySet()) {
            String manuResStoreId = entry.getKey();
            List<ResouceInstTrackDTO> sameStoreTrackList = entry.getValue();
            List<String> addNbrList = sameStoreTrackList.stream().map(ResouceInstTrackDTO::getMktResInstNbr).collect(Collectors.toList());
            ResultVO<MerchantDTO> sourceMerchantResultVO = resouceStoreService.getMerchantByStore(manuResStoreId);
            log.info("SupplierResourceInstServiceImpl.addResourceInstByAdmin resouceStoreService.getMerchantByStore req={} resp={}", manuResStoreId, JSON.toJSONString(sourceMerchantResultVO));
            if (!sourceMerchantResultVO.isSuccess() || null == sourceMerchantResultVO.getResultData()) {
                return ResultVO.error(constant.getCannotGetMerchantMsg());
            }
            MerchantDTO sourceMerchant = sourceMerchantResultVO.getResultData();
            req.setMktResInstNbrs(addNbrList);
            req.setMktResStoreId(manuResStoreId);
            req.setSourceType(ResourceConst.SOURCE_TYPE.MERCHANT.getCode());
            req.setMktResInstType(trackList.get(0).getMktResInstType());
            Boolean addNum = resourceInstService.addResourceInst(req);
            if (!addNum) {
                return ResultVO.error(constant.getAddNbrFail());
            }
            ResourceInstUpdateReq resourceInstUpdateReq = new ResourceInstUpdateReq();
            resourceInstUpdateReq.setDestStoreId(manuResStoreId);
            resourceInstUpdateReq.setMktResInstNbrs(addNbrList);
            resourceInstUpdateReq.setMktResStoreId(req.getMktResStoreId());
            resourceInstUpdateReq.setMerchantId(sourceMerchant.getMerchantId());
            resourceInstUpdateReq.setEventType(ResourceConst.EVENTTYPE.NO_RECORD.getCode());
            resourceInstUpdateReq.setCheckStatusCd(Lists.newArrayList(
                    ResourceConst.STATUSCD.AUDITING.getCode(),
                    ResourceConst.STATUSCD.ALLOCATIONED.getCode(),
                    ResourceConst.STATUSCD.ALLOCATIONING.getCode(),
                    ResourceConst.STATUSCD.RESTORAGEING.getCode(),
                    ResourceConst.STATUSCD.RESTORAGED.getCode(),
                    ResourceConst.STATUSCD.DELETED.getCode()));
            resourceInstUpdateReq.setStatusCd(ResourceConst.STATUSCD.SALED.getCode());
            resourceInstUpdateReq.setTypeId(req.getTypeId());
            ResultVO updateResourceresultVO = resourceInstService.updateResourceInst(resourceInstUpdateReq);
            if (!updateResourceresultVO.isSuccess()) {
                throw new RetailTipException(ResultCodeEnum.ERROR.getCode(), updateResourceresultVO.getResultMsg());
            }
            // step4 增加事件和批次
            Map<String, List<String>> mktResIdAndNbrMap = new HashMap<>();
            mktResIdAndNbrMap.put(req.getMktResId(), req.getMktResInstNbrs());
            BatchAndEventAddReq batchAndEventAddReq = new BatchAndEventAddReq();
            batchAndEventAddReq.setEventType(ResourceConst.EVENTTYPE.PUT_STORAGE.getCode());
            batchAndEventAddReq.setLanId(merchantDTO.getLanId());
            batchAndEventAddReq.setMktResIdAndNbrMap(mktResIdAndNbrMap);
            batchAndEventAddReq.setRegionId(merchantDTO.getCity());
            batchAndEventAddReq.setDestStoreId(req.getMktResStoreId());
            batchAndEventAddReq.setMerchantId(merchantDTO.getMerchantId());
            batchAndEventAddReq.setMktResStoreId(manuResStoreId);
            batchAndEventAddReq.setCreateStaff(req.getCreateStaff());
            batchAndEventAddReq.setStatusCd(ResourceConst.EVENTSTATE.DONE.getCode());
            resourceBatchRecService.saveEventAndBatch(batchAndEventAddReq);
            log.info("SupplierResourceInstServiceImpl.addResourceInstForProvinceStore resourceBatchRecService.saveEventAndBatch req={},resp={}", JSON.toJSONString(batchAndEventAddReq));
        }
        return ResultVO.success(constant.getAddNbrSucess(), resourceInstAddResp);
    }

    @Override
    public ResultVO<List<ResourceInstListPageResp>> queryForExport(ResourceInstListPageReq req){
        return ResultVO.success(queryResourceInstRunableTask.exceutorQueryResourceInst(req));
    }

    @Override
    public ResourceInstCheckResp getMktResInstNbrForCheck(ResourceStoreIdResnbr req) {
        return resourceInstManager.getMktResInstNbrForCheck(req);
    }

    @Override
    public List<ResourceInstCheckResp> getMktResInstNbrForCheckInTrack(ResourceStoreIdResnbr req) {
        log.info("SupplierResourceInstServiceImpl.getMktResInstNbrForCheckInTrack . req={},resp={}", JSON.toJSONString(req));

        return resourceInstManager.getMktResInstNbrForCheckInTrack(req);
    }


    @Override
    public ResultVO resetResourceInst(AdminResourceInstDelReq req) {
        List<String> mktResInstIdList = req.getMktResInstIdList();
        for (String mktResInstId : mktResInstIdList) {
            log.info("SupplierResourceInstServiceImpl.resetResourceInst req={}", JSON.toJSONString(req));
            ResourceInstsGetByIdListAndStoreIdReq queryReq = new ResourceInstsGetByIdListAndStoreIdReq();
            queryReq.setMktResInstIdList(Lists.newArrayList(mktResInstId));
            queryReq.setMktResStoreId(req.getDestStoreId());
            List<ResourceInstDTO> instListResps = resourceInstManager.selectByIds(queryReq);
            log.info("SupplierResourceInstServiceImpl.resetResourceInst resourceInstManager.selectByIds req={}, resp={}", JSON.toJSONString(req), JSON.toJSONString(instListResps));
            if (CollectionUtils.isEmpty(instListResps)) {
                return ResultVO.error(constant.getNoResInst());
            }
            ResourceInstDTO dto = instListResps.get(0);
            if (StringUtils.isNotEmpty(dto.getOrderId())) {
                return ResultVO.error(dto.getMktResInstNbr()+constant.getTradeNbrCanNotReset());
            }
            String mktResStoreId = resouceInstTrackDetailManager.getMerchantStoreId(dto.getMktResInstNbr());
            log.info("SupplierResourceInstServiceImpl.resetResourceInst resouceInstTrackDetailManager.getMerchantStoreId req={}, resp", instListResps.get(0).getMktResInstNbr(), mktResStoreId);
            if (StringUtils.isEmpty(mktResStoreId)) {
                return ResultVO.error(constant.getCannotGetStoreMsg());
            }
            req.setMktResInstIdList(Lists.newArrayList(mktResInstId));
            req.setMktResStoreId(mktResStoreId);
            ResultVO resultVO = resourceInstService.updateResourceInstByIds(req);
            if (!resultVO.isSuccess()) {
                return resultVO;
            }
            List<String> failMktResInstIdList = (List<String>)resultVO.getResultData();
            if (CollectionUtils.isNotEmpty(failMktResInstIdList)) {
                return resultVO;
            }
            ResultVO<MerchantDTO> sourceMerchantResultVO = resouceStoreService.getMerchantByStore(mktResStoreId);
            log.info("SupplierResourceInstServiceImpl.resetResourceInst resouceStoreService.getMerchantByStore mktResStoreId={}, resp={}", mktResStoreId, JSON.toJSONString(sourceMerchantResultVO));
            if (!sourceMerchantResultVO.isSuccess() || null == sourceMerchantResultVO.getResultData()) {
                return ResultVO.error(constant.getCannotGetMerchantMsg());
            }
            // 更新厂家对应的串码
            ResourceInstUpdateReq updateReq = new ResourceInstUpdateReq();
            updateReq.setUpdateStaff(req.getUpdateStaff());
            updateReq.setMktResInstNbrs(Lists.newArrayList(dto.getMktResInstNbr()));
            updateReq.setStatusCd(ResourceConst.STATUSCD.AVAILABLE.getCode());
            updateReq.setMktResStoreId(req.getMktResStoreId());
            updateReq.setDestStoreId(mktResStoreId);
            updateReq.setMktResId(dto.getMktResId());
            updateReq.setMerchantId(sourceMerchantResultVO.getResultData().getMerchantId());
            List<String> checkStatusCd = Lists.newArrayList(ResourceConst.STATUSCD.DELETED.getCode());
            updateReq.setCheckStatusCd(checkStatusCd);
            resourceInstService.updateInstState(updateReq);
            log.info("SupplierResourceInstServiceImpl.resetResourceInst resourceInstManager.batchUpdateInstState req={}", JSON.toJSONString(updateReq));
            resouceInstTrackService.asynResetResourceInst(req, ResultVO.success());
        }
        return ResultVO.success();
    }
}