package org.hisp.dhis.web.ohie.fred.webapi.v1.utils;

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

import org.springframework.ui.Model;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class ContextUtils
{
    public static void populateContextPath( Model model, HttpServletRequest request )
    {
        UriComponents contextPath = ServletUriComponentsBuilder.fromContextPath( request ).build();

        String contextPathString = contextPath.toString();
        String xForwardedProto = request.getHeader( "X-Forwarded-Proto" );

        if ( xForwardedProto != null )
        {
            if ( contextPathString.contains( "http://" ) && xForwardedProto.equalsIgnoreCase( "https" ) )
            {
                contextPathString = contextPathString.replace( "http://", "https://" );
            }
            else if ( contextPathString.contains( "https://" ) && xForwardedProto.equalsIgnoreCase( "http" ) )
            {
                contextPathString = contextPathString.replace( "https://", "http://" );
            }
        }

        model.addAttribute( "contextPath", contextPathString );
    }

    private ContextUtils()
    {
    }
}
