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

/**
 * @author Lars Helge Overland
 */
public enum ConfigurationKey
{
    SYSTEM_READ_ONLY_MODE( "system.read_only_mode", "off" ),
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
    FILE_STORE_CONTAINER( "filestore.container", "files" ),
    FILE_STORE_LOCATION( "filestore.location" ),
    FILE_STORE_IDENTITY( "filestore.identity", "" ),
    FILE_STORE_SECRET( "filestore.secret", "" );

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
