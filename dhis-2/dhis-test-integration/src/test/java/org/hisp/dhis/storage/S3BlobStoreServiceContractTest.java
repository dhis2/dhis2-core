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
package org.hisp.dhis.storage;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.junit.MinIOTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs the {@link BlobStoreServiceContractTest} suite against {@link S3BlobStoreService}, pointed
 * at the MinIO container managed by {@link MinIOTestExtension}. Validates the S3-shaped behaviours
 * (presigned URLs, server-side MD5 validation, custom endpoint + path-style addressing, recursive
 * deleteDirectory).
 *
 * <p>Tagged {@code integration} because it requires Docker.
 */
@Tag("integration")
@ExtendWith(MinIOTestExtension.class)
class S3BlobStoreServiceContractTest extends BlobStoreServiceContractTest {

  private S3BlobStoreService store;

  @BeforeAll
  void start() {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("contract");
    // FILESTORE_LOCATION intentionally not stubbed (mock returns null). S3BlobStoreService falls
    // back to us-east-1, which sends no LocationConstraint on bucket creation — newer MinIO
    // releases (>= RELEASE.2025-04-22) reject mismatched regions.
    lenient()
        .when(config.getProperty(ConfigurationKey.FILESTORE_ENDPOINT))
        .thenReturn(MinIOTestExtension.s3Url());
    lenient()
        .when(config.getProperty(ConfigurationKey.FILESTORE_IDENTITY))
        .thenReturn(MinIOTestExtension.MINIO_USER);
    lenient()
        .when(config.getProperty(ConfigurationKey.FILESTORE_SECRET))
        .thenReturn(MinIOTestExtension.MINIO_PASSWORD);

    store = new S3BlobStoreService(config);
    store.init();
  }

  @AfterAll
  void stop() {
    if (store != null) store.cleanUp();
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
}
