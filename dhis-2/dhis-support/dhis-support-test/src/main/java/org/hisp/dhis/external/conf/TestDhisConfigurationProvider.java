
package org.hisp.dhis.external.conf;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestDhisConfigurationProvider
    implements DhisConfigurationProvider
{
    private static final Log log = LogFactory.getLog( TestDhisConfigurationProvider.class );

    private Properties properties;

    public TestDhisConfigurationProvider( String configurationFileName )
    {
        this.properties = getPropertiesFromFile( configurationFileName );
    }

    @Override
    public Properties getProperties()
    {
        return this.properties;
    }

    @Override
    public String getProperty( ConfigurationKey key )
    {
        return this.properties.getProperty( key.getKey(), key.getDefaultValue() );
    }

    @Override
    public String getPropertyOrDefault( ConfigurationKey key, String defaultValue )
    {
        return this.properties.getProperty( key.getKey(), defaultValue );
    }

    @Override
    public boolean hasProperty( ConfigurationKey key )
    {
        return StringUtils.isNotEmpty( this.properties.getProperty( key.getKey() ) );
    }

    @Override
    public boolean isEnabled( ConfigurationKey key )
    {
        return "on".equals( getProperty( key ) );
    }

    @Override
    public boolean isDisabled( ConfigurationKey key )
    {
        return "off".equals( getProperty( key ) );
    }

    @Override
    public Optional<GoogleCredential> getGoogleCredential()
    {
        return Optional.empty();
    }

    @Override
    public Optional<GoogleAccessToken> getGoogleAccessToken()
    {
        return Optional.empty();
    }

    @Override
    public boolean isReadOnlyMode()
    {
        return false;
    }

    @Override
    public boolean isClusterEnabled()
    {
        return false;
    }

    @Override
    public boolean isLdapConfigured()
    {
        return false;
    }

    @Override
    public EncryptionStatus getEncryptionStatus()
    {
        return EncryptionStatus.OK;
    }

    @Override
    public Map<String, Serializable> getConfigurationsAsMap()
    {
        return Stream.of( ConfigurationKey.values() )
            .collect( Collectors.toMap( ConfigurationKey::getKey, v -> v.isConfidential() ? "" :
                getPropertyOrDefault( v, v.getDefaultValue() != null ? v.getDefaultValue() : "" ) ) );
    }

    private Properties getPropertiesFromFile( String fileName )
    {
        try
        {
            return PropertiesLoaderUtils.loadProperties( new ClassPathResource( fileName ) );
        }
        catch ( IOException ex )
        {
            log.warn( String.format( "Could not load %s from classpath", fileName ), ex );
            return new Properties();
        }
    }
}
