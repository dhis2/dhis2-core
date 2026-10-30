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
package org.hisp.dhis.analytics.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlRendererTest {

  @Test
  void testRenderWithStringParameter() {
    String sql = "SELECT * FROM table WHERE name = :name";
    Map<String, Object> params = Map.of("name", "test");

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE name = 'test'", result);
  }

  @Test
  void testRenderWithNumericParameter() {
    String sql = "SELECT * FROM table WHERE id = :id";
    Map<String, Object> params = Map.of("id", 42);

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE id = 42", result);
  }

  @Test
  void testRenderWithNullParameter() {
    String sql = "SELECT * FROM table WHERE value = :value";
    Map<String, Object> params = new java.util.HashMap<>();
    params.put("value", null);

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE value = null", result);
  }

  @Test
  void testRenderWithLocalDateParameter() {
    String sql = "SELECT * FROM table WHERE date = :date";
    Map<String, Object> params = Map.of("date", LocalDate.of(2024, 6, 15));

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE date = '2024-06-15'", result);
  }

  @Test
  void testRenderWithLocalDateTimeParameter() {
    String sql = "SELECT * FROM table WHERE timestamp = :ts";
    Map<String, Object> params = Map.of("ts", LocalDateTime.of(2024, 6, 15, 10, 30, 45));

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE timestamp = '2024-06-15 10:30:45'", result);
  }

  @Test
  void testRenderWithJavaUtilDateParameter() {
    String sql = "SELECT * FROM table WHERE created = :created";
    // Create a date: 2024-06-15 10:30:45 in system default timezone
    LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
    Date date = Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    Map<String, Object> params = Map.of("created", date);

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE created = '2024-06-15 10:30:45'", result);
  }

  @Test
  void testRenderWithCollectionParameter() {
    String sql = "SELECT * FROM table WHERE id IN (:ids)";
    Map<String, Object> params = Map.of("ids", List.of("a", "b", "c"));

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE id IN ([a, b, c])", result);
  }

  @Test
  void testRenderWithUnknownParameter() {
    String sql = "SELECT * FROM table WHERE name = :name AND status = :status";
    Map<String, Object> params = Map.of("name", "test");

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE name = 'test' AND status = :status", result);
  }

  @Test
  void testRenderWithMultipleParameters() {
    String sql = "SELECT * FROM table WHERE name = :name AND age = :age AND active = :active";
    Map<String, Object> params = Map.of("name", "test", "age", 25, "active", true);

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE name = 'test' AND age = 25 AND active = true", result);
  }

  @Test
  void testRenderWithNumericParameterName() {
    String sql = "SELECT * FROM table WHERE id = :1";
    Map<String, Object> params = Map.of("1", 42);

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE id = 42", result);
  }

  @Test
  void testRenderWithNoParameters() {
    String sql = "SELECT * FROM table";
    Map<String, Object> params = Map.of();

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table", result);
  }

  @Test
  void testRenderWithUnderscoreInParameterName() {
    String sql = "SELECT * FROM table WHERE user_name = :user_name";
    Map<String, Object> params = Map.of("user_name", "john");

    String result = SqlRenderer.render(sql, params);

    assertEquals("SELECT * FROM table WHERE user_name = 'john'", result);
  }
}
