package com.iwhalecloud.retail.goods2b.dto.req;

import com.iwhalecloud.retail.dto.AbstractRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@ApiModel(value = "店中商(分销商)和标签 关联关系 新增请求对象，对应模型prod_partner_tag_rel, 对应实体PartnerTagRel类")
public class MerchantTagRelSaveBatchReq extends AbstractRequest implements java.io.Serializable {

    private static final long serialVersionUID = -7628914638429295058L;

    /**
     * 商家 ID
     */
    @ApiModelProperty(value = "商家ID集合")
    @NotEmpty(message = "商家ID集合不能为空")
    private List<String> merchantIdList;

    /**
     * 标签ID集合
     */
    @ApiModelProperty(value = "标签ID集合，用于批量插入 ")
    private List<String> tagIdList;


}