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

package org.hisp.dhis.system.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
public class HibernateDatabaseInfoProvider
    implements DatabaseInfoProvider
{
    private static final String POSTGIS_MISSING_ERROR = "Postgis extension is not installed. Execute \"CREATE EXTENSION postgis;\" as a superuser and start the application again.";
    private static final Log log = LogFactory.getLog( HibernateDatabaseInfoProvider.class );
    private static final String DEL_A = "/";
    private static final String DEL_B = ":";
    private static final String DEL_C = "?";
    private static final String POSTGRES_REGEX = "^([a-zA-Z_-]+ \\d+\\.+\\d+)? .*$";

    private static final Pattern PATTERN = Pattern.compile( POSTGRES_REGEX );

    private DatabaseInfo info;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DhisConfigurationProvider config;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void init()
    {
        checkDatabaseConnectivity();

        boolean spatialSupport = isSpatialSupport();

        // Check if postgis is installed. If not, fail startup.

        if ( !spatialSupport && !SystemUtils.isTestRun() )
        {
            log.error( POSTGIS_MISSING_ERROR );
            throw new IllegalStateException( POSTGIS_MISSING_ERROR );
        }

        String url = config.getProperty( ConfigurationKey.CONNECTION_URL );
        String user = config.getProperty( ConfigurationKey.CONNECTION_USERNAME );
        String password = config.getProperty( ConfigurationKey.CONNECTION_PASSWORD );

        info = new DatabaseInfo();
        info.setName( getNameFromConnectionUrl( url ) );
        info.setUser( user );
        info.setPassword( password );
        info.setUrl( url );
        info.setSpatialSupport( spatialSupport );
        info.setDatabaseVersion( getDatabaseVersion() );
    }

    // -------------------------------------------------------------------------
    // DatabaseInfoProvider implementation
    // -------------------------------------------------------------------------

    @Override
    public DatabaseInfo getDatabaseInfo()
    {
        return info;
    }

    @Override
    public boolean isInMemory()
    {
        return info.getUrl() != null && info.getUrl().contains( ":mem:" );
    }

    @Override
    public String getNameFromConnectionUrl( String url )
    {
        String name = null;

        if ( url != null && url.lastIndexOf( DEL_B ) != -1 )
        {
            int startPos = url.lastIndexOf( DEL_A ) != -1 ? url.lastIndexOf( DEL_A ) : url.lastIndexOf( DEL_B );
            int endPos = url.lastIndexOf( DEL_C ) != -1 ? url.lastIndexOf( DEL_C ) : url.length();
            name = url.substring( startPos + 1, endPos );
        }

        return name;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Attempts to create a spatial database extension. Checks if spatial operations
     * are supported.
     */

    private String getDatabaseVersion()
    {
        try
        {
            String version = jdbcTemplate.queryForObject( "select version();", String.class );

            Matcher matcher = PATTERN.matcher( version );

            if( matcher.find() )
            {
                version = matcher.group( 1 );
            }

            return version;
        }
        catch ( Exception ex )
        {
            return "";
        }
    }

    private void checkDatabaseConnectivity()
    {
        jdbcTemplate.queryForObject( "select 'checking db connection';", String.class );
    }

    private boolean isSpatialSupport()
    {
        try
        {
            jdbcTemplate.execute( "create extension postgis;" );
        }
        catch ( Exception ex )
        {
        }

        try
        {
            String version = jdbcTemplate.queryForObject( "select postgis_full_version();", String.class );

            return version != null;
        }
        catch ( Exception ex )
        {
            log.error( "Exception when checking postgis version:", ex );
            return false;
        }
    }
}
