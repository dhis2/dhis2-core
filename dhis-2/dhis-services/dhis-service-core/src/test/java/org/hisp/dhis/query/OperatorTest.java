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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.hisp.dhis.query.operators.BetweenOperator;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.GreaterEqualOperator;
import org.hisp.dhis.query.operators.GreaterThanOperator;
import org.hisp.dhis.query.operators.InOperator;
import org.hisp.dhis.query.operators.LessEqualOperator;
import org.hisp.dhis.query.operators.LessThanOperator;
import org.hisp.dhis.query.operators.LikeOperator;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.query.operators.NotLikeOperator;
import org.hisp.dhis.query.operators.NotNullOperator;
import org.hisp.dhis.query.operators.NullOperator;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class OperatorTest {

  enum TestEnum {
    A,
    B,
    C
  }

  @Test
  void testBetweenValidTypes() {
    BetweenOperator<String> operator = new BetweenOperator<>("10", "20");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testBetweenInt() {
    BetweenOperator<String> operator = new BetweenOperator<>("10", "20");
    assertTrue(operator.test(10));
    assertTrue(operator.test(15));
    assertTrue(operator.test(20));
    assertFalse(operator.test(9));
    assertFalse(operator.test(21));
  }

  @Test
  void testBetweenCollection() {
    BetweenOperator<String> operator = new BetweenOperator<>("2", "4");
    assertFalse(operator.test(Collections.singletonList(1)));
    assertTrue(operator.test(Arrays.asList(1, 2)));
    assertTrue(operator.test(Arrays.asList(1, 2, 3)));
    assertTrue(operator.test(Arrays.asList(1, 2, 3, 4)));
    assertFalse(operator.test(Arrays.asList(1, 2, 3, 4, 5)));
  }

  @Test
  void testEqualValidTypes() {
    EqualOperator<String> operator = new EqualOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertTrue(operator.isValid(Enum.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testEqual() {
    EqualOperator<String> operator = new EqualOperator<>("operator");
    assertTrue(operator.test("operator"));
    assertFalse(operator.test("not operator"));
  }

  @Test
  void testEqualEnum() {
    assertTrue(new EqualOperator<>("A").test(TestEnum.A));
    assertTrue(new EqualOperator<>("B").test(TestEnum.B));
    assertTrue(new EqualOperator<>("C").test(TestEnum.C));
    assertFalse(new EqualOperator<>("A").test("abc"));
  }

  @Test
  void testNotEqualValidTypes() {
    NotEqualOperator<String> operator = new NotEqualOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testNotEqual() {
    NotEqualOperator<String> operator = new NotEqualOperator<>("operator");
    assertFalse(operator.test("operator"));
    assertTrue(operator.test(Boolean.TRUE));
  }

  @Test
  void testGreaterEqualValidTypes() {
    GreaterEqualOperator<String> operator = new GreaterEqualOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testGreaterEqual() {
    GreaterEqualOperator<String> operator = new GreaterEqualOperator<>("10");
    assertFalse(operator.test(6));
    assertFalse(operator.test(7));
    assertFalse(operator.test(8));
    assertFalse(operator.test(9));
    assertTrue(operator.test(10));
    assertTrue(operator.test(11));
    assertTrue(operator.test(12));
    assertTrue(operator.test(13));
  }

  @Test
  void testGreaterThanValidTypes() {
    GreaterThanOperator<String> operator = new GreaterThanOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testGreaterThan() {
    GreaterThanOperator<String> operator = new GreaterThanOperator<>("10");
    assertFalse(operator.test(6));
    assertFalse(operator.test(7));
    assertFalse(operator.test(8));
    assertFalse(operator.test(9));
    assertFalse(operator.test(10));
    assertTrue(operator.test(11));
    assertTrue(operator.test(12));
    assertTrue(operator.test(13));
  }

  @Test
  void testLikeValidTypes() {
    LikeOperator<String> operator = new LikeOperator<>("operator", true, MatchMode.ANYWHERE);
    assertTrue(operator.isValid(String.class));
    assertFalse(operator.isValid(Number.class));
    assertFalse(operator.isValid(Date.class));
    assertFalse(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testLikeAnywhere() {
    LikeOperator<String> operator = new LikeOperator<>("oper", true, MatchMode.ANYWHERE);
    assertTrue(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testLikeStart() {
    LikeOperator<String> operator = new LikeOperator<>("oper", true, MatchMode.START);
    assertTrue(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testLikeEnd() {
    LikeOperator<String> operator = new LikeOperator<>("ator", true, MatchMode.END);
    assertTrue(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testILikeAnywhere() {
    LikeOperator<String> operator = new LikeOperator<>("erat", false, MatchMode.ANYWHERE);
    assertTrue(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testILikeStart() {
    LikeOperator<String> operator = new LikeOperator<>("oper", false, MatchMode.START);
    assertTrue(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testILikeEnd() {
    LikeOperator<String> operator = new LikeOperator<>("ator", false, MatchMode.END);
    assertTrue(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testLessEqualValidTypes() {
    LessEqualOperator<String> operator = new LessEqualOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testLessEqual() {
    LessEqualOperator<String> operator = new LessEqualOperator<>("10");
    assertTrue(operator.test(6));
    assertTrue(operator.test(7));
    assertTrue(operator.test(8));
    assertTrue(operator.test(9));
    assertTrue(operator.test(10));
    assertFalse(operator.test(11));
    assertFalse(operator.test(12));
    assertFalse(operator.test(13));
  }

  @Test
  void testLessThanValidTypes() {
    LessThanOperator<String> operator = new LessThanOperator<>("operator");
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testLessThan() {
    LessThanOperator<String> operator = new LessThanOperator<>("10");
    assertTrue(operator.test(6));
    assertTrue(operator.test(7));
    assertTrue(operator.test(8));
    assertTrue(operator.test(9));
    assertFalse(operator.test(10));
    assertFalse(operator.test(11));
    assertFalse(operator.test(12));
    assertFalse(operator.test(13));
  }

  @Test
  void testNullValidTypes() {
    NullOperator<String> operator = new NullOperator<>();
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testNull() {
    NullOperator<String> operator = new NullOperator<>();
    assertTrue(operator.test(null));
    assertFalse(operator.test("test"));
  }

  @Test
  void testNotNullValidTypes() {
    NotNullOperator<String> operator = new NotNullOperator<>();
    assertTrue(operator.isValid(String.class));
    assertTrue(operator.isValid(Number.class));
    assertTrue(operator.isValid(Date.class));
    assertTrue(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testNotNull() {
    NotNullOperator<String> operator = new NotNullOperator<>();
    assertFalse(operator.test(null));
    assertTrue(operator.test("test"));
  }

  @Test
  void testInValidTypes() {
    InOperator<Integer> operator = new InOperator<>(Arrays.asList(1, 2, 3));
    assertTrue(operator.isValid(Collection.class));
  }

  @Test
  void testInInt() {
    InOperator<Integer> operator = new InOperator<>(Arrays.asList(1, 2, 3));
    assertFalse(operator.test(0));
    assertTrue(operator.test(1));
    assertTrue(operator.test(2));
    assertTrue(operator.test(3));
    assertFalse(operator.test(4));
  }

  @Test
  void testInString() {
    InOperator<String> operator = new InOperator<>(Arrays.asList("b", "c", "d"));
    assertFalse(operator.test("a"));
    assertTrue(operator.test("b"));
    assertTrue(operator.test("c"));
    assertTrue(operator.test("d"));
    assertFalse(operator.test("e"));
  }

  @Test
  void testInEnum() {
    InOperator<String> operator = new InOperator<>(Arrays.asList("A", "B"));
    assertTrue(operator.test(TestEnum.A));
    assertTrue(operator.test(TestEnum.B));
    assertFalse(operator.test(TestEnum.C));
    assertFalse(operator.test("abc"));
  }

  @Test
  void testNotLikeValidTypes() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("operator", true, MatchMode.ANYWHERE);
    assertTrue(operator.isValid(String.class));
    assertFalse(operator.isValid(Number.class));
    assertFalse(operator.isValid(Date.class));
    assertFalse(operator.isValid(Boolean.class));
    assertFalse(operator.isValid(Collection.class));
  }

  @Test
  void testNotLikeAnywhere() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("oper", true, MatchMode.ANYWHERE);
    assertFalse(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }

  @Test
  void testNotLikeStart() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("oper", true, MatchMode.START);
    assertFalse(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }

  @Test
  void tesNotLikeEnd() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("ator", true, MatchMode.END);
    assertFalse(operator.test("operator"));
    assertTrue(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }

  @Test
  void testINotLikeAnywhere() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("erat", false, MatchMode.ANYWHERE);
    assertFalse(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }

  @Test
  void testINotLikeStart() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("oper", false, MatchMode.START);
    assertFalse(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }

  @Test
  void testINotLikeEnd() {
    NotLikeOperator<String> operator = new NotLikeOperator<>("ator", false, MatchMode.END);
    assertFalse(operator.test("operator"));
    assertFalse(operator.test("OPERATOR"));
    assertTrue(operator.test("abc"));
  }
}
