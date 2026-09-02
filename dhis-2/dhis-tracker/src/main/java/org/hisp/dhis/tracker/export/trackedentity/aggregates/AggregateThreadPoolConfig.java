/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounds the executor {@link TrackedEntityAggregate} fans its per request branches out to.
 *
 * <p>Every branch holds a connection for the length of its query, so the thread count here is a
 * direct claim on {@code connection.pool.max_size}, a pool shared with Hibernate, login and every
 * other product.
 *
 * <p>The bound is {@code pool_size >= Tn * (Cm - 1) + 1}, where {@code Tn} is the concurrent
 * threads and {@code Cm} the most connections one thread holds at once. {@code Cm} is 2: the
 * enrollment branch runs under {@code @Transactional}, Hibernate holds the connection its metadata
 * reads acquire until the transaction ends ({@code
 * DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION}), and the JDBC store then takes a second one,
 * since {@code JpaTransactionManager} is built without a {@code DataSource} and so registers no
 * {@code ConnectionHolder} for a {@code JdbcTemplate} to join. That leaves {@code pool_size >= Tn +
 * 1}, which only a bounded {@code Tn} can satisfy.
 */
@Slf4j
@Configuration("trackedEntityAggregateThreadPoolConfig")
public class AggregateThreadPoolConfig {

  public static final String AGGREGATE_THREAD_POOL = "trackedEntityAggregateThreadPool";

  /** Branches {@link TrackedEntityAggregate#find} submits, each on its own connection. */
  private static final int BRANCHES_PER_REQUEST = 4;

  /** A constant, not a setting: the value only means anything against the connection pool size. */
  private static final int TARGET_CONCURRENT_REQUESTS = 5;

  /** Leaves the rest of the pool to everything else sharing it. */
  private static final double MAX_POOL_SHARE = 0.25;

  /**
   * One request's worth of backlog. A branch is a blocking query the request waits on, not deferred
   * work, so a deep queue only turns slow requests into timed out ones. Kept shallow so overload
   * reaches {@link ThreadPoolExecutor.CallerRunsPolicy} quickly, which throttles by occupying the
   * request thread.
   */
  private static final int QUEUE_CAPACITY = BRANCHES_PER_REQUEST;

  @Bean(AGGREGATE_THREAD_POOL)
  public Executor trackedEntityAggregateThreadPool(DhisConfigurationProvider config) {
    int maxThreads = maxThreads(config);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(maxThreads);
    executor.setMaxPoolSize(maxThreads);
    executor.setQueueCapacity(QUEUE_CAPACITY);
    executor.setThreadNamePrefix("TRACKER-TE-FETCH-");
    executor.setDaemon(true);
    // Overflow serialises the branch onto the request thread rather than failing the export. It
    // does not escape the bound: the caller borrows from the same connection pool.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // Otherwise core threads are never reclaimed and sit idle.
    executor.setAllowCoreThreadTimeOut(true);
    executor.initialize();

    return executor;
  }

  /**
   * Derived from the connection pool rather than an absolute default, so lowering {@code
   * connection.pool.max_size} cannot leave the cap oversized.
   */
  static int maxThreads(DhisConfigurationProvider config) {
    int dbPoolSize = config.getIntProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE);
    int wanted = BRANCHES_PER_REQUEST * TARGET_CONCURRENT_REQUESTS;
    // Floor of one request's parallelism, so a small pool serialises branches instead of a request
    // waiting on itself.
    int share = Math.max(BRANCHES_PER_REQUEST, (int) (dbPoolSize * MAX_POOL_SHARE));
    // Cm = 2 means maxThreads <= pool_size - 1. Caps the floor above, which must never win.
    int deadlockBound = dbPoolSize - 1;
    int maxThreads = Math.max(1, Math.min(Math.min(wanted, share), deadlockBound));

    log.info(
        "Tracked entity aggregate thread pool bounded to {} threads ({} branches per request, {} wanted for {} concurrent requests, {} allowed as a pool share, {} allowed by the deadlock bound, {}={})",
        maxThreads,
        BRANCHES_PER_REQUEST,
        wanted,
        TARGET_CONCURRENT_REQUESTS,
        share,
        deadlockBound,
        ConfigurationKey.CONNECTION_POOL_MAX_SIZE.getKey(),
        dbPoolSize);

    return maxThreads;
  }
}
