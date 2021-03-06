package com.iwhalecloud.retail.partner.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 供应商供货关系信息
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@TableName("PAR_SUPPLY_REL")
@KeySequence(value="seq_par_supply_rel_id",clazz = String.class)
@ApiModel(value = "对应模型PAR_SUPPLY_REL, 对应实体SupplyRel类")
public class SupplyRel implements Serializable {
    /**表名常量*/
    public static final String TNAME = "ES_SUPPLY_REL";
  	private static final long serialVersionUID = 1L;
  
  	
  	//属性 begin
  	/**
  	 * REL_ID
  	 */
	@TableId
	@ApiModelProperty(value = "REL_ID")
  	private java.lang.String relId;
  	
  	/**
  	 * SUPPLIER_ID
  	 */
	@ApiModelProperty(value = "SUPPLIER_ID")
  	private java.lang.String supplierId;
  	
  	/**
  	 * PARTNER_ID
  	 */
	@ApiModelProperty(value = "PARTNER_ID")
  	private java.lang.String partnerId;
  	
  	//属性 end
	
    /** 字段名称枚举. */
    public enum FieldNames {
		/** REL_ID. */
		relId("relId","REL_ID"),
		
		/** SUPPLIER_ID. */
		supplierId("supplierId","SUPPLIER_ID"),
		
		/** PARTNER_ID. */
		partnerId("partnerId","PARTNER_ID");

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
