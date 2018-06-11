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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private User user = createUser( 'A' );

    private JobConfiguration dataValueImportJobConfig;

    private JobConfiguration analyticsTableJobConfig;

    private JobConfiguration metadataImportJobConfig;

    private JobConfiguration dataValueImportSecondJobConfig;

    private JobConfiguration dataValueImportThirdJobConfig;

    private JobConfiguration dataValueImportFourthConfig;

    private JobConfiguration dataValueImportFifthConfig;

    public NotifierTest()
    {
        dataValueImportJobConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), false );
        dataValueImportJobConfig.setUid( "dvi1" );
        analyticsTableJobConfig = new JobConfiguration( null, ANALYTICS_TABLE, user.getUid(), false );
        analyticsTableJobConfig.setUid( "at1" );
        metadataImportJobConfig = new JobConfiguration( null, METADATA_IMPORT, user.getUid(), false );
        metadataImportJobConfig.setUid( "mdi1" );
        dataValueImportSecondJobConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), false );
        dataValueImportSecondJobConfig.setUid( "dvi2" );
        dataValueImportThirdJobConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), false );
        dataValueImportThirdJobConfig.setUid( "dvi3" );
        dataValueImportFourthConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), false );
        dataValueImportFourthConfig.setUid( "dvi4" );
        dataValueImportFifthConfig = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), false );
        dataValueImportFifthConfig.setUid( "dvi5" );
    }

    @Test
    public void testGetNotifications()
    {
        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );

        Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> notificationsMap = notifier.getNotifications();

        assertNotNull( notificationsMap );
        assertEquals( 3, notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() ).size() );
        assertEquals( 2, notifier.getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );
        assertEquals( 0, notifier.getNotificationsByJobId( metadataImportJobConfig.getJobType(), metadataImportJobConfig.getUid() ).size() );

        notifier.clear( dataValueImportJobConfig );
        notifier.clear( analyticsTableJobConfig );

        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );

        assertEquals( 3, notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() ).size() );
        assertEquals( 2, notifier.getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );

        notifier.clear( dataValueImportJobConfig );

        assertEquals( 0, notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() ).size() );
        assertEquals( 2, notifier.getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );

        notifier.clear( analyticsTableJobConfig );

        assertEquals( 0, notifier.getNotificationsByJobId( dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid() ).size() );
        assertEquals( 0, notifier.getNotificationsByJobId( analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid() ).size() );

        notifier.notify( dataValueImportSecondJobConfig, "Process done" );
        notifier.notify( dataValueImportJobConfig, "Import started" );
        notifier.notify( dataValueImportJobConfig, "Import working" );
        notifier.notify( dataValueImportJobConfig, "Import in progress" );
        notifier.notify( dataValueImportJobConfig, "Import done" );
        notifier.notify( analyticsTableJobConfig, "Process started" );
        notifier.notify( analyticsTableJobConfig, "Process done" );
        List<Notification> notifications = notifier.getLastNotificationsByJobType( DATAVALUE_IMPORT,
            dataValueImportJobConfig.getUid() );
        assertNotNull( notifications );
        assertEquals( 4, notifications.size() );

        notifier.notify( dataValueImportThirdJobConfig, "Completed1" );

        notifications = notifier.getLastNotificationsByJobType( DATAVALUE_IMPORT, null );
        assertNotNull( notifications );
        assertEquals( 1, notifications.size() );
        assertTrue( "Completed1".equals( notifications.get( 0 ).getMessage() ) );

        assertEquals( 3, notifier.getNotifications().get( DATAVALUE_IMPORT ).size() );

        notifier.notify( dataValueImportFourthConfig, "Completed2" );

        notifications = notifier.getLastNotificationsByJobType( DATAVALUE_IMPORT, null );
        assertNotNull( notifications );
        assertEquals( 1, notifications.size() );
        assertTrue( "Completed2".equals( notifications.get( 0 ).getMessage() ) );
        assertEquals( 4, notifier.getNotifications().get( DATAVALUE_IMPORT ).size() );

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetSummary()
    {
        notifier.addJobSummary( dataValueImportJobConfig, "somethingid1", String.class );
        notifier.addJobSummary( analyticsTableJobConfig, "somethingid2", String.class );
        notifier.addJobSummary( dataValueImportSecondJobConfig, "somethingid4", String.class );
        notifier.addJobSummary( metadataImportJobConfig, "somethingid3", String.class );

        Map<String, Object> jobSummariesForAnalyticsType = (Map<String, Object>) notifier
            .getJobSummariesForJobType( DATAVALUE_IMPORT );
        assertNotNull( jobSummariesForAnalyticsType );
        assertEquals( 2, jobSummariesForAnalyticsType.size() );

        Map<String, Object> jobSummariesForMetadataImportType = (Map<String, Object>) notifier
            .getJobSummariesForJobType( METADATA_IMPORT );
        assertNotNull( jobSummariesForMetadataImportType );
        assertEquals( 1, jobSummariesForMetadataImportType.size() );
        assertTrue( "somethingid3"
            .equals( (String) jobSummariesForMetadataImportType.get( metadataImportJobConfig.getUid() ) ) );

        Object summary = notifier.getJobSummaryByJobId( dataValueImportJobConfig.getJobType(),
            dataValueImportJobConfig.getUid() );
        assertNotNull( summary );
        assertTrue( "True", "somethingid1".equals( (String) summary ) );

        assertTrue( "somethingid4".equals( (String) notifier.getJobSummary( DATAVALUE_IMPORT ) ) );

        notifier.addJobSummary( dataValueImportThirdJobConfig, "summarry3", String.class );

        jobSummariesForAnalyticsType = (Map<String, Object>) notifier.getJobSummariesForJobType( DATAVALUE_IMPORT );
        assertNotNull( jobSummariesForAnalyticsType );
        assertEquals( 3, jobSummariesForAnalyticsType.size() );

        notifier.addJobSummary( dataValueImportFourthConfig, "summarry4", String.class );

        jobSummariesForAnalyticsType = (Map<String, Object>) notifier.getJobSummariesForJobType( DATAVALUE_IMPORT );
        assertNotNull( jobSummariesForAnalyticsType );
        assertEquals( 4, jobSummariesForAnalyticsType.size() );
    }

}
