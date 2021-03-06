package com.iwhalecloud.retail.web.controller.b2b.promo.utils;

import com.iwhalecloud.retail.web.controller.b2b.order.dto.ExcelTitleName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lhr 2019-03-29 15:01:30
 */
public class ReBateExcelColum {

    public static List<ExcelTitleName> reBateDetailColumn() {
        List<ExcelTitleName> reBateDetailColumn = new ArrayList<>();
        reBateDetailColumn.add(new ExcelTitleName("lanName", "地市"));
        reBateDetailColumn.add(new ExcelTitleName("productName", "产品名称"));
        reBateDetailColumn.add(new ExcelTitleName("unitType", "产品型号"));
        reBateDetailColumn.add(new ExcelTitleName("specName", "规格型号"));
        reBateDetailColumn.add(new ExcelTitleName("productSn", "机型编码"));
        reBateDetailColumn.add(new ExcelTitleName("unitType", "产品型号"));;
        reBateDetailColumn.add(new ExcelTitleName("rewardPriceYuan", "返利单价"));
        reBateDetailColumn.add(new ExcelTitleName("amountYuan", "入账金额"));;
        reBateDetailColumn.add(new ExcelTitleName("statusCdDesc", "状态"));
        reBateDetailColumn.add(new ExcelTitleName("actName", "活动名称"));;
        reBateDetailColumn.add(new ExcelTitleName("effDate", "生效时间"));
        reBateDetailColumn.add(new ExcelTitleName("sourceDesc", "备注"));
        return reBateDetailColumn;
    }
    public static List<ExcelTitleName> reBatePayOutColumn() {
        List<ExcelTitleName> columnList = new ArrayList<>();
        columnList.add(new ExcelTitleName("supplierName", "供应商"));
        columnList.add(new ExcelTitleName("supplierLoginName", "供应商账号"));
        columnList.add(new ExcelTitleName("amountYuan", "使用金额"));
        columnList.add(new ExcelTitleName("balanceYuan", "当前金额"));
        columnList.add(new ExcelTitleName("orderId", "订单交易号"));
        columnList.add(new ExcelTitleName("productName", "产品名称"));;
        columnList.add(new ExcelTitleName("specName", "规格型号"));
        columnList.add(new ExcelTitleName("lanName", "地市"));;
        columnList.add(new ExcelTitleName("regionName", "区县"));
        columnList.add(new ExcelTitleName("operDate", "返利使用时间"));;
        columnList.add(new ExcelTitleName("payoutDesc", "备注"));

        return columnList;
    }
}
