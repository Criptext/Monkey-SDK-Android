package com.criptext.comunication;

import java.io.File;
import com.google.gson.JsonObject;

public class MOKMessage {
	
	private int protocolCommand;
	private int protocolType;
	private String eph;
	private String cmpr;
	private String str;
	private String encr;

	private String datetime;
	private String msg;
	private String message_id;
	private String type;
	private String sid;
	private String rid;
	private JsonObject params;
	private File file;
	
	public MOKMessage(){
		
	}
	
	public MOKMessage(String message_id, String sid, String rid, String msg, String datatime, String type, JsonObject params){
		this.message_id = message_id;
		this.sid = sid;
		this.rid = rid;
		this.msg = msg;
		this.datetime = datatime;		
		this.type = type;
		this.params = params;
	}

	public int getProtocolCommand() {
		return protocolCommand;
	}

	public void setProtocolCommand(int protocolCommand) {
		this.protocolCommand = protocolCommand;
	}

	public int getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(int protocolType) {
		this.protocolType = protocolType;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getMessage_id() {
		return message_id;
	}

	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public JsonObject getParams() {
		return params;
	}

	public void setParams(JsonObject params) {
		this.params = params;
	}

	public String getEph() {
		return eph;
	}

	public void setEph(String eph) {
		this.eph = eph;
	}

	public String getCmpr() {
		return cmpr;
	}

	public void setCmpr(String cmpr) {
		this.cmpr = cmpr;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public String getEncr() {
		return encr;
	}

	public void setEncr(String encr) {
		this.encr = encr;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
}