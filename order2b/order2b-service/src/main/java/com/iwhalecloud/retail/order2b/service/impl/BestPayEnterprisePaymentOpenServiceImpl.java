package com.iwhalecloud.retail.order2b.service.impl;

import com.alibaba.fastjson.JSON;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.order2b.busiservice.BPEPPayLogService;
import com.iwhalecloud.retail.order2b.busiservice.PayService;
import com.iwhalecloud.retail.order2b.busiservice.UpdateOrderFlowService;
import com.iwhalecloud.retail.order2b.consts.PayConsts;
import com.iwhalecloud.retail.order2b.consts.order.ActionFlowType;
import com.iwhalecloud.retail.order2b.consts.order.OrderPayType;
import com.iwhalecloud.retail.order2b.dto.base.CommonResultResp;
import com.iwhalecloud.retail.order2b.dto.response.OffLinePayResp;
import com.iwhalecloud.retail.order2b.dto.response.OrderPayInfoResp;
import com.iwhalecloud.retail.order2b.dto.response.ToPayResp;
import com.iwhalecloud.retail.order2b.dto.resquest.order.PayOrderRequest;
import com.iwhalecloud.retail.order2b.dto.resquest.order.UpdateOrderStatusRequest;
import com.iwhalecloud.retail.order2b.dto.resquest.pay.AsynNotifyReq;
import com.iwhalecloud.retail.order2b.dto.resquest.pay.OffLinePayReq;
import com.iwhalecloud.retail.order2b.dto.resquest.pay.OrderPayInfoReq;
import com.iwhalecloud.retail.order2b.dto.resquest.pay.ToPayReq;
import com.iwhalecloud.retail.order2b.entity.AdvanceOrder;
import com.iwhalecloud.retail.order2b.entity.Order;
import com.iwhalecloud.retail.order2b.manager.AdvanceOrderManager;
import com.iwhalecloud.retail.order2b.manager.OrderManager;
import com.iwhalecloud.retail.order2b.model.SaveLogModel;
import com.iwhalecloud.retail.order2b.reference.MemberInfoReference;
import com.iwhalecloud.retail.order2b.service.BestPayEnterprisePaymentService;
import com.iwhalecloud.retail.partner.common.PartnerConst;
import com.iwhalecloud.retail.partner.dto.MerchantAccountDTO;
import com.iwhalecloud.retail.partner.dto.req.MerchantAccountListReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Slf4j
public class BestPayEnterprisePaymentOpenServiceImpl implements BestPayEnterprisePaymentService {

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private MemberInfoReference memberInfoReference;

    @Autowired
    private BPEPPayLogService bpepPayLogService;

    @Autowired
    private PayService payService;

    @Autowired
    private AdvanceOrderManager advanceOrderManager;

    @Autowired
    private  UpdateOrderFlowService updateOrderFlowService;

    @Override
    public ResultVO<ToPayResp> toPay(ToPayReq req) {
        log.info("BestPayEnterprisePaymentImpl.toPay req={}", JSON.toJSONString(req));
        Order order = orderManager.getOrderById(req.getOrderId());
        if (null == order) {
            log.info("BestPayEnterprisePaymentImpl.toPay orderManager.getOrderById resp is null. req={}", JSON.toJSONString(req));
            return ResultVO.error("订单不存在");
        } else {
            log.info("BestPayEnterprisePaymentImpl.toPay orderManager.getOrderById req={} resp={}", JSON.toJSONString(req), JSON.toJSONString(order));
        }

        /**
         * 校验订单状态
         */
        UpdateOrderStatusRequest statusRequest=new UpdateOrderStatusRequest();
        BeanUtils.copyProperties(req,statusRequest);
        statusRequest.setFlowType(req.getOperationType());
        CommonResultResp  checkResp= updateOrderFlowService.checkFlowType(statusRequest,order);
        if (checkResp.isFailure()) {
            return ResultVO.error(checkResp.getResultMsg());
        }
        String amount = "0";
        if (req.getOperationType().equals(ActionFlowType.ORDER_HANDLER_ZF.getCode())) {
            amount = req.getOrderAmount();
        } else {
            AdvanceOrder advanceOrder = advanceOrderManager.getAdvanceOrderByOrderId(order.getOrderId());
            if (req.getOperationType().equals(ActionFlowType.ORDER_HANDLER_DJZF.getCode())) {
                amount = String.valueOf(advanceOrder.getAdvanceAmount());
            } else if (req.getOperationType().equals(ActionFlowType.ORDER_HANDLER_WKZF.getCode())) {
                amount = String.valueOf(advanceOrder.getRestAmount());
            } else {
                return ResultVO.error("订单支付类型有误");
            }
        }
        MerchantAccountListReq merchantAccountListReq = new MerchantAccountListReq();
        merchantAccountListReq.setMerchantId(order.getMerchantId());
        merchantAccountListReq.setAccountType(PartnerConst.MerchantAccountTypeEnum.BEST_PAY.getType());
        ResultVO<List<MerchantAccountDTO>> merchantAccountList = memberInfoReference.listMerchantAccount(merchantAccountListReq);
        log.info("BestPayEnterprisePaymentImpl.toPay merchantAccountService.listMerchantAccount req={} resp={}", JSON.toJSONString(req), JSON.toJSONString(merchantAccountList.getResultData().size()));
        if (!merchantAccountList.isSuccess() || CollectionUtils.isEmpty(merchantAccountList.getResultData())) {
            return ResultVO.error("账号没有配置");
        }
        String orgLoginCode = merchantAccountList.getResultData().get(0).getAccount();
        String operationType = req.getOperationType();
        ToPayResp resp = bpepPayLogService.handlePayData(order.getOrderId(), amount, orgLoginCode, operationType);
        return ResultVO.success(resp);
    }

    @Override
    public ResultVO asynNotify(AsynNotifyReq req) {
        log.info("BestPayEnterprisePaymentOpenServiceImpl.asynNotify req={}", JSON.toJSONString(req));
        boolean check = bpepPayLogService.checkNotifyData(req);
        if (check) {
            String orderStatus = req.getORDERSTATUS();
            if ("1".equals(orderStatus)) {
                // 支付成功
                OrderPayInfoResp orderPayInfoResp = this.bpepPayLogService.qryOrderPayInfoById(req.getORDERID());
                // 已通知过，幂等操作
                if (orderPayInfoResp.getPayStatus().equals(PayConsts.PAY_STATUS_2)) {
                    return ResultVO.success();
                }
                if (null == orderPayInfoResp) {
                    return ResultVO.error("通知异常:没有支付订单");
                }
                String orderId = orderPayInfoResp.getOrderId();
                String payId = req.getORDERID();
                PayOrderRequest payOrderRequest = new PayOrderRequest();
                payOrderRequest.setOrderId(orderId);
                payOrderRequest.setFlowType(orderPayInfoResp.getOperationType());
                payOrderRequest.setPaymoney(Double.parseDouble(req.getORDERAMOUNT()));
                payOrderRequest.setPayType(OrderPayType.PAY_TYPE_1.getCode());
                payService.pay(payOrderRequest);
                SaveLogModel saveLogModel = new SaveLogModel();
                saveLogModel.setPayId(payId);
                saveLogModel.setOrderId(orderId);
                saveLogModel.setOrderAmount(req.getORDERAMOUNT());
                saveLogModel.setPayStatus(PayConsts.PAY_STATUS_2);
                saveLogModel.setOperationType(orderPayInfoResp.getOperationType());
                int i = bpepPayLogService.updateLog(saveLogModel);
            }else if ("0".equals(orderStatus)) {
                OrderPayInfoResp orderPayInfoResp = this.bpepPayLogService.qryOrderPayInfoById(req.getORDERID());
                // 已通知过，幂等操作
                if (orderPayInfoResp.getPayStatus().equals(PayConsts.PAY_STATUS_0)) {
                    return ResultVO.success();
                }
                String orderId = orderPayInfoResp.getOrderId();
                String payId = req.getORDERID();
                SaveLogModel saveLogModel = new SaveLogModel();
                saveLogModel.setPayId(payId);
                saveLogModel.setOrderId(orderId);
                saveLogModel.setOrderAmount(req.getORDERAMOUNT());
                saveLogModel.setPayStatus(PayConsts.PAY_STATUS_4);
                saveLogModel.setOperationType(orderPayInfoResp.getOperationType());
                int i = bpepPayLogService.updateLog(saveLogModel);
            }
            return ResultVO.success();
        }
        return ResultVO.error("通知异常");
    }

    @Override
    public ResultVO<OrderPayInfoResp> qryOrderPayInfo(OrderPayInfoReq req) {
        log.info("BestPayEnterprisePaymentOpenServiceImpl.qryOrderPayInfo req={}", JSON.toJSONString(req));
        return ResultVO.success(this.bpepPayLogService.qryOrderPayInfo(req));
    }

    @Override
    public ResultVO<OffLinePayResp> offLinePay(OffLinePayReq req) {
        log.info("BestPayEnterprisePaymentOpenServiceImpl.offLinePay req={}", JSON.toJSONString(req));
        int i = bpepPayLogService.offLinePay(req);
        return ResultVO.success();
    }

}
