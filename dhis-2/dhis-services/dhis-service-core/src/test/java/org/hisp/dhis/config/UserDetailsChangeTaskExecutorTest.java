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
package org.hisp.dhis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The executor that marks users with changed user details must run every task it is given: a task
 * it drops leaves the sessions of its users on their old principal.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserDetailsChangeTaskExecutorTest {

  private final ThreadPoolTaskExecutor executor =
      (ThreadPoolTaskExecutor) new ServiceConfig().userDetailsChangeTaskExecutor();

  @AfterEach
  void tearDown() {
    executor.shutdown();
  }

  @Test
  void runsEveryTaskOfABurstThatArrivesWhileItIsBusy() throws InterruptedException {
    CountDownLatch busy = new CountDownLatch(1);
    executor.execute(
        () -> {
          try {
            busy.await();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        });
    int burst = 5000;
    ConcurrentLinkedQueue<Integer> ran = new ConcurrentLinkedQueue<>();
    CountDownLatch done = new CountDownLatch(burst);

    IntStream.range(0, burst)
        .forEach(
            task ->
                executor.execute(
                    () -> {
                      ran.add(task);
                      done.countDown();
                    }));
    busy.countDown();

    assertTrue(done.await(30, TimeUnit.SECONDS), "not every task ran");
    assertEquals(IntStream.range(0, burst).boxed().toList(), List.copyOf(ran));
  }
}
