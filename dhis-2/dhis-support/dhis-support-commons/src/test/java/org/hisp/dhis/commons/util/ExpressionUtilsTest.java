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
package org.hisp.dhis.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class ExpressionUtilsTest {

  private static final double DELTA = 0.01;

  @Test
  void testEvaluateToDouble() {
    assertEquals(ExpressionUtils.evaluateToDouble("3", null), DELTA, 3d);
    assertEquals(ExpressionUtils.evaluateToDouble("3.45", null), DELTA, 3.45);
    assertEquals(ExpressionUtils.evaluateToDouble("2 + 3", null), DELTA, 5d);
    assertEquals(ExpressionUtils.evaluateToDouble("5 + -3", null), DELTA, 2d);
    assertEquals(ExpressionUtils.evaluateToDouble("2 + 3 + -4", null), DELTA, 1d);
    assertEquals(ExpressionUtils.evaluateToDouble("12.4 + 3.2", null), DELTA, 15.6);
    assertEquals(ExpressionUtils.evaluateToDouble("2 > 1 ? 2.0 : 1.0", null), DELTA, 2.0);
    assertEquals(ExpressionUtils.evaluateToDouble("2 > 4 ? 2.0 : 1.0", null), DELTA, 1.0);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(3)", null), DELTA, 3d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(-3) + 2.0", null), DELTA, 2d);
    assertEquals(
        ExpressionUtils.evaluateToDouble("d2:zing(-1) + 4 + d2:zing(-2)", null), DELTA, 4d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:oizp(-4)", null), DELTA, 0.01d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:oizp(0)", null), DELTA, 1d);
    assertEquals(
        ExpressionUtils.evaluateToDouble("d2:oizp(-4) + d2:oizp(0) + d2:oizp(3.0)", null),
        DELTA,
        2d);
    assertEquals(
        ExpressionUtils.evaluateToDouble("d2:daysBetween('2015-03-01','2015-03-04')", null),
        DELTA,
        3d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:oizp(d2:zing(3))", null), DELTA, 1d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(d2:oizp(3))", null), DELTA, 1d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zpvc(1,3)", null), DELTA, 2d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zpvc(1,-1,2,-3,0)", null), DELTA, 3d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:condition('3 > 2',4,3)", null), DELTA, 4d);
    assertEquals(ExpressionUtils.evaluateToDouble("2 + null + 1", null), DELTA, 3d);
    assertEquals(ExpressionUtils.evaluateToDouble("null + 4", null), DELTA, 4d);
    assertEquals(ExpressionUtils.evaluateToDouble("(3 + 2) - null", null), DELTA, 5d);
    assertEquals(ExpressionUtils.evaluateToDouble("(3 + 2) - null + 4 + null", null), DELTA, 9d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(null) + 2", null), DELTA, 2d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:oizp(null) + 2", null), DELTA, 2d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zpvc(1,null,2,-3,0)", null), DELTA, 3d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zpvc(null,null,2,-3,0)", null), DELTA, 2d);
  }

  @Test
  void testEvaluateToDoubleWithVars() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("v1", 4d);
    vars.put("v2", -5d);
    assertEquals(ExpressionUtils.evaluateToDouble("v1 + 3", vars), DELTA, 7d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(v1)", vars), DELTA, 4d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(v2)", vars), DELTA, 0.01d);
    assertEquals(ExpressionUtils.evaluateToDouble("d2:zing(v1) + d2:zing(v2)", vars), DELTA, 4d);
  }

  @Test
  void testEvaluateToDoubleZeroPositiveValueCount() {
    String expression = "d2:zpvc(2,3,-1,0)";
    assertEquals(ExpressionUtils.evaluateToDouble(expression, null), DELTA, 3d);
    expression = "(d2:zing(4) + d2:zing(0) + d2:zing(-1)) / d2:zpvc(2,0,-1)";
    assertEquals(ExpressionUtils.evaluateToDouble(expression, null), DELTA, 2d);
    expression =
        "((d2:zing(4) + d2:zing(0) + d2:zing(-1)) / d2:zpvc(2,0,-1) * 0.25) + "
            + "((d2:zing(8) + d2:zing(0) + d2:zing(-1)) / d2:zpvc(2,0,-1) * 0.75)";
    assertEquals(ExpressionUtils.evaluateToDouble(expression, null), DELTA, 3.5);
  }

  @Test
  void testEvaluate() {
    assertEquals(4, ExpressionUtils.evaluate("d2:condition('3 > 2',4,3)", null));
    assertEquals(3, ExpressionUtils.evaluate("d2:condition('5 > 7',4,3)", null));
    assertEquals(
        "yes", ExpressionUtils.evaluate("d2:condition(\"'goat' == 'goat'\",'yes','no')", null));
    assertEquals(
        "no", ExpressionUtils.evaluate("d2:condition(\"'goat' != 'goat'\",'yes','no')", null));
    assertEquals(
        "indoor",
        ExpressionUtils.evaluate("d2:condition(\"'weather' == 'nice'\",'beach','indoor')", null));
  }

  @Test
  void testIsTrue() {
    assertTrue(ExpressionUtils.isTrue("2 > 1", null));
    assertTrue(ExpressionUtils.isTrue("(2 * 3) == 6", null));
    assertTrue(ExpressionUtils.isTrue("\"a\" == \"a\"", null));
    assertTrue(ExpressionUtils.isTrue("'b' == 'b'", null));
    assertTrue(ExpressionUtils.isTrue("('b' == 'b') && ('c' == 'c')", null));
    assertTrue(ExpressionUtils.isTrue("'goat' == 'goat'", null));
    assertFalse(ExpressionUtils.isTrue("2 < 1", null));
    assertFalse(ExpressionUtils.isTrue("(2 * 3) == 8", null));
    assertFalse(ExpressionUtils.isTrue("\"a\" == \"b\"", null));
    assertFalse(ExpressionUtils.isTrue("'b' == 'c'", null));
    assertFalse(ExpressionUtils.isTrue("'goat' == 'cow'", null));
  }

  @Test
  void testIsTrueWithVars() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("v1", "4");
    vars.put("v2", "12");
    vars.put("v3", "goat");
    vars.put("v4", "horse");
    assertTrue(ExpressionUtils.isTrue("v1 > 1", vars));
    assertTrue(ExpressionUtils.isTrue("v2 < 18", vars));
    assertTrue(ExpressionUtils.isTrue("v2 < '23'", vars));
    assertTrue(ExpressionUtils.isTrue("v3 == 'goat'", vars));
    assertTrue(ExpressionUtils.isTrue("v4 == 'horse'", vars));
    assertTrue(ExpressionUtils.isTrue("v4 == \"horse\"", vars));
    assertFalse(ExpressionUtils.isTrue("v1 < 1", vars));
    assertFalse(ExpressionUtils.isTrue("v2 > 18", vars));
    assertFalse(ExpressionUtils.isTrue("v2 > '23'", vars));
    assertFalse(ExpressionUtils.isTrue("v3 == 'cow'", vars));
    assertFalse(ExpressionUtils.isTrue("v4 == 'goat'", vars));
    assertFalse(ExpressionUtils.isTrue("v4 == \"goat\"", vars));
  }

  @Test
  void testIsBoolean() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("uA2hsh8j26j", "FEMALE");
    vars.put("v2", "12");
    assertTrue(ExpressionUtils.isBoolean("2 > 1", null));
    assertTrue(ExpressionUtils.isBoolean("(2 * 3) == 6", null));
    assertTrue(ExpressionUtils.isBoolean("\"a\" == \"a\"", null));
    assertTrue(ExpressionUtils.isBoolean("'b' == 'b'", null));
    assertTrue(ExpressionUtils.isBoolean("('b' == 'b') && ('c' == 'c')", null));
    assertTrue(ExpressionUtils.isBoolean("'goat' == 'goat'", null));
    assertFalse(ExpressionUtils.isBoolean("4", null));
    assertFalse(ExpressionUtils.isBoolean("3 + 2", null));
    assertFalse(ExpressionUtils.isBoolean("someinvalid expr", null));
  }

  @Test
  void testAsSql() {
    assertEquals("2 > 1 and 3 < 4", ExpressionUtils.asSql("2 > 1 && 3 < 4"));
    assertEquals("2 > 1 or 3 < 4", ExpressionUtils.asSql("2 > 1 || 3 < 4"));
    assertEquals("'a' = 1", ExpressionUtils.asSql("'a' == 1"));
    assertEquals(
        "\"oZg33kd9taw\" = 'Female'", ExpressionUtils.asSql("\"oZg33kd9taw\" == 'Female'"));
  }

  @Test
  void testIsValid() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("v1", "12");
    assertTrue(ExpressionUtils.isValid("2 + 8", null));
    assertTrue(ExpressionUtils.isValid("3 - v1", vars));
    assertTrue(ExpressionUtils.isValid("d2:zing(1)", null));
    assertTrue(ExpressionUtils.isValid("d2:oizp(1)", null));
    assertTrue(ExpressionUtils.isValid("d2:oizp(d2:zing(1))", null));
    assertTrue(ExpressionUtils.isValid("d2:daysBetween('2015-02-01','2015-04-02')", null));
    assertTrue(ExpressionUtils.isValid("(d2:zing(1)+d2:zing(1))*50/1", null));
    assertTrue(ExpressionUtils.isValid("d2:condition('1 > 100',5,100)", null));
    assertTrue(ExpressionUtils.isValid("1/(1/100)", null));
    assertTrue(ExpressionUtils.isValid("SUM(1)", null));
    assertTrue(ExpressionUtils.isValid("avg(2+1)", null));
    assertFalse(ExpressionUtils.isValid("2 a 3", null));
    assertFalse(ExpressionUtils.isValid("4 b", vars));
    assertFalse(ExpressionUtils.isValid("4 + A", vars));
    assertFalse(ExpressionUtils.isValid("4 + someunkownvar", vars));
    assertFalse(ExpressionUtils.isValid("aver(2+1)", null));
  }

  @Test
  void testIsNumeric() {
    assertTrue(ExpressionUtils.isNumeric("123"));
    assertTrue(ExpressionUtils.isNumeric("0"));
    assertTrue(ExpressionUtils.isNumeric("1.2"));
    assertTrue(ExpressionUtils.isNumeric("12.34"));
    assertTrue(ExpressionUtils.isNumeric("0.0"));
    assertTrue(ExpressionUtils.isNumeric("1.234"));
    assertTrue(ExpressionUtils.isNumeric("-1234"));
    assertTrue(ExpressionUtils.isNumeric("-12.34"));
    assertTrue(ExpressionUtils.isNumeric("-0.34"));
    assertTrue(ExpressionUtils.isNumeric("6.34"));
    assertTrue(ExpressionUtils.isNumeric("3.34"));
    assertTrue(ExpressionUtils.isNumeric("2.43"));
    assertFalse(ExpressionUtils.isNumeric("Hey"));
    assertFalse(ExpressionUtils.isNumeric("45 Perinatal Condition"));
    assertFalse(ExpressionUtils.isNumeric("Long street 2"));
    assertFalse(ExpressionUtils.isNumeric("1.2f"));
    assertFalse(ExpressionUtils.isNumeric("1 234"));
    assertFalse(ExpressionUtils.isNumeric("."));
    assertFalse(ExpressionUtils.isNumeric("1."));
    assertFalse(ExpressionUtils.isNumeric(".1"));
    assertFalse(ExpressionUtils.isNumeric(""));
    assertFalse(ExpressionUtils.isNumeric(" "));
    assertFalse(ExpressionUtils.isNumeric("+1234  "));
    assertFalse(ExpressionUtils.isNumeric("1234  "));
    assertFalse(ExpressionUtils.isNumeric("  1234"));
    assertFalse(ExpressionUtils.isNumeric("1,234"));
    assertFalse(ExpressionUtils.isNumeric("0,1"));
    assertFalse(ExpressionUtils.isNumeric("0,"));
    assertFalse(ExpressionUtils.isNumeric("0."));
    assertFalse(ExpressionUtils.isNumeric("01"));
    assertFalse(ExpressionUtils.isNumeric("001"));
    assertFalse(ExpressionUtils.isNumeric("00.23"));
    assertFalse(ExpressionUtils.isNumeric("01.23"));
    assertFalse(ExpressionUtils.isNumeric("4.23E"));
    assertFalse(ExpressionUtils.isNumeric("4.23Ef"));
    assertFalse(ExpressionUtils.isNumeric("E5"));
  }
}
