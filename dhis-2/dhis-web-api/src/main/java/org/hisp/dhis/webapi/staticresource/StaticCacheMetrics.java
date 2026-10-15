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
package org.hisp.dhis.webapi.staticresource;

import io.micrometer.core.instrument.MeterRegistry;
import javax.annotation.CheckForNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for the static asset cache layer, exposed through the Prometheus scrape
 * endpoint at {@code /api/metrics}:
 *
 * <ul>
 *   <li>{@code dhis.static.cache.requests} (tag {@code policy}: {@code no_store}, {@code
 *       immutable}, {@code must_revalidate}, {@code default}): static resource responses by the
 *       Cache-Control policy they were served with
 *   <li>{@code dhis.static.cache.responses.not.modified}: 304 responses served from the early ETag
 *       check
 *   <li>{@code dhis.static.cache.html.rewrites} (tag {@code result}: {@code hit}, {@code miss},
 *       {@code skipped}): HTML cache busting rewrite cache outcomes
 * </ul>
 *
 * <p>All counters are no-ops when no {@link MeterRegistry} is available in the application context.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class StaticCacheMetrics {

  static final String REQUESTS = "dhis.static.cache.requests";
  static final String NOT_MODIFIED = "dhis.static.cache.responses.not.modified";
  static final String HTML_REWRITES = "dhis.static.cache.html.rewrites";

  static final String POLICY_NO_STORE = "no_store";
  static final String POLICY_IMMUTABLE = "immutable";
  static final String POLICY_MUST_REVALIDATE = "must_revalidate";
  static final String POLICY_DEFAULT = "default";

  static final String REWRITE_HIT = "hit";
  static final String REWRITE_MISS = "miss";
  static final String REWRITE_SKIPPED = "skipped";

  private final MeterRegistry registry;

  @Autowired
  public StaticCacheMetrics(ObjectProvider<MeterRegistry> registryProvider) {
    this(registryProvider.getIfAvailable());
  }

  StaticCacheMetrics(@CheckForNull MeterRegistry registry) {
    this.registry = registry;
  }

  /** Counts a static resource response by the Cache-Control policy it was served with. */
  public void countRequest(String policy) {
    if (registry != null) {
      registry.counter(REQUESTS, "policy", policy).increment();
    }
  }

  /** Counts a 304 Not Modified response served from the early ETag check. */
  public void countNotModified() {
    if (registry != null) {
      registry.counter(NOT_MODIFIED).increment();
    }
  }

  /** Counts an HTML cache busting rewrite by result: hit, miss or skipped. */
  public void countHtmlRewrite(String result) {
    if (registry != null) {
      registry.counter(HTML_REWRITES, "result", result).increment();
    }
  }
}
