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
package org.hisp.dhis.cacheinvalidation.redis;

import static org.hisp.dhis.cacheinvalidation.redis.RedisCacheInvalidationConfiguration.EXCLUDE_LIST;

import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.lettuce.core.api.StatefulRedisConnection;

/**
 * It listens for events from Hibernate and publishes a message to Redis when an
 * event occurs
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
@Profile( { "!test", "!test-h2" } )
@Conditional( value = RedisCacheInvalidationEnabledCondition.class )
public class PostEventCacheListener implements PostCommitUpdateEventListener, PostCommitInsertEventListener,
    PostCommitDeleteEventListener
{
    @Autowired
    @Qualifier( "cacheInvalidationServerId" )
    private String serverInstanceId;

    @Autowired
    @Qualifier( "redisConnection" )
    private StatefulRedisConnection<String, String> redisConnection;

    @Override
    public void onPostUpdate( PostUpdateEvent postUpdateEvent )
    {
        log.info( "onPostUpdate" );

        Class realClass = HibernateProxyUtils.getRealClass( postUpdateEvent.getEntity() );
        Serializable id = postUpdateEvent.getId();
        String message = serverInstanceId + ":" + "update:" + realClass.getName() + ":" + id;

        sendMessage( realClass, message );
    }

    @Override
    public void onPostInsert( PostInsertEvent postInsertEvent )
    {
        log.info( "onPostInsert" );

        Class realClass = HibernateProxyUtils.getRealClass( postInsertEvent.getEntity() );
        Serializable id = postInsertEvent.getId();
        String message = serverInstanceId + ":" + "insert:" + realClass.getName() + ":" + id;

        sendMessage( realClass, message );
    }

    @Override
    public void onPostDelete( PostDeleteEvent postDeleteEvent )
    {
        log.info( "onPostDelete" );

        Class realClass = HibernateProxyUtils.getRealClass( postDeleteEvent.getEntity() );
        Serializable id = postDeleteEvent.getId();
        String message = serverInstanceId + ":" + "delete:" + realClass.getName() + ":" + id;

        sendMessage( realClass, message );
    }

    private void sendMessage( Class realClass, String message )
    {
        if ( !EXCLUDE_LIST.contains( realClass ) )
        {
            redisConnection.async().publish( RedisCacheInvalidationConfiguration.CHANNEL_NAME, message );

            log.debug( "Published message: " + message );
        }
        else
        {
            log.debug( "Ignoring excluded class: " + realClass.getName() );
        }
    }

    @Override
    public boolean requiresPostCommitHanding( EntityPersister entityPersister )
    {
        return true;
    }

    @Override
    public boolean requiresPostCommitHandling( EntityPersister persister )
    {
        return PostCommitUpdateEventListener.super.requiresPostCommitHandling( persister );
    }

    @Override
    public void onPostUpdateCommitFailed( PostUpdateEvent event )
    {
        log.debug( "onPostUpdateCommitFailed: " + event );
    }

    @Override
    public void onPostInsertCommitFailed( PostInsertEvent event )
    {
        log.debug( "onPostInsertCommitFailed: " + event );
    }

    @Override
    public void onPostDeleteCommitFailed( PostDeleteEvent event )
    {
        log.debug( "onPostDeleteCommitFailed: " + event );
    }
}
