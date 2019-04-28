package com.iwhalecloud.retail.order2b.manager;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iwhalecloud.retail.order2b.consts.PurApplyConsts;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.PurApplyDeliveryReq;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.PurApplyReq;
import com.iwhalecloud.retail.order2b.entity.PurApply;
import com.iwhalecloud.retail.order2b.entity.PurApplyDelivery;
import com.iwhalecloud.retail.order2b.mapper.PurApplyDeliveryMapper;
import com.iwhalecloud.retail.order2b.mapper.PurApplyMapper;
import com.iwhalecloud.retail.system.common.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @auther lin.wenhui@iwhalecloud.com
 * @date 2019/4/24 14:17
 * @description 采购管理-发货记录
 */

@Component
public class PurApplyDeliveryManager {

    @Resource
    private PurApplyDeliveryMapper purApplyDeliveryMapper;

    @Resource
    private PurApplyMapper purApplyMapper;


    public int insertPurApplyDelivery(PurApplyDeliveryReq req) {
        req.setCreateDate(DateUtils.currentSysTimeForDate());
        req.setDeliveryType(PurApplyConsts.DELIVERY_TYPE);
        PurApplyDelivery purApplyDelivery = new PurApplyDelivery();
        BeanUtils.copyProperties(req, purApplyDelivery);
        return purApplyDeliveryMapper.insert(purApplyDelivery);
    }

    public int updatePurApplyStatus(PurApplyReq req) {
        String applyId = req.getApplyId();
        PurApply purApply = new PurApply();
        //更新采购申请单状态
        purApply.setStatusCd(req.getStatusCd());
        //修改条件
        UpdateWrapper<PurApply> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("APPLY_ID", applyId);
        return purApplyMapper.update(purApply, userUpdateWrapper);
    }

}
