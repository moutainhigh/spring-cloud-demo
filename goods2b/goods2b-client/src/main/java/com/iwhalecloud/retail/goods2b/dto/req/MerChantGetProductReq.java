package com.iwhalecloud.retail.goods2b.dto.req;

import com.iwhalecloud.retail.dto.AbstractRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * ProdProduct
 * @author generator
 * @version 1.0
 * @since 1.0
 */
@Data
@ApiModel(value = "对应模型prod_product, 对应实体ProdProduct类")
public class MerChantGetProductReq extends AbstractRequest implements Serializable {

  	private static final long serialVersionUID = -430667554698070270L;

	/**
	 * 产品D
	 */
	@ApiModelProperty(value = "产品ID")
	private String productId;


}
