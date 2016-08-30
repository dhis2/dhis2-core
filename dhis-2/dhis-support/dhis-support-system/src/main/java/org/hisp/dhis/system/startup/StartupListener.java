package org.hisp.dhis.system.startup;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Implementation of {@link javax.servlet.ServletContextListener} which hooks
 * into the context initialization and executes the configured
 * {@link StartupRoutineExecutor}.
 *
 * @author <a href="mailto:torgeilo@gmail.com">Torgeir Lorange Ostby</a>
 */
public class StartupListener
    implements ServletContextListener
{
    private static final Log log = LogFactory.getLog( StartupListener.class );

    // -------------------------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------------------------

    @Override
    public void contextInitialized( ServletContextEvent event )
    {
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext( event
            .getServletContext() );

        StartupRoutineExecutor startupRoutineExecutor = (StartupRoutineExecutor) applicationContext
            .getBean( StartupRoutineExecutor.ID );

        try
        {
            startupRoutineExecutor.execute();            
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            
            throw new RuntimeException( "Failed to run startup routines: " + ex.getMessage(), ex );
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent event )
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        
        while ( drivers.hasMoreElements() )
        {
            Driver driver = drivers.nextElement();
            try
            {
                DriverManager.deregisterDriver( driver );
                log.info( "De-registering jdbc driver: " + driver );
            }
            catch ( SQLException e )
            {
                log.info( "Error de-registering driver " + driver + " :" + e.getMessage() );
            }
        }
    }
}
