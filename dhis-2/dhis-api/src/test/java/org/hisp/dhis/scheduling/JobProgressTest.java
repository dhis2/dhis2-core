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
package org.hisp.dhis.scheduling;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tests the utility methods of the {@link JobProgress} interface.
 *
 * @author Jan Bernitt
 */
class JobProgressTest {

  @Test
  void testRunStage_Stream() {
    JobProgress progress = newMockJobProgress();
    List<Integer> worked = new ArrayList<>();
    progress.runStage(
        Stream.of(1, 2, 3), String::valueOf, worked::add, JobProgressTest::printSummary);
    assertEquals(asList(1, 2, 3), worked);
    verify(progress).startingWorkItem("1");
    verify(progress).startingWorkItem("2");
    verify(progress).startingWorkItem("3");
    verify(progress, times(3)).completedWorkItem(null);
    verify(progress).completedStage("(3/0)");
    verify(progress, atLeast(3)).isCancellationRequested();
    verify(progress, never()).failedWorkItem(any(Exception.class));
    verify(progress, never()).failedWorkItem(anyString());
    verify(progress, never()).failedStage(any(Exception.class));
    verify(progress, never()).failedStage(anyString());
  }

  @Test
  void testRunStage_StreamCancel() {
    JobProgress progress = newMockJobProgress();
    when(progress.isCancellationRequested()).thenReturn(true);
    List<Integer> worked = new ArrayList<>();
    progress.runStage(
        Stream.of(1, 2, 3), String::valueOf, worked::add, JobProgressTest::printSummary);
    assertTrue(worked.isEmpty());
    verify(progress).failedStage(any(CancellationException.class));
    verify(progress, never()).startingWorkItem(any());
    verify(progress, never()).completedWorkItem(any());
  }

  @Test
  void testRunStage_StreamExceptionNoCancel() {
    JobProgress progress = newMockJobProgress();
    List<Integer> worked = new ArrayList<>();
    progress.runStage(
        Stream.of(1, 2, 3),
        String::valueOf,
        e -> {
          if (worked.isEmpty()) {
            worked.add(e);
          } else {
            throw new RuntimeException("work item failed");
          }
        },
        JobProgressTest::printSummary);
    assertEquals(singletonList(1), worked);
    verify(progress, times(3)).startingWorkItem(anyString());
    verify(progress, times(1)).completedWorkItem(any());
    verify(progress, times(2)).failedWorkItem(any(RuntimeException.class));
    verify(progress).completedStage("(1/2)");
  }

  @Test
  void testRunStage_StreamExceptionWithCancel() {
    JobProgress progress = newMockJobProgress();
    // first return true after we failed once
    doAnswer(
            invocation -> {
              when(progress.isCancellationRequested()).thenReturn(true);
              return null;
            })
        .when(progress)
        .failedWorkItem(any(RuntimeException.class));
    List<Integer> worked = new ArrayList<>();
    progress.runStage(
        Stream.of(1, 2, 3),
        String::valueOf,
        e -> {
          if (worked.isEmpty()) {
            worked.add(e);
          } else {
            throw new RuntimeException("work item failed");
          }
        },
        JobProgressTest::printSummary);
    assertEquals(singletonList(1), worked);
    verify(progress, times(2)).startingWorkItem(anyString());
    verify(progress).completedWorkItem(any());
    verify(progress).failedWorkItem(any(RuntimeException.class));
    verify(progress).failedStage(any(CancellationException.class));
  }

  @Test
  void testRunStage_RunnableSuccess() {
    JobProgress progress = newMockJobProgress();
    progress.runStage(
        () -> {
          /* NOOP - work done fine */
        });
    verify(progress).completedStage(null);
    verify(progress, never()).startingWorkItem(anyString());
    verify(progress, never()).failedStage(any(Exception.class));
    verify(progress, never()).failedStage(anyString());
  }

  @Test
  void testRunStage_RunnableFailure() {
    JobProgress progress = newMockJobProgress();
    progress.runStage(
        () -> {
          throw new IllegalStateException();
        });
    verify(progress, never()).completedStage(anyString());
    verify(progress, never()).startingWorkItem(anyString());
    verify(progress).failedStage(any(IllegalStateException.class));
    verify(progress, never()).failedStage(anyString());
  }

  @Test
  void testRunStage_Callable() {
    JobProgress progress = newMockJobProgress();
    assertEquals(42, progress.runStage(-1, () -> 42).intValue());
    verify(progress).completedStage(null);
    verify(progress, never()).startingWorkItem(anyString());
    verify(progress, never()).failedStage(any(Exception.class));
    verify(progress, never()).failedStage(anyString());
  }

  @Test
  void testRunStage_CallableError() {
    JobProgress progress = newMockJobProgress();
    assertEquals(
        -1,
        progress
            .runStage(
                -1,
                () -> {
                  throw new IllegalStateException();
                })
            .intValue());
    verify(progress, never()).completedStage(anyString());
    verify(progress, never()).startingWorkItem(anyString());
    verify(progress).failedStage(any(IllegalStateException.class));
    verify(progress, never()).failedStage(anyString());
  }

  @Test
  void testRunStageInParallel_CommonPool() {
    // runStageInParallel_Success(
    // Runtime.getRuntime().availableProcessors() );
  }

  @Test
  void testRunStageInParallel_CustomPool() {
    // runStageInParallel_Success( max( 2,
    // Runtime.getRuntime().availableProcessors() / 2 ) );
  }

  @Test
  void testRunStageInParallel_CommonPoolHalfFailed() {
    runStageInParallel_Success(Runtime.getRuntime().availableProcessors());
  }

  @Test
  void testRunStageInParallel_CustomPoolHalfFailed() {
    runStageInParallel_Success(max(2, Runtime.getRuntime().availableProcessors() / 2));
  }

  private static void runStageInParallel_Success(int parallelism) {
    AtomicInteger enterCount = new AtomicInteger();
    AtomicInteger exitCount = new AtomicInteger();
    AtomicInteger concurrentCount = new AtomicInteger();
    AtomicInteger maxConcurrentCount = new AtomicInteger();
    List<Integer> worked = new CopyOnWriteArrayList<>();
    Consumer<Integer> work =
        value -> {
          enterCount.incrementAndGet();
          int cur = concurrentCount.incrementAndGet();
          maxConcurrentCount.updateAndGet(val -> max(val, cur));
          // simulate the actual "work"
          worked.add(value);
          await().atLeast(100, TimeUnit.MILLISECONDS);
          concurrentCount.decrementAndGet();
          exitCount.incrementAndGet();
        };
    JobProgress progress = newMockJobProgress();
    List<Integer> items = IntStream.range(1, parallelism * 2).boxed().collect(toList());
    assertTrue(progress.runStageInParallel(parallelism, items, String::valueOf, work));
    int itemCount = items.size();
    assertEquals(new HashSet<>(items), new HashSet<>(worked));
    assertEquals(itemCount, enterCount.get());
    assertEquals(itemCount, exitCount.get());
    assertTrue(maxConcurrentCount.get() <= parallelism, "too much parallel work");
    verify(progress, times(itemCount)).startingWorkItem(anyString());
    verify(progress, times(itemCount)).completedWorkItem(null);
    verify(progress).completedStage(null);
    verify(progress, never()).failedWorkItem(anyString());
    verify(progress, never()).failedWorkItem(any(Exception.class));
    verify(progress, never()).failedStage(anyString());
    verify(progress, never()).failedStage(any(Exception.class));
  }

  private static void runStageInParallel_HalfSuccessHalfError(int parallelism) {
    AtomicInteger enterCount = new AtomicInteger();
    AtomicInteger exitCount = new AtomicInteger();
    AtomicInteger concurrentCount = new AtomicInteger();
    AtomicInteger maxConcurrentCount = new AtomicInteger();
    List<Integer> processed = new CopyOnWriteArrayList<>();
    Consumer<Integer> work =
        value -> {
          enterCount.incrementAndGet();
          int cur = concurrentCount.incrementAndGet();
          maxConcurrentCount.updateAndGet(val -> max(val, cur));
          // simulate the actual "work"
          processed.add(value);
          await().atLeast(100, TimeUnit.MILLISECONDS);
          concurrentCount.decrementAndGet();
          exitCount.incrementAndGet();
          if (processed.size() % 2 == 1) {
            throw new RuntimeException();
          }
        };
    JobProgress progress = newMockJobProgress();
    List<Integer> items = IntStream.range(1, parallelism * 2).boxed().collect(toList());
    assertFalse(progress.runStageInParallel(parallelism, items, String::valueOf, work));
    int itemCount = items.size();
    int successCount = itemCount / 2;
    int errorCount = itemCount - successCount;
    assertEquals(new HashSet<>(items), new HashSet<>(processed));
    assertEquals(itemCount, enterCount.get());
    assertEquals(itemCount, exitCount.get());
    assertTrue(maxConcurrentCount.get() <= parallelism, "too much parallel work");
    verify(progress, times(itemCount)).startingWorkItem(anyString());
    verify(progress, times(successCount)).completedWorkItem(null);
    verify(progress).completedStage(null);
    verify(progress, never()).failedWorkItem(anyString());
    verify(progress, times(errorCount)).failedWorkItem(any(Exception.class));
    verify(progress, never()).failedStage(anyString());
    verify(progress, never()).failedStage(any(Exception.class));
  }

  private static String printSummary(int success, int failed) {
    return String.format("(%d/%d)", success, failed);
  }

  @SuppressWarnings("unchecked")
  private static JobProgress newMockJobProgress() {
    JobProgress progress = mock(JobProgress.class);
    // wire run methods to their default implementation...
    when(progress.runStage(any(), any(), any(), any())).thenCallRealMethod();
    when(progress.runStage(any(Stream.class), any(), any())).thenCallRealMethod();
    when(progress.runStage(any(Collection.class), any(), any())).thenCallRealMethod();
    when(progress.runStage(any(Map.class))).thenCallRealMethod();
    when(progress.runStage(any(Collection.class))).thenCallRealMethod();
    when(progress.runStage(any(Runnable.class))).thenCallRealMethod();
    when(progress.runStage(any(), any())).thenCallRealMethod();
    when(progress.runStageInParallel(anyInt(), any(), any(), any())).thenCallRealMethod();
    return progress;
  }
}
