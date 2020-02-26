/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.webapi.config;

import static org.springframework.http.MediaType.parseMediaType;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.webapi.mvc.CustomRequestMappingHandlerMapping;
import org.hisp.dhis.webapi.view.CustomPathExtensionContentNegotiationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class WebConfig
{
    private Map<String, MediaType> mediaTypeMap = new ImmutableMap.Builder<String, MediaType>()
        .put( "json", MediaType.APPLICATION_JSON )
        .put( "json.gz", parseMediaType( "application/json+gzip" ) )
        .put( "json.zip", parseMediaType( "application/json+zip" ) )
        .put( "jsonp", parseMediaType( "application/javascript" ) )
        .put( "xml", MediaType.APPLICATION_XML ) 
        .put( "xml.gz", parseMediaType( "application/xml+gzip" ) )
        .put( "xml.zip", parseMediaType( "application/xml+zip" ) )
        .put( "png", MediaType.IMAGE_PNG )
        .put( "pdf", MediaType.APPLICATION_PDF )
        .put( "xls", parseMediaType( "application/vnd.ms-excel" ) )
        .put( "xlsx", parseMediaType( "application/vnd.ms-excel" ) )
        .put( "csv", parseMediaType( "application/csv" ) )
        .put( "csv.gz", parseMediaType( "application/csv+gzip" ) )
        .put( "csv.zip", parseMediaType( "application/csv+zip" ) )
        .put( "geojson", parseMediaType( "application/json+geojson" ) )
        .build();

    @Bean
    public HandlerMappingIntrospector handlerMappingIntrospector()
    {
        return new HandlerMappingIntrospector();
    }

    @Bean
    public CustomPathExtensionContentNegotiationStrategy customPathExtensionContentNegotiationStrategy()
    {
        CustomPathExtensionContentNegotiationStrategy customPathExtensionContentNegotiationStrategy = new CustomPathExtensionContentNegotiationStrategy(
            mediaTypeMap );
        customPathExtensionContentNegotiationStrategy.setUseJaf( false );
        return customPathExtensionContentNegotiationStrategy;
    }

    @Bean
    public ParameterContentNegotiationStrategy parameterContentNegotiationStrategy()
    {
        String[] mediaTypes = new String[] { "json", "jsonp", "xml", "png", "xls","pdf", "csv"};

        return new ParameterContentNegotiationStrategy( mediaTypeMap.entrySet().stream()
            .filter( x -> ArrayUtils.contains( mediaTypes, x.getKey() ) )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
    }

    @Bean
    public HeaderContentNegotiationStrategy headerContentNegotiationStrategy()
    {
        return new HeaderContentNegotiationStrategy();
    }

    @Bean
    public FixedContentNegotiationStrategy fixedContentNegotiationStrategy()
    {
        return new FixedContentNegotiationStrategy( MediaType.APPLICATION_JSON );
    }

    @Bean
    public ContentNegotiationManager contentNegotiationManager(
        CustomPathExtensionContentNegotiationStrategy customPathExtensionContentNegotiationStrategy,
        ParameterContentNegotiationStrategy parameterContentNegotiationStrategy,
        HeaderContentNegotiationStrategy headerContentNegotiationStrategy,
        FixedContentNegotiationStrategy fixedContentNegotiationStrategy )
    {
        return new ContentNegotiationManager( Arrays.asList( customPathExtensionContentNegotiationStrategy,
            parameterContentNegotiationStrategy, headerContentNegotiationStrategy, fixedContentNegotiationStrategy ) );
    }

    @Bean
    public CustomRequestMappingHandlerMapping customRequestMappingHandlerMapping(
        ContentNegotiationManager contentNegotiationManager )
    {
        CustomRequestMappingHandlerMapping customRequestMappingHandlerMapping = new CustomRequestMappingHandlerMapping();
        customRequestMappingHandlerMapping.setContentNegotiationManager( contentNegotiationManager );

        return customRequestMappingHandlerMapping;
    }
}
