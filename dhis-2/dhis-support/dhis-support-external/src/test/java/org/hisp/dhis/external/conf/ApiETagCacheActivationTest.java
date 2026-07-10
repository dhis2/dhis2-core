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
package org.hisp.dhis.external.conf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Mockito is not a dependency of dhis-support-external; use a JDK proxy stub for the two methods
 * {@link ApiETagCacheActivation} reads.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiETagCacheActivationTest {

  @Test
  @DisplayName("On and not clustered => effectively enabled")
  void enabledWhenOnAndNotClustered() {
    assertTrue(ApiETagCacheActivation.isEffectivelyEnabled(stub(true, false)));
  }

  @Test
  @DisplayName("Config off => effectively disabled even without clustering")
  void disabledWhenConfigOff() {
    assertFalse(ApiETagCacheActivation.isEffectivelyEnabled(stub(false, false)));
  }

  @Test
  @DisplayName("On + clustering => forced off (unsupported combination)")
  void forcedOffWhenClustered() {
    assertFalse(ApiETagCacheActivation.isEffectivelyEnabled(stub(true, true)));
  }

  private static DhisConfigurationProvider stub(boolean etagOn, boolean clustered) {
    return (DhisConfigurationProvider)
        Proxy.newProxyInstance(
            DhisConfigurationProvider.class.getClassLoader(),
            new Class<?>[] {DhisConfigurationProvider.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if ("isEnabled".equals(name)
                  && args != null
                  && args.length == 1
                  && args[0] == ConfigurationKey.CACHE_API_ETAG_ENABLED) {
                return etagOn;
              }
              if ("isClusterEnabled".equals(name)) {
                return clustered;
              }
              if ("toString".equals(name)) {
                return "stub(etagOn=" + etagOn + ", clustered=" + clustered + ")";
              }
              if (method.getReturnType() == boolean.class) {
                return false;
              }
              if (method.getReturnType() == int.class) {
                return 0;
              }
              return null;
            });
  }
}
