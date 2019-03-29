package com.iwhalecloud.retail.web.controller.report;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.oms.OmsCommonConsts;
import com.iwhalecloud.retail.report.dto.request.ReportDeSaleDaoReq;
import com.iwhalecloud.retail.report.dto.request.ReportStorePurchaserReq;
import com.iwhalecloud.retail.report.dto.response.ProductListAllResp;
import com.iwhalecloud.retail.report.dto.response.ReportDeSaleDaoResq;
import com.iwhalecloud.retail.report.dto.response.ReportStorePurchaserResq;
import com.iwhalecloud.retail.report.service.IReportDataInfoService;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.system.dto.request.RegionsListReq;
import com.iwhalecloud.retail.system.dto.response.RegionsGetResp;
import com.iwhalecloud.retail.system.service.RegionsService;
import com.iwhalecloud.retail.web.controller.BaseController;
import com.iwhalecloud.retail.web.controller.b2b.order.dto.ExcelTitleName;
import com.iwhalecloud.retail.web.controller.b2b.order.service.DeliveryGoodsResNberExcel;
import com.iwhalecloud.retail.web.controller.system.RegionController;
import com.iwhalecloud.retail.web.interceptor.UserContext;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;


/**
 * @author liweisong
 * @date 2019-02-18
 * 门店报表
 */
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/reportDataInfo")
public class ReportDataInfoController extends BaseController {

    @Reference
    private IReportDataInfoService iReportDataInfoService;
    
    @Reference
    private RegionsService regionService;

    @Autowired
    private DeliveryGoodsResNberExcel deliveryGoodsResNberExcel;
    
	@ApiOperation(value = "查询报表", notes = "查询报表")
    @ApiResponses({
            @ApiResponse(code=400,message="请求参数没填好"),
            @ApiResponse(code=404,message="请求路径没有或页面跳转路径不对")
    })
    @PostMapping("/getStorePurchaserReport")
    public ResultVO<Page<ReportStorePurchaserResq>> getStorePurchaserReport(@RequestBody ReportStorePurchaserReq req) {
		String userType=req.getUserType();
		if(userType!=null && !userType.equals("") && "4".equals(userType)){//供应商
			String retailerCode=UserContext.getUser().getRelCode();
			req.setRetailerCode(retailerCode);
		}
		if(userType!=null && !userType.equals("") && "2".equals(userType)){//地市管理员
			String regionId = UserContext.getUser().getRegionId();
			req.setLanId(regionId);
		}
		return iReportDataInfoService.getStorePurchaserReport(req);
    }
	
	@ApiOperation(value = "区县查询", notes = "区县查询")
    @ApiResponses({
            @ApiResponse(code=400,message="请求参数没填好"),
            @ApiResponse(code=404,message="请求路径没有或页面跳转路径不对")
    })
	@GetMapping(value="/getRegionIdForCity")
	public ResultVO<List<RegionsGetResp>> getRegionIdForCity() {
		RegionsListReq req = new RegionsListReq();
		String regionId = UserContext.getUser().getRegionId();
		//regionId = "430100";
		//String lanId = UserContext.getUser().getLanId();//430100
		if(StringUtils.isNotEmpty(regionId)){
			req.setRegionParentId(regionId);	
		}else{
			req.setRegionParentId("430000");
		}
		return regionService.listRegions(req);
    }
	
	
	@ApiOperation(value = "查询用户角色", notes = "查询用户角色")
    @ApiResponses({
            @ApiResponse(code=400,message="请求参数没填好"),
            @ApiResponse(code=404,message="请求路径没有或页面跳转路径不对")
    })
	@GetMapping(value="/getUerRoleForView")
    public ResultVO<List<ReportStorePurchaserResq>> getUerRoleForView() {
		ReportStorePurchaserReq req = new ReportStorePurchaserReq();
		String userId = UserContext.getUser().getUserId();
		req.setUserId(userId);
		return iReportDataInfoService.getUerRoleForView(req);
    }
	
	
	 /**
     * 导出按钮
     */
    @ApiOperation(value = "导出", notes = "导出报表数据")
    @ApiResponses({
            @ApiResponse(code=400,message="请求参数没填好"),
            @ApiResponse(code=404,message="请求路径没有或页面跳转路径不对")
    })
    @PostMapping(value="/StorePurchaserReportExport")
    public ResultVO StorePurchaserReportExport(@RequestBody ReportStorePurchaserReq req) {
		String userType=req.getUserType();
		if(userType!=null && !userType.equals("") && "4".equals(userType)){//供应商
			String retailerCode=UserContext.getUser().getRelCode();
			req.setRetailerCode(retailerCode);
		}
		if(userType!=null && !userType.equals("") && "2".equals(userType)){//地市管理员
			String regionId = UserContext.getUser().getRegionId();
			req.setLanId(regionId);
		}
        ResultVO result = new ResultVO();
        ResultVO<List<ReportStorePurchaserResq>> resultVO = iReportDataInfoService.getStorePurchaserReportdc(req);
        if (!resultVO.isSuccess()) {
            result.setResultCode(OmsCommonConsts.RESULE_CODE_FAIL);
            result.setResultData("失败：" + resultVO.getResultMsg());
            return result;
        }
        List<ReportStorePurchaserResq> data = resultVO.getResultData();
        //创建Excel
        Workbook workbook = new HSSFWorkbook();
        
        List<ExcelTitleName> orderMap = new ArrayList<>();
        orderMap.add(new ExcelTitleName("productBaseName", "机型"));
        orderMap.add(new ExcelTitleName("brandName", "品牌"));
        orderMap.add(new ExcelTitleName("theTotalInventory", "入库总量"));
        orderMap.add(new ExcelTitleName("theTotalOutbound", "出库总量"));
        orderMap.add(new ExcelTitleName("stockTotalNum", "库存总量"));
        orderMap.add(new ExcelTitleName("purchaseNum", "交易入库量"));
        orderMap.add(new ExcelTitleName("manualNum", "手工入库量"));
        orderMap.add(new ExcelTitleName("totalSalesNum", "总销售量"));
        orderMap.add(new ExcelTitleName("manualSalesNum", "手工销售量"));
        orderMap.add(new ExcelTitleName("crmcontractNum", "CRM合约销量"));
        orderMap.add(new ExcelTitleName("registerNum", "自注册销量"));
        orderMap.add(new ExcelTitleName("returnNum", "退库量"));
        orderMap.add(new ExcelTitleName("averageDailySales", "近7天日均销量"));
        orderMap.add(new ExcelTitleName("stockNum", "库存量"));
        orderMap.add(new ExcelTitleName("stockTurnover", "库存周转率"));
        orderMap.add(new ExcelTitleName("inventoryWarning", "库存预警"));
        
        //创建orderItemDetail
        deliveryGoodsResNberExcel.builderOrderExcel(workbook, data,
        		orderMap, "串码");
        return deliveryGoodsResNberExcel.uploadExcel(workbook);
    }

    
}

