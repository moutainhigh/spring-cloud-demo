package com.iwhalecloud.retail.order2b.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.order2b.dto.response.purapply.*;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.*;

import java.util.List;

/**
 *
 * @author lws
 * @date 2019-04-15
 */
public interface PurApplyService {

	//查询采购申请单
	public ResultVO<Page<PurApplyResp>> cgSearchApply(PurApplyReq req);
	//查询采购申请单地市管理员
	public ResultVO<Page<PurApplyResp>> cgSearchApplyLan(PurApplyReq req);
	//获取登录用户是地市管理员还是省级管理员
	public PriCityManagerResp getLoginInfo(String userId);
	//采购申请的添加产商品的写表
	public void crPurApplyItem(AddProductReq req);
	////采购申请单单号写表
	public ResultVO tcProcureApply(ProcureApplyReq req);
	//采购申请的附件的写表
	public void crPurApplyFile(AddFileReq req);
	//采购申请查询的删除操作
	public ResultVO delSearchApply(PurApplyReq req);

	public CkProcureApplyResp ckApplyData1(PurApplyReq req);
	public List<AddProductReq> ckApplyData2(PurApplyReq req);
	public List<AddFileReq> ckApplyData3(PurApplyReq req);
	public List<PurApplyExtReq> ckApplyData4(PurApplyReq req);

	public ApplyHeadResp hqShenQingDaoHao();

	public String hqDiShiBuMen(String dsbm);

	public int isHaveSave(String applyId);

	public void updatePurApply(ProcureApplyReq state);

	public void delApplyItem(ProcureApplyReq req);
	public void delApplyFile(ProcureApplyReq req);
	public void delPurApplyExt(ProcureApplyReq req);

	public MemMemberAddressReq selectMemMeneberAddr(ProcureApplyReq req);

	public void insertPurApplyExt(MemMemberAddressReq req);

	public String getMerchantCode(String merchantCode);

	public String hqSeqFileId();

	public String hqSeqItemId();
	//添加收货地址
	public void addShippingAddress(MemMemberAddressReq req);

	public ResultVO updatePrice(UpdateCorporationPriceReq req);

	public ResultVO commitPriceExcel(UpdateCorporationPriceReq req);

	public ResultVO<List<ProdProductChangeDetail>> searchCommitPriceInfo(UpdateCorporationPriceReq req);
	public void insertTcProcureApply(ProcureApplyReq req);
	/*
	 *  采购申请
	 * */
	public ResultVO applyPurchase(ProcureApplyReq req);


	public ResultVO updatePurTypeByApplyId(ProcureApplyReq req);

	//政企省内代收报表
	public ResultVO<Page<PurApplyReportResp>> applySearchReport(PurApplyReportReq req);

	//政企省内代收项目状态报表
	public ResultVO<Page<PurApplyStatusReportResp>> applyStatusSearchReport(PurApplyStatusReportReq req);

	List<String>  countDelivery(String applyId);
}
