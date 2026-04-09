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

import javax.annotation.Nonnull;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.storage.BlobKey;
import org.hisp.dhis.storage.BlobKeyPrefix;

/**
 * The blob-store path of an installed app's folder (e.g. {@code apps/my-app_abc123xyz}).
 *
 * <p>The segment after {@code apps/} is capped at {@value #MAX_SEGMENT_LENGTH} characters so that
 * the full path stays well within filesystem limits. When the app key is shorter than the cap, a
 * random secure token is appended to avoid collisions between re-installs.
 *
 * <p>The {@code path} component must not be null or blank.
 */
public record AppFolderName(String path) {

  /** Maximum number of characters for the folder segment after {@code apps/}. */
  static final int MAX_SEGMENT_LENGTH = 255;

  public AppFolderName {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("AppFolderName path must not be null or blank");
    }
    if (!path.startsWith(AppStorageService.APPS_DIR + "/")) {
      throw new IllegalArgumentException(
          "AppFolderName path must start with " + AppStorageService.APPS_DIR + "/");
    }
    if (path.length() > MAX_SEGMENT_LENGTH + AppStorageService.APPS_DIR.length() + 1) {
      throw new IllegalArgumentException(
          "AppFolderName path must not exceed " + MAX_SEGMENT_LENGTH + " characters");
    }
  }

  /**
   * Derives an installation folder for the given app key. The segment after {@code apps/} is always
   * exactly {@value #MAX_SEGMENT_LENGTH} characters:
   *
   * <ul>
   *   <li>Keys longer than the cap are truncated to exactly {@value #MAX_SEGMENT_LENGTH} chars.
   *   <li>Shorter keys get {@code key_<token>} where the token is trimmed so the total segment
   *       length is {@value #MAX_SEGMENT_LENGTH}, ensuring uniqueness across re-installs.
   * </ul>
   */
  public static AppFolderName ofKey(@Nonnull String appKey) {
    String segment;
    if (appKey.length() >= MAX_SEGMENT_LENGTH) {
      segment = appKey.substring(0, MAX_SEGMENT_LENGTH);
    } else {
      String token = CodeGenerator.getRandomSecureToken();
      int maxTokenLen = MAX_SEGMENT_LENGTH - appKey.length() - 1; // -1 for the "_" separator
      segment = appKey + "_" + token.substring(0, Math.min(maxTokenLen, token.length()));
    }
    return new AppFolderName(AppStorageService.APPS_DIR + "/" + segment);
  }

  /**
   * Returns the {@link BlobKey} for a resource path relative to this folder. A leading {@code /} is
   * stripped so that HTTP-style paths (e.g. {@code /index.html}) and bare paths (e.g. {@code
   * index.html}) both resolve correctly.
   */
  public BlobKey resolve(String resource) {
    String rel = resource.startsWith("/") ? resource.substring(1) : resource;
    return BlobKey.of(path, rel);
  }

  /**
   * Returns a {@link BlobKeyPrefix} covering all blobs under this folder, suitable for passing to
   * {@link org.hisp.dhis.storage.BlobStoreService#listKeys} or {@link
   * org.hisp.dhis.storage.BlobStoreService#deleteDirectory}.
   */
  public BlobKeyPrefix asPrefix() {
    return new BlobKeyPrefix(path);
  }

  @Nonnull
  @Override
  public String toString() {
    return path;
  }
}
