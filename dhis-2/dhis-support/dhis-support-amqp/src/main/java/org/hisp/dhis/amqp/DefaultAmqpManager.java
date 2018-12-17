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

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class DefaultAmqpManager implements AmqpManager
{
    private final DhisConfigurationProvider dhisConfig;
    private AmqpConfig amqpConfig;

    public DefaultAmqpManager( DhisConfigurationProvider dhisConfig )
    {
        this.dhisConfig = dhisConfig;
    }

    @PostConstruct
    public void initAmqp() throws Exception
    {
        AmqpConfig amqpConfig = getAmqpConfig();

        if ( AmqpMode.NATIVE == amqpConfig.getMode() )
        {
            return;
        }

        EmbeddedActiveMQ embeddedActiveMQ = createEmbeddedServer( amqpConfig );
        embeddedActiveMQ.start();
    }

    @Override
    public AmqpClient getClient()
    {
        return null;
    }

    private EmbeddedActiveMQ createEmbeddedServer( AmqpConfig amqpConfig ) throws Exception
    {
        EmbeddedActiveMQ server = new EmbeddedActiveMQ();

        Configuration config = new ConfigurationImpl();
        config.addAcceptorConfiguration( "tcp", "tcp://127.0.0.1:15672?protocols=AMQP" );
        config.setSecurityEnabled( false );
        config.setPersistenceEnabled( false );

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
