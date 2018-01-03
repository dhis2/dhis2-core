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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
public class NotificationMap
{
    private Map<JobType, Map<String, List<Notification>>> notificationsWithType;

    NotificationMap()
    {
        notificationsWithType = new HashMap<>();
        Arrays.stream( JobType.values() ).filter( JobType::isExecutable )
            .forEach( jobType -> notificationsWithType.put( jobType, new HashMap<>() ) );
    }

    public List<Notification> getLastNotificationsByJobType( JobType jobType )
    {
        Map<String, List<Notification>> jobTypeNotifications = notificationsWithType.get( jobType );

        String lastUid = "";
        Notification lastNotification = null;
        for (Map.Entry<String, List<Notification>> entry : jobTypeNotifications.entrySet()) {
            String uid = entry.getKey();
            List<Notification> list = entry.getValue();

            if ( lastNotification == null || list.get( list.size() - 1 ).getTime().after( lastNotification.getTime() ) )
            {
                lastUid = uid;
                lastNotification = list.get( list.size() - 1 );
            }
        }

        if ( lastUid.equals( "" ) )
        {
            return new ArrayList<>( );
        } else
        {
            return jobTypeNotifications.get( lastUid );
        }
    }

    public Map<JobType, Map<String, List<Notification>>> getNotifications( )
    {
        return notificationsWithType;
    }

    public List<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        if ( notificationsWithType.get( jobType ).containsKey( jobId ) )
        {
            return notificationsWithType.get( jobType ).get( jobId );
        }
        else
        {
            return new ArrayList<>( );
        }
    }

    public Map<String, List<Notification>> getNotificationsWithType( JobType jobType )
    {
        return notificationsWithType.get( jobType );
    }

    public void add( JobConfiguration jobConfiguration, Notification notification )
    {
        String uid = jobConfiguration.getUid();

        Map<String, List<Notification>> uidNotifications = notificationsWithType.get( jobConfiguration.getJobType() );

        List<Notification> notifications;
        if ( uidNotifications.containsKey( uid ) )
        {
            notifications = uidNotifications.get( uid );
        }
        else
        {
            notifications = new ArrayList<>( );
        }

        notifications.add( notification );

        uidNotifications.put( uid, notifications );

        notificationsWithType.put( jobConfiguration.getJobType(), uidNotifications );

    }

    public void clear( JobConfiguration jobConfiguration )
    {
        notificationsWithType.get( jobConfiguration.getJobType() ).remove( jobConfiguration.getUid() );
    }
}
