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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Effective activation rules for the API ETag cache and its DML observer.
 *
 * <p>The feature is gated by {@link ConfigurationKey#CACHE_API_ETAG_ENABLED} (default {@code on}),
 * but it is <strong>not supported</strong> together with DHIS2 clustering ({@code cluster.members}
 * + {@code cluster.hostname}). Version counters are process-local, so multi-node invalidation is
 * incorrect. Until a shared version store exists, the cache is forced off when clustering is
 * enabled, even if the config key is {@code on}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiETagCacheActivation {

  /**
   * @return {@code true} when the config key is on and DHIS2 clustering is not enabled
   */
  public static boolean isEffectivelyEnabled(DhisConfigurationProvider config) {
    if (!config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)) {
      return false;
    }
    if (config.isClusterEnabled()) {
      return false;
    }
    return true;
  }

  /**
   * Logs once when the operator asked for the ETag cache but clustering forces it off. Safe to call
   * from both Spring conditions; the message is identical so duplicate lines are obvious and rare
   * (bean registration only).
   */
  public static void logIfClusterIncompatible(DhisConfigurationProvider config) {
    if (config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED) && config.isClusterEnabled()) {
      log.warn(
          "API ETag cache (cache.api.etag.enabled=on) is not supported with DHIS2 clustering "
              + "(cluster.members + cluster.hostname). Leaving the ETag cache and DML observer "
              + "disabled. Process-local version counters cannot invalidate peers. Set "
              + "cache.api.etag.enabled=off explicitly, or disable clustering. Shared-version "
              + "propagation may be added in a later release.");
    }
  }
}
