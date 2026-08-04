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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.commons.util.SystemUtils.getCpuCores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Runs the queries an event or enrollment analytics request was planned into, concurrently but with
 * a hard cap on how many hit the database at once.
 *
 * <p>The aggregate data-value path has been concurrent for years - {@code
 * JdbcAnalyticsManager.getAggregatedDataValues} is {@code @Async} and returns a {@code Future}. The
 * event and program-indicator path was a plain {@code for} loop over a blocking call, which is why
 * an aggregate request with 70 program indicators executed 81 statements one at a time, 0.23-0.32 s
 * each, for a flat 21.7 seconds.
 *
 * <p>This deliberately does not reuse Spring's {@code @Async} executor. With no {@code
 * TaskExecutor} bean configured, {@code @EnableAsync} falls back to {@code
 * SimpleAsyncTaskExecutor}, which starts a new thread per task and bounds nothing; the data-value
 * path gets away with it only because its query planner has already collapsed the work into at most
 * {@code MAX_QUERIES} groups. Here the fan-out is one query per program indicator per partition and
 * is not bounded in advance, so the bound has to live in the pool.
 *
 * <p>Concurrency is capped at the same figure the data-value path uses for its group count - {@code
 * databaseServerCpus}, or the JVM's core count when that setting is unset - because the resource
 * being protected is the same one: the analytics database and its connection pool. Work beyond the
 * cap queues rather than being rejected, so the worst case degrades towards the serial behaviour
 * this replaces rather than failing. The size is read once, at startup: changing {@code
 * databaseServerCpus} needs a restart to take effect here.
 */
@Slf4j
@Component
public class EventQueryExecutor implements DisposableBean {

  /**
   * Matches {@code DataHandler.MAX_QUERIES}. Past this the database is the bottleneck and more
   * concurrency only deepens the queue.
   */
  private static final int MAX_CONCURRENT_QUERIES = 8;

  private final int concurrency;

  private final ThreadPoolExecutor executor;

  public EventQueryExecutor(SystemSettingsProvider settingsProvider) {
    int threads = concurrency(settingsProvider);
    this.concurrency = threads;
    this.executor =
        new ThreadPoolExecutor(
            threads,
            threads,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
              Thread thread = new Thread(runnable, "event-analytics-query");
              thread.setDaemon(true);
              return thread;
            });
    this.executor.allowCoreThreadTimeOut(true);

    log.debug("Event analytics query executor sized to {} threads", threads);
  }

  /**
   * Runs every task, waits for all of them, and returns their results in the order the tasks were
   * given. A single task, or any number of tasks on a single-threaded configuration, runs on the
   * calling thread: handing work to one worker and blocking on it is pure overhead, measured at
   * about 7% slower than the serial loop this replaces.
   *
   * <p>Each task runs with the caller's {@link SecurityContext}, since everything downstream of
   * here resolves the current user for sharing and dimension restrictions.
   *
   * @throws RuntimeException the first task failure, with the original exception as its cause if it
   *     is not already a {@link RuntimeException}. No result is returned if any task failed.
   */
  public <T> List<T> invokeAll(List<Callable<T>> tasks) {
    if (tasks.isEmpty()) {
      return List.of();
    }
    if (tasks.size() == 1 || concurrency == 1) {
      List<T> results = new ArrayList<>(tasks.size());
      tasks.forEach(task -> results.add(call(task)));
      return results;
    }

    SecurityContext securityContext = SecurityContextHolder.getContext();

    List<Future<T>> futures = new ArrayList<>(tasks.size());
    for (Callable<T> task : tasks) {
      futures.add(executor.submit(() -> callWithSecurityContext(task, securityContext)));
    }

    List<T> results = new ArrayList<>(tasks.size());
    for (Future<T> future : futures) {
      try {
        results.add(future.get());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        futures.forEach(f -> f.cancel(true));
        throw new IllegalStateException("Interrupted while executing analytics queries", ex);
      } catch (ExecutionException ex) {
        futures.forEach(f -> f.cancel(true));
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }
        throw new IllegalStateException("Error during execution of analytics query", cause);
      }
    }
    return results;
  }

  private static <T> T callWithSecurityContext(Callable<T> task, SecurityContext securityContext) {
    SecurityContextHolder.setContext(securityContext);
    try {
      return task.call();
    } catch (Exception ex) {
      throw ex instanceof RuntimeException runtimeException
          ? runtimeException
          : new IllegalStateException(ex);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private static <T> T call(Callable<T> task) {
    try {
      return task.call();
    } catch (Exception ex) {
      throw ex instanceof RuntimeException runtimeException
          ? runtimeException
          : new IllegalStateException(ex);
    }
  }

  private static int concurrency(SystemSettingsProvider settingsProvider) {
    int cores = settingsProvider.getCurrentSettings().getDatabaseServerCpus();
    return Math.min(Math.max(cores == 0 ? getCpuCores() : cores, 1), MAX_CONCURRENT_QUERIES);
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }
}
