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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
public class NotificationMap
{
    private final static int MAX_POOL_TYPE_SIZE = 100;

    private Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> notificationsWithType;

    private Map<JobType, LinkedHashMap<String, Object>> summariesWithType;

    NotificationMap()
    {
        notificationsWithType = new HashMap<>();
        Arrays.stream( JobType.values() )
            .forEach( jobType -> notificationsWithType.put( jobType, new LinkedHashMap<>() ) );

        summariesWithType = new HashMap<>();
        Arrays.stream( JobType.values() )
            .forEach( jobType -> summariesWithType.put( jobType, new LinkedHashMap<>() ) );
    }

    public List<Notification> getLastNotificationsByJobType( JobType jobType )
    {
        LinkedHashMap<String, LinkedList<Notification>> jobTypeNotifications = notificationsWithType.get( jobType );

        if ( jobTypeNotifications.size() == 0 )
        {
            return new ArrayList<>();
        }

        String key = (String) jobTypeNotifications.keySet().toArray()[jobTypeNotifications.size() - 1];

        return jobTypeNotifications.get( key );
    }

    public Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> getNotifications()
    {
        return notificationsWithType;
    }

    public LinkedList<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        if ( notificationsWithType.get( jobType ).containsKey( jobId ) )
        {
            return notificationsWithType.get( jobType ).get( jobId );
        }
        else
        {
            return new LinkedList<>();
        }
    }

    public Map<String, LinkedList<Notification>> getNotificationsWithType( JobType jobType )
    {
        return notificationsWithType.get( jobType );
    }

    public void add( JobConfiguration jobConfiguration, Notification notification )
    {
        String uid = jobConfiguration.getUid();

        LinkedHashMap<String, LinkedList<Notification>> uidNotifications = notificationsWithType
            .get( jobConfiguration.getJobType() );

        LinkedList<Notification> notifications;
        if ( uidNotifications.containsKey( uid ) )
        {
            notifications = uidNotifications.get( uid );
        }
        else
        {
            notifications = new LinkedList<>();
        }

        notifications.addFirst( notification );

        if ( uidNotifications.size() >= MAX_POOL_TYPE_SIZE )
        {
            String key = (String) uidNotifications.keySet().toArray()[0];
            uidNotifications.remove( key );
        }

        uidNotifications.put( uid, notifications );

        notificationsWithType.put( jobConfiguration.getJobType(), uidNotifications );
    }

    public void addSummary( JobConfiguration jobConfiguration, Object summary )
    {
        LinkedHashMap<String, Object> summaries = summariesWithType.get( jobConfiguration.getJobType() );

        if ( summaries.size() >= MAX_POOL_TYPE_SIZE )
        {
            String key = (String) summaries.keySet().toArray()[0];
            summaries.remove( key );
        }

        summaries.put( jobConfiguration.getUid(), summary );
    }

    public Object getSummary( JobType jobType )
    {
        LinkedHashMap<String, Object> summariesForJobType = summariesWithType.get( jobType );

        if ( summariesForJobType.size() == 0 )
        {
            return null;
        }
        else
        {
            return summariesForJobType.values().toArray()[summariesForJobType.size() - 1];
        }
    }

    public Object getSummary( JobType jobType, String jobId )
    {
        return summariesWithType.get( jobType ).get( jobId );
    }

    public Object getJobSummariesForJobType( JobType jobType )
    {
        return summariesWithType.get( jobType );
    }

    public void clear( JobConfiguration jobConfiguration )
    {
        notificationsWithType.get( jobConfiguration.getJobType() ).remove( jobConfiguration.getUid() );
        summariesWithType.get( jobConfiguration.getJobType() ).remove( jobConfiguration.getUid() );
    }
}
