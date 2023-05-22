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

import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.JSON_GZIP_SUPPORTED_MEDIA_TYPES;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.JSON_SUPPORTED_MEDIA_TYPES;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.JSON_ZIP_SUPPORTED_MEDIA_TYPES;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.getContentDispositionHeaderValue;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.getExtensibleAttachmentFilename;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.isAttachment;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.Compression;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen
 */
public class StreamingJsonRootMessageConverter extends AbstractHttpMessageConverter<StreamingJsonRoot<?>>
{
    private final FieldFilterService fieldFilterService;

    private final Compression compression;

    public StreamingJsonRootMessageConverter( FieldFilterService fieldFilterService, Compression compression )
    {
        this.fieldFilterService = fieldFilterService;
        this.compression = compression;

        switch ( compression )
        {
            case NONE:
                setSupportedMediaTypes( JSON_SUPPORTED_MEDIA_TYPES );
                break;
            case GZIP:
                setSupportedMediaTypes( JSON_GZIP_SUPPORTED_MEDIA_TYPES );
                break;
            case ZIP:
                setSupportedMediaTypes( JSON_ZIP_SUPPORTED_MEDIA_TYPES );
        }
    }

    @Override
    protected boolean supports( Class<?> clazz )
    {
        return StreamingJsonRoot.class.equals( clazz );
    }

    @Override
    protected StreamingJsonRoot<?> readInternal( Class<? extends StreamingJsonRoot<?>> clazz,
        HttpInputMessage inputMessage )
        throws IOException,
        HttpMessageNotReadableException
    {
        return null;
    }

    @Override
    protected void writeInternal( @Nonnull StreamingJsonRoot<?> jsonRoot, HttpOutputMessage outputMessage )
        throws IOException,
        HttpMessageNotWritableException
    {
        ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

        final String contentDisposition = outputMessage.getHeaders()
            .getFirst( ContextUtils.HEADER_CONTENT_DISPOSITION );
        final boolean attachment = isAttachment( contentDisposition );
        final String extensibleAttachmentFilename = getExtensibleAttachmentFilename(
            contentDisposition, List.of( "metadata" ) );

        if ( Compression.GZIP == compression )
        {
            if ( !attachment || (extensibleAttachmentFilename != null) )
            {
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_DISPOSITION,
                    getContentDispositionHeaderValue( extensibleAttachmentFilename, "gz" ) );
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            }

            GZIPOutputStream outputStream = new GZIPOutputStream( outputMessage.getBody() );
            writeJsonRoot( jsonMapper, jsonRoot, outputStream );
            outputStream.close();
        }
        else if ( Compression.ZIP == compression )
        {
            if ( !attachment || (extensibleAttachmentFilename != null) )
            {
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_DISPOSITION,
                    getContentDispositionHeaderValue( extensibleAttachmentFilename, "zip" ) );
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            }

            ZipOutputStream outputStream = new ZipOutputStream( outputMessage.getBody() );
            outputStream.putNextEntry( new ZipEntry( "metadata.json" ) );
            writeJsonRoot( jsonMapper, jsonRoot, outputStream );
            outputStream.close();
        }
        else
        {
            if ( extensibleAttachmentFilename != null )
            {
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_DISPOSITION,
                    getContentDispositionHeaderValue( extensibleAttachmentFilename, null ) );
            }

            writeJsonRoot( jsonMapper, jsonRoot, outputMessage.getBody() );
            outputMessage.getBody().close();
        }
    }

    private void writeJsonRoot( ObjectMapper jsonMapper, StreamingJsonRoot<?> jsonRoot, OutputStream outputStream )
        throws IOException
    {
        try ( JsonGenerator generator = jsonMapper.getFactory().createGenerator( outputStream ) )
        {
            if ( jsonRoot.getPager() == null && jsonRoot.getWrapperName() == null )
            {
                fieldFilterService.toObjectNodesStream( jsonRoot.getParams(), generator );
            }
            else
            {
                generator.writeStartObject();
                if ( jsonRoot.getPager() != null )
                {
                    generator.writeObjectField( "pager", jsonRoot.getPager() );
                }
                generator.writeArrayFieldStart( jsonRoot.getWrapperName() );
                fieldFilterService.toObjectNodesStream( jsonRoot.getParams(), generator );
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }
    }
}
