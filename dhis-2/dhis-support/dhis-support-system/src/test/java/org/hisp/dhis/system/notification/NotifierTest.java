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
import java.util.Map;

import static org.hisp.dhis.scheduling.JobType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Lars Helge Overland
 */
public class NotifierTest
    extends DhisSpringTest
{
    @Autowired
    private Notifier notifier;

    private User user = createUser( 'A' );
    
    private JobConfiguration id1 = new JobConfiguration( null, DATAVALUE_IMPORT, user.getUid(), true );
    private JobConfiguration id2 = new JobConfiguration( null, ANALYTICS_TABLE, user.getUid(), true );
    private JobConfiguration id3 = new JobConfiguration( null, METADATA_IMPORT, user.getUid(), true );
    
    @Test
    public void testNotifiy()
    {
        notifier.notify( id1, "Import started" );
        notifier.notify( id1, "Import working" );
        notifier.notify( id1, "Import done" );
        notifier.notify( id2, "Process started" );
        notifier.notify( id2, "Process done" );
        
        Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> notifications = notifier.getNotifications( );
        
        assertNotNull( notifications );
        assertEquals( 3, notifier.getNotificationsByJobId( id1.getJobType(), id1.getUid() ).size() );
        assertEquals( 2, notifier.getNotificationsByJobId( id2.getJobType(), id2.getUid() ).size() );
        assertEquals( 0, notifier.getNotificationsByJobId( id3.getJobType(), id3.getUid() ).size() );
    }

    @Test
    public void testClearNotifications()
    {
        notifier.clear( id1 );
        
        notifier.notify( id1, "Import started" );
        notifier.notify( id1, "Import working" );
        notifier.notify( id1, "Import done" );
        notifier.notify( id2, "Process started" );
        notifier.notify( id2, "Process done" );
        
        assertEquals( 3, notifier.getNotificationsByJobId( id1.getJobType(), id1.getUid() ).size() );
        assertEquals( 2, notifier.getNotificationsByJobId( id2.getJobType(), id2.getUid() ).size() );
        
        notifier.clear( id1 );

        assertEquals( 0, notifier.getNotificationsByJobId( id1.getJobType(), id1.getUid() ).size() );
        assertEquals( 2,  notifier.getNotificationsByJobId( id2.getJobType(), id2.getUid() ).size() );

        notifier.clear( id2 );

        assertEquals( 0, notifier.getNotificationsByJobId( id1.getJobType(), id1.getUid() ).size() );
        assertEquals( 0, notifier.getNotificationsByJobId( id2.getJobType(), id2.getUid() ).size() );
    }
    
    @Test
    public void testTaskSummary()
    {
        notifier.addJobSummary( id1, new Object() );
        
        assertNotNull( notifier.getJobSummaryByJobId( id1.getJobType(), id1.getUid() ) );
    }
}
