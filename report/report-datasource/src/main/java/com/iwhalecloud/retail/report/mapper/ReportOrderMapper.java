package com.iwhalecloud.retail.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.report.dto.request.ReportOrderDaoReq;
import com.iwhalecloud.retail.report.dto.request.ReportOrderNbrDaoReq;
import com.iwhalecloud.retail.report.dto.response.ReportOrderNbrResp;
import com.iwhalecloud.retail.report.dto.response.ReportOrderResp;
import com.iwhalecloud.retail.report.entity.RptOrderOperatingDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportOrderMapper extends BaseMapper<RptOrderOperatingDay> {
	//订单明细报表
 public Page<ReportOrderResp> ListReportOrder(Page<ReportOrderResp> page,@Param("req") ReportOrderDaoReq req);
 
 public List<ReportOrderResp> ListReportOrder(@Param("req") ReportOrderDaoReq req);
 
 public List<ReportOrderNbrResp> ListReportOrderNbr(@Param("itemId")  String itemId);
 
 public Page<ReportOrderNbrResp> ListReportOrderNbr3(Page<ReportOrderNbrResp> page,@Param("req")  ReportOrderNbrDaoReq req);

 
}
