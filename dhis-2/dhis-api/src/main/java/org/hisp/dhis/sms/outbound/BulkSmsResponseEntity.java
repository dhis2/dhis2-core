package org.hisp.dhis.sms.outbound;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @Author Zubair Asghar.
 */
public class BulkSmsResponseEntity
{
    private String id;
    private String type;
    private String from;
    private String to;
    private String body;

    private String encoding;
    private String protocolId;
    private String messageClass;
    private String relatedSentMessageId;
    private String userSuppliedId;
    private String numberOfParts;
    private String creditCost;

    private Status status;
    private Submission submission;

    @JsonProperty
    @JacksonXmlProperty
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getFrom()
    {
        return from;
    }

    public void setFrom( String from )
    {
        this.from = from;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getTo()
    {
        return to;
    }

    public void setTo( String to )
    {
        this.to = to;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getBody()
    {
        return body;
    }

    public void setBody( String body )
    {
        this.body = body;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getProtocolId()
    {
        return protocolId;
    }

    public void setProtocolId( String protocolId )
    {
        this.protocolId = protocolId;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getMessageClass()
    {
        return messageClass;
    }

    public void setMessageClass( String messageClass )
    {
        this.messageClass = messageClass;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getRelatedSentMessageId()
    {
        return relatedSentMessageId;
    }

    public void setRelatedSentMessageId( String relatedSentMessageId )
    {
        this.relatedSentMessageId = relatedSentMessageId;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getUserSuppliedId()
    {
        return userSuppliedId;
    }

    public void setUserSuppliedId( String userSuppliedId )
    {
        this.userSuppliedId = userSuppliedId;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getNumberOfParts()
    {
        return numberOfParts;
    }

    public void setNumberOfParts( String numberOfParts )
    {
        this.numberOfParts = numberOfParts;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getCreditCost()
    {
        return creditCost;
    }

    public void setCreditCost( String creditCost )
    {
        this.creditCost = creditCost;
    }

    @JsonProperty
    @JacksonXmlProperty
    public Status getStatus()
    {
        return status;
    }

    public void setStatus( Status status )
    {
        this.status = status;
    }

    @JsonProperty
    @JacksonXmlProperty
    public Submission getSubmission()
    {
        return submission;
    }

    public void setSubmission( Submission submission )
    {
        this.submission = submission;
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class Submission
    {
        private String id;
        private String date;

        @JsonProperty
        @JacksonXmlProperty
        public String getId()
        {
            return id;
        }

        public void setId( String id )
        {
            this.id = id;
        }

        @JsonProperty
        @JacksonXmlProperty
        public String getDate()
        {
            return date;
        }

        public void setDate( String date )
        {
            this.date = date;
        }
    }

    private static class Status
    {
        private String id;
        private String type;
        private String subtype;

        @JsonProperty
        @JacksonXmlProperty
        public String getId()
        {
            return id;
        }

        public void setId( String id )
        {
            this.id = id;
        }

        @JsonProperty
        @JacksonXmlProperty
        public String getType()
        {
            return type;
        }

        public void setType( String type )
        {
            this.type = type;
        }

        @JsonProperty
        @JacksonXmlProperty
        public String getSubtype()
        {
            return subtype;
        }

        public void setSubtype(String subtype)
        {
            this.subtype = subtype;
        }
    }
}
