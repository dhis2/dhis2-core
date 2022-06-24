/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.helpers.extensions;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.ResourceTableActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This extension is used to prepare/populate the analytics tables, so they can
 * be further used by the analytics e2e tests.
 *
 * @author maikel arabori
 */
public class AnalyticsSetupExtension implements BeforeAllCallback
{

    private static final Logger logger = getLogger( AnalyticsSetupExtension.class.getName() );

    /**
     * Max limit, in minutes, until the process is timed-out.
     */
    private static final long TIMEOUT = minutes( 15 );

    private static AtomicBoolean run = new AtomicBoolean( false );

    @Override
    public void beforeAll( final ExtensionContext context )
    {
        if ( run.compareAndSet( false, true ) )
        {
            logger.info( "Starting analytics table export." );

            // Login into the current DHIS2 instance.
            new LoginActions().loginAsAdmin();

            final StopWatch watcher = new StopWatch();
            watcher.start();

            // Invoke the analytics table generation process.
            final ApiResponse response = new ResourceTableActions().post( "/analytics", EMPTY )
                .validateStatus( 200 );

            final String analyticsTaskId = response.extractString( "response.id" );

            // Wait until the process is completed.
            new SystemActions().waitUntilTaskCompleted( "ANALYTICS_TABLE", analyticsTaskId, TIMEOUT )
                .validate()
                .statusCode( 200 )
                .body( "[0].uid", equalTo( analyticsTaskId ) )
                .body( "[0].id", equalTo( analyticsTaskId ) )
                .body( "[0].category", equalTo( "ANALYTICS_TABLE" ) )
                .body( "[0].message", equalTo( "Analytics tables updated" ) );

            watcher.stop();

            logger.info( "Concluding analytics table export in {} minutes", watcher.getTime( MINUTES ) );
        }
    }

    private static long minutes( final int minutes )
    {
        return MINUTES.toSeconds( minutes );
    }
}
