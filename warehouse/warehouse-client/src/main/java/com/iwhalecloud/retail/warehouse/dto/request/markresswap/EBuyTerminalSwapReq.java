package com.iwhalecloud.retail.warehouse.dto.request.markresswap;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

/**
 * @author 吴良勇
 * @date 2019/3/6 18:01
 */
@Data
@ApiModel("固网串码入库请求参数")
public class EBuyTerminalSwapReq implements Serializable {
    @NotEmpty(message = "串码列表不能为空")
    private List<EBuyTerminalItemSwapReq> mktResList;
}
