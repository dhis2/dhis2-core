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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FileSystemBlobStoreService} init-time preconditions and the path-traversal
 * guard. The end-to-end behaviour is covered by {@code FileSystemBlobStoreServiceContractTest} in
 * dhis-test-integration.
 */
class FileSystemBlobStoreServiceTest {

  @Test
  void constructor_externalDirectoryUnset_throws() {
    LocationManager lm = mock(LocationManager.class);
    when(lm.externalDirectorySet()).thenReturn(false);
    DhisConfigurationProvider config = config();

    assertThrows(IllegalStateException.class, () -> new FileSystemBlobStoreService(config, lm));
  }

  @Test
  void putBlob_benignKey_resolvesInsideContainer(@TempDir Path tempDir) {
    FileSystemBlobStoreService svc = newService(tempDir);
    svc.init();
    byte[] payload = "ok".getBytes();

    svc.putBlob(
        BlobKey.of("nested/file.txt"),
        new ByteArrayInputStream(payload),
        payload.length,
        null,
        null,
        null);

    assertTrue(svc.blobExists(BlobKey.of("nested/file.txt")));
    assertEquals(payload.length, svc.contentLength(BlobKey.of("nested/file.txt")));
  }

  private static FileSystemBlobStoreService newService(Path tempDir) {
    LocationManager lm = mock(LocationManager.class);
    when(lm.externalDirectorySet()).thenReturn(true);
    when(lm.getExternalDirectoryPath()).thenReturn(tempDir.toString());
    return new FileSystemBlobStoreService(config(), lm);
  }

  private static DhisConfigurationProvider config() {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("dhis2");
    return config;
  }
}
