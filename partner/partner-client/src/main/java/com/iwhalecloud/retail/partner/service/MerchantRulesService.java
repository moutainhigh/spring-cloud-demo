package com.iwhalecloud.retail.partner.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.partner.dto.MerchantRulesDTO;
import com.iwhalecloud.retail.partner.dto.MerchantRulesDetailDTO;
import com.iwhalecloud.retail.partner.dto.req.*;
import com.iwhalecloud.retail.partner.dto.resp.MerchantImeiRulesResp;
import com.iwhalecloud.retail.partner.dto.resp.MerchantRulesDetailPageResp;
import com.iwhalecloud.retail.partner.dto.resp.TransferPermissionGetResp;
import com.iwhalecloud.retail.system.dto.UserDTO;

import java.util.List;

public interface MerchantRulesService {

    /**
     * 添加一个或一组 商家 权限规则
     * @param req
     * @return
     */
    ResultVO<Integer> saveMerchantRules(MerchantRulesSaveReq req);

    /**
     * 删除 商家 权限规则
     * @param req
     * @return
     */
    ResultVO<Integer> deleteMerchantRules(MerchantRulesDeleteReq req);



//    /**
//     * 商家 权限规则 信息 列表查询
//     * @param req
//     * @return
//     */
//    ResultVO<List<MerchantRulesDTO>> listMerchantRules(MerchantRulesListReq req);

    /**
     * 商家 权限规则详情 信息 列表查询
     * @param req
     * @return
     */
    ResultVO<List<MerchantRulesDetailDTO>> listMerchantRulesDetail(MerchantRulesDetailListReq req);

    /**
     * 商家 权限规则信息 列表查询
     * @param req
     * @return
     */
    ResultVO<List<MerchantRulesDTO>> listMerchantRules(MerchantRulesDetailListReq req);

    /**
     * 商家 权限规则详情 信息 列表查询分页
     * @param req
     * @return
     */
    ResultVO<Page<MerchantRulesDetailDTO>> pageMerchantRulesDetail(MerchantRulesDetailListReq req);

    /**
     * 商家 权限规则详情 信息 分页查询
     * @param req
     * @return
     */
    ResultVO<Page<MerchantRulesDetailPageResp>> pageMerchantRules(MerchantRulesDetailPageReq req);


    /**
     * 获取调拨权限
     * @param merchantId
     * @return
     */
    ResultVO<TransferPermissionGetResp> getTransferPermission(String merchantId);

    /**
     * 获取经营权限--机型
     * @param merchantId
     * @return
     */
    ResultVO<List<String>> getBusinessModelPermission(String merchantId);


    /**
     * 获取绿色通道权限--机型
     * @param merchantId
     * @return
     */
    ResultVO<List<String>> getGreenChannelPermission(String merchantId);

    /**
     * 获取串码权限--机型
     * @param merchantId
     * @return
     */
    ResultVO<List<String>> getImeiPermission(String merchantId);


    /**
     * 通过查询对象获取targetId的集合
     * @param req
     * @return
     */
    ResultVO<List<String>> getCommonPermission(MerchantRulesCommonReq req);

    /**
     * 通过merchantId查询商家品牌和机型权限集合
     * @param merchantId
     * @return 机型id集合
     */
    ResultVO<List<String>> getProductAndBrandPermission(String merchantId);

    /**
     * 通过merchantId查询 调拨权限的 商家区域和对象权限集合
     * @param req
     * @return 机型id集合
     */
    ResultVO<List<String>> getTransferRegionAndMerchantPermission(MerchantRulesCommonReq req);

    /**
     * 通过merchantId查询 经营权限的   商家区域和对象权限集合
     * @param req
     * @return 机型id集合
     */
    ResultVO<List<String>> getBusinessRegionAndMerchantPermission(MerchantRulesCommonReq req);

    /**
     * 批量添加 商家 权限规则
     * @param req
     * @return
     */
    ResultVO<Integer> saveExcelMerchantRules(List<MerchantRuleSaveReq> req);

    /**
     * 商家权限规则校验
     *
     * @param req 规则校验入参
     * @return ResultVO
     */
    ResultVO<String> checkMerchantRules(MerchantRulesCheckReq req);

    /**
     * 增加厂商串码录入权限
     * @param req
     * @return
     */
    ResultVO saveFactoryMerchantImeiRule(MerchantRulesSaveReq req);

    /**
     * 取消厂商串码录入权限
     * @param req
     * @return
     */
    ResultVO delFactoryMerchantImeiRule(MerchantRulesSaveReq req, UserDTO userDTO);

    /**
     * 商家串码录入权限
     * @param merchantId
     * @return
     */
    ResultVO<List<MerchantImeiRulesResp>> listFactoryMerchantImeiRule(String merchantId);

    /**
     * 商家权限规则校验 商家是否有经营权限进行提示
     *
     * @param merchantId 规则校验入参
     * @param productBaseId 规则校验入参
     * @return ResultVO
     */
    ResultVO<Boolean> checkProdListRule(String merchantId,String productBaseId);


}