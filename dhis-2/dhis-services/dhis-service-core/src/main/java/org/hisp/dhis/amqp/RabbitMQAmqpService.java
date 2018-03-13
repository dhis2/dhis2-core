package org.hisp.dhis.amqp;

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

import com.google.common.base.CaseFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.system.RabbitMQ;
import org.hisp.dhis.system.SystemService;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class RabbitMQAmqpService implements AmqpService
{
    private static final Log log = LogFactory.getLog( RabbitMQAmqpService.class );

    private final SystemService systemService;

    private final RenderService renderService;

    private AmqpTemplate amqpTemplate;

    private Boolean enabled;

    public RabbitMQAmqpService( SystemService systemService, RenderService renderService )
    {
        this.systemService = systemService;
        this.renderService = renderService;
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
    public void publish( String routingKey, Message message )
    {
        if ( !isEnabled() )
        {
            return;
        }

        RabbitMQ rabbitMQ = systemService.getSystemInfo().getRabbitMQ();

        amqpTemplate.convertAndSend( rabbitMQ.getExchange(), routingKey, message );
    }

    @Override
    public void publish( MetadataAudit audit )
    {
        String routingKey = "metadata."
            + CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, audit.getKlass() )
            + "." + audit.getType().toString().toLowerCase()
            + "." + audit.getUid();

        String auditJson = renderService.toJsonAsString( audit );

        publish( routingKey, new Message( auditJson.getBytes(), new MessageProperties() ) );
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
            connectionFactory.setUsername( rabbitMQ.getUsername() );
            connectionFactory.setPassword( rabbitMQ.getPassword() );
            connectionFactory.setAddresses( rabbitMQ.getAddresses() );
            connectionFactory.setVirtualHost( rabbitMQ.getVirtualHost() );
            connectionFactory.setConnectionTimeout( rabbitMQ.getConnectionTimeout() );

            if ( !verifyConnection( connectionFactory ) )
            {
                log.warn( "Unable to connect to RabbitMQ message broker: " + connectionFactory );
                return null;
            }

            AmqpAdmin admin = new RabbitAdmin( connectionFactory );
            admin.declareExchange( new TopicExchange( rabbitMQ.getExchange(), true, false ) );

            amqpTemplate = new RabbitTemplate( connectionFactory );
        }

        return amqpTemplate;
    }

    private boolean verifyConnection( ConnectionFactory connectionFactory )
    {
        try
        {
            connectionFactory.createConnection().close();
        }
        catch ( Exception ignored )
        {
            return false;
        }

        return true;
    }
}
