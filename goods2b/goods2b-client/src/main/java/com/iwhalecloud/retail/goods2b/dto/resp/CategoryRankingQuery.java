package com.iwhalecloud.retail.goods2b.dto.resp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author mzl
 * @date 2018/12/28
 */
@Data
@ApiModel(value = "分类销量排行返回")
public class CategoryRankingQuery implements Serializable {

    private static final long serialVersionUID = -5891507515403341815L;

    /**
     * 商品ID
     */
    @ApiModelProperty(value = "商品ID")
    private String goodsId;

    /**
     * 商品名称
     */
    @ApiModelProperty(value = "商品名称")
    private String goodsName;

    /**
     * 零售价
     */
    @ApiModelProperty(value = "零售价")
    private Double mktprice;

    /**
     * 提货价
     */
    @ApiModelProperty(value = "提货价")
    private Double deliveryPrice;


    /**
     * 供货商ID
     */
    @ApiModelProperty(value = "供货商ID")
    private String supplierId;

    /**
     * 供货商名称
     */
    @ApiModelProperty(value = "供货商名称")
    private String supplierName;

    /**
     * 成交量
     */
    @ApiModelProperty(value = "成交量")
    private String orderAmount;

    /**
     * 默认图片
     */
    @ApiModelProperty(value = "默认图片")
    private String defaultImage;
}
