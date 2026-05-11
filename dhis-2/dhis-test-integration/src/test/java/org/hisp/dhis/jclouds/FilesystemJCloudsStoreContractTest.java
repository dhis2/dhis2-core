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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.storage.BlobStoreService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Runs the {@link BlobStoreServiceContractTest} suite against the JClouds {@code filesystem}
 * provider, rooted at a temporary directory created in {@code @BeforeAll}.
 *
 * <p>Tagged {@code integration}: writes real files to disk on every test method.
 *
 * <p>The temp dir is created manually rather than via JUnit's {@code @TempDir} because non-static
 * {@code @TempDir} fields are injected too late to be visible inside {@code @BeforeAll}, which left
 * {@code tempDir} null at startup and corrupted Mockito's stubbing state for downstream tests in
 * the same suite.
 */
@Tag("integration")
class FilesystemJCloudsStoreContractTest extends BlobStoreServiceContractTest {

  private Path tempDir;
  private JCloudsStore store;

  @BeforeAll
  void start() throws IOException {
    tempDir = Files.createTempDirectory("dhis-fs-contract-");

    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient()
        .when(config.getProperty(ConfigurationKey.FILESTORE_PROVIDER))
        .thenReturn("filesystem");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("contract");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_LOCATION)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_ENDPOINT)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_IDENTITY)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_SECRET)).thenReturn("");

    LocationManager locationManager = mock(LocationManager.class);
    when(locationManager.externalDirectorySet()).thenReturn(true);
    when(locationManager.getExternalDirectoryPath()).thenReturn(tempDir.toString());

    store = new JCloudsStore(config, locationManager);
    store.init();
  }

  @AfterAll
  void stop() throws IOException {
    if (store != null) store.cleanUp();
    if (tempDir != null && Files.exists(tempDir)) {
      try (var paths = Files.walk(tempDir)) {
        paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
      }
    }
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
  protected boolean listKeysIncludesDirectoryMarkers() {
    // TODO(DHIS2-20648) jclouds-filesystem returns synthetic entries for parent directories
    // alongside real blobs. Drop this override once the replacement implementation
    // filters these out so listKeys returns blob keys only.
    return true;
  }
}
