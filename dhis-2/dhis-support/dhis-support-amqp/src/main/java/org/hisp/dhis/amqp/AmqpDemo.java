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

import org.apache.qpid.jms.JmsQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class AmqpDemo
{
    private final AmqpManager amqpManager;

    public AmqpDemo( AmqpManager amqpManager )
    {
        this.amqpManager = amqpManager;
    }

    @Scheduled( fixedRate = 5_000 )
    public void produce() throws Exception
    {
        AmqpClient client = amqpManager.getClient();
        Session session = client.createSession();
        MessageProducer producer = session.createProducer( new JmsQueue( "example" ) );

        TextMessage textMessage = session.createTextMessage( "Hello World" );
        producer.send( textMessage );

        session.close();
    }

    @Scheduled( fixedRate = 3_000, initialDelay = 5_000 )
    public void listen() throws Exception
    {
        AmqpClient client = amqpManager.getClient();
        Session session = client.createSession();
        MessageConsumer consumer = session.createConsumer( new JmsQueue( "example" ) );

        TextMessage textMessage = (TextMessage) consumer.receive( 500 );
        System.err.println( "recv: " + textMessage.getText() );

        session.close();
    }
}
