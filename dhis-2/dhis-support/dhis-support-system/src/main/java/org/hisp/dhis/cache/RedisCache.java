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
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.Assert.hasText;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * A redis backed implementation of {@link Cache}. This implementation uses a shared redis cache
 * server for any number of instances.
 *
 * @author Ameen Mohamed
 */
public class RedisCache<V> implements Cache<V> {
  private static final String VALUE_CANNOT_BE_NULL = "Value cannot be null";

  private RedisTemplate<String, V> redisTemplate;

  private boolean refreshExpriryOnAccess;

  private long expiryInSeconds;

  private String cacheRegion;

  private V defaultValue;

  private boolean expiryEnabled;

  /**
   * Constructor for instantiating RedisCache.
   *
   * @param cacheBuilder The cache builder instance
   */
  @SuppressWarnings("unchecked")
  public RedisCache(ExtendedCacheBuilder<V> cacheBuilder) {
    this.redisTemplate = (RedisTemplate<String, V>) cacheBuilder.getRedisTemplate();
    this.refreshExpriryOnAccess = cacheBuilder.isRefreshExpiryOnAccess();
    this.expiryInSeconds = cacheBuilder.getExpiryInSeconds();
    this.cacheRegion = cacheBuilder.getRegion();
    this.defaultValue = cacheBuilder.getDefaultValue();
    this.expiryEnabled = cacheBuilder.isExpiryEnabled();
  }

  @Override
  public Optional<V> getIfPresent(String key) {
    String redisKey = generateKey(key);
    if (expiryEnabled && refreshExpriryOnAccess) {
      redisTemplate.expire(redisKey, expiryInSeconds, SECONDS);
    }
    return Optional.ofNullable(redisTemplate.boundValueOps(redisKey).get());
  }

  @Override
  public Optional<V> get(String key) {
    String redisKey = generateKey(key);
    if (expiryEnabled && refreshExpriryOnAccess) {
      redisTemplate.expire(redisKey, expiryInSeconds, SECONDS);
    }
    return Optional.ofNullable(
        Optional.ofNullable(redisTemplate.boundValueOps(redisKey).get()).orElse(defaultValue));
  }

  @Override
  public V get(String key, Function<String, V> mappingFunction) {
    if (null == mappingFunction) {
      throw new IllegalArgumentException("MappingFunction cannot be null");
    }

    String redisKey = generateKey(key);

    if (expiryEnabled && refreshExpriryOnAccess) {
      redisTemplate.expire(redisKey, expiryInSeconds, SECONDS);
    }

    V value = redisTemplate.boundValueOps(redisKey).get();

    if (null == value) {
      value = mappingFunction.apply(key);

      if (null != value) {
        if (expiryEnabled) {
          redisTemplate.boundValueOps(redisKey).set(value, expiryInSeconds, SECONDS);
        } else {
          redisTemplate.boundValueOps(redisKey).set(value);
        }
      }
    }

    return Optional.ofNullable(value).orElse(defaultValue);
  }

  @Override
  public Stream<V> getAll() {
    Set<String> keySet = redisTemplate.keys(getAllKeysInRegionPattern());
    if (keySet == null) {
      return Stream.empty();
    }
    List<V> values = redisTemplate.opsForValue().multiGet(keySet);
    return values == null ? Stream.empty() : values.stream();
  }

  @Override
  public Set<String> keys() {
    var keys = redisTemplate.keys(getAllKeysInRegionPattern());
    return keys == null
        ? emptySet()
        : keys.stream().map(key -> key.substring(key.indexOf(':') + 1)).collect(toSet());
  }

  @Override
  public void put(String key, V value) {
    if (null == value) {
      throw new IllegalArgumentException(VALUE_CANNOT_BE_NULL);
    }

    String redisKey = generateKey(key);
    if (expiryEnabled) {
      redisTemplate.boundValueOps(redisKey).set(value, expiryInSeconds, SECONDS);
    } else {
      redisTemplate.boundValueOps(redisKey).set(value);
    }
  }

  @Override
  public void put(String key, V value, long ttlInSeconds) {
    hasText(key, VALUE_CANNOT_BE_NULL);

    String redisKey = generateKey(key);

    redisTemplate.boundValueOps(redisKey).set(value, ttlInSeconds, SECONDS);
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    if (null == value) {
      throw new IllegalArgumentException(VALUE_CANNOT_BE_NULL);
    }
    String redisKey = generateKey(key);

    var ops = redisTemplate.boundValueOps(redisKey);
    if (expiryEnabled) {
      return ops.setIfAbsent(value, expiryInSeconds, SECONDS) == Boolean.TRUE;
    } else {
      return ops.setIfAbsent(value) == Boolean.TRUE;
    }
  }

  @Override
  public void invalidate(String key) {
    redisTemplate.delete(generateKey(key));
  }

  private String generateKey(String key) {
    return cacheRegion.concat(":").concat(key);
  }

  private String getAllKeysInRegionPattern() {
    return generateKey("*");
  }

  @Override
  public void invalidateAll() {
    Set<String> keysToDelete = redisTemplate.keys(getAllKeysInRegionPattern());
    redisTemplate.delete(keysToDelete);
  }

  @Override
  public CacheType getCacheType() {
    return CacheType.REDIS;
  }
}
