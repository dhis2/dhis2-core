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
package org.hisp.dhis.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class QueryUtilsTest {

  @Test
  void testParseValidEnum() {
    assertNotNull(QueryUtils.parseValue(ValueType.class, "INTEGER"));
    assertNotNull(QueryUtils.parseValue(ValueType.class, "TEXT"));
  }

  @Test
  void testParseValidInteger() {
    Integer value1 = QueryUtils.parseValue(Integer.class, "10");
    Integer value2 = QueryUtils.parseValue(Integer.class, "100");
    assertNotNull(value1);
    assertNotNull(value2);
    org.junit.jupiter.api.Assertions.assertSame(10, value1);
    org.junit.jupiter.api.Assertions.assertSame(100, value2);
  }

  @Test
  void testParseInvalidEnum() {
    QueryUtils.parseValue(ValueType.class, "INTEGER");
    assertThrows(QueryParserException.class, () -> QueryUtils.parseValue(ValueType.class, "ABC"));
  }

  @Test
  void testInvalidInteger() {
    QueryUtils.parseValue(Integer.class, "1");
    assertThrows(QueryParserException.class, () -> QueryUtils.parseValue(Integer.class, "ABC"));
  }

  @Test
  void testInvalidFloat() {
    QueryUtils.parseValue(Float.class, "1.2");
    assertThrows(QueryParserException.class, () -> QueryUtils.parseValue(Float.class, "ABC"));
  }

  @Test
  void testInvalidDouble() {
    QueryUtils.parseValue(Double.class, "1.2");
    assertThrows(QueryParserException.class, () -> QueryUtils.parseValue(Double.class, "ABC"));
  }

  @Test
  void testInvalidDate() {
    QueryUtils.parseValue(Date.class, "2014");
    assertThrows(QueryParserException.class, () -> QueryUtils.parseValue(Date.class, "ABC"));
  }

  @Test
  void testParseValue() {
    assertEquals("'abc'", QueryUtils.parseValue("abc"));
    assertEquals("123", QueryUtils.parseValue("123"));
  }

  @Test
  void testParserNotFound() {
    // Given
    final Class<User> nonSupportedClass = User.class;
    final String anyValue = "wewee-4343";
    // When
    final QueryParserException e =
        assertThrows(
            QueryParserException.class, () -> QueryUtils.parseValue(nonSupportedClass, anyValue));
    assertThat(
        "Unable to parse `" + anyValue + "` to `" + nonSupportedClass.getSimpleName() + "`.",
        is(e.getMessage()));
  }

  @Test
  void testParseSelectFields() {
    List<String> fields = new ArrayList<>();
    fields.add("ABC");
    fields.add("DEF");
    assertEquals("ABC,DEF", QueryUtils.parseSelectFields(fields));
  }

  @Test
  void testParseSelectFieldsNull() {
    assertEquals(" * ", QueryUtils.parseSelectFields(null));
  }

  @Test
  void testTransformCollectionValue() {
    assertEquals("('x','y')", QueryUtils.convertCollectionValue("[x,y]"));
    assertEquals("(1,2)", QueryUtils.convertCollectionValue("[1,2]"));
  }

  @Test
  void testParseFilterOperator() {
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("= ?", "5"),
        QueryUtils.parseFilterOperator("eq", "5"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("= ?", "ABC"),
        QueryUtils.parseFilterOperator("eq", "ABC"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg(" ilike ?", "abc"),
        QueryUtils.parseFilterOperator("ieq", "abc"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("like ?", "%abc%"),
        QueryUtils.parseFilterOperator("like", "abc"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg(" like ?", "%abc"),
        QueryUtils.parseFilterOperator("$like", "abc"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("in ?", "(a,b,c)"),
        QueryUtils.parseFilterOperator("in", "[a,b,c]"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("in ?", "(1,2,3)"),
        QueryUtils.parseFilterOperator("in", "[1,2,3]"));
    assertEquals(
        new QueryUtils.QueryPlaceHolderWithArg("is not null", null),
        QueryUtils.parseFilterOperator("!null", null));
  }

  @Test
  void testConvertOrderStringsNull() {
    assertEquals(List.of(), Order.parse(null));
  }

  @Test
  void testConvertOrderStrings() {
    List<Order> orders =
        Order.parse(
            List.of(
                "value1:asc",
                "value2:asc",
                "value3:iasc",
                "value4:desc",
                "value5:idesc",
                "value6:xdesc",
                "value7"));
    assertEquals(7, orders.size());
    assertEquals(orders.get(0), Order.asc("value1"));
    assertEquals(orders.get(1), Order.asc("value2"));
    assertEquals(orders.get(2), Order.iasc("value3"));
    assertEquals(orders.get(3), Order.desc("value4"));
    assertEquals(orders.get(4), Order.idesc("value5"));
    assertEquals(orders.get(5), Order.asc("value6"));
    assertEquals(orders.get(6), Order.asc("value7"));
  }
}
