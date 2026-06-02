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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransientBlobStoreService} edge cases that the contract test doesn't probe.
 */
class TransientBlobStoreServiceTest {

  @Test
  void putBlob_streamShorterThanContentLength_throws() {
    BlobStoreService svc = newService();
    byte[] payload = "abc".getBytes();
    ByteArrayInputStream bais = new ByteArrayInputStream(payload);
    BlobKey key = BlobKey.of("k");
    // Declare 10 bytes but only provide 3 — readNBytes returns the 3 it could read.
    UncheckedIOException ex =
        assertThrows(
            UncheckedIOException.class, () -> svc.putBlob(key, bais, 10L, null, null, null));
    // Sanity-check the wrapped IOException message references the mismatch.
    assertTrue(
        ex.getCause().getMessage().contains("Expected 10"),
        "expected wrapped IOException to mention declared length, got: "
            + ex.getCause().getMessage());
  }

  @Test
  void putBlob_contentLengthAboveInt_throws() {
    // Use Long.MAX_VALUE so the size-limit check passes and the Integer.MAX_VALUE guard fires.
    BlobStoreService svc = newServiceWithMaxBytes(Long.MAX_VALUE);
    long tooBig = (long) Integer.MAX_VALUE + 1;
    BlobKey key = BlobKey.of("k");
    ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
    assertThrows(ArithmeticException.class, () -> svc.putBlob(key, bais, tooBig, null, null, null));
  }

  @Test
  void putBlob_exceedsMaxFileUploadSize_throws() {
    long maxBytes = 100L;
    BlobStoreService svc = newServiceWithMaxBytes(maxBytes);
    BlobKey key = BlobKey.of("k");
    ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> svc.putBlob(key, bais, maxBytes + 1, null, null, null));
    assertTrue(
        ex.getMessage().contains("File size can't be bigger than"), "got: " + ex.getMessage());
  }

  private static TransientBlobStoreService newService() {
    return newServiceWithMaxBytes(
        Long.parseLong(ConfigurationKey.MAX_FILE_UPLOAD_SIZE_BYTES.getDefaultValue()));
  }

  private static TransientBlobStoreService newServiceWithMaxBytes(long maxBytes) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    lenient().when(config.getProperty(ConfigurationKey.FILESTORE_CONTAINER)).thenReturn("dhis2");
    lenient()
        .when(config.getProperty(ConfigurationKey.MAX_FILE_UPLOAD_SIZE_BYTES))
        .thenReturn(Long.toString(maxBytes));
    return new TransientBlobStoreService(config);
  }
}
