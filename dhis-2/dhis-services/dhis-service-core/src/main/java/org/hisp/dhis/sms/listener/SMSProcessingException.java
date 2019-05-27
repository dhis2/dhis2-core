package org.hisp.dhis.sms.listener;

import org.hisp.dhis.sms.listener.NewSMSListener.SMSResponse;

public class SMSProcessingException extends RuntimeException {

	private SMSResponse resp;
	
	public SMSProcessingException(SMSResponse resp)
	{
		this.resp = resp;
	}
	
	@Override
	public String getMessage()
	{
		return resp.getDescription();
	}
	
	public SMSResponse getResp()
	{
		return resp;
	}
}
