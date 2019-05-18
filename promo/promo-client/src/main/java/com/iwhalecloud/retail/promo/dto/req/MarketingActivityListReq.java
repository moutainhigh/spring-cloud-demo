package com.iwhalecloud.retail.promo.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author mzl
 * @date 2019/1/30
 */
@Data
public class MarketingActivityListReq extends AbstractPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 营销活动名称
     */
    @ApiModelProperty(value = "营销活动名称")
    private String activityName;

    /**
     * 营销活动编码
     */
    @ApiModelProperty(value = "营销活动编码")
    private String activityCode;

    /**
     * 营销活动类型
     */
    @ApiModelProperty(value = "营销活动类型")
    private String activityType;

    /**
     * 营销活动发起人
     */
    @ApiModelProperty(value = "营销活动发起人")
    private String activityInitiator;

    /**
     * 营销活动状态
     */
    @ApiModelProperty(value = "营销活动状态")
    private String activityStatus;

    /**
     * 营销活动开始时间起
     */
    @ApiModelProperty(value = "营销活动开始时间起")
    private String startTimeS;

    /**
     * 营销活动开始时间止
     */
    @ApiModelProperty(value = "营销活动开始时间止")
    private String startTimeE;

    /**
     * 营销活动结束时间起
     */
    @ApiModelProperty(value = "营销活动结束时间起")
    private String endTimeS;

    /**
     * 营销活动结束时间止
     */
    @ApiModelProperty(value = "营销活动结束时间止")
    private String endTimeE;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String creator;
    
    /**
     * 营销活动开始时间起
     */
    @ApiModelProperty(value = "营销活动开始时间起")
    private String deliverStartTimeS;

    /**
     * 营销活动开始时间止
     */
    @ApiModelProperty(value = "营销活动开始时间止")
    private String deliverStartTimeE;

    /**
     * 营销活动结束时间起
     */
    @ApiModelProperty(value = "营销活动结束时间起")
    private String deliverEndTimeS;

    /**
     * 营销活动结束时间止
     */
    @ApiModelProperty(value = "营销活动结束时间止")
    private String deliverEndTimeE;
    
}
