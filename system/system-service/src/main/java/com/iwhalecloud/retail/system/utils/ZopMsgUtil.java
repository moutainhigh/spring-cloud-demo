package com.iwhalecloud.retail.system.utils;

import com.iwhalecloud.retail.system.common.ZopMessageConst;
import com.iwhalecloud.retail.system.model.ZopMsgModel;
import com.iwhalecloud.retail.system.model.ZopReqContent;
import com.ztesoft.codec.binary.Hex;
import com.ztesoft.fastjson.JSON;
import com.ztesoft.zop.ZopClient;
import com.ztesoft.zop.common.message.RequestParams;
import com.ztesoft.zop.common.message.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Component
public class ZopMsgUtil {
    @Autowired
    Environment env;

    /**】
     * 发送一条模板短信
     * @param zopMsgModel  短信报文模型
     * @param msgtTemplate 短信模板内容类,参照SmsVerificationtemplate
     * @return
     */
    public boolean SendMsg(ZopMsgModel zopMsgModel,Object msgtTemplate) {
        if(!ifOpen()){
            log.warn("zopMsg closed");
            return false;
        }
        RequestParams params = new RequestParams();
        params.setAccess_token(getToken());
        params.setTimeout(getTimeout());
        params.setUrl(getUrl());
        ZopReqContent content = new ZopReqContent();
        List list = new ArrayList();
        //模板所需要的参数转16进制
        String param = JSON.toJSONString(msgtTemplate);
        param = new String(Hex.encodeHex(param.getBytes()));
        zopMsgModel.setParams(param);
        //====
        list.add(zopMsgModel);
        content.setBillReqVo(list);
        ResponseResult responseResult = ZopClient.callRest(params,  ZopMessageConst.ZopServiceEnum.SEND_MESSAGE.getVersion()
                ,ZopMessageConst.ZopServiceEnum.SEND_MESSAGE.getMethod(),content,true);
        return responseResult.getRes_code().equals("00000");
    }

    /**
     * 发送多条模板短信
     * @param zopMsgModels 短信报文模型
     * @param msgtTemplate 短信模板内容类,参照SmsVerificationtemplate
     * @return
     */
    public boolean SendMsgs(List<ZopMsgModel> zopMsgModels,List<Object> msgtTemplate) {
        if(!ifOpen()){
            log.warn("zopMsg closed");
            return false;
        }
        if(zopMsgModels.size()!=msgtTemplate.size()){
            return false;
        }
        RequestParams params = new RequestParams();
        params.setAccess_token(getToken());
        params.setTimeout(getTimeout());
        params.setUrl(getUrl());
        ZopReqContent content = new ZopReqContent();

        for(int i=0;i<zopMsgModels.size();i++){
            String param = JSON.toJSONString(msgtTemplate.get(i));
            try {
                param = new String(Hex.encodeHex(param.getBytes(getCharset())));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            zopMsgModels.get(i).setParams(param);
        }
        content.setBillReqVo(zopMsgModels);
        ResponseResult responseResult = ZopClient.callRest(params,  ZopMessageConst.ZopServiceEnum.SEND_MESSAGE.getVersion()
                ,ZopMessageConst.ZopServiceEnum.SEND_MESSAGE.getMethod(),content,true);
        return responseResult.getRes_code().equals("00000");
    }





    public String getToken(){
        return env.getProperty("zop.secret");
    }

    public String getCharset(){
        return env.getProperty("zop.charset");
    }

    public Integer getTimeout() {
        return Integer.getInteger(env.getProperty("zop,timeout"));
    }

    public String getUrl() {
        return env.getProperty("zop.url");
    }

    public boolean ifOpen(){
        return Boolean.parseBoolean(env.getProperty("zop.ifopen"));
    }

}
