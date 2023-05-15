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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.WebTestConfiguration.TestSchedulingManager;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link ResourceTableController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class ResourceTableControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private SchedulingManager schedulingManager;

    @BeforeEach
    void setUp()
    {
        ((TestSchedulingManager) schedulingManager).setEnabled( false );
    }

    @AfterEach
    void tearDown()
    {
        TestSchedulingManager testSchedulingManager = (TestSchedulingManager) schedulingManager;
        testSchedulingManager.setEnabled( true );
        testSchedulingManager.setRunning( false );
    }

    @Test
    void testAnalytics()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryAnalyticsJob",
            POST( "/resourceTables/analytics" ).content( HttpStatus.OK ) );
    }

    @Test
    void testAnalytics_SecondRequestWhileRunning()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryAnalyticsJob",
            POST( "/resourceTables/analytics" ).content( HttpStatus.OK ) );

        // we fake that the first request above would internally still run (in
        // tests it never starts)
        ((TestSchedulingManager) schedulingManager).setRunning( true );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "ERROR",
            "Job of type ANALYTICS_TABLE is already running",
            POST( "/resourceTables/analytics" ).content( HttpStatus.CONFLICT ) );
        assertEquals( "FAILED", message.getResponse().getString( "jobStatus" ).string() );
    }

    @Test
    void testResourceTables()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryResourceTableJob",
            POST( "/resourceTables" ).content( HttpStatus.OK ) );
    }

    @Test
    void testMonitoring()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryMonitoringJob",
            POST( "/resourceTables/monitoring" ).content( HttpStatus.OK ) );
    }
}
