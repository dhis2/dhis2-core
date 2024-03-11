package org.hisp.dhis.startup;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class ConfigurationPopulator
    extends TransactionContextStartupRoutine
{
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

    private static final Log log = LogFactory.getLog( ConfigurationPopulator.class );

    @Override
    public void executeInTransaction()
    {
        checkSecurityConfiguration();

        Configuration config = configurationService.getConfiguration();

        if ( config != null && config.getSystemId() == null )
        {
            config.setSystemId( UUID.randomUUID().toString() );
            configurationService.setConfiguration( config );
        }
    }

    private void checkSecurityConfiguration()
    {
        EncryptionStatus status = dhisConfigurationProvider.getEncryptionStatus();

        if ( !status.isOk() )
        {
            log.warn( "Encryption not configured: " + status.getKey() );
        }
        else
        {
            log.info( "Encryption is available" );
        }
    }
}
