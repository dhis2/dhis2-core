package org.hisp.dhis.api.mobile.support;

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

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class DataStreamSerializableMessageConverter
    implements HttpMessageConverter<DataStreamSerializable>
{
    @Override
    public boolean canRead( Class<?> clazz, MediaType mediaType )
    {
        if ( mediaType == null )
        {
            return DataStreamSerializable.class.isAssignableFrom( clazz );
        }
        else
        {
            return MediaTypes.MEDIA_TYPES.contains( mediaType )
                && DataStreamSerializable.class.isAssignableFrom( clazz );
        }
    }

    @Override
    public boolean canWrite( Class<?> clazz, MediaType mediaType )
    {
        if ( mediaType == null )
        {
            return DataStreamSerializable.class.isAssignableFrom( clazz );
        }
        else
        {
            return MediaTypes.MEDIA_TYPES.contains( mediaType )
                && DataStreamSerializable.class.isAssignableFrom( clazz );
        }
    }

    @Override
    public List<MediaType> getSupportedMediaTypes()
    {
        return MediaTypes.MEDIA_TYPES;
    }

    @Override
    public DataStreamSerializable read( Class<? extends DataStreamSerializable> clazz, HttpInputMessage inputMessage )
        throws IOException, HttpMessageNotReadableException
    {
        return DataStreamSerializer.read( clazz, inputMessage.getBody() );

    }

    @Override
    public void write( DataStreamSerializable entity, MediaType contentType, HttpOutputMessage outputMessage )
        throws IOException, HttpMessageNotWritableException
    {
        outputMessage.getHeaders().setContentType( contentType );
        DataStreamSerializer.write( entity, outputMessage.getBody() );
    }
}
