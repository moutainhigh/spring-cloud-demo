package com.iwhalecloud.retail.warehouse.dubbo.workflow;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.warehouse.common.ResourceConst;
import com.iwhalecloud.retail.warehouse.dto.request.ResourceRequestUpdateReq;
import com.iwhalecloud.retail.warehouse.service.ResourceRequestService;
import com.iwhalecloud.retail.warehouse.service.TerminalBusinessCheckPassActionService;
import com.iwhalecloud.retail.workflow.config.InvokeRouteServiceRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Administrator on 2019/4/25.
 */
@Slf4j
@Service
public class TerminalBusinessCheckPassActionImpl implements TerminalBusinessCheckPassActionService {
    @Reference
    private ResourceRequestService requestService;

    @Override
    public ResultVO run(InvokeRouteServiceRequest params) {
        // 对应申请单ID
        String businessId = params.getBusinessId();
        // 申请单修改状态
        ResourceRequestUpdateReq updateReq = new ResourceRequestUpdateReq();
        updateReq.setMktResReqId(businessId);
        updateReq.setStatusCd(ResourceConst.MKTRESSTATE.WAIT_SPOTCHECK_MOBINT.getCode());
        ResultVO<Boolean> updatRequestVO = requestService.updateResourceRequestState(updateReq);
        log.info("TerminalSamplingInspectionNotPassActionImpl.run requestService.updateResourceRequestState updateReq={}, resp={}", JSON.toJSONString(updateReq), JSON.toJSONString(updatRequestVO));
        // 申请单ID->明细
        return ResultVO.success();
    }
}
