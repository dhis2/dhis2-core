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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.JmsTopic;
import org.springframework.util.Assert;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AmqpClient
{
    private final Connection connection;

    public AmqpClient( Connection connection )
    {
        Assert.notNull( connection, "connection is a required dependency of AmqpClient." );
        this.connection = connection;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public Session createSession() throws JMSException
    {
        return createSession( false );
    }

    public Session createSession( boolean transacted ) throws JMSException
    {
        return connection.createSession( transacted, Session.AUTO_ACKNOWLEDGE );
    }

    public Topic createTopic( String topic ) throws JMSException
    {
        Session session = createSession();
        Topic sessionTopic = session.createTopic( topic );
        session.close();

        return sessionTopic;
    }

    public Queue createQueue( String queue ) throws JMSException
    {
        Session session = createSession();
        Queue sessionQueue = session.createQueue( queue );
        session.close();

        return sessionQueue;
    }

    public <T> void sendQueue( String queue, T value )
    {
        send( new JmsQueue( queue ), value );
    }

    public <T> void sendTopic( String topic, T value )
    {
        send( new JmsTopic( topic ), value );
    }

    public <T> void send( Destination destination, Object value )
    {
        try
        {
            Session session = createSession();
            MessageProducer producer = session.createProducer( destination );
            String message = toJson( value );
            TextMessage textMessage = session.createTextMessage( message );
            producer.send( textMessage );
            producer.close();
            session.close();
        }
        catch ( JMSException ex )
        {
            ex.printStackTrace();
        }
    }

    public void close()
    {
        if ( connection == null )
        {
            return;
        }

        try
        {
            connection.close();
        }
        catch ( JMSException ex )
        {
            ex.printStackTrace();
        }
    }

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson( Object value )
    {
        try
        {
            return objectMapper.writeValueAsString( value );
        }
        catch ( JsonProcessingException ignored )
        {
        }

        return value.toString();
    }

    static
    {
        objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        objectMapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
        objectMapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
        objectMapper.enable( SerializationFeature.WRAP_EXCEPTIONS );

        objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        objectMapper.enable( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
        objectMapper.enable( DeserializationFeature.WRAP_EXCEPTIONS );

        objectMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        objectMapper.registerModules( new JavaTimeModule(), new Jdk8Module() );
    }
}
