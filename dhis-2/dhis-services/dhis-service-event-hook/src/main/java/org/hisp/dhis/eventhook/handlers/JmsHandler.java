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

import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.hisp.dhis.eventhook.Event;
import org.hisp.dhis.eventhook.EventHook;
import org.hisp.dhis.eventhook.Handler;
import org.hisp.dhis.eventhook.targets.JmsTarget;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Morten Olav Hansen
 */
@Slf4j
public class JmsHandler implements Handler
{
    private final JmsTarget target;

    private JmsTemplate jmsTemplate;

    private ActiveMQConnectionFactory connectionFactory;

    public JmsHandler( JmsTarget target )
    {
        this.target = target;
        configure( target );
    }

    private void configure( JmsTarget target )
    {
        try
        {
            connectionFactory = ActiveMQJMSClient.createConnectionFactory( target.getBrokerUrl(),
                target.getUsername() );
            connectionFactory.setPassword( target.getPassword() );
            connectionFactory.setClientID( target.getClientId() );
            connectionFactory.setGroupID( target.getGroupId() );

            connectionFactory.setConnectionTTL( 60_000 );
            connectionFactory.setCallTimeout( 30_000 );
            connectionFactory.setCallFailoverTimeout( 30_000 );
            connectionFactory.setRetryInterval( 2_000 );
            connectionFactory.setMaxRetryInterval( 2_000 );
            connectionFactory.setReconnectAttempts( 0 );

            connectionFactory.setAutoGroup( false );
            connectionFactory.setBlockOnAcknowledge( false );
            connectionFactory.setBlockOnDurableSend( true );
            connectionFactory.setBlockOnNonDurableSend( false );

            // open and close a connection to see that the configuration is correct
            connectionFactory.createConnection().close();
        }
        catch ( Exception e )
        {
            log.warn( "Could not create connection factory for JMS target: " + target.getBrokerUrl()
                + ", check and validate that your broker is up and running on the correct address" );
            return;
        }

        this.jmsTemplate = new JmsTemplate( connectionFactory );
    }

    @Override
    public void run( EventHook eventHook, Event event, String payload )
    {
        if ( jmsTemplate == null )
        {
            log.error( "Jms is not properly configured. Please check Event Hook '" + eventHook.getName()
                + "' with ID '" + eventHook.getUid() + "'" );
            return;
        }

        if ( target.isUseQueue() )
        {
            sendTo( new ActiveMQQueue( target.getAddress() ), payload );
        }
        else
        {
            sendTo( new ActiveMQTopic( target.getAddress() ), payload );
        }
    }

    private void sendTo( ActiveMQDestination destination, String payload )
    {
        try
        {
            jmsTemplate.send( destination, session -> session.createTextMessage( payload ) );
        }
        catch ( JmsException ex )
        {
            log.warn( "Could not send message to JMS target: " + target.getBrokerUrl()
                + ", check and validate that your broker is up and running on the correct address" );
        }
    }

    @Override
    public void close()
    {
        if ( connectionFactory != null )
        {
            connectionFactory.close();
        }
    }
}
