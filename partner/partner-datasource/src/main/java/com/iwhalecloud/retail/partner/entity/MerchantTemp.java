package com.iwhalecloud.retail.partner.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * MerchantTemp
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@TableName("par_merchant_temp")
@ApiModel(value = "par_merchant_temp, 对应实体MerchantTemp类")
@KeySequence(value="seq_par_merchant_temp_id",clazz = String.class)
public class MerchantTemp implements Serializable {
    /**表名常量*/
    public static final String TNAME = "par_merchant_temp";
  	private static final long serialVersionUID = 1L;
  
  	
  	//属性 begin
  	/**
  	 * 主键
  	 */
	@TableId
	@ApiModelProperty(value = "主键")
  	private String id;
  	
  	/**
  	 * 每行参数串
  	 */
	@ApiModelProperty(value = "每行参数串")
  	private String txtStr;
  	
  	/**
  	 * 批次
  	 */
	@ApiModelProperty(value = "批次")
  	private String patch;
  	
  	/**
  	 * 创建时间
  	 */
	@ApiModelProperty(value = "创建时间")
  	private java.util.Date createDate;
  	
  	/**
  	 * 状态
  	 */
	@ApiModelProperty(value = "状态")
  	private String status;
  	
  	/**
  	 * 处理时间
  	 */
	@ApiModelProperty(value = "处理时间")
  	private String handleDate;
  	
  	/**
  	 * 备注
  	 */
	@ApiModelProperty(value = "备注")
  	private String note;
  	
  	
  	//属性 end
	
    /** 字段名称枚举. */
    public enum FieldNames {
		/** 主键. */
		id("id","id"),
		
		/** 每行参数串. */
		txtStr("txtStr","txt_str"),
		
		/** 批次. */
		patch("patch","patch"),
		
		/** 创建时间. */
		createDate("createDate","create_date"),
		
		/** 状态. */
		status("status","status"),
		
		/** 处理时间. */
		handleDate("handleDate","handle_date"),
		
		/** 备注. */
		note("note","note");

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
