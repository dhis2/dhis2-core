package org.hisp.dhis.program.message;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.DeliveryChannel;
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

    private String subject;

    private String text;

    private Date processedDate;

    private transient boolean storeCopy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramMessage()
    {
    }

    public ProgramMessage( String subject, String text, ProgramMessageRecipients recipients )
    {
        this.subject = subject;
        this.text = text;
        this.recipients = recipients;
    }

    public ProgramMessage( String subject, String text, ProgramMessageRecipients recipients, Set<DeliveryChannel> deliveryChannels,
        ProgramInstance programInstance )
    {
        this( subject, text, recipients );
        this.deliveryChannels = deliveryChannels;
        this.programInstance = programInstance;
    }

    public ProgramMessage( String subject, String text, ProgramMessageRecipients recipients, Set<DeliveryChannel> deliveryChannels,
        ProgramStageInstance programStageInstance )
    {
        this( subject, text, recipients );
        this.deliveryChannels = deliveryChannels;
        this.programStageInstance = programStageInstance;
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public void setProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        this.programStageInstance = programStageInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramMessageRecipients getRecipients()
    {
        return recipients;
    }

    public void setRecipients( ProgramMessageRecipients programMessagerecipients )
    {
        this.recipients = programMessagerecipients;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "deliveryChannels", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "deliveryChannel", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DeliveryChannel> getDeliveryChannels()
    {
        return deliveryChannels;
    }

    public void setDeliveryChannels( Set<DeliveryChannel> deliveryChannels )
    {
        this.deliveryChannels = deliveryChannels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramMessageStatus getMessageStatus()
    {
        return messageStatus;
    }

    public void setMessageStatus( ProgramMessageStatus messageStatus )
    {
        this.messageStatus = messageStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String messageSubject )
    {
        this.subject = messageSubject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getProcessedDate()
    {
        return processedDate;
    }

    public void setProcessedDate( Date processedDate )
    {
        this.processedDate = processedDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isStoreCopy()
    {
        return storeCopy;
    }

    public void setStoreCopy( boolean storeCopy )
    {
        this.storeCopy = storeCopy;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "uid", uid )
            .add( "program stage instance", programStageInstance )
            .add( "program instance", programInstance )
            .add( "recipients", recipients )
            .add( "delivery channels", deliveryChannels )
            .add( "subject", subject )
            .add( "text", text )
            .toString();
    }
}
