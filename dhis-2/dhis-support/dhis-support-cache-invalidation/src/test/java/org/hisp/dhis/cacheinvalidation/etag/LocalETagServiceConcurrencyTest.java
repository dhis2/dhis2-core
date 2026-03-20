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
package org.hisp.dhis.cacheinvalidation.etag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Concurrency tests for {@link LocalETagService}. Verifies that concurrent increments produce
 * monotonically increasing versions with no lost updates.
 */
class LocalETagServiceConcurrencyTest {

  private LocalETagService service;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)).thenReturn(true);
    when(config.isEnabled(ConfigurationKey.SQL_DML_OBSERVER_ENABLED)).thenReturn(true);
    when(config.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue()))
        .thenReturn("60");

    service = new LocalETagService();

    // Inject the mock config via reflection (field is @Autowired)
    var field = LocalETagService.class.getDeclaredField("configurationProvider");
    field.setAccessible(true);
    field.set(service, config);

    service.afterPropertiesSet();
  }

  @RepeatedTest(5)
  void concurrentEntityTypeIncrements_noLostUpdates() throws InterruptedException {
    int threadCount = 20;
    int incrementsPerThread = 500;

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);

    for (int t = 0; t < threadCount; t++) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            for (int i = 0; i < incrementsPerThread; i++) {
              service.incrementEntityTypeVersion(DataElement.class);
            }
          });
    }

    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    long expected = (long) threadCount * incrementsPerThread;
    assertEquals(
        expected,
        service.getEntityTypeVersion(DataElement.class),
        "Concurrent increments should not lose updates");
  }

  @RepeatedTest(5)
  void concurrentAllCacheVersionIncrements() throws InterruptedException {
    int threadCount = 10;
    int incrementsPerThread = 1000;

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);

    for (int t = 0; t < threadCount; t++) {
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            for (int i = 0; i < incrementsPerThread; i++) {
              service.incrementAllCacheVersion();
            }
          });
    }

    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    long expected = (long) threadCount * incrementsPerThread;
    assertEquals(expected, service.getAllCacheVersion());
  }

  @Test
  void concurrentMixedEntityTypeIncrements_isolation() throws InterruptedException {
    int threadCount = 10;
    int incrementsPerThread = 200;

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);

    // Half threads increment DataElement, half increment OrganisationUnit
    for (int t = 0; t < threadCount; t++) {
      Class<?> entityType = (t % 2 == 0) ? DataElement.class : OrganisationUnit.class;
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            for (int i = 0; i < incrementsPerThread; i++) {
              service.incrementEntityTypeVersion(entityType);
            }
          });
    }

    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    // Each entity type gets half the threads
    long expectedPerType = (long) (threadCount / 2) * incrementsPerThread;
    assertEquals(
        expectedPerType,
        service.getEntityTypeVersion(DataElement.class),
        "DataElement increments should be isolated");
    assertEquals(
        expectedPerType,
        service.getEntityTypeVersion(OrganisationUnit.class),
        "OrganisationUnit increments should be isolated");
  }

  @Test
  void versionsAreMonotonicallyIncreasing() {
    int increments = 1000;
    long previous = 0;

    for (int i = 0; i < increments; i++) {
      long current = service.incrementEntityTypeVersion(DataElement.class);
      assertTrue(
          current > previous,
          "Version should be monotonically increasing: " + current + " <= " + previous);
      previous = current;
    }
  }
}
