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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes a static method to fetch an Executor for the Aggregates operations
 *
 * @author Luciano Fiandesio
 */
@Slf4j
class ThreadPoolManager {

  private ThreadPoolManager() {
    throw new IllegalStateException("only used for its static fields");
  }

  private static final ThreadFactory threadFactory =
      new ThreadFactoryBuilder().setNameFormat("TRACKER-TE-FETCH-%d").setDaemon(true).build();

  /**
   * Cached thread pool: not bound to a size, but can reuse existing threads. Equivalent to
   * Executors.newCachedThreadPool() but using ThreadPoolExecutor directly so we can expose it via
   * JMX.
   */
  private static final ThreadPoolExecutor AGGREGATE_THREAD_POOL = createAndRegisterPool();

  private static ThreadPoolExecutor createAndRegisterPool() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);

    try {
      ObjectName name = new ObjectName("org.hisp.dhis:type=ThreadPool,name=TrackerTeFetch");
      ManagementFactory.getPlatformMBeanServer()
          .registerMBean(new ThreadPoolExecutorMXBeanImpl(executor), name);
      log.debug("Registered TrackerTeFetch thread pool MBean");
    } catch (Exception e) {
      log.warn("Failed to register TrackerTeFetch thread pool MBean", e);
    }

    return executor;
  }

  static Executor getPool() {
    return AGGREGATE_THREAD_POOL;
  }

  /** MXBean interface for exposing ThreadPoolExecutor metrics via JMX. */
  public interface ThreadPoolExecutorMXBean {
    int getActiveCount();

    int getPoolSize();

    int getCorePoolSize();

    int getMaximumPoolSize();

    int getLargestPoolSize();

    long getCompletedTaskCount();

    long getTaskCount();

    int getQueueSize();
  }

  /** MXBean implementation that delegates to the ThreadPoolExecutor. */
  static class ThreadPoolExecutorMXBeanImpl implements ThreadPoolExecutorMXBean {
    private final ThreadPoolExecutor executor;

    ThreadPoolExecutorMXBeanImpl(ThreadPoolExecutor executor) {
      this.executor = executor;
    }

    @Override
    public int getActiveCount() {
      return executor.getActiveCount();
    }

    @Override
    public int getPoolSize() {
      return executor.getPoolSize();
    }

    @Override
    public int getCorePoolSize() {
      return executor.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize() {
      return executor.getMaximumPoolSize();
    }

    @Override
    public int getLargestPoolSize() {
      return executor.getLargestPoolSize();
    }

    @Override
    public long getCompletedTaskCount() {
      return executor.getCompletedTaskCount();
    }

    @Override
    public long getTaskCount() {
      return executor.getTaskCount();
    }

    @Override
    public int getQueueSize() {
      return executor.getQueue().size();
    }
  }
}
