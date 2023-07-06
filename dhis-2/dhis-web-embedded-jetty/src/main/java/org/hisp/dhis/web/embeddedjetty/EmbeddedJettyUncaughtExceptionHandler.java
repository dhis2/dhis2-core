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
package org.hisp.dhis.web.embeddedjetty;

import org.slf4j.Logger;

public final class EmbeddedJettyUncaughtExceptionHandler {

  private EmbeddedJettyUncaughtExceptionHandler() {}

  /**
   * Returns an exception handler that exits the system. This is particularly useful for the main
   * thread, which may start up other, non-daemon threads, but fail to fully initialize the
   * application successfully.
   *
   * <p>Example usage:
   *
   * <pre>
   * public static void main(String[] args) {
   *   Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
   *   ...
   * </pre>
   *
   * <p>The returned handler logs any exception as error and then shuts down the process with an
   * exit status of 1, indicating abnormal termination.
   */
  public static Thread.UncaughtExceptionHandler systemExit(Logger log) {
    return new Exiter(log, Runtime.getRuntime());
  }

  static final class Exiter implements Thread.UncaughtExceptionHandler {
    private final Runtime runtime;

    private final Logger logger;

    Exiter(Logger log, Runtime runtime) {
      this.logger = log;
      this.runtime = runtime;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      try {
        logger.error(String.format("Caught an exception in %s.  Shutting down.", t), e);
      } catch (Throwable errorInLogging) {
        // If logging fails, e.g. due to low memory conditions, at least
        // try to log the
        // message and the cause for the failed logging to system error.
        System.err.println(e.getMessage());
        System.err.println(errorInLogging.getMessage());
      } finally {
        runtime.exit(1);
      }
    }
  }
}
