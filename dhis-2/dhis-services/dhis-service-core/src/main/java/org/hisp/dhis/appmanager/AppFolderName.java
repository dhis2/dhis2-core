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
 */
public record AppFolderName(String value) {

  /** Maximum number of characters for the folder segment after {@code apps/}. */
  static final int MAX_SEGMENT_LENGTH = 32;

  /**
   * Derives an installation folder for the given app key. The result is unique per install because
   * a random token is appended when the key fits within the length cap.
   */
  public static AppFolderName forApp(String appKey) {
    String segment =
        appKey.length() > MAX_SEGMENT_LENGTH
            ? appKey.substring(0, MAX_SEGMENT_LENGTH)
            : appKey + "_" + CodeGenerator.getRandomSecureToken();
    return new AppFolderName(AppStorageService.APPS_DIR + "/" + segment);
  }

  /** Returns the exact {@link BlobKey} for a file directly inside this folder. */
  public BlobKey resolve(String filename) {
    return new BlobKey(value + "/" + filename);
  }

  /**
   * Returns a {@link BlobKeyPrefix} covering all blobs under this folder, suitable for passing to
   * {@link org.hisp.dhis.storage.BlobStoreService#listKeys} or {@link
   * org.hisp.dhis.storage.BlobStoreService#deleteDirectory}.
   */
  public BlobKeyPrefix asPrefix() {
    return new BlobKeyPrefix(value);
  }

  @Nonnull
  @Override
  public String toString() {
    return value;
  }
}
