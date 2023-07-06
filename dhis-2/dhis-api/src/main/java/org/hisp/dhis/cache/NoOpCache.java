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

import static java.util.Collections.emptySet;
import static org.springframework.util.Assert.hasText;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A No operation implementation of {@link Cache}. The implementation will not cache anything and
 * can be used during system testing when caching has to be disabled.
 *
 * @author Ameen Mohamed
 */
public class NoOpCache<V> implements Cache<V> {
  private static final String VALUE_CANNOT_BE_NULL = "Value cannot be null";

  private final V defaultValue;

  public NoOpCache(CacheBuilder<V> cacheBuilder) {
    this(cacheBuilder.getDefaultValue());
  }

  public NoOpCache() {
    this((V) null);
  }

  public NoOpCache(V defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public Optional<V> getIfPresent(String key) {
    return Optional.empty();
  }

  @Override
  public Optional<V> get(String key) {
    return Optional.ofNullable(defaultValue);
  }

  @Override
  public V get(String key, Function<String, V> mappingFunction) {
    if (null == mappingFunction) {
      throw new IllegalArgumentException("MappingFunction cannot be null");
    }
    return Optional.ofNullable(mappingFunction.apply(key)).orElse(defaultValue);
  }

  @Override
  public Stream<V> getAll() {
    return Stream.empty();
  }

  @Override
  public Iterable<String> keys() {
    return emptySet();
  }

  @Override
  public void put(String key, V value) {
    if (null == value) {
      throw new IllegalArgumentException(VALUE_CANNOT_BE_NULL);
    }
    // No operation
  }

  @Override
  public void put(String key, V value, long ttlInSeconds) {
    hasText(key, VALUE_CANNOT_BE_NULL);
    // No operation
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    if (null == value) {
      throw new IllegalArgumentException(VALUE_CANNOT_BE_NULL);
    }
    // No operation
    return false;
  }

  @Override
  public void invalidate(String key) {
    // No operation
  }

  @Override
  public void invalidateAll() {
    // No operation
  }

  @Override
  public CacheType getCacheType() {
    return CacheType.NONE;
  }
}
