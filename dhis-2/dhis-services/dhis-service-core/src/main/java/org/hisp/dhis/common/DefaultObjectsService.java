/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.common;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.function.ToLongFunction;
import javax.annotation.Nonnull;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultObjectsService {

  private final Cache<Long> defaultObjectCache;
  private final IdentifiableObjectManager manager;

  public DefaultObjectsService(CacheProvider cacheProvider, IdentifiableObjectManager manager) {
    this.defaultObjectCache = cacheProvider.createDefaultObjectCache();
    this.manager = manager;
  }

  @Nonnull
  @Transactional(readOnly = true)
  public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults() {
    ToLongFunction<Class<? extends IdentifiableObject>> getIdCachedByName =
        type ->
            defaultObjectCache.get(
                type.getName(),
                t -> {
                  IdentifiableObject obj = manager.getByName(type, "default");
                  return obj == null ? -1 : obj.getId();
                });
    return Map.of(
        Category.class,
        requireNonNull(manager.get(Category.class, getIdCachedByName.applyAsLong(Category.class))),
        CategoryCombo.class,
        requireNonNull(
            manager.get(CategoryCombo.class, getIdCachedByName.applyAsLong(CategoryCombo.class))),
        CategoryOption.class,
        requireNonNull(
            manager.get(CategoryOption.class, getIdCachedByName.applyAsLong(CategoryOption.class))),
        CategoryOptionCombo.class,
        requireNonNull(
            manager.get(
                CategoryOptionCombo.class,
                getIdCachedByName.applyAsLong(CategoryOptionCombo.class))));
  }
}
