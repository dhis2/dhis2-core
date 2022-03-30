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
 * Keeps an ordered list of {@link Notification}s and/or summary {@link Object}s
 * per {@link JobType} and {@link JobConfiguration} UID.
 * <p>
 * For each {@link JobType} the capacity of entries is capped at a fixed maximum
 * {@link #capacity}.
 * <p>
 * If maximum capacity is reached and another entry is added for that
 * {@link JobType} the overall oldest entry is removed and the new one added.
 * This means entries of another {@link JobConfiguration} can be removed as long
 * as they belong to the same {@link JobType}.
 * <p>
 * The values maps contain the data that is remembered. Since the capacity and
 * order spans potentially multiple {@link JobConfiguration}s for the same
 * {@link JobType} there is an additional {@link Deque} per type to track the
 * capacity usage and to have an order, so it can be identified which
 * {@link JobConfiguration} list needs to be shortended next.
 *
 * @author Henning Håkonsen
 * @author Jan Bernitt (thread-safety)
 */
public class NotificationMap
{
    private final Map<JobType, Map<String, Deque<Notification>>> notificationValuesByType = new EnumMap<>(
        JobType.class );

    private final Map<JobType, Map<String, Object>> summaryValuesByType = new EnumMap<>( JobType.class );

    private final Map<JobType, Deque<String>> notificationJobIdsByType = new EnumMap<>( JobType.class );

    private final Map<JobType, Deque<String>> summaryJobIdsByType = new EnumMap<>( JobType.class );

    private final int capacity;

    NotificationMap( int capacity )
    {
        this.capacity = capacity;
        stream( JobType.values() ).forEach( jobType -> {
            notificationValuesByType.put( jobType, new ConcurrentHashMap<>() );
            summaryValuesByType.put( jobType, new ConcurrentHashMap<>() );
            notificationJobIdsByType.put( jobType, new ConcurrentLinkedDeque<>() );
            summaryJobIdsByType.put( jobType, new ConcurrentLinkedDeque<>() );
        } );
    }

    public Map<JobType, Map<String, Deque<Notification>>> getNotifications()
    {
        return unmodifiableMap( notificationValuesByType );
    }

    public Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        Deque<Notification> notifications = notificationValuesByType.get( jobType ).get( jobId );
        // return a defensive copy
        return notifications == null ? new LinkedList<>() : new LinkedList<>( notifications );
    }

    public Map<String, Deque<Notification>> getNotificationsWithType( JobType jobType )
    {
        return unmodifiableMap( notificationValuesByType.get( jobType ) );
    }

    public void add( JobConfiguration configuration, Notification notification )
    {
        String jobId = configuration.getUid();
        if ( jobId == null )
        {
            return;
        }
        JobType jobType = configuration.getJobType();
        Deque<String> notificationJobIds = notificationJobIdsByType.get( jobType );
        Map<String, Deque<Notification>> notificationValues = notificationValuesByType.get( jobType );
        if ( notificationJobIds.size() >= capacity )
        {
            String jobIdToShorten = notificationJobIds.removeLast();
            Deque<Notification> notifications = notificationValues.get( jobIdToShorten );
            if ( notifications != null )
            {
                notifications.removeLast();
            }
        }
        notificationJobIds.addFirst( jobId );
        notificationValues
            .computeIfAbsent( jobId, key -> new ConcurrentLinkedDeque<>() )
            .addFirst( notification );
    }

    public void addSummary( JobConfiguration configuration, Object summary )
    {
        String jobId = configuration.getUid();
        if ( jobId == null )
        {
            return;
        }
        JobType jobType = configuration.getJobType();
        Deque<String> summaryJobIds = summaryJobIdsByType.get( jobType );
        Map<String, Object> summaryValues = summaryValuesByType.get( jobType );
        if ( summaryJobIds.size() >= capacity )
        {
            summaryValues.remove( summaryJobIds.removeLast() );
        }
        summaryJobIds.addFirst( jobId );
        summaryValues.put( jobId, summary );
    }

    public Object getSummary( JobType jobType, String jobId )
    {
        return summaryValuesByType.get( jobType ).get( jobId );
    }

    public Map<String, Object> getJobSummariesForJobType( JobType jobType )
    {
        return unmodifiableMap( summaryValuesByType.get( jobType ) );
    }

    public void clear( JobConfiguration configuration )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        notificationValuesByType.get( jobType ).remove( jobId );
        notificationJobIdsByType.get( jobType ).removeIf( e -> e.equals( jobId ) );
        summaryValuesByType.get( jobType ).remove( jobId );
        summaryJobIdsByType.get( jobType ).removeIf( e -> e.equals( jobId ) );
    }
}
