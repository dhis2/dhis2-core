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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.OpenApi;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Tags( "system" )
@RestController
@RequestMapping( "" )
@AllArgsConstructor
public class OpenApiController
{
    private static final String APPLICATION_X_YAML = "application/x-yaml";

    private final ApplicationContext context;

    /*
     * YAML
     */

    @GetMapping( value = "/openapi.yaml", produces = APPLICATION_X_YAML )
    public void getFullOpenApiYaml(
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, Set.of(), Set.of(), failOnNameClash, APPLICATION_X_YAML,
            OpenApiGenerator::generateYaml );
    }

    @GetMapping( value = "/{path}/openapi.yaml", produces = APPLICATION_X_YAML )
    public void getPathOpenApiYaml( @PathVariable String path,
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, Set.of( "/" + path ), Set.of(), failOnNameClash, APPLICATION_X_YAML,
            OpenApiGenerator::generateYaml );
    }

    @GetMapping( value = "/openapi/openapi.yaml", produces = APPLICATION_X_YAML )
    public void getOpenApiYaml(
        @RequestParam( required = false ) Set<String> path,
        @RequestParam( required = false ) Set<String> tag,
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, path, tag, failOnNameClash, APPLICATION_X_YAML,
            OpenApiGenerator::generateYaml );
    }

    /*
     * JSON
     */

    @GetMapping( value = "/openapi.json", produces = APPLICATION_JSON_VALUE )
    public void getFullOpenApiJson(
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, Set.of(), Set.of(), failOnNameClash, APPLICATION_JSON_VALUE,
            OpenApiGenerator::generateJson );
    }

    @GetMapping( value = "/{path}/openapi.json", produces = APPLICATION_JSON_VALUE )
    public void getPathOpenApiJson( @PathVariable String path,
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, Set.of( "/" + path ), Set.of(), failOnNameClash, APPLICATION_JSON_VALUE,
            OpenApiGenerator::generateJson );
    }

    @GetMapping( value = "/openapi/openapi.json", produces = APPLICATION_JSON_VALUE )
    public void getOpenApiJson(
        @RequestParam( required = false ) Set<String> path,
        @RequestParam( required = false ) Set<String> tag,
        @RequestParam( required = false, defaultValue = "false" ) boolean failOnNameClash,
        HttpServletRequest request, HttpServletResponse response )
    {
        writeDocument( request, response, path, tag, failOnNameClash, APPLICATION_JSON_VALUE,
            OpenApiGenerator::generateJson );
    }

    private void writeDocument( HttpServletRequest request, HttpServletResponse response,
        Set<String> paths, Set<String> tags, boolean failOnNameClash,
        String contentType, BiFunction<Api, String, String> writer )
    {
        Api api = ApiAnalyse.analyseApi(
            new ApiAnalyse.Scope( getAllControllerClasses(), paths, tags ) );

        ApiFinalise.finaliseApi( api, ApiFinalise.Configuration.builder()
            .failOnNameClash( failOnNameClash )
            .namePartDelimiter( "-" )
            .build() );
        response.setContentType( contentType );
        try
        {
            response.getWriter().write( writer.apply( api, getServerUrl( request ) ) );
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    private Set<Class<?>> getAllControllerClasses()
    {
        return Stream.concat(
            context.getBeansWithAnnotation( RestController.class ).values().stream(),
            context.getBeansWithAnnotation( Controller.class ).values().stream() )
            .map( Object::getClass )
            .map( OpenApiController::deProxyClass )
            .collect( toSet() );
    }

    /**
     * In case the bean class is a spring-enhanced proxy this resolves the
     * source class.
     */
    private static Class<?> deProxyClass( Class<?> c )
    {
        return !c.isAnnotationPresent( RestController.class )
            && !c.isAnnotationPresent( Controller.class )
                ? c.getSuperclass()
                : c;
    }

    /**
     * This has to work with 3 types of URLs
     *
     * <pre>
     *     http://localhost/openapi.json
     *     http://localhost:8080/api/openapi.json
     *     https://play.dhis2.org/dev/api/openapi.json
     * </pre>
     *
     * And any of the variants when it comes to the path the controller allows
     * to query an OpenAPI document.
     */
    private static String getServerUrl( HttpServletRequest request )
    {
        StringBuffer url = request.getRequestURL();
        String servletPath = request.getServletPath();
        servletPath = servletPath.substring( servletPath.indexOf( "/api" ) + 1 );
        int apiStart = url.indexOf( "/api/" );
        String root = apiStart < 0 ? url.substring( 0, url.indexOf( "/", 10 ) ) : url.substring( 0, apiStart );
        return root + "/" + servletPath;
    }
}
