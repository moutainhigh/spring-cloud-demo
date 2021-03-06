package com.iwhalecloud.retail.goods2b.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iwhalecloud.retail.goods2b.dto.TagsDTO;
import com.iwhalecloud.retail.goods2b.dto.req.ProdTagsListReq;
import com.iwhalecloud.retail.goods2b.entity.Tags;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Class: TagsMapper
 * @author autoCreate
 */
@Mapper
public interface TagsMapper extends BaseMapper<Tags>{
    /**
     * 查询标签列表 通过goodsId
     * @param goodsId
     * @return
     */
    List<TagsDTO> getTagsByGoodsId(String goodsId);

    /**
     * 查询标签列表
     * @param req 请求入参
     * @return
     */
    List<TagsDTO> listProdTags(@Param("req") ProdTagsListReq req);
    
    /**
     * 查询渠道标签列表
     * @return
     */
    List<TagsDTO> listProdTagsChannel();
}