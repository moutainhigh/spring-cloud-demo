package com.iwhalecloud.retail.goods2b.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.goods2b.dto.req.ProductBaseListReq;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductBaseGetResp;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductBaseLightResp;
import com.iwhalecloud.retail.goods2b.dto.resp.ProductDetailResp;
import com.iwhalecloud.retail.goods2b.entity.ProductBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Class: ProdProductBaseMapper
 * @author autoCreate
 */
@Mapper
public interface ProductBaseMapper extends BaseMapper<ProductBase>{

    /**
     * 根据产品基本信息ID获取产品产品基本信息对象
     * @param productBaseId 产品ID
     * @return 产品对象
     */
    ProductBase getProductBase(String productBaseId);

    /**
     * 获取产品产品基本信息列表
     * @param page
     * @param req
     * @return
     */
    Page<ProductBaseGetResp> getProductBaseList(Page<ProductBaseGetResp> page,  @Param("req") ProductBaseListReq req);

    /**
     * 删除
     * @param productBaseId
     * @return
     */
    Integer softDelProdProductBase(String productBaseId);

    /**
     * 产品详情
     * @param productBaseId
     * @return
     */
    ProductDetailResp getProductDetail(@Param("productBaseId") String productBaseId);

    /**
     * 根据产品基本信息id查询省包平均供货价
     * @param productBaseId
     * @return
     */
    Double getAvgSupplyPrice(@Param("productBaseId") String productBaseId);
    
    public String selectisFixedLineByBatchId(@Param("batchId") String batchId) ;

    public String getSeq();

    /**
     * 获取不重复的unit_type数组
     * @param typeId
     * @param brandId
     * @return
     */
    List<String> getDistinctUnitType(@Param("typeId") String typeId, @Param("brandId") String brandId);

    /**
     * 查询产品基本表的基本信息
     * @param productId
     * @return
     */
    ProductBaseLightResp getProductBaseByProductId(@Param("productId") String productId);
}