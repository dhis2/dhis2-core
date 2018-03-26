package org.hisp.dhis.scheduling;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * @author Henning Håkonsen
 */
public class SchedulingManagerTest
    extends DhisSpringTest
{
    private String CRON_EVERY_MIN = "0 * * ? * *";

    private String CRON_EVERY_SEC = "* * * ? * *";

    private JobConfiguration jobA;

    private JobConfiguration jobB;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private JobConfigurationService jobConfigurationService;

    private void verifyScheduledJobs( int expectedFutureJobs )
    {
        assertEquals( expectedFutureJobs, schedulingManager.getAllFutureJobs().size() );
    }

    private void createAndSchedule()
    {
        MockJobParameters jobConfigurationParametersA = new MockJobParameters();
        jobConfigurationParametersA.setMessage( "parameters A" );

        jobA = new JobConfiguration( "jobA", JobType.MOCK, CRON_EVERY_MIN, jobConfigurationParametersA, false, true );

        MockJobParameters jobConfigurationParametersB = new MockJobParameters();
        jobConfigurationParametersB.setMessage( "parameters B" );

        jobB = new JobConfiguration( "jobB", JobType.MOCK, CRON_EVERY_SEC, jobConfigurationParametersB, false, true );

        jobConfigurationService.addJobConfiguration( jobA );
        jobConfigurationService.addJobConfiguration( jobB );

        schedulingManager.scheduleJob( jobA );
        schedulingManager.scheduleJob( jobB );
    }

    /**
     * No assertions in this test. Tester has to verify by looking at the output in the terminal. JobA should fire at the first minute and every minute after that.
     * jobB should fire every second. (Unless sleep in actual job - or the job uses longer time than the expected delay to next execution time)
     */
    @Test
    @Ignore
    public void testScheduleJobs()
    {
        createAndSchedule();

        try
        {
            Thread.sleep( 1000000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testScheduleJobsWithUpdate()
    {
        createAndSchedule();

        verifyScheduledJobs( 2 );

        // Wait 10 seconds. jobB should fire
        try
        {
            Thread.sleep( 10000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        schedulingManager.stopJob( jobB );

        verifyScheduledJobs( 1 );

        // Wait 1 minute. Job b should stop and jobA should fire
        try
        {
            Thread.sleep( 60000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testExecuteJobs()
    {
        createAndSchedule();

        verifyScheduledJobs( 2 );

        // Wait 5 seconds and fire off jobC
        try
        {
            Thread.sleep( 5000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        JobConfiguration jobC = new JobConfiguration( "jobC", JobType.MOCK, "", new MockJobParameters(), false, true );
        schedulingManager.executeJob( jobC );

        verifyScheduledJobs( 2 );

        try
        {
            Thread.sleep( 5000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
}
