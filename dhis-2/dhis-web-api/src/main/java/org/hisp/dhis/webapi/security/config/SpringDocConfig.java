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
package org.hisp.dhis.webapi.security.config;

import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.system.SystemService;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 934 )
public class SpringDocConfig
{
    @Autowired
    private SystemService systemService;

    class GroupCustomizer
        extends SpecFilter
        implements GlobalOpenApiCustomizer
    {
        @Override
        public void customise( OpenAPI openApi )
        {
            Paths paths = openApi.getPaths();
            HashSet<String> refsInPaths = new HashSet<>();
            paths.forEach( ( s, pathItem ) -> pathItem.readOperations()
                .forEach( operation -> {
                    firstNonNull( operation.getRequestBody(),
                        new RequestBody().content( new Content() ) ).getContent()
                            .forEach( ( s1, mediaType ) -> refsInPaths.add( mediaType.getSchema().get$ref() ) );
                    firstNonNull( operation.getResponses(), new ApiResponses() )
                        .forEach( ( s1, apiResponse ) -> firstNonNull( apiResponse.getContent(), new Content() )
                            .forEach( ( s2, mediaType ) -> refsInPaths.add( mediaType.getSchema().get$ref() ) ) );
                    firstNonNull( operation.getParameters(), new ArrayList<Parameter>() ).forEach(
                        parameter -> refsInPaths.add( parameter.getSchema().get$ref() ) );
                } ) );
            refsInPaths.remove( null );

            Set<String> refsToKeep = refsInPaths.stream().map( s -> s.split( "/" )[3] ).collect( Collectors.toSet() );
            openApi.getComponents().getSchemas().keySet().removeIf( s -> !refsToKeep.contains( s ) );
        }
    }

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer()
    {
        return new GroupCustomizer();
    }

    @Bean
    public GroupedOpenApi trackerGroup()
    {
        return GroupedOpenApi.builder().group( "tracker" )
            .packagesToScan( "org.hisp.dhis.webapi.controller.event" )
            .pathsToMatch( "/events/**", "/programs/**", "/enrollments/**", "/relationships/**",
                "/trackedEntityInstances/**" )
            .build();
    }

    @Bean
    public GroupedOpenApi eventsGroup()
    {
        return GroupedOpenApi.builder().group( "events" )
            .packagesToScan( "org.hisp.dhis.webapi.controller" )
            .pathsToMatch( "/events/**" )
            .build();
    }

    @Bean
    public GroupedOpenApi apiTokenGroup()
    {
        return GroupedOpenApi.builder().group( "apiToken" )
            .packagesToScan( "org.hisp.dhis.webapi.controller" )
            .pathsToMatch( "/apiToken/**" )
            .build();
    }

    @Bean
    public GroupedOpenApi userRolesGroup()
    {
        return GroupedOpenApi.builder().group( "userRoles" )
            .packagesToScan( "org.hisp.dhis.webapi.controller" )
            // .producesToMatch( "application/json" )
            .pathsToMatch( "/userRoles/**" )
            .build();
    }

    @Bean
    public GroupedOpenApi usersGroup()
    {
        return GroupedOpenApi.builder().group( "users" )
            .packagesToScan( "org.hisp.dhis.webapi.controller" )
            // .producesToMatch( "application/json","text/csv",
            // "application/text" )
            // .consumesToMatch( "application/json",
            // "application/json-patch+json" )

            .pathsToMatch( "/users/**" )
            .build();
    }

    @Bean
    public GroupedOpenApi accountGroup()
    {
        return GroupedOpenApi.builder().group( "account" )
            .packagesToScan( "org.hisp.dhis.webapi.controller" )
            .pathsToMatch( "/account/**" )
            .build();
    }

    @Bean
    public OpenAPI customOpenAPI()
    {
        return new OpenAPI().components( new Components() )
            .info( new Info().title( "DHIS2 API" )
                .version( systemService.getSystemInfo().getVersion() )
                .description( "DHIS2 OpenApi specification" ) );
    }

    @Bean
    SwaggerUiConfigProperties swaggerUiConfigProperties()
    {
        SwaggerUiConfigProperties props = new SwaggerUiConfigProperties();
        props.setDefaultModelExpandDepth( -1 );
        return props;
    }

    @Bean
    SpringDocConfigProperties springDocConfigProperties()
    {
        SpringDocConfigProperties props = new SpringDocConfigProperties();
        //
        // props.setPackagesToScan( List.of(
        // "org.hisp.dhis.webapi.controller.users" ) );
        // props.setPathsToMatch( List.of( "/users/**" ) );
        // props.getApiDocs().setResolveSchemaProperties( false );
        props.getApiDocs().setEnabled( false );
        props.getApiDocs().setPath( "/api/v3/api-docs" );
        // props.getModelConverters().getPolymorphicConverter().setEnabled(
        // false );

        return props;
    }

}
