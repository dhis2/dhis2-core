package org.hisp.dhis.hibernate;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

/**
 * @author Torgeir Lorange Ostby
 */
public class DefaultHibernateConfigurationProvider
    implements HibernateConfigurationProvider
{
    private static final Log log = LogFactory.getLog( DefaultHibernateConfigurationProvider.class );

    private Configuration configuration = null;

    private static final String MAPPING_RESOURCES_ROOT = "org/hisp/dhis/";

    // -------------------------------------------------------------------------
    // Property resources
    // -------------------------------------------------------------------------

    private String defaultPropertiesFile = "hibernate-default.properties";
    private String regularPropertiesFile = "hibernate.properties";
    private String testPropertiesFile = "hibernate-test.properties";
    
    private List<Resource> jarResources = new ArrayList<>();
    private List<Resource> dirResources = new ArrayList<>();
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private LocationManager locationManager;

    public void setLocationManager( LocationManager locationManager )
    {
        this.locationManager = locationManager;
    }

    // -------------------------------------------------------------------------
    // Initialise
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init()
        throws Exception
    {
        Configuration configuration = new Configuration();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // ---------------------------------------------------------------------
        // Add mapping resources
        // ---------------------------------------------------------------------

        Enumeration<URL> resources = classLoader.getResources( MAPPING_RESOURCES_ROOT );

        while ( resources.hasMoreElements() )
        {
            URL resource = resources.nextElement();

            if ( ResourceUtils.isJarURL( resource ) )
            {
                URL jarFile = ResourceUtils.extractJarFileURL( resource );

                File file = ResourceUtils.getFile( jarFile );
                
                jarResources.add( new FileSystemResource( file.getAbsolutePath() ) );
                
                log.debug( "Adding jar in which to search for hbm.xml files: " + file.getAbsolutePath() );

                configuration.addJar( file );
            }
            else
            {
                File file = ResourceUtils.getFile( resource );

                dirResources.add( new FileSystemResource( file ) );
                
                log.debug( "Adding directory in which to search for hbm.xml files: " + file.getAbsolutePath() );
                
                configuration.addDirectory( file );
            }
        }

        // ---------------------------------------------------------------------
        // Add default properties
        // ---------------------------------------------------------------------

        Properties defaultProperties = getProperties( defaultPropertiesFile );

        configuration.addProperties( defaultProperties );

        // ---------------------------------------------------------------------
        // Choose which properties file to look for
        // ---------------------------------------------------------------------

        boolean testing = "true".equals( System.getProperty( "org.hisp.dhis.test", "false" ) );

        String propertiesFile = testing ? testPropertiesFile : regularPropertiesFile;

        // ---------------------------------------------------------------------
        // Add custom properties from classpath
        // ---------------------------------------------------------------------

        Properties customProperties = getProperties( propertiesFile );

        if ( customProperties != null )
        {
            configuration.addProperties( customProperties );
        }

        // ---------------------------------------------------------------------
        // Add custom properties from file system
        // ---------------------------------------------------------------------
        
        try
        {
            configuration.addProperties( getProperties( locationManager.getInputStream( propertiesFile ) ) );   
        }
        catch ( LocationManagerException ex )
        {
            log.info( "Could not read external configuration from file system" );
        }

        // ---------------------------------------------------------------------
        // Disable second-level cache during testing
        // ---------------------------------------------------------------------
        
        if ( testing )
        {
            configuration.setProperty( "hibernate.cache.use_second_level_cache", "false" );
            configuration.setProperty( "hibernate.cache.use_query_cache", "false" );
        }
        
        log.info( "Hibernate configuration loaded, using dialect: " + configuration.getProperty( "hibernate.dialect" ) );
        
        this.configuration = configuration;
    }
    
    // -------------------------------------------------------------------------
    // HibernateConfigurationProvider implementation
    // -------------------------------------------------------------------------

    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    @Override
    public List<Resource> getJarResources()
    {
        return jarResources;
    }

    @Override
    public List<Resource> getDirectoryResources()
    {
        return dirResources;
    }    
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Properties getProperties( String path )
        throws IOException
    {
        try
        {
            return getProperties( new ClassPathResource( path ).getInputStream() );
        }
        catch ( FileNotFoundException ex )
        {
            return null;
        }
        catch ( SecurityException ex )
        {
            log.warn( "Not permitted to read properties file: " + path, ex );
            
            return null;
        }
    }
    
    private Properties getProperties( InputStream inputStream )
        throws IOException
    {
        try
        {
            Properties properties = new Properties();            
            properties.load( inputStream );
            
            return properties;
        }
        finally
        {
            inputStream.close();
        }
    }
}
