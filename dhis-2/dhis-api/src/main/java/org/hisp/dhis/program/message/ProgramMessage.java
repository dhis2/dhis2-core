package org.hisp.dhis.program.message;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

@JacksonXmlRootElement( localName = "programMessage", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramMessage
    extends BaseIdentifiableObject
    implements Serializable
{
    private static final long serialVersionUID = -5882823752156937730L;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private ProgramMessageRecipients recipients;

    private Set<DeliveryChannel> deliveryChannels = new HashSet<>();

    private ProgramMessageStatus messageStatus;

    private String text;

    private String subject;

    private Date processedDate;

    private transient boolean storeCopy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramMessage()
    {
    }

    public ProgramMessage( String text, ProgramMessageRecipients recipients )
    {
        this.text = text;
        this.recipients = recipients;
    }

    public ProgramMessage( String text, ProgramMessageRecipients recipients, Set<DeliveryChannel> deliveryChannels,
        ProgramInstance programInstance )
    {
        this(text, recipients);
        this.deliveryChannels.addAll( deliveryChannels );
        this.programInstance = programInstance;
    }

    public ProgramMessage( String text, ProgramMessageRecipients recipients, Set<DeliveryChannel> deliveryChannels,
        ProgramStageInstance programStageInstance )
    {
        this(text, recipients);
        this.deliveryChannels.addAll( deliveryChannels );
        this.programStageInstance = programStageInstance;
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @JsonProperty( value = "programInstance" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "programInstance" )
    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
    }

    @JsonProperty( value = "programStageInstance" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "programStageInstance" )
    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public void setProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        this.programStageInstance = programStageInstance;
    }

    @JsonProperty( value = "recipients" )
    @JacksonXmlProperty( localName = "recipients" )
    public ProgramMessageRecipients getRecipients()
    {
        return recipients;
    }

    public void setRecipients( ProgramMessageRecipients programMessagerecipients )
    {
        this.recipients = programMessagerecipients;
    }

    @JsonProperty( value = "deliveryChannels" )
    @JacksonXmlProperty( localName = "deliveryChannels" )
    public Set<DeliveryChannel> getDeliveryChannels()
    {
        return deliveryChannels;
    }

    public void setDeliveryChannels( Set<DeliveryChannel> deliveryChannels )
    {
        this.deliveryChannels = deliveryChannels;
    }

    @JsonProperty( value = "text" )
    @JacksonXmlProperty( localName = "text" )
    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    @JsonProperty( value = "storeCopy" )
    @JacksonXmlProperty( localName = "storeCopy" )
    public boolean getStoreCopy()
    {
        return storeCopy;
    }

    public void setStoreCopy( boolean storeCopy )
    {
        this.storeCopy = storeCopy;
    }

    @JsonProperty( value = "messageStatus" )
    @JacksonXmlProperty( localName = "messageStatus" )
    public ProgramMessageStatus getMessageStatus()
    {
        return messageStatus;
    }

    public void setMessageStatus( ProgramMessageStatus messageStatus )
    {
        this.messageStatus = messageStatus;
    }

    @JsonProperty( value = "subject" )
    @JacksonXmlProperty( localName = "subject" )
    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String messageSubject )
    {
        this.subject = messageSubject;
    }

    @JsonProperty( value = "processedDate" )
    @JacksonXmlProperty( localName = "processedDate" )
    public Date getProcessedDate()
    {
        return processedDate;
    }

    public void setProcessedDate( Date processedAt )
    {
        this.processedDate = processedAt;
    }

    @Override
    public String toString()
    {
        return "ProgramMessage: [id : " + this.id + ", " + "uid :" + this.uid + ", " + this.text + ", " + this.subject
            + ", " + this.deliveryChannels.toString() + "]";
    }
}
