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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.Serializable;
import java.net.URI;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards that the Hibernate L2 cache configured in {@code ehcache.xml} stores entries <b>by
 * reference</b> (Ehcache {@code IdentityCopier}) rather than by value ({@code SerializingCopier}).
 *
 * <p>The {@code defaultCacheTemplate} declares an explicit {@code IdentityCopier} so that concurrent
 * imports do not pay a full Java serialize/deserialize round-trip on every L2 get/putFromLoad (see
 * DHIS2-21800). This test creates a cache exactly the way Hibernate's {@code JCacheRegionFactory}
 * does — through the JSR-107 API with the default {@link MutableConfiguration} whose {@code
 * storeByValue} is {@code true} — and asserts the template's copier overrides that default.
 *
 * <p>Behavioural check: with {@code IdentityCopier} a {@code get} returns the very instance that was
 * {@code put} (same identity); with {@code SerializingCopier} it would return a distinct
 * deserialized copy. The value type is {@link Serializable} on purpose so that, if the fix were
 * reverted, this test fails on the identity assertion rather than erroring out.
 */
class EhcacheStoreByReferenceTest {

  /** A mutable, serializable value — a stand-in for a Hibernate entity cache entry. */
  static final class Holder implements Serializable {
    int value;
  }

  @Test
  @DisplayName("ehcache.xml defaultCacheTemplate stores by reference (IdentityCopier), not by value")
  void defaultTemplateStoresByReference() throws Exception {
    URI ehcacheXml = getClass().getResource("/ehcache.xml").toURI();
    CachingProvider provider =
        Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
    try (CacheManager cacheManager =
        provider.getCacheManager(ehcacheXml, getClass().getClassLoader())) {
      // Same call shape as Hibernate JCacheRegionFactory + MissingCacheStrategy.CREATE:
      // a default MutableConfiguration (storeByValue == true) for a region that is not explicitly
      // configured, so it inherits defaultCacheTemplate from <jsr107:defaults>.
      Cache<Object, Object> region =
          cacheManager.createCache("l2-store-by-reference-probe", new MutableConfiguration<>());
      try {
        Holder put = new Holder();
        put.value = 42;
        region.put("k", put);

        Object got = region.get("k");
        assertSame(
            put,
            got,
            "L2 cache returned a different instance than was put: the region is serializing "
                + "entries (SerializingCopier / store-by-value). Expected store-by-reference via "
                + "IdentityCopier configured on defaultCacheTemplate in ehcache.xml.");
        // A second read must also hand back the identical instance.
        assertSame(put, region.get("k"), "Repeated L2 get returned a different instance.");
      } finally {
        cacheManager.destroyCache("l2-store-by-reference-probe");
      }
    }
  }
}
