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
package org.hisp.dhis.system.notification;

import java.util.Deque;
import java.util.Map;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Lars Helge Overland
 * @author Jan Bernitt (pulled up default methods)
 */
public interface Notifier
{
    default Notifier notify( JobConfiguration id, String message )
    {
        return notify( id, NotificationLevel.INFO, message, false );
    }

    default Notifier notify( JobConfiguration id, NotificationLevel level, String message )
    {
        return notify( id, level, message, false );
    }

    Notifier notify( JobConfiguration id, NotificationLevel level, String message, boolean completed );

    default Notifier update( JobConfiguration id, String message )
    {
        return update( id, NotificationLevel.INFO, message, false );
    }

    default Notifier update( JobConfiguration id, String message, boolean completed )
    {
        return update( id, NotificationLevel.INFO, message, completed );
    }

    default Notifier update( JobConfiguration id, NotificationLevel level, String message )
    {
        return update( id, level, message, false );
    }

    default Notifier update( JobConfiguration id, NotificationLevel level, String message, boolean completed )
    {
        if ( id != null && !(level != null && level.isOff()) )
        {
            notify( id, level, message, completed );
        }
        return this;
    }

    Map<JobType, Map<String, Deque<Notification>>> getNotifications();

    Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId );

    Map<String, Deque<Notification>> getNotificationsByJobType( JobType jobType );

    Notifier clear( JobConfiguration id );

    default <T> Notifier addJobSummary( JobConfiguration id, T jobSummary, Class<T> jobSummaryType )
    {
        return addJobSummary( id, NotificationLevel.INFO, jobSummary, jobSummaryType );
    }

    <T> Notifier addJobSummary( JobConfiguration id, NotificationLevel level, T jobSummary, Class<T> jobSummaryType );

    Map<String, Object> getJobSummariesForJobType( JobType jobType );

    Object getJobSummaryByJobId( JobType jobType, String jobId );
}
