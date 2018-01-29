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

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public interface Notifier
{
    Notifier notify( JobConfiguration id, String message );
    
    Notifier notify( JobConfiguration id, NotificationLevel level, String message );
    
    Notifier notify( JobConfiguration id, NotificationLevel level, String message, boolean completed );

    Notifier update( JobConfiguration id, String message );

    Notifier update( JobConfiguration id, NotificationLevel level, String message );

    Notifier update( JobConfiguration id, NotificationLevel level, String message, boolean completed );

    Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> getNotifications( );

    List<Notification> getLastNotificationsByJobType( JobType jobType, String lastId );

    List<Notification> getNotificationsByJobId( JobType jobType, String jobId );

    Map<String, LinkedList<Notification>> getNotificationsByJobType( JobType jobType );
    
    Notifier clear( JobConfiguration id );
    
    Notifier addJobSummary( JobConfiguration id, Object taskSummary );
    
    Notifier addJobSummary( JobConfiguration id, NotificationLevel level, Object jobSummary );

    Object getJobSummariesForJobType( JobType jobType );

    Object getJobSummary( JobType jobType );

    Object getJobSummaryByJobId( JobType jobType, String jobId );
}
