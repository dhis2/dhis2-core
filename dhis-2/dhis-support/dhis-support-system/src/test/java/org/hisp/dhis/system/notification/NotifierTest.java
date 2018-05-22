package org.hisp.dhis.system.notification;

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
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.scheduling.JobType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Lars Helge Overland
 */
public class NotifierTest extends DhisSpringTest
{
    @Autowired
    private Notifier notifier;

    @Autowired( required = false )
    private RedisTemplate<String, ?> redisTemplate;

    private User user = createUser( 'A' );

    private JobConfiguration dataValueImportJobConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(),
        true );

    private JobConfiguration analyticsTableJobConfig = new JobConfiguration( null, ANALYTICS_TABLE, user.getUid(),
        true );

    private JobConfiguration metadataImportJobConfig = new JobConfiguration( null, METADATA_IMPORT, user.getUid(),
        true );

    private JobConfiguration dataValueImportSecondJobConfig = new JobConfiguration( null, DATAVALUE_IMPORT,
        user.getUid(), true );

    @Before
    public void flushRedis()
    {
        String NOTIFICATIONS_KEY_PREFIX = "notifications:*";
        String LAST_NOTIFICATION_KEY_PREFIX = "last:notification:*";
        String SUMMARIES_KEY_PREFIX = "summaries:*";
        String SUMMARIES_KEY_ORDER_PREFIX = "summary:*";
        if ( redisTemplate != null )
        {
            Set<String> keys = redisTemplate.keys( NOTIFICATIONS_KEY_PREFIX );
            redisTemplate.delete( keys );
            keys = redisTemplate.keys( LAST_NOTIFICATION_KEY_PREFIX );
            redisTemplate.delete( keys );
            keys = redisTemplate.keys( SUMMARIES_KEY_PREFIX );
            redisTemplate.delete( keys );
            keys = redisTemplate.keys( SUMMARIES_KEY_ORDER_PREFIX );
            redisTemplate.delete( keys );
        }
    }

    @Test
    public void testNotifiy()
    {
        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );

        Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> notifications = notifier.getNotifications();

        assertNotNull( notifications );
        assertEquals( 3,
            notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() )
                .size() );
        assertEquals( 2, notifier
            .getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );
        assertEquals( 0, notifier
            .getNotificationsByJobId( metadataImportJobConfig.getJobType(), metadataImportJobConfig.getUid() ).size() );
    }

    @Test
    public void testClearNotifications()
    {
        notifier.clear( dataValueImportJobConfig );

        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );

        assertEquals( 3,
            notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() )
                .size() );
        assertEquals( 2, notifier
            .getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );

        notifier.clear( dataValueImportJobConfig );

        assertEquals( 0,
            notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() )
                .size() );
        assertEquals( 2, notifier
            .getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );

        notifier.clear( analyticsTableJobConfig );

        assertEquals( 0,
            notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() )
                .size() );
        assertEquals( 0, notifier
            .getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );
    }

    @Test
    public void testTaskSummaryById()
    {
        notifier.addJobSummary( dataValueImportJobConfig, new String( "something" ), String.class );
        Object summary = notifier.getJobSummaryByJobId( dataValueImportJobConfig.getJobType(),
            dataValueImportJobConfig.getUid() );
        assertNotNull( summary );
        assertTrue( "True", "something".equals( (String) summary ) );
    }

    @Test
    public void testTaskSummariesByJobType()
    {
        notifier.addJobSummary( dataValueImportJobConfig, "somethingid1", String.class );
        notifier.addJobSummary( analyticsTableJobConfig, "somethingid2", String.class );
        notifier.addJobSummary( metadataImportJobConfig, "somethingid3", String.class );
        notifier.addJobSummary( dataValueImportSecondJobConfig, "somethingid4", String.class );

        Map<String, Object> jobSummariesForType = (Map<String, Object>) notifier
            .getJobSummariesForJobType( DATAVALUE_IMPORT );
        assertNotNull( jobSummariesForType );
        assertEquals( 2, jobSummariesForType.size() );
    }

    @Test
    public void testLastTaskNotification()
    {
        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import in progress" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );
        List<Notification> notifications = notifier.getLastNotificationsByJobType( DATAVALUE_IMPORT,
            dataValueImportJobConfig.getUid() );
        assertNotNull(notifications);
        assertEquals( 3, notifications.size() );
        
    }
}
