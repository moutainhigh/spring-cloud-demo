package com.iwhalecloud.retail.system.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.system.common.SystemConst;
import com.iwhalecloud.retail.system.dto.CommonRegionDTO;
import com.iwhalecloud.retail.system.dto.request.CommonRegionListReq;
import com.iwhalecloud.retail.system.dto.request.CommonRegionPageReq;
import com.iwhalecloud.retail.system.entity.CommonRegion;
import com.iwhalecloud.retail.system.manager.CommonRegionManager;
import com.iwhalecloud.retail.system.service.CommonRegionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class CommonRegionServiceImpl implements CommonRegionService {

    @Autowired
    private CommonRegionManager commonRegionManager;

    /**
     * 跟regionId获取本地区域
     * @param regionId
     * @return
     */
    @Override
    public ResultVO<CommonRegionDTO> getCommonRegionById(String regionId) {
        log.info("CommonRegionServiceImpl.getCommonRegionById(), input：regionId={} ", JSON.toJSONString(regionId));

        CommonRegion commonRegion = commonRegionManager.getCommonRegionById(regionId);
        CommonRegionDTO commonRegionDTO = new CommonRegionDTO();
        if (commonRegion != null) {
            BeanUtils.copyProperties(commonRegion, commonRegionDTO);
        } else {
            commonRegionDTO = null;
        }
        log.info("CommonRegionServiceImpl.getCommonRegionById(), output：commonRegionDTO={} ", JSON.toJSONString(commonRegionDTO));
        return ResultVO.success(commonRegionDTO);
    }


    /**
     * 获取本地区域 列表
     * @param req
     * @return
     */
    @Override
    public ResultVO<List<CommonRegionDTO>> listCommonRegion(CommonRegionListReq req) {
        log.info("CommonRegionServiceImpl.listCommonRegion(), input：req={} ", JSON.toJSONString(req));
        if (CollectionUtils.isEmpty(req.getRegionIdList()) && CollectionUtils.isEmpty(req.getParRegionIdList())
                && StringUtils.isEmpty(req.getParRegionId()) && StringUtils.isEmpty(req.getRegionName())) {
            // 三个条件都为空 默认查湖南的 本地网
            req.setParRegionId(SystemConst.HN_DEFAULT_PAR_REGION_ID);
        }

        List<CommonRegion> commonRegionList = commonRegionManager.listCommonRegion(req);
        List<CommonRegionDTO> commonRegionDTOList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(commonRegionList)) {
            commonRegionList.forEach(commonRegion -> {
                CommonRegionDTO commonRegionDTO = new CommonRegionDTO();
                BeanUtils.copyProperties(commonRegion, commonRegionDTO);
                commonRegionDTOList.add(commonRegionDTO);
            });
        }
        log.info("CommonRegionServiceImpl.listCommonRegion(), output：commonRegionDTOList={} ", JSON.toJSONString(commonRegionDTOList));
        return ResultVO.success(commonRegionDTOList);
    }

    /**
     * 获取 湖南本地网 列表
     * @return
     */
    @Override
    public ResultVO<List<CommonRegionDTO>> listLan() {
        log.info("CommonRegionServiceImpl.listLan() ");
        CommonRegionListReq req = new CommonRegionListReq();
        req.setParRegionId(SystemConst.HN_DEFAULT_PAR_REGION_ID);
        List<CommonRegion> commonRegionList = commonRegionManager.listCommonRegion(req);
        List<CommonRegionDTO> commonRegionDTOList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(commonRegionList)) {
            commonRegionList.forEach(commonRegion -> {
                CommonRegionDTO commonRegionDTO = new CommonRegionDTO();
                BeanUtils.copyProperties(commonRegion, commonRegionDTO);
                commonRegionDTOList.add(commonRegionDTO);
            });
        }
        log.info("CommonRegionServiceImpl.listLan(), output：commonRegionDTOList={} ", JSON.toJSONString(commonRegionDTOList));
        return ResultVO.success(commonRegionDTOList);
    }

    /**
     * 分页 获取本地区域 列表
     * @param req
     * @return
     */
    @Override
    public ResultVO<Page<CommonRegionDTO>> pageCommonRegion(CommonRegionPageReq req) {
        log.info("CommonRegionServiceImpl.pageCommonRegion(), input：req={} ", JSON.toJSONString(req));
        Page<CommonRegionDTO> respPage = commonRegionManager.pageCommonRegion(req);
        log.info("CommonRegionServiceImpl.pageCommonRegion(), output：list={} ", JSON.toJSONString(respPage.getRecords()));
        return ResultVO.success(respPage);
    }

}