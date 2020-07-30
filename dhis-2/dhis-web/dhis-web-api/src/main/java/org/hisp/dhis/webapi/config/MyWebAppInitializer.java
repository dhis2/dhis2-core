package org.hisp.dhis.webapi.config;

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
public class MyWebAppInitializer implements WebApplicationInitializer
{

    @Override
    public void onStartup( ServletContext context )
    {
//        container.getSessionCookieConfig().setHttpOnly( true );
//        container.getSessionCookieConfig().setSecure( true );
        context.setSessionTrackingModes( EnumSet.of( SessionTrackingMode.COOKIE ) );

        AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
        annotationConfigWebApplicationContext.register( HttpConfig.class );

        context.addListener( new ContextLoaderListener( annotationConfigWebApplicationContext ) );

        ServletRegistration.Dynamic dispatcher = context
            .addServlet( "dispatcher", new DispatcherServlet( annotationConfigWebApplicationContext ) );

        dispatcher.setLoadOnStartup( 1 );
        dispatcher.addMapping( "/api/*" );

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
    }
}