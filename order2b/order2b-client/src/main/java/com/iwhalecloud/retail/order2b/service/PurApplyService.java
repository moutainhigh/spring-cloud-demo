package com.iwhalecloud.retail.order2b.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.order2b.dto.response.purapply.PurApplyResp;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.AddProductReq;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.ProcureApplyReq;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.PurApplyReq;

/**
 * 
 * @author lws
 * @date 2019-04-15
 */
public interface PurApplyService {

	//查询采购申请单
	public ResultVO<Page<PurApplyResp>> cgSearchApply(PurApplyReq req);
	//采购申请的添加产商品的写表
	public void crPurApplyItem(AddProductReq req);
	////采购申请单单号写表
	public void tcProcureApply(ProcureApplyReq req);
	//采购申请的附件的写表
	public void crPurApplyFile(ProcureApplyReq req);
	//采购申请查询的删除操作
	public void delSearchApply(PurApplyReq req);

}