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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author Zubair Asghar.
 */
public class BulkSmsRequestEntity
{
    private Set<To> to = new HashSet<>();

    private String body;

    public BulkSmsRequestEntity( String body, Set<String> recipients )
    {
        this.to = recipients.stream().map( r ->
        {
             To to = new To();
             to.setAddress( r );
             return to;
        } ).collect( Collectors.toSet() );

        this.body = body;
    }

    public BulkSmsRequestEntity()
    {
    }

    @JsonProperty
    @JacksonXmlProperty
    public Set<To> getTo()
    {
        return to;
    }

    public void setTo ( Set<To> to )
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

    private static class To
    {
        private String type = "INTERNATIONAL";
        private String address;

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
        public String getAddress()
        {
            return address;
        }

        public void setAddress( String address )
        {
            this.address = address;
        }
    }
}
