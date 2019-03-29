package com.iwhalecloud.retail.promo.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.iwhalecloud.retail.promo.PromoServiceApplication;
import com.iwhalecloud.retail.promo.dto.ActActivityProductRuleDTO;
import com.iwhalecloud.retail.promo.dto.ActivityRuleDTO;
import com.iwhalecloud.retail.promo.dto.req.ActReBateProductReq;
import com.iwhalecloud.retail.promo.dto.req.ActivityProductReq;
import com.iwhalecloud.retail.promo.service.ActActivityProductRuleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lhr 2019-03-27 16:23:30
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PromoServiceApplication.class)
public class ActActivityProductRuleServiceImplTest {

    @Reference
    private ActActivityProductRuleService actActivityProductRuleService;
    @Test
    public void addReBateProduct(){
        ActReBateProductReq actReBateProductReq = new ActReBateProductReq();
        actReBateProductReq.setMarketingActivityId("10008987");
        actReBateProductReq.setCalculationRule("30%");
        ActActivityProductRuleDTO actActivityProductRuleDTO = new ActActivityProductRuleDTO();
        actActivityProductRuleDTO.setCreator("1");
        actActivityProductRuleDTO.setGmtCreate(new Date());
        actActivityProductRuleDTO.setGmtModified(new Date());
        actActivityProductRuleDTO.setIsDeleted("0");
        actActivityProductRuleDTO.setActProdRelId("10008987");
        actActivityProductRuleDTO.setModifier("1");
        actActivityProductRuleDTO.setPrice("40");
        actActivityProductRuleDTO.setRuleAmount("1");
        actActivityProductRuleDTO.setProductId("123456");
        actReBateProductReq.setActActivityProductRuleDTO(actActivityProductRuleDTO);
        List<ActivityRuleDTO> activityRuleDTOList = new ArrayList<>();
        ActivityRuleDTO activityRuleDTO = new ActivityRuleDTO();
        activityRuleDTO.setCreator("1");
        activityRuleDTO.setCalculationRule("10");
        activityRuleDTO.setDeductionScale("30");
        activityRuleDTO.setModifier("1");
        activityRuleDTO.setGmtCreate(new Date());
        activityRuleDTO.setGmtModified(new Date());
        activityRuleDTO.setMarketingActivityId("10008987");
        activityRuleDTO.setIsDeleted("0");
        activityRuleDTO.setUseEndTime(new Date());
        activityRuleDTO.setUseStartTime(new Date());
        activityRuleDTO.setSettlementRule("");
        activityRuleDTO.setRuleId("21312");
        activityRuleDTO.setRuleName("313123");
        activityRuleDTOList.add(activityRuleDTO);
        actReBateProductReq.setActivityRuleDTOList(activityRuleDTOList);
        List<ActivityProductReq> activityProductReqs = new ArrayList<>();
        ActivityProductReq activityProductReq = new ActivityProductReq();
        activityProductReq.setCreator("1");
        activityProductReq.setGmtModified(new Date());
        activityProductReq.setIsDelete(0);
        activityProductReq.setMarketingActivityId("10008987");
        activityProductReq.setModifier("1");
        activityProductReq.setReachAmount(Long.parseLong("40"));
        activityProductReq.setRebatePrice(Long.parseLong("40"));
        activityProductReq.setProductId("123456");
        activityProductReq.setProductBaseId("11111");
        activityProductReq.setNum(20L);
        activityProductReq.setPrice(Long.parseLong("40"));
        activityProductReqs.add(activityProductReq);
        actReBateProductReq.setActivityProductReqs(activityProductReqs);
        actActivityProductRuleService.addReBateProduct(actReBateProductReq);
    }
}

