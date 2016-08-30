package org.hisp.dhis.sms;

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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

@JacksonXmlRootElement( localName = "messageResponseStatus", namespace = DxfNamespaces.DXF_2_0 )
public class MessageResponseStatus<T>
{
    private String responseMessage;
    
    private boolean ok;

    private T responseObject;

    public MessageResponseStatus()
    {
    }

    public MessageResponseStatus( String responseMessage, T response, boolean ok )
    {
        this.ok = ok;
        this.responseObject = response;
        this.responseMessage = responseMessage;
    }

    public T getResponseObject()
    {
        return responseObject;
    }

    public void setResponseObject( T response )
    {
        this.responseObject = response;
    }

    @JsonProperty( value = "sent" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getResponseMessage()
    {
        return responseMessage;
    }

    public void setResponseMessage( String result )
    {
        this.responseMessage = result;
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk( boolean ok )
    {
        this.ok = ok;
    }
}
