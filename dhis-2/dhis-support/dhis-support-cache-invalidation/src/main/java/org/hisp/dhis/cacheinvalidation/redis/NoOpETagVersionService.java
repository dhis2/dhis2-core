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
package org.hisp.dhis.cacheinvalidation.redis;

import javax.annotation.Nonnull;
import org.hisp.dhis.cache.ETagVersionService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link ETagVersionService} that is used when Redis cache invalidation is
 * not enabled. All methods return 0 or false, effectively disabling the ETag versioning feature.
 *
 * @author Morten Svanæs
 */
@Service
@Conditional(value = CacheInvalidationDisabledCondition.class)
public class NoOpETagVersionService implements ETagVersionService {

  @Override
  public long getVersion(@Nonnull String userUid) {
    return 0L;
  }

  @Override
  public long incrementVersion(@Nonnull String userUid) {
    return 0L;
  }

  @Override
  public long incrementGlobalVersion() {
    return 0L;
  }

  @Override
  public long getGlobalVersion() {
    return 0L;
  }

  @Override
  public long getEntityTypeVersion(@Nonnull Class<?> entityType) {
    return 0L;
  }

  @Override
  public long incrementEntityTypeVersion(@Nonnull Class<?> entityType) {
    return 0L;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public int getTtlMinutes() {
    return 60;
  }
}
