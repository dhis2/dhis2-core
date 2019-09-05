package org.hisp.dhis.artemis;

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

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.JmsTopic;
import org.hisp.dhis.artemis.config.ArtemisEmbeddedConfig;
import org.hisp.dhis.artemis.config.ArtemisMode;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.BeanFactoryDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@EnableJms
@Configuration
public class ArtemisConfig
{
    private final DhisConfigurationProvider dhisConfig;
    private final LocationManager locationManager;
    private final BeanFactory springContextBeanFactory;

    public ArtemisConfig(
        DhisConfigurationProvider dhisConfig,
        LocationManager locationManager,
        BeanFactory springContextBeanFactory )
    {
        this.dhisConfig = dhisConfig;
        this.locationManager = locationManager;
        this.springContextBeanFactory = springContextBeanFactory;
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory( org.hisp.dhis.artemis.config.ArtemisConfig artemisConfig )
    {
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory( String.format( "amqp://%s:%d", artemisConfig.getHost(), artemisConfig.getPort() ) );
        connectionFactory.setClientIDPrefix( "dhis2" );
        connectionFactory.setCloseLinksThatFailOnReconnect( false );
        connectionFactory.setForceAsyncAcks( true );

        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate( ConnectionFactory connectionFactory )
    {
        JmsTemplate template = new JmsTemplate( connectionFactory );
        template.setDeliveryMode( DeliveryMode.NON_PERSISTENT );

        return template;
    }

    @Bean // configured for topics
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory( ConnectionFactory connectionFactory )
    {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory( connectionFactory );
        factory.setDestinationResolver( new BeanFactoryDestinationResolver( springContextBeanFactory ) );
        factory.setConcurrency( "1" );

        return factory;
    }

    @Bean // configured for queues
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory( ConnectionFactory connectionFactory )
    {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory( connectionFactory );
        factory.setDestinationResolver( new BeanFactoryDestinationResolver( springContextBeanFactory ) );
        factory.setConcurrency( "3-10" );

        return factory;
    }

    @Bean
    public Destination metadataDestination()
    {
        return new JmsTopic( "dhis2.metadata" );
    }

    @Bean
    public Destination dlqDestination()
    {
        return new JmsQueue( "DLQ" );
    }

    @Bean
    public Destination expiryQueueDestination()
    {
        return new JmsQueue( "ExpiryQueue" );
    }

    @Bean
    public EmbeddedActiveMQ createEmbeddedServer( org.hisp.dhis.artemis.config.ArtemisConfig artemisConfig ) throws Exception
    {
        EmbeddedActiveMQ server = new EmbeddedActiveMQ();

        org.apache.activemq.artemis.core.config.Configuration config = new ConfigurationImpl();

        ArtemisEmbeddedConfig embeddedConfig = artemisConfig.getEmbedded();

        config.addAcceptorConfiguration( "tcp",
            String.format( "tcp://%s:%d?protocols=AMQP&nioRemotingThreads=%d",
                artemisConfig.getHost(),
                artemisConfig.getPort(),
                embeddedConfig.getNioRemotingThreads() ) );

        config.setSecurityEnabled( embeddedConfig.isSecurity() );
        config.setPersistenceEnabled( embeddedConfig.isPersistence() );

        if ( locationManager.externalDirectorySet() && embeddedConfig.isPersistence() )
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

    @Bean
    public org.hisp.dhis.artemis.config.ArtemisConfig getArtemisConfig()
    {
        org.hisp.dhis.artemis.config.ArtemisConfig artemisConfig = new org.hisp.dhis.artemis.config.ArtemisConfig();
        artemisConfig.setMode( ArtemisMode.valueOf( (dhisConfig.getProperty( ConfigurationKey.ARTEMIS_MODE )).toUpperCase() ) );
        artemisConfig.setHost( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_HOST ) );
        artemisConfig.setPort( Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_PORT ) ) );
        artemisConfig.setUsername( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_USERNAME ) );
        artemisConfig.setPassword( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_PASSWORD ) );

        ArtemisEmbeddedConfig artemisEmbeddedConfig = new ArtemisEmbeddedConfig();
        artemisEmbeddedConfig.setSecurity( Boolean.parseBoolean( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_EMBEDDED_SECURITY ) ) );
        artemisEmbeddedConfig.setPersistence( Boolean.parseBoolean( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_EMBEDDED_PERSISTENCE ) ) );

        artemisConfig.setEmbedded( artemisEmbeddedConfig );

        return artemisConfig;
    }
}
