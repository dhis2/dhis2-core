/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.config.sqlobserver;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Warns about write statements the DML observer cannot parse, at most once per interval. Such a
 * write never bumps an ETag version, so the endpoints serving its table stay stale until the TTL
 * window ends. The warning names the statement so the parser can be extended; the rate limit keeps
 * a statement in a loop from flooding the log.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
final class UnmatchedDmlLog {

  static final long INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

  /** Only the start of the statement is logged: enough to see its shape, without its values. */
  static final int MAX_SQL_CHARS = 120;

  private final LongSupplier nanoClock;

  private boolean logged;

  private long lastLoggedNanos;

  private long suppressed;

  UnmatchedDmlLog() {
    this(System::nanoTime);
  }

  UnmatchedDmlLog(LongSupplier nanoClock) {
    this.nanoClock = nanoClock;
  }

  /**
   * Records an unparsed write statement.
   *
   * @return the number of statements suppressed since the previous warning when this call logged,
   *     or -1 when it was suppressed
   */
  synchronized long report(String sql) {
    long now = nanoClock.getAsLong();
    if (logged && now - lastLoggedNanos < INTERVAL_NANOS) {
      suppressed++;
      return -1;
    }
    long skipped = suppressed;
    logged = true;
    lastLoggedNanos = now;
    suppressed = 0;
    log.warn(
        "DML observer could not parse a write statement, so it bumps no ETag version and the "
            + "endpoints serving its table refresh only at the TTL. {} more since the last warning. "
            + "Statement starts with: {}",
        skipped,
        sql.length() > MAX_SQL_CHARS ? sql.substring(0, MAX_SQL_CHARS) : sql);
    return skipped;
  }
}
