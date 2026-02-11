/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.query.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.hisp.dhis.query.planner.PropertyPath;
import org.hisp.dhis.schema.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InOperatorBatchingTest {

  private CriteriaBuilder builder;
  private Path<Object> jpaPath;
  private Predicate singlePredicate;
  private Predicate combinedPredicate;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    builder = mock(CriteriaBuilder.class);
    jpaPath = mock(Path.class);
    singlePredicate = mock(Predicate.class);
    combinedPredicate = mock(Predicate.class);

    when(jpaPath.in(any(List.class))).thenReturn(singlePredicate);
  }

  @Test
  void testInOperator_smallList_noBatching() {
    List<String> values = List.of("a", "b", "c");
    InOperator<String> op = new InOperator<>(values);

    Predicate result = op.buildBatchedInPredicate(builder, jpaPath, values);

    assertSame(singlePredicate, result);
    verify(builder, never()).or(any(Predicate[].class));
  }

  @Test
  void testInOperator_exactBatchSize_noBatching() {
    List<String> values = generateValues(InOperator.BATCH_SIZE);
    InOperator<String> op = new InOperator<>(values);

    Predicate result = op.buildBatchedInPredicate(builder, jpaPath, values);

    assertSame(singlePredicate, result);
    verify(builder, never()).or(any(Predicate[].class));
  }

  @Test
  void testInOperator_exceedsBatchSize_usesOrCombination() {
    int size = InOperator.BATCH_SIZE + 1;
    List<String> values = generateValues(size);
    InOperator<String> op = new InOperator<>(values);

    when(builder.or(any(Predicate[].class))).thenReturn(combinedPredicate);

    Predicate result = op.buildBatchedInPredicate(builder, jpaPath, values);

    assertSame(combinedPredicate, result);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testInOperator_twoBatches_callsInTwice() {
    int size = InOperator.BATCH_SIZE + 1;
    List<String> values = generateValues(size);
    InOperator<String> op = new InOperator<>(values);

    List<List<String>> capturedInArgs = new ArrayList<>();
    when(jpaPath.in(any(List.class)))
        .thenAnswer(
            inv -> {
              capturedInArgs.add(new ArrayList<>((List<String>) inv.getArgument(0)));
              return singlePredicate;
            });
    when(builder.or(any(Predicate[].class))).thenReturn(combinedPredicate);

    op.buildBatchedInPredicate(builder, jpaPath, values);

    assertEquals(2, capturedInArgs.size());
    assertEquals(InOperator.BATCH_SIZE, capturedInArgs.get(0).size());
    assertEquals(1, capturedInArgs.get(1).size());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testInOperator_threeBatches_callsInThreeTimes() {
    int size = InOperator.BATCH_SIZE * 2 + 1;
    List<String> values = generateValues(size);
    InOperator<String> op = new InOperator<>(values);

    AtomicInteger inCallCount = new AtomicInteger(0);
    when(jpaPath.in(any(List.class)))
        .thenAnswer(
            inv -> {
              inCallCount.incrementAndGet();
              return singlePredicate;
            });
    when(builder.or(any(Predicate[].class))).thenReturn(combinedPredicate);

    op.buildBatchedInPredicate(builder, jpaPath, values);

    assertEquals(3, inCallCount.get());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testNotInOperator_smallList_noBatching() {
    List<String> values = List.of("a", "b", "c");
    NotInOperator<String> op = new NotInOperator<>(values);
    Predicate notPredicate = mock(Predicate.class);

    Root<Object> root = mock(Root.class);
    when(root.get(anyString())).thenReturn(jpaPath);
    when(jpaPath.get(anyString())).thenReturn(jpaPath);
    when(builder.not(singlePredicate)).thenReturn(notPredicate);

    Predicate result = op.getPredicate(builder, root, mockPropertyPath());

    assertSame(notPredicate, result);
    verify(builder, never()).and(any(Predicate[].class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testNotInOperator_exceedsBatchSize_usesAndCombination() {
    int size = InOperator.BATCH_SIZE + 1;
    List<String> values = generateValues(size);
    NotInOperator<String> op = new NotInOperator<>(values);

    Predicate notPredicate = mock(Predicate.class);
    Root<Object> root = mock(Root.class);
    when(root.get(anyString())).thenReturn(jpaPath);
    when(jpaPath.get(anyString())).thenReturn(jpaPath);
    when(builder.not(singlePredicate)).thenReturn(notPredicate);
    when(builder.and(any(Predicate[].class))).thenReturn(combinedPredicate);

    Predicate result = op.getPredicate(builder, root, mockPropertyPath());

    assertSame(combinedPredicate, result);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testNotInOperator_twoBatches_callsNotTwice() {
    int size = InOperator.BATCH_SIZE + 1;
    List<String> values = generateValues(size);
    NotInOperator<String> op = new NotInOperator<>(values);

    Predicate notPredicate = mock(Predicate.class);
    Root<Object> root = mock(Root.class);
    when(root.get(anyString())).thenReturn(jpaPath);
    when(jpaPath.get(anyString())).thenReturn(jpaPath);

    AtomicInteger notCallCount = new AtomicInteger(0);
    when(builder.not(singlePredicate))
        .thenAnswer(
            inv -> {
              notCallCount.incrementAndGet();
              return notPredicate;
            });
    when(builder.and(any(Predicate[].class))).thenReturn(combinedPredicate);

    op.getPredicate(builder, root, mockPropertyPath());

    assertEquals(2, notCallCount.get());
  }

  /**
   * Simulates what happens when {@code getPreQueryMatches()} returns 65,536+ UIDs. The pipeline is:
   * {@code getPreQueryMatches()} returns {@code List<UID>} → {@code createIdInFilter()} creates
   * {@code Filters.in("id", values)} → creates an {@code InOperator} → {@code
   * JpaCriteriaQueryEngine} calls {@code InOperator.getPredicate()} → {@code
   * buildBatchedInPredicate()} splits into batches of 30K.
   *
   * <p>Without batching, PostgreSQL would crash with: "Tried to send an out-of-range integer as a
   * 2-byte value: 65536"
   */
  @SuppressWarnings("unchecked")
  @Test
  void testInOperator_simulatePreQueryExceedingPostgresqlLimit_batchesCorrectly() {
    // PostgreSQL's 2-byte parameter index limit is 65,535
    int postgresqlLimit = 65_535;
    int preQueryResultSize = postgresqlLimit + 1;
    List<String> preQueryUids = generateValues(preQueryResultSize);

    // This is what Filters.in("id", values) creates internally
    InOperator<String> op = new InOperator<>(preQueryUids);

    // Capture each batch that would be sent as a separate IN clause to the database
    List<Integer> batchSizes = new ArrayList<>();
    when(jpaPath.in(any(List.class)))
        .thenAnswer(
            inv -> {
              List<?> batch = inv.getArgument(0);
              batchSizes.add(batch.size());
              return singlePredicate;
            });
    when(builder.or(any(Predicate[].class))).thenReturn(combinedPredicate);

    Predicate result = op.buildBatchedInPredicate(builder, jpaPath, preQueryUids);

    // Must batch — a single IN with 65,536 params would crash PostgreSQL
    assertSame(combinedPredicate, result, "should use OR-combined batched predicates");

    // 65,536 / 30,000 = 3 batches: [30000, 30000, 5536]
    assertEquals(3, batchSizes.size(), "65,536 values should produce 3 batches of max 30,000");
    assertEquals(InOperator.BATCH_SIZE, batchSizes.get(0), "first batch");
    assertEquals(InOperator.BATCH_SIZE, batchSizes.get(1), "second batch");
    assertEquals(
        preQueryResultSize - InOperator.BATCH_SIZE * 2, batchSizes.get(2), "remainder batch");

    // Each individual batch is well under the PostgreSQL limit
    for (int batchSize : batchSizes) {
      assert batchSize <= InOperator.BATCH_SIZE
          : "batch size " + batchSize + " exceeds BATCH_SIZE " + InOperator.BATCH_SIZE;
    }
  }

  /**
   * Same scenario for NOT IN — De Morgan's law splits into: {@code NOT IN (batch1) AND NOT IN
   * (batch2) AND NOT IN (batch3)}
   */
  @SuppressWarnings("unchecked")
  @Test
  void testNotInOperator_simulatePreQueryExceedingPostgresqlLimit_batchesCorrectly() {
    int preQueryResultSize = 65_536;
    List<String> preQueryUids = generateValues(preQueryResultSize);

    NotInOperator<String> op = new NotInOperator<>(preQueryUids);

    Root<Object> root = mock(Root.class);
    when(root.get(anyString())).thenReturn(jpaPath);
    when(jpaPath.get(anyString())).thenReturn(jpaPath);

    // Track how many NOT(IN(...)) predicates get created — one per batch
    AtomicInteger notInBatchCount = new AtomicInteger(0);
    when(builder.not(singlePredicate))
        .thenAnswer(
            inv -> {
              notInBatchCount.incrementAndGet();
              return mock(Predicate.class);
            });
    when(builder.and(any(Predicate[].class))).thenReturn(combinedPredicate);

    Predicate result = op.getPredicate(builder, root, mockPropertyPath());

    assertSame(combinedPredicate, result, "should use AND-combined batched NOT IN predicates");
    assertEquals(3, notInBatchCount.get(), "65,536 values should produce 3 NOT IN batches");
  }

  private static List<String> generateValues(int count) {
    return IntStream.range(0, count).mapToObj(i -> "val" + i).toList();
  }

  private PropertyPath mockPropertyPath() {
    Property property = mock(Property.class);
    when(property.isCollection()).thenReturn(false);
    when(property.getFieldName()).thenReturn("uid");

    PropertyPath path = mock(PropertyPath.class);
    when(path.getProperty()).thenReturn(property);
    when(path.getAlias()).thenReturn(new String[0]);
    when(path.getPath()).thenReturn("uid");
    return path;
  }
}
