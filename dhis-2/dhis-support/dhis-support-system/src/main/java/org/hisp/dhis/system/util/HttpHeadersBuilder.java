package org.hisp.dhis.system.util;

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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;

/**
 * Builder of Spring {@link HttpHeaders} instances.
 * 
 * @author Lars Helge Overland
 */
public class HttpHeadersBuilder
{
    private HttpHeaders headers;
    
    public HttpHeadersBuilder()
    {
        this.headers = new HttpHeaders();
    }
    
    /**
     * Builds the {@link HttpHeaders} instance.
     */
    public HttpHeaders build()
    {
        return headers;
    }
    
    public HttpHeadersBuilder withContentTypeJson()
    {
        this.headers.set( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE );
        return this;
    }
    
    public HttpHeadersBuilder withContentTypeXml()
    {
        this.headers.set( HttpHeaders.CONTENT_TYPE,  MediaType.APPLICATION_XML_VALUE );
        return this;
    }
    
    public HttpHeadersBuilder withAcceptJson()
    {
        this.headers.set( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        return this;
    }
    
    public HttpHeadersBuilder withAcceptXml()
    {
        this.headers.set( HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE );
        return this;
    }
    
    public HttpHeadersBuilder withNoCache()
    {
        this.headers.set( HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue() );
        return this;
    }
    
    public HttpHeadersBuilder withBasicAuth( String username, String password )
    {
        this.headers.set( HttpHeaders.AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
        return this;
    }
}
