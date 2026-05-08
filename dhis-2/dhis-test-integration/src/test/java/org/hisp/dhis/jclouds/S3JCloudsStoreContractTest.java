/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.storage.BlobStoreService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MinIOContainer;

/**
 * Runs the {@link BlobStoreServiceContractTest} suite against the JClouds {@code s3} provider,
 * pointed at a MinIO container started by Testcontainers. Validates the S3-shaped behaviours
 * (presigned URLs, server-side MD5 validation, custom endpoint + path-style addressing) that have
 * no other automated coverage today.
 *
 * <p>Lives in {@code dhis-test-integration} because it requires Docker. The {@code
 * BlobStoreServiceContractTest} base class is consumed via the {@code dhis-service-core} test-jar;
 * the {@code org.hisp.dhis.jclouds} package is shared across jars so the package-private {@link
 * JCloudsStore} constructor remains reachable.
 *
 * <p>The container is managed manually rather than via {@code @Testcontainers} because the {@code
 * org.testcontainers:junit-jupiter} integration artifact is not on the test classpath; only the
 * core {@code testcontainers} + {@code minio} modules are pulled in via {@code dhis-support-test}.
 */
@Tag("integration")
class S3JCloudsStoreContractTest extends BlobStoreServiceContractTest {

  private static final String USER = "testuser";
  private static final String PASSWORD = "testpassword";

  private MinIOContainer minio;
  private JCloudsStore store;

  @BeforeAll
  void start() {
    minio =
        new MinIOContainer("minio/minio:RELEASE.2025-04-22T22-12-26Z")
            .withUserName(USER)
            .withPassword(PASSWORD);
    minio.start();

    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_PROVIDER)).thenReturn("s3");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("contract");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_LOCATION)).thenReturn("eu-west-1");
    lenient()
        .when(config.getProperty(ConfigurationKey.FILESTORE_ENDPOINT))
        .thenReturn(minio.getS3URL());
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_IDENTITY)).thenReturn(USER);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_SECRET)).thenReturn(PASSWORD);

    LocationManager locationManager = mock(LocationManager.class);

    store = new JCloudsStore(config, locationManager);
    store.init();
  }

  @AfterAll
  void stop() {
    if (store != null) store.cleanUp();
    if (minio != null) minio.stop();
  }

  @Override
  protected BlobStoreService service() {
    return store;
  }

  @Override
  protected boolean supportsRequestSigning() {
    return true;
  }

  @Override
  protected boolean validatesContentMd5() {
    return true;
  }

  @Override
  protected boolean supportsRecursiveDirectoryDelete() {
    // TODO(DHIS2-20648) jclouds-S3 deleteDirectory is non-recursive — JCloudsAppStorageService
    // works around it by enumerating + deleting individually. Drop this override once the
    // replacement implementation does recursive deleteDirectory on S3.
    return false;
  }
}
