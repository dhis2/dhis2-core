/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.hisp.dhis.tracker.preheat.mappers.CategoryComboMapper;
import org.hisp.dhis.tracker.preheat.mappers.CategoryMapper;
import org.hisp.dhis.tracker.preheat.mappers.CategoryOptionComboMapper;
import org.hisp.dhis.tracker.preheat.mappers.CategoryOptionMapper;
import org.hisp.dhis.tracker.preheat.mappers.PreheatMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DefaultsSupplier extends AbstractPreheatSupplier {

  private static final int CACHE_TTL = 720; // 12h

  private static final long CACHE_CAPACITY = 10;

  @Nonnull private final IdentifiableObjectManager manager;

  @Nonnull private final PreheatCacheService cache;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    // not using manager.getDefaults() as the collections of the entities
    // are still hibernate proxies
    // this leads to lazy init exceptions with - no session. reason is the
    // defaults are cached in another thread
    // and the session is closed. using manager.getDefaults() and
    // session.merge(entity) works, but we would not
    // benefit from caching.

    preheatDefault(preheat, Category.class, CategoryMapper.INSTANCE, Category.DEFAULT_NAME);
    preheatDefault(
        preheat,
        CategoryCombo.class,
        CategoryComboMapper.INSTANCE,
        CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
    preheatDefault(
        preheat, CategoryOption.class, CategoryOptionMapper.INSTANCE, CategoryOption.DEFAULT_NAME);
    preheatDefault(
        preheat,
        CategoryOptionCombo.class,
        CategoryOptionComboMapper.INSTANCE,
        CategoryOptionCombo.DEFAULT_NAME);
  }

  private <T extends IdentifiableObject> void preheatDefault(
      TrackerPreheat preheat, Class<T> klass, PreheatMapper<T> mapper, String name) {
    Optional<T> metadata =
        (Optional<T>)
            cache.get(
                DefaultsSupplier.class.getName(),
                klass.getName(),
                (k, n) -> Optional.ofNullable(mapper.map(manager.getByName(klass, name))),
                CACHE_TTL,
                CACHE_CAPACITY);
    metadata.ifPresent(t -> preheat.putDefault(klass, t));
  }
}
