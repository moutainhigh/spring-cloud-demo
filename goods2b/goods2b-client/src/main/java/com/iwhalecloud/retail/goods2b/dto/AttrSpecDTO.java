package com.iwhalecloud.retail.goods2b.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * AttrSpec
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@ApiModel(value = "对应模型PROD_ATTR_SPEC, 对应实体AttrSpec类")
public class AttrSpecDTO implements java.io.Serializable {
    
  	private static final long serialVersionUID = 1L;


	@ApiModelProperty(value = "attrId")
	private java.lang.String attrId;

	/**
	 * 属性所属的分组
	 */
	@ApiModelProperty(value = "属性所属的分组")
	private java.lang.String attrGroupId;

	/**
	 * 属性所属的分组名称
	 */
	@ApiModelProperty(value = "属性所属的分组名称")
	private java.lang.String attrGroupName;

	/**
	 * 商品分类ID
	 */
	@ApiModelProperty(value = "商品分类ID")
	private java.lang.String typeId;

	/**
	 * 存储在数据库中的表名
	 */
	@ApiModelProperty(value = "存储在数据库中的表名")
	private java.lang.String tableName;

	/**
	 * 存储在数据库中的字段名
	 */
	@ApiModelProperty(value = "存储在数据库中的字段名")
	private java.lang.String filedName;

	/**
	 * 展示在前端页面的名称
	 */
	@ApiModelProperty(value = "展示在前端页面的名称")
	private java.lang.String cname;

	/**
	 * 取值类型
	 1：文本框
	 2：下拉框
	 3：弹出框（弹出的页面取值于展示页面字段）
	 */
	@ApiModelProperty(value = "取值类型" +
			"1：文本框" +
			"2：下拉框" +
			"3：弹出框（弹出的页面取值于展示页面字段）")
	private java.lang.String valueType;

	/**
	 * 当取值类型为2：下拉框时，定义下拉框的值。多个用逗号，隔开
	 */
	@ApiModelProperty(value = "商品保存时，展示的默认值" +
			"1：文本框     --》当前字段配置的值" +
			"2：下拉框     --》取值ID" +
			"3：弹出框（弹出的页面取值于展示页面字段）--》无默认值")
	private java.lang.String defaultValue;

	/**
	 * 当取值类型为2：下拉框时，定义下拉框的值。多个用逗号，隔开
	 */
	@ApiModelProperty(value = "当取值类型为2：下拉框时，定义下拉框的值。多个用逗号，隔开")
	private java.lang.String valueScope;

	/**
	 * 值类型为3设置值时需要展示的页面
	 */
	@ApiModelProperty(value = "值类型为3设置值时需要展示的页面")
	private java.lang.String pageUrl;

	/**
	 * 页面展示时的排序
	 */
	@ApiModelProperty(value = "页面展示时的排序")
	private java.lang.String specOrder;

	/**
	 * 保存时是否校验该字段值
	 */
	@ApiModelProperty(value = "保存时是否校验该字段值")
	private java.lang.Long booCheck;

	/**
	 * 修改时是否允许修改该字段
	 */
	@ApiModelProperty(value = "修改时是否允许修改该字段")
	private java.lang.Long booEdit;

	/**
	 * 校验不通过时，弹出的提示消息
	 */
	@ApiModelProperty(value = "校验不通过时，弹出的提示消息")
	private java.lang.String checkMessage;

	/**
	 * 校验配置值的正则表达式
	 */
	@ApiModelProperty(value = "校验配置值的正则表达式")
	private java.lang.String regexCode;

	/**
	 * 页面展示时，配置的跨列个数
	 */
	@ApiModelProperty(value = "页面展示时，配置的跨列个数")
	private java.lang.Long colspan;

	/**
	 * 页面上的id名称
	 */
	@ApiModelProperty(value = "页面上的id名称")
	private java.lang.String htmlFiledName;

	/**
	 * 配置时设置字段是否允许为空
	 */
	@ApiModelProperty(value = "配置时设置字段是否允许为空")
	private java.lang.Long booNull;

	/**
	 * 记录状态。LOVB=PUB-C-0001。
	 */
	@ApiModelProperty(value = "记录状态。1000有效，1100无效。")
	private java.lang.String statusCd;

	/**
	 * 备注
	 */
	@ApiModelProperty(value = "备注")
	private java.lang.String remark;

	/**
	 * 记录首次创建的员工标识。
	 */
	@ApiModelProperty(value = "记录首次创建的员工标识。")
	private java.lang.String createStaff;

	/**
	 * 记录首次创建的时间。
	 */
	@ApiModelProperty(value = "记录首次创建的时间。")
	private java.util.Date createDate;

	/**
	 * 记录每次修改的员工标识。
	 */
	@ApiModelProperty(value = "记录每次修改的员工标识。")
	private java.lang.String updateStaff;

	/**
	 * 记录每次修改的时间。
	 */
	@ApiModelProperty(value = "记录每次修改的时间。")
	private java.util.Date updateDate;

	@ApiModelProperty(value = "属性名称")
	private String attrName;

	@ApiModelProperty(value = "属性类型")
	private String attrType;

	@ApiModelProperty(value = "是否为查询条件")
	private String isFilter;

	@ApiModelProperty(value = "是否为默认查询条件")
	private String isDefaultFilter;

	@ApiModelProperty(value = "查询条件排序")
	private int filterOrder;

	@ApiModelProperty(value = "实例值")
	private String instValue;

	@ApiModelProperty(value = "删除标识，有值则表示删除")
	private String isDeleted;

	@ApiModelProperty(value = "是否展示")
	private String isDisplay;

}
