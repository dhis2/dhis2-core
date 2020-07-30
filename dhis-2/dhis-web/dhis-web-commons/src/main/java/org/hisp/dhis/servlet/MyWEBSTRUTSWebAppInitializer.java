package org.hisp.dhis.servlet;

import org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.EnumSet;

@Order( 12 )
public class MyWEBSTRUTSWebAppInitializer implements WebApplicationInitializer
{

    @Override
    public void onStartup( ServletContext context )
    {
        FilterRegistration.Dynamic strutsFilter = context
            .addFilter( "StrutsDispatcher", new StrutsPrepareAndExecuteFilter() );

        strutsFilter.addMappingForUrlPatterns( EnumSet.of( DispatcherType.REQUEST ), true, "*.action" );
    }
}