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

import org.hisp.dhis.encryption.EncryptionStatus;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import java.util.Optional;
import java.util.Properties;

/**
 * Interface which provides access to the DHIS 2 configuration specified through
 * the dhis.config file.
 * 
 * @author Lars Helge Overland
 */
public interface DhisConfigurationProvider
{
    /**
     * Get configuration as a set of properties.
     * 
     * @return a Properties instance.
     */
    Properties getProperties();
    
    /**
     * Get the property value for the given key, or the default value for the
     * configuration key if not exists.
     * 
     * @param key the configuration key.
     * @return the property value.
     */
    String getProperty( ConfigurationKey key );

    /**
     * Get the property value for the given key, or the default value if not
     * exists.
     * 
     * @param key the configuration key.
     * @param defaultValue the default value.
     * @return the property value.
     */
    String getPropertyOrDefault( ConfigurationKey key, String defaultValue );
    
    /**
     * Indicates whether it exists a value which is not null or blank for the
     * given key.
     * 
     * @param key the configuration key.
     * @return true if a value exists.
     */
    boolean hasProperty( ConfigurationKey key );
    
    /**
     * Indicates whether a value for the given key is equal to "on".
     * 
     * @param key the configuration key.
     * @return true if the configuration key is enabled.
     */
    boolean isEnabled( ConfigurationKey key );
    
    /**
     * Returns a GoogleCredential, if a Google service account has been configured.
     * 
     * @return a GoogleCredential
     */
    Optional<GoogleCredential> getGoogleCredential();
    
    /**
     * Returns a GoogleAccessToken. Returns empty if no Google service account
     * has been configured, or if no refresh token could be retrieved.
     * 
     * @return a GoogleAccessToken.
     * @throws IllegalStateException if an error occurred while retrieving a token.
     */
    Optional<GoogleAccessToken> getGoogleAccessToken();
    
    /**
     * Indicates whether the system is set to read-only mode.
     * 
     * @return true if the system is in read-only mode.
     */
    public boolean isReadOnlyMode();
    
    /**
     * Indicates whether clustering is enabled.
     * 
     * @return true if clustering is enabled.
     */
    boolean isClusterEnabled();
    
    /**
     * Indicates whether {@code memcached} is enabled as cache provider.
     * 
     * @return true if {@code memcached} is enabled as cache provider.
     */
    boolean isMemcachedCacheProviderEnabled();
    
    /**
     * Indicates whether LDAP authentication is configured.
     * 
     * @return true if LDAP authentication is configured.
     */
    boolean isLdapConfigured();

    /**
     * Returns the status of the encryption setup.
     *
     * @return the EncryptionStatus.
     */
    EncryptionStatus getEncryptionStatus();
}
