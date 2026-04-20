/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.jclouds;

import static org.jclouds.Constants.PROPERTY_ENDPOINT;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.prefix;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.storage.BlobContainerName;
import org.hisp.dhis.storage.BlobKey;
import org.hisp.dhis.storage.BlobKeyPrefix;
import org.hisp.dhis.storage.BlobStoreService;
import org.hisp.dhis.storage.BlobStoreService.ContentDisposition;
import org.hisp.dhis.storage.ContentHash;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.LocalBlobRequestSigner;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.internal.RequestSigningUnsupported;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.http.HttpRequest;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.s3.reference.S3Constants;
import org.springframework.stereotype.Component;

/**
 * JClouds-backed implementation of {@link BlobStoreService}. Manages the JClouds {@link
 * BlobStoreContext} and initializes the container {@link ConfigurationKey#FILESTORE_CONTAINER} in
 * which DHIS2 stores files/images/icons/apps.
 */
@Slf4j
@Component
public class JCloudsStore implements BlobStoreService {

  private final LocationManager locationManager;
  private final FileStoreConfig fileStoreConfig;
  private final BlobStoreContext blobStoreContext;

  JCloudsStore(DhisConfigurationProvider configurationProvider, LocationManager locationManager) {
    this.locationManager = locationManager;

    FileStoreProvider provider =
        FileStoreProvider.of(
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_PROVIDER));
    String location = configurationProvider.getProperty(ConfigurationKey.FILESTORE_LOCATION);
    String endpoint = configurationProvider.getProperty(ConfigurationKey.FILESTORE_ENDPOINT);
    String container = configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER);

    validateFilesystemProvider(provider);

    fileStoreConfig = new FileStoreConfig(provider, location, container);

    String identity = configurationProvider.getProperty(ConfigurationKey.FILESTORE_IDENTITY);
    String secret = configurationProvider.getProperty(ConfigurationKey.FILESTORE_SECRET);

    blobStoreContext =
        ContextBuilder.newBuilder(provider.key())
            .credentials(identity, secret)
            .overrides(configureOverrides(provider, endpoint))
            .modules(Set.of(new SLF4JLoggingModule()))
            .build(BlobStoreContext.class);
  }

  @PostConstruct
  public void init() {
    Location location = createLocation(fileStoreConfig.provider.key(), fileStoreConfig.location);
    blobStoreContext.getBlobStore().createContainerInLocation(location, fileStoreConfig.container);

    log.info(
        "File store configured with provider: '{}', container: '{}' and location: '{}'.",
        fileStoreConfig.provider.key(),
        fileStoreConfig.container,
        fileStoreConfig.location);
  }

  @PreDestroy
  public void cleanUp() {
    blobStoreContext.close();
  }

  @Override
  public boolean blobExists(BlobKey key) {
    return key != null && getBlobStore().blobExists(fileStoreConfig.container, key.value());
  }

  @Override
  @CheckForNull
  public InputStream openStream(BlobKey key) {
    Blob blob = getBlobStore().getBlob(fileStoreConfig.container, key.value());
    if (blob == null) {
      return null;
    }
    try {
      return blob.getPayload().openStream();
    } catch (IOException e) {
      log.warn("Unable to open stream for key: {}. {}", key, e.getMessage());
      return null;
    }
  }

  @Override
  public long contentLength(BlobKey key) {
    Blob blob = getBlobStore().getBlob(fileStoreConfig.container, key.value());
    if (blob == null) {
      return 0;
    }
    return blob.getMetadata().getContentMetadata().getContentLength();
  }

  @Override
  public void putBlob(
      BlobKey key,
      InputStream content,
      long contentLength,
      @CheckForNull String contentType,
      @CheckForNull ContentDisposition contentDisposition,
      @CheckForNull ContentHash contentHash) {
    BlobStore bs = getBlobStore();
    var builder = bs.blobBuilder(key.value()).payload(content).contentLength(contentLength);
    if (StringUtils.isNotEmpty(contentType)) {
      builder.contentType(contentType);
    }
    if (contentDisposition != null) {
      builder.contentDisposition(contentDisposition.value());
    }
    if (contentHash != null) {
      builder.contentMD5(HashCode.fromString(contentHash.hex()));
    }
    bs.putBlob(fileStoreConfig.container, builder.build());
  }

  @Override
  public void deleteBlob(BlobKey key) {
    getBlobStore().removeBlob(fileStoreConfig.container, key.value());
  }

  @Override
  public void deleteDirectory(BlobKeyPrefix prefix) {
    getBlobStore().deleteDirectory(fileStoreConfig.container, prefix.value());
  }

  @Override
  public Iterable<BlobKeyPrefix> listFolders(BlobKeyPrefix prefix) {
    // JClouds directory listing requires a trailing "/" on the prefix
    String jcloudsPrefix = prefix.value() + "/";
    return getBlobStore()
        .list(fileStoreConfig.container, prefix(jcloudsPrefix).delimiter("/"))
        .stream()
        .map(m -> BlobKeyPrefix.of(m.getName()))
        .toList();
  }

  @Override
  public Iterable<BlobKey> listKeys(BlobKeyPrefix prefix) {
    return getBlobStore()
        .list(fileStoreConfig.container, prefix(prefix.value()).recursive())
        .stream()
        .map(StorageMetadata::getName)
        .map(BlobKey::new)
        .toList();
  }

  @Override
  @CheckForNull
  public URI signedGetUri(BlobKey key, long expirationSeconds) {
    BlobRequestSigner signer = blobStoreContext.getSigner();
    if (signer instanceof RequestSigningUnsupported || signer instanceof LocalBlobRequestSigner) {
      return null;
    }
    try {
      HttpRequest httpRequest =
          signer.signGetBlob(fileStoreConfig.container, key.value(), expirationSeconds);
      return httpRequest.getEndpoint();
    } catch (UnsupportedOperationException e) {
      return null;
    }
  }

  @Override
  public BlobContainerName container() {
    return new BlobContainerName(fileStoreConfig.container);
  }

  @Override
  public boolean isFilesystem() {
    return fileStoreConfig.provider == FileStoreProvider.FILESYSTEM;
  }

  private void validateFilesystemProvider(FileStoreProvider provider) {
    if (provider == FileStoreProvider.FILESYSTEM && !locationManager.externalDirectorySet()) {
      throw new IllegalArgumentException(
          "File system file store provider could not be configured; external directory is not set. ");
    }
  }

  private Properties configureOverrides(FileStoreProvider provider, String endpoint) {
    if (provider == FileStoreProvider.FILESYSTEM && locationManager.externalDirectorySet()) {
      Properties overrides = new Properties();
      overrides.setProperty(
          FilesystemConstants.PROPERTY_BASEDIR, locationManager.getExternalDirectoryPath());
      return overrides;
    }

    if (provider == FileStoreProvider.AWS_S3) {
      Properties overrides = new Properties();
      overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");
      return overrides;
    }

    if (provider == FileStoreProvider.S3) {
      Properties overrides = new Properties();
      overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");

      if (StringUtils.isNotEmpty(endpoint)) {
        overrides.setProperty(PROPERTY_ENDPOINT, endpoint);
      }
      return overrides;
    }

    return new Properties();
  }

  private static Location createLocation(String provider, String location) {
    if (location == null) {
      // some BlobStores allow specifying a location, such as US-EAST, where containers will exist.
      // null will choose a default location.
      return null;
    }

    Location parent =
        new LocationBuilder()
            .scope(LocationScope.PROVIDER)
            .id(provider)
            .description(provider)
            .build();

    return new LocationBuilder()
        .scope(LocationScope.REGION)
        .id(location)
        .description(location)
        .parent(parent)
        .build();
  }

  private record FileStoreConfig(FileStoreProvider provider, String location, String container) {}

  private BlobStore getBlobStore() {
    return blobStoreContext.getBlobStore();
  }
}
