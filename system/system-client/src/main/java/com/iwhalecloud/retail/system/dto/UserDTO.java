package com.iwhalecloud.retail.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * User
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@ApiModel(value = "对应模型SYS_USER, 对应实体User类")
public class UserDTO implements java.io.Serializable {

	private static final long serialVersionUID = 1L;


	//属性 begin
	/**
	 * 用户ID
	 */
	@ApiModelProperty(value = "用户ID")
	private java.lang.String userId;

	/**
	 * 登陆用户名
	 */
	@ApiModelProperty(value = "登陆用户名")
	private java.lang.String loginName;

	/**
	 * 登陆密码
	 */
//	@ApiModelProperty(value = "登陆密码")
//	private java.lang.String loginPwd;

	/**
	 * 状态   1有效、 0 禁用   2：失效(删除)  3:锁住（密码错误次数超限 等等）
	 */
	@ApiModelProperty(value = "状态   1有效、 0 禁用   2：失效(删除)  3:锁住（密码错误次数超限 等等）")
	private java.lang.Integer statusCd;

	/**
	 * 用户昵称
	 */
	@ApiModelProperty(value = "用户昵称")
	private java.lang.String userName;

	/**
	 * 备注
	 */
	@ApiModelProperty(value = "备注")
	private java.lang.String remark;

	/**
	 * 1超级管理员 2普通管理员  3零售商(门店、店中商)  4省包供应商  5地包供应商
	 * 6 代理商店员  7经营主体  8厂商
	 */
	@ApiModelProperty(value = "1超级管理员 2普通管理员  3零售商(门店、店中商)  4省包供应商  5地包供应商  " +
			" 6 代理商店员  7经营主体  8厂商  \n")
	private java.lang.Integer userFounder;

	/**
	 * 关联类型
	 */
	@ApiModelProperty(value = "关联类型")
	private java.lang.String relType;

	/**
	 * 关联员工ID
	 */
	@ApiModelProperty(value = "关联员工ID")
	private java.lang.String relNo;

	/**
	 * 用户归属本地网
	 */
	@ApiModelProperty(value = "用户归属本地网")
	private java.lang.String lanId;

	/**
	 * 关联代理商ID  或 供应商ID
	 */
	@ApiModelProperty(value = "关联ID")
	private java.lang.String relCode;

	/**
	 * 当前登陆时间
	 */
	@ApiModelProperty(value = "当前登陆时间")
	private java.util.Date curLoginTime;

	/**
	 * 上次登陆时间
	 */
	@ApiModelProperty(value = "上次登陆时间")
	private java.util.Date lastLoginTime;

	/**
	 * 登陆失败的次数
	 */
	@ApiModelProperty(value = "登陆失败的次数")
	private java.lang.Integer failLoginCnt;

	/**
	 * 登陆成功的次数
	 */
	@ApiModelProperty(value = "登陆成功的次数")
	private java.lang.Integer successLoginCnt;

	/**
	 * 用户电话号码
	 */
	@ApiModelProperty(value = "用户电话号码")
	private java.lang.String phoneNo;

	/**
	 * 关联组织 id
	 */
	@ApiModelProperty(value = "关联组织 id")
	private java.lang.String orgId;

	/**
	 * 关联组织名称
	 */
	@ApiModelProperty(value = "关联组织名称")
	private java.lang.String orgName;

	/**
	 * 邮箱账号
	 */
	@ApiModelProperty(value = "邮箱账号")
	private java.lang.String email;

	/**
	 * 区域ID
	 */
	@ApiModelProperty(value = "区域ID")
	private java.lang.String regionId;

	/**
	 * 创建人
	 */
	@ApiModelProperty(value = "创建人")
	private java.lang.String createStaff;

	/**
	 * 创建时间
	 */
	@ApiModelProperty(value = "创建时间")
	private java.util.Date createDate;

	/**
	 * 修改人
	 */
	@ApiModelProperty(value = "修改人")
	private java.lang.String updateStaff;

	/**
	 * 修改时间
	 */
	@ApiModelProperty(value = "修改时间")
	private java.util.Date updateDate;

	@ApiModelProperty(value = "岗位ID")
	private Long sysPostId;

	@ApiModelProperty(value = "岗位名称")
	private String sysPostName;

	@ApiModelProperty(value = "工号来源")
	private Integer userSource;

	@ApiModelProperty(value = "changePwdCount")
	private Integer changePwdCount;

	/**
	 * 重写脱敏
	 */
//	public String getPhoneNo() {
//		return DesensitizedUtils.mobilePhone(phoneNo);
//	}

}
