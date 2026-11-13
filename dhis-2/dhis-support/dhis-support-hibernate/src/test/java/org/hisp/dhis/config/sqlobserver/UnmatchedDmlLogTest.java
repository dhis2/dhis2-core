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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UnmatchedDmlLogTest {

  @Test
  void logsOncePerIntervalAndCountsWhatItSuppressed() {
    AtomicLong now = new AtomicLong(1_000L);
    UnmatchedDmlLog log = new UnmatchedDmlLog(now::get);

    assertEquals(0, log.report("WITH a AS (SELECT 1) DELETE FROM x"), "first statement logs");
    now.addAndGet(UnmatchedDmlLog.INTERVAL_NANOS - 1);
    assertEquals(-1, log.report("WITH a AS (SELECT 1) DELETE FROM x"), "inside the interval");
    assertEquals(-1, log.report("  UPDATE y SET z = 1"), "inside the interval");
    now.addAndGet(1);
    assertEquals(2, log.report("  UPDATE y SET z = 1"), "next interval logs the suppressed count");
    assertEquals(-1, log.report("  UPDATE y SET z = 1"), "and starts a new interval");
  }
}
