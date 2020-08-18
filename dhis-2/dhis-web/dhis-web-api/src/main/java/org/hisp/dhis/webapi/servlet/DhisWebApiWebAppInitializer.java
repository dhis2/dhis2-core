package org.hisp.dhis.webapi.servlet;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.system.startup.StartupListener;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.springframework.core.annotation.Order;
import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;
import java.util.EnumSet;

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

        ServletRegistration.Dynamic dispatcher = context
            .addServlet( "dispatcher", new DispatcherServlet( annotationConfigWebApplicationContext ) );

        dispatcher.setAsyncSupported( true );
        dispatcher.setLoadOnStartup( 1 );
        dispatcher.addMapping( "/api/*" );
        dispatcher.addMapping( "/uaa/*" );

        FilterRegistration.Dynamic openSessionInViewFilter = context.addFilter( "openSessionInViewFilter",
            OpenSessionInViewFilter.class );
        openSessionInViewFilter.setInitParameter( "sessionFactoryBeanName", "sessionFactory" );
        openSessionInViewFilter.setInitParameter( "singleSession", "true" );
        openSessionInViewFilter.setInitParameter( "flushMode", "AUTO" );
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

        context.addListener( new StartupListener() );
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