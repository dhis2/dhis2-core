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

import java.time.Duration;
import java.util.function.LongSupplier;
import javax.annotation.CheckForNull;

/**
 * Per-call overrides for a blob read, so a caller under its own time limit can bound the read by
 * it. Stores that reach no remote system ignore them.
 *
 * <p>Asks the caller what is left rather than holding a duration, because one read makes several
 * calls to the store and they share the caller's limit, so each gets the remainder rather than the
 * whole of it again. The caller owns the clock, so there is none here to disagree with it.
 *
 * @param remainingNanos supplies the nanoseconds the read has left, or null to leave it to the
 *     store's own configuration
 */
public record BlobReadOptions(@CheckForNull LongSupplier remainingNanos) {

  private static final BlobReadOptions NONE = new BlobReadOptions(null);

  /** No overrides, the store's own configuration applies. */
  public static BlobReadOptions none() {
    return NONE;
  }

  /**
   * A read bounded by the caller's own limit, re-read before every call to the store.
   *
   * @param remainingNanos how much of that limit is left, negative once it is spent
   */
  public static BlobReadOptions remaining(LongSupplier remainingNanos) {
    return new BlobReadOptions(remainingNanos);
  }

  /**
   * What the next call to the store may take, or null if it is unbounded.
   *
   * @throws BlobReadTimeoutException if under a millisecond is left, the AWS SDK's resolution, so
   *     the call could only time out and is not made
   */
  @CheckForNull
  public Duration nextTimeout() {
    if (remainingNanos == null) {
      return null;
    }
    Duration remaining = Duration.ofNanos(remainingNanos.getAsLong());
    if (remaining.toMillis() <= 0) {
      throw new BlobReadTimeoutException("Blob read budget is spent");
    }
    return remaining;
  }
}
