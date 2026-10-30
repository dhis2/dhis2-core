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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies {@link BlobStoreConfig} selects the right {@link BlobStoreService} implementation. */
class BlobStoreConfigTest {

  @Test
  void selectsS3ForS3Provider() {
    BlobStoreService svc =
        new BlobStoreConfig().blobStoreService(config("s3"), mock(LocationManager.class));
    assertInstanceOf(S3BlobStoreService.class, svc);
    ((S3BlobStoreService) svc).cleanUp();
  }

  @Test
  void selectsS3ForAwsS3Provider() {
    BlobStoreService svc =
        new BlobStoreConfig().blobStoreService(config("aws-s3"), mock(LocationManager.class));
    assertInstanceOf(S3BlobStoreService.class, svc);
    ((S3BlobStoreService) svc).cleanUp();
  }

  @Test
  void selectsFileSystemForFilesystemProvider(@TempDir Path tempDir) {
    LocationManager lm = mock(LocationManager.class);
    when(lm.externalDirectorySet()).thenReturn(true);
    when(lm.getExternalDirectoryPath()).thenReturn(tempDir.toString());

    BlobStoreService svc = new BlobStoreConfig().blobStoreService(config("filesystem"), lm);
    assertInstanceOf(FileSystemBlobStoreService.class, svc);
  }

  @Test
  void selectsTransientForTransientProvider() {
    BlobStoreService svc =
        new BlobStoreConfig().blobStoreService(config("transient"), mock(LocationManager.class));
    assertInstanceOf(TransientBlobStoreService.class, svc);
  }

  @Test
  void rejectsUnknownProvider() {
    LocationManager locationManager = mock(LocationManager.class);
    BlobStoreConfig blobStoreConfig = new BlobStoreConfig();
    DhisConfigurationProvider dhisConfigWithS4 = config("s4");
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> blobStoreConfig.blobStoreService(dhisConfigWithS4, locationManager));
    assertTrue(e.getMessage().contains("s4"));
  }

  private static DhisConfigurationProvider config(String provider) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_PROVIDER)).thenReturn(provider);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("dhis2");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_LOCATION)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_ENDPOINT)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_IDENTITY)).thenReturn("");
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_SECRET)).thenReturn("");
    return config;
  }
}
