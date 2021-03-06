package com.iwhalecloud.retail.member.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Group
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@TableName("mem_group")
@KeySequence(value = "seq_mem_group_group_id", clazz = String.class)
@ApiModel(value = "对应模型mem_group, 对应实体Group类")
public class Group implements Serializable {
    /**表名常量*/
    public static final String TNAME = "mem_group";
  	private static final long serialVersionUID = 1L;
  
  	
  	//属性 begin
  	/**
  	 * 会员群ID
  	 */
	@TableId
	@ApiModelProperty(value = "会员群ID")
  	private String groupId;
  	
  	/**
  	 * 会员群名称
  	 */
	@ApiModelProperty(value = "会员群名称")
  	private String groupName;
  	
  	/**
  	 * 会员群类型 (值待确定)
  	 */
	@ApiModelProperty(value = "会员群类型 (值待确定)")
  	private String groupType;
  	
  	/**
  	 * 商圈
  	 */
	@ApiModelProperty(value = "商圈")
  	private String tradeName;
  	
  	/**
  	 * 会员群状态  1有效   0无效
  	 */
	@ApiModelProperty(value = "会员群状态  1有效   0无效")
  	private String status;
  	
  	/**
  	 * 群描述(备注)
  	 */
	@ApiModelProperty(value = "群描述(备注)")
  	private String meno;
  	
  	/**
  	 * 平台类型(来源)
  	 */
	@ApiModelProperty(value = "平台类型(来源)")
  	private String sourceFrom;
  	
  	/**
  	 * 创建时间
  	 */
	@ApiModelProperty(value = "创建时间")
  	private java.util.Date createDate;
  	
  	/**
  	 * 创建人
  	 */
	@ApiModelProperty(value = "创建人")
  	private String createStaff;
  	
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
  	
  	
  	//属性 end
	
    /** 字段名称枚举. */
    public enum FieldNames {
		/** 会员群ID. */
		groupId("groupId","GROUP_ID"),
		
		/** 会员群名称. */
		groupName("groupName","GROUP_NAME"),
		
		/** 会员群类型 (值待确定). */
		groupType("groupType","GROUP_TYPE"),
		
		/** 商圈. */
		tradeName("tradeName","TRADE_NAME"),
		
		/** 会员群状态  1有效   0无效. */
		status("status","STATUS"),
		
		/** 群描述(备注). */
		meno("meno","MENO"),
		
		/** 平台类型(来源). */
		sourceFrom("sourceFrom","SOURCE_FROM"),
		
		/** 创建时间. */
		createDate("createDate","CREATE_DATE"),
		
		/** 创建人. */
		createStaff("createStaff","CREATE_STAFF"),
		
		/** 更新时间. */
		updateDate("updateDate","UPDATE_DATE"),
		
		/** 更新人. */
		updateStaff("updateStaff","UPDATE_STAFF");

		private String fieldName;
		private String tableFieldName;
		FieldNames(String fieldName, String tableFieldName){
			this.fieldName = fieldName;
			this.tableFieldName = tableFieldName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getTableFieldName() {
			return tableFieldName;
		}
	}

}
