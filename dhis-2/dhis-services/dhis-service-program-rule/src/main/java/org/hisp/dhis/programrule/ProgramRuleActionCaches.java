/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.programrule;

import java.util.List;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.stereotype.Component;

/**
 * Holds the caches for {@link DefaultProgramRuleService}'s {@code ProgramRuleAction}-derived
 * metadata queries, as a standalone bean rather than fields on the service itself.
 *
 * <p>{@code DefaultProgramRuleService} has {@code @Transactional} methods, so Spring registers it
 * behind a JDK dynamic proxy implementing {@link ProgramRuleService} - the context never holds a
 * bean of the concrete class, so nothing can autowire it by that type. Splitting the caches into
 * this plain, unproxied bean lets both the service and {@link
 * ProgramRuleActionCacheInvalidationListener} depend on it directly instead.
 *
 * @see DefaultProgramRuleService#getProgramRulesByActionTypes
 * @see DefaultProgramRuleService#getDataElementsPresentInProgramRules
 * @see DefaultProgramRuleService#getTrackedEntityAttributesPresentInProgramRules
 */
@Component
public class ProgramRuleActionCaches {
  private final Cache<List<ProgramRule>> programRulesByActionTypes;

  private final Cache<List<String>> programRuleActionUids;

  public ProgramRuleActionCaches(CacheProvider cacheProvider) {
    this.programRulesByActionTypes = cacheProvider.createProgramRulesByActionTypesCache();
    this.programRuleActionUids = cacheProvider.createProgramRuleActionUidsCache();
  }

  public Cache<List<ProgramRule>> getProgramRulesByActionTypes() {
    return programRulesByActionTypes;
  }

  public Cache<List<String>> getProgramRuleActionUids() {
    return programRuleActionUids;
  }

  /** Evicts both caches, so the next read recomputes them instead of serving a stale answer. */
  public void invalidateAll() {
    programRulesByActionTypes.invalidateAll();
    programRuleActionUids.invalidateAll();
  }
}
