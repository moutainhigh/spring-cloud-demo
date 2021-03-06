package com.iwhalecloud.retail.partner.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.goods2b.dto.MerchantTagRelDTO;
import com.iwhalecloud.retail.goods2b.dto.req.MerchantTagRelListReq;
import com.iwhalecloud.retail.goods2b.service.dubbo.MerchantTagRelService;
import com.iwhalecloud.retail.goods2b.service.dubbo.ProductService;
import com.iwhalecloud.retail.partner.common.ParInvoiceConst;
import com.iwhalecloud.retail.partner.common.PartnerConst;
import com.iwhalecloud.retail.partner.dto.MerchantDTO;
import com.iwhalecloud.retail.partner.dto.MerchantDetailDTO;
import com.iwhalecloud.retail.partner.dto.req.*;
import com.iwhalecloud.retail.partner.dto.resp.*;
import com.iwhalecloud.retail.partner.entity.BusinessEntity;
import com.iwhalecloud.retail.partner.entity.Invoice;
import com.iwhalecloud.retail.partner.entity.Merchant;
import com.iwhalecloud.retail.partner.entity.MerchantAccount;
import com.iwhalecloud.retail.partner.manager.BusinessEntityManager;
import com.iwhalecloud.retail.partner.manager.InvoiceManager;
import com.iwhalecloud.retail.partner.manager.MerchantAccountManager;
import com.iwhalecloud.retail.partner.manager.MerchantManager;
import com.iwhalecloud.retail.partner.service.MerchantService;
import com.iwhalecloud.retail.system.common.SystemConst;
import com.iwhalecloud.retail.system.dto.CommonFileDTO;
import com.iwhalecloud.retail.system.dto.CommonRegionDTO;
import com.iwhalecloud.retail.system.dto.OrganizationDTO;
import com.iwhalecloud.retail.system.dto.UserDTO;
import com.iwhalecloud.retail.system.dto.request.*;
import com.iwhalecloud.retail.system.dto.response.OrganizationListResp;
import com.iwhalecloud.retail.system.service.*;
import com.iwhalecloud.retail.workflow.common.WorkFlowConst;
import com.iwhalecloud.retail.workflow.dto.req.ProcessStartReq;
import com.iwhalecloud.retail.workflow.service.TaskService;
import com.twmacinta.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

import static com.iwhalecloud.retail.dto.ResultVO.success;

@Slf4j
@Service(timeout = 20000)
@Component("merchantService")
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantManager merchantManager;

    @Reference
    private UserService userService;

    @Reference
    private CommonRegionService commonRegionService;

    @Reference
    private OrganizationService organizationService;

    @Reference
    private MerchantTagRelService merchantTagRelService;

    @Autowired
    private InvoiceManager invoiceManager;

    @Autowired
    private MerchantAccountManager merchantAccountManager;

    @Autowired
    private BusinessEntityManager businessEntityManager;

    @Reference
    private CommonFileService commonFileService;

    @Reference
    private TaskService taskService;

    @Reference
    private ProductService productService;

    @Reference
    private ZopMessageService zopMessageService;

    /**
     * 根据条件 查询商家条数
     *
     * @param req
     * @return
     */
    @Override
    public ResultVO<Integer> countMerchant(MerchantCountReq req) {
        log.info("MerchantServiceImpl.countMerchant(), input: MerchantCountReq={} ", JSON.toJSONString(req));
        ResultVO<Integer> resultVO = ResultVO.success(merchantManager.countMerchant(req));
        log.info("MerchantServiceImpl.countMerchant(), output: resultVO={} ", JSON.toJSONString(resultVO));
        return resultVO;
    }


    /**
     * 根据商家id 获取一个 商家 概要信息（字段不够用的话 用getMerchantDetail（）取）
     *
     * @param merchantId
     * @return
     */
    @Override
    public ResultVO<MerchantDTO> getMerchantById(String merchantId) {
        log.info("MerchantServiceImpl.getMerchantById(), input: merchantId={} ", merchantId);
        MerchantGetReq req = new MerchantGetReq();
        req.setMerchantId(merchantId);
        Merchant merchant = merchantManager.getMerchant(req);
        MerchantDTO merchantDTO = new MerchantDTO();
        if (merchant == null) {
            merchantDTO = null;
        } else {
            BeanUtils.copyProperties(merchant, merchantDTO);
            // 取本地网名称  市县名称
            merchantDTO.setLanName(getRegionNameByRegionId(merchantDTO.getLanId()));
            merchantDTO.setCityName(getRegionNameByRegionId(merchantDTO.getCity()));
        }
        log.info("MerchantServiceImpl.getMerchantById(), output: merchantDTO={} ", JSON.toJSONString(merchantDTO));
        return ResultVO.success(merchantDTO);
    }

    /**
     * 根据商家编码 获取一个 商家 概要信息（字段不够用的话 用getMerchantDetail（）取）
     *
     * @param merchantCode
     * @return
     */
    @Override
    public ResultVO<MerchantDTO> getMerchantByCode(String merchantCode) {
        log.info("MerchantServiceImpl.getMerchantByCode(), input: merchantCode={} ", merchantCode);
        MerchantGetReq req = new MerchantGetReq();
        req.setMerchantCode(merchantCode);
        Merchant merchant = merchantManager.getMerchant(req);
        MerchantDTO merchantDTO = new MerchantDTO();
        if (merchant == null) {
            merchantDTO = null;
        } else {
            BeanUtils.copyProperties(merchant, merchantDTO);
            // 取本地网名称  市县名称
            merchantDTO.setLanName(getRegionNameByRegionId(merchantDTO.getLanId()));
            merchantDTO.setCityName(getRegionNameByRegionId(merchantDTO.getCity()));
        }
        log.info("MerchantServiceImpl.getMerchantByCode(), output: merchantDTO={} ", JSON.toJSONString(merchantDTO));
        return ResultVO.success(merchantDTO);
    }

    /**
     * 获取一个 商家 概要信息（字段不够用的话 用getMerchantDetail（）取）
     *
     * @param req
     * @return
     */
    @Override
    public ResultVO<MerchantDTO> getMerchant(MerchantGetReq req) {
        log.info("MerchantServiceImpl.getMerchant(), input: MerchantGetReq={} ", JSON.toJSONString(req));
        Merchant merchant = merchantManager.getMerchant(req);
        MerchantDTO merchantDTO = new MerchantDTO();
        if (merchant == null) {
            merchantDTO = null;
        } else {
            BeanUtils.copyProperties(merchant, merchantDTO);
            // 取本地网名称  市县名称
            merchantDTO.setLanName(getRegionNameByRegionId(merchantDTO.getLanId()));
            merchantDTO.setCityName(getRegionNameByRegionId(merchantDTO.getCity()));

            // 获取经营主体ID
            if (!StringUtils.isEmpty(merchantDTO.getBusinessEntityCode())) {
                BusinessEntityGetReq businessEntityGetReq = new BusinessEntityGetReq();
                businessEntityGetReq.setBusinessEntityCode(merchantDTO.getBusinessEntityCode());
                BusinessEntity businessEntity = businessEntityManager.getBusinessEntity(businessEntityGetReq);
                if (businessEntity != null) {
                    merchantDTO.setBusinessEntityId(businessEntity.getBusinessEntityId());
                }
            }
        }

        log.info("MerchantServiceImpl.getMerchant(), output: MerchantDTO={} ", JSON.toJSONString(merchantDTO));
        return ResultVO.success(merchantDTO);
    }


    /**
     * 获取一个 商家详情（取全表的）
     *
     * @param req
     * @return
     */
    @Override
    public ResultVO<MerchantDetailDTO> getMerchantDetail(MerchantGetReq req) {
        log.info("MerchantServiceImpl.getMerchantDetail(), input: MerchantGetReq={} ", JSON.toJSONString(req));
        Merchant merchant = merchantManager.getMerchant(req);
        MerchantDetailDTO merchantDetailDTO = new MerchantDetailDTO();
        if (merchant == null) {
            merchantDetailDTO = null;
        } else {
            BeanUtils.copyProperties(merchant, merchantDetailDTO);

            // 取本地网名称  市县名称
            merchantDetailDTO.setLanName(getRegionNameByRegionId(merchantDetailDTO.getLanId()));
            merchantDetailDTO.setCityName(getRegionNameByRegionId(merchantDetailDTO.getCity()));

            // 营业执照号、税号、公司账号、营业执照失效期
            InvoiceListReq invoiceListReq = new InvoiceListReq();
            invoiceListReq.setMerchantId(merchantDetailDTO.getMerchantId());
            invoiceListReq.setInvoiceType(ParInvoiceConst.InvoiceType.SPECIAL_VAT_INVOICE.getCode());
            List<Invoice> invoiceList = invoiceManager.listInvoice(invoiceListReq);
            if (!CollectionUtils.isEmpty(invoiceList)) {
                // 取第一条数据
                BeanUtils.copyProperties(invoiceList.get(0), merchantDetailDTO);
            }

            // “系统账号”、“系统状态”
            if (!StringUtils.isEmpty(merchantDetailDTO.getMerchantId())) {
                UserGetReq userGetReq = new UserGetReq();
                userGetReq.setRelCode(merchantDetailDTO.getMerchantId());
                UserDTO userDTO = userService.getUser(userGetReq);
                if (userDTO != null) {
                    merchantDetailDTO.setLoginName(userDTO.getLoginName());
                    merchantDetailDTO.setUserStatus(userDTO.getStatusCd().toString());
                }
            }

            // 取经营单元（三级组织） 、 营销支局（四级组织）
            // 根据parCrmOrgPathCode取3、4级组织ID （如果有）
            String orgIdWithLevel3 = getOrgIdByPathCode(merchantDetailDTO.getParCrmOrgPathCode(), 3);
            String orgIdWithLevel4 = getOrgIdByPathCode(merchantDetailDTO.getParCrmOrgPathCode(), 4);
            merchantDetailDTO.setOrgIdWithLevel3(orgIdWithLevel3);
            merchantDetailDTO.setOrgIdWithLevel4(orgIdWithLevel4);
            if (StringUtils.isNotEmpty(orgIdWithLevel3)) {
                OrganizationDTO dto = organizationService.getOrganization(orgIdWithLevel3).getResultData();
                if (Objects.nonNull(dto)) {
                    merchantDetailDTO.setOrgNameWithLevel3(dto.getOrgName());
                }
            }
            if (StringUtils.isNotEmpty(orgIdWithLevel4)) {
                OrganizationDTO dto = organizationService.getOrganization(orgIdWithLevel4).getResultData();
                if (Objects.nonNull(dto)) {
                    merchantDetailDTO.setOrgNameWithLevel4(dto.getOrgName());
                }
            }


        }
        log.info("MerchantServiceImpl.getMerchantDetail(), output: merchantDetailDTO={} ", JSON.toJSONString(merchantDetailDTO));
        return ResultVO.success(merchantDetailDTO);
    }

    /**
     * 更新 商家
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResultVO<Integer> updateMerchant(MerchantUpdateReq req) {
        log.info("MerchantServiceImpl.updateMerchant() input：MerchantUpdateReq={}", req.toString());
        Merchant merchant = new Merchant();
        BeanUtils.copyProperties(req, merchant);
        int result = merchantManager.updateMerchant(merchant);
        log.info("MerchantServiceImpl.updateMerchant() output：update effect row num={}", result);
        if (result <= 0) {
            return ResultVO.error("更新商家信息失败");
        }
        //如果修改商家状态值，则同步用户状态
        if (!StringUtils.isEmpty(req.getStatus())) {
            // 可能有绑定多个用户
            UserListReq userListReq = new UserListReq();
            userListReq.setRelCode(merchant.getMerchantId());
            List<UserDTO> userDTOList = userService.getUserList(userListReq);
            if (!CollectionUtils.isEmpty(userDTOList)) {
                // 有绑定用户
                try {
                    updateUserBYMerchantCode(userDTOList, merchant);
                } catch (Exception e) {
                    // 更新商家信息失败
                    throw new RuntimeException("更新商家信息失败");
                }
            }
        }
        return ResultVO.success(result);
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO<Integer> updateMerchantBatch(MerchantUpdateBatchReq req) {
        log.info("MerchantServiceImpl.updateMerchantBatch req={}", req.toString());
        final int[] result = {0};
        List<String> merchantIdList = req.getMerchantIdList();
        if (CollectionUtils.isEmpty(merchantIdList)) {
            return ResultVO.success(result[0]);
        }
        merchantIdList.forEach(item -> {
            Merchant merchant = new Merchant();
            merchant.setMerchantType(req.getMerchantType());
            merchant.setStatus(req.getStatus());
            merchant.setMerchantId(item);
            result[0] += merchantManager.updateMerchant(merchant);
            log.info("MerchantServiceImpl.updateMerchant output：update effect row num={}", result[0]);
            if (result[0] > 0) {
                if (!StringUtils.isEmpty(req.getStatus())) {
                    // 可能有绑定多个用户
                    UserListReq userListReq = new UserListReq();
                    userListReq.setRelCode(merchant.getMerchantId());
                    List<UserDTO> userDTOList = userService.getUserList(userListReq);
                    if (!CollectionUtils.isEmpty(userDTOList)) {
                        // 有绑定用户
                        try {
                            updateUserBYMerchantCode(userDTOList, merchant);
                        } catch (Exception e) {
                            // 更新商家信息失败
                            log.error("更新商家信息失败 error={}", e.getMessage());
                            //手动回滚
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        }
                    }
                }
            }
        });
        return ResultVO.success(result[0]);
    }

    /**
     * 根据商家状态设置用户状态
     *
     * @param req
     * @return
     */
    private void updateUserBYMerchantCode(List<UserDTO> userDTOList, Merchant req) throws Exception {
        log.info("MerchantServiceImpl.updateUserBYMerchantCode() input：userDTOList={} Merchant={}", JSON.toJSONString(userDTOList), JSON.toJSONString(req));

        /*
         * 根据商家状态设置用户状态：
         * 1.如果商家状态值为空，则不执行用户绑定商家操作；
         * 2.如果商家状态值为有效VALID：1000,设置用户状态值为有效USER_STATUS_VALID=1;
         * 3.如果商家状态值为除有效外的其他值，设置用户状态值为禁用USER_STATUS_INVALID=0;
         * */
        if (!StringUtils.isEmpty(req.getStatus())
                && !CollectionUtils.isEmpty(userDTOList)) {

            for (UserDTO userDTO : userDTOList) {
                UserEditReq userEditReq = new UserEditReq();
//                BeanUtils.copyProperties(userDTO, userEditReq);
                userEditReq.setUserId(userDTO.getUserId());

                // 如果商家状态值为有效VALID：1000,设置用户状态值为有效USER_STATUS_VALID=1;
                if (PartnerConst.MerchantStatusEnum.VALID.getType().equals(req.getStatus())) {
                    userEditReq.setStatusCd(SystemConst.USER_STATUS_VALID);
                } else {
                    // 如果商家状态值为除有效外的其他值，设置用户状态值为禁用USER_STATUS_INVALID=0;
                    userEditReq.setStatusCd(SystemConst.USER_STATUS_INVALID);
                }
                ResultVO editUserResultVO = userService.editUser(userEditReq);
                if (!editUserResultVO.isSuccess()) {
                    // 抛出异常 回滚
                    throw new Exception();
                }
            }
        }
    }

    /**
     * 商家 信息列表（只取 部分必要字段）
     *
     * @param req
     * @return
     */
    @Override
    public ResultVO<List<MerchantDTO>> listMerchant(MerchantListReq req) {
        log.info("MerchantServiceImpl.listMerchant(), input: MerchantListReq={} ", JSON.toJSONString(req));

        List<MerchantDTO> list = merchantManager.listMerchant(req);

        if (!CollectionUtils.isEmpty(list) && BooleanUtils.isTrue(req.getNeedOtherTableFields())) {

            /***** 其他表字段  统一获取  避免循环获取  不然导出调用时可能会出现超时问题 *****/

            // 取本地网  市县  ID集合
            HashSet<String> regionIdHashSet = new HashSet<>(); // 去重
            for (MerchantDTO dto : list) {
                regionIdHashSet.add(dto.getLanId());
                regionIdHashSet.add(dto.getCity());
            }

            Map<String, String> regionNamesMap = getRegionNamesMap(regionIdHashSet);

            // 取本地网名称  市县名称
            for (MerchantDTO dto : list) {
                // 取本地网名称  市县名称
                dto.setLanName(regionNamesMap.get(dto.getLanId()));
                dto.setCityName(regionNamesMap.get(dto.getCity()));
            }

        }

        //数据量太大，改为输出大小
        log.info("MerchantServiceImpl.listMerchant(), output: list.size={} ", JSON.toJSONString(getSize(list)));
        return ResultVO.success(list);
    }

    private int getSize(List list) {
        if (Objects.isNull(list)) {
            return 0;
        }
        return list.size();
    }

    /**
     * 商家 信息列表分页
     *
     * @param pageReq
     * @return
     */
    @Override
    public ResultVO<Page<MerchantPageResp>> pageMerchant(MerchantPageReq pageReq) {
        log.info("MerchantServiceImpl.pageMerchant(), input: MerchantPageReq={} ", JSON.toJSONString(pageReq));
        Page<MerchantPageResp> page = merchantManager.pageMerchant(pageReq);

        /***** 其他表字段  统一获取  避免循环获取  不然导出调用时可能会出现超时问题 *****/

        if (!CollectionUtils.isEmpty(page.getRecords())) {

            // 取本地网  市县  ID集合  去重
            HashSet<String> regionIdHashSet = new HashSet<>();
            // 所有商家ID去重
            HashSet<String> merchantIdHashSet = new HashSet<>();

            for (MerchantPageResp merchantDTO : page.getRecords()) {
                // 取本地网  市县  ID集合
                regionIdHashSet.add(merchantDTO.getLanId());
                regionIdHashSet.add(merchantDTO.getCity());

                // 取merchantId集合
                merchantIdHashSet.add(merchantDTO.getMerchantId());
            }

            Map<String, String> regionNamesMap = getRegionNamesMap(regionIdHashSet);
            Map<String, String> loginNamesMap = getLoginNamesMap(merchantIdHashSet);
            Map<String, String> tagNamesMap = getTagNamesMap(merchantIdHashSet);

            // 取本地网名称  市县名称
            for (MerchantPageResp merchantDTO : page.getRecords()) {
                // 取本地网名称  市县名称
                merchantDTO.setLanName(regionNamesMap.get(merchantDTO.getLanId()));
                merchantDTO.setCityName(regionNamesMap.get(merchantDTO.getCity()));

                // 取商家的登录名称
                merchantDTO.setLoginName(loginNamesMap.get(merchantDTO.getMerchantId()));

                // 设置标签
                merchantDTO.setTagNames(tagNamesMap.get(merchantDTO.getMerchantId()));
            }
        }
        log.info("MerchantServiceImpl.pageMerchant() output: list<MerchantPageResp>.siz()={} ", JSON.toJSONString(page.getRecords().size()));
        return ResultVO.success(page);
    }

    /**
     * 商家 绑定 用户
     *
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackForClassName = "Exception")
    public ResultVO<Integer> bindUser(MerchantBindUserReq req) throws Exception {
        log.info("MerchantServiceImpl.bindUser() input：MerchantBindUserReq={}", req.toString());
        //TODO 获取用户信息  校对 用户类型和商家类型

        // 更新商家
        Merchant merchant = new Merchant();
//        merchant.setMerchantId(req.getMerchantId());
//        merchant.setUserId(req.getUserId());
//        merchant.setMerchantType(req.getMerchantType());
        BeanUtils.copyProperties(req, merchant);
        int result = merchantManager.updateMerchant(merchant);
        log.info("MerchantServiceImpl.bindUser() output：更新商家信息影响的行数={}", result);
        if (result <= 0) {
            return ResultVO.error("更新商家信息失败");
        }

        // 用户绑定商家
        UserEditReq userEditReq = new UserEditReq();
        userEditReq.setUserId(req.getUserId());
        userEditReq.setRelCode(req.getMerchantId());

//        if (req.getMerchantType() == PartnerConst.MerchantTypeEnum.MANUFACTURER.getType()) {
//            // 商家
//            userEditReq.setUserFounder(SystemConst.USER_FOUNDER_8);
//        }
//        switch (req.getMerchantType()) {
//            case PartnerConst.MerchantTypeEnum.MANUFACTURER.getType():
//            ;
//            case "11":
//
//        }
//        ResultVO editUserResultVO = userService.editUser(userEditReq);
        ResultVO editUserResultVO = ResultVO.error();
        if (!editUserResultVO.isSuccess()) {
            // 抛出异常 回滚
            throw new Exception();
        }

        return ResultVO.success(result);
    }

    /**
     * 零售商类型的 商家 信息列表分页
     *
     * @param pageReq
     * @return
     */
    @Override
    public ResultVO<Page<RetailMerchantDTO>> pageRetailMerchant(RetailMerchantPageReq pageReq) {
        log.info("MerchantServiceImpl.pageRetailMerchant() input：RetailMerchantPageReq={}", JSON.toJSONString(pageReq));
        Page<Merchant> merchantPage = merchantManager.pageRetailMerchant(pageReq);

        // 零售商列表字段:
        // 店中商编码、店中商名称、店中商经营主体名称、销售点名称、销售点编码、
        // 区域（分公司、市县、分局/县部门、营销中心/支局）、渠道视图状态、渠道大类、渠道小类、渠道子类。
        // 分组标签
        // 营业执照失效期

        // 转换成 对应的 dto
        Page<RetailMerchantDTO> targetPage = new Page<>();
        BeanUtils.copyProperties(merchantPage, targetPage);
        List<RetailMerchantDTO> targetList = Lists.newArrayList();

        /***** 其他表字段  统一获取  避免循环获取  不然导出调用时可能会出现超时问题 *****/
        if (!CollectionUtils.isEmpty(merchantPage.getRecords())) {

            // 取本地网  市县  ID集合  去重
            HashSet<String> regionIdHashSet = new HashSet<>();
            // 所有商家ID去重
            HashSet<String> merchantIdHashSet = new HashSet<>();
            // 所有组织id去重
            HashSet<String> orgIdHashSet = new HashSet<>();

            for (Merchant entity : merchantPage.getRecords()) {

                RetailMerchantDTO targetDTO = new RetailMerchantDTO();
                BeanUtils.copyProperties(entity, targetDTO);

                // 根据parCrmOrgPathCode取3、4级组织ID （如果有）
                String orgIdWithLevel3 = getOrgIdByPathCode(targetDTO.getParCrmOrgPathCode(), 3);
                String orgIdWithLevel4 = getOrgIdByPathCode(targetDTO.getParCrmOrgPathCode(), 4);
                targetDTO.setOrgIdWithLevel3(orgIdWithLevel3);
                targetDTO.setOrgIdWithLevel4(orgIdWithLevel4);

                targetList.add(targetDTO);

                // 取组织  ID集合
                orgIdHashSet.add(orgIdWithLevel3);
                orgIdHashSet.add(orgIdWithLevel4);

                // 取本地网  市县  ID集合
                regionIdHashSet.add(entity.getLanId());
                regionIdHashSet.add(entity.getCity());

                // 取merchantId集合
                merchantIdHashSet.add(entity.getMerchantId());
            }

            Map<String, String> regionNamesMap = getRegionNamesMap(regionIdHashSet);
            Map<String, Invoice> invoiceMap = getInvoiceMap(merchantIdHashSet);
            Map<String, String> tagNamesMap = getTagNamesMap(merchantIdHashSet);
            Map<String, String> orgNamesMap = getOrgNamesMap(orgIdHashSet);
            Map<String, String> loginNamesMap = getLoginNamesMap(merchantIdHashSet);

            // 设置 对应要翻译的名称
            for (RetailMerchantDTO dto : targetList) {

                // 设置 3\4级组织名称
                dto.setOrgNameWithLevel3(orgNamesMap.get(dto.getOrgIdWithLevel3()));
                dto.setOrgNameWithLevel4(orgNamesMap.get(dto.getOrgIdWithLevel4()));

                // 设置 本地网名称  市县名称
                dto.setLanName(regionNamesMap.get(dto.getLanId()));
                dto.setCityName(regionNamesMap.get(dto.getCity()));

                // 取商家的登录名称
                dto.setLoginName(loginNamesMap.get(dto.getMerchantId()));

                // 设置 发票里面的相关信息
                Invoice invoice = invoiceMap.get(dto.getMerchantId());
                if (Objects.nonNull(invoice)) {
                    dto.setBusiLicenceCode(invoice.getBusiLicenceCode());
                    dto.setBusiLicenceExpDate(invoice.getBusiLicenceExpDate());
                    dto.setTaxCode(invoice.getTaxCode());
                    dto.setRegisterBankAcct(invoice.getRegisterBankAcct());
                    // 不要用copyProperties  有可能两个表的数据不一样 覆盖商家名称
                    //                BeanUtils.copyProperties(invoice, dto);
                }
                // 设置标签
                dto.setTagNames(tagNamesMap.get(dto.getMerchantId()));
            }
        }

        targetPage.setRecords(targetList);

        log.info("MerchantServiceImpl.pageRetailMerchant() output：list<RetailMerchantDTO>.size()={}", JSON.toJSONString(targetPage.getRecords().size()));
        return ResultVO.success(targetPage);
    }

    /**
     * 根据pathCode 获取对应等级orgId
     *
     * @param pathCode
     * @param level    取第几级 （从0开始)
     */
    private String getOrgIdByPathCode(String pathCode, int level) {
        // pathCode示例：1000000020.843000000000000.843073800000000.843073805020000.843073805021007
        //  比如level= 3  取的值 是 843073805020000
        if (StringUtils.isEmpty(pathCode) || level < 0) {
            return null;
        }
        // 要用 \\.  转义分割符号
        String[] orgIds = pathCode.split("\\.");
        if (orgIds.length < level + 1) {
            // 不够个数
            return null;
        }
//        String orgId = null;
//        orgId = orgIds[level];
//        return orgId;
        return orgIds[level];
    }


    /**
     * 供应商类型的 商家 信息列表分页
     *
     * @param pageReq
     * @return
     */
    @Override
    public ResultVO<Page<SupplyMerchantDTO>> pageSupplyMerchant(SupplyMerchantPageReq pageReq) {
        log.info("MerchantServiceImpl.pageSupplyMerchant() input：SupplyMerchantPageReq={}", JSON.toJSONString(pageReq));
        Page<Merchant> merchantPage = merchantManager.pageSupplierMerchant(pageReq);

        // 地包商、国/省包商列表字段：
        // 商家编码、商家名称、经营主体名称、渠道视图状态、
        // 分公司、市县（sys_common_region 表字段）
        // 翼支付收款账号、支付宝收款账号 (par_merchant_account 表字段）
        // 营业执照号、税号、公司账号、营业执照失效期 （par_invoice 表字段）
        // 转换成 对应的 dto
        Page<SupplyMerchantDTO> targetPage = new Page<>();
        BeanUtils.copyProperties(merchantPage, targetPage);
        List<SupplyMerchantDTO> targetList = Lists.newArrayList();

        /***** 其他表字段  统一获取  避免循环获取  不然导出调用时可能会出现超时问题 *****/
        if (!CollectionUtils.isEmpty(merchantPage.getRecords())) {

            // 取本地网  市县  ID集合  去重
            HashSet<String> regionIdHashSet = new HashSet<>();
            // 所有商家ID去重
            HashSet<String> merchantIdHashSet = new HashSet<>();

            for (Merchant entity : merchantPage.getRecords()) {

                SupplyMerchantDTO targetDTO = new SupplyMerchantDTO();
                BeanUtils.copyProperties(entity, targetDTO);
                targetList.add(targetDTO);

                // 取本地网  市县  ID集合
                regionIdHashSet.add(entity.getLanId());
                regionIdHashSet.add(entity.getCity());

                // 取merchantId集合
                merchantIdHashSet.add(entity.getMerchantId());
            }

            Map<String, String> regionNamesMap = getRegionNamesMap(regionIdHashSet);
            Map<String, Invoice> invoiceMap = getInvoiceMap(merchantIdHashSet);
            Map<String, String> merchantAccountMap = getMerchantAccountMap(merchantIdHashSet);
            Map<String, String> loginNamesMap = getLoginNamesMap(merchantIdHashSet);
            Map<String, String> tagNamesMap = getTagNamesMap(merchantIdHashSet);

            // 取本地网名称  市县名称
            for (SupplyMerchantDTO dto : targetList) {

                // 设置 本地网名称  市县名称
                dto.setLanName(regionNamesMap.get(dto.getLanId()));
                dto.setCityName(regionNamesMap.get(dto.getCity()));

                // 取商家的登录名称
                dto.setLoginName(loginNamesMap.get(dto.getMerchantId()));

                // 设置 发票里面的相关信息
                Invoice invoice = invoiceMap.get(dto.getMerchantId());
                if (Objects.nonNull(invoice)) {
                    dto.setBusiLicenceCode(invoice.getBusiLicenceCode());
                    dto.setBusiLicenceExpDate(invoice.getBusiLicenceExpDate());
                    dto.setTaxCode(invoice.getTaxCode());
                    dto.setRegisterBankAcct(invoice.getRegisterBankAcct());
                    // 不要用copyProperties  有可能两个表的数据不一样 覆盖商家名称
//                BeanUtils.copyProperties(invoice, dto);
                }

                // 设置 账户名称
                dto.setAccount(merchantAccountMap.get(dto.getMerchantId()));

                // 设置标签
                dto.setTagNames(tagNamesMap.get(dto.getMerchantId()));
            }
        }

        targetPage.setRecords(targetList);
        log.info("MerchantServiceImpl.pageSupplyMerchant() output：list<SupplyMerchantDTO>.size()={}", JSON.toJSONString(targetPage.getRecords().size()));
        return ResultVO.success(targetPage);
    }

    /**
     * 厂商类型的 商家 信息列表分页
     *
     * @param pageReq
     * @return
     */
    @Override
    public ResultVO<Page<FactoryMerchantDTO>> pageFactoryMerchant(FactoryMerchantPageReq pageReq) {
        log.info("MerchantServiceImpl.pageFactoryMerchant() input：FactoryMerchantPageReq={}", JSON.toJSONString(pageReq));
        Page<Merchant> merchantPage = merchantManager.pageFactoryMerchant(pageReq);

        // 厂商列表字段：商家编码、商家名称、分公司、市县、渠道视图状态
        // 转换成 对应的 dto
        Page<FactoryMerchantDTO> targetPage = new Page<>();
        BeanUtils.copyProperties(merchantPage, targetPage);
        List<FactoryMerchantDTO> targetList = Lists.newArrayList();

        /***** 其他表字段  统一获取  避免循环获取  不然导出调用时可能会出现超时问题 *****/
        if (!CollectionUtils.isEmpty(merchantPage.getRecords())) {

            // 取本地网  市县  ID集合  去重
            HashSet<String> regionIdHashSet = new HashSet<>();
            // 所有商家ID去重
            HashSet<String> merchantIdHashSet = new HashSet<>();

            for (Merchant entity : merchantPage.getRecords()) {

                FactoryMerchantDTO targetDTO = new FactoryMerchantDTO();
                BeanUtils.copyProperties(entity, targetDTO);
                targetList.add(targetDTO);

                // 取本地网  市县  ID集合
                regionIdHashSet.add(entity.getLanId());
                regionIdHashSet.add(entity.getCity());

                // 取merchantId集合
                merchantIdHashSet.add(entity.getMerchantId());
            }

            Map<String, String> regionNamesMap = getRegionNamesMap(regionIdHashSet);
            Map<String, String> loginNamesMap = getLoginNamesMap(merchantIdHashSet);

            // 取本地网名称  市县名称
            for (FactoryMerchantDTO dto : targetList) {

                // 设置 本地网名称  市县名称
                dto.setLanName(regionNamesMap.get(dto.getLanId()));
                dto.setCityName(regionNamesMap.get(dto.getCity()));

                // 取商家的登录名称
                dto.setLoginName(loginNamesMap.get(dto.getMerchantId()));

            }
        }

        targetPage.setRecords(targetList);

        log.info("MerchantServiceImpl.pageFactoryMerchant() output：list<FactoryMerchantDTO>.size()={}", JSON.toJSONString(targetPage.getRecords().size()));
        return ResultVO.success(targetPage);
    }

    /**
     * 根据regionId集合获取所有的  区域名称
     *
     * @param orgIdHashSet
     * @return
     */
    private Map<String, String> getOrgNamesMap(HashSet<String> orgIdHashSet) {
        Map resultMap = new HashMap();
        if (CollectionUtils.isEmpty(orgIdHashSet)) {
            return resultMap;
        }
        OrganizationListReq req = new OrganizationListReq();
        req.setOrgIdList(Lists.newArrayList(orgIdHashSet));
        List<OrganizationListResp> dtoList = organizationService.listOrganization(req).getResultData();
        if (!CollectionUtils.isEmpty(dtoList)) {
            dtoList.forEach(dto -> {
                resultMap.put(dto.getOrgId(), dto.getOrgName());
            });
        }
        return resultMap;
    }

    /**
     * 根据merchantId集合获取所有的  登录名称
     *
     * @param merchantIdHashSet
     * @return
     */
    private Map<String, String> getLoginNamesMap(HashSet<String> merchantIdHashSet) {
        UserListReq req = new UserListReq();
        req.setRelCodeList(Lists.newArrayList(merchantIdHashSet));
        List<UserDTO> dtoList = userService.getUserList(req);
        Map resultMap = new HashMap();
        if (!CollectionUtils.isEmpty(dtoList)) {
            dtoList.forEach(userDTO -> {
                resultMap.put(userDTO.getRelCode(), userDTO.getLoginName());
            });
        }
        return resultMap;
    }

    /**
     * 根据regionId集合获取所有的  区域名称
     *
     * @param regionIdHashSet
     * @return
     */
    private Map<String, String> getRegionNamesMap(HashSet<String> regionIdHashSet) {
        CommonRegionListReq req = new CommonRegionListReq();
        req.setRegionIdList(Lists.newArrayList(regionIdHashSet));
        List<CommonRegionDTO> dtoList = commonRegionService.listCommonRegion(req).getResultData();
        Map resultMap = new HashMap();
        if (!CollectionUtils.isEmpty(dtoList)) {
            dtoList.forEach(commonRegionDTO -> {
                resultMap.put(commonRegionDTO.getRegionId(), commonRegionDTO.getRegionName());
            });
        }
        return resultMap;
    }

    /**
     * 根据商家ID集合获取所有的 发票信息
     *
     * @param merchantIdHashSet
     * @return
     */
    private Map<String, Invoice> getInvoiceMap(HashSet<String> merchantIdHashSet) {
        Map<String, Invoice> resultMap = new HashMap();
        if (CollectionUtils.isEmpty(merchantIdHashSet)) {
            return resultMap;
        }
        // 营业执照号、税号、公司账号、营业执照失效期
        InvoiceListReq invoiceListReq = new InvoiceListReq();
        invoiceListReq.setMerchantIdList(Lists.newArrayList(merchantIdHashSet));
        invoiceListReq.setInvoiceType(ParInvoiceConst.InvoiceType.SPECIAL_VAT_INVOICE.getCode());
        List<Invoice> invoiceList = invoiceManager.listInvoice(invoiceListReq);
        if (!CollectionUtils.isEmpty(invoiceList)) {
            invoiceList.forEach(invoice -> {
                resultMap.put(invoice.getMerchantId(), invoice);
            });
        }
        return resultMap;
    }

    /**
     * 根据商家ID集合获取所有的 发票信息
     *
     * @param merchantIdHashSet
     * @return
     */
    private Map<String, String> getMerchantAccountMap(HashSet<String> merchantIdHashSet) {

        // 取收款账号
        MerchantAccountListReq accountListReq = new MerchantAccountListReq();
        accountListReq.setMerchantIdList(Lists.newArrayList(merchantIdHashSet));
        accountListReq.setAccountType(PartnerConst.MerchantAccountTypeEnum.BEST_PAY.getType());
        List<MerchantAccount> merchantAccountList = merchantAccountManager.listMerchantAccount(accountListReq);
        Map<String, String> resultMap = new HashMap();
        if (!CollectionUtils.isEmpty(merchantAccountList)) {
            merchantAccountList.forEach(merchantAccount -> {
                resultMap.put(merchantAccount.getMerchantId(), merchantAccount.getAccount());
            });
        }
        return resultMap;
    }

    /**
     * 根据商家ID集合获取所有的 标签名称
     *
     * @param merchantIdHashSet
     * @return
     */
    private Map<String, String> getTagNamesMap(HashSet<String> merchantIdHashSet) {

        // 标签信息
        MerchantTagRelListReq tagRelListReq = new MerchantTagRelListReq();
        tagRelListReq.setMerchantIdList(Lists.newArrayList(merchantIdHashSet));
        List<MerchantTagRelDTO> merchantTagRelDTOList = merchantTagRelService.listMerchantTagRel(tagRelListReq).getResultData();
        Map<String, String> resultMap = new HashMap();
        if (!CollectionUtils.isEmpty(merchantTagRelDTOList)) {
            merchantTagRelDTOList.forEach(dto -> {
                String str = resultMap.get(dto.getMerchantId());
                if (StringUtils.isEmpty(str)) {
                    resultMap.put(dto.getMerchantId(), dto.getTagName());
                } else {  // 多个标签的  要拼装
                    str = str + "," + dto.getTagName();
                    resultMap.put(dto.getMerchantId(), str);
                }
            });
        }

        return resultMap;
    }

    /**
     * 根据regionId获取 regionName
     *
     * @param regionId
     * @return
     */
    private String getRegionNameByRegionId(String regionId) {
        if (StringUtils.isEmpty(regionId)) {
            return "";
        }
        CommonRegionDTO commonRegionDTO = commonRegionService.getCommonRegionById(regionId).getResultData();
        if (commonRegionDTO != null) {
            return commonRegionDTO.getRegionName();
        }
        return "";
    }

    @Override
    public ResultVO<List<String>> listMerchantByLanCity(MerchantListLanCityReq req) {
        log.info("MerchantServiceImpl.listMerchantByLanCity MerchantListReq={}", JSON.toJSON(req));
        List<Merchant> merchants = merchantManager.listMerchantByLanCity(req);
        log.info("MerchantServiceImpl.listMerchantByLanCity merchantManager.listMerchantByLanCity merchants.size()={}", JSON.toJSON(merchants.size()));
        List<String> merchantIds = merchants.stream().distinct().map(Merchant::getMerchantId).collect(Collectors.toList());
        return ResultVO.success(merchantIds);
    }

    /**
     * 根据商家id 获取一个 商家 概要信息（字段不够用的话 用getMerchantDetail（）取）
     *
     * @param merchantId
     * @return
     */
    @Override
    public MerchantDTO getMerchantInfoById(String merchantId) {
        log.info("MerchantServiceImpl.getMerchantById(), input: merchantId={} ", merchantId);
        MerchantGetReq req = new MerchantGetReq();
        req.setMerchantId(merchantId);
        Merchant merchant = merchantManager.getMerchant(req);
        MerchantDTO merchantDTO = new MerchantDTO();
        if (merchant == null) {
            merchantDTO = null;
        } else {
            BeanUtils.copyProperties(merchant, merchantDTO);
        }
        log.info("MerchantServiceImpl.getMerchantById(), output: merchantDTO={} ", JSON.toJSONString(merchantDTO));
        return merchantDTO;
    }

    @Override
    public ResultVO<List<MerchantLigthResp>> listMerchantForOrder(MerchantLigthReq req) {
        return ResultVO.success(merchantManager.listMerchantForOrder(req));
    }

    @Override
    public ResultVO<MerchantLigthResp> getMerchantForOrder(MerchantGetReq req) {
        return ResultVO.success(merchantManager.getMerchantForOrder(req));
    }

    @Override
    public List<String> getMerchantIdList(String merchantName) {
        return merchantManager.getMerchantIdList(merchantName);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> saveFactoryMerchant(FactoryMerchantSaveReq req) {
        //新增厂商表记录
        Merchant merchant = new Merchant();
        BeanUtils.copyProperties(req, merchant);
        merchant = this.addMerchant(merchant);
        String merchantId = merchant.getMerchantId();
        req.setMerchantId(merchantId);
        //注册厂商信息并生成审核流程
        UserFactoryMerchantReq userFactoryMerchantReq = new UserFactoryMerchantReq();
        BeanUtils.copyProperties(req, userFactoryMerchantReq);
        //生成管理平台注册的工作流请求参数
        ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_3040701.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_3040701.getProcessTitle(),
                req.getMerchantId(),req.getCreateStaff(),req.getCreateStaffName(),WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3038.getTaskSubType(),null);
        MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
        BeanUtils.copyProperties(userFactoryMerchantReq, fileReq);
        ResultVO vo = initFactoryMerchant(fileReq, processStartReq);
        if (!vo.isSuccess()) {
            //注册厂商信息失败，回滚生成的账户信息
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error(vo.getResultMsg());
        }
        //关联账户信息,生成user信息
        ResultVO<UserDTO> userResult = this.addUser(req);
        if (!userResult.isSuccess() || null == userResult.getResultData()) {
            //注册失败，回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error(userResult.getResultMsg());
        }
        return success("新建厂商成功");
    }

    /**
     *厂商自注册
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO registerFactoryMerchant(FactoryMerchantSaveReq req) {
        try {
            Merchant merchant = new Merchant();
            BeanUtils.copyProperties(req, merchant);
            merchant = this.addMerchant(merchant);
            req.setMerchantId(merchant.getMerchantId());
            ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_3040501.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_3040501.getProcessTitle(),
                    req.getMerchantId(),req.getCreateStaff(),req.getCreateStaffName(),WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3034.getTaskSubType(),null );
            //上传的附件
            MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
            BeanUtils.copyProperties(req, fileReq);
            initFactoryMerchant(fileReq, processStartReq);
            return ResultVO.success(merchant.getMerchantId());
        }
        catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 上传厂商附件，生成审核流程
     * @param
     * @param
     * @return
     */
    public ResultVO<String> initFactoryMerchant(MerchantCommonFileReq fileReq, ProcessStartReq processStartReq) {
        //发起审核流程
        ResultVO processResult = taskService.startProcess(processStartReq);
        if (!processResult.isSuccess()) {
            return processResult;
        }
        //新增厂商附件记录
        ResultVO fileResult = this.addCommonFile(fileReq);
        return fileResult;
    }

    /**
     * 添加厂商初始记录
     * @param
     * @return
     */
    private Merchant addMerchant(Merchant merchant) {
        //默认未生效
        merchant.setStatus(PartnerConst.MerchantStatusEnum.NOT_EFFECT.getType());
        //类别为厂商
        merchant.setMerchantType(PartnerConst.MerchantTypeEnum.MANUFACTURER.getType());
        merchant.setMerchantCode(merchantManager.getMerchantCode(PartnerConst.MerchantTypeEnum.MANUFACTURER.getType()));
        merchant.setParCrmOrgId("843073100000000");
        merchant.setParCrmOrgCode("843073100000000");
        merchantManager.insertMerchant(merchant);
        return merchant;
    }

    /**
     * 厂商添加关联的用户信息
     * @param req
     * @return
     * @throws Exception
     */
    private ResultVO<UserDTO> addUser(FactoryMerchantSaveReq req) {
        UserAddReq userAddReq = new UserAddReq();
        BeanUtils.copyProperties(req, userAddReq);
        //默认禁用,流程审批通过后修改状态
        userAddReq.setStatusCd(SystemConst.USER_STATUS_INVALID);
        //用户类型为厂商
        userAddReq.setUserFounder(SystemConst.USER_FOUNDER_8);
        //关联id为厂商id
        userAddReq.setRelCode(req.getMerchantId());
        userAddReq.setCreateStaff(req.getCreateStaff());
        userAddReq.setUserSource(SystemConst.USER_RESOURCE_YHJ);
        //生成默认密码
        String loginPwd = "Ab_123456";
        userAddReq.setLoginPwd(new MD5(loginPwd).asHex());
        return userService.addUser(userAddReq);
    }

    /**
     * 上传厂商附件信息
     * @param req
     * @return
     */
    private  ResultVO addCommonFile(MerchantCommonFileReq req) {
        String merchantId = req.getMerchantId();
        //上传营业执照正本
        if (StringUtils.isNotEmpty(req.getBusinessLicense())) {
            ResultVO result = this.saveCommonFile(req.getBusinessLicense(), merchantId, SystemConst.FileClass.BUSINESS_LICENSE.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传营业执照正本失败");
            }
        }
        //上传营业执照副本
        if (StringUtils.isNotEmpty(req.getBusinessLicenseCopy())) {
            ResultVO result = this.saveCommonFile(req.getBusinessLicenseCopy(), merchantId, SystemConst.FileClass.BUSINESS_LICENSE.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传营业执照副本失败");
            }
        }
        //上传身份证正面
        if (StringUtils.isNotEmpty(req.getLegalPersonIdCardFont())) {
            ResultVO result = this.saveCommonFile(req.getLegalPersonIdCardFont(), merchantId, SystemConst.FileClass.IDENTITY_CARD_PHOTOS.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传身份证正面失败");
            }
        }
        //上传身份证背面
        if (StringUtils.isNotEmpty(req.getLegalPersonIdCardBack())) {
            ResultVO result = this.saveCommonFile(req.getLegalPersonIdCardFont(), merchantId, SystemConst.FileClass.IDENTITY_CARD_PHOTOS.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传身份证背面失败");
            }
        }
        //上传授权证明
        if (StringUtils.isNotEmpty(req.getAuthorizationCertificate())) {
            ResultVO result = this.saveCommonFile(req.getAuthorizationCertificate(), merchantId, SystemConst.FileClass.AUTHORIZATION_CERTIFICATE.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传授权证明失败");
            }
        }
        //上传合同
        if (StringUtils.isNotEmpty(req.getContract())) {
            ResultVO result = this.saveCommonFile(req.getContract(), merchantId, SystemConst.FileClass.CONTRACT_TEXT.getCode());
            if (!result.isSuccess()) {
                return ResultVO.error("上传合同失败");
            }
        }
        return ResultVO.success();
    }

    /**
     * 上传附件
     * @param fileUrl
     * @param merchantId
     * @param fileClass
     * @return
     */
    private ResultVO saveCommonFile(String fileUrl, String merchantId, String fileClass) {
        CommonFileDTO dto = new CommonFileDTO(SystemConst.FileType.IMG_FILE.getCode(), fileClass, merchantId, fileUrl);
        return commonFileService.saveCommonFile(dto);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO registLandSupplier(SupplierResistReq req) {
        try {
            //地包商
            req.setMerchantType(PartnerConst.MerchantTypeEnum.SUPPLIER_GROUND.getType());
            //插入商家信息
            ResultVO<Merchant> merchantRt = addMerchantInfo(req);
            String merchantId = merchantRt.getResultData().getMerchantId();
            //插入附件表  --未做补偿
            MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
            BeanUtils.copyProperties(req, fileReq);
            fileReq.setMerchantId(merchantId);
            this.addCommonFile(fileReq);
            //发起审核流程
            ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_DBGL.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_DBGL.getProcessTitle(),
                    merchantId,req.getUserId(),req.getMerchantName(),WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3033.getTaskSubType(),null);
            taskService.startProcess(processStartReq);
        }catch (Exception e){
            log.info(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error("注册失败");
        }
        return ResultVO.success("注册成功");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO registProvinceSupplier(SupplierResistReq req) {
        try {
            req.setMerchantType(PartnerConst.MerchantTypeEnum.SUPPLIER_PROVINCE.getType());
            //插入商家信息
            ResultVO<Merchant> merchantRt = addMerchantInfo(req);
            String merchantId = merchantRt.getResultData().getMerchantId();
            //插入附件表  --未做补偿
            MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
            BeanUtils.copyProperties(req, fileReq);
            fileReq.setMerchantId(merchantId);
            this.addCommonFile(fileReq);
            //发起审核流程
            ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_SBGL.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_SBGL.getProcessTitle(),
                    merchantId,req.getUserId(),req.getMerchantName(),WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3033.getTaskSubType(),null);
            taskService.startProcess(processStartReq);
        }catch (Exception e){
            log.error("注册失败" + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error("注册失败");
        }
        return ResultVO.success("注册成功");
    }

    /**
     * 管理员注册地包商
     * @param req
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO registLandSupplierForAdmin(SupplierResistReq req) {
        try {
            req.setMerchantType(PartnerConst.MerchantTypeEnum.SUPPLIER_GROUND.getType());
            req.setIfByAdmin(true);
            //插入商家信息
            ResultVO<Merchant> merchantRt = addMerchantInfo(req);
            String merchantId = merchantRt.getResultData().getMerchantId();
            //插入附件表  --未做补偿
            MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
            BeanUtils.copyProperties(req, fileReq);
            fileReq.setMerchantId(merchantId);
            this.addCommonFile(fileReq);
            //调用user服务插入注册user信息
            ResultVO<UserDTO> addUserRt = addUser(req, SystemConst.USER_FOUNDER_4, merchantId);
            if (!addUserRt.isSuccess()) return addUserRt;
            UserDTO user = addUserRt.getResultData();
            log.info("注册用户id" + user.getUserId());
            String statrProUserId = req.getUserId();
            log.info("发起者id" + statrProUserId);
            UserGetReq userGetReq = new UserGetReq();
            userGetReq.setUserId(statrProUserId);
            UserDTO startProcessUser = userService.getUser(userGetReq);
            //发起审核流程
            ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_ADMIN_DBGL.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_ADMIN_DBGL.getProcessTitle(),
                    merchantId,startProcessUser.getUserId(),startProcessUser.getUserName(), WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3036.getTaskSubType(),null );
            taskService.startProcess(processStartReq);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error("注册失败");
        }
        return ResultVO.success("注册成功");
    }


    /**
     * 上传合同
     * @param contract 图片地址
     * @param merchantId 关联主键
     * @param rollBack 上传失败是否回滚 true 抛出异常 false处理异常
     */
    private void addContract(String contract, String merchantId,Boolean rollBack) {
        //合同不为空，上传合同
        if(StringUtils.isNotBlank(contract)){
            CommonFileDTO dto=new CommonFileDTO(SystemConst.FileType.IMG_FILE.getCode(),SystemConst.FileClass.CONTRACT_TEXT.getCode(),merchantId,contract);
            if( !commonFileService.saveCommonFile(dto).isSuccess() && rollBack)
            {
                throw new RuntimeException("上传合同失败");
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ResultVO registProvinceSupplierForAdmin(SupplierResistReq req) {
        try {
            req.setMerchantType(PartnerConst.MerchantTypeEnum.SUPPLIER_PROVINCE.getType());
            req.setIfByAdmin(true);
            //插入商家信息
            ResultVO<Merchant> merchantRt = addMerchantInfo(req);
            String merchantId = merchantRt.getResultData().getMerchantId();
            //插入附件表  --未做补偿
            MerchantCommonFileReq fileReq = new MerchantCommonFileReq();
            BeanUtils.copyProperties(req, fileReq);
            fileReq.setMerchantId(merchantId);
            this.addCommonFile(fileReq);
            //USER_FOUNDER_4 省包
            ResultVO<UserDTO> addUserRt = addUser(req,SystemConst.USER_FOUNDER_4,merchantId);
            if(!addUserRt.isSuccess())return addUserRt;
            UserDTO user = addUserRt.getResultData();
            String statrProUserId = req.getUserId();
            UserGetReq userGetReq = new UserGetReq();
            userGetReq.setUserId(statrProUserId);
            UserDTO startProcessUser = userService.getUser(userGetReq);
            //发起审核流程   如果是电信人员则显示“岗位+部门”信息
            ProcessStartReq processStartReq = new ProcessStartReq(PartnerConst.MerchantProcessEnum.PROCESS_ADMIN_SBGL.getProcessId(),PartnerConst.MerchantProcessEnum.PROCESS_ADMIN_SBGL.getProcessTitle(),
                    merchantId,startProcessUser.getUserId(),startProcessUser.getUserName(), WorkFlowConst.TASK_SUB_TYPE.TASK_SUB_TYPE_3036.getTaskSubType(),null );
            taskService.startProcess(processStartReq);
        }catch (Exception e){
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultVO.error("注册失败");
        }
        return ResultVO.success("注册成功");
    }

    @Override
    public ResultVO<SupplierResistResp> getSupplierInfo(String merchantId) {
        SupplierResistResp resistResp;
        try {
            MerchantGetReq req = new MerchantGetReq();
            req.setMerchantId(merchantId);
            UserGetReq userGetReq = new UserGetReq();
            MerchantDetailDTO merchant = this.getMerchantDetail(req).getResultData();
            MerchantAccountListReq merchantAccountListReq = new MerchantAccountListReq();
            merchantAccountListReq.setMerchantId(merchantId);
            List<MerchantAccount> merchantAccountList = merchantAccountManager.listMerchantAccount(merchantAccountListReq);
            userGetReq.setUserId(merchant.getUserId());
            UserDTO user = userService.getUser(userGetReq);
            CommonFileDTO commonFileDTO = new CommonFileDTO();
            commonFileDTO.setObjId(merchantId);
            List<CommonFileDTO>commonFileList= commonFileService.listCommonFile(commonFileDTO).getResultData();
            resistResp = fillSupplierRegistResp(merchant,user,merchantAccountList,commonFileList);
        }catch (Exception e){
            log.error(e.getMessage());
            return ResultVO.error("获取详情失败");
        }
        return ResultVO.success(resistResp);
    }

    private SupplierResistResp fillSupplierRegistResp(MerchantDetailDTO merchant, UserDTO user, List<MerchantAccount> merchantAccountList, List<CommonFileDTO> commonFileList) {
        SupplierResistResp resistResp = new SupplierResistResp();
        BeanUtils.copyProperties(merchant,resistResp);
        BeanUtils.copyProperties(user,resistResp);
        for(CommonFileDTO commonFileDTO : commonFileList) {
            //身份证
            if (commonFileDTO.getFileClass().equals(SystemConst.FileClass.IDENTITY_CARD_PHOTOS.getCode())) {
                if (resistResp.getLegalPersonIdCardFont() == null) {
                    resistResp.setLegalPersonIdCardFont(commonFileDTO.getFileUrl());
                } else resistResp.setLegalPersonIdCardBack(commonFileDTO.getFileUrl());
            }
            //营业执照
            if (commonFileDTO.getFileClass().equals(SystemConst.FileClass.BUSINESS_LICENSE.getCode())) {
                resistResp.setBusinessLicense(commonFileDTO.getFileUrl());
                if (resistResp.getBusinessLicense() == null) {
                    resistResp.setBusinessLicense(commonFileDTO.getFileUrl());
                } else {
                    resistResp.setBusinessLicenseCopy(commonFileDTO.getFileUrl());
                }
            }
            if (commonFileDTO.getFileClass().equals(SystemConst.FileClass.CONTRACT_TEXT.getCode())) {
                resistResp.setContract(commonFileDTO.getFileUrl());
            }
            if (commonFileDTO.getFileClass().equals(SystemConst.FileClass.AUTHORIZATION_CERTIFICATE.getCode())) {
                resistResp.setAuthorizationCertificate(commonFileDTO.getFileUrl());
            }
        }
        //accout
        for(MerchantAccount account : merchantAccountList){
            resistResp.setAccount(account.getAccount());
            if(account.getAccountType().equals(PartnerConst.MerchantAccountTypeEnum.BEST_PAY.getType())){
                resistResp.setWindPayCount(account.getAccountName());
            }
            if(account.getAccountType().equals(PartnerConst.MerchantAccountTypeEnum.BANK_ACCOUNT.getType())){
                resistResp.setBank(account.getBank());
                resistResp.setBankAccount(account.getBankAccount());
            }
        }
        return resistResp;
    }


    /**
     * remote add sys_user
     * @param req
     * @param userFounder
     * @param merchantId
     * @return
     */
    public ResultVO<UserDTO> addUser(SupplierResistReq req,int userFounder,String merchantId){
        UserAddReq userAddReq = new UserAddReq();
        BeanUtils.copyProperties(req,userAddReq);
        userAddReq.setLoginPwd(new MD5(SystemConst.DFPASSWD).asHex());
        userAddReq.setUserFounder(userFounder);
        //关联商户信息和系统用户
        userAddReq.setRelCode(merchantId); //关联商户信息和系统用户
        //刚注册用户处于禁用状态
        userAddReq.setStatusCd(SystemConst.USER_STATUS_INVALID);
        return userService.addUser(userAddReq);
    }

    public ResultVO<Merchant> addMerchantInfo (SupplierResistReq req) {
        //管理员注册
        if(req.isIfByAdmin()) req.setUserId(null);
        MerchantAccount account = new MerchantAccount();
        BeanUtils.copyProperties(req,account);
        Merchant merchant = fillMerchant(req);
        String merchantId = merchantManager.addMerchan(merchant);
        account.setMerchantId(merchantId);
        //插入银行收款信息
        account = fillBankAccount(req,account);
        merchantAccountManager.insert(account);
        //插入翼支付收款信息
        account = fillWindPayAccount(req,account);
        merchantAccountManager.insert(account);
        return ResultVO.success(merchant);
    }


    public ResultVO checkBank(SupplierResistReq req) {
        //银行卡号是否正确
        JSONObject obj = JSONObject.parseObject(getCardDetail(req.getBankAccount()));
        String stat = (String) obj.get("stat");
        Boolean flag = (Boolean) obj.get("validated");
        if(!stat.equals("ok") || !flag)
            return ResultVO.error("银行卡无效");
        return ResultVO.success();
    }

    /**
     * 填充merchant
     * @param req
     * @return
     */
    private Merchant fillMerchant(SupplierResistReq req) {
        //商户编码，id，名称，地市lanid，联系电话,状态,商户类型
        //code要自动生成兼容手工维护的code，city可以地市+01,状态为待审核
        Merchant merchant = new Merchant();
        BeanUtils.copyProperties(req,merchant);
        merchant.setStatus(PartnerConst.MerchantStatusEnum.NOT_EFFECT.getType());
        if(merchant.getLanId()!=null)
            merchant.setCity(merchant.getLanId() + "01");
        return merchant;
    }



    private MerchantAccount fillWindPayAccount(SupplierResistReq resistReq, MerchantAccount account) {
        account.setAccount(resistReq.getAccount());
        account.setAccountName(resistReq.getWindPayCount());
        //支付类型为---翼支付
        account.setAccountType(PartnerConst.MerchantAccountTypeEnum.BEST_PAY.getType());
        return account;
    }

    private MerchantAccount fillBankAccount(SupplierResistReq resistReq,MerchantAccount account) {
        account.setAccount(resistReq.getAccount());
        account.setBank(resistReq.getBank());
        account.setBankAccount(resistReq.getBankAccount());
        //支付类型为---银行
        account.setAccountType(PartnerConst.MerchantAccountTypeEnum.BANK_ACCOUNT.getType());
        return account;
    }

    private String getCardDetail(String cardNo) {
        String url = "https://ccdcapi.alipay.com/validateAndCacheCardInfo.json?_input_charset=utf-8&cardNo=";
        url+=cardNo;
        url+="&cardBinCheck=true";
        StringBuilder sb = new StringBuilder();
        try {
            URL urlObject = new URL(url);
            URLConnection uc = urlObject.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String inputLine = null;
            while ( (inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 修改商户信息
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResultVO editMerchant(MerchantEditReq req) {
        Merchant merchant = new Merchant();
        BeanUtils.copyProperties(req, merchant);
        this.merchantManager.updateMerchant(merchant);
        return ResultVO.success();
    }

    @Override
    public ResultVO<FactoryMerchantResp> getFactoryMerchant(String merchantId) {
        FactoryMerchantResp factoryMerchantResp = new FactoryMerchantResp();
        //获取厂家基本信息
        ResultVO<MerchantDTO> merchantResult = this.getMerchantById(merchantId);
        if (merchantResult.getResultData() == null) {
            return ResultVO.error("未找到对应的厂商信息");
        }
        MerchantDTO merchantDTO = merchantResult.getResultData();
        BeanUtils.copyProperties(merchantDTO, factoryMerchantResp);
        //获取厂家账号信息
        UserGetReq userGetReq = new UserGetReq();
        userGetReq.setRelCode(merchantId);
        UserDTO userDTO = userService.getUser(userGetReq);
        if (userDTO != null) {
            factoryMerchantResp.setLoginName(userDTO.getLoginName());
        }
        //获取厂家附件信息
        CommonFileDTO commonFileDTO = new CommonFileDTO();
        commonFileDTO.setObjId(merchantId);
        ResultVO<List<CommonFileDTO>> commonFileResult = commonFileService.listCommonFile(commonFileDTO);
        if (commonFileResult.isSuccess() && commonFileResult.getResultData() != null) {
            makeUpMerchantFile(commonFileResult.getResultData(), factoryMerchantResp);
        }
        return ResultVO.success(factoryMerchantResp);
    }

    /**
     * 获取厂商附件
     * @param fileList
     * @param factoryMerchantResp
     */
    private void makeUpMerchantFile(List<CommonFileDTO> fileList, FactoryMerchantResp factoryMerchantResp) {
        for(CommonFileDTO file : fileList ){
            String fileUrl=file.getFileUrl();
            if(file.getFileClass().equals(SystemConst.FileClass.BUSINESS_LICENSE.getCode())){
                //营业执照
                if (factoryMerchantResp.getBusinessLicense() == null) {
                    factoryMerchantResp.setBusinessLicense(fileUrl);
                } else {
                    factoryMerchantResp.setBusinessLicenseCopy(fileUrl);
                }
            }else if(file.getFileClass().equals(SystemConst.FileClass.IDENTITY_CARD_PHOTOS.getCode())){
                //身份证照片
                if(factoryMerchantResp.getLegalPersonIdCardFont()==null){
                    factoryMerchantResp.setLegalPersonIdCardFont(fileUrl);
                }else{
                    factoryMerchantResp.setLegalPersonIdCardBack(fileUrl);
                }
            }else if(file.getFileClass().equals(SystemConst.FileClass.AUTHORIZATION_CERTIFICATE.getCode())){
                //授权证书
                factoryMerchantResp.setAuthorizationCertificate(fileUrl);
            }else if(file.getFileClass().equals(SystemConst.FileClass.CONTRACT_TEXT.getCode())){
                //合同文本
                factoryMerchantResp.setContract(fileUrl);
            }
        }
    }
}