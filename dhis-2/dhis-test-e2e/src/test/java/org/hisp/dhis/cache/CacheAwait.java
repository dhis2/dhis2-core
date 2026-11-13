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
package org.hisp.dhis.cache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.hisp.dhis.test.e2e.actions.LoginActions;

final class CacheAwait {
  private CacheAwait() {}

  static CacheProbe.CacheResponse awaitInvalidation(
      CacheProbe probe,
      CacheProbeUser probeUser,
      LoginActions loginActions,
      String path,
      String oldEtag) {
    AtomicReference<CacheProbe.CacheResponse> latestResponse = new AtomicReference<>();

    Awaitility.await()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .until(
            () -> {
              probeUser.login(loginActions);
              CacheProbe.CacheResponse response = probe.getIfNoneMatch(path, oldEtag);
              latestResponse.set(response);

              return response.statusCode() == 200
                  && response.etag() != null
                  && !response.etag().equals(oldEtag);
            });

    return latestResponse.get();
  }
}
