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

/**
 * @author Lars Helge Overland
 */
public enum ConfigurationKey
{
    SYSTEM_READ_ONLY_MODE( "system.read_only_mode", "off" ),
    SYSTEM_SESSION_TIMEOUT( "system.session.timeout", "3600" ),
    SYSTEM_INTERNAL_SERVICE_API( "system.internal_service_api", "off" ),
    NODE_ID( "node.id" ),
    ENCRYPTION_PASSWORD( "encryption.password", "" ),
    CONNECTION_DIALECT( "connection.dialect" ),
    CONNECTION_DRIVER_CLASS( "connection.driver_class" ),
    CONNECTION_URL( "connection.url" ),
    CONNECTION_USERNAME( "connection.username" ),
    CONNECTION_PASSWORD( "connection.password" ),
    CONNECTION_SCHEMA( "connection.schema" ),
    CONNECTION_POOL_MAX_SIZE( "connection.pool.max_size" ),
    LDAP_URL( "ldap.url", "ldaps://0:1" ),
    LDAP_MANAGER_DN( "ldap.manager.dn" ),
    LDAP_MANAGER_PASSWORD( "ldap.manager.password" ),
    LDAP_SEARCH_BASE( "ldap.search.base", "" ),
    LDAP_SEARCH_FILTER( "ldap.search.filter", "(cn={0})" ),
    FILESTORE_PROVIDER( "filestore.provider", "filesystem" ),
    FILESTORE_CONTAINER( "filestore.container", "files" ),
    FILESTORE_LOCATION( "filestore.location" ),
    FILESTORE_IDENTITY( "filestore.identity", "" ),
    FILESTORE_SECRET( "filestore.secret", "" ),
    GOOGLE_SERVICE_ACCOUNT_CLIENT_ID( "google.service.account.client.id" ),
    META_DATA_SYNC_RETRY( "metadata.sync.retry", "3" ),
    META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC( "metadata.sync.retry.time.frequency.millisec", "30000" ),
    CLUSTER_INSTANCE_HOSTNAME( "cluster.instance0.hostname" ),
    CLUSTER_INSTANCE_CACHE_PORT( "cluster.instance0.cache.port", "4001" ),
    CLUSTER_INSTANCE_CACHE_REMOTE_OBJECT_PORT( "cluster.instance0.cache.remote.object.port", "0" ),
    CACHE_PROVIDER( "cache.provider", "ehcache" ),
    CACHE_SERVERS( "cache.servers", "localhost:11211" ),
    CACHE_TIME( "cache.time", "600" );

    private final String key;
    
    private final String defaultValue;
    
    ConfigurationKey( String key )
    {
        this.key = key;
        this.defaultValue = null;
    }

    ConfigurationKey( String key, String defaultValue )
    {
        this.key = key;
        this.defaultValue = defaultValue;
    }
    
    public String getKey()
    {
        return key;
    }
    
    public String getDefaultValue()
    {
        return defaultValue;
    }
}
