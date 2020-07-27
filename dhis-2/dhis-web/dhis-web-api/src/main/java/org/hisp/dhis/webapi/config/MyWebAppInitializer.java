package org.hisp.dhis.webapi.config;

import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;
import java.util.EnumSet;

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

        FilterRegistration.Dynamic filter = context
            .addFilter( "openSessionInViewFilter", OpenSessionInViewFilter.class );
//        filter.setInitParameter("sessionFactoryBeanName", "sessionFactory");
//        filter.setInitParameter( "singleSession", "true" );
//        filter.setInitParameter( "singleSession", "true" );
        filter.addMappingForServletNames( null, true, "dispatcher" );
    }
}