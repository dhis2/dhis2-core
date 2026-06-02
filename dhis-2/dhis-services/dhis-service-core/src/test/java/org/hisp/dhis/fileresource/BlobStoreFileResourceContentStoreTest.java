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
package org.hisp.dhis.fileresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.storage.BlobStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the {@code MAX_FILE_UPLOAD_SIZE_BYTES} enforcement in {@link
 * BlobStoreFileResourceContentStore}. The size check is the only behaviour worth isolating at this
 * level — the rest of {@code saveFileResourceContent} is thin delegation to {@link
 * BlobStoreService}, which is covered by the storage contract tests.
 */
class BlobStoreFileResourceContentStoreTest {

  private static final long MAX_BYTES = 100L;

  @Test
  void saveFileResourceContent_bytes_oversize_throwsAndSkipsBlobStore() {
    BlobStoreService blobStore = mock(BlobStoreService.class);
    BlobStoreFileResourceContentStore store = newStore(blobStore);

    byte[] payload = new byte[(int) (MAX_BYTES + 1)];
    FileResource fr = newFileResource();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> store.saveFileResourceContent(fr, payload));
    assertTrue(ex.getMessage().contains("File size can't be bigger than"), ex.getMessage());
    verify(blobStore, never()).putBlob(any(), any(), anyLong(), any(), any(), any());
  }

  @Test
  void saveFileResourceContent_file_oversize_throwsAndSkipsBlobStore(@TempDir Path tempDir)
      throws IOException {
    BlobStoreService blobStore = mock(BlobStoreService.class);
    BlobStoreFileResourceContentStore store = newStore(blobStore);

    File file = writeFile(tempDir, "big.bin", (int) (MAX_BYTES + 1));
    FileResource fr = newFileResource();

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> store.saveFileResourceContent(fr, file));
    assertTrue(ex.getMessage().contains("File size can't be bigger than"), ex.getMessage());
    verify(blobStore, never()).putBlob(any(), any(), anyLong(), any(), any(), any());
    // File should not have been deleted since save did not complete.
    assertTrue(file.exists(), "temp file should remain after rejected upload");
  }

  @Test
  void saveFileResourceContent_imageMap_oversize_throwsAndSkipsBlobStore(@TempDir Path tempDir)
      throws IOException {
    BlobStoreService blobStore = mock(BlobStoreService.class);
    BlobStoreFileResourceContentStore store = newStore(blobStore);

    File big = writeFile(tempDir, "big.png", (int) (MAX_BYTES + 1));
    FileResource fr = newFileResource();
    Map<ImageFileDimension, File> images = Map.of(ImageFileDimension.ORIGINAL, big);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> store.saveFileResourceContent(fr, images));
    assertTrue(ex.getMessage().contains("File size can't be bigger than"), ex.getMessage());
    verify(blobStore, never()).putBlob(any(), any(), anyLong(), any(), any(), any());
  }

  @Test
  void saveFileResourceContent_atOrUnderLimit_succeeds(@TempDir Path tempDir) throws IOException {
    BlobStoreService blobStore = mock(BlobStoreService.class);
    BlobStoreFileResourceContentStore store = newStore(blobStore);

    FileResource fr = newFileResource();

    // byte[] overload — exactly at the limit
    byte[] atLimit = new byte[(int) MAX_BYTES];
    assertEquals(fr.getStorageKey(), store.saveFileResourceContent(fr, atLimit));

    // File overload — under the limit (writeFile deletes after save; recreate)
    File file = writeFile(tempDir, "ok.bin", (int) MAX_BYTES - 1);
    assertEquals(fr.getStorageKey(), store.saveFileResourceContent(fr, file));
  }

  private static BlobStoreFileResourceContentStore newStore(BlobStoreService blobStore) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient()
        .when(config.getProperty(ConfigurationKey.MAX_FILE_UPLOAD_SIZE_BYTES))
        .thenReturn(Long.toString(MAX_BYTES));
    return new BlobStoreFileResourceContentStore(blobStore, config);
  }

  private static FileResource newFileResource() {
    FileResource fr = new FileResource();
    fr.setName("test.bin");
    fr.setContentType("application/octet-stream");
    fr.setStorageKey("storage-key-1");
    fr.setUid("uid000000001");
    return fr;
  }

  private static File writeFile(Path dir, String name, int size) throws IOException {
    Path p = dir.resolve(name);
    Files.write(p, new byte[size]);
    return p.toFile();
  }
}
