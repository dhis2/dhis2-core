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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

public class StubDriver implements Driver {
  @Override
  public Connection connect(String s, Properties properties) throws SQLException {
    Statement stubStatement = proxyFor(Statement.class, null);
    return proxyFor(
        Connection.class,
        (proxy, method, args) -> {
          return switch (method.getName()) {
            case "createStatement" -> stubStatement;
            case "isValid" -> true;
            case "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED;
            default -> null;
          };
        });
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxyFor(Class<T> iface, java.lang.reflect.InvocationHandler extra) {
    return (T)
        Proxy.newProxyInstance(
            StubDriver.class.getClassLoader(),
            new Class[] {iface},
            (proxy, method, args) -> {
              if (extra != null) {
                Object result = extra.invoke(proxy, method, args);
                if (result != null) return result;
              }
              return defaultFor(proxy, method);
            });
  }

  private static Object defaultFor(Object proxy, Method method) {
    String name = method.getName();
    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
    if ("equals".equals(name)) return false;
    if ("toString".equals(name)) return proxy.getClass().getName() + "@stub";
    Class<?> ret = method.getReturnType();
    if (ret == boolean.class || ret == Boolean.class) return false;
    if (ret == int.class || ret == Integer.class) return 0;
    if (ret == long.class || ret == Long.class) return 0L;
    if (ret == double.class || ret == Double.class) return 0.0;
    if (ret == float.class || ret == Float.class) return 0.0f;
    if (ret == short.class || ret == Short.class) return (short) 0;
    if (ret == byte.class || ret == Byte.class) return (byte) 0;
    if (ret == char.class || ret == Character.class) return (char) 0;
    return null;
  }

  @Override
  public boolean acceptsURL(String url) {
    return url.equals("jdbc:fake:db");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) {
    return new DriverPropertyInfo[0];
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() {
    return null;
  }
}
