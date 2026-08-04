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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class EventQueryExecutorTest {

  private EventQueryExecutor executor;

  @AfterEach
  void tearDown() {
    if (executor != null) {
      executor.destroy();
    }
    SecurityContextHolder.clearContext();
  }

  private EventQueryExecutor executor(int cpus) {
    SystemSettings settings = mock(SystemSettings.class);
    when(settings.getDatabaseServerCpus()).thenReturn(cpus);
    SystemSettingsProvider provider = mock(SystemSettingsProvider.class);
    when(provider.getCurrentSettings()).thenReturn(settings);
    executor = new EventQueryExecutor(provider);
    return executor;
  }

  @Test
  void testResultsComeBackInTaskOrderRegardlessOfCompletionOrder() {
    // Task i sleeps for (n - i) ms, so completion order is the reverse of submission order.
    int n = 40;
    List<Callable<Integer>> tasks =
        IntStream.range(0, n)
            .<Callable<Integer>>mapToObj(
                i ->
                    () -> {
                      Thread.sleep(n - i);
                      return i;
                    })
            .toList();

    assertEquals(IntStream.range(0, n).boxed().toList(), executor(4).invokeAll(tasks));
  }

  @Test
  void testConcurrencyIsCappedAtTheConfiguredNumberOfDatabaseCpus() throws Exception {
    int cap = 3;
    AtomicInteger running = new AtomicInteger();
    AtomicInteger highWaterMark = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(30);

    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      tasks.add(
          () -> {
            highWaterMark.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(20);
            running.decrementAndGet();
            done.countDown();
            return 1;
          });
    }

    assertEquals(30, executor(cap).invokeAll(tasks).size());
    assertTrue(done.await(30, TimeUnit.SECONDS));
    assertTrue(
        highWaterMark.get() <= cap,
        "at most " + cap + " queries may be in flight, saw " + highWaterMark.get());
  }

  @Test
  void testSingleTaskRunsOnTheCallingThread() {
    List<String> threadName = new ArrayList<>();
    executor(4).invokeAll(List.of(() -> threadName.add(Thread.currentThread().getName())));

    assertEquals(List.of(Thread.currentThread().getName()), threadName);
  }

  @Test
  void testASingleThreadedConfigurationRunsEverythingOnTheCallingThread() {
    // Handing work to one worker and blocking on it is pure overhead - measured at about 7% slower
    // than the serial loop this class replaces - so a cap of one has to stay on the caller.
    String caller = Thread.currentThread().getName();
    List<String> threadNames = new ArrayList<>();
    List<Callable<Integer>> tasks =
        IntStream.range(0, 5)
            .<Callable<Integer>>mapToObj(
                i ->
                    () -> {
                      threadNames.add(Thread.currentThread().getName());
                      return i;
                    })
            .toList();

    executor(1).invokeAll(tasks);

    assertEquals(List.of(caller, caller, caller, caller, caller), threadNames);
  }

  @Test
  void testEveryTaskSeesTheCallersSecurityContext() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("someone", "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    ConcurrentHashMap<Integer, Authentication> seen = new ConcurrentHashMap<>();
    List<Callable<Integer>> tasks =
        IntStream.range(0, 10)
            .<Callable<Integer>>mapToObj(
                i ->
                    () -> {
                      seen.put(i, SecurityContextHolder.getContext().getAuthentication());
                      return i;
                    })
            .toList();

    executor(4).invokeAll(tasks);

    assertEquals(10, seen.size());
    seen.values().forEach(seenAuthentication -> assertSame(authentication, seenAuthentication));
  }

  @Test
  void testFirstFailureIsRethrownUnwrapped() {
    // A query that hits the max-limit guard throws IllegalQueryException; the caller has to see
    // that, not an ExecutionException, because the API translates it into a 409 with an error code.
    IllegalQueryException expected = new IllegalQueryException(ErrorCode.E7128);
    List<Callable<Integer>> tasks =
        List.of(
            () -> 1,
            () -> {
              throw expected;
            },
            () -> 3);

    assertSame(
        expected, assertThrows(IllegalQueryException.class, () -> executor(4).invokeAll(tasks)));
  }

  @Test
  void testCheckedExceptionsAreWrappedRatherThanSwallowed() {
    List<Callable<Integer>> tasks =
        List.of(
            () -> 1,
            () -> {
              throw new java.io.IOException("boom");
            });

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> executor(4).invokeAll(tasks));
    assertEquals("boom", thrown.getCause().getMessage());
  }

  @Test
  void testNoTasksIsNotAnError() {
    assertEquals(List.of(), executor(4).invokeAll(List.of()));
  }
}
