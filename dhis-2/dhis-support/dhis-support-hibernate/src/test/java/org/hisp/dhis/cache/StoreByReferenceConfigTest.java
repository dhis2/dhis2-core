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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the storage semantics of the shipped {@code ehcache.xml} for BOTH ways a Hibernate L2 region
 * comes into existence.
 *
 * <p>Regions declared in the file are ehcache-native and store by reference. Regions NOT declared
 * are created at runtime by hibernate-jcache exactly like {@link #createRuntimeCache(String)}
 * below: {@code cacheManager.createCache(name, new MutableConfiguration<>())}. The JCache spec
 * defaults that configuration to store-by-value, which Ehcache implements by serializing and
 * deserializing every entry on every get and put (SerializingCopier). DHIS2 ran store-by-reference
 * for every region from 2016 through 2.41 (Ehcache 2); the 2.42 Spring 6 upgrade flipped
 * runtime-created regions to by-value as a silent side effect of that spec default. The {@code
 * <default-copiers>} block in ehcache.xml restores by-reference for them; this test fails if that
 * block is removed.
 *
 * <p>By-reference is safe for Hibernate's use: the cache holds Hibernate-internal disassembled
 * state that is never handed to application code and never mutated in place. Note entries are no
 * longer required to be {@link java.io.Serializable}; a deployment overriding the file to add
 * off-heap or disk tiers must configure serializers, exactly as before 2.42.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class StoreByReferenceConfigTest {

  private CacheManager cacheManager;

  @BeforeEach
  void setUp() throws Exception {
    URI config = getClass().getClassLoader().getResource("ehcache.xml").toURI();
    cacheManager =
        Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider")
            .getCacheManager(config, getClass().getClassLoader());
  }

  @AfterEach
  void tearDown() {
    if (cacheManager != null) {
      cacheManager.close();
    }
  }

  @Test
  void runtimeCreatedRegionStoresByReference() {
    Cache<Object, Object> cache = createRuntimeCache("org.hisp.dhis.test.RuntimeRegion");

    List<String> value = new ArrayList<>(List.of("entry"));
    cache.put("key", value);

    Object first = cache.get("key");
    Object second = cache.get("key");
    assertNotNull(first);
    assertSame(first, second, "two gets must return the same instance (store-by-reference)");
    assertSame(value, first, "the cached instance must be the stored one, not a serialized copy");
  }

  @Test
  void declaredRegionStoresByReference() {
    Cache<Object, Object> cache = cacheManager.getCache("org.hisp.dhis.option.Option");
    assertNotNull(cache, "declared region must exist in ehcache.xml");

    List<String> value = new ArrayList<>(List.of("entry"));
    cache.put("key", value);

    assertSame(value, cache.get("key"), "declared regions are ehcache-native, by reference");
  }

  /** Exactly what hibernate-jcache's JCacheRegionFactory.createCache does for missing regions. */
  private Cache<Object, Object> createRuntimeCache(String name) {
    return cacheManager.createCache(name, new MutableConfiguration<>());
  }
}
