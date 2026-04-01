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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppFolderNameTest {

  @Test
  void shortKeyProducesFolderWithAppsPrefix() {
    AppFolderName folder = AppFolderName.forApp("my-app");
    assertTrue(folder.value().startsWith("apps/my-app_"), folder.value());
  }

  @Test
  void shortKeyAppendsRandomToken() {
    // The segment after "apps/" must be "key_<token>"; token is non-empty
    AppFolderName folder = AppFolderName.forApp("my-app");
    String segment = folder.value().substring("apps/".length());
    assertTrue(segment.startsWith("my-app_"));
    assertTrue(segment.length() > "my-app_".length());
  }

  @Test
  void longKeyIsTruncatedToMaxSegmentLength() {
    String longKey = "a".repeat(AppFolderName.MAX_SEGMENT_LENGTH + 10);
    AppFolderName folder = AppFolderName.forApp(longKey);
    String segment = folder.value().substring("apps/".length());
    assertEquals(AppFolderName.MAX_SEGMENT_LENGTH, segment.length());
  }

  @Test
  void longKeySegmentIsExactPrefixOfOriginalKey() {
    String longKey = "abcdefghij".repeat(5); // 50 chars
    AppFolderName folder = AppFolderName.forApp(longKey);
    String segment = folder.value().substring("apps/".length());
    assertEquals(longKey.substring(0, AppFolderName.MAX_SEGMENT_LENGTH), segment);
  }

  @Test
  void resolveProducesCorrectBlobKey() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123/manifest.webapp", folder.resolve("manifest.webapp").value());
  }

  @Test
  void asPrefixProducesCorrectBlobKeyPrefix() {
    AppFolderName folder = new AppFolderName("apps/my-app_abc123");
    assertEquals("apps/my-app_abc123", folder.asPrefix().value());
  }

  @Test
  void twoCallsForSameShortKeyProduceDifferentFolders() {
    // Random token ensures uniqueness across installs of the same app
    AppFolderName a = AppFolderName.forApp("my-app");
    AppFolderName b = AppFolderName.forApp("my-app");
    assertNotEquals(a.value(), b.value(), "Expected different folders but got: " + a.value());
  }
}
