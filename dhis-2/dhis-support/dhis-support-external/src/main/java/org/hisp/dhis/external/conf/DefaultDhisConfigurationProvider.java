package org.hisp.dhis.external.conf;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;

import javax.crypto.Cipher;

/**
 * @author Lars Helge Overland
 */
public class DefaultDhisConfigurationProvider
    implements DhisConfigurationProvider
{
    private static final Log log = LogFactory.getLog( DefaultDhisConfigurationProvider.class );

    private static final String CONF_FILENAME = "dhis.conf";

    private static final String ENABLED_VALUE = "on";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private LocationManager locationManager;

    public void setLocationManager( LocationManager locationManager )
    {
        this.locationManager = locationManager;
    }

    /**
     * Cache for properties.
     */
    private Properties properties;

    public void init()
    {
        InputStream in = null;

        try
        {
            in = locationManager.getInputStream( CONF_FILENAME );
        }
        catch ( LocationManagerException ex1 )
        {
            log.debug( "Could not load dhis.conf, looking for hibernate.properties" );

            try // Deprecated
            {
                in = locationManager.getInputStream( "hibernate.properties" );
            }
            catch ( LocationManagerException ex2 )
            {
                log.debug( "Could not load hibernate.properties" );
            }
        }

        Properties properties = new Properties();

        if ( in != null )
        {
            try
            {
                properties.load( in );
            }
            catch ( IOException ex )
            {
                throw new IllegalStateException( "Properties could not be loaded", ex );
            }
        }

        this.properties = properties;
    }

    // -------------------------------------------------------------------------
    // DhisConfigurationProvider implementation
    // -------------------------------------------------------------------------

    @Override
    public Properties getProperties()
    {
        return properties;
    }

    @Override
    public String getProperty( ConfigurationKey key )
    {
        return properties.getProperty( key.getKey(), key.getDefaultValue() );
    }

    @Override
    public String getPropertyOrDefault( ConfigurationKey key, String defaultValue )
    {
        return properties.getProperty( key.getKey(), defaultValue );
    }

    @Override
    public boolean isEnabled( ConfigurationKey key )
    {
        return ENABLED_VALUE.equals( getProperty( key ) );
    }

    @Override
    public boolean isReadOnlyMode()
    {
        return ENABLED_VALUE.equals( getProperty( ConfigurationKey.SYSTEM_READ_ONLY_MODE ) );
    }

    @Override
    public boolean isLdapConfigured()
    {
        String ldapUrl = getProperty( ConfigurationKey.LDAP_URL );
        String managerDn = getProperty( ConfigurationKey.LDAP_MANAGER_DN );

        return !( ConfigurationKey.LDAP_URL.getDefaultValue().equals( ldapUrl ) ||
            ldapUrl == null || managerDn == null );
    }

    @Override
    public EncryptionStatus isEncryptionConfigured()
    {
        String password;
        
        int maxKeyLength;

        // Check for JCE files is present (key length > 128) and AES is available
        
        try
        {
            maxKeyLength = Cipher.getMaxAllowedKeyLength( "AES" );
            
            if ( maxKeyLength == 128 )
            {
                return EncryptionStatus.MISSING_JCE_POLICY;
            }
        }
        catch ( NoSuchAlgorithmException e )
        {
            return EncryptionStatus.MISSING_JCE_POLICY;
        }

        password = getProperty( ConfigurationKey.ENCRYPTION_PASSWORD );

        if ( password.length() == 0 )
        {
            return EncryptionStatus.MISSING_ENCRYPTION_PASSWORD;
        }

        if ( password.length() < 24 )
        {
            return EncryptionStatus.ENCRYPTION_PASSWORD_TOO_SHORT;
        }

        return EncryptionStatus.OK;
    }
}
