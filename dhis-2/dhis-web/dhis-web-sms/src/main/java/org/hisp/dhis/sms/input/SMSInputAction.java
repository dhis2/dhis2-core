package org.hisp.dhis.sms.input;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageEncoding;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;

import com.opensymphony.xwork2.Action;

/**
 * @author Christian and Magnus
 */
public class SMSInputAction
    implements Action
{
    private String sender, phone, number, msisdn;

    private String message, text, content;

    private IncomingSmsService incomingSmsService;

    @Override
    public String execute()
        throws Exception
    {
        IncomingSms sms = new IncomingSms();

        // setter for sms originator
        if ( sender != null )
        {
            sms.setOriginator( sender );
        }
        else if ( phone != null )
        {
            sms.setOriginator( phone );
        }
        else if ( number != null )
        {
            sms.setOriginator( number );
        }
        else if ( msisdn != null )
        {
            sms.setOriginator( msisdn );
        }

        // setter for sms text
        if ( message != null )
        {
            sms.setText( message );
        }
        else if ( text != null )
        {
            sms.setText( text );
        }
        else if ( content != null )
        {
            sms.setText( content );
        }

        // check whether two necessary attributes are null 
        if ( sms.getOriginator() == null || sms.getText() == null )
        {
            setNullToAll();
            return ERROR;
        }

        java.util.Date rec = new java.util.Date();
        sms.setReceivedDate( rec );
        sms.setSentDate( rec );

        sms.setEncoding( SmsMessageEncoding.ENC7BIT );
        sms.setStatus( SmsMessageStatus.INCOMING );
        sms.setGatewayId( "HARDCODEDTESTGATEWAY" );

        incomingSmsService.save( sms );

        setNullToAll();

        return SUCCESS;
    }

    public void setNullToAll()
    {
        sender = null;
        phone = null;
        number = null;
        message = null;
        text = null;
        content = null;
    }

    public void setSender( String sender )
    {
        this.sender = sender;
    }

    public void setPhone( String phone )
    {
        this.phone = phone;
    }

    public void setNumber( String number )
    {
        this.number = number;
    }

    public void setMsisdn( String msisdn )
    {
        this.msisdn = msisdn;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    public void setIncomingSmsService( IncomingSmsService incomingSmsService )
    {
        this.incomingSmsService = incomingSmsService;
    }
}
