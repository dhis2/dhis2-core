/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.system.notification;

import java.util.Deque;
import java.util.Map;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Lars Helge Overland
 */
public interface Notifier
{
    Notifier notify( JobConfiguration id, String message );

    Notifier notify( JobConfiguration id, NotificationLevel level, String message );

    Notifier notify( JobConfiguration id, NotificationLevel level, String message, boolean completed );

    Notifier update( JobConfiguration id, String message );

    Notifier update( JobConfiguration id, String message, boolean completed );

    Notifier update( JobConfiguration id, NotificationLevel level, String message );

    Notifier update( JobConfiguration id, NotificationLevel level, String message, boolean completed );

    Map<JobType, Map<String, Deque<Notification>>> getNotifications();

    Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId );

    Map<String, Deque<Notification>> getNotificationsByJobType( JobType jobType );

    Notifier clear( JobConfiguration id );

    Notifier addJobSummary( JobConfiguration id, Object taskSummary, Class<?> jobSummaryType );

    Notifier addJobSummary( JobConfiguration id, NotificationLevel level, Object jobSummary, Class<?> jobSummaryType );

    Map<String, Object> getJobSummariesForJobType( JobType jobType );

    Object getJobSummaryByJobId( JobType jobType, String jobId );
}
