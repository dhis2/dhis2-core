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
 * An exact key identifying a single blob in the store (e.g. {@code
 * dataValue/b38d3f6c-7e2a-4d1e-a9f0-12345678abcd}).
 *
 * <p>Keys are relative paths — they must not start with {@code /}. The constructor enforces this
 * so that {@code container + "/" + key.value()} is always safe without additional slash-cleaning.
 *
 * <p>Use {@link #of(String, String...)} to build a key from multiple path segments. Prefer it over
 * manual string concatenation to keep key construction readable and consistent.
 *
 * <p>Distinct from {@link BlobKeyPrefix}, which identifies a namespace for listing or bulk
 * deletion.
 */
public record BlobKey(String value) {

  public BlobKey {
    Objects.requireNonNull(value, "BlobKey value must not be null");
    if (value.startsWith("/")) {
      throw new IllegalArgumentException("BlobKey value must not start with '/': " + value);
    }
  }

  /**
   * Builds a key by joining {@code first} and any additional {@code more} segments with {@code /}.
   * Prefer this over manual concatenation when assembling a key from multiple parts.
   *
   * <p>Example: {@code BlobKey.of("apps", "my-app", "index.html")} →
   * {@code "apps/my-app/index.html"}.
   */
  public static BlobKey of(String first, String... more) {
    if (more.length == 0) return new BlobKey(first);
    StringBuilder sb = new StringBuilder(first);
    for (String segment : more) sb.append('/').append(segment);
    return new BlobKey(sb.toString());
  }

  @Nonnull
  @Override
  public String toString() {
    return value;
  }
}
