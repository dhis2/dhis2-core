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

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/openapi" )
@AllArgsConstructor
public class OpenApiController
{
    private final ApplicationContext context;

    @GetMapping( value = "/openapi.json", produces = APPLICATION_JSON_VALUE )
    public void getOpenApiDocument( @RequestParam( required = false ) Set<String> root, HttpServletResponse response )
        throws IOException
    {
        Api api = ApiAnalyser.describeApi( getAllControllerClasses(), root );
        response.setContentType( APPLICATION_JSON_VALUE );
        response.getWriter().write( OpenApiGenerator.generate( api ) );
    }

    private List<Class<?>> getAllControllerClasses()
    {
        return Stream.concat(
            context.getBeansWithAnnotation( RestController.class ).values().stream(),
            context.getBeansWithAnnotation( Controller.class ).values().stream() )
            .map( Object::getClass )
            .map( OpenApiController::deProxyClass )
            .collect( toList() );
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
}
