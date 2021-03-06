package com.iwhalecloud.retail.order2b.service;

import com.iwhalecloud.retail.order2b.dto.model.report.ReportOrderShopRankDTO;
import com.iwhalecloud.retail.order2b.dto.model.report.ReportOrderTimeIntervalDTO;
import com.iwhalecloud.retail.order2b.dto.resquest.report.ReportOrderOpeDeskReq;

import java.util.List;

/**
 * 运营人员工作台数据展示接口
 */
public interface OrderOperationDeskService {

    /**
     * 获取订单数（区域限制）
     * @return
     */
    int getOrderCountByArea(ReportOrderOpeDeskReq req);

    /**
     * 获取销售额（区域限制）
     * @return
     */
    double getOrderAmountByArea(ReportOrderOpeDeskReq req);

    /**
     * 获取商品销售数量（区域限制）
     * @return
     */
    int getSaleCountByArea(ReportOrderOpeDeskReq req);

    /**
     * 获取厅店消费额排行榜
     */
    List<ReportOrderShopRankDTO> getShopSaleAmountRankByArea(ReportOrderOpeDeskReq req);

    /**
     * 按天获取销售额统计量
     * @return
     */
    List<ReportOrderTimeIntervalDTO> getTimeIntervalAmountByArea(ReportOrderOpeDeskReq req) throws Exception;
}
