package com.iwhalecloud.retail.order2b.dto.response.purapply;

import com.iwhalecloud.retail.dto.PageVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class PurApplyFileResp extends PageVO implements Serializable  {/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String url;//文件的url
	private String uid;
	private String name;//文件的名字
	private String fileType;

}
