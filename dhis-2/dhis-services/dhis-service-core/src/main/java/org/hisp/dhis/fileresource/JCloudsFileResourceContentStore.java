package org.hisp.dhis.fileresource;

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

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
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
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponseException;
import org.jclouds.rest.AuthorizationException;
import org.joda.time.Minutes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Halvdan Hoem Grelland
 */
public class JCloudsFileResourceContentStore
    implements FileResourceContentStore
{
    private static final Log log = LogFactory.getLog( JCloudsFileResourceContentStore.class );

    private static final Pattern CONTAINER_NAME_PATTERN = Pattern.compile( "^((?!-)[a-zA-Z0-9-]{1,63}(?<!-))+$" );

    private static final long FIVE_MINUTES_IN_SECONDS = Minutes.minutes( 5 ).toStandardDuration().getStandardSeconds();

    private BlobStore blobStore;
    private BlobStoreContext blobStoreContext;
    private String container;

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    private static final String JCLOUDS_PROVIDER_KEY_FILESYSTEM = "filesystem";
    private static final String JCLOUDS_PROVIDER_KEY_AWS_S3 = "aws-s3";
    private static final String JCLOUDS_PROVIDER_KEY_TRANSIENT = "transient";

    private static final List<String> SUPPORTED_PROVIDERS = Arrays.asList( JCLOUDS_PROVIDER_KEY_FILESYSTEM, JCLOUDS_PROVIDER_KEY_AWS_S3 );

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
        String provider = configurationProvider.getProperty( ConfigurationKey.FILESTORE_PROVIDER );
        String location = configurationProvider.getProperty( ConfigurationKey.FILESTORE_LOCATION );
        String identity = configurationProvider.getProperty( ConfigurationKey.FILESTORE_IDENTITY );
        String secret = configurationProvider.getProperty( ConfigurationKey.FILESTORE_SECRET );

        provider = validateAndSelectProvider( provider );

        container = configurationProvider.getProperty( ConfigurationKey.FILESTORE_CONTAINER );

        if ( !isValidContainerName( container ) )
        {
            if ( container != null )
            {
                log.warn( "Container name '" + container + "' is illegal." +
                    "Standard domain name naming conventions apply (and underscores are not allowed). " +
                    "Using default container name '" + ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue() + "'." );
            }

            container = ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue();
        }

        Properties overrides = new Properties();

        Credentials credentials = new Credentials( "Unused", "Unused" );

        // ---------------------------------------------------------------------
        // Provider specific configuration
        // ---------------------------------------------------------------------

        if ( provider.equals( JCLOUDS_PROVIDER_KEY_FILESYSTEM ) && locationManager.externalDirectorySet() )
        {
            overrides.setProperty( FilesystemConstants.PROPERTY_BASEDIR, locationManager.getExternalDirectoryPath() );
        }
        else if ( provider.equals( JCLOUDS_PROVIDER_KEY_AWS_S3 ) )
        {
            credentials = new Credentials( identity, secret );

            if ( credentials.identity.isEmpty() || credentials.credential.isEmpty() )
            {
                log.warn( "AWS S3 store configured with empty credentials, authentication not possible." );
            }
        }

        // ---------------------------------------------------------------------
        // Set up JClouds context
        // ---------------------------------------------------------------------

        blobStoreContext = ContextBuilder.newBuilder( provider )
            .credentials( credentials.identity, credentials.credential )
            .overrides( overrides ).build( BlobStoreContext.class );

        blobStore = blobStoreContext.getBlobStore();

        Optional<? extends Location> configuredLocation = blobStore.listAssignableLocations()
            .stream().filter( l -> l.getId().equals( location ) ).findFirst();

        try
        {
            blobStore.createContainerInLocation( configuredLocation.isPresent() ? configuredLocation.get() : null, container );

            log.info( "File store configured with provider '" + provider + "' and container '" + container + "'. " +
                ( configuredLocation.isPresent() ? "Provider location: " + configuredLocation.get().getId() : StringUtils.EMPTY ) );
        }
        catch ( HttpResponseException ex )
        {
            log.error( "Could not configure file store with provider '" + provider + "' and container '" + container + "'. " +
                "Check your internet connectivity. File storage will not be available.", ex );
        }
        catch ( AuthorizationException ex )
        {
            log.error( "Could not authenticate with file store provider '" + provider + "' and container '" + container + "'. " +
                "File storage will not be available.", ex );
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
    public ByteSource getFileResourceContent( String key )
    {
        final Blob blob = getBlob( key );

        if ( blob == null )
        {
            return null;
        }

        final ByteSource byteSource = new ByteSource()
        {
            @Override
            public InputStream openStream()
            {
                try
                {
                    return blob.getPayload().openStream();
                }
                catch ( IOException e )
                {
                    return new NullInputStream( 0 );
                }
            }
        };

        boolean isEmptyOrFailed;

        try
        {
            isEmptyOrFailed = byteSource.isEmpty();
        }
        catch ( IOException e )
        {
            isEmptyOrFailed = true;
        }

        return isEmptyOrFailed ? null : byteSource;
    }

    @Override public String saveFileResourceContent( FileResource fileResource, byte[] bytes )
    {
        Blob blob = createBlob( fileResource, bytes );

        if ( blob == null )
        {
            return null;
        }

        putBlob( blob );

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

        putBlob( blob );

        try
        {
            Files.deleteIfExists( file.toPath() );
        }
        catch ( IOException ioe )
        {
            // Intentionally ignored
            log.warn( "Temporary file '" + file.toPath() + "' could not be deleted.", ioe );
        }

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
            httpRequest = signer.signGetBlob( container, key, FIVE_MINUTES_IN_SECONDS );
        }
        catch ( UnsupportedOperationException uoe )
        {
            return null;
        }

        return httpRequest.getEndpoint();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Blob getBlob( String key )
    {
        return blobStore.getBlob( container, key );
    }

    private boolean blobExists( String key )
    {
        return key != null && blobStore.blobExists( container, key );
    }

    private void deleteBlob( String key )
    {
        blobStore.removeBlob( container, key );
    }

    private String putBlob( Blob blob )
    {
        String etag = null;

        try
        {
            etag = blobStore.putBlob( container, blob );
        }
        catch ( RuntimeException rte )
        {
            Throwable cause = rte.getCause();

            if ( cause != null && cause instanceof UserPrincipalNotFoundException )
            {
                // Intentionally ignored exception which occurs with JClouds on localized
                // Windows systems while trying to resolve the "Everyone" group.
                // See https://issues.apache.org/jira/browse/JCLOUDS-1015
                log.debug( "Ignored UserPrincipalNotFoundException. Workaround for JClouds bug 'JCLOUDS-1015'." );
            }
            else
            {
                throw rte;
            }
        }

        return etag;
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

    private String validateAndSelectProvider( String provider )
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

        return provider;
    }

    private boolean isValidContainerName( String containerName ) 
    {
        return containerName != null && CONTAINER_NAME_PATTERN.matcher( containerName ).matches();
    }

    private boolean requestSigningSupported( BlobRequestSigner signer )
    {
        return !( signer instanceof RequestSigningUnsupported ) && !( signer instanceof LocalBlobRequestSigner );
    }
}
