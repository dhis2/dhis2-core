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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Token buckets for the unauthenticated report intake: per client IP 30 reports per minute with a
 * burst of 60, and a global cap of 500 per minute. In memory per node.
 *
 * @author Morten Svanæs
 */
@Component
public class CspReportRateLimiter {
  static final double PER_IP_PER_MINUTE = 30;
  static final double PER_IP_BURST = 60;
  static final double GLOBAL_PER_MINUTE = 500;
  private static final int MAX_TRACKED_IPS = 10_000;
  private static final long IDLE_NANOS = TimeUnit.MINUTES.toNanos(5);

  private final Map<String, Bucket> perIp = new ConcurrentHashMap<>();
  private final Bucket global = new Bucket(GLOBAL_PER_MINUTE, GLOBAL_PER_MINUTE, System.nanoTime());

  /** Why a report was not accepted, or {@code null} when it was. */
  public String tryAcquire(String clientIp) {
    return tryAcquire(clientIp, System.nanoTime());
  }

  String tryAcquire(String clientIp, long now) {
    if (perIp.size() > MAX_TRACKED_IPS) {
      perIp.values().removeIf(b -> now - b.last > IDLE_NANOS);
      if (perIp.size() > MAX_TRACKED_IPS) {
        return "rate_limited_global";
      }
    }
    Bucket bucket =
        perIp.computeIfAbsent(clientIp, ip -> new Bucket(PER_IP_PER_MINUTE, PER_IP_BURST, now));
    if (!bucket.take(now)) {
      return "rate_limited_ip";
    }
    if (!global.take(now)) {
      return "rate_limited_global";
    }
    return null;
  }

  static final class Bucket {
    private final double perNano;
    private final double capacity;
    private double tokens;
    private long last;

    Bucket(double perMinute, double capacity, long now) {
      this.perNano = perMinute / TimeUnit.MINUTES.toNanos(1);
      this.capacity = capacity;
      this.tokens = capacity;
      this.last = now;
    }

    synchronized boolean take(long now) {
      tokens = Math.min(capacity, tokens + (now - last) * perNano);
      last = now;
      if (tokens < 1) {
        return false;
      }
      tokens -= 1;
      return true;
    }
  }
}
