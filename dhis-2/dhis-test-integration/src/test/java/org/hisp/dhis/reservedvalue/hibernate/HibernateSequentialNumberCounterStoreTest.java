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
package org.hisp.dhis.reservedvalue.hibernate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class HibernateSequentialNumberCounterStoreTest extends TransactionalIntegrationTest {

  @Autowired private DummyService dummyService;

  @Test
  void getNextValues() {
    List<Integer> result = dummyService.getNextValues("ABC", "ABC-#", 3);
    assertEquals(3, result.size());
    assertTrue(result.contains(1));
    assertTrue(result.contains(2));
    assertTrue(result.contains(3));
    result = dummyService.getNextValues("ABC", "ABC-#", 50);
    assertEquals(50, result.size());
    assertTrue(result.contains(4));
    assertTrue(result.contains(5));
    assertTrue(result.contains(52));
    assertTrue(result.contains(53));
  }

  private void test(final int threadCount) throws InterruptedException, ExecutionException {
    final String uid = RandomStringUtils.randomAlphabetic(3).toUpperCase();
    Callable<List<Integer>> task = () -> dummyService.getNextValues(uid, uid + "-#", 50);
    List<Callable<List<Integer>>> tasks = Collections.nCopies(threadCount, task);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    List<Future<List<Integer>>> futures = executorService.invokeAll(tasks);
    List<List<Integer>> resultList = new ArrayList<>(futures.size());
    // Check for exceptions
    for (Future<List<Integer>> future : futures) {
      // Throws an exception if an exception was thrown by the task.
      resultList.add(future.get());
    }
    assertEquals(threadCount, futures.size());
    Set<Integer> allIds = new HashSet<>();
    List<Integer> allIdList = new ArrayList<>();
    for (List<Integer> integers : resultList) {
      allIds.addAll(integers);
      allIdList.addAll(integers);
    }
    assertThat(allIds, hasSize(threadCount * 50));
    Collections.sort(allIdList);
    assertThat(allIdList.get(0), is(1));
    assertThat(allIdList.get(allIdList.size() - 1), is(50 * threadCount));
  }

  @Test
  void test1() throws InterruptedException, ExecutionException {
    test(1);
  }

  @Test
  void test4() throws InterruptedException, ExecutionException {
    test(4);
  }

  @Test
  void test8() throws InterruptedException, ExecutionException {
    test(8);
  }

  @Test
  void test16() throws InterruptedException, ExecutionException {
    test(16);
  }

  @Test
  void test32() throws InterruptedException, ExecutionException {
    test(32);
  }

  @Test
  void deleteCounter() {
    assertTrue(dummyService.getNextValues("ABC", "ABC-#", 3).contains(1));
    dummyService.deleteCounter("ABC");
    assertTrue(dummyService.getNextValues("ABC", "ABC-#", 3).contains(1));
    assertTrue(dummyService.getNextValues("ABC", "ABC-##", 3).contains(1));
    assertTrue(dummyService.getNextValues("ABC", "ABC-###", 3).contains(1));
    dummyService.deleteCounter("ABC");
    assertTrue(dummyService.getNextValues("ABC", "ABC-#", 3).contains(1));
    assertTrue(dummyService.getNextValues("ABC", "ABC-##", 3).contains(1));
    assertTrue(dummyService.getNextValues("ABC", "ABC-###", 3).contains(1));
  }

  @Configuration
  static class TestConfig {

    @Autowired private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Bean
    public DummyService dummyService() {
      return new DummyService(sequentialNumberCounterStore);
    }
  }
}
