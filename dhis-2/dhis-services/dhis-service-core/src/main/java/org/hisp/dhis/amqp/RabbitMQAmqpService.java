package org.hisp.dhis.amqp;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hisp.dhis.system.RabbitMQ;
import org.hisp.dhis.system.SystemService;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class RabbitMQAmqpService implements AmqpService
{
    private final SystemService systemService;

    private AmqpTemplate amqpTemplate;

    private Boolean enabled;

    public RabbitMQAmqpService( SystemService systemService )
    {
        this.systemService = systemService;
    }

    @Override
    public boolean isEnabled()
    {
        if ( enabled == null )
        {
            enabled = getAmqpTemplate() != null;
        }

        return enabled;
    }

    @Override
    public void publish( String key, Message message )
    {
        if ( !isEnabled() )
        {
            return;
        }

        amqpTemplate.convertAndSend( "dhis2", key, message );
    }

    private AmqpTemplate getAmqpTemplate()
    {
        if ( amqpTemplate == null )
        {
            RabbitMQ rabbitMQ = systemService.getSystemInfo().getRabbitMQ();

            if ( rabbitMQ == null || !rabbitMQ.isValid() )
            {
                return null;
            }

            CachingConnectionFactory connectionFactory = new CachingConnectionFactory( rabbitMQ.getHost(), rabbitMQ.getPort() );
            connectionFactory.setVirtualHost( rabbitMQ.getVirtualHost() );
            connectionFactory.setUsername( rabbitMQ.getUsername() );
            connectionFactory.setPassword( rabbitMQ.getPassword() );

            AmqpAdmin admin = new RabbitAdmin( connectionFactory );
            admin.declareExchange( new TopicExchange( "dhis2", true, false ) );

            amqpTemplate = new RabbitTemplate( connectionFactory );
        }

        return amqpTemplate;
    }
}
