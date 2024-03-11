package org.hisp.dhis.fileresource;

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

import com.google.common.hash.HashCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.LocalBlobRequestSigner;
import org.jclouds.blobstore.domain.Blob;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Halvdan Hoem Grelland
 */
public class JCloudsFileResourceContentStore
    implements FileResourceContentStore
{
    private static final Log log = LogFactory.getLog( JCloudsFileResourceContentStore.class );

    private static final Pattern CONTAINER_NAME_PATTERN = Pattern
        .compile( "^(?![.-])(?=.{1,63}$)([.-]?[a-zA-Z0-9]+)+$" );

    private static final long FIVE_MINUTES_IN_SECONDS = Minutes.minutes( 5 ).toStandardDuration().getStandardSeconds();

    private BlobStore blobStore;

    private BlobStoreContext blobStoreContext;

    private BlobStoreProperties config;

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    private static final String JCLOUDS_PROVIDER_KEY_FILESYSTEM = "filesystem";

    private static final String JCLOUDS_PROVIDER_KEY_AWS_S3 = "aws-s3";

    private static final String JCLOUDS_PROVIDER_KEY_TRANSIENT = "transient";

    private static final List<String> SUPPORTED_PROVIDERS =
        Arrays.asList( JCLOUDS_PROVIDER_KEY_FILESYSTEM, JCLOUDS_PROVIDER_KEY_AWS_S3, JCLOUDS_PROVIDER_KEY_TRANSIENT );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private LocationManager locationManager;

    public void setLocationManager( LocationManager locationManager )
    {
        this.locationManager = locationManager;
    }

    private DhisConfigurationProvider configurationProvider;

    public void setConfigurationProvider( DhisConfigurationProvider configurationProvider )
    {
        this.configurationProvider = configurationProvider;
    }

    // -------------------------------------------------------------------------
    // Life cycle management
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init()
    {
        // ---------------------------------------------------------------------
        // Bootstrap config
        // ---------------------------------------------------------------------

        config = new BlobStoreProperties(
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_PROVIDER ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_LOCATION ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_CONTAINER )
        );

        Pair<Credentials, Properties> providerConfig = configureForProvider(
            config.provider,
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_IDENTITY ),
            configurationProvider.getProperty( ConfigurationKey.FILESTORE_SECRET )
        );

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

    // -------------------------------------------------------------------------
    // FileResourceContentStore implementation
    // -------------------------------------------------------------------------

    @Override
    public InputStream getFileResourceContent( String key )
    {
        final Blob blob = getBlob( key );

        if ( blob == null )
        {
            return null;
        }

        try
        {
            return blob.getPayload().openStream();
        }
        catch ( IOException e )
        {
            log.warn( String.format( "Unable to retrieve fileResource with key: %s. Message: %s", key, e.getMessage() ) );
            return null;
        }
    }

    @Override
    public long getFileResourceContentLength( String key )
    {
        final Blob blob = getBlob( key );

        if ( blob == null )
        {
            return 0;
        }

        return blob.getMetadata().getContentMetadata().getContentLength();
    }
    
    @Override
    public String saveFileResourceContent( FileResource fileResource, byte[] bytes )
    {
        Blob blob = createBlob( fileResource, bytes );

        if ( blob == null )
        {
            return null;
        }

        blobStore.putBlob( config.container, blob );

        log.debug( String.format( "File resource saved with key: %s", fileResource.getStorageKey() ) );

        return fileResource.getStorageKey();
    }

    @Override
    public String saveFileResourceContent( FileResource fileResource, File file )
    {
        Blob blob = createBlob( fileResource, file );

        if ( blob == null )
        {
            return null;
        }

        blobStore.putBlob( config.container, blob );

        try
        {
            Files.deleteIfExists( file.toPath() );
        }
        catch ( IOException ioe )
        {
            log.warn( String.format( "Temporary file '%s' could not be deleted.", file.toPath() ), ioe );
        }

        log.debug( String.format( "File resource saved with key: %s", fileResource.getStorageKey() ) );

        return fileResource.getStorageKey();
    }

    @Override
    public void deleteFileResourceContent( String key )
    {
        deleteBlob( key );
    }

    @Override
    public boolean fileResourceContentExists( String key )
    {
        return blobExists( key );
    }

    @Override
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

    @Override
    public void copyContent( String key, OutputStream output )
        throws IOException, NoSuchElementException
    {
        if ( !blobExists( key ) )
        {
            throw new NoSuchElementException( "key '" + key + "' not found." );
        }

        Blob blob = getBlob( key );

        try ( InputStream in = blob.getPayload().openStream() )
        {
            IOUtils.copy( in, output );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Blob getBlob( String key )
    {
        return blobStore.getBlob( config.container, key );
    }

    private boolean blobExists( String key )
    {
        return key != null && blobStore.blobExists( config.container, key );
    }

    private void deleteBlob( String key )
    {
        blobStore.removeBlob( config.container, key );
    }

    private Blob createBlob( FileResource fileResource, byte[] bytes )
    {
        return blobStore.blobBuilder( fileResource.getStorageKey() )
            .payload( bytes )
            .contentLength( fileResource.getContentLength() )
            .contentMD5( HashCode.fromString( fileResource.getContentMd5() ) )
            .contentType( fileResource.getContentType() )
            .contentDisposition( "filename=" + fileResource.getName() )
            .build();
    }

    private Blob createBlob( FileResource fileResource, File file )
    {
        return blobStore.blobBuilder( fileResource.getStorageKey() )
            .payload( file )
            .contentLength( fileResource.getContentLength() )
            .contentMD5( HashCode.fromString( fileResource.getContentMd5() ) )
            .contentType( fileResource.getContentType() )
            .contentDisposition( "filename=" + fileResource.getName() )
            .build();
    }

    private boolean requestSigningSupported( BlobRequestSigner signer )
    {
        return !( signer instanceof RequestSigningUnsupported) && !( signer instanceof LocalBlobRequestSigner );
    }

    private static Location createRegionLocation( BlobStoreProperties config, Location provider )
    {
        return config.location != null ?
            new LocationBuilder()
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
}
