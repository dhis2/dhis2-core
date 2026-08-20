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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Mockito is not a dependency of dhis-support-external; use a JDK proxy stub for the methods {@link
 * ApiETagCacheActivation} reads.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiETagCacheActivationTest {

  @ParameterizedTest(name = "etagOn={0} clustered={1} redisInvalidation={2} => enabled={3}")
  @MethodSource("activationTruthTable")
  @DisplayName("isEffectivelyEnabled truth table including redis cache invalidation")
  void activationTruthTable(
      boolean etagOn, boolean clustered, boolean redisInvalidation, boolean expectedEnabled) {
    DhisConfigurationProvider config = stub(etagOn, clustered, redisInvalidation);
    assertEquals(expectedEnabled, ApiETagCacheActivation.isEffectivelyEnabled(config));
    assertEquals(
        clustered || redisInvalidation, ApiETagCacheActivation.isMultiNodeIncompatible(config));
  }

  static Stream<Arguments> activationTruthTable() {
    // etagOn, clustered, redisInvalidation, expectedEnabled
    return Stream.of(
        Arguments.of(true, false, false, true),
        Arguments.of(false, false, false, false),
        Arguments.of(true, true, false, false),
        Arguments.of(true, false, true, false),
        Arguments.of(true, true, true, false),
        Arguments.of(false, true, false, false),
        Arguments.of(false, false, true, false),
        Arguments.of(false, true, true, false));
  }

  @Test
  @DisplayName("Plain redis.enabled alone does not force the feature off")
  void plainRedisEnabledDoesNotForceOff() {
    // Documented product choice: redis.enabled is a single-node-valid cache backend, not a
    // multi-node coherence signal. Activation must not consult REDIS_ENABLED.
    DhisConfigurationProvider config =
        (DhisConfigurationProvider)
            Proxy.newProxyInstance(
                DhisConfigurationProvider.class.getClassLoader(),
                new Class<?>[] {DhisConfigurationProvider.class},
                (proxy, method, args) -> {
                  String name = method.getName();
                  if ("isEnabled".equals(name)
                      && args != null
                      && args.length == 1
                      && args[0] == ConfigurationKey.CACHE_API_ETAG_ENABLED) {
                    return true;
                  }
                  if ("isEnabled".equals(name)
                      && args != null
                      && args.length == 1
                      && args[0] == ConfigurationKey.REDIS_ENABLED) {
                    return true;
                  }
                  if ("isEnabled".equals(name)
                      && args != null
                      && args.length == 1
                      && args[0] == ConfigurationKey.REDIS_CACHE_INVALIDATION_ENABLED) {
                    return false;
                  }
                  if ("isClusterEnabled".equals(name)) {
                    return false;
                  }
                  if ("toString".equals(name)) {
                    return "stub(redis.enabled=on only)";
                  }
                  if (method.getReturnType() == boolean.class) {
                    return false;
                  }
                  if (method.getReturnType() == int.class) {
                    return 0;
                  }
                  return null;
                });

    assertTrue(ApiETagCacheActivation.isEffectivelyEnabled(config));
  }

  @Test
  @DisplayName("On + redis cache invalidation => forced off")
  void forcedOffWhenRedisCacheInvalidationEnabled() {
    assertFalse(ApiETagCacheActivation.isEffectivelyEnabled(stub(true, false, true)));
  }

  private static DhisConfigurationProvider stub(
      boolean etagOn, boolean clustered, boolean redisInvalidation) {
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
              if ("isEnabled".equals(name)
                  && args != null
                  && args.length == 1
                  && args[0] == ConfigurationKey.REDIS_CACHE_INVALIDATION_ENABLED) {
                return redisInvalidation;
              }
              if ("isClusterEnabled".equals(name)) {
                return clustered;
              }
              if ("toString".equals(name)) {
                return "stub(etagOn="
                    + etagOn
                    + ", clustered="
                    + clustered
                    + ", redisInvalidation="
                    + redisInvalidation
                    + ")";
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
