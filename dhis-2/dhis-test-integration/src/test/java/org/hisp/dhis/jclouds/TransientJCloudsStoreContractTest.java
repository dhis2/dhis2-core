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

/**
 * Runs the {@link BlobStoreServiceContractTest} suite against the JClouds {@code transient}
 * (in-memory) provider — the backend used by H2 and Postgres integration tests today.
 *
 * <p>Tagged {@code integration}: although it uses an in-memory backend, it exercises full jclouds
 * machinery and runs ~25 real I/O round-trips per class — too heavy for the unit-test run. Belongs
 * alongside the other contract subclasses that validate cross-backend conformance.
 */
@Tag("integration")
class TransientJCloudsStoreContractTest extends BlobStoreServiceContractTest {

  private JCloudsStore store;

  @BeforeAll
  void start() {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_PROVIDER)).thenReturn("transient");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("contract");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_LOCATION)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_ENDPOINT)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_IDENTITY)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_SECRET)).thenReturn("");

    LocationManager locationManager = mock(LocationManager.class);

    store = new JCloudsStore(config, locationManager);
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
    return false;
  }

  @Override
  protected boolean supportsRecursiveDirectoryDelete() {
    // TODO(DHIS2-20648) jclouds-transient deleteDirectory is non-recursive in practice; same
    // limitation as jclouds-S3. Drop this override once the replacement BlobStoreService
    // implementation does recursive deleteDirectory on every backend.
    return false;
  }
}
