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
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.http.HttpResponseException;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.s3.reference.S3Constants;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JCloudsStore {
  private static final Pattern CONTAINER_NAME_PATTERN =
      Pattern.compile("^(?![.-])(?=.{1,63})([.-]?[a-zA-Z0-9]+)+$");

  private static final String JCLOUDS_PROVIDER_KEY_FILESYSTEM = "filesystem";
  private static final String JCLOUDS_PROVIDER_KEY_AWS_S3 = "aws-s3";
  private static final String JCLOUDS_PROVIDER_KEY_TRANSIENT = "transient";
  private static final List<String> SUPPORTED_PROVIDERS =
      List.of(
          JCLOUDS_PROVIDER_KEY_FILESYSTEM,
          JCLOUDS_PROVIDER_KEY_AWS_S3,
          JCLOUDS_PROVIDER_KEY_TRANSIENT);

  private BlobStoreContext blobStoreContext;
  private BlobStoreProperties config;

  private final LocationManager locationManager;

  private final DhisConfigurationProvider configurationProvider;

  @PostConstruct
  public void init() {
    config =
        new BlobStoreProperties(
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_PROVIDER),
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_LOCATION),
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_ENDPOINT),
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER));

    Pair<Credentials, Properties> providerConfig =
        configureForProvider(
            config,
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_IDENTITY),
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_SECRET));

    blobStoreContext =
        ContextBuilder.newBuilder(config.provider)
            .credentials(providerConfig.getLeft().identity, providerConfig.getLeft().credential)
            .overrides(providerConfig.getRight())
            .build(BlobStoreContext.class);

    BlobStore blobStore = blobStoreContext.getBlobStore();
    Location provider =
        new LocationBuilder()
            .scope(LocationScope.PROVIDER)
            .id(config.provider)
            .description(config.provider)
            .build();
    try {
      blobStore.createContainerInLocation(createRegionLocation(config, provider), config.container);

      log.info(
          String.format(
              "File store configured with provider: '%s', container: '%s' and location: '%s'.",
              config.provider, config.container, config.location));
    } catch (HttpResponseException ex) {
      log.error(
          String.format(
              "Could not configure file store with provider '%s' and container '%s'.\n"
                  + "File storage will not be available.",
              config.provider, config.container),
          ex);
    } catch (AuthorizationException ex) {
      log.error(
          String.format(
              "Could not authenticate with file store provider '%s' and container '%s'. "
                  + "File storage will not be available.",
              config.provider, config.location),
          ex);
    }
  }

  @PreDestroy
  public void cleanUp() {
    blobStoreContext.close();
  }

  private static Location createRegionLocation(BlobStoreProperties config, Location provider) {
    return config.location != null
        ? new LocationBuilder()
            .scope(LocationScope.REGION)
            .id(config.location)
            .description(config.location)
            .parent(provider)
            .build()
        : null;
  }

  private Pair<Credentials, Properties> configureForProvider(
      BlobStoreProperties properties, String identity, String secret) {
    Properties overrides = new Properties();
    Credentials credentials = new Credentials("Unused", "Unused");

    if (properties.provider.equals(JCLOUDS_PROVIDER_KEY_FILESYSTEM)
        && locationManager.externalDirectorySet()) {
      overrides.setProperty(
          FilesystemConstants.PROPERTY_BASEDIR, locationManager.getExternalDirectoryPath());
    } else if (properties.provider.equals(JCLOUDS_PROVIDER_KEY_AWS_S3)) {
      overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");

      if (!properties.endpoint.isEmpty()) {
        overrides.setProperty(PROPERTY_ENDPOINT, properties.endpoint);
      }

      credentials = new Credentials(identity, secret);
      if (credentials.identity.isEmpty() || credentials.credential.isEmpty()) {
        log.warn("AWS S3 store configured without credentials, authentication not possible.");
      }
    }

    return Pair.of(credentials, overrides);
  }

  private class BlobStoreProperties {
    private String provider;
    private final String location;
    private final String endpoint;
    private String container;

    BlobStoreProperties(String provider, String location, String endpoint, String container) {
      this.provider = provider;
      this.location = location;
      this.endpoint = endpoint;
      this.container = container;

      validate();
      validateAndSelectProvider();
    }

    private void validate() {
      if (!isValidContainerName(container)) {
        if (container != null) {
          log.warn(
              String.format(
                  "Container name '%s' is illegal. "
                      + "Standard domain name naming conventions apply (no underscores allowed). "
                      + "Using default container name ' %s'",
                  container, ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue()));
        }

        container = ConfigurationKey.FILESTORE_CONTAINER.getDefaultValue();
      }
    }

    private boolean isValidContainerName(String containerName) {
      return containerName != null && CONTAINER_NAME_PATTERN.matcher(containerName).matches();
    }

    private void validateAndSelectProvider() {
      if (!SUPPORTED_PROVIDERS.contains(provider)) {
        log.warn(
            "Ignored unsupported file store provider '"
                + provider
                + "', using file system provider.");
        provider = JCLOUDS_PROVIDER_KEY_FILESYSTEM;
      }

      if (provider.equals(JCLOUDS_PROVIDER_KEY_FILESYSTEM)
          && !locationManager.externalDirectorySet()) {
        log.info(
            "File system file store provider could not be configured; external directory is not set. "
                + "Falling back to in-memory provider.");
        provider = JCLOUDS_PROVIDER_KEY_TRANSIENT;
      }
    }
  }

  public String getBlobContainer() {
    return config.container;
  }

  public BlobStore getBlobStore() {
    return blobStoreContext.getBlobStore();
  }

  public BlobRequestSigner getBlobRequestSigner() {
    return blobStoreContext.getSigner();
  }
}
