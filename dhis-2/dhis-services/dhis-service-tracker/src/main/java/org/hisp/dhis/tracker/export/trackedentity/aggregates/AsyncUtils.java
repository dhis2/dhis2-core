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

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * @author Luciano Fiandesio
 */
class AsyncUtils {
  AsyncUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Executes the Supplier asynchronously using the thread pool from the provided {@see Executor}
   *
   * @param condition A condition that, if true, executes the Supplier, if false, returns an empty
   *     Multimap
   * @param supplier The Supplier to execute
   * @param executor an Executor instance
   * @return A CompletableFuture with the result of the Supplier
   */
  static <T> CompletableFuture<Multimap<String, T>> conditionalAsyncFetch(
      boolean condition, Supplier<Multimap<String, T>> supplier, Executor executor) {
    return (condition
        ? supplyAsync(withMdc(supplier), executor)
        : supplyAsync(withMdc(ArrayListMultimap::create), executor));
  }

  /**
   * Wraps a Supplier to propagate the MDC context from the calling thread to the async thread. This
   * ensures that log statements in async operations include the same contextual information (e.g.,
   * sessionId) as the parent HTTP request.
   *
   * @param supplier The Supplier to wrap
   * @return A Supplier that propagates MDC context
   */
  static <T> Supplier<T> withMdc(Supplier<T> supplier) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    return () -> {
      Map<String, String> previous = MDC.getCopyOfContextMap();
      try {
        if (contextMap != null) {
          MDC.setContextMap(contextMap);
        }
        return supplier.get();
      } finally {
        if (previous != null) {
          MDC.setContextMap(previous);
        } else {
          MDC.clear();
        }
      }
    };
  }

  /**
   * Wraps a Function to propagate the MDC context from the calling thread to the async thread. This
   * is useful for parallelStream().map() operations that spawn work on ForkJoinPool.commonPool(),
   * ensuring that log statements and SQL comments include the request ID.
   *
   * @param function The Function to wrap
   * @return A Function that propagates MDC context
   */
  static <T, R> Function<T, R> withMdcFunction(Function<T, R> function) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    return input -> {
      Map<String, String> previous = MDC.getCopyOfContextMap();
      try {
        if (contextMap != null) {
          MDC.setContextMap(contextMap);
        }
        return function.apply(input);
      } finally {
        if (previous != null) {
          MDC.setContextMap(previous);
        } else {
          MDC.clear();
        }
      }
    };
  }
}
