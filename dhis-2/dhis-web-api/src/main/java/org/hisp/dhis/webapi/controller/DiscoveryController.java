/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NamedParams;
import org.hisp.dhis.webapi.JsonBuilder;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * A controller that provides information about available endpoints for metadata
 * API.
 *
 * @author Jan Bernitt
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DiscoveryController
{
    @Autowired
    private ObjectMapper jsonMapper;

    @Getter
    public static final class ControllerInfo
    {

        final Class<? extends IdentifiableObject> objectType;

        final String rootPath;

        final boolean supportsGistApi;

        ControllerInfo( AbstractCrudController<?> controller )
        {
            this.objectType = controller.getEntityClass();
            RequestMapping mapping = controller.getClass().getAnnotation( RequestMapping.class );
            this.rootPath = mapping == null ? null : mapping.value()[0];
            this.supportsGistApi = true;
        }

    }

    public static void register( AbstractCrudController<?> controller )
    {
        ControllerInfo info = new ControllerInfo( controller );
        if ( info.rootPath != null )
        {
            CONTROLLERS_BY_PATH.put( info.rootPath, info );
        }
    }

    private static final ConcurrentMap<String, ControllerInfo> CONTROLLERS_BY_PATH = new ConcurrentHashMap<>();

    @GetMapping( value = "/gist", produces = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody ResponseEntity<JsonNode> getAllGistEndpoints( HttpServletRequest request )
    {
        NamedParams params = new NamedParams( request::getParameter, request::getParameterValues );
        boolean absoluteUrls = params.getBoolean( "absoluteUrls", false );
        String contextRoot = absoluteUrls ? ContextUtils.getRootPath( request ) : "";
        JsonBuilder responseBuilder = new JsonBuilder( jsonMapper );
        ArrayNode body = responseBuilder.toArray( singletonList( "relativeApiEndpoint" ),
            CONTROLLERS_BY_PATH.values().stream()
                .filter( ControllerInfo::isSupportsGistApi )
                .map( info -> contextRoot + info.getRootPath() + "/gist" )
                .collect( toList() ) );
        return ResponseEntity.ok( body );
    }
}
