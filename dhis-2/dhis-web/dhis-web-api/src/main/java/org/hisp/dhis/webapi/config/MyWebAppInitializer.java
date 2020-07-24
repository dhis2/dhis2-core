package org.hisp.dhis.webapi.config;

import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

public class MyWebAppInitializer implements WebApplicationInitializer
{
    @Override
    public void onStartup( ServletContext container )
    {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register( HttpConfig.class );

        container.addListener( new ContextLoaderListener( context ) );

        ServletRegistration.Dynamic dispatcher = container.addServlet( "dispatcher", new DispatcherServlet( context ) );

        dispatcher.setLoadOnStartup( 1 );
        dispatcher.addMapping( "/api/*" );


        FilterRegistration.Dynamic filter = container
            .addFilter( "openSessionInViewFilter", OpenSessionInViewFilter.class );
//        filter.setInitParameter("sessionFactoryBeanName", "sessionFactory");
//        filter.setInitParameter( "singleSession", "true" );
//        filter.setInitParameter( "singleSession", "true" );
        filter.addMappingForServletNames( null, true, "dispatcher" );
    }
}