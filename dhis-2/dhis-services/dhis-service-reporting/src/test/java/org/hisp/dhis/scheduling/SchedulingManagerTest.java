package org.hisp.dhis.scheduling;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.scheduling.Configuration.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Lars Helge Overland
 */
public class SchedulingManagerTest
    extends DhisSpringTest
{
    String CRON_DAILY_11PM = "0 0 23 * * ?";
    String CRON_DAILY_0AM = "0 0 0 * * ?";
    String CRON_DAILY_2AM = "0 0 2 * * ?";
    String CRON_DAILY_5AM = "0 0 5 * * ?";
    String CRON_DAILY_6AM = "0 0 6 * * ?";
    String CRON_DAILY_7AM = "0 0 7 * * ?";
    String CRON_DAILY_8AM = "0 0 8 * * ?";

    String CRON_EVERY_MIN = "0 0/1 * * * ?";
    String CRON_EVERY_15MIN = "0 0/15 * * * ?";

    String CRON_TEST = "0 * * * * ?";

    @Autowired
    private SchedulingManager schedulingManager;

    private boolean verifySortedJobs( List<Job> jobs )
    {
        for ( int i=0; i<jobs.size() - 1; i++ )
        {
            if ( jobs.get( i ).getNextExecutionTime().compareTo( jobs.get( i + 1 ).getNextExecutionTime() ) > 0 ) return false;
        }

        return true;
    }

    @Test
    public void testScheduleTasks()
    {
        JobConfiguration jobConfigurationA = new AnalyticsJobConfiguration( 1, null );
        Job jobA = new DefaultJob( "jobA", JobType.ANALYTICS, CRON_DAILY_6AM,  jobConfigurationA );

        JobConfiguration jobConfigurationB = new MessageSendJobConfiguration( null );
        Job jobB = new DefaultJob( "jobB", JobType.MESSAGE_SEND, CRON_DAILY_5AM,  jobConfigurationB);

        JobConfiguration jobConfigurationC = new PushAnalysisJobConfiguration( null, 1 );
        Job jobC = new DefaultJob( "jobC", JobType.PUSH_ANALYSIS, CRON_DAILY_2AM,  jobConfigurationC);

        JobConfiguration jobConfigurationD = new DataSyncJobConfiguration( null );
        Job jobD = new DefaultJob( "jobD", JobType.DATA_SYNC, CRON_DAILY_11PM,  jobConfigurationD);

        schedulingManager.scheduleJobs( Lists.newArrayList( jobA, jobB, jobC, jobD ) );

        List<Job> futureJobs = schedulingManager.getAllFutureJobs();

        assertEquals(4, futureJobs.size());
        assertTrue( verifySortedJobs( futureJobs ) );
    }

    /*@Test
    public void testStopTasks()
    {
        ListMap<String, String> cronKeyMap = new ListMap<>();
        cronKeyMap.putValue( CRON_DAILY_0AM, TASK_RESOURCE_TABLE );
        cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_ALL );

        assertEquals( ScheduledTaskStatus.NOT_STARTED, schedulingManager.getTaskStatus() );
        
        schedulingManager.scheduleTasks( cronKeyMap );
        
        assertEquals( ScheduledTaskStatus.RUNNING, schedulingManager.getTaskStatus() );
        
        schedulingManager.stopTasks();

        assertEquals( ScheduledTaskStatus.NOT_STARTED, schedulingManager.getTaskStatus() );
    }
    
    @Test
    public void testGetScheduledKeys()
    {
        ListMap<String, String> cronKeyMap = new ListMap<>();
        cronKeyMap.putValue( CRON_DAILY_0AM, TASK_RESOURCE_TABLE );
        cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_ALL );
        cronKeyMap.putValue( CRON_DAILY_0AM, TASK_DATAMART_LAST_YEAR );
                
        schedulingManager.scheduleTasks( cronKeyMap );
        
        Set<String> keys = schedulingManager.getScheduledKeys();
        
        assertEquals( 3, keys.size() );
        assertTrue( keys.contains( TASK_RESOURCE_TABLE ) );
        assertTrue( keys.contains( TASK_ANALYTICS_ALL ) );
        assertTrue( keys.contains( TASK_DATAMART_LAST_YEAR ) );        
    }*/
}
