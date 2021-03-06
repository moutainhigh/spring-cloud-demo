package com.iwhalecloud.retail.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 存 登录用户的其他信息
 */
@Data
@ApiModel(value = "存放 待处理、我的申请及未读消息数量、用户的消息数量、经办数量")
public class WorkPlatformMsgDTO implements Serializable {
    private static final long serialVersionUID = 1783506762137275983L;

    @ApiModelProperty(value = "待处理数量")
    private Long unhandledItemCount;

    @ApiModelProperty(value = "我的申请数量")
    private Long appliedItemCount;

    @ApiModelProperty(value = "未读消息数量")
    private Integer notReadNoticeCount;
    
    @ApiModelProperty(value = "用户业务告警消息数量")
    private Long sysAlarmCount;

    @ApiModelProperty(value = "我的经办数量（自己申请的和处理过的）")
    private Long handledItemCount;
}
