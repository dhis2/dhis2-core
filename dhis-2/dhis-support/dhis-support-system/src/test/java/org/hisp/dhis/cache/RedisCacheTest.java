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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest {

  private static final String CACHE_REGION = "testRegion";
  private static final long EXPIRY_IN_SECONDS = 300L;

  @Mock private RedisTemplate<String, String> redisTemplate;

  @Mock private BoundValueOperations<String, String> boundValueOps;

  @Mock private ValueOperations<String, String> valueOps;

  @Mock private ExtendedCacheBuilder<String> cacheBuilder;

  @BeforeEach
  void setUp() {
    lenient().doReturn(redisTemplate).when(cacheBuilder).getRedisTemplate();
    lenient().when(cacheBuilder.getRegion()).thenReturn(CACHE_REGION);
    lenient().when(cacheBuilder.getExpiryInSeconds()).thenReturn(EXPIRY_IN_SECONDS);
    lenient().when(cacheBuilder.isExpiryEnabled()).thenReturn(true);
    lenient().when(cacheBuilder.isRefreshExpiryOnAccess()).thenReturn(false);
    lenient().when(cacheBuilder.getDefaultValue()).thenReturn(null);
  }

  private RedisCache<String> buildCache() {
    return new RedisCache<>(cacheBuilder);
  }

  @Nested
  class PutTests {

    @Test
    void shouldStoreValueWithExpiry() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);

      buildCache().put("myKey", "myValue");

      verify(redisTemplate).boundValueOps(CACHE_REGION + ":myKey");
      verify(boundValueOps).set("myValue", EXPIRY_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void shouldStoreWithoutExpiry_whenExpiryDisabled() {
      when(cacheBuilder.isExpiryEnabled()).thenReturn(false);
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);

      buildCache().put("myKey", "myValue");

      verify(boundValueOps).set("myValue");
    }

    @Test
    void shouldThrowException_whenValueIsNull() {
      RedisCache<String> cache = buildCache();
      assertThrows(IllegalArgumentException.class, () -> cache.put("myKey", null));
    }
  }

  @Nested
  class PutWithTtlTests {

    @Test
    void shouldStoreValueWithCustomTtl() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);

      buildCache().put("myKey", "myValue", 60L);

      verify(boundValueOps).set("myValue", 60L, TimeUnit.SECONDS);
    }

    @Test
    void shouldThrowException_whenKeyIsEmpty() {
      RedisCache<String> cache = buildCache();
      assertThrows(IllegalArgumentException.class, () -> cache.put("", "value", 60L));
    }

    @Test
    void shouldThrowException_whenValueIsNull() {
      RedisCache<String> cache = buildCache();
      assertThrows(IllegalArgumentException.class, () -> cache.put("myKey", null, 60L));
    }
  }

  @Nested
  class GetTests {

    @Test
    void shouldReturnValue_whenKeyExists() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn("cachedValue");

      Optional<String> result = buildCache().get("myKey");

      assertTrue(result.isPresent());
      assertEquals("cachedValue", result.get());
    }

    @Test
    void shouldReturnDefaultValue_whenKeyDoesNotExist() {
      when(cacheBuilder.getDefaultValue()).thenReturn("default");
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      Optional<String> result = buildCache().get("missingKey");

      assertTrue(result.isPresent());
      assertEquals("default", result.get());
    }

    @Test
    void shouldReturnEmpty_whenKeyDoesNotExistAndNoDefault() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      Optional<String> result = buildCache().get("missingKey");

      assertTrue(result.isEmpty());
    }

    @Test
    void shouldRefreshExpiry_whenRefreshExpiryOnAccessIsEnabled() {
      when(cacheBuilder.isRefreshExpiryOnAccess()).thenReturn(true);
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn("value");

      buildCache().get("myKey");

      verify(redisTemplate).expire(CACHE_REGION + ":myKey", EXPIRY_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void shouldNotRefreshExpiry_whenRefreshExpiryOnAccessIsDisabled() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn("value");

      buildCache().get("myKey");

      verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
  }

  @Nested
  class GetIfPresentTests {

    @Test
    void shouldReturnValue_whenKeyExists() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn("cachedValue");

      Optional<String> result = buildCache().getIfPresent("myKey");

      assertTrue(result.isPresent());
      assertEquals("cachedValue", result.get());
    }

    @Test
    void shouldReturnEmpty_whenKeyDoesNotExist() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      Optional<String> result = buildCache().getIfPresent("missingKey");

      assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotReturnDefaultValue_whenKeyDoesNotExist() {
      when(cacheBuilder.getDefaultValue()).thenReturn("default");
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      Optional<String> result = buildCache().getIfPresent("missingKey");

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class GetWithMappingFunctionTests {

    @Test
    void shouldComputeAndStoreValue_whenKeyDoesNotExist() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      String result = buildCache().get("myKey", key -> "computed_" + key);

      assertEquals("computed_myKey", result);
      verify(boundValueOps).set("computed_myKey", EXPIRY_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void shouldStoreWithoutExpiry_whenExpiryDisabled() {
      when(cacheBuilder.isExpiryEnabled()).thenReturn(false);
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      buildCache().get("myKey", key -> "computed");

      verify(boundValueOps).set("computed");
    }

    @Test
    void shouldReturnCachedValue_whenKeyExists() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn("cachedValue");

      @SuppressWarnings("unchecked")
      Function<String, String> mappingFunction = mock(Function.class);

      String result = buildCache().get("myKey", mappingFunction);

      assertEquals("cachedValue", result);
      verifyNoInteractions(mappingFunction);
    }

    @Test
    void shouldReturnDefaultValue_whenMappingFunctionReturnsNull() {
      when(cacheBuilder.getDefaultValue()).thenReturn("default");
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      String result = buildCache().get("myKey", key -> null);

      assertEquals("default", result);
    }

    @Test
    void shouldNotStoreValue_whenMappingFunctionReturnsNull() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.get()).thenReturn(null);

      buildCache().get("myKey", key -> null);

      verify(boundValueOps, never()).set(anyString(), anyLong(), any(TimeUnit.class));
      verify(boundValueOps, never()).set(anyString());
    }

    @Test
    void shouldThrowException_whenMappingFunctionIsNull() {
      RedisCache<String> cache = buildCache();
      assertThrows(IllegalArgumentException.class, () -> cache.get("myKey", null));
    }
  }

  @Nested
  class PutIfAbsentTests {

    @Test
    void shouldReturnTrue_whenKeyDoesNotExist() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.setIfAbsent("myValue", EXPIRY_IN_SECONDS, TimeUnit.SECONDS))
          .thenReturn(true);

      boolean result = buildCache().putIfAbsent("myKey", "myValue");

      assertTrue(result);
    }

    @Test
    void shouldReturnFalse_whenKeyExists() {
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.setIfAbsent("myValue", EXPIRY_IN_SECONDS, TimeUnit.SECONDS))
          .thenReturn(false);

      boolean result = buildCache().putIfAbsent("myKey", "myValue");

      assertFalse(result);
    }

    @Test
    void shouldSetWithoutExpiry_whenExpiryDisabled() {
      when(cacheBuilder.isExpiryEnabled()).thenReturn(false);
      when(redisTemplate.boundValueOps(anyString())).thenReturn(boundValueOps);
      when(boundValueOps.setIfAbsent("myValue")).thenReturn(true);

      boolean result = buildCache().putIfAbsent("myKey", "myValue");

      assertTrue(result);
      verify(boundValueOps).setIfAbsent("myValue");
    }

    @Test
    void shouldThrowException_whenValueIsNull() {
      RedisCache<String> cache = buildCache();
      assertThrows(IllegalArgumentException.class, () -> cache.putIfAbsent("myKey", null));
    }
  }

  @Nested
  class InvalidateTests {

    @Test
    void shouldDeleteKey() {
      when(redisTemplate.delete(anyString())).thenReturn(true);

      buildCache().invalidate("myKey");

      verify(redisTemplate).delete(CACHE_REGION + ":myKey");
    }
  }

  @Nested
  class InvalidateAllTests {

    @Test
    void shouldDeleteAllKeysInRegion() {
      Set<String> keys = Set.of(CACHE_REGION + ":key1", CACHE_REGION + ":key2");
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(keys);

      buildCache().invalidateAll();

      verify(redisTemplate).delete(keys);
    }

    @Test
    void shouldNotCallDelete_whenNoKeysExist() {
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(null);

      buildCache().invalidateAll();

      verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    void shouldNotCallDelete_whenKeysSetIsEmpty() {
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(Set.of());

      buildCache().invalidateAll();

      verify(redisTemplate, never()).delete(anySet());
    }
  }

  @Nested
  class KeysTests {

    @Test
    void shouldReturnKeysWithoutRegionPrefix() {
      Set<String> redisKeys = Set.of(CACHE_REGION + ":key1", CACHE_REGION + ":key2");
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(redisKeys);

      Set<String> result = (Set<String>) buildCache().keys();

      assertEquals(Set.of("key1", "key2"), result);
    }

    @Test
    void shouldReturnEmptySet_whenNoKeysExist() {
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(null);

      Iterable<String> result = buildCache().keys();

      assertFalse(result.iterator().hasNext());
    }

    @Test
    void shouldReturnEmptySet_whenKeysSetIsEmpty() {
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(Set.of());

      Iterable<String> result = buildCache().keys();

      assertFalse(result.iterator().hasNext());
    }
  }

  @Nested
  class GetAllTests {

    @Test
    void shouldReturnAllValues() {
      Set<String> keys = Set.of(CACHE_REGION + ":key1", CACHE_REGION + ":key2");
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(keys);
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.multiGet(keys)).thenReturn(List.of("value1", "value2"));

      Stream<String> result = buildCache().getAll();

      assertEquals(List.of("value1", "value2"), result.toList());
    }

    @Test
    void shouldReturnEmptyStream_whenNoKeysExist() {
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(null);

      Stream<String> result = buildCache().getAll();

      assertEquals(0, result.count());
    }

    @Test
    void shouldReturnEmptyStream_whenMultiGetReturnsNull() {
      Set<String> keys = Set.of(CACHE_REGION + ":key1");
      when(redisTemplate.keys(CACHE_REGION + ":*")).thenReturn(keys);
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.multiGet(keys)).thenReturn(null);

      Stream<String> result = buildCache().getAll();

      assertEquals(0, result.count());
    }
  }

  @Nested
  class CacheTypeTests {

    @Test
    void shouldReturnRedis() {
      assertEquals(CacheType.REDIS, buildCache().getCacheType());
    }
  }
}
