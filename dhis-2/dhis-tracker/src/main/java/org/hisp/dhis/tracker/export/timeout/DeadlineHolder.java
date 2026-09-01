/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.timeout;

import javax.annotation.CheckForNull;

/**
 * Holds the {@link Deadline} of the tracker export request being served on this thread.
 *
 * <p>A {@link ThreadLocal} because the enforcement point, {@code
 * DeadlineAwareJdbcTemplate#applyStatementSettings}, has no parameter to read a deadline from.
 *
 * <p>Tomcat reuses worker threads, so {@link #clear()} must run for every request that called
 * {@link #set}, whatever the outcome.
 *
 * <p>An unset deadline means unbounded, so a lost deadline degrades to today's behaviour rather
 * than to wrongly cancelled queries. That is also why the background sync job, which shares the
 * export read path but runs on no HTTP request, is unaffected.
 */
public final class DeadlineHolder {

  private static final ThreadLocal<Deadline> DEADLINE = new ThreadLocal<>();

  private DeadlineHolder() {
    throw new UnsupportedOperationException("utility class");
  }

  /** Null if this thread's request is unbounded. */
  @CheckForNull
  public static Deadline get() {
    return DEADLINE.get();
  }

  /** Pass null to leave the request unbounded. */
  public static void set(@CheckForNull Deadline deadline) {
    if (deadline == null) {
      DEADLINE.remove();
    } else {
      DEADLINE.set(deadline);
    }
  }

  public static void clear() {
    DEADLINE.remove();
  }

  /**
   * @throws DeadlineExceededException if this thread's budget is used up
   */
  public static void checkNotExpired(String what) {
    Deadline deadline = DEADLINE.get();
    if (deadline != null && deadline.isExpired()) {
      throw new DeadlineExceededException("Tracker export exceeded its time budget before " + what);
    }
  }
}
