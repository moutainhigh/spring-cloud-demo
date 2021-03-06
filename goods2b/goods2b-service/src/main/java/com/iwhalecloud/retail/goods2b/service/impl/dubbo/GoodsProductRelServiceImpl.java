package com.iwhalecloud.retail.goods2b.service.impl.dubbo;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultCodeEnum;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.goods2b.common.GoodsConst;
import com.iwhalecloud.retail.goods2b.common.GoodsResultCodeEnum;
import com.iwhalecloud.retail.goods2b.dto.ActivityGoodsDTO;
import com.iwhalecloud.retail.goods2b.dto.BuyCountCheckDTO;
import com.iwhalecloud.retail.goods2b.dto.GoodsDetailDTO;
import com.iwhalecloud.retail.goods2b.dto.GoodsProductRelDTO;
import com.iwhalecloud.retail.goods2b.dto.req.ActivityGoodsReq;
import com.iwhalecloud.retail.goods2b.dto.req.GoodsProductRelEditReq;
import com.iwhalecloud.retail.goods2b.dto.req.GoodsQueryByProductIdsReq;
import com.iwhalecloud.retail.goods2b.dto.req.ProdFileReq;
import com.iwhalecloud.retail.goods2b.dto.resp.GoodsQueryByProductIdsResp;
import com.iwhalecloud.retail.goods2b.entity.*;
import com.iwhalecloud.retail.goods2b.manager.*;
import com.iwhalecloud.retail.goods2b.service.dubbo.GoodsProductRelService;
import com.iwhalecloud.retail.partner.common.PartnerConst;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.service.MerchantRulesService;
import com.iwhalecloud.retail.partner.service.MerchantService;
import com.iwhalecloud.retail.promo.common.PromoConst;
import com.iwhalecloud.retail.promo.dto.resp.ActivityProductRespDTO;
import com.iwhalecloud.retail.promo.dto.resp.PreSubsidyProductRespDTO;
import com.iwhalecloud.retail.promo.service.ActivityProductService;
import com.iwhalecloud.retail.system.dto.OrganizationDTO;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.system.service.OrganizationService;
import com.iwhalecloud.retail.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("goodsProductRelService")
@Service
public class GoodsProductRelServiceImpl implements GoodsProductRelService {

    @Autowired
    private GoodsProductRelManager goodsProductRelManager;

    @Autowired
    private GoodsManager goodsManager;

    @Autowired
    private ProdFileManager prodFileManager;

    @Reference
    private MerchantService merchantService;

    @Autowired
    private CatManager catManager;

    @Reference
    private ActivityProductService activityProductService;

    @Reference
    private MerchantRulesService merchantRulesService;

    @Reference
    private UserService userService;

//    @Reference
//    private CommonOrgService commonOrgService;

    @Reference
    private OrganizationService organizationService;

    @Autowired
    private GoodsRegionRelManager goodsRegionRelManager;

    @Autowired
    private GoodsTargetManager goodsTargetManager;

    /**
     * 根据产品ID查询商品
     *
     * @param goodsProductRelEditReq
     * @return
     */
    @Override
    public ResultVO<List<GoodsDetailDTO>> qryGoodsByProductId(GoodsProductRelEditReq goodsProductRelEditReq) {
        String productId = goodsProductRelEditReq.getProductId();
        return ResultVO.success(goodsProductRelManager.qryGoodsByProductId(productId));
    }

    @Override
    public ResultVO<Boolean> updateIsHaveStock(GoodsProductRelEditReq goodsProductRelEditReq) {
        String supplierId = goodsProductRelEditReq.getMerchantId();
        String productId = goodsProductRelEditReq.getProductId();
        Boolean isHaveStock = goodsProductRelEditReq.getIsHaveStock();
        log.info("GoodsProductRelServiceImpl.updateIsHaveStock supplierId={},productId={},isHaveStock={}", supplierId, productId, isHaveStock);
        if (supplierId == null || productId == null || isHaveStock == null) {
            ResultVO.errorEnum(ResultCodeEnum.LACK_OF_PARAM);
        }
        List<String> goodsIdList = goodsProductRelManager.listGoodsBySupplierId(supplierId, productId);
        if (CollectionUtils.isEmpty(goodsIdList)) {
            ResultVO.success();
        }
        for (String goodsId : goodsIdList) {
            goodsProductRelManager.updateIsHaveStock(goodsId, productId, isHaveStock);
        }
        return ResultVO.success(true);
    }

    @Override
    public ResultVO<GoodsProductRelDTO> qryMinAndMaxNum(GoodsProductRelEditReq goodsProductRelEditReq) {
        String goodsId = goodsProductRelEditReq.getGoodsId();
        String productId = goodsProductRelEditReq.getProductId();
        log.info("GoodsProductRelServiceImpl.qryMinAndMaxNum goodsId={},productId={}", goodsId, productId);
        if (goodsId == null || productId == null) {
            ResultVO.errorEnum(ResultCodeEnum.LACK_OF_PARAM);
        }
        GoodsProductRel goodsProductRel = goodsProductRelManager.getGoodsProductRel(goodsId, productId);
        GoodsProductRelDTO goodsProductRelDTO = new GoodsProductRelDTO();
        BeanUtils.copyProperties(goodsProductRel, goodsProductRelDTO);
        return ResultVO.success(goodsProductRelDTO);
    }

    @Override
    public ResultVO checkBuyCount(GoodsProductRelEditReq goodsProductRelEditReq) {
        List<BuyCountCheckDTO> buyCountCheckDTOList = goodsProductRelEditReq.getBuyCountCheckDTOList();
        log.info("GoodsProductRelServiceImpl.checkBuyCount goodsProductRelEditReq={}", JSON.toJSONString(goodsProductRelEditReq));
        String merchantId = goodsProductRelEditReq.getMerchantId();
        String userId = goodsProductRelEditReq.getUserId();
        if (CollectionUtils.isEmpty(buyCountCheckDTOList) || merchantId == null || userId == null) {
            log.error("GoodsProductRelServiceImpl.checkBuyCount LACK_OF_PARAM");
            ResultVO.errorEnum(ResultCodeEnum.LACK_OF_PARAM);
        }
        String lanId;
        String regionId;
        try {
            UserDTO userDTO = userService.getUserByUserId(userId);
            lanId = userDTO.getLanId();
            regionId = userDTO.getRegionId();
        } catch (Exception ex) {
            return ResultVO.errorEnum(GoodsResultCodeEnum.INVOKE_SYSTEM_SERVICE_EXCEPTION);
        }
        // 获取商家经营的机型
        ResultVO<List<String>> listResultVO = merchantRulesService.getProductAndBrandPermission(merchantId);
        List<String> productIdList = listResultVO.getResultData();
        if (CollectionUtils.isEmpty(productIdList)) {
            // 该商家没有经营的机型
            log.error("GoodsProductRelServiceImpl.checkBuyCount 商家没有经营的机型，merchantId={}", merchantId);
            return ResultVO.error("没有经营权限");
        }
        for (BuyCountCheckDTO buyCountCheck : buyCountCheckDTOList) {
            String goodsId = buyCountCheck.getGoodsId();
            String productId = buyCountCheck.getProductId();
            Long buyCount = buyCountCheck.getBuyCount();
            if (StringUtils.isEmpty(goodsId) || StringUtils.isEmpty(productId) || buyCount == null) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount LACK_OF_PARAM");
                ResultVO.errorEnum(ResultCodeEnum.LACK_OF_PARAM);
            }
            // 校验发布区域和发布对象
            if (checkRegionAndTarget(merchantId, lanId, regionId, goodsId)) {
                return ResultVO.error("校验发布区域或发布对象失败");
            }
            if (!productIdList.contains(productId)) {
                // 该商家没有这款产品的经营权限
                log.error("GoodsProductRelServiceImpl.checkBuyCount 商家没有该机型的经营权限，merchantId={}，productId={}", merchantId, productId);
                return ResultVO.error("没有经营权限");
            }
            GoodsProductRel goodsProductRel = goodsProductRelManager.getGoodsProductRel(goodsId, productId);
            if (goodsProductRel == null) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount getGoodsProductRel return null goodsId={}, productId={}", goodsId, productId);
                return ResultVO.error("数据不完整");
            }

            // 校验当前预售商品已订购的数量是否超出活动定义的参与数
            String isAdvanceSale = goodsProductRelEditReq.getIsAdvanceSale();
            String marketingActivityId = goodsProductRelEditReq.getMarketingActivityId();
            if (!StringUtils.isEmpty(isAdvanceSale) && GoodsConst.IsAdvanceSale.IS_ADVANCE_SALE.equals(isAdvanceSale)) {
                ResultVO<List<PreSubsidyProductRespDTO>> respResultVO = activityProductService.queryPreSubsidyProduct(marketingActivityId);
                if (respResultVO.isSuccess() && respResultVO.getResultData() != null) {
                    if (checkAdvanceSale(productId, buyCount, respResultVO)) {
                        log.error("GoodsProductRelServiceImpl.checkBuyCount check advance sale is false，productId={}", productId);
                        return ResultVO.error("当前预售商品已订购的数量是否超出活动定义的参与数");
                    }
                }
            }
            Long minNum = goodsProductRel.getMinNum();
            Long maxNum = goodsProductRel.getMaxNum();
            Long supplyNum = goodsProductRel.getSupplyNum();
            if (buyCount < minNum || buyCount > maxNum || supplyNum - buyCount < 0) {
                log.info("GoodsProductRelServiceImpl.checkBuyCount buyCountCheckDTOList={},minNum={},maxNum={},buyCount={}, supplyNum={}",
                        JSON.toJSONString(buyCountCheckDTOList), minNum, maxNum, buyCount, supplyNum);
                return ResultVO.error("购买数量校验失败，未达到规则要求");
            }
        }
        return ResultVO.success();
    }

    private boolean checkRegionAndTarget(String merchantId, String lanId, String regionId, String goodsId) {
        Goods goods = goodsManager.queryGoods(goodsId);
        String targetType = goods.getTargetType();
        // 发布区域校验
        if (targetType != null && targetType.equals(GoodsConst.TARGET_TYPE_REGION)) {
            List<GoodsRegionRel> goodsRegionRelList = goodsRegionRelManager.queryGoodsRegionRel(goodsId);
            if (CollectionUtils.isEmpty(goodsRegionRelList)) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount check region is fail goodsId={}", goodsId);
                return true;
            }
            if (lanId == null || regionId == null) {
                return true;
            }

            // start  增加零售商的 orgId 判断  zhong.wenlong 2019.06.13
            // 是否需要根据零售的orgId 进行区域校验
            boolean isNeedCheckOrgId = false;
            String orgPathCode = "";
            MerchantDTO merchantDTO = merchantService.getMerchantById(merchantId).getResultData();
            if (Objects.nonNull(merchantDTO)) {
                // 判断是否 是零售商  组织ID 是否为空
                if (StringUtils.equals(merchantDTO.getMerchantType(), PartnerConst.MerchantTypeEnum.PARTNER.getType())
                        && StringUtils.isNotEmpty(merchantDTO.getParCrmOrgId())) {
//                    CommonOrgDTO commonOrgDTO = commonOrgService.getCommonOrgById(merchantDTO.getParCrmOrgId()).getResultData();
                    OrganizationDTO organizationDTO = organizationService.getOrganization(merchantDTO.getParCrmOrgId()).getResultData();
                    if (Objects.nonNull(organizationDTO) && StringUtils.isNotEmpty(organizationDTO.getPathCode())) {
                        orgPathCode = organizationDTO.getPathCode();
                        isNeedCheckOrgId = true;
                        log.info("GoodsProductRelServiceImpl.checkBuyCount 需要根据零售商的orgId去校验商品发布区域，merchantId={}, orgId={}, orgPathCode={}", merchantId, merchantDTO.getParCrmOrgId(), orgPathCode);
                    }
                }
            }
            // end  增加零售商的 orgId 判断  zhong.wenlong 2019.06.13


            boolean checkRegionFlag = false;
            for (GoodsRegionRel item : goodsRegionRelList) {
                boolean flag;
                // 新逻辑
                if (isNeedCheckOrgId) {
                    flag = StringUtils.contains(orgPathCode, item.getOrgId());
                } else {
                    flag = lanId.equals(item.getLanId()) && (lanId.equals(item.getRegionId()) || regionId.equals(item.getRegionId()));
                }
                // 旧逻辑
//                flag = lanId.equals(item.getLanId()) && (lanId.equals(item.getRegionId()) || regionId.equals(item.getRegionId()));
                if (flag) {
                    checkRegionFlag = true;
                    break;
                }
            }
            if (!checkRegionFlag) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount check region is fail goodsId={}，checkRegionFlag={}", goodsId, checkRegionFlag);
                return true;
            }
        } else if (targetType != null && targetType.equals(GoodsConst.TARGET_TYPE_TARGET)) {
            // 发布对象校验
            List<GoodsTargetRel> goodsTargetRelList = goodsTargetManager.queryGoodsTargerRel(goodsId);
            if (CollectionUtils.isEmpty(goodsTargetRelList)) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount check target is fail goodsId={}", goodsId);
                return true;
            }
            boolean checkTargetFlag = false;
            for (GoodsTargetRel goodsTargetRel : goodsTargetRelList) {
                if (merchantId.equals(goodsTargetRel.getTargetId())) {
                    checkTargetFlag = true;
                    break;
                }
            }
            if (!checkTargetFlag) {
                log.error("GoodsProductRelServiceImpl.checkBuyCount check target is fail goodsId={}，checkTargetFlag={}", goodsId, checkTargetFlag);
                return true;
            }
        }
        return false;
    }

    private boolean checkAdvanceSale(String productId, Long buyCount, ResultVO<List<PreSubsidyProductRespDTO>>
            respResultVO) {
        List<PreSubsidyProductRespDTO> preSubsidyProductRespDTOs = respResultVO.getResultData();
        if (!CollectionUtils.isEmpty(preSubsidyProductRespDTOs)) {
            for (PreSubsidyProductRespDTO preSubsidyProductRespDTO : preSubsidyProductRespDTOs) {
                ActivityProductRespDTO activityProductRespDTO = preSubsidyProductRespDTO.getActivityProductResqDTO();
                String productId1 = activityProductRespDTO.getProductId();
                if (productId1.equals(productId)) {
                    if (PromoConst.ProductNumFlg.ProductNumFlg_1.getCode().equals(activityProductRespDTO.getNumLimitFlg())) {
                        long num = activityProductRespDTO.getNum();
                        long reachAmount = activityProductRespDTO.getReachAmount();
                        if (num - reachAmount < buyCount) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    @Override
    public ResultVO<GoodsDetailDTO> qryGoodsByProductIdAndGoodsId(GoodsProductRelEditReq goodsProductRelEditReq) {
        String productId = goodsProductRelEditReq.getProductId();
        String goodsId = goodsProductRelEditReq.getGoodsId();
        return ResultVO.success(goodsProductRelManager.qryGoodsByProductIdAndGoodsId(productId, goodsId));
    }

    @Override
    public ResultVO<List<ActivityGoodsDTO>> qryActivityGoodsId(List<String> productIdList, String merchantId, String pathCode) {
        List<ActivityGoodsDTO> activityGoodsDTOs = new ArrayList<>();
        if (CollectionUtils.isEmpty(productIdList)) {
            return ResultVO.successMessage("产品列表为空");
        }

        ActivityGoodsReq activityGoodsReq = new ActivityGoodsReq();
        activityGoodsReq.setProductIdList(productIdList);
        activityGoodsReq.setMerchantId(merchantId);
        activityGoodsReq.setPathCode(pathCode);
        List<ActivityGoodsDTO> activityGoodsDTOs1 = goodsProductRelManager.qryActivityGoodsId(activityGoodsReq);
        if (CollectionUtils.isEmpty(activityGoodsDTOs1)) {
            return ResultVO.success(activityGoodsDTOs);
        }
        for (ActivityGoodsDTO activityGoodsDTO : activityGoodsDTOs1) {
            String supplierId = activityGoodsDTO.getSupplierId();
            if (!StringUtils.isEmpty(supplierId)) {
                try {
                    ResultVO<MerchantDTO> merchantDTOResultVO = merchantService.getMerchantById(supplierId);
                    if (merchantDTOResultVO.isSuccess() && merchantDTOResultVO.getResultData() != null) {
                        MerchantDTO merchantDTO = merchantDTOResultVO.getResultData();
                        activityGoodsDTO.setSupplierName(merchantDTO.getMerchantName());
                    }
                } catch (Exception ex) {
                    log.error("GoodsServiceImpl.queryPageByConditionAdmin getMerchantById throw exception ex={}", ex);
                    return ResultVO.error(GoodsResultCodeEnum.INVOKE_PARTNER_SERVICE_EXCEPTION);
                }
            }

            String parCategoryId = activityGoodsDTO.getParCategoryId();
            if (!StringUtils.isEmpty(parCategoryId) && parCategoryId.indexOf("|") > -1) {
                String[] parCategoryIds = parCategoryId.split("\\|");
                if (parCategoryIds.length > 1) {
                    String parCateId = parCategoryIds[1];
                    if (!StringUtils.isEmpty(parCategoryId)) {
                        Cat cat = catManager.queryProdCat(parCateId);
                        activityGoodsDTO.setParCategoryName(cat.getCatName());
                        activityGoodsDTO.setParCategoryId(parCateId);
                    }
                }
            }
            activityGoodsDTOs.add(activityGoodsDTO);
        }
        return ResultVO.success(activityGoodsDTOs);
    }

    @Override
    public ResultVO<GoodsQueryByProductIdsResp> queryGoodsIdsByProductIds(GoodsQueryByProductIdsReq req) {
        log.info("GoodsProductRelServiceImpl.queryGoodsIdsByProductIds(), req={} ", JSON.toJSONString(req));
        GoodsQueryByProductIdsResp resp = new GoodsQueryByProductIdsResp();
        List<GoodsProductRel> goodsProductRelList = goodsProductRelManager.queryGoodsByProductIds(req.getProductIds());
        log.info("GoodsProductRelServiceImpl.queryGoodsByProductIds(), goodsProductRelList={} ", JSON.toJSONString(goodsProductRelList));
        if (!CollectionUtils.isEmpty(goodsProductRelList)) {
            List<String> goodsIdList = Lists.newArrayList();
            goodsProductRelList.forEach(item -> {
                goodsIdList.add(item.getGoodsId());
            });
            resp.setGoodsIds(goodsIdList);
        }
        log.info("GoodsProductRelServiceImpl.queryGoodsIdsByProductIds(), resp={} ", JSON.toJSONString(resp));
        return ResultVO.success(resp);
    }

    @Override
    public ResultVO<List<GoodsProductRelDTO>> listGoodsProductRel(String goodsId) {
        List<GoodsProductRel> goodsProductRelList = goodsProductRelManager.listGoodsProductRel(goodsId);
        List<GoodsProductRelDTO> goodsProductRelDTOList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(goodsProductRelList)) {
            for (GoodsProductRel goodsProductRel : goodsProductRelList) {
                GoodsProductRelDTO goodsProductRelDTO = new GoodsProductRelDTO();
                BeanUtils.copyProperties(goodsProductRel, goodsProductRelDTO);
                goodsProductRelDTOList.add(goodsProductRelDTO);
            }
            return ResultVO.success(goodsProductRelDTOList);
        }
        return ResultVO.success(goodsProductRelDTOList);
    }

    @Override
    public void insertProdFile(ProdFileReq req) {
        goodsProductRelManager.insertProdFile(req);
    }

    @Override
    public String selectProdFileId() {
        return goodsProductRelManager.selectProdFileId();
    }

    @Override
    public String isBindingToPricture(String targetId) {
    	return goodsProductRelManager.isBindingToPricture(targetId);
    }
    
    @Override
    public void delProdFileByTargetId(String goodsId) {
        goodsProductRelManager.delProdFileByTargetId(goodsId);
    }
}