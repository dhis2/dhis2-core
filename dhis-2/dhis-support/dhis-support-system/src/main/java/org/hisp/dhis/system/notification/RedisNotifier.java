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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ameen Mohamed
 */
public class RedisNotifier implements Notifier
{
    private static final Log log = LogFactory.getLog( RedisNotifier.class );

    private RedisTemplate<String, String> redisTemplate;

    private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:";

    private static final String LAST_NOTIFICATION_KEY_PREFIX = "last:notification:";

    private static final String SUMMARIES_KEY_PREFIX = "summaries:";

    private static final String SUMMARIES_KEY_ORDER_PREFIX = "summary:order:";

    private static final String SUMMARY_TYPE_PREFIX = "summary:type:";

    private static final String COLON = ":";

    private final static int MAX_POOL_TYPE_SIZE = 4;

    private ObjectMapper objectMapper;

    public RedisNotifier( RedisTemplate<String, String> redisTemplate )
    {
        this.redisTemplate = redisTemplate;
        objectMapper = new ObjectMapper();
        objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        objectMapper.setSerializationInclusion( Include.NON_NULL );
    }

    // -------------------------------------------------------------------------
    // Notifier implementation backed by Redis
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
            String notificationKey = generateNotificationKey( id.getJobType(), id.getUid() );
            String lastNotificationKey = generateLastNotificationKey( id.getJobType() );
            Date now = new Date();
            try
            {
                redisTemplate.boundZSetOps( notificationKey ).add( objectMapper.writeValueAsString( notification ),
                    now.getTime() );
                redisTemplate.boundZSetOps( lastNotificationKey ).add( id.getUid(), now.getTime() );

                if ( redisTemplate.boundZSetOps( notificationKey ).zCard() >= MAX_POOL_TYPE_SIZE )
                {
                    redisTemplate.boundZSetOps( notificationKey ).removeRange( 0, 0 );
                    if ( redisTemplate.boundZSetOps( notificationKey ).range( 0, 0 ).isEmpty() )
                    {
                        redisTemplate.boundZSetOps( lastNotificationKey ).removeRange( 0, 0 );
                    }
                }
            }
            catch ( JsonProcessingException e )
            {
            }

            log.info( notification );
        }
        return this;
    }

    @Override
    public Notifier update( JobConfiguration id, String message )
    {
        return update( id, NotificationLevel.INFO, message, false );
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

        Set<String> lastJobUidSet = redisTemplate.boundZSetOps( generateLastNotificationKey( jobType ) ).range( -1,
            -1 );
        if ( !lastJobUidSet.iterator().hasNext() )
        {
            return list;
        }

        String lastJobUid = (String) lastJobUidSet.iterator().next();

        for ( Notification notification : getNotificationsByJobId( jobType, lastJobUid ) )
        {
            if ( lastId != null && lastId.equals( notification.getUid() ) )
            {
                break;
            }

            list.add( notification );
        }

        return list;
    }

    @Override
    public Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> getNotifications()
    {
        Map<JobType, LinkedHashMap<String, LinkedList<Notification>>> notifications = new HashMap<>();
        for ( JobType jobType : JobType.values() )
        {
            Map<String, LinkedList<Notification>> uidNotificationMap = getNotificationsByJobType( jobType );
            notifications.put( jobType, (LinkedHashMap<String, LinkedList<Notification>>) uidNotificationMap );
        }
        return notifications;
    }

    @Override
    public List<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        List<Notification> notifications = new LinkedList<>();
        redisTemplate.boundZSetOps( generateNotificationKey( jobType, jobId ) ).range( 0, -1 ).forEach( x -> {
            try
            {
                notifications.add( objectMapper.readValue( x, Notification.class ) );
            }
            catch ( IOException e )
            {
            }
        } );
        return notifications;
    }

    @Override
    public Map<String, LinkedList<Notification>> getNotificationsByJobType( JobType jobType )
    {
        String jobTypeKeyPrefix = NOTIFICATIONS_KEY_PREFIX + jobType.toString() + COLON;
        LinkedHashMap<String, LinkedList<Notification>> uidNotificationMap = new LinkedHashMap<>();
        Set<String> jobUids = redisTemplate.keys( jobTypeKeyPrefix + "*" );
        jobUids.forEach( j -> uidNotificationMap.put( j.substring( jobTypeKeyPrefix.length() ),
            new LinkedList<>( getNotificationsByJobId( jobType, j ) ) ) );
        return uidNotificationMap;
    }

    @Override
    public Notifier clear( JobConfiguration id )
    {
        if ( id != null )
        {
            redisTemplate.delete( generateNotificationKey( id.getJobType(), id.getUid() ) );
            redisTemplate.boundHashOps( generateSummaryKey( id.getJobType() ) ).delete( id.getUid() );
            String lastNotificationKey = generateLastNotificationKey( id.getJobType() );
            Set<String> lastJobUidSet = redisTemplate.boundZSetOps( lastNotificationKey ).range( -1, -1 );
            if ( lastJobUidSet.iterator().hasNext() )
            {
                String lastJobUid = (String) lastJobUidSet.iterator().next();
                if ( lastJobUid.equals( id.getUid() ) )
                {
                    redisTemplate.boundZSetOps( lastNotificationKey ).removeRange( -1, -1 );
                }
            }
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
        if ( id != null && !(level != null && level.isOff()) && jobSummary.getClass().equals( jobSummaryType ) )
        {
            String summaryKey = generateSummaryKey( id.getJobType() );
            try
            {
                String existingSummaryTypeStr = redisTemplate.boundValueOps( generateSummaryTypeKey( id.getJobType() ) )
                    .get();
                if ( existingSummaryTypeStr == null )
                {
                    redisTemplate.boundValueOps( generateSummaryTypeKey( id.getJobType() ) )
                        .set( jobSummaryType.getName() );
                }
                else
                {
                    Class<?> existingSummaryType = Class.forName( existingSummaryTypeStr );
                    if ( !existingSummaryType.equals( jobSummaryType ) )
                    {
                        return this;
                    }
                }

                redisTemplate.boundHashOps( summaryKey ).put( id.getUid(),
                    objectMapper.writeValueAsString( jobSummary ) );
                Date now = new Date();

                String summaryOrderKey = generateSummaryOrderKey( id.getJobType() );
                redisTemplate.boundZSetOps( summaryOrderKey ).add( id.getUid(), now.getTime() );

                if ( redisTemplate.boundZSetOps( summaryOrderKey ).zCard() >= MAX_POOL_TYPE_SIZE )
                {
                    Set<String> summaryKeyToBeDeleted = redisTemplate.boundZSetOps( summaryOrderKey ).range( 0, 0 );
                    redisTemplate.boundZSetOps( summaryOrderKey ).removeRange( 0, 0 );
                    summaryKeyToBeDeleted.forEach( d -> redisTemplate.boundHashOps( summaryKey ).delete( d ) );
                }
            }
            catch ( JsonProcessingException | ClassNotFoundException e )
            {
            }
        }
        return this;
    }

    @Override
    public Object getJobSummariesForJobType( JobType jobType )
    {
        Map<String, Object> jobSummariesForType = new LinkedHashMap<>();
        try
        {
            String existingSummaryTypeStr = redisTemplate.boundValueOps( generateSummaryTypeKey( jobType ) ).get();
            if ( existingSummaryTypeStr == null )
            {
                return jobSummariesForType;
            }

            Class<?> existingSummaryType = Class.forName( existingSummaryTypeStr );
            Map<Object, Object> serializedSummaryMap = redisTemplate.boundHashOps( generateSummaryKey( jobType ) )
                .entries();

            serializedSummaryMap.forEach( ( k, v ) -> {
                try
                {
                    jobSummariesForType.put( (String) k, objectMapper.readValue( (String) v, existingSummaryType ) );
                }
                catch ( IOException e )
                {
                }
            } );
        }
        catch ( ClassNotFoundException e1 )
        {
        }

        return jobSummariesForType;
    }

    @Override
    public Object getJobSummary( JobType jobType )
    {
        Map<Object, Object> summaries = redisTemplate.boundHashOps( generateSummaryKey( jobType ) ).entries();
        Object serializedSummary;

        String existingSummaryTypeStr = redisTemplate.boundValueOps( generateSummaryTypeKey( jobType ) ).get();
        if ( existingSummaryTypeStr == null )
        {
            return null;
        }
        try
        {
            Class<?> existingSummaryType = Class.forName( existingSummaryTypeStr );

            if ( summaries.size() == 0 )
            {
                return null;
            }
            else
            {
                serializedSummary = summaries.values().toArray()[summaries.size() - 1];
            }

            return objectMapper.readValue( (String) serializedSummary, existingSummaryType );
        }
        catch ( IOException | ClassNotFoundException e )
        {
        }
        return null;
    }

    @Override
    public Object getJobSummaryByJobId( JobType jobType, String jobId )
    {
        String existingSummaryTypeStr = redisTemplate.boundValueOps( generateSummaryTypeKey( jobType ) ).get();
        if ( existingSummaryTypeStr == null )
        {
            return null;
        }
        try
        {
            Class<?> existingSummaryType = Class.forName( existingSummaryTypeStr );
            Object serializedSummary = redisTemplate.boundHashOps( generateSummaryKey( jobType ) ).get( jobId );
            if ( serializedSummary == null )
            {
                return null;
            }
            return objectMapper.readValue( (String) serializedSummary, existingSummaryType );
        }
        catch ( IOException | ClassNotFoundException e )
        {
        }
        return null;
    }

    private static String generateNotificationKey( JobType jobType, String jobUid )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( NOTIFICATIONS_KEY_PREFIX );
        builder.append( jobType.toString() );
        builder.append( COLON );
        builder.append( jobUid );
        return builder.toString();
    }

    private static String generateLastNotificationKey( JobType jobType )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( LAST_NOTIFICATION_KEY_PREFIX );
        builder.append( jobType.toString() );
        return builder.toString();
    }

    private static String generateSummaryKey( JobType jobType )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( SUMMARIES_KEY_PREFIX );
        builder.append( jobType.toString() );
        return builder.toString();
    }

    private static String generateSummaryOrderKey( JobType jobType )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( SUMMARIES_KEY_ORDER_PREFIX );
        builder.append( jobType.toString() );
        return builder.toString();
    }

    private static String generateSummaryTypeKey( JobType jobType )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( SUMMARY_TYPE_PREFIX );
        builder.append( jobType.toString() );
        return builder.toString();
    }

}
