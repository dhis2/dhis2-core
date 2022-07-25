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
package org.hisp.dhis.webapi.mvc.messageconverter;

import javax.servlet.http.HttpServletRequest;

import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom {@link MappingJackson2XmlHttpMessageConverter} that only supports XML
 * for certain endpoints.
 *
 * @author Morten Olav Hansen
 */
public class XmlPathMappingJackson2XmlHttpMessageConverter extends MappingJackson2XmlHttpMessageConverter
{
    public XmlPathMappingJackson2XmlHttpMessageConverter( ObjectMapper objectMapper )
    {
        super( objectMapper );
    }

    @Override
    public boolean canRead( Class<?> clazz, MediaType mediaType )
    {
        HttpServletRequest request = ContextUtils.getRequest();
        String pathInfo = request.getPathInfo();

        for ( var pathPattern : WebMvcConfig.XML_PATTERNS )
        {
            if ( pathPattern.matcher( pathInfo ).matches() )
            {
                return super.canRead( clazz, mediaType );
            }
        }

        return false;
    }

    @Override
    public boolean canWrite( Class<?> clazz, MediaType mediaType )
    {
        HttpServletRequest request = ContextUtils.getRequest();
        String pathInfo = request.getPathInfo();

        for ( var pathPattern : WebMvcConfig.XML_PATTERNS )
        {
            if ( pathPattern.matcher( pathInfo ).matches() )
            {
                return super.canWrite( clazz, mediaType );
            }
        }

        return false;
    }
}
