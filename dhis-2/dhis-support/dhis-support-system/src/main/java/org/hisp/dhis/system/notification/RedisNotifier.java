/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

<<<<<<< HEAD
=======
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
/**
 * Notifier implementation backed by redis. It holds 2 types of data.
 * Notifications and Summaries. Since order of the Notifications and Summaries
 * are important, (to limit the maximum number of objects held), we use a
 * combination of "Sorted Sets" , "HashMaps" and "Values" (data structures in
 * redis) to have a similar behaviour as InMemoryNotifier.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class RedisNotifier implements Notifier
{
    private static final String NOTIFIER_ERROR = "Redis Notifier error:%s";

    private RedisTemplate<String, String> redisTemplate;

    private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:";

    private static final String NOTIFICATION_ORDER_KEY_PREFIX = "notification:order:";

    private static final String SUMMARIES_KEY_PREFIX = "summaries:";

    private static final String SUMMARIES_KEY_ORDER_PREFIX = "summary:order:";

    private static final String SUMMARY_TYPE_PREFIX = "summary:type:";

    private static final String COLON = ":";

    private final static int MAX_POOL_TYPE_SIZE = 100;

    private final ObjectMapper jsonMapper;

    public RedisNotifier(
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper jsonMapper )
    {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
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
    public Notifier update( JobConfiguration id, String message, boolean completed )
    {
        return notify( id, NotificationLevel.INFO, message, completed );
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

            if ( id.isInMemoryJob() && StringUtils.isEmpty( id.getUid() ) )
            {
                notification.setUid( id.getUid() );
            }

            String notificationKey = generateNotificationKey( id.getJobType(), id.getUid() );
            String notificationOrderKey = generateNotificationOrderKey( id.getJobType() );

            Date now = new Date();

            try
            {
                if ( redisTemplate.boundZSetOps( notificationOrderKey ).zCard() >= MAX_POOL_TYPE_SIZE )
                {
                    Set<String> deleteKeys = redisTemplate.boundZSetOps( notificationOrderKey ).range( 0, 0 );
                    redisTemplate.delete( deleteKeys );
                    redisTemplate.boundZSetOps( notificationOrderKey ).removeRange( 0, 0 );
                }

                redisTemplate.boundZSetOps( notificationKey ).add( jsonMapper.writeValueAsString( notification ),
                    now.getTime() );
                redisTemplate.boundZSetOps( notificationOrderKey ).add( id.getUid(), now.getTime() );
            }
            catch ( JsonProcessingException ex )
            {
                log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
            }

            log.info( notification.toString() );
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

        Set<String> lastJobUidSet = redisTemplate.boundZSetOps( generateNotificationOrderKey( jobType ) ).range( -1,
            -1 );
        if ( !lastJobUidSet.iterator().hasNext() )
        {
            return list;
        }

        String lastJobUid = lastJobUidSet.iterator().next();

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
                notifications.add( jsonMapper.readValue( x, Notification.class ) );
            }
            catch ( IOException ex )
            {
                log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
            }
        } );
        return notifications;
    }

    @Override
    public Map<String, LinkedList<Notification>> getNotificationsByJobType( JobType jobType )
    {
        Set<String> notificationKeys = redisTemplate.boundZSetOps( generateNotificationOrderKey( jobType ) ).range( 0, -1 );
        LinkedHashMap<String, LinkedList<Notification>> uidNotificationMap = new LinkedHashMap<>();
        notificationKeys
            .forEach( j -> uidNotificationMap.put( j, new LinkedList<>( getNotificationsByJobId( jobType, j ) ) ) );

        return uidNotificationMap;
    }

    @Override
    public Notifier clear( JobConfiguration id )
    {
        if ( id != null )
        {
            redisTemplate.delete( generateNotificationKey( id.getJobType(), id.getUid() ) );
            redisTemplate.boundHashOps( generateSummaryKey( id.getJobType() ) ).delete( id.getUid() );
            redisTemplate.boundZSetOps( generateNotificationOrderKey( id.getJobType() ) ).remove( id.getUid() );
            redisTemplate.boundZSetOps( generateSummaryOrderKey( id.getJobType() ) ).remove( id.getUid() );
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

                String summaryOrderKey = generateSummaryOrderKey( id.getJobType() );
                if ( redisTemplate.boundZSetOps( summaryOrderKey ).zCard() >= MAX_POOL_TYPE_SIZE )
                {
                    Set<String> summaryKeyToBeDeleted = redisTemplate.boundZSetOps( summaryOrderKey ).range( 0, 0 );
                    redisTemplate.boundZSetOps( summaryOrderKey ).removeRange( 0, 0 );
                    summaryKeyToBeDeleted.forEach( d -> redisTemplate.boundHashOps( summaryKey ).delete( d ) );
                }
                redisTemplate.boundHashOps( summaryKey ).put( id.getUid(),
                    jsonMapper.writeValueAsString( jobSummary ) );
                Date now = new Date();

                redisTemplate.boundZSetOps( summaryOrderKey ).add( id.getUid(), now.getTime() );

            }
            catch ( JsonProcessingException | ClassNotFoundException ex )
            {
                log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
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
                    jobSummariesForType.put( (String) k, jsonMapper.readValue( (String) v, existingSummaryType ) );
                }
                catch ( IOException e )
                {
                    log.warn( String.format( NOTIFIER_ERROR, e.getMessage() ) );
                }
            } );
        }
        catch ( ClassNotFoundException ex )
        {
            log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
        }

        return jobSummariesForType;
    }

    @Override
    public Object getJobSummary( JobType jobType )
    {
        String existingSummaryTypeStr = redisTemplate.boundValueOps( generateSummaryTypeKey( jobType ) ).get();
        if ( existingSummaryTypeStr == null )
        {
            return null;
        }

        try
        {
            Class<?> existingSummaryType = Class.forName( existingSummaryTypeStr );

            Set<String> lastJobUidSet = redisTemplate.boundZSetOps( generateSummaryOrderKey( jobType ) ).range( -1,
                -1 );
            if ( !lastJobUidSet.iterator().hasNext() )
            {
                return null;
            }

            String lastJobUid = (String) lastJobUidSet.iterator().next();
            Object serializedSummary = redisTemplate.boundHashOps( generateSummaryKey( jobType ) ).get( lastJobUid );

<<<<<<< HEAD
            return serializedSummary != null ? jsonMapper.readValue( (String) serializedSummary, existingSummaryType ) : null;
=======
            return serializedSummary != null ? jsonMapper.readValue( (String) serializedSummary, existingSummaryType )
                : null;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        }
        catch ( IOException | ClassNotFoundException ex )
        {
            log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
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

<<<<<<< HEAD
            return serializedSummary != null ? jsonMapper.readValue( (String) serializedSummary, existingSummaryType ) : null;
=======
            return serializedSummary != null ? jsonMapper.readValue( (String) serializedSummary, existingSummaryType )
                : null;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        }
        catch ( IOException | ClassNotFoundException ex )
        {
            log.warn( String.format( NOTIFIER_ERROR, ex.getMessage() ) );
        }
        return null;
    }

    private static String generateNotificationKey( JobType jobType, String jobUid )
    {
        return new StringBuilder()
            .append( NOTIFICATIONS_KEY_PREFIX )
            .append( jobType.toString() )
            .append( COLON )
            .append( jobUid ).toString();
    }

    private static String generateNotificationOrderKey( JobType jobType )
    {
        return new StringBuilder()
            .append( NOTIFICATION_ORDER_KEY_PREFIX )
            .append( jobType.toString() ).toString();
    }

    private static String generateSummaryKey( JobType jobType )
    {
        return new StringBuilder()
            .append( SUMMARIES_KEY_PREFIX )
            .append( jobType.toString() ).toString();
    }

    private static String generateSummaryOrderKey( JobType jobType )
    {
        return new StringBuilder()
            .append( SUMMARIES_KEY_ORDER_PREFIX )
            .append( jobType.toString() ).toString();
    }

    private static String generateSummaryTypeKey( JobType jobType )
    {
        return new StringBuilder()
            .append( SUMMARY_TYPE_PREFIX )
            .append( jobType.toString() ).toString();
    }
}
