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
package org.hisp.dhis.webapi;

import static org.springframework.http.MediaType.parseMediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.DefaultNodeService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.webapi.mvc.CustomRequestMappingHandlerMapping;
import org.hisp.dhis.webapi.mvc.DhisApiVersionHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.interceptor.TranslationInterceptor;
import org.hisp.dhis.webapi.mvc.messageconverter.CsvMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.ExcelMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonPMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.PdfMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.RenderServiceMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter;
import org.hisp.dhis.webapi.view.CustomPathExtensionContentNegotiationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.common.collect.ImmutableMap;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@EnableWebMvc
@EnableGlobalMethodSecurity( prePostEnabled = true )
public class MvcTestConfig implements WebMvcConfigurer
{
    @Bean
    public CustomRequestMappingHandlerMapping requestMappingHandlerMapping()
    {
        CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy = new CustomPathExtensionContentNegotiationStrategy(
            mediaTypeMap );
        pathExtensionNegotiationStrategy.setUseJaf( false );

        String[] mediaTypes = new String[] { "json", "jsonp", "xml", "png", "xls", "pdf", "csv" };

        ParameterContentNegotiationStrategy parameterContentNegotiationStrategy = new ParameterContentNegotiationStrategy(
            mediaTypeMap.entrySet().stream()
                .filter( x -> ArrayUtils.contains( mediaTypes, x.getKey() ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );

        HeaderContentNegotiationStrategy headerContentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        FixedContentNegotiationStrategy fixedContentNegotiationStrategy = new FixedContentNegotiationStrategy(
            MediaType.APPLICATION_JSON );

        ContentNegotiationManager manager = new ContentNegotiationManager(
            Arrays.asList(
                pathExtensionNegotiationStrategy,
                parameterContentNegotiationStrategy,
                headerContentNegotiationStrategy,
                fixedContentNegotiationStrategy ) );

        CustomRequestMappingHandlerMapping mapping = new CustomRequestMappingHandlerMapping();
        mapping.setOrder( 0 );
        mapping.setContentNegotiationManager( manager );

        return mapping;
    }

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
        .put( "adx.xml", parseMediaType( "application/adx+xml" ) )
        .put( "adx.xml.gz", parseMediaType( "application/adx+xml+gzip" ) )
        .put( "adx.xml.zip", parseMediaType( "application/adx+xml+zip" ) )
        .put( "geojson", parseMediaType( "application/json+geojson" ) )
        .build();

    @Bean
    public NodeService nodeService()
    {
        return new DefaultNodeService();
    }

    @Override
    public void addInterceptors( InterceptorRegistry registry )
    {
        registry.addInterceptor( TranslationInterceptor.get() );
    }

    @Override
    public void configureMessageConverters(
        List<HttpMessageConverter<?>> converters )
    {
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new JsonMessageConverter( nodeService(), compression ) ) );
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new XmlMessageConverter( nodeService(), compression ) ) );
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new CsvMessageConverter( nodeService(), compression ) ) );

        converters.add( new JsonPMessageConverter( nodeService(), null ) );
        converters.add( new PdfMessageConverter( nodeService() ) );
        converters.add( new ExcelMessageConverter( nodeService() ) );

        converters.add( new StringHttpMessageConverter() );
        converters.add( new ByteArrayHttpMessageConverter() );
        converters.add( new FormHttpMessageConverter() );

        converters.add( renderServiceMessageConverter() );
    }

    @Bean
    public RenderServiceMessageConverter renderServiceMessageConverter()
    {
        return new RenderServiceMessageConverter();
    }

    @Bean
    public DhisApiVersionHandlerMethodArgumentResolver dhisApiVersionHandlerMethodArgumentResolver()
    {
        return new DhisApiVersionHandlerMethodArgumentResolver();
    }

    @Override
    public void addArgumentResolvers( List<HandlerMethodArgumentResolver> resolvers )
    {
        resolvers.add( dhisApiVersionHandlerMethodArgumentResolver() );
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler()
    {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setDefaultRolePrefix( "" );
        return expressionHandler;
    }

}
