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
package org.hisp.dhis.datastatistics;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DefaultDataStatisticsServiceTest {

  /** Each data counts computation queries the data value count once per window (0, 1, 7, 30). */
  private static final int DATA_VALUE_QUERIES_PER_COMPUTATION = 4;

  @Mock private DataStatisticsStore dataStatisticsStore;

  @Mock private DataStatisticsEventStore dataStatisticsEventStore;

  @Mock private UserService userService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private DataValueService dataValueService;

  @Mock private StatisticsProvider statisticsProvider;

  @Mock private EventVisualizationStore eventVisualizationStore;

  @Mock private SystemService systemService;

  @Mock private CacheProvider cacheProvider;

  @Mock private PlatformTransactionManager transactionManager;

  private DefaultDataStatisticsService service;

  @BeforeEach
  void setUp() {
    when(cacheProvider.<DataSummary>createSystemStatisticsDataCountsCache())
        .thenReturn(
            new SimpleCacheBuilder<DataSummary>()
                .expireAfterWrite(30, MINUTES)
                .withMaximumSize(1)
                .build());

    service =
        new DefaultDataStatisticsService(
            dataStatisticsStore,
            dataStatisticsEventStore,
            userService,
            idObjectManager,
            dataValueService,
            statisticsProvider,
            eventVisualizationStore,
            systemService,
            cacheProvider,
            transactionManager);
  }

  @Test
  void concurrentCallersOnCacheMissComputeDataCountsOnce() throws Exception {
    CountDownLatch computing = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(dataValueService.getDataValueCountLastUpdatedAfter(any(Date.class), anyBoolean()))
        .thenAnswer(
            invocation -> {
              computing.countDown();
              release.await(10, SECONDS);
              return 0;
            });

    AtomicReference<DataSummary> first = new AtomicReference<>();
    AtomicReference<DataSummary> second = new AtomicReference<>();
    Thread firstCaller = new Thread(() -> first.set(service.getSystemStatisticsDataCounts()));
    Thread secondCaller = new Thread(() -> second.set(service.getSystemStatisticsDataCounts()));

    firstCaller.start();
    assertTrue(computing.await(10, SECONDS), "First caller never started computing");
    secondCaller.start();
    awaitBlocked(secondCaller);
    release.countDown();
    firstCaller.join(10_000);
    secondCaller.join(10_000);

    verify(dataValueService, times(DATA_VALUE_QUERIES_PER_COMPUTATION))
        .getDataValueCountLastUpdatedAfter(any(Date.class), anyBoolean());
    assertSame(first.get(), second.get());
  }

  @Test
  void cachedDataCountsAreNotRecomputed() {
    DataSummary first = service.getSystemStatisticsDataCounts();
    DataSummary second = service.getSystemStatisticsDataCounts();

    verify(dataValueService, times(DATA_VALUE_QUERIES_PER_COMPUTATION))
        .getDataValueCountLastUpdatedAfter(any(Date.class), anyBoolean());
    assertSame(first, second);
  }

  /** Waits until the given thread is blocked waiting for the computation lock. */
  private static void awaitBlocked(Thread thread) throws InterruptedException {
    long deadline = System.nanoTime() + SECONDS.toNanos(10);
    while (thread.getState() != Thread.State.BLOCKED) {
      if (System.nanoTime() > deadline) {
        fail("Second caller did not wait for the first caller's computation");
      }
      Thread.sleep(10);
    }
  }
}
