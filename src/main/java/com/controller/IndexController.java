package com.controller;

import com.config.Constant;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.DingTalkSignatureUtil;
import com.dingtalk.api.request.OapiServiceGetCorpTokenRequest;
import com.dingtalk.api.request.OapiUserGetRequest;
import com.dingtalk.api.request.OapiUserGetuserinfoRequest;
import com.dingtalk.api.response.OapiServiceGetCorpTokenResponse;
import com.dingtalk.api.response.OapiUserGetuserinfoResponse;
import com.dingtalk.api.response.OapiUserGetResponse;
import com.dingtalk.api.response.OapiUserGetResponse.Roles;
import com.config.URLConstant;
import com.taobao.api.ApiException;
import com.util.DyData;
import com.util.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * ISV E应用Quick-Start示例代码
 * 实现了最简单的免密登录（免登）功能
 */
@RestController
public class IndexController {
	private static final Logger bizLogger = LoggerFactory.getLogger(IndexController.class);

	/**
	 * 欢迎页面
	 */
	@RequestMapping(value = "/welcome", method = RequestMethod.GET)
	public String welcome() {
		return "welcome";
	}


	/**
	 * 钉钉用户登录，显示当前登录的企业和用户
	 * @param corpId			授权企业的CorpId
	 * @param requestAuthCode	免登临时code
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@ResponseBody
	public ServiceResult login(@RequestParam(value = "corpId") String corpId,
									@RequestParam(value = "authCode") String requestAuthCode, HttpServletRequest request) {
		
		bizLogger.info("request from {}", request.getHeader("referer"));
		this.bizLogger.info("authCode: {}", requestAuthCode);
		//DyData.Referer=request.getHeader("referer");

		Long start = System.currentTimeMillis();
		//获取accessToken,注意正是代码要有异常流处理
		OapiServiceGetCorpTokenResponse oapiServiceGetCorpTokenResponse = getOapiServiceGetCorpToken(corpId);
		String accessToken = oapiServiceGetCorpTokenResponse.getAccessToken();
		this.bizLogger.info("The CorpId is :{}",corpId);

		//获取用户信息
		OapiUserGetuserinfoResponse oapiUserGetuserinfoResponse = getOapiUserGetuserinfo(accessToken,requestAuthCode);

		//3.查询得到当前用户的userId
		// 获得到userId之后应用应该处理应用自身的登录会话管理（session）,避免后续的业务交互（前端到应用服务端）每次都要重新获取用户身份，提升用户体验
		String userId = oapiUserGetuserinfoResponse.getUserid();
		OapiUserGetResponse oapiUserGetResponse=getOapiUser(accessToken,userId);
        List<Roles> roleList=oapiUserGetResponse.getRoles();
        this.bizLogger.info("The roleList is:{}",oapiUserGetResponse.getBody());
        StringBuffer sb=new StringBuffer();
        if(roleList!=null)
        for(int i=0,len = roleList.size();i<len;i++) {
        	if(i==len-1) {
        		sb.append(roleList.get(i).getName());
        	}else {
        		sb.append(roleList.get(i).getName());
        	}
        }
		//返回结果
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("userId",userId);
		resultMap.put("corpId",corpId);
		resultMap.put("name", oapiUserGetResponse.getName());
		resultMap.put("role",sb.toString());
		ServiceResult serviceResult = ServiceResult.success(resultMap);
		return serviceResult;
	}

	private OapiUserGetResponse getOapiUser(String accessToken, String userid) {
		DingTalkClient client = new DefaultDingTalkClient(URLConstant.URL_GET_USER);
		OapiUserGetRequest request = new OapiUserGetRequest();
		request.setUserid(userid);		
		request.setHttpMethod("GET");

		OapiUserGetResponse response;
		try {
			response = client.execute(request, accessToken);
		} catch (ApiException e) {
			e.printStackTrace();
			return null;
		}
		if (response == null || !response.isSuccess()) {
			return null;
		}
		return response;
	}


	/**
	 * ISV获取企业访问凭证
	 * @param corpId	授权企业的corpId
	 */
	private OapiServiceGetCorpTokenResponse getOapiServiceGetCorpToken(String corpId) {
		if (corpId == null || corpId.isEmpty()) {
			return null;
		}

		long timestamp = System.currentTimeMillis();
		//正式应用应该由钉钉通过开发者的回调地址动态获取到
		String suiteTicket = getSuiteTickt(Constant.SUITE_KEY);
		String signature = DingTalkSignatureUtil.computeSignature(Constant.SUITE_SECRET, DingTalkSignatureUtil.getCanonicalStringForIsv(timestamp, suiteTicket));
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("timestamp", String.valueOf(timestamp));
		params.put("suiteTicket", suiteTicket);
		params.put("accessKey", Constant.SUITE_KEY);
		params.put("signature", signature);
		String queryString = DingTalkSignatureUtil.paramToQueryString(params, "utf-8");
		DingTalkClient client = new DefaultDingTalkClient(URLConstant.URL_GET_CORP_TOKEN + "?" + queryString);
		OapiServiceGetCorpTokenRequest request = new OapiServiceGetCorpTokenRequest();
		request.setAuthCorpid(corpId);
		OapiServiceGetCorpTokenResponse response;
		try {
			response = client.execute(request);
		} catch (ApiException e) {
			bizLogger.info(e.toString(),e);
			return null;
		}
		if (response == null || !response.isSuccess()) {
			return null;
		}
		return response;
	}



	/**
	 * 通过钉钉服务端API获取用户在当前企业的userId
	 * @param accessToken	企业访问凭证Token
	 * @param code			免登code
	 * @
	 */
	private OapiUserGetuserinfoResponse getOapiUserGetuserinfo(String accessToken, String code) {
		DingTalkClient client = new DefaultDingTalkClient(URLConstant.URL_GET_USER_INFO);
		OapiUserGetuserinfoRequest request = new OapiUserGetuserinfoRequest();
		request.setCode(code);
		request.setHttpMethod("GET");

		OapiUserGetuserinfoResponse response;
		try {
			response = client.execute(request, accessToken);
		} catch (ApiException e) {
			e.printStackTrace();
			return null;
		}
		if (response == null || !response.isSuccess()) {
			return null;
		}
		return response;
	}




	/**
	 * suiteTicket是一个定时变化的票据，主要目的是为了开发者的应用与钉钉之间访问时的安全加固。
	 * 测试应用：可随意设置，钉钉只做签名不做安全加固限制。
	 * 正式应用：开发者应该从自己的db中读取suiteTicket,suiteTicket是由开发者在开发者平台设置的应用回调地址，由钉钉定时推送给应用，
	 * 由开发者在回调地址所在代码解密和验证签名完成后获取到的.正式应用钉钉会在开发者代码访问时做严格检查。
	 * @return suiteTicket
	 */
	private String getSuiteTickt(String suiteKey){
		//正式应用必须由应用回调地址从钉钉推送获取
		return "temp_suite_ticket_only4_test";

	}
}


