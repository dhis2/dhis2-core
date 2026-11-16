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

import static org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationConfig.CHANNEL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationListener;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Redis delivers cache invalidation messages on a Lettuce thread that has no authenticated user.
 * The listener must still evict the caches for entities with a composite id.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CacheInvalidationListenerWithoutAuthenticationTest extends PostgresIntegrationTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  private final Cache secondLevelCache = mock(Cache.class);

  private final QueryCacheManager queryCacheManager = mock(QueryCacheManager.class);

  private final PaginationCacheManager paginationCacheManager = mock(PaginationCacheManager.class);

  private CacheInvalidationListener listener;

  @BeforeEach
  void setUp() {
    SessionFactory realSessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    SessionFactory sessionFactory = mock(SessionFactory.class);
    when(sessionFactory.getCache()).thenReturn(secondLevelCache);
    when(sessionFactory.openSession()).thenAnswer(invocation -> realSessionFactory.openSession());

    listener =
        new CacheInvalidationListener(
            sessionFactory, paginationCacheManager, queryCacheManager, "SERVER_A");
  }

  static Stream<Arguments> compositeIdMessages() {
    return Stream.of(
        Arguments.of(DataValue.class, "11;12;13;14;15"),
        Arguments.of(CompleteDataSetRegistration.class, "21;22;23;24"),
        Arguments.of(TrackedEntityAttributeValue.class, "31;32"));
  }

  @ParameterizedTest
  @MethodSource("compositeIdMessages")
  void shouldEvictCachesOnInsertWithoutAuthenticatedUser(Class<?> entityClass, String id)
      throws InterruptedException {
    receiveOnThreadWithoutUser("SERVER_B:insert:" + entityClass.getName() + ":" + id);

    verify(queryCacheManager).evictQueryCache(secondLevelCache, entityClass);
    verify(paginationCacheManager).evictCache(entityClass.getName());
  }

  @ParameterizedTest
  @MethodSource("compositeIdMessages")
  void shouldEvictEntityOnUpdateWithoutAuthenticatedUser(Class<?> entityClass, String id)
      throws InterruptedException {
    receiveOnThreadWithoutUser("SERVER_B:update:" + entityClass.getName() + ":" + id);

    assertEquals(parseIds(id), referencedIds(capturedEvictedId(entityClass)));
  }

  @ParameterizedTest
  @MethodSource("compositeIdMessages")
  void shouldEvictCachesOnDeleteWithoutAuthenticatedUser(Class<?> entityClass, String id)
      throws InterruptedException {
    receiveOnThreadWithoutUser("SERVER_B:delete:" + entityClass.getName() + ":" + id);

    verify(queryCacheManager).evictQueryCache(secondLevelCache, entityClass);
    verify(paginationCacheManager).evictCache(entityClass.getName());
    assertEquals(parseIds(id), referencedIds(capturedEvictedId(entityClass)));
  }

  private void receiveOnThreadWithoutUser(String message) throws InterruptedException {
    Thread redisListenerThread =
        new Thread(
            () -> {
              SecurityContextHolder.clearContext();
              listener.message(CHANNEL_NAME, message);
            });
    redisListenerThread.start();
    redisListenerThread.join();
  }

  private Object capturedEvictedId(Class<?> entityClass) {
    ArgumentCaptor<Object> evictedId = ArgumentCaptor.forClass(Object.class);
    verify(secondLevelCache).evict(eq(entityClass), evictedId.capture());
    return evictedId.getValue();
  }

  private static List<Long> parseIds(String id) {
    return Arrays.stream(id.split(";")).map(Long::valueOf).toList();
  }

  private static List<Long> referencedIds(Object compositeId) {
    if (compositeId instanceof DataValue dataValue) {
      return List.of(
          dataValue.getDataElement().getId(),
          dataValue.getPeriod().getId(),
          dataValue.getSource().getId(),
          dataValue.getCategoryOptionCombo().getId(),
          dataValue.getAttributeOptionCombo().getId());
    }
    if (compositeId instanceof CompleteDataSetRegistration registration) {
      return List.of(
          registration.getDataSet().getId(),
          registration.getPeriod().getId(),
          registration.getSource().getId(),
          registration.getAttributeOptionCombo().getId());
    }
    if (compositeId instanceof TrackedEntityAttributeValue attributeValue) {
      return List.of(
          attributeValue.getAttribute().getId(), attributeValue.getTrackedEntity().getId());
    }
    throw new IllegalArgumentException("Unexpected composite id: " + compositeId);
  }
}
