package org.hisp.dhis.artemis.config;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class ArtemisPortChecker
{
    private final DhisConfigurationProvider dhisConfig;

    public ArtemisPortChecker( DhisConfigurationProvider dhisConfig )
    {
        this.dhisConfig = dhisConfig;
    }

    @PostConstruct
    public void init()
    {
        final int artemisPort = Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.ARTEMIS_PORT ) );
        final String artemisHost = dhisConfig.getProperty( ConfigurationKey.ARTEMIS_HOST );

        if ( isEmbedded() && !isPortAvailable( artemisHost, artemisPort ) )
        {
            String message = "\n\n";
            message += "############################################################################################\n";
            message += "#\n";
            message += String.format( "# Current selected Apache Artemis port '%s' on host '%s' is already in use.\n", artemisPort, artemisHost );
            message += "#\n";
            message += "# Please change this in your 'dhis.conf' by using the 'artemis.port = X' key.\n";
            message += "#\n";
            message += "############################################################################################\n";
            message += "\n\n";

            System.err.println( message );

            System.exit( -1 );
        }
    }

    private boolean isEmbedded()
    {
        return ArtemisMode.valueOf( (dhisConfig.getProperty( ConfigurationKey.ARTEMIS_MODE )).toUpperCase() ) == ArtemisMode.EMBEDDED;
    }

    private boolean isPortAvailable( String host, int port )
    {
        try
        {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                port, 1, InetAddress.getByName( host ) );
            serverSocket.close();
            return true;
        }
        catch ( Exception ex )
        {
            return false;
        }
    }
}
