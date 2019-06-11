package com.iwhalecloud.retail.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.report.dto.SupplierOperatingDayDTO;
import com.iwhalecloud.retail.report.dto.request.SupplierOperatingDayPageReq;

/**
 * @author wenlong.zhong
 * @date 2019/6/10
 */
public interface SupplierOperatingDayService {

    /**
     * 地包进销存 数据 分页查询
     * @return
     */
    ResultVO<Page<SupplierOperatingDayDTO>> page(SupplierOperatingDayPageReq req);

}