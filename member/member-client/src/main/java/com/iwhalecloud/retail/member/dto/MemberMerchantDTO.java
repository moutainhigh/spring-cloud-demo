package com.iwhalecloud.retail.member.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * MemberMerchant
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@ApiModel(value = "对应模型mem_member_merchant, 对应实体MemberMerchant类")
public class MemberMerchantDTO implements java.io.Serializable {
    
  	private static final long serialVersionUID = 1L;
  
  	
  	//属性 begin
	/**
  	 * 商家ID
  	 */
	@ApiModelProperty(value = "ID")
	private String id;

	@ApiModelProperty(value = "商家ID")
  	private String merchId;
	
	/**
  	 * 会员ID
  	 */
	@ApiModelProperty(value = "会员ID")
  	private String memId;
	
	/**
  	 * 会员等级ID
  	 */
	@ApiModelProperty(value = "会员等级ID")
  	private Integer lvId;
	
	/**
  	 * 会员状态  1有效  0无效
  	 */
	@ApiModelProperty(value = "会员状态  1有效  0无效")
  	private String status;
	
	/**
  	 * 更新时间
  	 */
	@ApiModelProperty(value = "更新时间")
  	private java.util.Date updateDate;
	
	/**
  	 * 更新人
  	 */
	@ApiModelProperty(value = "更新人")
  	private String updateStaff;
	
  	
}
