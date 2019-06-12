package com.iwhalecloud.retail.system.service;


import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.system.dto.CommonOrgDTO;
import com.iwhalecloud.retail.system.dto.request.CommonOrgListReq;

import java.util.List;

/**
 * 通用组织信息服务接口实现类
 * @author lipeng
 */
public interface CommonOrgService{

    /**
     * 获取通用组织信息列表
     * @param req
     * @return
     */
    ResultVO<List<CommonOrgDTO>> listCommonOrg(CommonOrgListReq req);

    /**
     * orgId
     * @param orgId
     * @return
     */
    ResultVO<CommonOrgDTO> getCommonOrgById(String orgId);
}