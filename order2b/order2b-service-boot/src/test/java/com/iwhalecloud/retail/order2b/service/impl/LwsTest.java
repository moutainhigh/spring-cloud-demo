package com.iwhalecloud.retail.order2b.service.impl;

import com.iwhalecloud.retail.order2b.TestBase;
import com.iwhalecloud.retail.order2b.dto.resquest.purapply.PurApplyReq;
import com.iwhalecloud.retail.order2b.dubbo.PurApplyServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LwsTest extends TestBase {
	
	@Autowired
    private PurApplyServiceImpl purApplyServiceImpl;
	
	@Test
	public void delSearchApply() {
		PurApplyReq req = new PurApplyReq();
		req.setApplyId("1");
		purApplyServiceImpl.delSearchApply(req);
    }

}
