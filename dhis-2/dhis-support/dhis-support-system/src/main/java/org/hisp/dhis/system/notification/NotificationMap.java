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

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;

import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Henning Håkonsen
 */
public class NotificationMap
{
    public static final int MAX_POOL_TYPE_SIZE = 500;

    private final Map<JobType, Map<String, Deque<Notification>>> notificationsWithType = new EnumMap<>( JobType.class );

    private final Map<JobType, Map<String, Object>> summariesWithType = new EnumMap<>( JobType.class );

    private final Map<JobType, Deque<String>> notificationsJobIdOrder = new EnumMap<>( JobType.class );

    private final Map<JobType, Deque<String>> summariesJobIdOrder = new EnumMap<>( JobType.class );

    NotificationMap()
    {
        stream( JobType.values() ).forEach( jobType -> {
            notificationsWithType.put( jobType, new ConcurrentHashMap<>() );
            summariesWithType.put( jobType, new ConcurrentHashMap<>() );
            notificationsJobIdOrder.put( jobType, new ConcurrentLinkedDeque<>() );
            summariesJobIdOrder.put( jobType, new ConcurrentLinkedDeque<>() );
        } );
    }

    public Map<JobType, Map<String, Deque<Notification>>> getNotifications()
    {
        return unmodifiableMap( notificationsWithType );
    }

    public Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        Deque<Notification> notifications = notificationsWithType.get( jobType ).get( jobId );
        // return a defensive copy
        return notifications == null ? new LinkedList<>() : new LinkedList<>( notifications );
    }

    public Map<String, Deque<Notification>> getNotificationsWithType( JobType jobType )
    {
        return unmodifiableMap( notificationsWithType.get( jobType ) );
    }

    public void add( JobConfiguration configuration, Notification notification )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        Deque<String> notifications = notificationsJobIdOrder.get( jobType );
        if ( notifications.size() > MAX_POOL_TYPE_SIZE )
        {
            notificationsWithType.get( jobType ).remove( notifications.removeLast() );
        }
        notificationsWithType.get( jobType )
            .computeIfAbsent( jobId, key -> new ConcurrentLinkedDeque<>() )
            .addFirst( notification );
    }

    public void addSummary( JobConfiguration configuration, Object summary )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        Deque<String> summaries = summariesJobIdOrder.get( jobType );
        if ( summaries.size() >= MAX_POOL_TYPE_SIZE )
        {
            summariesWithType.get( jobType ).remove( summaries.removeLast() );
        }
        summaries.addFirst( jobId );
        summariesWithType.get( jobType ).put( jobId, summary );
    }

    public Object getSummary( JobType jobType, String jobId )
    {
        return summariesWithType.get( jobType ).get( jobId );
    }

    public Map<String, Object> getJobSummariesForJobType( JobType jobType )
    {
        return unmodifiableMap( summariesWithType.get( jobType ) );
    }

    public void clear( JobConfiguration configuration )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        notificationsWithType.get( jobType ).remove( jobId );
        notificationsJobIdOrder.get( jobType ).removeIf( e -> e.equals( jobId ) );
        summariesWithType.get( jobType ).remove( jobId );
        summariesJobIdOrder.get( jobType ).removeIf( e -> e.equals( jobId ) );
    }
}
