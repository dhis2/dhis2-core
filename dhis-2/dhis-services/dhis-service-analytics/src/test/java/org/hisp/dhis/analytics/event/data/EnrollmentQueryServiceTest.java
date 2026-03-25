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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.Test;

class EnrollmentQueryServiceTest {

  @Test
  void shouldAcceptCreatedAndCompletedHeaders() {
    Program program = TestBase.createProgram('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .withHeaders(new LinkedHashSet<>(List.of("created", "completed")))
            .withSkipData(true)
            .withSkipMeta(true)
            .build();

    EnrollmentQueryService service =
        new EnrollmentQueryService(
            null,
            null,
            securityManager(params),
            eventQueryValidator(),
            new MetadataItemsHandler(null, null, null),
            new SchemeIdHandler(null),
            sqlBuilder());

    Grid grid = assertDoesNotThrow(() -> service.getEnrollments(params));

    assertEquals(
        List.of("created", "completed"), grid.getHeaders().stream().map(h -> h.getName()).toList());
  }

  private static AnalyticsSecurityManager securityManager(EventQueryParams params) {
    return proxy(
        AnalyticsSecurityManager.class,
        (method, args) -> {
          if ("withUserConstraints".equals(method.getName())) {
            return params;
          }
          return defaultValue(method.getReturnType());
        });
  }

  private static EventQueryValidator eventQueryValidator() {
    return proxy(
        EventQueryValidator.class,
        (method, args) -> {
          if ("getMaxLimit".equals(method.getName())) {
            return 0;
          }
          return defaultValue(method.getReturnType());
        });
  }

  private static SqlBuilder sqlBuilder() {
    return proxy(
        SqlBuilder.class,
        (method, args) -> {
          if ("supportsGeospatialData".equals(method.getName())) {
            return false;
          }
          return defaultValue(method.getReturnType());
        });
  }

  private interface InvocationResult {
    Object invoke(java.lang.reflect.Method method, Object[] args);
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationResult result) {
    return (T)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> result.invoke(method, args));
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(type)) {
      return false;
    }
    if (int.class.equals(type)) {
      return 0;
    }
    if (long.class.equals(type)) {
      return 0L;
    }
    if (double.class.equals(type)) {
      return 0d;
    }
    if (float.class.equals(type)) {
      return 0f;
    }
    if (short.class.equals(type)) {
      return (short) 0;
    }
    if (byte.class.equals(type)) {
      return (byte) 0;
    }
    if (char.class.equals(type)) {
      return (char) 0;
    }
    return null;
  }
}
