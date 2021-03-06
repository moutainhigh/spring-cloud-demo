package com.iwhalecloud.retail.warehouse.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 吴良勇
 * @date 2019/3/8 21:07
 */
@Data
@ApiModel(value = "串码校验返回")
public class ValidResourceInstResp  implements Serializable {

    @ApiModelProperty(value = "响应编码")
    private String resultCode;
    /**
     * 响应消息
     */
    @ApiModelProperty(value = "响应消息")
    private String resultMsg;

    @ApiModelProperty(value = "校验列表")
    private List<ValidResourceInstItemResp> validList;

}
