package com.iwhalecloud.retail.report;

import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.report.dto.request.SupplierOperatingDayPageReq;
import com.iwhalecloud.retail.report.service.SupplierOperatingDayService;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author wenlong.zhong
 * @date 2019/6/10
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ReportServiceApplication.class)
public class SupplierOperatingDayServiceTest {


    @Autowired
    private SupplierOperatingDayService supplierOperatingDayService;

    @org.junit.Test
    public void page(){
        SupplierOperatingDayPageReq req = new SupplierOperatingDayPageReq();
//        req.setRegionIdList(Lists.newArrayList("731","732","733","734","735"));
        req.setPageNo(1);
        req.setPageSize(3);
        ResultVO resultVO = supplierOperatingDayService.page(req);
        System.out.print("结果：" + resultVO.toString());
    }
}
