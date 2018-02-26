package org.hisp.dhis.hibernate;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
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
    private static final String FILENAME_CACHE_NAMES = "hibernate-caches.txt";
    private static final String PROP_EHCACHE_PEER_PROVIDER_RIM_URLS = "ehcache.peer.provider.rmi.urls";
    private static final String PROP_EHCACHE_PEER_LISTENER_HOSTNAME = "ehcache.peer.listener.hostname";
    private static final String PROP_EHCACHE_PEER_LISTENER_PORT = "ehcache.peer.listener.port";
    private static final String PROP_EHCACHE_PEER_LISTENER_REMOTE_OBJECT_PORT = "ehcache.peer.listener.remote.object.port";
    private static final String FORMAT_CLUSTER_INSTANCE_HOSTNAME = "cluster.instance%d.hostname";
    private static final String FORMAT_CLUSTER_INSTANCE_CACHE_PORT = "cluster.instance%d.cache.port";
    private static final String FILENAME_EHCACHE_REPLICATION = "/ehcache-replication.xml";

    private static final String PROP_MEMCACHED_CONNECTION_FACTORY = "hibernate.memcached.connectionFactory";
    private static final String PROP_MEMCACHED_OPERATION_TIMEOUT = "hibernate.memcached.operationTimeout";
    private static final String PROP_MEMCACHED_HASH_ALGORITHM = "hibernate.memcached.hashAlgorithm";
    private static final String PROP_MEMCACHED_CLEAR_SUPPORTED = "hibernate.memcached.clearSupported";    
    private static final String PROP_MEMCACHED_SERVERS = "hibernate.memcached.servers";
    private static final String PROP_MEMCACHED_CACHE_TIME_SECONDS = "hibernate.memcached.cacheTimeSeconds";
    
    private static final int MAX_CLUSTER_INSTANCES = 5;

    // -------------------------------------------------------------------------
    // Property resources
    // -------------------------------------------------------------------------

    private String defaultPropertiesFile = "hibernate-default.properties";
    
    private List<Resource> jarResources = new ArrayList<>();
    private List<Resource> dirResources = new ArrayList<>();
    private List<String> clusterHostnames = new ArrayList<>();
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DhisConfigurationProvider configurationProvider;

    public void setConfigurationProvider( DhisConfigurationProvider configurationProvider )
    {
        this.configurationProvider = configurationProvider;
    }

    // -------------------------------------------------------------------------
    // Initialize
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init()
        throws Exception
    {
        Configuration config = new Configuration();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        boolean testing = "true".equals( System.getProperty( "org.hisp.dhis.test", "false" ) );

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
                
                log.debug( String.format( "Adding jar in which to search for hbm.xml files: %s", file.getAbsolutePath() ) );

                config.addJar( file );
            }
            else
            {
                File file = ResourceUtils.getFile( resource );

                dirResources.add( new FileSystemResource( file ) );
                
                log.debug( String.format( "Adding directory in which to search for hbm.xml files: %s", file.getAbsolutePath() ) );
                
                config.addDirectory( file );
            }
        }

        // ---------------------------------------------------------------------
        // Add default properties from class path
        // ---------------------------------------------------------------------

        Properties defaultProperties = getProperties( defaultPropertiesFile );

        config.addProperties( defaultProperties );

        // ---------------------------------------------------------------------
        // Add custom properties from file system
        // ---------------------------------------------------------------------
        
        if ( !testing )
        {
            try
            {
                Properties fileProperties = configurationProvider.getProperties();
                
                mapToHibernateProperties( fileProperties );

                if ( configurationProvider.isReadOnlyMode() )
                {
                    fileProperties.setProperty( Environment.HBM2DDL_AUTO, "validate" );
                    
                    log.info( "Read-only mode enabled, setting hibernate.hbm2ddl.auto to 'validate'" );
                }
                
                config.addProperties( fileProperties );
            }
            catch ( LocationManagerException ex )
            {
                log.info( "Could not read external configuration from file system" );
            }
        }

        // ---------------------------------------------------------------------
        // Second-level cache
        // ---------------------------------------------------------------------
        
        if ( configurationProvider.isMemcachedCacheProviderEnabled() )
        {
            setMemcachedCacheProvider( config );
            
            log.info( String.format( "Memcached set as cache provider, using server: %s", config.getProperty( PROP_MEMCACHED_SERVERS ) ) );
        }
        
        // ---------------------------------------------------------------------
        // Handle cache replication
        // ---------------------------------------------------------------------
        
        if ( configurationProvider.isClusterEnabled() )
        {
            config.setProperty( "net.sf.ehcache.configurationResourceName", FILENAME_EHCACHE_REPLICATION );
            
            setCacheReplicationConfigSystemProperties();
            
            log.info( "Clustering and cache replication enabled" );
        }

        // ---------------------------------------------------------------------
        // Disable second-level cache during testing
        // ---------------------------------------------------------------------

        if ( testing )
        {
            config.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
            config.setProperty( Environment.USE_QUERY_CACHE, "false" );
        }

        log.info( String.format( "Hibernate configuration loaded, using dialect: %s, region factory: %s",
            config.getProperty( Environment.DIALECT ), config.getProperty( Environment.CACHE_REGION_FACTORY ) ) );
        
        this.configuration = config;
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

    @Override
    public List<String> getClusterHostnames()
    {
        return clusterHostnames;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void mapToHibernateProperties( Properties properties )
    {
        putIfExists( properties, ConfigurationKey.CONNECTION_DIALECT.getKey(), Environment.DIALECT );
        putIfExists( properties, ConfigurationKey.CONNECTION_DRIVER_CLASS.getKey(), Environment.DRIVER );
        putIfExists( properties, ConfigurationKey.CONNECTION_URL.getKey(), Environment.URL );
        putIfExists( properties, ConfigurationKey.CONNECTION_USERNAME.getKey(), Environment.USER );
        putIfExists( properties, ConfigurationKey.CONNECTION_PASSWORD.getKey(), Environment.PASS );
        putIfExists( properties, ConfigurationKey.CONNECTION_SCHEMA.getKey(), Environment.HBM2DDL_AUTO );
        putIfExists( properties, ConfigurationKey.CONNECTION_POOL_MAX_SIZE.getKey(), Environment.C3P0_MAX_SIZE );
    }
    
    private void putIfExists( Properties properties, String from, String to )
    {
        String value = properties.getProperty( from );
        
        if ( value != null && !value.isEmpty() )
        {
            properties.put( to, value );
        }
    }
    
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
    
    /**
     * Sets Hibernate configuration for using {@code memcached} as second-level 
     * cache provider.
     * 
     * @param config the Hibernate configuration object.
     */
    private void setMemcachedCacheProvider( Configuration config )
    {
        config.setProperty( Environment.CACHE_REGION_FACTORY, "com.mc.hibernate.memcached.MemcachedRegionFactory" );
        config.setProperty( PROP_MEMCACHED_CONNECTION_FACTORY, "KetamaConnectionFactory" );
        config.setProperty( PROP_MEMCACHED_OPERATION_TIMEOUT, "5000" );
        config.setProperty( PROP_MEMCACHED_HASH_ALGORITHM, "HashAlgorithm.FNV1_64_HASH" );
        config.setProperty( PROP_MEMCACHED_CLEAR_SUPPORTED, "true" );
        config.setProperty( PROP_MEMCACHED_SERVERS, configurationProvider.getProperty( ConfigurationKey.CACHE_SERVERS ) );
        config.setProperty( PROP_MEMCACHED_CACHE_TIME_SECONDS, configurationProvider.getProperty( ConfigurationKey.CACHE_TIME ) );
    }
    
    /**
     * Sets system properties to be resolved in the Ehcache cache replication 
     * configuration.
     */
    private void setCacheReplicationConfigSystemProperties()
    {
        String instanceHost = configurationProvider.getProperty( ConfigurationKey.CLUSTER_INSTANCE_HOSTNAME );
        String instancePort = configurationProvider.getProperty( ConfigurationKey.CLUSTER_INSTANCE_CACHE_PORT );
        String remoteObjectPort = configurationProvider.getProperty( ConfigurationKey.CLUSTER_INSTANCE_CACHE_REMOTE_OBJECT_PORT );
        
        Properties dhisProps = configurationProvider.getProperties();
        
        List<String> cacheNames = getCacheNames();
        
        final StringBuilder rmiUrlBuilder = new StringBuilder();
        
        for ( int i = 1; i < MAX_CLUSTER_INSTANCES; i++ )
        {
            String hostname = dhisProps.getProperty( String.format( FORMAT_CLUSTER_INSTANCE_HOSTNAME, i ) );
            String port = dhisProps.getProperty( String.format( FORMAT_CLUSTER_INSTANCE_CACHE_PORT, i ) );
            port = StringUtils.defaultIfBlank( port, ConfigurationKey.CLUSTER_INSTANCE_CACHE_PORT.getDefaultValue() );
            
            if ( StringUtils.isNotBlank( hostname ) )
            {   
                final String baseUrl = "//" + hostname + ":" + port + "/";
                
                cacheNames.stream().forEach( name -> rmiUrlBuilder.append( baseUrl + name + "|" ) );
                
                clusterHostnames.add( hostname );
                                
                log.info( "Found cluster instance: " + hostname + ":" + port );
            }
        }
        
        String rmiUrls = StringUtils.removeEnd( rmiUrlBuilder.toString(), "|" );
        
        if ( StringUtils.isBlank( rmiUrls ) )
        {
            log.warn( "At least one cluster instance must be specified when clustering is enabled" );
        }
        
        System.setProperty( PROP_EHCACHE_PEER_LISTENER_HOSTNAME, instanceHost );
        System.setProperty( PROP_EHCACHE_PEER_LISTENER_PORT, instancePort );
        System.setProperty( PROP_EHCACHE_PEER_PROVIDER_RIM_URLS, rmiUrls );
        System.setProperty( PROP_EHCACHE_PEER_LISTENER_REMOTE_OBJECT_PORT, remoteObjectPort );

        log.info( "Ehcache config properties: " + instanceHost + ", " + instancePort + ", " + rmiUrls + ", " + remoteObjectPort  );
    }
    
    /**
     * Returns a list of names of all Hibernate caches.
     */
    private List<String> getCacheNames()
    {
        try ( InputStream input = new ClassPathResource( FILENAME_CACHE_NAMES ).getInputStream() )
        {
            return IOUtils.readLines( input );
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }        
    }
}
