package org.hisp.dhis.web.mobile.controller;

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

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
public class MobileController
{
    @RequestMapping( value = "/mobile" )
    public String base()
    {
        return "redirect:/mobile/index";
    }

    @RequestMapping( value = "/" )
    public String baseWithSlash()
    {
        return "redirect:/mobile/index";
    }

    @RequestMapping( value = "/index" )
    public String index( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "index.vm" );

        return "layout";
    }

    @RequestMapping( value = "/messages" )
    public String messages( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "messages.vm" );

        return "layout";
    }

    @RequestMapping( value = "/messages/new-message" )
    public String newMessage( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "new-message.vm" );

        return "layout";
    }

    @RequestMapping( value = "/messages/{uid}" )
    public String message( @PathVariable( "uid" ) String uid, Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "message.vm" );
        model.addAttribute( "messageId", uid );

        return "layout";
    }

    @RequestMapping( value = "/interpretations" )
    public String interpretations( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "interpretations.vm" );

        return "layout";
    }

    @RequestMapping( value = "/user-account" )
    public String settings( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "user-account.vm" );

        return "layout";
    }

    @RequestMapping( value = "/data-entry" )
    public String dataEntry( Model model, HttpServletRequest request )
    {
        populateContextPath( model, request );
        model.addAttribute( "page", "data-entry.vm" );

        return "layout";
    }

    @RequestMapping( value = "/app-cache" )
    public void appCache( HttpServletResponse response ) throws IOException
    {
        response.setContentType( "text/cache-manifest" );
        InputStream inputStream = new ClassPathResource( "dhis-mobile-manifest.appcache" ).getInputStream();
        IOUtils.copy( inputStream, response.getOutputStream() );
    }

    private void populateContextPath( Model model, HttpServletRequest request )
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
}
