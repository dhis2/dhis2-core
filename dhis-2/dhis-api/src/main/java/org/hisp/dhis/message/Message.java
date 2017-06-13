package org.hisp.dhis.message;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.user.User;

import java.util.Date;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "message", namespace = DxfNamespaces.DXF_2_0 )
public class Message
    extends BaseIdentifiableObject
{
    /**
     * The message text.
     */
    private String text;

    /**
     * The message meta data, like user agent and OS of sender.
     */
    private String metaData;

    /**
     * The message sender.
     */
    private User sender;

    /**
     * Internal message flag. Can only be seen by users in "FeedbackRecipients" group.
     */
    private Boolean internal;

    public Message()
    {
        this.uid = CodeGenerator.generateUid();
        this.lastUpdated = new Date();
        this.internal = false;
    }

    public Message( String text, String metaData, User sender )
    {
        this.uid = CodeGenerator.generateUid();
        this.lastUpdated = new Date();
        this.text = text;
        this.metaData = metaData;
        this.sender = sender;
        this.internal = false;
    }

    public Message( String text, String metaData, User sender, boolean internal )
    {
        this.uid = CodeGenerator.generateUid();
        this.lastUpdated = new Date();
        this.text = text;
        this.metaData = metaData;
        this.sender = sender;
        this.internal = internal;
    }

    @Override
    public String getName()
    {
        return text;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getMetaData()
    {
        return metaData;
    }

    public void setMetaData( String metaData )
    {
        this.metaData = metaData;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty
    public User getSender()
    {
        return sender;
    }

    public void setSender( User sender )
    {
        this.sender = sender;
    }

    @Override
    public String toString()
    {
        return "[" + text + "]";
    }

    public boolean isInternal()
    {
        return internal;
    }

    public void setInternal( boolean internal )
    {
        this.internal = internal;
    }
}
