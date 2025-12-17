/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * DataSource wrapper that logs the time taken to acquire a connection from the pool and the
 * duration the connection was held before being released. This helps identify connection pool
 * contention issues where threads wait for available connections, as well as long-lived connections
 * that may contribute to pool exhaustion.
 *
 * <p>Enable DEBUG logging for this class to see acquisition times, release times, and hold
 * durations.
 */
@Slf4j
public class ConnectionLoggingDataSource extends DelegatingDataSource {

  public ConnectionLoggingDataSource(DataSource targetDataSource) {
    super(targetDataSource);
  }

  @Override
  public Connection getConnection() throws SQLException {
    long startNanos = System.nanoTime();
    Connection connection = super.getConnection();
    long acquisitionMs = (System.nanoTime() - startNanos) / 1_000_000;
    return wrapConnection(connection, acquisitionMs);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    long startNanos = System.nanoTime();
    Connection connection = super.getConnection(username, password);
    long acquisitionMs = (System.nanoTime() - startNanos) / 1_000_000;
    return wrapConnection(connection, acquisitionMs);
  }

  private Connection wrapConnection(Connection connection, long acquisitionMs) {
    log.debug("CONN_ACQUIRED wait_ms={}", acquisitionMs);
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            new ConnectionTimingHandler(connection, acquisitionMs));
  }

  private static class ConnectionTimingHandler implements InvocationHandler {
    private final Connection delegate;
    private final long acquisitionMs;
    private final long acquiredAtNanos;

    public ConnectionTimingHandler(Connection delegate, long acquisitionMs) {
      this.delegate = delegate;
      this.acquisitionMs = acquisitionMs;
      this.acquiredAtNanos = System.nanoTime();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        long heldMs = (System.nanoTime() - acquiredAtNanos) / 1_000_000;
        log.debug("CONN_RELEASED wait_ms={} held_ms={}", acquisitionMs, heldMs);
      }

      try {
        return method.invoke(delegate, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
