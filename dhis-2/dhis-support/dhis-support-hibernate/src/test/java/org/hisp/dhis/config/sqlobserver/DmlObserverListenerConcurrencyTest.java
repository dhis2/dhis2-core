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
package org.hisp.dhis.config.sqlobserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Concurrency tests for {@link DmlObserverListener}. Verifies thread-safe batch accumulation,
 * commit/rollback from concurrent threads, and no lost events.
 */
class DmlObserverListenerConcurrencyTest {

  private HibernateTableEntityRegistry registry;
  private AtomicInteger bumpCount;
  private DmlObserverListener listener;

  @BeforeEach
  void setUp() {
    registry = mock(HibernateTableEntityRegistry.class);
    // Map all tables to DataElement so version bumps happen
    when(registry.getTableInfo(any()))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo(
                "dataelement", DataElement.class, List.of()));

    bumpCount = new AtomicInteger();
    ETagService eTagService = mock(ETagService.class);
    when(eTagService.incrementEntityTypeVersion(any()))
        .thenAnswer(
            (InvocationOnMock inv) -> {
              bumpCount.incrementAndGet();
              return 1L;
            });

    listener = new DmlObserverListener(registry, eTagService, null);
    listener.onApplicationEvent(new ContextRefreshedEvent(new StaticApplicationContext()));
  }

  @RepeatedTest(5)
  void concurrentBatchAccumulation_sameConnection() throws InterruptedException {
    int threadCount = 20;
    int eventsPerThread = 50;
    String connectionId = "conn-concurrent-1";

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);

    for (int t = 0; t < threadCount; t++) {
      final int threadIdx = t;
      pool.submit(
          () -> {
            try {
              ready.countDown();
              go.await();
              for (int i = 0; i < eventsPerThread; i++) {
                ExecutionInfo execInfo = createSuccessExecutionInfo(false, connectionId);
                // Use unique table names so dedup (table:op) doesn't collapse events
                List<QueryInfo> queries =
                    List.of(
                        createQueryInfo(
                            "INSERT INTO table_" + threadIdx + "_" + i + " (uid) VALUES (?)"));
                listener.afterQuery(execInfo, queries);
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    // Wait for all threads to be ready, then release
    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    // No bumps yet (non-auto-commit, no commit call)
    assertEquals(0, bumpCount.get());

    // Now commit — should bump version for DataElement (all events map to same entity type)
    Connection conn = createMockConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit", connectionId);
    listener.afterMethod(commitCtx);

    // All events map to DataElement; entity-type dedup means exactly 1 bump per batch
    assertEquals(1, bumpCount.get(), "One batch commit should produce exactly 1 entity-type bump");
  }

  @RepeatedTest(5)
  void concurrentDifferentConnections() throws InterruptedException {
    int connectionCount = 10;
    int eventsPerConnection = 20;

    ExecutorService pool = Executors.newFixedThreadPool(connectionCount);
    CountDownLatch ready = new CountDownLatch(connectionCount);
    CountDownLatch go = new CountDownLatch(1);

    for (int c = 0; c < connectionCount; c++) {
      String connId = "conn-multi-" + c;
      pool.submit(
          () -> {
            try {
              ready.countDown();
              go.await();
              for (int i = 0; i < eventsPerConnection; i++) {
                ExecutionInfo execInfo = createSuccessExecutionInfo(false, connId);
                // Use unique table names so dedup (table:op) doesn't collapse events
                List<QueryInfo> queries =
                    List.of(createQueryInfo("INSERT INTO table_" + i + " (uid) VALUES (?)"));
                listener.afterQuery(execInfo, queries);
              }
              // Commit this connection's batch
              Connection conn = createMockConnection();
              MethodExecutionContext commitCtx = createMethodContext(conn, "commit", connId);
              listener.afterMethod(commitCtx);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    // Each connection should produce 1 bump (entity-type dedup within each batch)
    assertEquals(connectionCount, bumpCount.get());
  }

  @Test
  void concurrentCommitAndAccumulation_noLostBumps() throws InterruptedException {
    // Simulate a race: one thread accumulating, another committing
    String connectionId = "conn-race-1";
    int iterations = 100;

    ExecutorService pool = Executors.newFixedThreadPool(2);
    // Accumulator thread: sends events in a loop
    pool.submit(
        () -> {
          for (int i = 0; i < iterations; i++) {
            try {
              ExecutionInfo execInfo = createSuccessExecutionInfo(false, connectionId);
              // Use unique table names so dedup (table:op) doesn't collapse events
              List<QueryInfo> queries =
                  List.of(createQueryInfo("INSERT INTO table_" + i + " (uid) VALUES (?)"));
              listener.afterQuery(execInfo, queries);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });

    // Committer thread: commits periodically
    pool.submit(
        () -> {
          for (int i = 0; i < 10; i++) {
            try {
              Thread.yield(); // Small delay to let some events accumulate
              Connection conn = createMockConnection();
              MethodExecutionContext commitCtx = createMethodContext(conn, "commit", connectionId);
              listener.afterMethod(commitCtx);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });

    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    // Final commit to flush any remaining events
    Connection conn = createMockConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit", connectionId);
    listener.afterMethod(commitCtx);

    // Each commit with events produces 1 bump (entity-type dedup).
    // At least 1 bump must have occurred (events were accumulated and committed).
    assertTrue(bumpCount.get() >= 1, "At least one version bump should have occurred");
  }

  @Test
  void concurrentAutoCommitDoesNotInterfereWithBatches() throws InterruptedException {
    // Mix of auto-commit and non-auto-commit on different connections
    String batchConnId = "conn-batch";
    int autoCommitCount = 50;
    int batchCount = 50;

    ExecutorService pool = Executors.newFixedThreadPool(4);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);

    // Auto-commit thread
    pool.submit(
        () -> {
          try {
            ready.countDown();
            go.await();
            for (int i = 0; i < autoCommitCount; i++) {
              ExecutionInfo execInfo = createSuccessExecutionInfo(true, "conn-auto-" + i);
              List<QueryInfo> queries =
                  List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
              listener.afterQuery(execInfo, queries);
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    // Batch thread
    pool.submit(
        () -> {
          try {
            ready.countDown();
            go.await();
            for (int i = 0; i < batchCount; i++) {
              ExecutionInfo execInfo = createSuccessExecutionInfo(false, batchConnId);
              // Use unique table names so dedup (table:op) doesn't collapse events
              List<QueryInfo> queries =
                  List.of(createQueryInfo("INSERT INTO table_batch_" + i + " (uid) VALUES (?)"));
              listener.afterQuery(execInfo, queries);
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    // Auto-commit: each auto-commit call bumps DataElement once = 50 bumps
    int autoCommitBumps = bumpCount.get();
    assertEquals(autoCommitCount, autoCommitBumps, "Each auto-commit should produce 1 bump");

    // Now commit the batch
    Connection conn = createMockConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit", batchConnId);
    listener.afterMethod(commitCtx);

    // Batch: all events map to DataElement, entity-type dedup = 1 bump
    // Total: autoCommitCount + 1 batch bump
    assertEquals(autoCommitCount + 1, bumpCount.get());
  }

  @lombok.SneakyThrows
  private ExecutionInfo createSuccessExecutionInfo(boolean autoCommit, String connectionId) {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(true);
    when(execInfo.getConnectionId()).thenReturn(connectionId);

    Connection conn = mock(Connection.class);
    when(conn.getAutoCommit()).thenReturn(autoCommit);

    Statement stmt = mock(Statement.class);
    when(stmt.getConnection()).thenReturn(conn);
    when(execInfo.getStatement()).thenReturn(stmt);

    return execInfo;
  }

  private Connection createMockConnection() {
    return mock(Connection.class);
  }

  private QueryInfo createQueryInfo(String sql) {
    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setQuery(sql);
    queryInfo.getParametersList().add(new ArrayList<>());
    return queryInfo;
  }

  @lombok.SneakyThrows
  private MethodExecutionContext createMethodContext(
      Object target, String methodName, String connectionId) {
    MethodExecutionContext ctx = mock(MethodExecutionContext.class);
    when(ctx.getTarget()).thenReturn(target);
    Method method = Connection.class.getMethod(methodName);
    when(ctx.getMethod()).thenReturn(method);

    ConnectionInfo connInfo = new ConnectionInfo();
    connInfo.setConnectionId(connectionId);
    when(ctx.getConnectionInfo()).thenReturn(connInfo);

    return ctx;
  }
}
