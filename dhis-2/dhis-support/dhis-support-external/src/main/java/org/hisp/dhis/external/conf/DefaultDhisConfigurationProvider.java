package org.hisp.dhis.external.conf;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.crypto.Cipher;

/**
 * @author Lars Helge Overland
 */
public class DefaultDhisConfigurationProvider
    implements DhisConfigurationProvider
{
    private static final Log log = LogFactory.getLog( DefaultDhisConfigurationProvider.class );

    private static final String CONF_FILENAME = "dhis.conf";
    private static final String TEST_CONF_FILENAME = "dhis-test.conf";
    private static final String GOOGLE_AUTH_FILENAME = "dhis-google-auth.json";
    private static final String GOOGLE_EE_SCOPE = "https://www.googleapis.com/auth/earthengine";
    private static final String ENABLED_VALUE = "on";
    private static final String CACHE_PROVIDER_MEMCACHED = "memcached";

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
    
    /**
     * Cache for Google credential.
     */
    private Optional<GoogleCredential> googleCredential = Optional.empty();

    public void init()
    {
        // ---------------------------------------------------------------------
        // Load DHIS 2 configuration file into properties bundle
        // ---------------------------------------------------------------------

        if ( SystemUtils.isTestRun() )
        {
            this.properties = loadDhisTestConf();

            return; // Short-circuit here when we're setting up a test context
        }
        else
        {
            this.properties = loadDhisConf();
        }

        // ---------------------------------------------------------------------
        // Load Google JSON authentication file into properties bundle
        // ---------------------------------------------------------------------

        try ( InputStream jsonIn = locationManager.getInputStream( GOOGLE_AUTH_FILENAME ) )
        {
            Map<String, String> json = new ObjectMapper().readValue( jsonIn, new TypeReference<HashMap<String,Object>>() {} );
            
            this.properties.put( ConfigurationKey.GOOGLE_SERVICE_ACCOUNT_CLIENT_ID.getKey(), json.get( "client_id" ) );
        }
        catch ( LocationManagerException ex )
        {
            log.info( "Could not find dhis-google-auth.json" );
        }
        catch ( IOException ex )
        {
            log.warn( "Could not load credential from dhis-google-auth.json", ex );
        }

        // ---------------------------------------------------------------------
        // Load Google JSON authentication file into GoogleCredential
        // ---------------------------------------------------------------------
        
        try ( InputStream credentialIn = locationManager.getInputStream( GOOGLE_AUTH_FILENAME ) )
        {
            GoogleCredential credential = GoogleCredential
                .fromStream( credentialIn )
                .createScoped( Collections.singleton( GOOGLE_EE_SCOPE ) );
            
            this.googleCredential = Optional.of( credential );
            
            log.info( "Loaded dhis-google-auth.json authentication file" );
        }
        catch ( LocationManagerException ex )
        {
            log.info( "Could not find dhis-google-auth.json" );
        }
        catch ( IOException ex )
        {
            log.warn( "Could not load credential from dhis-google-auth.json", ex );
        }
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
    public boolean hasProperty( ConfigurationKey key )
    {        
        return StringUtils.isNotEmpty( properties.getProperty( key.getKey() ) );
    }

    @Override
    public boolean isEnabled( ConfigurationKey key )
    {
        return ENABLED_VALUE.equals( getProperty( key ) );
    }

    @Override
    public Optional<GoogleCredential> getGoogleCredential()
    {
        return googleCredential;
    }

    @Override
    public Optional<GoogleAccessToken> getGoogleAccessToken()
    {        
        if ( !getGoogleCredential().isPresent() )
        {
            return Optional.empty();
        }
        
        GoogleCredential credential = getGoogleCredential().get();
        
        try
        {
            if ( !credential.refreshToken() || credential.getExpiresInSeconds() == null )
            {
                log.warn( "There is no refresh token to be retrieved" );
                
                return Optional.empty();
            }            
        }
        catch ( IOException ex )
        {
            throw new IllegalStateException( "Could not retrieve refresh token: " + ex.getMessage(), ex );
        }
        
        GoogleAccessToken token = new GoogleAccessToken();
        
        token.setAccessToken( credential.getAccessToken() );        
        token.setClientId( getProperty( ConfigurationKey.GOOGLE_SERVICE_ACCOUNT_CLIENT_ID ) );
        token.setExpiresInSeconds( credential.getExpiresInSeconds() );
        token.setExpiresOn( LocalDateTime.now().plusSeconds( token.getExpiresInSeconds() ) );
        
        return Optional.of( token );
    }
    
    @Override
    public boolean isReadOnlyMode()
    {
        return isEnabled( ConfigurationKey.SYSTEM_READ_ONLY_MODE );
    }

    @Override
    public boolean isClusterEnabled()
    {        
        return StringUtils.isNotBlank( getProperty( ConfigurationKey.CLUSTER_INSTANCE_HOSTNAME ) );
    }

    @Override
    public boolean isMemcachedCacheProviderEnabled()
    {
        return CACHE_PROVIDER_MEMCACHED.equals( getProperty( ConfigurationKey.CACHE_PROVIDER ) );
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
    public EncryptionStatus getEncryptionStatus()
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

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Properties loadDhisConf()
        throws IllegalStateException
    {
        try ( InputStream in = locationManager.getInputStream( CONF_FILENAME ) )
        {
            Properties conf = PropertiesLoaderUtils.loadProperties( new InputStreamResource( in ) );
            substituteEnvironmentVariables( conf );

            return conf;
        }
        catch ( LocationManagerException | IOException | SecurityException ex )
        {
            log.debug( String.format( "Could not load %s", CONF_FILENAME ), ex );

            throw new IllegalStateException( "Properties could not be loaded", ex );
        }
    }

    private Properties loadDhisTestConf()
    {
        try
        {
            return PropertiesLoaderUtils.loadProperties( new ClassPathResource( TEST_CONF_FILENAME ) );
        }
        catch ( IOException ex )
        {
            log.warn( String.format( "Could not load %s from classpath", TEST_CONF_FILENAME ), ex );

            return new Properties();
        }
    }

    private void substituteEnvironmentVariables( Properties properties )
    {
        final StrSubstitutor substitutor = new StrSubstitutor( System.getenv() ); // Matches on ${...}

        properties.entrySet().forEach( entry -> entry.setValue( substitutor.replace( entry.getValue() ).trim() ) );
    }
}
