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
package org.hisp.dhis.web.embeddedjetty;

import static org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;

import java.security.Security;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hisp.dhis.system.startup.StartupListener;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

public class JettyEmbeddedCoreWeb extends EmbeddedJettyBase
{
    private static final int DEFAULT_HTTP_PORT = 9080;

    private static final int DEFAULT_HTTPS_PORT = 9443;

    private static final String SERVER_HOSTNAME_OR_IP = "localhost";

    private static final String WEBAPP_RESOURCE_BASE = "/webroot/";

    private static final String DEFAULT_TRUST_STORE_PATH = "ssl.jks";

    private static final String DEFAULT_PW = "secret";

    private static final Long elapsedSinceStart = System.currentTimeMillis();

    public JettyEmbeddedCoreWeb()
        throws Exception
    {
        super();
        // abortLaunchIfLogbackIsMissing();
    }

    public static void main( String[] args )
        throws Exception
    {
        setDefaultPropertyValue( "config.dir", getDefaultConfigDir() );
        // setDefaultPropertyValue( "log.audit.retain", "3650" );
        // setDefaultPropertyValue( "log.stdout.retain", "200" );
        // setDefaultPropertyValue( "log.level", "INFO" );
        // setDefaultPropertyValue( "org.eclipse.jetty.util.log.class",
        // "org.apache.logging.log4j.Logger" );

        Security.setProperty( "crypto.policy", "unlimited" );
        Security.setProperty( "networkaddress.cache.ttl", "FOREVER" );
        Security.setProperty( "networkaddress.cache.negative.ttl", "10" );

        setDefaultPropertyValue( "instance.name", "instance__core_web_default" );
        setDefaultPropertyValue( "dhis2.mode", "DEV" );
        setDefaultPropertyValue( "jdk.tls.ephemeralDHKeySize", "2048" );

        setDefaultPropertyValue( "jetty.host", SERVER_HOSTNAME_OR_IP );
        setDefaultPropertyValue( "jetty.http.port", String.valueOf( DEFAULT_HTTP_PORT ) );
        setDefaultPropertyValue( "jetty.ssl.port", String.valueOf( DEFAULT_HTTPS_PORT ) );

        setDefaultPropertyValue( "spring.profiles.active", "embeddedJetty" );

        String keystorePath = System.getProperty( "config.dir" ) + DEFAULT_TRUST_STORE_PATH;
        setDefaultSSLProperties( keystorePath, keystorePath, DEFAULT_PW, DEFAULT_PW, DEFAULT_PW );

        JettyEmbeddedCoreWeb jettyEmbeddedCoreWeb = new JettyEmbeddedCoreWeb();
        jettyEmbeddedCoreWeb.printBanner( "DHIS2 API Server" );
        jettyEmbeddedCoreWeb.startJetty();
    }

    public static Long getElapsedMsSinceStart()
    {
        return System.currentTimeMillis() - elapsedSinceStart;
    }

    public ServletContextHandler getServletContextHandler()
    {
        ServletContextHandler contextHandler = new ServletContextHandler( ServletContextHandler.SESSIONS );
        contextHandler.setErrorHandler( null );

        AnnotationConfigWebApplicationContext webApplicationContext = getWebApplicationContext();
        contextHandler.addEventListener( new ContextLoaderListener( webApplicationContext ) );

        StartupListener startupListener = new StartupListener();
        contextHandler.addEventListener( startupListener );

        // Spring Security Filter
        contextHandler.addFilter(
            new FilterHolder( new DelegatingFilterProxy( DEFAULT_FILTER_NAME ) ),
            "/*",
            EnumSet.allOf( DispatcherType.class ) );

        ContextHandler.Context context = contextHandler.getServletContext();

        DispatcherServlet servlet = new DispatcherServlet( webApplicationContext );

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

        return contextHandler;
    }

    private static AnnotationConfigWebApplicationContext getWebApplicationContext()
    {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register( SpringConfiguration.class );
        return context;
    }

    private static void abortLaunchIfLogbackIsMissing()
    {
        try
        {
            StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
            String loggerFactoryName = binder.getLoggerFactoryClassStr();
            if ( !loggerFactoryName.contains( "ch.qos.logback" ) )
            {
                String message = String.format(
                    "Unsupported log framework found. Found '%s' but Logback is required. Aborting start up.",
                    loggerFactoryName );
                System.err.println( message );
                System.exit( 1 );
            }
        }
        catch ( Throwable e )
        {
            System.err.println( String.format( "Severe log error; Aborting start up. '%s'", e ) );
            System.exit( 1 );
        }
    }

}
