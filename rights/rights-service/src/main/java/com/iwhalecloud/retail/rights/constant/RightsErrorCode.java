package com.iwhalecloud.retail.rights.constant;

import com.iwhalecloud.retail.annotation.ErrorCodeAnnotation;
import com.iwhalecloud.retail.dto.ResultCode;
import org.springframework.stereotype.Service;

/**
 * @Description: 权限模块异常编码，需要配置sys_error_code表
 * @author: Z
 * @date: 2019/7/2 11:43
 */
@ErrorCodeAnnotation
@Service
public class RightsErrorCode {

    /**
     * 第一位编码
     */
    public static ResultCode ERROR_500000;
}
