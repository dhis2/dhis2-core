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
package org.hisp.dhis.appmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppFolderNameTest {

  // -------------------------------------------------------------------------
  // Constructor validation
  // -------------------------------------------------------------------------

  @Test
  void nullPathIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new AppFolderName(null));
  }

  @Test
  void blankPathIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new AppFolderName("   "));
  }

  @Test
  void pathWithoutAppsPrefixIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new AppFolderName("other/my-app_abc123"));
  }

  @Test
  void pathTooLongIsRejected() {
    // Max length = APPS_DIR.length() + 1 + MAX_SEGMENT_LENGTH = 4 + 1 + 32 = 37
    String tooLong = "apps/" + "a".repeat(AppFolderName.MAX_SEGMENT_LENGTH + 1);
    assertThrows(IllegalArgumentException.class, () -> new AppFolderName(tooLong));
  }

  @Test
  void pathAtExactMaxLengthIsAccepted() {
    String exactMax = "apps/" + "a".repeat(AppFolderName.MAX_SEGMENT_LENGTH);
    assertDoesNotThrow(() -> new AppFolderName(exactMax));
  }

  @Test
  void validPathIsAccepted() {
    assertDoesNotThrow(() -> new AppFolderName("apps/my-app_abc123"));
  }

  // -------------------------------------------------------------------------
  // ofKey factory
  // -------------------------------------------------------------------------

  @Test
  void shortKeyProducesFolderWithAppsPrefix() {
    AppFolderName folder = AppFolderName.ofKey("my-app");
    assertTrue(folder.path().startsWith("apps/my-app_"), folder.path());
  }

  @Test
  void shortKeySegmentIsExactlyMaxLength() {
    // Token is truncated so the segment after "apps/" is always MAX_SEGMENT_LENGTH chars
    AppFolderName folder = AppFolderName.ofKey("my-app");
    String segment = folder.path().substring("apps/".length());
    assertEquals(AppFolderName.MAX_SEGMENT_LENGTH, segment.length());
  }

  @Test
  void shortKeyAppendsRandomToken() {
    AppFolderName folder = AppFolderName.ofKey("my-app");
    String segment = folder.path().substring("apps/".length());
    // Segment format is "key_<token>"
    assertTrue(segment.startsWith("my-app_"), segment);
    // Token portion is non-empty
    assertTrue(segment.length() > "my-app_".length(), segment);
  }

  @Test
  void longKeyIsTruncatedToMaxSegmentLength() {
    String longKey = "a".repeat(AppFolderName.MAX_SEGMENT_LENGTH + 10);
    AppFolderName folder = AppFolderName.ofKey(longKey);
    String segment = folder.path().substring("apps/".length());
    assertEquals(AppFolderName.MAX_SEGMENT_LENGTH, segment.length());
  }

  @Test
  void longKeySegmentIsExactPrefixOfOriginalKey() {
    String longKey = "abcdefghij".repeat(5); // 50 chars
    AppFolderName folder = AppFolderName.ofKey(longKey);
    String segment = folder.path().substring("apps/".length());
    assertEquals(longKey.substring(0, AppFolderName.MAX_SEGMENT_LENGTH), segment);
  }

  @Test
  void twoCallsForSameShortKeyProduceDifferentFolders() {
    // Truncated token still has enough entropy to differ between calls
    AppFolderName a = AppFolderName.ofKey("my-app");
    AppFolderName b = AppFolderName.ofKey("my-app");
    assertNotEquals(a.path(), b.path(), "Expected different folders but got: " + a.path());
  }

  // -------------------------------------------------------------------------
  // resolve / asPrefix / toString
  // -------------------------------------------------------------------------

  @Test
  void resolveProducesCorrectBlobKey() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123/manifest.webapp", folder.resolve("manifest.webapp").value());
  }

  @Test
  void resolveStripsLeadingSlash() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123/index.html", folder.resolve("/index.html").value());
  }

  @Test
  void asPrefixProducesCorrectBlobKeyPrefix() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123", folder.asPrefix().value());
  }

  @Test
  void toStringReturnPath() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123", folder.toString());
  }
}
