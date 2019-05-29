package org.hisp.dhis.sms.listener;

import org.hisp.dhis.sms.listener.NewSMSListener.SMSResponse;

public class SMSProcessingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 353425388316643481L;
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
