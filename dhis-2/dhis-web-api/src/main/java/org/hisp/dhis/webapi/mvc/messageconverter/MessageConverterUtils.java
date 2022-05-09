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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.MediaType;

import com.google.common.collect.ImmutableList;

/**
 * @author Morten Olav Hansen
 */
public final class MessageConverterUtils
{
    private MessageConverterUtils()
    {
    }

    public static final ImmutableList<MediaType> XML_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "xml" ) )
        .build();

    public static final ImmutableList<MediaType> XML_GZIP_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "xml+gzip" ) )
        .build();

    public static final ImmutableList<MediaType> XML_ZIP_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "xml+zip" ) )
        .build();

    public static final ImmutableList<MediaType> JSON_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "json" ) )
        .build();

    public static final ImmutableList<MediaType> JSON_GZIP_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "json+gzip" ) )
        .build();

    public static final ImmutableList<MediaType> JSON_ZIP_SUPPORTED_MEDIA_TYPES = ImmutableList.<MediaType> builder()
        .add( new MediaType( "application", "json+zip" ) )
        .build();

    @Nonnull
    public static String getContentDispositionHeaderValue( @Nullable String extensibleFilename,
        @Nullable String compressionExtension )
    {
        final String suffix = (compressionExtension == null) ? "" : "." + compressionExtension;
        return "attachment; filename=" + StringUtils.defaultString( extensibleFilename, "metadata.json" ) + suffix;
    }

    public static boolean isAttachment( @Nullable String contentDispositionHeaderValue )
    {
        return (contentDispositionHeaderValue != null) && contentDispositionHeaderValue.contains( "attachment" );
    }

    /**
     * File name that will get a media type related suffix when included as an
     * attachment file name.
     */
    @Nullable
    public static String getExtensibleAttachmentFilename( @Nullable String contentDispositionHeaderValue,
        List<String> extensibleAttachmentFilenames )
    {
        final String filename = ContextUtils.getAttachmentFileName( contentDispositionHeaderValue );
        return (filename != null) && extensibleAttachmentFilenames.contains( filename ) ? filename : null;
    }
}
