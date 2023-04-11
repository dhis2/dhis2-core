/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.scheduling.parameters;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.scheduling.JobParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobParameters", namespace = DxfNamespaces.DXF_2_0 )
public class SmsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -6116489359345047961L;

    private String smsSubject;

    private List<String> recipientsList = new ArrayList<>();

    private String message;

    public SmsJobParameters()
    {
    }

    public SmsJobParameters( String smsSubject, String message, List<String> recipientsList )
    {
        this.smsSubject = smsSubject;
        this.recipientsList = recipientsList;
        this.message = message;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSmsSubject()
    {
        return smsSubject;
    }

    public void setSmsSubject( String smsSubject )
    {
        this.smsSubject = smsSubject;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "recipientsLists", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "recipientsList", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getRecipientsList()
    {
        return recipientsList;
    }

    public void setRecipientsList( List<String> recipientsList )
    {
        this.recipientsList = recipientsList;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }
}
