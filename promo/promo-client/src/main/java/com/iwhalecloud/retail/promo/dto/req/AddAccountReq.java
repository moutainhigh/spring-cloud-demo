package com.iwhalecloud.retail.promo.dto.req;

import com.iwhalecloud.retail.dto.AbstractRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * @author 吴良勇
 * @date 2019/3/25 15:58
 */
@Data
public class AddAccountReq extends AbstractRequest implements Serializable {

    @NotEmpty(message = "账户名称不能为空")
    @ApiModelProperty(value = "账户名称")
    private String acctName;
    @NotEmpty(message = "商家标识不能为空")
    @ApiModelProperty(value = "商家标识")
    private String custId;
    @NotEmpty(message = "账户类型不能为空")
    @ApiModelProperty(value = "账户类型")
    private String acctType;

    @ApiModelProperty(value = "生效时间")
    private String effDate;

    @ApiModelProperty(value = "失效时间")
    private String expDate;

    @ApiModelProperty(value = "状态")
    private String statusCd;

    @ApiModelProperty(value = "创建人")
    private String createStaff;
}
