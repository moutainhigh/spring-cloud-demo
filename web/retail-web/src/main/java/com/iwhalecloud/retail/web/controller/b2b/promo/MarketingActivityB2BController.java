package com.iwhalecloud.retail.web.controller.b2b.promo;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.promo.common.PromoConst;
import com.iwhalecloud.retail.promo.dto.ActivityChangeDetailDTO;
import com.iwhalecloud.retail.promo.dto.ActivityParticipantDTO;
import com.iwhalecloud.retail.promo.dto.ActivityScopeDTO;
import com.iwhalecloud.retail.promo.dto.req.*;
import com.iwhalecloud.retail.promo.dto.resp.*;
import com.iwhalecloud.retail.promo.service.ActivityChangeService;
import com.iwhalecloud.retail.promo.service.MarketingActivityService;
import com.iwhalecloud.retail.rights.dto.request.OrderCouponListReq;
import com.iwhalecloud.retail.rights.dto.response.OrderCouponListResp;
import com.iwhalecloud.retail.rights.service.CouponInstService;
import com.iwhalecloud.retail.system.common.SystemConst;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.system.dto.request.UserListReq;
import com.iwhalecloud.retail.system.service.UserService;
import com.iwhalecloud.retail.web.annotation.UserLoginToken;
import com.iwhalecloud.retail.web.controller.b2b.promo.request.MarketingActivityListActivityReq;
import com.iwhalecloud.retail.web.controller.b2b.promo.request.MarketingActivityListCouponyReq;
import com.iwhalecloud.retail.web.controller.b2b.promo.request.MarketingActivityListReliefReq;
import com.iwhalecloud.retail.web.interceptor.UserContext;
import com.iwhalecloud.retail.web.utils.FastDFSImgStrJoinUtil;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author mzl
 * @date 2019/1/31
 */
@RestController
@RequestMapping("/api/b2b/promo")
@Slf4j
@Api(value = "营销活动:", tags = {"营销活动:MarketingActivityB2BController"})
public class MarketingActivityB2BController {

    @Reference
    private MarketingActivityService marketingActivityService;

    @Reference
    private CouponInstService couponInstService;

    @Reference
    ActivityChangeService activityChangeService;

    @Reference
    private UserService userService;

    @Value("${fdfs.showUrl}")
    private String dfsShowIp;

    @ApiOperation(value = "创建活动", notes = "创建活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/addMarketingActivity")
    @UserLoginToken
    public ResultVO<MarketingActivityAddResp> addMarketingActivity(@RequestBody MarketingActivityAddReq req) {
        log.info("MarketingActivityB2BController addMarketingActivity req={} ", JSON.toJSON(req));
        //随机生成营销活动code   日期+随机数
        String str = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        // 获取4位随机数
        int rannum = (int) (new Random().nextDouble() * (9999 - 1000 + 1)) + 1000;
        //将code添加到入库参数中
        req.setCode(str + rannum);
        //处理活动范围对象和活动参与对象
        handleActivityScopeAndParticipant(req);
        UserDTO userDTO = UserContext.getUser();
        if (!Objects.isNull(userDTO)) {
            req.setUserId(userDTO.getUserId());
            req.setUserName(userDTO.getUserName());
            req.setSysPostName(userDTO.getUserName());
            req.setOrgId(userDTO.getOrgId());
            req.setUserFounder(userDTO.getUserFounder());
        }
        if (!StringUtils.isEmpty(req.getPageImgUrl())) {
            req.setPageImgUrl(req.getPageImgUrl().replaceAll(dfsShowIp, ""));
        }
        if (!StringUtils.isEmpty(req.getActivityUrl())) {
            req.setActivityUrl(req.getActivityUrl().replaceAll(dfsShowIp, ""));
        }
        if (!StringUtils.isEmpty(req.getTopImgUrl())) {
            req.setTopImgUrl(req.getTopImgUrl().replaceAll(dfsShowIp, ""));
        }
        return marketingActivityService.addMarketingActivity(req);
    }

    @ApiOperation(value = "根据条件查询活动列表", notes = "条件分页查询")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/listMarketingActivity")
    @UserLoginToken
    public ResultVO<Page<MarketingActivityListResp>> listMarketingActivity(@RequestBody MarketingActivityListReq req) {
        //如果不是管理员，根据当前登录帐号过滤
        if (!UserContext.isAdminType()) {
            req.setCreator(UserContext.getUserId());
        } else if (UserContext.isCityAdminType()) {
            // zhongwenlong
            // 判断是否是地市管理员 是：只能查当前地市的管理员创建的地市级活动
            if (UserContext.isCityAdminType()) {
                UserListReq userListReq = new UserListReq();
                userListReq.setLanId(UserContext.getUser().getLanId());
                userListReq.setUserFounderList(Lists.newArrayList(SystemConst.USER_FOUNDER_9));
                List<UserDTO> userDTOList = userService.getUserList(userListReq);
                if (CollectionUtils.isNotEmpty(userDTOList)) {
                    List<String> userIdList = userDTOList.stream().map(UserDTO::getUserId).collect(Collectors.toList());
                    req.setCreatorIdList(userIdList);
                } else {
                    // 没有相应条件下的 用户  塞个 空字符串 使查询结果为空 (理论上不会为空，起码会查出当前用户自己）
                    req.setCreatorIdList(Lists.newArrayList(""));
                }
                // 设置 活动级别 为地市级活动
                req.setActivityLevel(PromoConst.ActivityLevel.LEVEL_2.getCode());
            }
        }

        if ("Invalid date".equals(req.getStartTimeS())) {
            req.setStartTimeS("");
        }
        if ("Invalid date".equals(req.getStartTimeE())) {
            req.setStartTimeE("");
        }
        if ("Invalid date".equals(req.getEndTimeS())) {
            req.setEndTimeS("");
        }
        if ("Invalid date".equals(req.getEndTimeE())) {
            req.setEndTimeE("");
        }

        log.info("MarketingActivityB2BController listMarketingActivity MarketingActivityListReq={} ", req);
        return marketingActivityService.listMarketingActivity(req);
    }

    @ApiOperation(value = "查询活动基本信息", notes = "查询活动基本信息")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @RequestMapping(value = "/queryMarketingActivity", method = RequestMethod.GET)
    public ResultVO<MarketingActivityDetailResp> queryMarketingActivity(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController queryMarketingActivity marketingActivityCode={} ", activityId);
        ResultVO<MarketingActivityDetailResp> respResultVO = marketingActivityService.queryMarketingActivity(activityId);
        MarketingActivityDetailResp marketingActivityDetailResp = respResultVO.getResultData();
        List<ActivityScopeDTO> scopeList = marketingActivityDetailResp.getActivityScopeList();
        List<ActivityParticipantDTO> activityParticipantList = marketingActivityDetailResp.getActivityParticipantList();
        if (scopeList != null && scopeList.size() > 0) {
            if (PromoConst.ActivityScopeType.ACTIVITY_SCOPE_TYPE_20.getCode().equals(marketingActivityDetailResp.getActivityScopeType())) {
                for (int i = 0; i < scopeList.size(); i++) {
                    scopeList.get(i).setMerchantName(scopeList.get(i).getSupplierName());
                    scopeList.get(i).setMerchantCode(scopeList.get(i).getSupplierCode());
                    scopeList.get(i).setMerchantId(scopeList.get(i).getSupplierCode());
                }
            }
        }
        if (activityParticipantList != null && activityParticipantList.size() > 0) {
            if (PromoConst.ActivityParticipantType.ACTIVITY_PARTICIPANT_TYPE_20.getCode().equals(marketingActivityDetailResp.getActivityParticipantType())) {
                for (int i = 0; i < activityParticipantList.size(); i++) {
                    activityParticipantList.get(i).setMerchantName(activityParticipantList.get(i).getShopName());
                    activityParticipantList.get(i).setMerchantCode(activityParticipantList.get(i).getShopCode());
                    activityParticipantList.get(i).setMerchantId(activityParticipantList.get(i).getShopCode());
                }
            }

        }
        //将活动详情中图片信息补充全路径
        setFullImageUrl(marketingActivityDetailResp);
        return respResultVO;
    }


    @ApiOperation(value = "删除活动", notes = "删除活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/deleteMarketingActivity")
    public ResultVO<Boolean> deleteMarketingActivity(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController deleteMarketingActivity id={} ", activityId);
        CancelMarketingActivityStatusReq req = new CancelMarketingActivityStatusReq();
        req.setActivityId(activityId);
        return marketingActivityService.cancleMarketingActivity(req);
    }

    @ApiOperation(value = "终止活动", notes = "终止活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/stopb2bactivity")
    public ResultVO<Boolean> stopMarketingActivity(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController stopb2bactivity id={} ", activityId);
        EndMarketingActivityStatusReq req = new EndMarketingActivityStatusReq();
        req.setActivityId(activityId);
        return marketingActivityService.endMarketingActivity(req);
    }

    @ApiOperation(value = "变更活动", notes = "变更活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/updateMarketingActivity")
    @UserLoginToken
    public ResultVO<Boolean> updateMarketingActivity(@RequestBody MarketingActivityAddReq req) {
        log.info("MarketingActivityB2BController updateMarketingActivity req={} ", JSON.toJSON(req));
        //处理活动范围对象和活动参与对象
        handleActivityScopeAndParticipant(req);
        UserDTO userDTO = UserContext.getUser();
        req.setUserId(userDTO.getUserId());
        req.setUserName(userDTO.getUserName());
        req.setModifier(userDTO.getUserName());
        req.setSysPostName(userDTO.getSysPostName());
        req.setOrgId(userDTO.getOrgId());
        if (!StringUtils.isEmpty(req.getPageImgUrl())) {
            req.setPageImgUrl(req.getPageImgUrl().replaceAll(dfsShowIp, ""));
        }
        if (!StringUtils.isEmpty(req.getActivityUrl())) {
            req.setActivityUrl(req.getActivityUrl().replaceAll(dfsShowIp, ""));
        }
        if (!StringUtils.isEmpty(req.getTopImgUrl())) {
            req.setTopImgUrl(req.getTopImgUrl().replaceAll(dfsShowIp, ""));
        }
        return marketingActivityService.updateMarketingActivity(req);
    }

    /**
     * 处理活动范围对象和活动参与对象
     *
     * @param req
     */
    private void handleActivityScopeAndParticipant(MarketingActivityAddReq req) {
        //从入参中获取活动范围对象
        List<ActivityScopeDTO> scopeList = req.getActivityScopeList();
        //从入参中获取活动参与对象
        List<ActivityParticipantDTO> activityParticipantList = req.getActivityParticipantList();
        //解析处理活动范围对象，范围类型分为：10地区，20商家。
        if (scopeList != null && scopeList.size() > 0) {
            for (int i = 0; i < scopeList.size(); i++) {
                ActivityScopeDTO scopeDTO = scopeList.get(i);
                if(scopeDTO==null){
                    continue;
                }
                if (PromoConst.ActivityScopeType.ACTIVITY_SCOPE_TYPE_20.getCode().equals(req.getActivityScopeType())) {
                    scopeDTO.setSupplierName(scopeDTO.getMerchantName());
                    scopeDTO.setSupplierCode(scopeDTO.getMerchantCode());
                } else {
                    if (scopeDTO.getRegionId().length() > 3) {
                        scopeDTO.setCity(scopeDTO.getRegionId());
                    } else {
                        scopeDTO.setLanId(scopeDTO.getRegionId());
                    }
                }
            }
        }
        //解析处理活动范围对象，参与类型分为：10地区，20商家。
        if (activityParticipantList != null && activityParticipantList.size() > 0) {
            for (int i = 0; i < activityParticipantList.size(); i++) {
                ActivityParticipantDTO participantDTO = activityParticipantList.get(i);
                if(participantDTO==null){
                    continue;
                }
                if (PromoConst.ActivityParticipantType.ACTIVITY_PARTICIPANT_TYPE_20.getCode().equals(req.getActivityParticipantType())) {
                    participantDTO.setShopName(participantDTO.getMerchantName());
                    participantDTO.setShopCode(participantDTO.getMerchantCode());
                } else if(PromoConst.ActivityParticipantType.ACTIVITY_PARTICIPANT_TYPE_10.getCode().equals(req.getActivityParticipantType())){
                    if (participantDTO.getRegionId().length() > 3) {
                        participantDTO.setCity(participantDTO.getRegionId());
                    } else {
                        participantDTO.setLanId(participantDTO.getRegionId());
                    }
                }else{
                    String filterValue = participantDTO.getFilterValue();
                    if(StringUtils.isNotEmpty(filterValue)){
                        participantDTO.setFilterValue(StringEscapeUtils.unescapeHtml(filterValue));
                    }
                }
            }
        }
    }

    @ApiOperation(value = "商品适用活动查询", notes = "查询B2B商品适用活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "listGoodsActivity")
    public ResultVO<List<MarketingGoodsActivityQueryResp>> listGoodsActivity(@RequestBody MarketingActivityListActivityReq request) {
        log.info(MarketingActivityB2BController.class.getName() + " listGoodsActivity request={} ", request);
        MarketingActivityQueryByGoodsReq temp = new MarketingActivityQueryByGoodsReq();
        BeanUtils.copyProperties(request, temp);

        return marketingActivityService.listGoodsMarketingActivitys(temp);
    }

    @ApiOperation(value = "商品适用的预售活动查询", notes = "商品适用的预售活动查询")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @GetMapping(value = "listGoodsAdvanceActivity")
    public ResultVO<List<MarketingGoodsActivityQueryResp>> listGoodsAdvanceActivity
            (@RequestParam(value = "goodsId") @ApiParam("商品ID") String goodsId) {
        log.info(MarketingActivityB2BController.class.getName() + " listGoodsAdvanceActivity goodsId={}", goodsId);

        return marketingActivityService.listGoodsAdvanceActivity(goodsId);
    }

    @ApiOperation(value = "商品适用卡券", notes = "商品可领券的优惠券信息")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "listGoodsCoupon")
    public ResultVO listGoodsCoupon(@RequestBody MarketingActivityListCouponyReq request) {
        log.info(MarketingActivityB2BController.class.getName() + " listGoodsCoupon request={} ", request);
        MarketingActivityQueryByGoodsReq temp = new MarketingActivityQueryByGoodsReq();
        BeanUtils.copyProperties(request, temp);

        return marketingActivityService.listGoodsMarketingCouponActivitys(temp);
    }

    @ApiOperation(value = "商品适用减免", notes = "商品适用减免信息")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "listGoodsRelief")
    public ResultVO listGoodsRelief(@RequestBody MarketingActivityListReliefReq request) {
        log.info(MarketingActivityB2BController.class.getName() + " listGoodsRelief request={} ", request);
        MarketingActivityQueryByGoodsReq temp = new MarketingActivityQueryByGoodsReq();
        BeanUtils.copyProperties(request, temp);
        return marketingActivityService.listGoodsMarketingReliefActivitys(temp);
    }

    @ApiOperation(value = "查询订单优惠卷", notes = "查询订单优惠卷")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/qryOrderPromotionList")
    public ResultVO<OrderCouponListResp> qryOrderPromotionList(@RequestBody OrderCouponListReq req) {
        log.info("MarketingActivityB2BController qryOrderPromotionList req={} ", JSON.toJSON(req));
        return couponInstService.orderCouponList(req);
    }

    @ApiOperation(value = "查询活动详情", notes = "查询活动详情")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @RequestMapping(value = "/queryMarketingActivityInfo", method = RequestMethod.GET)
    public ResultVO<MarketingActivityInfoResp> queryMarketingActivityInfo(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController queryMarketingActivityInfo marketingActivityCode={} ", activityId);
        ResultVO<MarketingActivityInfoResp> respResultVO = marketingActivityService.queryMarketingActivityInfo(activityId);
        if (respResultVO.getResultData()!=null&&respResultVO.getResultData().getMarketingActivityDetailResp()!=null){
            //将活动详情中图片信息补充全路径
            setFullImageUrl(respResultVO.getResultData().getMarketingActivityDetailResp());
        }
        return respResultVO;
    }


    @ApiOperation(value = "查询活动详情", notes = "查询活动详情")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @RequestMapping(value = "/queryMarketingActivityInfor", method = RequestMethod.GET)
    public ResultVO<MarketingActivityInfoResp> queryMarketingActivityInfor(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController queryMarketingActivityInfo marketingActivityCode={} ", activityId);
        ResultVO<MarketingActivityInfoResp> respResultVO = marketingActivityService.queryMarketingActivityInfor(activityId);
        if (respResultVO.getResultData()!=null&&respResultVO.getResultData().getMarketingActivityDetailResp()!=null){
            //将活动详情中图片信息补充全路径
            setFullImageUrl(respResultVO.getResultData().getMarketingActivityDetailResp());
        }
        return respResultVO;
    }

    /**
     * 查询活动变更详情
     *
     * @param activityId
     * @return
     */
    @ApiOperation(value = "查询活动变更详情", notes = "查询活动变更详情")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @RequestMapping(value = "/queryMarketingActivityChangeInfo", method = RequestMethod.GET)
    public ResultVO<ActivityChangeResp> queryMarketingActivityChangeInfo(@RequestParam String activityId) {
        log.info("MarketingActivityB2BController queryMarketingActivityChangeInfo activityId={} ", activityId);
        ResultVO<ActivityChangeResp> activityChangeResp = activityChangeService.queryMarketingActivityChangeInfo(activityId);
        //补充图片字段全路径
        List<ActivityChangeDetailDTO> activityChangeDetailDTOList = null;
        if (activityChangeResp != null && activityChangeResp.getResultData() != null) {
            activityChangeDetailDTOList = activityChangeResp.getResultData().getActivityChangeDetailDTOList();
        }
        if (activityChangeDetailDTOList != null) {
            for (ActivityChangeDetailDTO detailDTO : activityChangeDetailDTOList) {
                if (detailDTO.getChangeField() != null && detailDTO.getChangeField().contains("URL")) {
                    detailDTO.setOldValue(FastDFSImgStrJoinUtil.fullImageUrl(detailDTO.getOldValue(), dfsShowIp, true));
                    detailDTO.setNewValue(FastDFSImgStrJoinUtil.fullImageUrl(detailDTO.getNewValue(), dfsShowIp, true));
                }
            }
        }

        return activityChangeResp;
    }

    @ApiOperation(value = "查询供货商允许参加的活动", notes = "查询供货商允许参加的活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/queryMarketingActivityQueryBySupplier")
    @UserLoginToken
    public ResultVO<List<MarketingActivityQueryBySupplierResp>> queryMarketingActivityQueryBySupplier(@RequestBody MarketingActivityQueryBySupplierReq req) {
        //如果是商家用会话中的商家ID，否则用传进来的
        if (UserContext.isMerchant()) {
            req.setSupplierId(UserContext.getMerchantId());
        }
        log.info("MarketingActivityB2BController queryMarketingActivityQueryBySupplier-->req={} ", JSON.toJSONString(req));
        ResultVO<List<MarketingActivityQueryBySupplierResp>> resultVO = marketingActivityService.queryMarketingActivityQueryBySupplier(req);
        log.info("MarketingActivityB2BController queryMarketingActivityQueryBySupplier-->resultVO={} ", JSON.toJSONString(resultVO));
        return resultVO;
    }

    @ApiOperation(value = "查询已经结束的前置补贴活动", notes = "查询已经结束的前置补贴活动")
    @ApiResponses({
            @ApiResponse(code = 400, message = "请求参数没填好"),
            @ApiResponse(code = 404, message = "请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value = "/queryInvalidActivity")
    public ResultVO<List<MarketingActivityListResp>> queryInvalidActivity(@RequestBody MarketingActivityListReq marketingActivityListReq) {
        log.info("MarketingActivityB2BController queryPreSubsidyActivity-->marketingActivityListReq={} ", JSON.toJSON(marketingActivityListReq));
        return marketingActivityService.queryInvalidActivity(marketingActivityListReq);
    }

    /**
     * 将活动详情中图片信息补充全路径
     * @param marketingActivityDetailResp
     */
    private void setFullImageUrl(MarketingActivityDetailResp marketingActivityDetailResp){
        if(marketingActivityDetailResp==null){
            return;
        }
        if (!StringUtils.isEmpty(marketingActivityDetailResp.getPageImgUrl())) {
            String newImageFile = FastDFSImgStrJoinUtil.fullImageUrl(marketingActivityDetailResp.getPageImgUrl(), dfsShowIp, true);
            marketingActivityDetailResp.setPageImgUrl(newImageFile);
        }
        if (!StringUtils.isEmpty(marketingActivityDetailResp.getTopImgUrl())) {
            String newImageFile = FastDFSImgStrJoinUtil.fullImageUrl(marketingActivityDetailResp.getTopImgUrl(), dfsShowIp, true);
            marketingActivityDetailResp.setTopImgUrl(newImageFile);
        }
    }
}
