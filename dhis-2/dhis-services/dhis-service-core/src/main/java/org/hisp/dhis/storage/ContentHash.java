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

import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A hex-encoded MD5 content hash used for integrity verification on upload. Object stores such as
 * S3 validate the received bytes against this value and reject the upload if they disagree.
 */
public record ContentHash(String hex) {

  private static final Pattern MD5_PATTERN = Pattern.compile("[0-9a-fA-F]{32}");

  public ContentHash {
    if (!MD5_PATTERN.matcher(hex).matches()) {
      throw new IllegalArgumentException(
          "contentHash must be a 32-character hex string, got: '" + hex + "'");
    }
  }

  /**
   * Returns a {@code ContentHash} for the given hex string, or {@code null} if the string is {@code
   * null} or blank. Use this when the hash may not be available.
   */
  @CheckForNull
  public static ContentHash ofNullable(@CheckForNull String hex) {
    return (hex == null || hex.isBlank()) ? null : new ContentHash(hex);
  }

  @Nonnull
  @Override
  public String toString() {
    return hex;
  }
}
