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
 * but it is <strong>not supported</strong> on multi-node topologies where peer nodes would need to
 * share version counters. Version counters are process-local, so multi-node invalidation is
 * incorrect. Until a shared version store exists, the cache is forced off when either of these
 * multi-node signals is present, even if the config key is {@code on}:
 *
 * <ul>
 *   <li>legacy DHIS2 clustering: {@code cluster.members} <em>and</em> {@code cluster.hostname}
 *       ({@link DhisConfigurationProvider#isClusterEnabled()})
 *   <li>Redis-based cache invalidation: {@link ConfigurationKey#REDIS_CACHE_INVALIDATION_ENABLED}
 *       ({@code redis.cache.invalidation.enabled}). This is the common modern multi-node signal and
 *       is independent of {@code cluster.*}. The Redis invalidation listener does not propagate
 *       ETag version bumps to peers.
 * </ul>
 *
 * <p>{@link ConfigurationKey#REDIS_ENABLED} ({@code redis.enabled}) alone does <strong>not</strong>
 * force the feature off. That key enables Redis as a general cache/session backend and is valid on
 * a single node; it does not imply peer-to-peer invalidation. Only {@code
 * redis.cache.invalidation.enabled} indicates a multi-node coherence deployment for this purpose.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiETagCacheActivation {

  /**
   * @return {@code true} when the config key is on and no multi-node force-off signal is present
   *     (neither DHIS2 clustering nor Redis cache invalidation)
   */
  public static boolean isEffectivelyEnabled(DhisConfigurationProvider config) {
    if (!config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)) {
      return false;
    }
    if (isMultiNodeIncompatible(config)) {
      return false;
    }
    return true;
  }

  /**
   * @return {@code true} when a multi-node topology signal is set that is incompatible with
   *     process-local ETag version counters
   */
  public static boolean isMultiNodeIncompatible(DhisConfigurationProvider config) {
    return config.isClusterEnabled()
        || config.isEnabled(ConfigurationKey.REDIS_CACHE_INVALIDATION_ENABLED);
  }

  /**
   * Logs when the operator asked for the ETag cache but a multi-node signal forces it off. Safe to
   * call from both Spring conditions; the message is identical so duplicate lines are obvious.
   * Called during bean registration condition evaluation (may run more than once per process).
   */
  public static void logIfClusterIncompatible(DhisConfigurationProvider config) {
    if (!config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)
        || !isMultiNodeIncompatible(config)) {
      return;
    }

    boolean clustered = config.isClusterEnabled();
    boolean redisInvalidation = config.isEnabled(ConfigurationKey.REDIS_CACHE_INVALIDATION_ENABLED);

    StringBuilder signals = new StringBuilder();
    if (clustered) {
      signals.append("DHIS2 clustering (cluster.members + cluster.hostname)");
    }
    if (redisInvalidation) {
      if (!signals.isEmpty()) {
        signals.append(" and ");
      }
      signals.append("Redis cache invalidation (redis.cache.invalidation.enabled=on)");
    }

    log.warn(
        "API ETag cache (cache.api.etag.enabled=on) is not supported with {}. Leaving the ETag "
            + "cache and DML observer disabled. Process-local version counters cannot invalidate "
            + "peers, and Redis cache invalidation does not propagate ETag versions. Set "
            + "cache.api.etag.enabled=off explicitly, or disable the multi-node signal(s). "
            + "Shared-version propagation may be added in a later release.",
        signals);
  }
}
