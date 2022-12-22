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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconData;
import org.hisp.dhis.icon.IconService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.net.MediaType;

/**
 * @author Kristian Wærstad
 */
@OpenApi.Tags( "ui" )
@Controller
@RequestMapping( value = IconSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class IconController
{
    private static final int TTL = 365;

    @Autowired
    private IconService iconService;

    @Autowired
    private ContextService contextService;

    @GetMapping
    public @ResponseBody List<IconData> getIcons( HttpServletResponse response,
        @RequestParam( required = false ) Collection<String> keywords )
    {
        Collection<IconData> icons;

        if ( keywords == null )
        {
            icons = iconService.getIcons();
        }
        else
        {
            icons = iconService.getIcons( keywords );
        }

        return icons.stream()
            .map( data -> data.setReference( String.format( "%s%s/%s/icon.%s", contextService.getApiPath(),
                IconSchemaDescriptor.API_ENDPOINT, data.getKey(), Icon.SUFFIX ) ) )
            .collect( Collectors.toList() );
    }

    @GetMapping( "/keywords" )
    public @ResponseBody Collection<String> getKeywords( HttpServletResponse response )
    {
        return iconService.getKeywords();
    }

    @GetMapping( "/{iconKey}" )
    public @ResponseBody IconData getIcon( HttpServletResponse response, @PathVariable String iconKey )
        throws WebMessageException
    {
        Optional<IconData> icon = iconService.getIcon( iconKey );

        if ( !icon.isPresent() )
        {
            throw new WebMessageException(
                notFound( String.format( "Icon not found: '%s", iconKey ) ) );
        }

        icon.get().setReference( String.format( "%s%s/%s/icon.%s", contextService.getApiPath(),
            IconSchemaDescriptor.API_ENDPOINT, icon.get().getKey(), Icon.SUFFIX ) );

        return icon.get();
    }

    @GetMapping( "/{iconKey}/icon.svg" )
    public void getIconData( HttpServletResponse response, @PathVariable String iconKey )
        throws WebMessageException,
        IOException
    {
        Optional<Resource> icon = iconService.getIconResource( iconKey );

        if ( !icon.isPresent() )
        {
            throw new WebMessageException(
                notFound( String.format( "Icon resource not found: '%s", iconKey ) ) );
        }

        response.setHeader( "Cache-Control", CacheControl.maxAge( TTL, TimeUnit.DAYS ).getHeaderValue() );
        response.setContentType( MediaType.SVG_UTF_8.toString() );

        StreamUtils.copyThenCloseInputStream( icon.get().getInputStream(), response.getOutputStream() );
    }
}
