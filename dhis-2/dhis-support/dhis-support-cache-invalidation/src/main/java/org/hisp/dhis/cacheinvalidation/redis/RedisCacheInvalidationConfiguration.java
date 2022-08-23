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

import static org.hisp.dhis.common.CodeGenerator.generateUid;

import java.util.List;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionRegistryImpl;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * It configures the Redis client and the connection to the Redis server
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Order( 10002 )
@Configuration
@ComponentScan( basePackages = { "org.hisp.dhis" } )
@Profile( { "!test", "!test-h2" } )
@Conditional( value = RedisCacheInvalidationEnabledCondition.class )
public class RedisCacheInvalidationConfiguration
{
    public static final List<Class> EXCLUDE_LIST = List.of( JobConfiguration.class );

    public static final String CHANNEL_NAME = "dhis2_cache_invalidation";

    @Bean
    public static SessionRegistryImpl sessionRegistry()
    {
        return new SessionRegistryImpl();
    }

    @Bean( name = "cacheInvalidationServerId" )
    public String getCacheInvalidationServerId()
    {
        return generateUid();
    }

    @Bean
    public ConfigurationPropertyFactoryBean redisHost()
    {
        return new ConfigurationPropertyFactoryBean( ConfigurationKey.REDIS_HOST );
    }

    @Bean
    public ConfigurationPropertyFactoryBean redisPort()
    {
        return new ConfigurationPropertyFactoryBean( ConfigurationKey.REDIS_PORT );
    }

    @Bean
    public ConfigurationPropertyFactoryBean redisPassword()
    {
        return new ConfigurationPropertyFactoryBean( ConfigurationKey.REDIS_PASSWORD );
    }

    @Bean
    public ConfigurationPropertyFactoryBean redisSslEnabled()
    {
        return new ConfigurationPropertyFactoryBean( ConfigurationKey.REDIS_USE_SSL );
    }

    @Bean( destroyMethod = "shutdown" )
    ClientResources clientResources()
    {
        return DefaultClientResources.create();
    }

    @Bean( destroyMethod = "shutdown", name = "redisClient" )
    RedisClient redisClient( ClientResources clientResources )
    {
        Object hostProperty = redisHost().getObject();
        Object portProperty = redisPort().getObject();

        if ( hostProperty == null || portProperty == null )
        {
            throw new IllegalArgumentException( "Redis host/port configuration properties is not set" );
        }

        Object passwordProperty = redisPassword().getObject();
        Object sslEnabledProperty = redisSslEnabled().getObject();

        String host = (String) hostProperty;
        int port = Integer.parseInt( (String) portProperty );

        RedisURI.Builder builder;

        if ( passwordProperty != null )
        {
            builder = RedisURI.builder().withHost( host ).withPort( port )
                .withPassword( ((String) passwordProperty).toCharArray() );
            useSsl( sslEnabledProperty, builder );
        }
        else
        {
            builder = RedisURI.builder().withHost( host ).withPort( port );
            useSsl( sslEnabledProperty, builder );
        }

        return RedisClient.create( clientResources, builder.build() );
    }

    private static void useSsl( Object sslEnabledProperty, RedisURI.Builder builder )
    {
        if ( sslEnabledProperty != null )
        {
            boolean sslEnabled = Boolean.parseBoolean( (String) sslEnabledProperty );
            builder.withSsl( sslEnabled );
        }
    }

    @Bean( destroyMethod = "close", name = "redisConnection" )
    StatefulRedisConnection<String, String> connection( RedisClient redisClient )
    {
        return redisClient.connect();
    }

    @Bean( destroyMethod = "close", name = "pubSubConnection" )
    StatefulRedisPubSubConnection<String, String> pubSubConnection( RedisClient redisClient )
    {
        return redisClient.connectPubSub();
    }

    @Bean
    public RedisCacheInvalidationPreStartupRoutine redisCacheInvalidationPreStartupRoutine()
    {
        RedisCacheInvalidationPreStartupRoutine routine = new RedisCacheInvalidationPreStartupRoutine();
        routine.setName( "redisPreStartupRoutine" );
        routine.setRunlevel( 20 );
        routine.setSkipInTests( true );
        return routine;
    }

    @Bean
    public StartupRedisCacheInvalidationServiceRoutine redisCacheInvalidationServiceRoutine()
    {
        StartupRedisCacheInvalidationServiceRoutine routine = new StartupRedisCacheInvalidationServiceRoutine();
        routine.setName( "redisCacheInvalidationPreStartupRoutine" );
        routine.setRunlevel( 1 );
        routine.setSkipInTests( true );
        return routine;
    }
}
