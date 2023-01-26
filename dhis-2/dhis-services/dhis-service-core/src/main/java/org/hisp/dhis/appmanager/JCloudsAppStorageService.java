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
package org.hisp.dhis.appmanager;

import static org.jclouds.blobstore.options.ListContainerOptions.Builder.prefix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.util.ZipFileUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.LocalBlobRequestSigner;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.internal.RequestSigningUnsupported;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponseException;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.s3.reference.S3Constants;
import org.joda.time.Minutes;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Stian Sandvold
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.appmanager.JCloudsAppStorageService" )
public class JCloudsAppStorageService
    implements AppStorageService
{
    private static final Pattern CONTAINER_NAME_PATTERN = Pattern
        .compile( "^(?![.-])(?=.{1,63})([.-]?[a-zA-Z0-9]+)+$" );

    private static final long FIVE_MINUTES_IN_SECONDS = Minutes.minutes( 5 ).toStandardDuration().getStandardSeconds();

    private Map<String, App> reservedNamespaces = new HashMap<>();

    private BlobStore blobStore;

    private BlobStoreContext blobStoreContext;

    private BlobStoreProperties config;

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    private static final String JCLOUDS_PROVIDER_KEY_FILESYSTEM = "filesystem";

    private static final String JCLOUDS_PROVIDER_KEY_AWS_S3 = "aws-s3";

    private static final String JCLOUDS_PROVIDER_KEY_TRANSIENT = "transient";

    private static final List<String> SUPPORTED_PROVIDERS = Arrays.asList( JCLOUDS_PROVIDER_KEY_FILESYSTEM,
        JCLOUDS_PROVIDER_KEY_AWS_S3, JCLOUDS_PROVIDER_KEY_TRANSIENT );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final LocationManager locationManager;

    private final DhisConfigurationProvider configurationProvider;

    private final ObjectMapper jsonMapper;

    @PostConstruct
    public void init()
    {
        // ---------------------------------------------------------------------
        // Bootstrap config
        // ---------------------------------------------------------------------

        config = new BlobStoreProperties(
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_PROVIDER ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_LOCATION ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_CONTAINER ) );

        Pair<Credentials, Properties> providerConfig = configureForProvider(
            config.provider,
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_IDENTITY ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_SECRET ) );

        // ---------------------------------------------------------------------
        // Set up JClouds context
        // ---------------------------------------------------------------------

        blobStoreContext = ContextBuilder.newBuilder( config.provider )
            .credentials( providerConfig.getLeft().identity, providerConfig.getLeft().credential )
            .overrides( providerConfig.getRight() )
            .build( BlobStoreContext.class );

        blobStore = blobStoreContext.getBlobStore();

        Location provider = new LocationBuilder()
            .scope( LocationScope.PROVIDER )
            .id( config.provider )
            .description( config.provider )
            .build();

        try
        {
            blobStore.createContainerInLocation( createRegionLocation( config, provider ), config.container );

            log.info( String.format( "File store configured with provider: '%s', container: '%s' and location: '%s'.",
                config.provider, config.container, config.location ) );
        }
        catch ( HttpResponseException ex )
        {
            log.error( String.format( "Could not configure file store with provider '%s' and container '%s'.\n" +
                "File storage will not be available.", config.provider, config.container ), ex );
        }
        catch ( AuthorizationException ex )
        {
            log.error( String.format( "Could not authenticate with file store provider '%s' and container '%s'. " +
                "File storage will not be available.", config.provider, config.location ), ex );
        }
    }

    @PreDestroy
    public void cleanUp()
    {
        blobStoreContext.close();
    }

    @Override
    public Map<String, App> discoverInstalledApps()
    {
        Map<String, App> appMap = new HashMap<>();
        List<App> appList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

        log.info( "Starting JClouds discovery" );

        for ( StorageMetadata resource : blobStore.list( config.container, prefix( APPS_DIR + "/" ).delimiter( "/" ) ) )
        {
            log.info( "Found potential app: " + resource.getName() );

            // Found potential app
            Blob manifest = blobStore.getBlob( config.container, resource.getName() + "manifest.webapp" );

            if ( manifest == null )
            {
                log.warn( "Could not find manifest file of " + resource.getName() );
                continue;
            }

            try
            {
                InputStream inputStream = manifest.getPayload().openStream();
                App app = mapper.readValue( inputStream, App.class );
                inputStream.close();

                app.setAppStorageSource( AppStorageSource.JCLOUDS );
                app.setFolderName( resource.getName() );

                appList.add( app );
            }
            catch ( IOException ex )
            {
                log.error( "Could not read manifest file of " + resource.getName(), ex );
                log.error( DebugUtils.getStackTrace( ex ) );
            }
        }

        appList.forEach(
            app -> {
                String namespace = app.getActivities().getDhis().getNamespace();

                if ( namespace != null && !namespace.isEmpty() )
                {
                    reservedNamespaces.put( namespace, app );
                }

                appMap.put( app.getUrlFriendlyName(), app );

                log.info( "Discovered app '" + app.getName() + "' from JClouds storage " );
            } );

        if ( appList.isEmpty() )
        {
            log.info( "No apps found during JClouds discovery." );
        }
        return appMap;
    }

    @Override
    public Map<String, App> getReservedNamespaces()
    {
        return reservedNamespaces;
    }

    private boolean validateApp( App app, Cache<App> appCache )
    {
        // -----------------------------------------------------------------
        // Check if app with same key is currently being deleted
        // (deletion_in_progress)
        // -----------------------------------------------------------------
        Optional<App> existingApp = appCache.getIfPresent( app.getKey() );
        if ( existingApp.isPresent() && existingApp.get().getAppState() == AppStatus.DELETION_IN_PROGRESS )
        {
            log.error( "Failed to install app: App with same name is currently being deleted" );

            app.setAppState( AppStatus.DELETION_IN_PROGRESS );
            return false;
        }

        // -----------------------------------------------------------------
        // Check for namespace and if it's already taken by another app
        // Allow install if namespace was taken by another version of this app
        // -----------------------------------------------------------------

        String namespace = app.getActivities().getDhis().getNamespace();

        if ( namespace != null && !namespace.isEmpty() && reservedNamespaces.containsKey( namespace )
            && !app.equals( reservedNamespaces.get( namespace ) ) )
        {
            log.error( String.format( "Failed to install app '%s': Namespace '%s' already taken.",
                app.getName(), namespace ) );

            app.setAppState( AppStatus.NAMESPACE_TAKEN );
            return false;
        }

        // -----------------------------------------------------------------
        // Check that, iff this is a bundled app, it is configured as a core app
        // -----------------------------------------------------------------

        if ( app.isBundled() != app.isCoreApp() )
        {
            if ( app.isBundled() )
            {
                log.error(
                    String.format(
                        "Failed to install app '%s': bundled app overrides muse be declared with core_app=true",
                        app.getShortName() ) );
                app.setAppState( AppStatus.INVALID_BUNDLED_APP_OVERRIDE );
            }
            else
            {
                log.error(
                    String.format(
                        "Failed to install app '%s': apps declared with core_app=true must override a bundled app",
                        app.getShortName() ) );
                app.setAppState( AppStatus.INVALID_CORE_APP );
            }

            return false;
        }
        return true;
    }

    @Override
    public App installApp( File file, String filename, Cache<App> appCache )
    {
        App app = new App();
        log.info( "Installing new app: " + filename );

        try ( ZipFile zip = new ZipFile( file ) )
        {
            // -----------------------------------------------------------------
            // Determine top-level directory name, if the zip file contains one
            // -----------------------------------------------------------------

            String prefix = ZipFileUtils.getTopLevelDirectory( zip.entries().asIterator() );
            log.debug( "Detected top-level directory '" + prefix + "' in zip" );

            // -----------------------------------------------------------------
            // Parse manifest.webapp file from ZIP archive.
            // -----------------------------------------------------------------

            ZipEntry entry = zip.getEntry( prefix + MANIFEST_FILENAME );

            if ( entry == null )
            {
                log.error( "Failed to install app: Missing manifest.webapp in zip" );

                app.setAppState( AppStatus.MISSING_MANIFEST );
                return app;
            }

            InputStream inputStream = zip.getInputStream( entry );

            app = jsonMapper.readValue( inputStream, App.class );

            app.setFolderName( APPS_DIR + File.separator + filename.substring( 0, filename.lastIndexOf( '.' ) ) );
            app.setAppStorageSource( AppStorageSource.JCLOUDS );

            if ( !this.validateApp( app, appCache ) )
            {
                return app;
            }

            // -----------------------------------------------------------------
            // Unzip the app
            // -----------------------------------------------------------------

            String dest = APPS_DIR + File.separator + filename.substring( 0, filename.lastIndexOf( '.' ) );

            zip.stream().forEach( (Consumer<ZipEntry>) zipEntry -> {

                log.debug( "Uploading zipEntry: " + zipEntry );
                String name = zipEntry.getName().substring( prefix.length() );

                try
                {
                    InputStream input = zip.getInputStream( zipEntry );

                    Blob blob = blobStore.blobBuilder( dest + File.separator + name )
                        .payload( input )
                        .contentLength( zipEntry.getSize() )
                        .build();

                    blobStore.putBlob( config.container, blob );

                    input.close();

                }
                catch ( IOException e )
                {
                    log.error( "Unable to store app file '" + name + "'", e );
                }
            } );

            String namespace = app.getActivities().getDhis().getNamespace();
            if ( namespace != null && !namespace.isEmpty() )
            {
                reservedNamespaces.put( namespace, app );
            }

            log.info( String.format( ""
                + "New app '%s' installed"
                + "\n\tInstall path: %s"
                + (namespace != null && !namespace.isEmpty() ? "\n\tNamespace reserved: %s" : ""),
                app.getName(), dest, namespace ) );

            // -----------------------------------------------------------------
            // Installation complete.
            // -----------------------------------------------------------------

            app.setAppState( AppStatus.OK );
            return app;
        }
        catch ( ZipException e )
        {
            log.error( "Failed to install app: Invalid ZIP format", e );
            app.setAppState( AppStatus.INVALID_ZIP_FORMAT );
        }
        catch ( JsonParseException e )
        {
            log.error( "Failed to install app: Invalid manifest.webapp", e );
            app.setAppState( AppStatus.INVALID_MANIFEST_JSON );
        }
        catch ( IOException e )
        {
            log.error( "Failed to install app: Could not save app", e );
            app.setAppState( AppStatus.INSTALLATION_FAILED );
        }

        return app;
    }

    @Override
    public void deleteApp( App app )
    {
        log.info( "Deleting app " + app.getName() );

        // Delete all files related to app
        for ( StorageMetadata resource : blobStore.list( config.container, prefix( app.getFolderName() ).recursive() ) )
        {
            log.debug( "Deleting app file: " + resource.getName() );

            blobStore.removeBlob( config.container, resource.getName() );
        }

        reservedNamespaces.remove( app.getActivities().getDhis().getNamespace(), app );

        log.info( "Deleted app " + app.getName() );
    }

    @Override
    public Resource getAppResource( App app, String pageName )
        throws IOException
    {
        if ( app == null || !app.getAppStorageSource().equals( AppStorageSource.JCLOUDS ) )
        {
            log.warn( "Can't look up resource " + pageName + ". The specified app was not found in JClouds storage." );
            return null;
        }

        String key = (app.getFolderName() + ("/" + pageName)).replaceAll( "//", "/" );
        URI uri = getSignedGetContentUri( key );

        if ( uri == null )
        {

            String filepath = configurationProvider.getProperty( ConfigurationKey.FILESTORE_CONTAINER ) + "/" + key;
            filepath = filepath.replaceAll( "//", "/" );
            File res;

            try
            {
                res = locationManager.getFileForReading( filepath );
            }
            catch ( LocationManagerException e )
            {
                return null;
            }

            if ( res.isDirectory() )
            {
                String indexPath = pageName.replaceAll( "/+$", "" ) + "/index.html";
                log.info( "Resource " + pageName + " (" + filepath + " is a directory, serving " + indexPath );
                return getAppResource( app, indexPath );
            }
            else if ( res.exists() )
            {
                return new FileSystemResource( res );
            }
            else
            {
                return null;
            }
        }

        return new UrlResource( uri );
    }

    private static Location createRegionLocation( BlobStoreProperties config, Location provider )
    {
        return config.location != null ? new LocationBuilder()
            .scope( LocationScope.REGION )
            .id( config.location )
            .description( config.location )
            .parent( provider )
            .build() : null;
    }

    private Pair<Credentials, Properties> configureForProvider( String provider, String identity, String secret )
    {
        Properties overrides = new Properties();
        Credentials credentials = new Credentials( "Unused", "Unused" );

        if ( provider.equals( JCLOUDS_PROVIDER_KEY_FILESYSTEM ) && locationManager.externalDirectorySet() )
        {
            overrides.setProperty( FilesystemConstants.PROPERTY_BASEDIR, locationManager.getExternalDirectoryPath() );
        }
        else if ( provider.equals( JCLOUDS_PROVIDER_KEY_AWS_S3 ) )
        {
            credentials = new Credentials( identity, secret );
            overrides.setProperty( S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false" );

            if ( credentials.identity.isEmpty() || credentials.credential.isEmpty() )
            {
                log.warn( "AWS S3 store configured without credentials, authentication not possible." );
            }
        }

        return Pair.of( credentials, overrides );
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private class BlobStoreProperties
    {
        private String provider;

        private String location;

        private String container;

        BlobStoreProperties( String provider, String location, String container )
        {
            this.provider = provider;
            this.location = location;
            this.container = container;

            validate();
            validateAndSelectProvider();
        }

        private void validate()
        {
            if ( !isValidContainerName( container ) )
            {
                if ( container != null )
                {
                    log.warn( String.format( "Container name '%s' is illegal. " +
                        "Standard domain name naming conventions apply (no underscores allowed). " +
                        "Using default container name ' %s'", container,
                        ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue() ) );
                }

                container = ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue();
            }
        }

        private boolean isValidContainerName( String containerName )
        {
            return containerName != null && CONTAINER_NAME_PATTERN.matcher( containerName ).matches();
        }

        private void validateAndSelectProvider()
        {
            if ( !SUPPORTED_PROVIDERS.contains( provider ) )
            {
                log.warn( "Ignored unsupported file store provider '" + provider + "', using file system provider." );
                provider = JCLOUDS_PROVIDER_KEY_FILESYSTEM;
            }

            if ( provider.equals( JCLOUDS_PROVIDER_KEY_FILESYSTEM ) && !locationManager.externalDirectorySet() )
            {
                log.info( "File system file store provider could not be configured; external directory is not set. " +
                    "Falling back to in-memory provider." );
                provider = JCLOUDS_PROVIDER_KEY_TRANSIENT;
            }
        }
    }

    public URI getSignedGetContentUri( String key )
    {
        BlobRequestSigner signer = blobStoreContext.getSigner();

        if ( !requestSigningSupported( signer ) )
        {
            return null;
        }

        HttpRequest httpRequest;

        try
        {
            httpRequest = signer.signGetBlob( config.container, key, FIVE_MINUTES_IN_SECONDS );
        }
        catch ( UnsupportedOperationException uoe )
        {
            return null;
        }

        return httpRequest.getEndpoint();
    }

    private boolean requestSigningSupported( BlobRequestSigner signer )
    {
        return !(signer instanceof RequestSigningUnsupported) && !(signer instanceof LocalBlobRequestSigner);
    }
}
