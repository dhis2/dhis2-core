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

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * A key prefix identifying a namespace (logical directory) within the blob store — used for listing
 * and bulk deletion (e.g. {@code apps/my-app_abc123}).
 *
 * <p>Values do <em>not</em> carry a trailing {@code /}; implementations add it internally when the
 * underlying API requires it.
 *
 * <p>Distinct from {@link BlobKey}, which identifies a single blob.
 */
public record BlobKeyPrefix(String value) {

  public BlobKeyPrefix {
    Objects.requireNonNull(value, "BlobKeyPrefix value must not be null");
  }

  /**
   * Factory equivalent to {@code new BlobKeyPrefix(value)}; prefer this for readability at
   * call-sites where the intent is to create a prefix rather than a literal string.
   */
  public static BlobKeyPrefix of(String value) {
    return new BlobKeyPrefix(value);
  }

  /**
   * Returns the {@link BlobKey} for a named file directly inside this prefix (i.e. {@code
   * value/filename}). Use this to address a specific file whose name is known, rather than
   * constructing the key by hand.
   */
  public BlobKey resolve(String filename) {
    return BlobKey.of(value, filename);
  }

  @Nonnull
  @Override
  public String toString() {
    return value;
  }
}
