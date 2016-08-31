package org.hisp.dhis.webapi.view;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

/**
 * Custom PathExtensionContentNegotiationStrategy that handles multiple dots in filename.
 * Based on:
 * org.springframework.web.accept.PathExtensionContentNegotiationStrategy
 * org.springframework.util.StringUtils
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CustomPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy
{
    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    static
    {
        URL_PATH_HELPER.setUrlDecode( false );
    }

    public CustomPathExtensionContentNegotiationStrategy( Map<String, MediaType> mediaTypes )
    {
        super( mediaTypes );
    }

    @Override
    protected String getMediaTypeKey( NativeWebRequest webRequest )
    {
        HttpServletRequest servletRequest = webRequest.getNativeRequest( HttpServletRequest.class );

        if ( servletRequest == null )
        {
            return null;
        }

        String path = URL_PATH_HELPER.getLookupPathForRequest( servletRequest );
        String filename = WebUtils.extractFullFilenameFromUrlPath( path );
        String extension = getFilenameExtension( filename );
        return (StringUtils.hasText( extension )) ? extension.toLowerCase( Locale.ENGLISH ) : null;
    }

    private static final char EXTENSION_SEPARATOR = '.';

    private static final String FOLDER_SEPARATOR = "/";

    public static String getFilenameExtension( String path )
    {
        if ( path == null )
        {
            return null;
        }
        int extIndex = path.indexOf( EXTENSION_SEPARATOR );
        if ( extIndex == -1 )
        {
            return null;
        }
        int folderIndex = path.indexOf( FOLDER_SEPARATOR );
        if ( folderIndex > extIndex )
        {
            return null;
        }
        return path.substring( extIndex + 1 );
    }
}
