package com.controller;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.config.Constant;
import com.config.URLConstant;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.DingTalkSignatureUtil;
import com.dingtalk.api.request.OapiAuthScopesRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiMessageCorpconversationGetsendresultRequest;
import com.dingtalk.api.request.OapiServiceGetAuthInfoRequest;
import com.dingtalk.api.request.OapiServiceGetCorpTokenRequest;
import com.dingtalk.api.request.OapiUserListRequest;
import com.dingtalk.api.response.OapiAuthScopesResponse;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.dingtalk.api.response.OapiMessageCorpconversationGetsendresultResponse;
import com.dingtalk.api.response.OapiServiceGetAuthInfoResponse;
import com.dingtalk.api.response.OapiServiceGetCorpTokenResponse;
import com.dingtalk.api.response.OapiUserListResponse;
import com.dingtalk.api.response.OapiUserListResponse.Userlist;
import com.dysoft.model.InvoiceInfo;
import com.taobao.api.ApiException;
import com.util.DyData;


@RestController
public class InvoiceController {
	private static int times=1;
	private static final Logger bizLogger = LoggerFactory.getLogger(IndexController.class);
	static {
		InvoiceInfo invoice = new InvoiceInfo();
		invoice.setChehao("83D17");
		invoice.setMenwei("Yangman");
		DyData.InvoiceList.put("83D17", invoice);
	}
	private String corpId = "ding7267631d767e30fb35c2f4657eb6378f";
	
	@CrossOrigin
	@RequestMapping(value= "/addInvoice", method=RequestMethod.POST)
	public void addInvoice(@RequestBody InvoiceInfo invoiceInfo) throws ApiException {
		this.bizLogger.info("The Invoice's chehao is:{}",invoiceInfo.getChehao());
		DyData.InvoiceList.put(invoiceInfo.getChehao(), invoiceInfo);
		this.notification("采购员发送了一条到货通知!");
	}
	
	@CrossOrigin
	@RequestMapping(value="/getInvoice/{chehao}",method=RequestMethod.GET)
	public InvoiceInfo GetInvoice(@PathVariable("chehao") String chehao) {
		this.bizLogger.info("The chehao is:{}",chehao);	
		return (InvoiceInfo)DyData.InvoiceList.get(chehao);
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getInvoiceList",method=RequestMethod.GET)
	public List<InvoiceInfo> GetInvoiceList() throws ApiException{
		this.bizLogger.info("The agent id is :{}",this.getAgentId());
		return new ArrayList<InvoiceInfo>(DyData.InvoiceList.values());
	}
	
	@CrossOrigin
	@RequestMapping(value="/menweiSign",method=RequestMethod.POST)	
	public InvoiceInfo menweiSign(@RequestBody InvoiceInfo invoice) throws ApiException {
		InvoiceInfo result=DyData.InvoiceList.get(invoice.getChehao());
		result.setMenwei(invoice.getMenwei());
		this.notification("门卫确认物资已进场!");
		return result;
		
	}

	@CrossOrigin
	@RequestMapping(value="/fzrSign",method=RequestMethod.POST)	
	public InvoiceInfo fzrSign(@RequestBody InvoiceInfo invoice) throws ApiException {
		InvoiceInfo result=DyData.InvoiceList.get(invoice.getChehao());
		result.setXiangmufuzeren(invoice.getXiangmufuzeren());
		this.notification("项目负责人确认物资已到场!");
		return result;
	}
	
	public void notification(String message) throws ApiException {
		
		DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");

		OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
		List<Userlist> list=this.getUserList();
		StringBuffer sb=new StringBuffer();
		for(int i=0,len = list.size();i<len;i++) {
			if(i==(len-1)) {				
				sb.append(list.get(i).getUserid());
			}else {
				sb.append(list.get(i).getUserid()).append(",");
			}
		}
		this.bizLogger.info("The UserList is :{}",sb.toString());
		request.setUseridList(sb.toString());
		request.setAgentId(this.getAgentId());
		request.setToAllUser(false);

		OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
		msg.setActionCard(new OapiMessageCorpconversationAsyncsendV2Request.ActionCard());
		msg.getActionCard().setTitle("系统通知");
		msg.getActionCard().setSingleTitle("物资进场验收");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		msg.getActionCard().setMarkdown(String.format("%s   [%s]",message,df.format(new Date())));
		msg.getActionCard().setSingleUrl(String.format(DyData.Referer,this.corpId));
		msg.setMsgtype("action_card");
		//msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
		//msg.getText().setContent(message);
		request.setMsg(msg);
		
		OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request,this.getAccessToken());
		this.bizLogger.info("The result is :{}",response.getBody());
		//this.bizLogger.info("The Result is :{}",this.notificationResult(response.getTaskId(),this.getAccessToken()));
	}
	
	private String notificationResult(long taskid,String accessToken) throws ApiException {
		DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/getsendresult");
		OapiMessageCorpconversationGetsendresultRequest request  = new OapiMessageCorpconversationGetsendresultRequest();
		request.setAgentId(this.getAgentId());
		request.setTaskId(taskid);
		OapiMessageCorpconversationGetsendresultResponse response = client.execute(request, accessToken);
		return response.getBody();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getUserList",method=RequestMethod.GET)
	public List<Userlist> getDingUserList() throws ApiException {
		return this.getUserList();
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
	private String getAccessToken() {
		OapiServiceGetCorpTokenResponse oapiServiceGetCorpTokenResponse = getOapiServiceGetCorpToken(corpId);
		String accessToken = oapiServiceGetCorpTokenResponse.getAccessToken();
		return accessToken;
	}
  public long getAgentId() throws ApiException {
//	  DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/service/get_auth_info");
//	  OapiServiceGetAuthInfoRequest req1 = new OapiServiceGetAuthInfoRequest();
//	  req1.setAuthCorpid(this.corpId);
//	  OapiServiceGetAuthInfoResponse response = client.execute(req1,"suite3ajxcniz7xitvjew","yCiWqJcaat9MPAWQ8cZTOQv9AyvfjfPDAtf808-HWqJ3skl1UVwmUK_yKYf5xWLa","testSuiteTicket");
//	  this.bizLogger.info(response.getBody());
//	  return response.getAuthInfo().getAgent().get(0).getAgentid();
	  return 183886908L;
  }
  
  private List<Userlist> getUserList() throws ApiException{
	  DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/list");
	  OapiUserListRequest request = new OapiUserListRequest();
	  request.setHttpMethod("GET");
	  request.setDepartmentId(1L);
	  OapiUserListResponse response = client.execute(request, this.getAccessToken());
	  this.bizLogger.info("The OapiUserListResponse is:{}",response.getBody());
	  return response.getUserlist();

  }
}
