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
import static java.util.stream.Collectors.toMap;

import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * Keeps an ordered list of {@link Notification}s and/or summary {@link Object}s
 * per {@link JobType} and {@link JobConfiguration} UID.
 * <p>
 * For each {@link Pool} the capacity of entries is capped at a fixed maximum
 * capacity.
 * <p>
 * If maximum capacity is reached and another entry is added for that
 * {@link JobType}'s {@link Pool} the overall oldest entry is removed and the
 * new one added. This means entries of another {@link JobConfiguration} can be
 * removed as long as they belong to the same {@link JobType}.
 *
 * @author Henning Håkonsen
 * @author Jan Bernitt (thread-safety)
 */
public class NotificationMap
{
    private final Map<JobType, Pool<Deque<Notification>>> notificationsByJobType = new EnumMap<>( JobType.class );

    private final Map<JobType, Pool<Object>> summariesByJobType = new EnumMap<>( JobType.class );

    @RequiredArgsConstructor
    static final class Pool<T>
    {
        private final int capacity;

        /**
         * Remembers the job IDs in the order messages came in, so we know what
         * job ID has the oldest message. When {@link #capacity} is exceeded
         * that value is responsible for clearing memory.
         */
        private final Deque<String> jobIdsInOrder = new ConcurrentLinkedDeque<>();

        private final Map<String, T> valuesByJobId = new ConcurrentHashMap<>();

        synchronized void remove( String jobId )
        {
            jobIdsInOrder.removeIf( jobId::equals );
            valuesByJobId.remove( jobId );
        }

        synchronized void add( String jobId, UnaryOperator<T> limit, ToIntFunction<T> size, UnaryOperator<T> adder )
        {
            if ( jobIdsInOrder.size() >= capacity )
            {
                String jobIdToShorten = jobIdsInOrder.removeLast();
                valuesByJobId.compute( jobIdToShorten, ( key, values ) -> limit.apply( values ) );
            }
            int sizeBefore = size.applyAsInt( valuesByJobId.get( jobId ) );
            int sizeAfter = size.applyAsInt( valuesByJobId.compute( jobId, ( key, values ) -> adder.apply( values ) ) );
            if ( sizeAfter > sizeBefore )
            {
                jobIdsInOrder.addFirst( jobId );
            }
        }
    }

    NotificationMap( int capacity )
    {
        stream( JobType.values() ).forEach( jobType -> {
            notificationsByJobType.put( jobType, new Pool<>( capacity ) );
            summariesByJobType.put( jobType, new Pool<>( capacity ) );
        } );
    }

    public Map<JobType, Map<String, Deque<Notification>>> getNotifications()
    {
        return notificationsByJobType.entrySet().stream()
            .collect( toMap( Entry::getKey, e -> e.getValue().valuesByJobId ) );
    }

    public Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        Deque<Notification> res = notificationsByJobType.get( jobType ).valuesByJobId.get( jobId );
        // return a defensive copy
        return res == null ? new LinkedList<>() : new LinkedList<>( res );
    }

    public Map<String, Deque<Notification>> getNotificationsWithType( JobType jobType )
    {
        return unmodifiableMap( notificationsByJobType.get( jobType ).valuesByJobId );
    }

    public void add( JobConfiguration configuration, Notification notification )
    {
        String jobId = configuration.getUid();
        if ( jobId == null )
        {
            return;
        }
        notificationsByJobType.get( configuration.getJobType() ).add( jobId,
            NotificationMap::withLimit,
            notifications -> notifications == null ? 0 : notifications.size(),
            notifications -> withAdded( notifications, notification ) );
    }

    public void addSummary( JobConfiguration configuration, Object summary )
    {
        String jobId = configuration.getUid();
        if ( jobId == null )
        {
            return;
        }
        summariesByJobType.get( configuration.getJobType() ).add( jobId,
            currentSummary -> null,
            currentSummary -> currentSummary == null ? 0 : 1,
            currentSummary -> summary );
    }

    public Object getSummary( JobType jobType, String jobId )
    {
        return summariesByJobType.get( jobType ).valuesByJobId.get( jobId );
    }

    public Map<String, Object> getJobSummariesForJobType( JobType jobType )
    {
        return unmodifiableMap( summariesByJobType.get( jobType ).valuesByJobId );
    }

    public void clear( JobConfiguration configuration )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        notificationsByJobType.get( jobType ).remove( jobId );
        summariesByJobType.get( jobType ).remove( jobId );
    }

    private static Deque<Notification> withAdded( Deque<Notification> notifications, Notification item )
    {
        if ( notifications == null )
        {
            notifications = new ConcurrentLinkedDeque<>();
        }
        Notification mostRecent = notifications.peekFirst();
        if ( mostRecent != null && mostRecent.getLevel() == NotificationLevel.LOOP )
        {
            notifications.pollFirst();
        }
        notifications.addFirst( item );
        return notifications;
    }

    private static Deque<Notification> withLimit( Deque<Notification> notifications )
    {
        if ( notifications != null )
        {
            Notification mostRecent = notifications.peekFirst();
            if ( mostRecent != null && mostRecent.getLevel() == NotificationLevel.LOOP )
            {
                notifications.pollFirst();
            }
            else
            {
                notifications.pollLast();
            }
            if ( notifications.isEmpty() )
            {
                return null;
            }
        }
        return notifications;
    }
}
