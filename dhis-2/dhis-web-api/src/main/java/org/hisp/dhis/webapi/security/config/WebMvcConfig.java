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
package org.hisp.dhis.webapi.security.config;

import static org.springframework.http.MediaType.*;

import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.commons.lang3.*;
import org.hisp.dhis.common.*;
import org.hisp.dhis.node.*;
import org.hisp.dhis.user.*;
import org.hisp.dhis.webapi.mvc.*;
import org.hisp.dhis.webapi.mvc.interceptor.*;
import org.hisp.dhis.webapi.mvc.messageconverter.*;
import org.hisp.dhis.webapi.service.*;
import org.hisp.dhis.webapi.view.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.*;
import org.springframework.format.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.security.access.expression.method.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.web.accept.*;
import org.springframework.web.method.support.*;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.commons.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

import com.google.common.collect.*;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 1000 )
@ComponentScan( basePackages = { "org.hisp.dhis" } )
@EnableGlobalMethodSecurity( prePostEnabled = true )
public class WebMvcConfig extends DelegatingWebMvcConfiguration
{
    @Autowired
    public CurrentUserHandlerMethodArgumentResolver currentUserHandlerMethodArgumentResolver;

    @Autowired
    public CurrentUserInfoHandlerMethodArgumentResolver currentUserInfoHandlerMethodArgumentResolver;

    @Autowired
    private ContextService contextService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Bean( "multipartResolver" )
    public MultipartResolver multipartResolver()
    {
        return new CommonsMultipartResolver();
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
        resolvers.add( currentUserHandlerMethodArgumentResolver );
        resolvers.add( currentUserInfoHandlerMethodArgumentResolver );
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler()
    {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setDefaultRolePrefix( "" );
        return expressionHandler;
    }

    @Bean
    public NodeService nodeService()
    {
        return new DefaultNodeService();
    }

    @Bean
    public RenderServiceMessageConverter renderServiceMessageConverter()
    {
        return new RenderServiceMessageConverter();
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

        converters.add( new JsonPMessageConverter( nodeService(), contextService ) );
        converters.add( new PdfMessageConverter( nodeService() ) );
        converters.add( new ExcelMessageConverter( nodeService() ) );

        converters.add( new StringHttpMessageConverter( StandardCharsets.UTF_8 ) );
        converters.add( new ByteArrayHttpMessageConverter() );
        converters.add( new FormHttpMessageConverter() );

        converters.add( renderServiceMessageConverter() );
    }

    @Override
    protected void addFormatters( FormatterRegistry registry )
    {
        registry.addConverter( new StringToOrderCriteriaListConverter() );
    }

    @Primary
    @Bean
    @Override
    public ContentNegotiationManager mvcContentNegotiationManager()
    {
        CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy = new CustomPathExtensionContentNegotiationStrategy(
            mediaTypeMap );
        pathExtensionNegotiationStrategy.setUseJaf( false );

        String[] mediaTypes = new String[] { "json", "jsonp", "xml", "png", "xls", "pdf", "csv", "adx.xml" };

        ParameterContentNegotiationStrategy parameterContentNegotiationStrategy = new ParameterContentNegotiationStrategy(
            mediaTypeMap.entrySet().stream()
                .filter( x -> ArrayUtils.contains( mediaTypes, x.getKey() ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );

        HeaderContentNegotiationStrategy headerContentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        FixedContentNegotiationStrategy fixedContentNegotiationStrategy = new FixedContentNegotiationStrategy(
            MediaType.APPLICATION_JSON );

        return new ContentNegotiationManager(
            Arrays.asList(
                pathExtensionNegotiationStrategy,
                parameterContentNegotiationStrategy,
                headerContentNegotiationStrategy,
                fixedContentNegotiationStrategy ) );
    }

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping()
    {
        CustomRequestMappingHandlerMapping mapping = new CustomRequestMappingHandlerMapping();
        mapping.setOrder( 0 );
        mapping.setContentNegotiationManager( mvcContentNegotiationManager() );
        return mapping;
    }

    @Override
    public void addInterceptors( InterceptorRegistry registry )
    {
        registry.addInterceptor( new UserContextInterceptor( currentUserService, userSettingService ) );
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
}
