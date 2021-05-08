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

        ContextHandler.Context context = contextHandler.getServletContext();

        contextHandler.addFilter(
            new FilterHolder( new DelegatingFilterProxy( DEFAULT_FILTER_NAME ) ),
            "/*",
            EnumSet.allOf( DispatcherType.class ) );

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
