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
package org.hisp.dhis.webapi.security.csp.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs
 */
class CspReportRateLimiterTest {
  @Test
  void perIp_burstOf60_thenRefillsAt30PerMinute() {
    CspReportRateLimiter limiter = new CspReportRateLimiter();
    long t0 = System.nanoTime();
    for (int i = 0; i < 60; i++) {
      assertNull(limiter.tryAcquire("10.0.0.1", t0));
    }
    assertEquals("rate_limited_ip", limiter.tryAcquire("10.0.0.1", t0));
    // another client is not affected
    assertNull(limiter.tryAcquire("10.0.0.2", t0));
    // two seconds later one token is back
    long t2 = t0 + TimeUnit.SECONDS.toNanos(2);
    assertNull(limiter.tryAcquire("10.0.0.1", t2));
    assertEquals("rate_limited_ip", limiter.tryAcquire("10.0.0.1", t2));
  }

  @Test
  void globalCap_of500_appliesAcrossClients() {
    CspReportRateLimiter limiter = new CspReportRateLimiter();
    long t0 = System.nanoTime();
    int accepted = 0;
    for (int ip = 0; ip < 20; ip++) {
      for (int i = 0; i < 30; i++) {
        if (limiter.tryAcquire("10.0.1." + ip, t0) == null) {
          accepted++;
        }
      }
    }
    assertEquals(500, accepted);
    assertEquals("rate_limited_global", limiter.tryAcquire("10.0.2.1", t0));
  }
}
