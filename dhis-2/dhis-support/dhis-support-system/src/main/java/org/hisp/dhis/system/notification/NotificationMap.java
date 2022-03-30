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
import java.util.concurrent.atomic.AtomicInteger;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * Keeps an ordered list of {@link Notification}s and/or summary {@link Object}s
 * per {@link JobType} and {@link JobConfiguration} UID.
 * <p>
 * For each {@link Pool} the capacity of entries is capped at a fixed maximum
 * {@link #capacity}.
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
    private final Map<JobType, Pool<Deque<Notification>>> notifications = new EnumMap<>( JobType.class );

    private final Map<JobType, Pool<Object>> summaries = new EnumMap<>( JobType.class );

    static final class Pool<T>
    {

        final AtomicInteger size = new AtomicInteger();

        final Deque<String> jobIdsInOrder = new ConcurrentLinkedDeque<>();

        final Map<String, T> valuesByJobId = new ConcurrentHashMap<>();

        private synchronized void remove( String jobId )
        {
            size.set( jobIdsInOrder.size() );
            jobIdsInOrder.removeIf( jobId::equals );
            valuesByJobId.remove( jobId );
        }
    }

    private final int capacity;

    NotificationMap( int capacity )
    {
        this.capacity = capacity;
        stream( JobType.values() ).forEach( jobType -> {
            notifications.put( jobType, new Pool<>() );
            summaries.put( jobType, new Pool<>() );
        } );
    }

    public Map<JobType, Map<String, Deque<Notification>>> getNotifications()
    {
        return notifications.entrySet().stream()
            .collect( toMap( Entry::getKey, e -> e.getValue().valuesByJobId ) );
    }

    public Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        Deque<Notification> res = notifications.get( jobType ).valuesByJobId.get( jobId );
        // return a defensive copy
        return res == null ? new LinkedList<>() : new LinkedList<>( res );
    }

    public Map<String, Deque<Notification>> getNotificationsWithType( JobType jobType )
    {
        return unmodifiableMap( notifications.get( jobType ).valuesByJobId );
    }

    public void add( JobConfiguration configuration, Notification notification )
    {
        String jobId = configuration.getUid();
        if ( jobId == null )
        {
            return;
        }
        Pool<Deque<Notification>> pool = notifications.get( configuration.getJobType() );
        if ( pool.size.incrementAndGet() > capacity )
        {
            String jobIdToShorten = pool.jobIdsInOrder.removeLast();
            pool.valuesByJobId.compute( jobIdToShorten, ( key, value ) -> {
                if ( value != null )
                {
                    value.removeLast();
                    if ( value.isEmpty() )
                    {
                        return null;
                    }
                }
                return value;
            } );
        }
        pool.jobIdsInOrder.addFirst( jobId );
        pool.valuesByJobId
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
        Pool<Object> pool = summaries.get( configuration.getJobType() );
        if ( pool.size.incrementAndGet() > capacity )
        {
            pool.valuesByJobId.remove( pool.jobIdsInOrder.removeLast() );
        }
        pool.jobIdsInOrder.addFirst( jobId );
        pool.valuesByJobId.put( jobId, summary );
    }

    public Object getSummary( JobType jobType, String jobId )
    {
        return summaries.get( jobType ).valuesByJobId.get( jobId );
    }

    public Map<String, Object> getJobSummariesForJobType( JobType jobType )
    {
        return unmodifiableMap( summaries.get( jobType ).valuesByJobId );
    }

    public void clear( JobConfiguration configuration )
    {
        JobType jobType = configuration.getJobType();
        String jobId = configuration.getUid();
        notifications.get( jobType ).remove( jobId );
        summaries.get( jobType ).remove( jobId );
    }
}
