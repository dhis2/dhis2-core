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

import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.getContentDispositionHeaderValue;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.getExtensibleAttachmentFilename;
import static org.hisp.dhis.webapi.mvc.messageconverter.MessageConverterUtils.isAttachment;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Abstract base class for HTTP message converters that convert root nodes.
 *
 * @author Volker Schmidt <volker@dhis2.org>
 */
public abstract class AbstractRootNodeMessageConverter extends AbstractHttpMessageConverter<RootNode>
{
    private final NodeService nodeService;

    private final String contentType;

    private final String fileExtension;

    private final Compression compression;

    protected AbstractRootNodeMessageConverter( @Nonnull NodeService nodeService, @Nonnull String contentType,
        @Nonnull String fileExtension, Compression compression )
    {
        this.nodeService = nodeService;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
        this.compression = compression;
    }

    protected Compression getCompression()
    {
        return compression;
    }

    @Override
    protected boolean supports( Class<?> clazz )
    {
        return RootNode.class.equals( clazz );
    }

    @Override
    protected boolean canRead( MediaType mediaType )
    {
        return false;
    }

    @Override
    protected RootNode readInternal( Class<? extends RootNode> clazz, HttpInputMessage inputMessage )
    {
        return null;
    }

    @Override
    protected void writeInternal( RootNode rootNode, HttpOutputMessage outputMessage )
        throws IOException,
        HttpMessageNotWritableException
    {
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
            nodeService.serialize( rootNode, contentType, outputStream );
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
            outputStream.putNextEntry( new ZipEntry( "metadata." + fileExtension ) );
            nodeService.serialize( rootNode, contentType, outputStream );
            outputStream.close();
        }
        else
        {
            if ( extensibleAttachmentFilename != null )
            {
                outputMessage.getHeaders().set( ContextUtils.HEADER_CONTENT_DISPOSITION,
                    getContentDispositionHeaderValue( extensibleAttachmentFilename, null ) );
            }

            nodeService.serialize( rootNode, contentType, outputMessage.getBody() );
            outputMessage.getBody().close();
        }
    }
}
