package com.iwhalecloud.retail.promo.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.goods2b.dto.ProductDTO;
import com.iwhalecloud.retail.goods2b.dto.req.ProductGetReq;
import com.iwhalecloud.retail.goods2b.service.dubbo.ProductService;
import com.iwhalecloud.retail.order2b.dto.model.order.SettleRecordOrderDTO;
import com.iwhalecloud.retail.order2b.service.SettleRecordOrderService;
import com.iwhalecloud.retail.partner.common.PartnerConst;
import com.iwhalecloud.retail.partner.dto.MerchantAccountDTO;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.dto.req.MerchantAccountListReq;
import com.iwhalecloud.retail.partner.dto.req.MerchantListReq;
import com.iwhalecloud.retail.partner.service.MerchantAccountService;
import com.iwhalecloud.retail.partner.service.MerchantService;
import com.iwhalecloud.retail.promo.common.PromoConst;
import com.iwhalecloud.retail.promo.dto.SettleRecordDTO;
import com.iwhalecloud.retail.promo.entity.ActivityProduct;
import com.iwhalecloud.retail.promo.manager.ActivityProductManager;
import com.iwhalecloud.retail.promo.manager.SettleRecordManager;
import com.iwhalecloud.retail.promo.service.SettleRecordService;
import com.iwhalecloud.retail.promo.utils.DateUtil;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.system.dto.request.UserListReq;
import com.iwhalecloud.retail.system.service.UserService;
import com.iwhalecloud.retail.warehouse.dto.ResouceStoreDTO;
import com.iwhalecloud.retail.warehouse.dto.request.StorePageReq;
import com.iwhalecloud.retail.warehouse.service.ResouceStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2019/3/28.
 */
@Slf4j
@Service
public class SettleRecordServiceImpl implements SettleRecordService {
    @Autowired
    private SettleRecordManager settleRecordManager;

    @Reference
    private ProductService productService;

    @Reference
    private SettleRecordOrderService settleRecordOrderService;

    @Reference
    private MerchantService merchantService;

    @Reference
    private MerchantAccountService merchantAccountService;

    @Reference
    private UserService userService;

    @Reference
    private ResouceStoreService resouceStoreService;

    @Autowired
    private ActivityProductManager activityProductManager;


    @Override
    public Integer batchAddSettleRecord(List<SettleRecordDTO> settleRecordDTOs) {
        List<SettleRecordDTO> settleRecords = new ArrayList<>();
        if(CollectionUtils.isEmpty(settleRecordDTOs)){
            return 0;
        }

        for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
            if(settleRecordDTO.getSubsidyAmount()>0){
                settleRecords.add(settleRecordDTO);
            }
        }
        return settleRecordManager.addSettleRecord(settleRecords);

    }

    @Override
    public List<SettleRecordDTO> getSettleRecord(String lanId) throws Exception{
        List<SettleRecordDTO> settleRecordDTOs = new ArrayList<>();
        if(StringUtils.isEmpty(lanId)){
            return settleRecordDTOs;
        }
        //结算周期记录
        List<SettleRecordDTO> settleRecords1 = settleRecordManager.getSettleRecord();
        log.info("SettleRecordServiceImpl.getSettleRecord getSettleRecord settleRecords1={},lanId={}",settleRecords1,lanId);
        //结算周期补录记录
        List<SettleRecordDTO> settleRecords2 = settleRecordManager.getSupplementaryRecord();
        log.info("SettleRecordServiceImpl.getSettleRecord getSupplementaryRecord settleRecords2={},lanId={}",settleRecords2,lanId);
        List<String> orderIds = new ArrayList<>();
        List<String> supplementaryOrderIds = new ArrayList<>();
        //结算周期的订单id List
        if(!CollectionUtils.isEmpty(settleRecords1)){
            for(SettleRecordDTO settleRecordDTO:settleRecords1){
                orderIds.add(settleRecordDTO.getOrderId());
            }
        }

        List<SettleRecordOrderDTO> settleRecordOrderDTOs = new ArrayList<>();
        if(!CollectionUtils.isEmpty(orderIds)){
            settleRecordOrderDTOs = settleRecordOrderService.getSettleRecordOrder(orderIds,lanId);
            log.info("SettleRecordServiceImpl.getSettleRecord getSettleRecordOrder1 settleRecordOrderDTOs={},lanId={}",settleRecordOrderDTOs,lanId);
        }
        //目标时间是否在起始时间和结束时间范围内的orders
        List<String> orderList = new ArrayList<>();
        List<String> orderMarketingActivityList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(settleRecordOrderDTOs) && !CollectionUtils.isEmpty(settleRecords1)){
            for(SettleRecordDTO settleRecordDTO:settleRecords1){
                String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                String deliverStartTime = DateUtil.formatDate(settleRecordDTO.getDeliverStartTime(), DATE_FORMAT);
                String deliverEndTime = DateUtil.formatDate(settleRecordDTO.getDeliverEndTime(), DATE_FORMAT);
                String orderId = settleRecordDTO.getOrderId();
                if(orderMarketingActivityList.contains(orderId+settleRecordDTO.getMarketingActivityId())){
                    continue;
                }else{
                    orderMarketingActivityList.add(orderId+settleRecordDTO.getMarketingActivityId());
                }
                orderList.add(orderId);
                //校验目标时间是否在起始时间和结束时间范围内
                for(SettleRecordOrderDTO settleRecordOrderDTO: settleRecordOrderDTOs){
                    if(StringUtils.isNotEmpty(orderId) && orderId.equals(settleRecordOrderDTO.getOrderId())){
                        String DeliveryTime = DateUtil.formatDate(settleRecordOrderDTO.getDeliveryTime(), DATE_FORMAT);
                        Integer lanid = settleRecordOrderDTO.getLanId();
                        if(DateUtil.compare(deliverStartTime, deliverEndTime, DeliveryTime)
                                && lanId.equals(String.valueOf(lanid))){
                            log.info("SettleRecordServiceImpl.getSettleRecord addList1 begin settleRecordDTOs={},lanId={}",settleRecordDTOs,lanId);
                            this.addList(settleRecordDTOs,settleRecordDTO,settleRecordOrderDTO);
                            log.info("SettleRecordServiceImpl.getSettleRecord addList1 end settleRecordDTOs={},lanId={}",settleRecordDTOs,lanId);
                        }
                    }
                }
            }

        }
        //补录的订单id List
        if(!CollectionUtils.isEmpty(settleRecords2)){
            for(SettleRecordDTO settleRecordDTO:settleRecords2){
                if(!orderList.contains(settleRecordDTO.getOrderId())){
                    supplementaryOrderIds.add(settleRecordDTO.getOrderId());
                }
            }
        }
        List<SettleRecordOrderDTO> settleRecordOrderDTOs2 = new ArrayList<>();
        if(!CollectionUtils.isEmpty(supplementaryOrderIds)){
            settleRecordOrderDTOs2 = settleRecordOrderService.getSettleRecordOrder(supplementaryOrderIds,lanId);
            log.info("SettleRecordServiceImpl.getSettleRecord getSettleRecordOrder2 settleRecordOrderDTOs2={},lanId={}",settleRecordOrderDTOs2,lanId);
        }
        List<String> orderMarketingActivityList2 = new ArrayList<>();
        if(!CollectionUtils.isEmpty(settleRecordOrderDTOs2) && !CollectionUtils.isEmpty(settleRecords2)){
            for(SettleRecordDTO settleRecordDTO:settleRecords2){
                String orderId = settleRecordDTO.getOrderId();
                if(orderMarketingActivityList2.contains(orderId+settleRecordDTO.getMarketingActivityId())){
                    continue;
                }else{
                    orderMarketingActivityList2.add(orderId+settleRecordDTO.getMarketingActivityId());
                }
                for(SettleRecordOrderDTO settleRecordOrderDTO: settleRecordOrderDTOs2){
                    Integer lanid = settleRecordOrderDTO.getLanId();
                    if(StringUtils.isNotEmpty(orderId) && orderId.equals(settleRecordOrderDTO.getOrderId())
                            && lanId.equals(String.valueOf(lanid))){
                        log.info("SettleRecordServiceImpl.getSettleRecord addList2 begin settleRecordDTOs={},lanId={}",settleRecordDTOs,lanId);
                        this.addList(settleRecordDTOs,settleRecordDTO,settleRecordOrderDTO);
                        log.info("SettleRecordServiceImpl.getSettleRecord addList2 end settleRecordDTOs={},lanId={}",settleRecordDTOs,lanId);
                    }
                }

            }
        }
        log.info("SettleRecordServiceImpl.getSettleRecord settleRecordDTOs={},lanId={}",settleRecordDTOs,lanId);
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            this.setProductInfo(settleRecordDTOs);
            this.setSupplierInfo(settleRecordDTOs);
            this.setAccount(settleRecordDTOs);
            this.setResStoreId(settleRecordDTOs);
            this.setCouponPrice(settleRecordDTOs);
        }

        return settleRecordDTOs;
    }

    private void addList(List<SettleRecordDTO> settleRecordDTOs,SettleRecordDTO settleRecordDTO,SettleRecordOrderDTO settleRecordOrderDTO){
        SettleRecordDTO settleRecord = new SettleRecordDTO();
        BeanUtils.copyProperties(settleRecordDTO, settleRecord);
        settleRecord.setLanId(settleRecordOrderDTO.getLanId());
        settleRecord.setResNbr(settleRecordOrderDTO.getResNbr());
        settleRecord.setSupplierId(settleRecordOrderDTO.getSupplierId());
        settleRecord.setMerchantId(settleRecordOrderDTO.getMerchantId());
        settleRecord.setSupplierCode(settleRecordOrderDTO.getSupplierCode());
        settleRecord.setMerchantCode(settleRecordOrderDTO.getMerchantCode());
        settleRecord.setPrice(settleRecordOrderDTO.getPrice());
        settleRecord.setSubsidyAmount(settleRecordOrderDTO.getCouponPrice());
        settleRecord.setOrderCreateTime(settleRecordOrderDTO.getOrderCreateTime());
        settleRecord.setOperationType(PromoConst.OperationType.OPERATION_TYPE_1.getCode());
        settleRecord.setSupplierAccountId(settleRecordOrderDTO.getSupplierAccountId());
        settleRecord.setMerchantAccountId(settleRecordOrderDTO.getMerchantAccountId());
        settleRecord.setCreateUserId("system_user");
        settleRecord.setSettleMode(PromoConst.SettleMode.SETTLE_MODE_1.getCode());
        settleRecordDTOs.add(settleRecord);

    }

    private void setResStoreId(List<SettleRecordDTO> settleRecordDTOs){
        List<String> supplierIds = new ArrayList<>();
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                supplierIds.add(settleRecordDTO.getSupplierId());
            }
        }
        StorePageReq storePageReq = new StorePageReq();
        storePageReq.setMerchantIds(supplierIds);
        log.info("SettleRecordServiceImpl.getSettleRecord pageStore storePageReq={}", storePageReq);
        Page<ResouceStoreDTO> resouceStoreDTOPage = resouceStoreService.pageStore(storePageReq);
        if (!CollectionUtils.isEmpty( resouceStoreDTOPage.getRecords()) ) {
           List<ResouceStoreDTO> resouceStoreDTOs = resouceStoreDTOPage.getRecords();
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                for(ResouceStoreDTO resouceStoreDTO:resouceStoreDTOs){
                    if(settleRecordDTO.getSupplierId().equals(resouceStoreDTO.getMerchantId())){
                        settleRecordDTO.setMktResStoreId(resouceStoreDTO.getMktResStoreId());
                    }
                }
            }
        }
    }

    private void setSupplierInfo(List<SettleRecordDTO> settleRecordDTOs){
        List<String> merchantCodeList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                merchantCodeList.add(settleRecordDTO.getSupplierCode());
                merchantCodeList.add(settleRecordDTO.getMerchantCode());
            }
        }
        MerchantListReq merchantListReq = new MerchantListReq();
        merchantListReq.setMerchantCodeList(merchantCodeList);
        log.info("SettleRecordServiceImpl.getSettleRecord listMerchant merchantListReq={}", merchantListReq);
        ResultVO<List<MerchantDTO>> listResultVO = merchantService.listMerchant(merchantListReq);
        log.info("SettleRecordServiceImpl.getSettleRecord listMerchant listResultVO={}", listResultVO);

        List<String> supplierIds = new ArrayList<>();
        if (listResultVO.isSuccess() && !CollectionUtils.isEmpty(listResultVO.getResultData())) {
            List<MerchantDTO> merchantDTOList = listResultVO.getResultData();
            for (SettleRecordDTO settleRecordDTO : settleRecordDTOs) {
                for (MerchantDTO merchantDTO : merchantDTOList) {
                    if (merchantDTO.getMerchantCode().equals(settleRecordDTO.getSupplierCode())) {
                        settleRecordDTO.setSupplierName(merchantDTO.getMerchantName());
                        settleRecordDTO.setSupplierId(merchantDTO.getMerchantId());
                        settleRecordDTO.setSupplierCode(merchantDTO.getShopCode());
                        supplierIds.add(merchantDTO.getMerchantId());
                    }
                    if(merchantDTO.getMerchantCode().equals(settleRecordDTO.getMerchantCode())){
                        settleRecordDTO.setMerchantName(merchantDTO.getMerchantName());
                        settleRecordDTO.setMerchantId(merchantDTO.getMerchantId());
                        supplierIds.add(merchantDTO.getMerchantId());
                    }
                }
            }
        }

        //添加账户信息
        MerchantAccountListReq merchantAccountListReq = new MerchantAccountListReq();
        merchantAccountListReq.setMerchantIdList(supplierIds);
        merchantAccountListReq.setAccountType(PartnerConst.MerchantAccountTypeEnum.BEST_PAY.getType());
        log.info("SettleRecordServiceImpl.getSettleRecord listMerchantAccount merchantAccountListReq={}", merchantAccountListReq);
        ResultVO<List<MerchantAccountDTO>> resultVO = merchantAccountService.listMerchantAccount(merchantAccountListReq);
        if(resultVO.isSuccess() && !CollectionUtils.isEmpty(resultVO.getResultData())){
            List<MerchantAccountDTO> merchantAccountDTOs = resultVO.getResultData();
            for(SettleRecordDTO settleRecordDTO : settleRecordDTOs){
                for (MerchantAccountDTO merchantAccountDTO : merchantAccountDTOs) {
                    if(merchantAccountDTO.getMerchantId().equals(settleRecordDTO.getSupplierId())){
                        settleRecordDTO.setSupplierBestpayAccount(merchantAccountDTO.getAccount());
                    }
                    if(merchantAccountDTO.getMerchantId().equals(settleRecordDTO.getMerchantId())){
                        settleRecordDTO.setMerchantBestpayAccount(merchantAccountDTO.getAccount());
                    }
                }
            }
        }



    }

    private void setAccount(List<SettleRecordDTO> settleRecordDTOs){
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            List<String> userIds = new ArrayList<>();
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                userIds.add(settleRecordDTO.getSupplierAccountId());
                userIds.add(settleRecordDTO.getMerchantAccountId());
            }
            UserListReq userListReq = new UserListReq();
            userListReq.setUserIdList(userIds);
            log.info("SettleRecordServiceImpl.getSettleRecord getUserList userListReq={}", userListReq);
            List<UserDTO> userDTOs = userService.getUserList(userListReq);
            if(!CollectionUtils.isEmpty(userDTOs)){
                for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                    for(UserDTO userDTO:userDTOs){
                        if(settleRecordDTO.getSupplierAccountId().equals(userDTO.getUserId())){
                            settleRecordDTO.setSupplierAccount(userDTO.getLoginName());
                        }
                        if(settleRecordDTO.getMerchantAccountId().equals(userDTO.getUserId())){
                            settleRecordDTO.setMerchantAccount(userDTO.getLoginName());
                        }
                    }
                }
            }
        }
    }

    private void setProductInfo(List<SettleRecordDTO> settleRecordDTOs){
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            List<String> productIds = new ArrayList<>();
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                productIds.add(settleRecordDTO.getProductId());
            }
            ProductGetReq productGetReq = new ProductGetReq();
            productGetReq.setProductIdList(productIds);
            ResultVO<Page<ProductDTO>> resultVO = productService.selectProduct(productGetReq);
            if(resultVO.isSuccess() && null != resultVO.getResultData() &&
                    !CollectionUtils.isEmpty( resultVO.getResultData().getRecords())){
                List<ProductDTO> productDTOs = resultVO.getResultData().getRecords();
                for(SettleRecordDTO settleRecordDTO : settleRecordDTOs){
                    for(ProductDTO productDTO:productDTOs){
                        if(settleRecordDTO.getProductId().equals(productDTO.getProductId())){
                            settleRecordDTO.setSn(productDTO.getSn());
                            settleRecordDTO.setTypeId(productDTO.getTypeId());
                            break;
                        }
                    }
                }
            }
        }
    }

    private void setCouponPrice(List<SettleRecordDTO> settleRecordDTOs){
        if(!CollectionUtils.isEmpty(settleRecordDTOs)){
            List<String> marketingActivityIds = new ArrayList<>();

            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                marketingActivityIds.add(settleRecordDTO.getMarketingActivityId());
            }

            List<ActivityProduct> activityProducts = activityProductManager.queryActivityProductByCondition(marketingActivityIds);
            for(SettleRecordDTO settleRecordDTO:settleRecordDTOs){
                for(ActivityProduct activityProduct:activityProducts){
                    if(settleRecordDTO.getProductId().equals(activityProduct.getProductId()) &&
                            settleRecordDTO.getMarketingActivityId().equals(activityProduct.getMarketingActivityId())){
                        settleRecordDTO.setSubsidyAmount(Double.valueOf(activityProduct.getDiscountAmount()));
                    }
                }
            }
        }
    }
}
