package com.iwhalecloud.retail.promo.manager;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.promo.common.PromoConst;
import com.iwhalecloud.retail.promo.dto.MarketingActivityDTO;
import com.iwhalecloud.retail.promo.dto.req.AdvanceActivityProductInfoReq;
import com.iwhalecloud.retail.promo.dto.req.MarketingActivityAddReq;
import com.iwhalecloud.retail.promo.dto.req.MarketingActivityListReq;
import com.iwhalecloud.retail.promo.dto.resp.AdvanceActivityProductInfoResp;
import com.iwhalecloud.retail.promo.dto.resp.MarketingActivityListResp;
import com.iwhalecloud.retail.promo.entity.ActivityProduct;
import com.iwhalecloud.retail.promo.mapper.ActivityProductMapper;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.promo.entity.MarketingActivity;
import com.iwhalecloud.retail.promo.mapper.MarketingActivityMapper;
import com.iwhalecloud.retail.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import com.iwhalecloud.retail.system.entity.User;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.beans.beancontext.BeanContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class MarketingActivityManager{
    @Resource
    private MarketingActivityMapper marketingActivityMapper;

    @Resource
    private ActivityProductMapper activityProductMapper;

    @Reference
    private UserService userService;
    /**
     * 添加营销活动
     * @param marketingActivity 营销活动实体
     * @return
     */
    public int addMarketingActivity(MarketingActivity marketingActivity) {
        if(StringUtils.isEmpty(marketingActivity.getId())){
            marketingActivity.setGmtCreate(new Date());
            marketingActivity.setGmtModified(new Date());
            //is_delete 默认为 0-未删
            marketingActivity.setIsDeleted("0");
            marketingActivity.setStatus(PromoConst.STATUSCD.STATUS_CD_1.getCode());
            marketingActivity.setPayType(PromoConst.PayType.PAY_TYPE_1.getCode());
            return marketingActivityMapper.insert(marketingActivity);
        } else{
            log.info("MarketingActivityManager.addMarketingActivity marketingActivity{}",marketingActivity);
            marketingActivity.setGmtModified(new Date());
            marketingActivity.setModifier(marketingActivity.getCreator());
            return marketingActivityMapper.updateById(marketingActivity);
        }
    }

    /**
     * 查询营销活动列表
     * @param req 查询营销活动列表
     * @return
     */
    public Page<MarketingActivityListResp> listMarketingActivity(MarketingActivityListReq req) {
        Page<MarketingActivityListResp> page = new Page<>(req.getPageNo(), req.getPageSize());
        Page<MarketingActivityListResp> prodGoodsPage = marketingActivityMapper.listMarketingActivity(page, req);
        if (!CollectionUtils.isEmpty(prodGoodsPage.getRecords())){
            for (int i = 0; i< prodGoodsPage.getRecords().size(); i++){
                String userId=prodGoodsPage.getRecords().get(i).getCreator();
                if (StringUtils.isNotEmpty(userId)){
                    UserDTO userDTO= userService.getUserByUserId(userId);
                    if (userDTO != null){
                        prodGoodsPage.getRecords().get(i).setCreatorName(userDTO.getUserName());
                    }
                }
            }
        }
        log.info("MarketingActivityManager.listMarketingActivity prodGoodsPage{}",prodGoodsPage);
        return prodGoodsPage;
    }

    /**
     * 根据ID查询营销活动
     * @param id 询营销活动ID
     * @return
     */
    public MarketingActivity getMarketingActivityById(String id) {
        QueryWrapper<MarketingActivity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        queryWrapper.eq("is_deleted",PromoConst.UNDELETED);
        MarketingActivity marketingActivity = marketingActivityMapper.selectById(id);
        return marketingActivity;
    }
    /**
     * 根据ID查询营销活动
     * @param id 询营销活动ID
     * @return
     */
    public ResultVO<MarketingActivityDTO> getMarketingActivityDtoById(String id) {
        log.info("MarketingActivityManager.getMarketingActivityDtoById id={}", id);
        if (org.springframework.util.StringUtils.isEmpty(id)){
            return  ResultVO.error("营销活动ID为空");
        }
        MarketingActivityDTO marketingActivityDTO = marketingActivityMapper.getMarketingActivityDtoById(id);
        String userId=marketingActivityDTO.getCreator();
        if (StringUtils.isNotEmpty(userId)){
            UserDTO userDTO= userService.getUserByUserId(userId);
           if (userDTO == null){
               return ResultVO.successMessage("MarketingActivityManager.getMarketingActivityDtoById userDTO is null");
           }
            marketingActivityDTO.setUserName(userDTO.getUserName());
        }
        return ResultVO.success(marketingActivityDTO);
    }

    /**
     * 根据ID修改营销活动
     * @param id
     * @param status
     * @return
     */
    public Boolean updateMarketingActivityById(String id, String status) {
        MarketingActivity marketingActivity = new MarketingActivity();
        marketingActivity.setId(id);
        marketingActivity.setStatus(status);
        return marketingActivityMapper.updateById(marketingActivity) > 0;
    }
    /**
     * 删除营销活动
     * @param id
     * @param status
     * @return
     */
    public Boolean deleteMarketingActivityById(String id, String status) {
        MarketingActivity marketingActivity = new MarketingActivity();
        marketingActivity.setId(id);
        marketingActivity.setStatus(status);
        //逻辑删除 is_delete: 1--已删除
        marketingActivity.setIsDeleted("1");
        return marketingActivityMapper.updateById(marketingActivity) > 0;
    }
    /**
     * 根据营销活动编码查询营销活动列表
     * @param ids 活动编码集合
     * @return 营销活动列表
     */
    public List<MarketingActivity> listMarketingActivitysByCodes(List<String> ids, String activityType){
        QueryWrapper queryWrapper = new QueryWrapper();
        if(StringUtils.isNotBlank(activityType)){
            queryWrapper.eq(MarketingActivity.FieldNames.activityType.getTableFieldName(),activityType);
        }
        queryWrapper.in(MarketingActivity.FieldNames.id.getTableFieldName(),ids);
        queryWrapper.eq(MarketingActivity.FieldNames.isDeleted.getTableFieldName(),PromoConst.UNDELETED);
        queryWrapper.eq(MarketingActivity.FieldNames.status.getTableFieldName(),PromoConst.STATUSCD.STATUS_CD_20.getCode());
        queryWrapper.apply(MarketingActivity.FieldNames.startTime.getTableFieldName() + " <= now()");
        queryWrapper.apply(MarketingActivity.FieldNames.endTime.getTableFieldName() + " >= now()");
        return marketingActivityMapper.selectList(queryWrapper);
    }

    /**
     * 根据营销活动id查询营销活动列表
     * @param idList 活动编码集合
     * @return 营销活动列表
     */
    public List<MarketingActivity> listMarketingActivitiesByIds(List<String> idList){
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.in(MarketingActivity.FieldNames.id.getTableFieldName(), idList);
        return marketingActivityMapper.selectList(queryWrapper);
    }
    /**
     * 根据活动ID更新状态
     * @param marketingActivityId
     * @return
     */
    public ResultVO<Boolean> updateMarketingActivityStatus(String marketingActivityId){
        MarketingActivity marketingActivity = new MarketingActivity();
        marketingActivity.setId(marketingActivityId);
        marketingActivity.setStatus(PromoConst.STATUSCD.STATUS_CD_10.getCode());
        return ResultVO.success(marketingActivityMapper.updateById(marketingActivity) > 0);
    }


    /**
     * 根据活动id查询活动信息
     * @param marketingActivityId
     * @return
     */
    public MarketingActivity queryMarketingActivity(String marketingActivityId){
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(MarketingActivity.FieldNames.id.getTableFieldName(),marketingActivityId);
        queryWrapper.eq(MarketingActivity.FieldNames.isDeleted.getTableFieldName(),PromoConst.IsDelete.IS_DELETE_CD_0.getCode());
        return marketingActivityMapper.selectOne(queryWrapper);
    }

    /**
     * 更新预售活动的规则
     * @param marketingActivity
     * @return
     */
    public Integer updatePreSaleActivityRule (MarketingActivity marketingActivity){
        QueryWrapper queryWrapper = new QueryWrapper();
        marketingActivity.setGmtModified(new Date());
        queryWrapper.eq(MarketingActivity.FieldNames.isDeleted.getTableFieldName(),PromoConst.IsDelete.IS_DELETE_CD_0.getCode());
        queryWrapper.eq(MarketingActivity.FieldNames.id.getTableFieldName(),marketingActivity.getId());
        return marketingActivityMapper.update(marketingActivity,queryWrapper);
    }

    /**
     * 查询预售活动的单个产品信息
     * @param advanceActivityProductInfoReq
     * @return
     */
    public AdvanceActivityProductInfoResp getAdvanceActivityProductInfo(AdvanceActivityProductInfoReq advanceActivityProductInfoReq) {
        return marketingActivityMapper.getAdvanceActivityProductInfo(advanceActivityProductInfoReq);
    }

    /**
     * 查询有效的活动列表
     * @return
     */
    public List<MarketingActivity> queryInvlidMarketingActivity(String activityType) {
        QueryWrapper queryWrapper = new QueryWrapper();

        if (StringUtils.isNotEmpty(activityType)) {
            queryWrapper.eq(MarketingActivity.FieldNames.activityType.getTableFieldName(),activityType);
        }

        //todo 后面需要改成铜鼓数据库类型获取。oracle改为sysdate
        //小于等于 <=
        queryWrapper.apply(MarketingActivity.FieldNames.startTime.getTableFieldName() + " <= now()");
        //大于等于 >=
        queryWrapper.apply(MarketingActivity.FieldNames.endTime.getTableFieldName() + " >= now()");
        queryWrapper.eq(MarketingActivity.FieldNames.isDeleted.getTableFieldName(),PromoConst.IsDelete.IS_DELETE_CD_0.getCode());

        return marketingActivityMapper.selectList(queryWrapper);
    }

    /**
     * 查询失效的活动
     * @param marketingActivityListReq
     * @return
     */
    public List<MarketingActivityListResp> queryInvalidActivity(MarketingActivityListReq marketingActivityListReq){
        marketingActivityListReq.setActivityType(PromoConst.ACTIVITYTYPE.PRESUBSIDY.getCode());
        marketingActivityListReq.setActivityName(marketingActivityListReq.getActivityName().trim());
        return marketingActivityMapper.queryInvalidActivity(marketingActivityListReq);
    }
    /**
     * 通过产品ID查询某一类型的活动列表
     * @param productId
     * @param activityType
     * @return
     */
    public List<MarketingActivity> queryActivityByProductId(String productId,String activityType) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(ActivityProduct.FieldNames.productId.getTableFieldName(),productId);
        List<ActivityProduct> activityProductList = activityProductMapper.selectList(queryWrapper);
        List<MarketingActivity> marketingActivityList = new ArrayList<>();
        if(activityProductList != null && activityProductList.size() > 0) {
            List activityIds = new ArrayList();
            for (int i = 0; i < activityProductList.size(); i++) {
                activityIds.add(activityProductList.get(i).getMarketingActivityId());
            }
            queryWrapper = new QueryWrapper();
            queryWrapper.in(MarketingActivity.FieldNames.id.getTableFieldName(), activityIds);
            queryWrapper.eq(MarketingActivity.FieldNames.activityType.getTableFieldName(), activityType);
            queryWrapper.eq(MarketingActivity.FieldNames.isDeleted.getTableFieldName(),PromoConst.IsDelete.IS_DELETE_CD_0.getCode());
            marketingActivityList = marketingActivityMapper.selectList(queryWrapper);
        }
        return marketingActivityList;
    }
}
