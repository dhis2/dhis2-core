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
package org.hisp.dhis.webapi.servlet;

import java.util.EnumSet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.jsonfiltering.web.JsonFilteringRequestFilter;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.system.startup.StartupListener;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.springframework.core.annotation.Order;
import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

@Order( 10 )
@Slf4j
public class DhisWebApiWebAppInitializer implements WebApplicationInitializer
{
    @Override
    public void onStartup( ServletContext context )
    {
        boolean httpsOnly = getConfig().isEnabled( ConfigurationKey.SERVER_HTTPS );

        log.debug( String.format( "Configuring cookies, HTTPS only: %b", httpsOnly ) );

        if ( httpsOnly )
        {
            context.getSessionCookieConfig().setSecure( true );
            context.getSessionCookieConfig().setHttpOnly( true );

            log.info( "HTTPS only is enabled, cookies configured as secure" );
        }

        context.setSessionTrackingModes( EnumSet.of( SessionTrackingMode.COOKIE ) );

        AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
        annotationConfigWebApplicationContext.register( WebMvcConfig.class );

        context.addListener( new ContextLoaderListener( annotationConfigWebApplicationContext ) );

        DispatcherServlet servlet = new DispatcherServlet( annotationConfigWebApplicationContext );

        ServletRegistration.Dynamic dispatcher = context.addServlet( "dispatcher", servlet );
        dispatcher.setAsyncSupported( true );
        dispatcher.setLoadOnStartup( 1 );
        dispatcher.addMapping( "/api/*" );
        dispatcher.addMapping( "/uaa/*" );

        FilterRegistration.Dynamic openSessionInViewFilter = context.addFilter( "openSessionInViewFilter",
            OpenSessionInViewFilter.class );
        openSessionInViewFilter.setInitParameter( "sessionFactoryBeanName", "sessionFactory" );
        openSessionInViewFilter.addMappingForUrlPatterns( null, false, "/*" );
        openSessionInViewFilter.addMappingForServletNames( null, false, "dispatcher" );

        FilterRegistration.Dynamic characterEncodingFilter = context.addFilter( "characterEncodingFilter",
            CharacterEncodingFilter.class );
        characterEncodingFilter.setInitParameter( "encoding", "UTF-8" );
        characterEncodingFilter.setInitParameter( "forceEncoding", "true" );
        characterEncodingFilter.addMappingForUrlPatterns( null, false, "/*" );
        characterEncodingFilter.addMappingForServletNames( null, false, "dispatcher" );

        context.addFilter( "RequestIdentifierFilter", new DelegatingFilterProxy( "requestIdentifierFilter" ) )
            .addMappingForUrlPatterns( null, true, "/*" );

        context.addFilter( "AppOverrideFilter", new DelegatingFilterProxy( "appOverrideFilter" ) )
            .addMappingForUrlPatterns( null, true, "/*" );

        context.addFilter( "JsonFilteringRequestFilter", JsonFilteringRequestFilter.class )
            .addMappingForUrlPatterns( null, true, "/*" );

        context.addListener( new StartupListener() );
        context.addListener( new HttpSessionEventPublisher() );
    }

    private DhisConfigurationProvider getConfig()
    {
        DefaultLocationManager locationManager = DefaultLocationManager.getDefault();
        locationManager.init();
        DefaultDhisConfigurationProvider configProvider = new DefaultDhisConfigurationProvider( locationManager );
        configProvider.init();

        return configProvider;
    }
}