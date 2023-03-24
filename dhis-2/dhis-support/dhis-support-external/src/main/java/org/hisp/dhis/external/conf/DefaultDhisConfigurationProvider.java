/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.external.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.external.util.LogOnceLogger;
import org.slf4j.event.Level;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

/**
 * @author Lars Helge Overland
 */
@Profile( {
    "!test-h2",
    "!test-postgres",
} )
@Component( "dhisConfigurationProvider" )
@Slf4j
public class DefaultDhisConfigurationProvider extends LogOnceLogger
    implements DhisConfigurationProvider
{
    private static final String CONF_FILENAME = "dhis.conf";

    private static final String GOOGLE_AUTH_FILENAME = "dhis-google-auth.json";

    private static final String GOOGLE_EE_SCOPE = "https://www.googleapis.com/auth/earthengine";

    private static DhisConfigurationProvider instance = null;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final LocationManager locationManager;

    public DefaultDhisConfigurationProvider( LocationManager locationManager )
    {
        checkNotNull( locationManager );
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

    @PostConstruct
    public void init()
    {
        // ---------------------------------------------------------------------
        // Load DHIS 2 configuration file into properties bundle
        // ---------------------------------------------------------------------

        this.properties = loadDhisConf();
        this.properties
            .setProperty( "connection.dialect", "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect" );

        // ---------------------------------------------------------------------
        // Load Google JSON authentication file into properties bundle
        // ---------------------------------------------------------------------

        try ( InputStream jsonIn = locationManager.getInputStream( GOOGLE_AUTH_FILENAME ) )
        {
            HashMap<String, Object> json = new ObjectMapper().readValue( jsonIn,
                new TypeReference<HashMap<String, Object>>()
                {
                } );

            this.properties.put( ConfigurationKey.GOOGLE_SERVICE_ACCOUNT_CLIENT_ID.getKey(), json.get( "client_id" ) );
        }
        catch ( LocationManagerException ex )
        {
            log( log, Level.INFO, "Could not find dhis-google-auth.json" );
        }
        catch ( IOException ex )
        {
            warn( log, "Could not load credential from dhis-google-auth.json", ex );
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

            log( log, Level.INFO, "Loaded dhis-google-auth.json authentication file" );
        }
        catch ( LocationManagerException ex )
        {
            log( log, Level.INFO, "Could not find dhis-google-auth.json" );
        }
        catch ( IOException ex )
        {
            warn( log, "Could not load credential from dhis-google-auth.json", ex );
        }

        if ( instance == null )
        {
            instance = this;
        }
    }

    public static DhisConfigurationProvider getInstance()
    {
        if ( instance != null )
        {
            return instance;
        }
        else
        {
            throw new IllegalStateException( "The DefaultDhisConfigurationProvider is not initialized yet" );
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
        return getPropertyOrDefault( key, key.getDefaultValue() );
    }

    @Override
    public String getPropertyOrDefault( ConfigurationKey key, String defaultValue )
    {
        for ( String alias : key.getAliases() )
        {
            if ( properties.contains( alias ) )
            {
                return properties.getProperty( alias );
            }
        }

        return properties.getProperty( key.getKey(), defaultValue );
    }

    @Override
    public boolean hasProperty( ConfigurationKey key )
    {
        String value = properties.getProperty( key.getKey() );

        for ( String alias : key.getAliases() )
        {
            if ( properties.contains( alias ) )
            {
                value = alias;
            }
        }

        return StringUtils.isNotEmpty( value );
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
        return StringUtils.isNotBlank( getProperty( ConfigurationKey.CLUSTER_MEMBERS ) )
            && StringUtils.isNotBlank( getProperty( ConfigurationKey.CLUSTER_HOSTNAME ) );
    }

    @Override
    public String getServerBaseUrl()
    {
        return StringUtils.trimToNull( properties.getProperty( ConfigurationKey.SERVER_BASE_URL.getKey() ) );
    }

    @Override
    public boolean isLdapConfigured()
    {
        String ldapUrl = getProperty( ConfigurationKey.LDAP_URL );
        String managerDn = getProperty( ConfigurationKey.LDAP_MANAGER_DN );

        return !(ConfigurationKey.LDAP_URL.getDefaultValue().equals( ldapUrl ) ||
            ldapUrl == null || managerDn == null);
    }

    @Override
    public EncryptionStatus getEncryptionStatus()
    {
        String password;

        int maxKeyLength;

        // Check for JCE files is present (key length > 128) and AES is
        // available

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

    @Override
    public Map<String, Serializable> getConfigurationsAsMap()
    {
        return Stream.of( ConfigurationKey.values() )
            .collect( Collectors.toMap( ConfigurationKey::getKey, v -> v.isConfidential() ? ""
                : getPropertyOrDefault( v, v.getDefaultValue() != null ? v.getDefaultValue() : "" ) ) );
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

    private void substituteEnvironmentVariables( Properties properties )
    {
        // Matches on ${...}
        final StringSubstitutor substitutor = new StringSubstitutor( System.getenv() );

        properties.entrySet().forEach( entry -> entry.setValue( substitutor.replace( entry.getValue() ).trim() ) );
    }
}
