package org.hisp.dhis.sms.incoming;

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

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "inboundsms" )
public class IncomingSms
    implements Serializable
{
    private static final long serialVersionUID = 3954710607630454226L;

    private Integer id;

    private SmsMessageEncoding encoding;

    private Date sentDate;

    private Date receivedDate;

    /*
     * The originator of the received message.
     */
    private String originator;

    /*
     * The ID of the gateway from which the message was received.
     */
    private String gatewayId;

    private String text;

    private byte[] bytes;

    private SmsMessageStatus status = SmsMessageStatus.INCOMING;

    private String statusMessage;

    private boolean parsed = false;

    /**
     * Incoming smses are one of two types, text or binary.
     * 
     * @return is this message a text (not binary) message?
     */
    public boolean isTextSms()
    {
        return text != null;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    @JsonProperty( value = "smsencoding", defaultValue = "1" )
    @JacksonXmlProperty( localName = "smsencoding" )
    public SmsMessageEncoding getEncoding()
    {
        return encoding;
    }

    public void setEncoding( SmsMessageEncoding encoding )
    {
        this.encoding = encoding;
    }

    @JsonProperty( value = "sentdate" )
    @JacksonXmlProperty( localName = "sentdate" )
    public Date getSentDate()
    {
        return sentDate;
    }

    public void setSentDate( Date sentDate )
    {
        this.sentDate = sentDate;
    }

    @JsonProperty( value = "receiveddate" )
    @JacksonXmlProperty( localName = "receiveddate" )
    public Date getReceivedDate()
    {
        return receivedDate;
    }

    public void setReceivedDate( Date receivedDate )
    {
        this.receivedDate = receivedDate;
    }

    @JsonProperty( value = "originator" )
    @JacksonXmlProperty( localName = "originator" )
    public String getOriginator()
    {
        return originator;
    }

    public void setOriginator( String originator )
    {
        this.originator = originator;
    }

    @JsonProperty( value = "gatewayid", defaultValue = "unknown" )
    @JacksonXmlProperty( localName = "gatewayid" )
    public String getGatewayId()
    {
        return gatewayId;
    }

    public void setGatewayId( String gatewayId )
    {
        this.gatewayId = gatewayId;
    }

    @JsonProperty( value = "text" )
    @JacksonXmlProperty( localName = "text" )
    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        if ( bytes != null )
        {
            throw new IllegalArgumentException( "Text and bytes cannot both be set on incoming sms" );
        }
        this.text = text;
    }

    public byte[] getBytes()
    {
        return bytes;
    }

    public void setBytes( byte[] bytes )
    {
        if ( text != null )
        {
            throw new IllegalArgumentException( "Text and bytes cannot both be set on incoming sms" );
        }
        this.bytes = bytes;
    }

    @JsonProperty( value = "smsstatus", defaultValue = "1" )
    @JacksonXmlProperty( localName = "smsstatus" )
    public SmsMessageStatus getStatus()
    {
        return status;
    }

    public void setStatus( SmsMessageStatus status )
    {
        this.status = status;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public void setStatusMessage( String statusMessage )
    {
        this.statusMessage = statusMessage;
    }

    public boolean isParsed()
    {
        return parsed;
    }

    public void setParsed( boolean parsed )
    {
        this.parsed = parsed;
    }
}