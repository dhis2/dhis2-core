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
package org.hisp.dhis.eventhook.handlers;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.eventhook.Event;
import org.hisp.dhis.eventhook.EventHook;
import org.hisp.dhis.eventhook.Handler;
import org.hisp.dhis.eventhook.targets.AmqpTarget;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Morten Olav Hansen
 */
@Slf4j
public class AmqpHandler implements Handler
{
    private final AmqpTarget target;

    private RabbitTemplate rabbitTemplate;

    public AmqpHandler( AmqpTarget target )
    {
        this.target = target;
        configure( target );
    }

    private void configure( AmqpTarget target )
    {
        try
        {
            System.err.println( "HELLO" );

            RabbitConnectionFactoryBean factoryBean = new RabbitConnectionFactoryBean();
            factoryBean.setPort( 5672 );
            factoryBean.setHost( "127.0.0.1" );
            factoryBean.setUsername( "guest" );
            factoryBean.setPassword( "guest" );
            factoryBean.setVirtualHost( "/" );
            factoryBean.afterPropertiesSet();

            ConnectionFactory connectionFactory = factoryBean.getRabbitConnectionFactory();

            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory( connectionFactory );
            cachingConnectionFactory.setConnectionLimit( 3 );
            cachingConnectionFactory.afterPropertiesSet();

            this.rabbitTemplate = new RabbitTemplate( cachingConnectionFactory );
            this.rabbitTemplate.setRoutingKey( "dhis2.hooks" );
            this.rabbitTemplate.setExchange( "dhis2.hooks" );
            this.rabbitTemplate.setMandatory( false );
            this.rabbitTemplate.setReplyTimeout( 1_000 );
        }
        catch ( Exception e )
        {
            log.error( "Could not configure AMQP handler", e );
        }
    }

    @Override
    public void run( EventHook eventHook, Event event, String payload )
    {
        log.info( payload );
        rabbitTemplate.convertAndSend( payload );
    }
}
