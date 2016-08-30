package org.hisp.dhis.webapi.messageconverter;

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

import com.google.common.collect.ImmutableList;
import org.hisp.dhis.render.RenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class RenderServiceMessageConverter extends AbstractHttpMessageConverter<Object>
{
    public static final ImmutableList<MediaType> SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType>builder()
        .add( new MediaType( "application", "json" ) )
        .add( new MediaType( "application", "xml" ) )
        .build();

    @Autowired
    private RenderService renderService;

    public RenderServiceMessageConverter()
    {
        setSupportedMediaTypes( SUPPORTED_MEDIA_TYPES );
    }

    @Override
    protected boolean supports( Class<?> clazz )
    {
        return Object.class.isAssignableFrom( clazz );
    }

    @Override
    protected Object readInternal( Class<?> clazz, HttpInputMessage inputMessage ) throws IOException, HttpMessageNotReadableException
    {
        MediaType mediaType = inputMessage.getHeaders().getContentType();

        if ( isJson( mediaType ) )
        {
            return renderService.fromJson( inputMessage.getBody(), clazz );
        }
        else if ( isXml( mediaType ) )
        {
            return renderService.fromXml( inputMessage.getBody(), clazz );
        }

        return null;
    }

    @Override
    protected void writeInternal( Object object, HttpOutputMessage outputMessage ) throws IOException, HttpMessageNotWritableException
    {
        MediaType mediaType = outputMessage.getHeaders().getContentType();

        if ( isJson( mediaType ) )
        {
            renderService.toJson( outputMessage.getBody(), object );
        }
        else if ( isXml( mediaType ) )
        {
            renderService.toXml( outputMessage.getBody(), object );
        }
    }

    private boolean isXml( MediaType mediaType )
    {
        return (mediaType.getType().equals( "application" ) || mediaType.getType().equals( "text" ))
            && mediaType.getSubtype().equals( "xml" );
    }

    private boolean isJson( MediaType mediaType )
    {
        return mediaType.getType().equals( "application" ) && mediaType.getSubtype().equals( "json" );
    }
}
