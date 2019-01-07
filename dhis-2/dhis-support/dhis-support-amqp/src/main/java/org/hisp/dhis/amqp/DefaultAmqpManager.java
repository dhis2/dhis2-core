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

import com.google.common.collect.ImmutableList;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.hisp.dhis.amqp.config.AmqpConfig;
import org.hisp.dhis.amqp.config.AmqpEmbeddedConfig;
import org.hisp.dhis.amqp.config.AmqpMode;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class DefaultAmqpManager implements AmqpManager
{
    private final Log log = LogFactory.getLog( DefaultAmqpManager.class );

    private final DhisConfigurationProvider dhisConfig;
    private final LocationManager locationManager;
    private AmqpConfig amqpConfig = null;
    private JmsConnectionFactory connectionFactory = null;
    private EmbeddedActiveMQ embeddedActiveMQ = null;

    private ImmutableList<String> topics = ImmutableList.<String>builder()
        .add( "dhis2.stats" )
        .build();

    private ImmutableList<String> queues = ImmutableList.<String>builder()
        .add( "DLO" )
        .add( "ExpiryQueue" )
        .build();

    public DefaultAmqpManager( DhisConfigurationProvider dhisConfig, LocationManager locationManager )
    {
        this.dhisConfig = dhisConfig;
        this.locationManager = locationManager;
    }

    @PostConstruct
    public void startAmqp() throws Exception
    {
        AmqpConfig amqpConfig = getAmqpConfig();

        if ( AmqpMode.EMBEDDED == amqpConfig.getMode() )
        {
            embeddedActiveMQ = createEmbeddedServer( amqpConfig );

            log.info( "Starting embedded Artemis ActiveMQ server." );
            embeddedActiveMQ.start();
        }

        AmqpClient client = getClient();

        topics.forEach( t -> {
            try
            {
                client.createTopic( t );
            }
            catch ( JMSException e )
            {
                log.info( e.getMessage() );
            }
        } );

        queues.forEach( t -> {
            try
            {
                client.createQueue( t );
            }
            catch ( JMSException e )
            {
                log.info( e.getMessage() );
            }
        } );

        client.close();
    }

    @PreDestroy
    public void stopAmqp() throws Exception
    {
        if ( embeddedActiveMQ == null )
        {
            return;
        }

        embeddedActiveMQ.stop();
        embeddedActiveMQ = null;
        connectionFactory = null;
    }

    @Override
    public AmqpClient getClient()
    {
        AmqpConfig amqpConfig = getAmqpConfig();
        Connection connection = createConnection( amqpConfig );

        if ( connection == null )
        {
            return null;
        }

        try
        {
            connection.start();
            connection.setExceptionListener( exception -> System.err.println( exception.getMessage() ) );
        }
        catch ( JMSException e )
        {
            e.printStackTrace();
        }

        return new AmqpClient( connection );
    }

    private Connection createConnection( AmqpConfig amqpConfig )
    {
        if ( connectionFactory == null )
        {
            connectionFactory = createConnectionFactory( amqpConfig );
        }

        Connection connection = null;

        try
        {
            if ( StringUtils.isEmpty( amqpConfig.getUsername() ) || StringUtils.isEmpty( amqpConfig.getPassword() ) )
            {
                connection = connectionFactory.createConnection();
            }
            else
            {
                connection = connectionFactory.createConnection( amqpConfig.getUsername(), amqpConfig.getPassword() );
            }
        }
        catch ( JMSException e )
        {
            e.printStackTrace();
        }

        return connection;
    }

    private JmsConnectionFactory createConnectionFactory( AmqpConfig amqpConfig )
    {
        connectionFactory = new JmsConnectionFactory( String.format( "amqp://%s:%d", amqpConfig.getHost(), amqpConfig.getPort() ) );
        return connectionFactory;
    }

    private EmbeddedActiveMQ createEmbeddedServer( AmqpConfig amqpConfig ) throws Exception
    {
        EmbeddedActiveMQ server = new EmbeddedActiveMQ();

        Configuration config = new ConfigurationImpl();

        config.addAcceptorConfiguration( "tcp",
            String.format( "tcp://%s:%d?protocols=AMQP", amqpConfig.getHost(), amqpConfig.getPort() ) );
        config.setSecurityEnabled( amqpConfig.getEmbedded().isSecurity() );
        config.setPersistenceEnabled( amqpConfig.getEmbedded().isPersistence() );

        if ( locationManager.externalDirectorySet() && amqpConfig.getEmbedded().isPersistence() )
        {
            String dataDir = locationManager.getExternalDirectoryPath();
            config.setJournalDirectory( dataDir + "/artemis/journal" );

            config.setJournalType( JournalType.NIO );
            config.setLargeMessagesDirectory( dataDir + "/artemis/largemessages" );
            config.setBindingsDirectory( dataDir + "/artemis/bindings" );
            config.setPagingDirectory( dataDir + "/artemis/paging" );
        }

        config.addAddressesSetting( "#",
            new AddressSettings()
                .setDeadLetterAddress( SimpleString.toSimpleString( "DLQ" ) )
                .setExpiryAddress( SimpleString.toSimpleString( "ExpiryQueue" ) ) );

        config.addAddressConfiguration(
            new CoreAddressConfiguration()
                .setName( "DLQ" )
                .addRoutingType( RoutingType.ANYCAST )
                .addQueueConfiguration(
                    new CoreQueueConfiguration()
                        .setName( "DLQ" )
                        .setRoutingType( RoutingType.ANYCAST ) ) );

        config.addAddressConfiguration(
            new CoreAddressConfiguration()
                .setName( "ExpiryQueue" )
                .addRoutingType( RoutingType.ANYCAST )
                .addQueueConfiguration(
                    new CoreQueueConfiguration()
                        .setName( "ExpiryQueue" )
                        .setRoutingType( RoutingType.ANYCAST ) ) );

        server.setConfiguration( config );

        return server;
    }

    private AmqpConfig getAmqpConfig()
    {
        if ( amqpConfig != null )
        {
            return amqpConfig;
        }

        amqpConfig = new AmqpConfig();
        amqpConfig.setMode( AmqpMode.valueOf( (dhisConfig.getProperty( ConfigurationKey.AMQP_MODE )).toUpperCase() ) );
        amqpConfig.setHost( dhisConfig.getProperty( ConfigurationKey.AMQP_HOST ) );
        amqpConfig.setPort( Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.AMQP_PORT ) ) );
        amqpConfig.setUsername( dhisConfig.getProperty( ConfigurationKey.AMQP_USERNAME ) );
        amqpConfig.setPassword( dhisConfig.getProperty( ConfigurationKey.AMQP_PASSWORD ) );

        AmqpEmbeddedConfig amqpEmbeddedConfig = new AmqpEmbeddedConfig();
        amqpEmbeddedConfig.setSecurity( Boolean.parseBoolean( dhisConfig.getProperty( ConfigurationKey.AMQP_EMBEDDED_SECURITY ) ) );
        amqpEmbeddedConfig.setPersistence( Boolean.parseBoolean( dhisConfig.getProperty( ConfigurationKey.AMQP_EMBEDDED_PERSISTENCE ) ) );

        amqpConfig.setEmbedded( amqpEmbeddedConfig );

        return amqpConfig;
    }
}
