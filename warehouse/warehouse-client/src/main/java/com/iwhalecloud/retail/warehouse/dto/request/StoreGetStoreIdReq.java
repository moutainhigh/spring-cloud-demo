package com.iwhalecloud.retail.warehouse.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("根据商家id和仓库类型")
public class StoreGetStoreIdReq implements Serializable{

    private static final long serialVersionUID = -1L;
    /**
     * 商家ID
     */
    @ApiModelProperty(value = "商家ID")
    private String merchantId;

    /**
     * 仓库类型
     */
    @ApiModelProperty(value = "仓库类型")
    private String storeSubType;

}
