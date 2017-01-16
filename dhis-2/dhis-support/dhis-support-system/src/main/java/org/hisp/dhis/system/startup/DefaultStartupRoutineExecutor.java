package org.hisp.dhis.system.startup;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * Default implementation of StartupRoutineExecutor. The execute method will
 * execute the added StartupRoutines ordered by their run levels. Startup routines
 * can be ignored from the command line by appending the below.
 * 
 * <code>-Ddhis.skip.startup=true</code>
 * 
 * @author <a href="mailto:torgeilo@gmail.com">Torgeir Lorange Ostby</a>
 */
public class DefaultStartupRoutineExecutor
    implements StartupRoutineExecutor
{
    private static final Log log = LogFactory.getLog( DefaultStartupRoutineExecutor.class );

    private static final String TRUE = "true";
    private static final String SKIP_PROP = "dhis.skip.startup";
    
    @Autowired
    private DhisConfigurationProvider config;
    
    @Autowired( required = false )
    private List<StartupRoutine> startupRoutines;

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public void execute()
        throws Exception
    {
        execute( false );
    }
    
    @Override
    public void executeForTesting()
        throws Exception
    {
        execute( true );
    }
    
    private void execute( boolean testing )
        throws Exception
    {
        if ( startupRoutines == null || startupRoutines.isEmpty() )
        {
            log.debug( "No startup routines found" );
            return;
        }
        
        if ( TRUE.equalsIgnoreCase( System.getProperty( SKIP_PROP ) ) )
        {
            log.info( "Skipping startup routines, system property " + SKIP_PROP + " is true" );
            return;
        }
        
        if ( config.isReadOnlyMode() )
        {
            log.info( "Skipping startup routines, read-only mode is enabled" );
            return;
        }
        
        Collections.sort( startupRoutines, new StartupRoutineComparator() );

        int total = startupRoutines.size();
        int index = 1;

        for ( StartupRoutine routine : startupRoutines )
        {
            if ( !( testing && routine.skipInTests() ) )
            {
                log.info( "Executing startup routine [" + index + " of " + total + ", runlevel " + routine.getRunlevel()
                    + "]: " + routine.getName() );

                routine.execute();
                
                ++index;
            }
        }

        log.info( "All startup routines done" );
    }
}
