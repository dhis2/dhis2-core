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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.util.StringUtils;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class InMemoryNotifier implements Notifier
{
    private NotificationMap notificationMap = new NotificationMap();

    private NotificationLoggerUtil notificationLogger;

    // -------------------------------------------------------------------------
    // Notifier implementation
    // -------------------------------------------------------------------------

    @Override
    public Notifier notify( JobConfiguration id, String message )
    {
        return notify( id, NotificationLevel.INFO, message, false );
    }

    @Override
    public Notifier notify( JobConfiguration id, NotificationLevel level, String message )
    {
        return notify( id, level, message, false );
    }

    @Override
    public Notifier notify( JobConfiguration id, NotificationLevel level, String message, boolean completed )
    {
        if ( id != null && !(level != null && level.isOff()) )
        {
            Notification notification = new Notification( level, id.getJobType(), new Date(), message, completed );

            if ( id.isInMemoryJob() && !StringUtils.isEmpty( id.getUid() ) )
            {
                notification.setUid( id.getUid() );
            }

            notificationMap.add( id, notification );

            NotificationLoggerUtil.log( log, level, message );
        }

        return this;
    }

    @Override
    public Notifier update( JobConfiguration id, String message )
    {
        return update( id, NotificationLevel.INFO, message, false );
    }

    @Override
    public Notifier update( JobConfiguration id, String message, boolean completed )
    {
        return update( id, NotificationLevel.INFO, message, completed );
    }

    @Override
    public Notifier update( JobConfiguration id, NotificationLevel level, String message )
    {
        return update( id, level, message, false );
    }

    @Override
    public Notifier update( JobConfiguration id, NotificationLevel level, String message, boolean completed )
    {
        if ( id != null && !(level != null && level.isOff()) )
        {
            notify( id, level, message, completed );
        }

        return this;
    }

    @Override
    public List<Notification> getLastNotificationsByJobType( JobType jobType, String lastId )
    {
        List<Notification> list = new ArrayList<>();

        for ( Notification notification : notificationMap.getLastNotificationsByJobType( jobType ) )
        {
            if ( lastId != null && lastId.equals( notification.getUid() ) )
            {
                if ( list.isEmpty() )
                {
                    list.add( notification );
                }
                break;
            }
            list.add( notification );
        }

        return list;
    }

    @Override
    public Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> getNotifications()
    {
        return notificationMap.getNotifications();
    }

    @Override
    public List<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        return notificationMap.getNotificationsByJobId( jobType, jobId );
    }

    @Override
    public Map<String, LinkedList<Notification>> getNotificationsByJobType( JobType jobType )
    {
        return notificationMap.getNotificationsWithType( jobType );
    }

    @Override
    public Notifier clear( JobConfiguration id )
    {
        if ( id != null )
        {
            notificationMap.clear( id );
        }

        return this;
    }

    @Override
    public Notifier addJobSummary( JobConfiguration id, Object jobSummary, Class<?> jobSummaryType )
    {
        return addJobSummary( id, NotificationLevel.INFO, jobSummary, jobSummaryType );
    }

    @Override
    public Notifier addJobSummary( JobConfiguration id, NotificationLevel level, Object jobSummary,
        Class<?> jobSummaryType )
    {
        if ( id != null && !(level != null && level.isOff()) )
        {
            notificationMap.addSummary( id, jobSummary );
        }

        return this;
    }

    @Override
    public Object getJobSummariesForJobType( JobType jobType )
    {
        return notificationMap.getJobSummariesForJobType( jobType );
    }

    @Override
    public Object getJobSummary( JobType jobType )
    {
        return notificationMap.getSummary( jobType );
    }

    @Override
    public Object getJobSummaryByJobId( JobType jobType, String jobId )
    {
        return notificationMap.getSummary( jobType, jobId );
    }
}
