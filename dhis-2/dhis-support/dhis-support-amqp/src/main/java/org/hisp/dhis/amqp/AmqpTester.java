package org.hisp.dhis.amqp;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.apache.qpid.jms.JmsTopic;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class AmqpTester
{
    private final JmsTemplate jmsTemplate;

    public AmqpTester( JmsTemplate jmsTemplate )
    {
        this.jmsTemplate = jmsTemplate;
    }

    @Scheduled( initialDelay = 10_000, fixedRate = 5_000 )
    public void listener() throws Exception
    {
        for ( ; ; )
        {
            TextMessage textMessage = (TextMessage) jmsTemplate.receive( new JmsTopic( "dhis2.metadata" ) );

            if ( textMessage == null )
            {
                continue;
            }

            System.err.println( "JMS: " + textMessage.getText() );
        }
    }

    @JmsListener( containerFactory = "jmsListenerContainerFactory", destination = "metadataDestination" )
    public void metadataEventListener( TextMessage message ) throws JMSException
    {
        System.err.println( "JmsListener:" + message.getText() );
    }
}
