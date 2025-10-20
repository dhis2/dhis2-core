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
package org.hisp.dhis.sql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.intellij.lang.annotations.Language;

/**
 * Base class with utilities for testing {@link QueryBuilder} usages.
 *
 * @author Jan Bernitt
 */
public abstract class AbstractQueryBuilderTest {

  private final AtomicReference<String> sql = new AtomicReference<>();
  private final Map<String, SQL.Param> params = new TreeMap<>();

  @Nonnull
  protected final SQL.QueryAPI createQueryAPI() {
    return SQL.of(this.sql::set, params::put);
  }

  protected final void assertSQL(@Language("sql") String expected, QueryBuilder actual) {
    assertNotNull(actual.stream()); // capture
    assertEquals(expected, sql.get());
  }

  protected final void assertCountSQL(@Language("sql") String expected, QueryBuilder actual) {
    assertEquals(0, actual.count()); // capture
    assertEquals(expected, sql.get());
  }

  protected final void assertSQL(
      @Language("sql") String expectedSql,
      @Nonnull Set<String> expectedParams,
      QueryBuilder actual) {
    assertSQL(expectedSql, actual);
    assertEquals(expectedParams, params.keySet());
  }

  protected final void assertSQL(
      @Language("sql") String expectedSql,
      @Nonnull Map<String, Object> expectedParams,
      QueryBuilder actual) {
    assertSQL(expectedSql, actual);
    assertParamsEquals(expectedParams);
  }

  protected final void assertCountSQL(
      @Language("sql") String expectedSql,
      @Nonnull Set<String> expectedParams,
      QueryBuilder actual) {
    assertCountSQL(expectedSql, actual);
    assertEquals(expectedParams, params.keySet());
  }

  protected final void assertCountSQL(
      @Language("sql") String expectedSql,
      @Nonnull Map<String, Object> expectedParams,
      QueryBuilder actual) {
    assertCountSQL(expectedSql, actual);
    assertParamsEquals(expectedParams);
  }

  private final void assertParamsEquals(@Nonnull Map<String, Object> expected) {
    assertEquals(expected.keySet(), params.keySet());
    for (String key : expected.keySet()) {
      Object pExpected = expected.get(key);
      SQL.Param param = params.get(key);
      Object pActual = param.value();
      Supplier<String> msg =
          () ->
              "parameter `%s` of type %s had a different value, expected %s but was: %s"
                  .formatted(param.name(), param.type(), pExpected, pActual);
      // automatically make a sensible comparison considering the parameter type
      if (pExpected == null) {
        assertNull(pActual, msg);
      } else if (param.type().isArray()) {
        Object[] arr = pExpected instanceof List<?> list ? list.toArray() : (Object[]) pExpected;
        assertArrayEquals(arr, (Object[]) pActual, msg);
      } else {
        assertEquals(pExpected, pActual, msg);
      }
    }
  }
}
