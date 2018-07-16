package com.controller;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.dysoft.model.InvoiceInfo;
import com.util.DyData;


@RestController
public class InvoiceController {
	
	private static final Logger bizLogger = LoggerFactory.getLogger(IndexController.class);
	static {
		InvoiceInfo invoice = new InvoiceInfo();
		invoice.setChehao("83D17");
		invoice.setMenwei("Yangman");
		DyData.InvoiceList.put("83D17", invoice);
	}
	
	@CrossOrigin
	@RequestMapping(value= "/addInvoice", method=RequestMethod.POST)
	public void addInvoice(@RequestBody InvoiceInfo invoiceInfo) {
		this.bizLogger.info("The Invoice's chehao is:{}",invoiceInfo.getChehao());
		DyData.InvoiceList.put(invoiceInfo.getChehao(), invoiceInfo);
	}
	
	@CrossOrigin
	@RequestMapping(value="/getInvoice/{chehao}",method=RequestMethod.GET)
	public InvoiceInfo GetInvoice(@PathVariable("chehao") String chehao) {
		this.bizLogger.info("The chehao is:{}",chehao);	
		return (InvoiceInfo)DyData.InvoiceList.get(chehao);
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getInvoiceList",method=RequestMethod.GET)
	public List<InvoiceInfo> GetInvoiceList(){
		return new ArrayList<InvoiceInfo>(DyData.InvoiceList.values());
	}
	
	
	
}
