package com.test.jms;

import java.io.Serializable;

public class Message implements Serializable {
	
	private String REQUESTID;
	private String SFDCID;
	private String CUSTOMERID;
	public String getREQUESTID() {
		return REQUESTID;
	}
	public void setREQUESTID(String rEQUESTID) {
		REQUESTID = rEQUESTID;
	}
	public String getSFDCID() {
		return SFDCID;
	}
	public Message(String rEQUESTID, String sFDCID, String cUSTOMERID) {
		super();
		REQUESTID = rEQUESTID;
		SFDCID = sFDCID;
		CUSTOMERID = cUSTOMERID;
	}
	public void setSFDCID(String sFDCID) {
		SFDCID = sFDCID;
	}
	public String getCUSTOMERID() {
		return CUSTOMERID;
	}
	public void setCUSTOMERID(String cUSTOMERID) {
		CUSTOMERID = cUSTOMERID;
	}

}
