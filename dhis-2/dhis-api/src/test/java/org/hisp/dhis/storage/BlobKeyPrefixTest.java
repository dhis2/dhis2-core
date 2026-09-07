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

import org.junit.jupiter.api.Test;

class BlobKeyPrefixTest {

  // -------------------------------------------------------------------------
  // Constructor validation
  // -------------------------------------------------------------------------

  @Test
  void nullValueIsRejected() {
    assertThrows(NullPointerException.class, () -> new BlobKeyPrefix(null));
  }

  @Test
  void leadingSlashIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new BlobKeyPrefix("/apps/my-app"));
  }

  @Test
  void trailingSlashIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new BlobKeyPrefix("apps/my-app/"));
  }

  @Test
  void validValueIsAccepted() {
    BlobKeyPrefix prefix = new BlobKeyPrefix("apps/my-app");
    assertEquals("apps/my-app", prefix.value());
  }

  // -------------------------------------------------------------------------
  // of() — normalisation
  // -------------------------------------------------------------------------

  @Test
  void ofStripsTrailingSlash() {
    assertEquals("apps/my-app", BlobKeyPrefix.of("apps/my-app/").value());
  }

  @Test
  void ofStripsLeadingSlash() {
    assertEquals("apps/my-app", BlobKeyPrefix.of("/apps/my-app").value());
  }

  @Test
  void ofStripsLeadingAndTrailingSlash() {
    assertEquals("apps/my-app", BlobKeyPrefix.of("/apps/my-app/").value());
  }

  @Test
  void ofWithCleanValuePassesThroughUnchanged() {
    assertEquals("apps/my-app", BlobKeyPrefix.of("apps/my-app").value());
  }

  // -------------------------------------------------------------------------
  // resolve / toString
  // -------------------------------------------------------------------------

  @Test
  void resolveProducesCorrectBlobKey() {
    BlobKey key = BlobKeyPrefix.of("apps/my-app").resolve("manifest.webapp");
    assertEquals("apps/my-app/manifest.webapp", key.value());
  }

  @Test
  void toStringReturnValue() {
    assertEquals("apps/my-app", BlobKeyPrefix.of("apps/my-app").toString());
  }

  // -------------------------------------------------------------------------
  // APPS constant
  // -------------------------------------------------------------------------

  @Test
  void appsConstantHasCorrectValue() {
    assertEquals("apps", BlobKeyPrefix.APPS.value());
  }
}
