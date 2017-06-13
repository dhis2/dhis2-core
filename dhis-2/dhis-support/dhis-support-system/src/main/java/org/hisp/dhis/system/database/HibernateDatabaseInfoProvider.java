package org.hisp.dhis.system.database;

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

import org.hibernate.cfg.Configuration;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class HibernateDatabaseInfoProvider
    implements DatabaseInfoProvider
{
    private static final String KEY_DIALECT = "hibernate.dialect";
    private static final String KEY_DRIVER_CLASS = "hibernate.connection.driver_class";
    private static final String KEY_URL = "hibernate.connection.url";
    private static final String KEY_USERNAME = "hibernate.connection.username";
    private static final String KEY_PASSWORD = "hibernate.connection.password";
       
    private static final String SEPARATOR = ".";
    private static final String DIALECT_SUFFIX = "Dialect";

    private static final String DEL_A = "/";
    private static final String DEL_B = ":";
    private static final String DEL_C = "?";

    private DatabaseInfo info;
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private HibernateConfigurationProvider hibernateConfigurationProvider;
    
    public void setHibernateConfigurationProvider( HibernateConfigurationProvider hibernateConfigurationProvider )
    {
        this.hibernateConfigurationProvider = hibernateConfigurationProvider;
    }

    private JdbcTemplate jdbcTemplate;
    
    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void init()
    {
        Configuration config = hibernateConfigurationProvider.getConfiguration();
        
        boolean spatialSupport = isSpatialSupport();
        
        String dialect = config.getProperty( KEY_DIALECT );
        String driverClass = config.getProperty( KEY_DRIVER_CLASS );
        String url = config.getProperty( KEY_URL );
        String user = config.getProperty( KEY_USERNAME );
        String password = config.getProperty( KEY_PASSWORD );

        info = new DatabaseInfo();
        
        if ( dialect != null && dialect.lastIndexOf( SEPARATOR ) != -1 && dialect.lastIndexOf( DIALECT_SUFFIX ) != -1 )
        {
            info.setType( dialect.substring( dialect.lastIndexOf( SEPARATOR ) + 1, dialect.lastIndexOf( DIALECT_SUFFIX ) ) );
        }
        
        if ( url != null && url.lastIndexOf( DEL_B ) != -1 )
        {
            int startPos = url.lastIndexOf( DEL_A ) != -1 ? url.lastIndexOf( DEL_A ) : url.lastIndexOf( DEL_B );            
            int endPos = url.lastIndexOf( DEL_C ) != -1 ? url.lastIndexOf( DEL_C ) : url.length();
                    
            info.setName( url.substring( startPos + 1, endPos ) );
        }
        
        info.setUser( user );
        info.setPassword( password );
        info.setDialect( dialect );
        info.setDriverClass( driverClass );
        info.setUrl( url );
        info.setSpatialSupport( spatialSupport );
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

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Attempts to create a spatial database extension. Checks if spatial operations
     * are supported.
     */
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
            return false;
        }
    }
}
