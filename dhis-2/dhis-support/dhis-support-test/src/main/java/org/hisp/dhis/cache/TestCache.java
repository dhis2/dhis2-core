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
package org.hisp.dhis.cache;

import static java.util.Collections.unmodifiableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Luciano Fiandesio
 */
public class TestCache<V> implements Cache<V> {
  private Map<String, V> mapCache = new HashMap<>();

  @Override
  public Optional<V> getIfPresent(String key) {
    if (mapCache.containsKey(key)) {
      return get(key);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<V> get(String key) {
    return Optional.ofNullable(mapCache.get(key));
  }

  @Override
  public V get(String key, Function<String, V> mappingFunction) {
    return null;
  }

  @Override
  public Stream<V> getAll() {
    return mapCache.values().stream();
  }

  @Override
  public Iterable<String> keys() {
    return unmodifiableSet(mapCache.keySet());
  }

  @Override
  public void put(String key, V value) {
    mapCache.put(key, value);
  }

  @Override
  public void put(String key, V value, long ttlInSeconds) {
    // Ignoring ttl for this testing cache
    mapCache.put(key, value);
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    return mapCache.putIfAbsent(key, value) != value;
  }

  @Override
  public void invalidate(String key) {
    mapCache.remove(key);
  }

  @Override
  public void invalidateAll() {
    mapCache = new HashMap<>();
  }

  @Override
  public CacheType getCacheType() {
    return null;
  }
}
