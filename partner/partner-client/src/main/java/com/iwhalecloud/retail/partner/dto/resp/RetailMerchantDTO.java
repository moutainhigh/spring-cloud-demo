package com.iwhalecloud.retail.partner.dto.resp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("零售商类型的商家 信息，par_merchant表字段")
public class RetailMerchantDTO implements Serializable {


/*** 非当前表字段 ****/

    // sys_common_region 表字段
    @ApiModelProperty(value = "地市名称")
    private java.lang.String lanName;

    @ApiModelProperty(value = "市县名称")
    private java.lang.String cityName;

    // par_invoice 表字段
    @ApiModelProperty(value = "纳税人识别号")
    private String taxCode;

    @ApiModelProperty(value = "营业执照号")
    private String busiLicenceCode;

    @ApiModelProperty(value = "营业执照到期日期")
    private Date busiLicenceExpDate;

    @ApiModelProperty(value = "银行账号")
    private String registerBankAcct;

    // prod_merchant_tag_rel表字段
    @ApiModelProperty(value = "标签组名")
    private String tagNames;


    /* 市县、分局/县部门 合并为一个字段 经营单元（对应 组织表的 第3级组织部门）*/
    @ApiModelProperty(value = "经营单元(3级组织部门)id")
    private java.lang.String orgIdWithLevel3;

    /* 营销中心/支局 字段 改为 查  组织表的 第4级组织部门 */
    @ApiModelProperty(value = "营销中心/支局（4级组织部）id")
    private java.lang.String orgIdWithLevel4;

    /* 市县、分局/县部门 合并为一个字段 经营单元（对应 组织表的 第3级组织部门）*/
    @ApiModelProperty(value = "经营单元(3级组织部门) 名称")
    private java.lang.String orgNameWithLevel3;

    /* 营销中心/支局 字段 改为 查  组织表的 第4级组织部门 */
    @ApiModelProperty(value = "营销中心/支局（4级组织部）名称")
    private java.lang.String orgNameWithLevel4;

 /*** 非当前表字段 ****/


    //属性 begin
    @ApiModelProperty(value = "商家ID")
    private String merchantId;

    /**
     * 商家编码
     */
    @ApiModelProperty(value = "商家编码")
    private String merchantCode;

    /**
     * 商家名称
     */
    @ApiModelProperty(value = "商家名称")
    private String merchantName;

    /**
     * 渠道状态:  有效 1000 主动暂停 1001 异常暂停 1002 无效 1100 终止 1101 退出 1102 未生效 1200 已归档 1300 预退出 8922 冻结 8923
     主动暂停 1001
     异常暂停 1002
     无效 1100
     终止 1101
     退出 1102
     未生效 1200
     已归档 1300
     预退出 8922
     冻结 8923
     */
    @ApiModelProperty(value = "渠道状态:" +
            " 有效1000  主动暂停1001  异常暂停1002 无效1100 终止1101 退出1102 未生效1200" +
            " 已归档1300 预退出8922 冻结8923 主动暂停1001  异常暂停1002   无效1100  终止1101  " +
            " 退出1102  未生效1200  已归档1300 预退出8922  冻结8923 ")
    private java.lang.String status;

    /**
     * 商家类型:  1 厂商    2 地包商    3 省包商   4 零售商
     */
    @ApiModelProperty(value = "商家类型:  1 厂商    2 地包商    3 省包商   4 零售商")
    private java.lang.String merchantType;

    /**
     * 关联sys_user表user_id
     */
    @ApiModelProperty(value = "关联sys_user表user_id")
    private java.lang.String userId;

    /**
     * 最后更新时间
     */
    @ApiModelProperty(value = "最后更新时间")
    private java.util.Date lastUpdateDate;

    /**
     * 商家所属经营主体
     */
    @ApiModelProperty(value = "商家所属经营主体	")
    private java.lang.String businessEntityName;

    /**
     * 商家所属经营主体	编码
     */
    @ApiModelProperty(value = "商家所属经营主体	编码")
    private java.lang.String businessEntityCode;

    /**
     * 客户编码
     */
    @ApiModelProperty(value = "客户编码")
    private java.lang.String customerCode;

    /**
     * 地市
     */
    @ApiModelProperty(value = "地市")
    private java.lang.String lanId;

    /**
     * 市县
     */
    @ApiModelProperty(value = "市县")
    private java.lang.String city;

    /**
     * 分局/县部门
     */
    @ApiModelProperty(value = "分局/县部门")
    private java.lang.String subBureau;

    /**
     * 营销中心/支局
     */
    @ApiModelProperty(value = "营销中心/支局")
    private java.lang.String marketCenter;

    /**
     * 销售点编码
     */
    @ApiModelProperty(value = "销售点编码")
    private java.lang.String shopCode;

    /**
     * 销售点名称
     */
    @ApiModelProperty(value = "销售点名称")
    private java.lang.String shopName;

    /**
     * 自营厅级别
     */
    @ApiModelProperty(value = "自营厅级别	")
    private java.lang.String selfShopLevel;

    /**
     * 渠道大类	: 自有厅 10  政企直销经理 11  政企直销代理 12  公众直销经理 13 公众直销代理 14 连锁店 20 独立店 24 便利点 30 互联网自营 31 互联网代理 32 移动物联网自营 33 移动互联网代理 34 客服型电子渠道 35 专营店 40
     政企直销经理 11
     政企直销代理 12
     公众直销经理 13
     公众直销代理 14
     连锁店 20
     独立店 24
     便利点 30
     互联网自营 31
     互联网代理 32
     移动物联网自营 33
     移动互联网代理 34
     客服型电子渠道 35
     专营店 40

     */
    @ApiModelProperty(value = "渠道大类:" +
            " 自有厅10  政企直销经理11  政企直销代理 12  公众直销经理 13 公众直销代理 14 连锁店 20 独立店 " +
            " 24 便利点 30 互联网自营 31 互联网代理 32 移动物联网自营 33 移动互联网代理 34 客服型电子渠道 35 专营店 " +
            " 40  政企直销经理 11  政企直销代理 12 公众直销经理 13  公众直销代理 14  连锁店 20 独立店 24  便利点 " +
            " 30 互联网自营 31 互联网代理 32  移动物联网自营 33  移动互联网代理 34 客服型电子渠道 35  专营店 40 ")
    private java.lang.String channelType;

    /**
     * 渠道小类
     */
    @ApiModelProperty(value = "渠道小类	")
    private java.lang.String channelMediType;

    /**
     * 渠道子类
     */
    @ApiModelProperty(value = "渠道子类	")
    private java.lang.String channelSubType;

    /**
     * 销售点地址
     */
    @ApiModelProperty(value = "销售点地址	")
    private java.lang.String shopAddress;

    /**
     * (商家)CRM组织ID
     */
    @ApiModelProperty(value = "(商家)CRM组织ID	")
    private java.lang.String parCrmOrgId;

    /**
     * (商家)CRM组织编码
     */
    @ApiModelProperty(value = "(商家)CRM组织编码")
    private java.lang.String parCrmOrgCode;

   /**
    * (商家)CRM组织路径编码
    */
   @ApiModelProperty(value = "(商家)CRM组织路径编码")
   private java.lang.String parCrmOrgPathCode;

//
//	/**
//  	 * 是否自有物业
//  	 */
//	@ApiModelProperty(value = "是否自有物业	")
//  	private java.lang.String isOwnProperty;

    /**
     * (商家)联系人
     */
    @ApiModelProperty(value = "(商家)联系人	")
    private java.lang.String linkman;

    /**
     * (商家)联系电话
     */
    @ApiModelProperty(value = "(商家)联系电话")
    private java.lang.String phoneNo;

    /**
     * 供应商名称
     */
    @ApiModelProperty(value = "供应商名称	")
    private java.lang.String supplierName;

    /**
     * 供应商编码
     */
    @ApiModelProperty(value = "供应商编码	")
    private java.lang.String supplierCode;

    /**
     * (商家)生效时间
     */
    @ApiModelProperty(value = "(商家)生效时间	")
    private java.util.Date effDate;

    /**
     * (商家)失效时间
     */
    @ApiModelProperty(value = "(商家)失效时间	")
    private java.util.Date expDate;

   /**
    * 系统账号
    */
   @ApiModelProperty(value = "系统账号	")
   private java.lang.String loginName;

}
