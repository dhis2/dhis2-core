/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.jclouds;

import static org.jclouds.Constants.PROPERTY_ENDPOINT;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.s3.reference.S3Constants;
import org.springframework.stereotype.Component;

/**
 * JCloudsStore manages the JClouds {@link BlobStoreContext} and initializes the container {@link
 * ConfigurationKey#FILESTORE_CONTAINER} in which DHIS2 stores files/images/icons/apps.
 */
@Slf4j
@Component
public class JCloudsStore {

  private static final String JCLOUDS_PROVIDER_KEY_FILESYSTEM = "filesystem";
  private static final String JCLOUDS_PROVIDER_KEY_AWS_S3 = "aws-s3";
  private static final String JCLOUDS_PROVIDER_KEY_S3 =
      "s3"; // for s3 compatible API providers like Minio
  private static final String JCLOUDS_PROVIDER_KEY_TRANSIENT = "transient";
  private static final List<String> SUPPORTED_PROVIDERS =
      List.of(
          JCLOUDS_PROVIDER_KEY_FILESYSTEM,
          JCLOUDS_PROVIDER_KEY_AWS_S3,
          JCLOUDS_PROVIDER_KEY_S3,
          JCLOUDS_PROVIDER_KEY_TRANSIENT);

  private final LocationManager locationManager;
  private final FileStoreConfig fileStoreConfig;
  private final BlobStoreContext blobStoreContext;

  JCloudsStore(DhisConfigurationProvider configurationProvider, LocationManager locationManager) {
    this.locationManager = locationManager;

    String provider =
        validateProvider(configurationProvider.getProperty(ConfigurationKey.FILESTORE_PROVIDER));
    String location = configurationProvider.getProperty(ConfigurationKey.FILESTORE_LOCATION);
    String endpoint = configurationProvider.getProperty(ConfigurationKey.FILESTORE_ENDPOINT);
    String container = configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER);

    fileStoreConfig = new FileStoreConfig(provider, location, container);

    String identity = configurationProvider.getProperty(ConfigurationKey.FILESTORE_IDENTITY);
    String secret = configurationProvider.getProperty(ConfigurationKey.FILESTORE_SECRET);

    blobStoreContext =
        ContextBuilder.newBuilder(provider)
            .credentials(identity, secret)
            .overrides(configureOverrides(provider, endpoint))
            .modules(Set.of(new SLF4JLoggingModule()))
            .build(BlobStoreContext.class);
  }

  private String validateProvider(String provider) {
    if (!SUPPORTED_PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException(
          "Configuration contains unsupported file store provider '"
              + provider
              + "'. Falling back to file system provider instead.");
    }

    if (JCLOUDS_PROVIDER_KEY_FILESYSTEM.equals(provider)
        && !locationManager.externalDirectorySet()) {
      throw new IllegalArgumentException(
          "File system file store provider could not be configured; external directory is not set. ");
    }

    return provider;
  }

  private Properties configureOverrides(String provider, String endpoint) {
    if (JCLOUDS_PROVIDER_KEY_FILESYSTEM.equals(provider)
        && locationManager.externalDirectorySet()) {
      Properties overrides = new Properties();
      overrides.setProperty(
          FilesystemConstants.PROPERTY_BASEDIR, locationManager.getExternalDirectoryPath());
      return overrides;
    }

    if (JCLOUDS_PROVIDER_KEY_AWS_S3.equals(provider)) {
      Properties overrides = new Properties();
      overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");
      return overrides;
    }

    if (JCLOUDS_PROVIDER_KEY_S3.equals(provider)) {
      Properties overrides = new Properties();
      overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");

      if (StringUtils.isNotEmpty(endpoint)) {
        overrides.setProperty(PROPERTY_ENDPOINT, endpoint);
      }
      return overrides;
    }

    return new Properties();
  }

  @PostConstruct
  public void init() {
    Location location = createLocation(fileStoreConfig.provider, fileStoreConfig.location);
    blobStoreContext.getBlobStore().createContainerInLocation(location, fileStoreConfig.container);

    log.info(
        "File store configured with provider: '{}', container: '{}' and location: '{}'.",
        fileStoreConfig.provider,
        fileStoreConfig.container,
        fileStoreConfig.location);
  }

  @PreDestroy
  public void cleanUp() {
    blobStoreContext.close();
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

  private record FileStoreConfig(String provider, String location, String container) {}

  public boolean blobExists(String key) {
    return key != null && getBlobStore().blobExists(getBlobContainer(), key);
  }

  public Blob getBlob(String key) {
    return getBlobStore().getBlob(getBlobContainer(), key);
  }

  public PageSet<? extends StorageMetadata> getBlobList(ListContainerOptions options) {
    return getBlobStore().list(getBlobContainer(), options);
  }

  public void putBlob(Blob blob) {
    getBlobStore().putBlob(getBlobContainer(), blob);
  }

  public void removeBlob(String key) {
    getBlobStore().removeBlob(getBlobContainer(), key);
  }

  public void deleteDirectory(String dirName) {
    getBlobStore().deleteDirectory(getBlobContainer(), dirName);
  }

  public String getBlobContainer() {
    return fileStoreConfig.container;
  }

  public BlobStore getBlobStore() {
    return blobStoreContext.getBlobStore();
  }

  public BlobRequestSigner getBlobRequestSigner() {
    return blobStoreContext.getSigner();
  }

  public boolean isUsingFileSystem() {
    return JCLOUDS_PROVIDER_KEY_FILESYSTEM.equals(fileStoreConfig.provider());
  }

  public boolean isUsingTransient() {
    return JCLOUDS_PROVIDER_KEY_TRANSIENT.equals(fileStoreConfig.provider());
  }
}
