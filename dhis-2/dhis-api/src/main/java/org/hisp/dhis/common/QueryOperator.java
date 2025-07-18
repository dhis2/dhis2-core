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
package org.hisp.dhis.common;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.replaceOnce;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Lars Helge Overland
 */
@Getter
@RequiredArgsConstructor
public enum QueryOperator {
  EQ("=", true, true),
  GT(">", false, true),
  GE(">=", false, true),
  LT("<", false, true),
  LE("<=", false, true),
  LIKE("like", false, false),
  IN("in", true, true),
  SW("sw", false, false),
  EW("ew", false, false),
  // casting the is (not) null operand to a different data type adds no benefit so we won't do it
  // even though PostgreSQL does have implementations for different data types
  NULL("is null", false, false),
  NNULL("is not null", false, false),
  // Analytics specifics
  IEQ("==", true, false),
  NE("!=", true, true),
  NEQ("!=", true, true),
  NIEQ("!==", true, false),
  NLIKE("not like", false, false),
  ILIKE("ilike", false, false),
  NILIKE("not ilike", false, false);

  private static final Set<QueryOperator> EQ_OPERATORS = EnumSet.of(EQ, NE, NEQ, IEQ, NIEQ);

  private static final Set<QueryOperator> NE_OPERATORS = EnumSet.of(NE, NEQ, NIEQ);

  private static final Set<QueryOperator> LIKE_OPERATORS = EnumSet.of(LIKE, NLIKE, ILIKE, NILIKE);

  /**
   * All query operators that are implemented using the SQL {@code like} operator (see {@link
   * #value}).
   *
   * <p>This is a union of {@link #LIKE_OPERATORS} and SW, EW. So keep it in sync!
   */
  private static final Set<QueryOperator> LIKE_BASED_OPERATORS =
      EnumSet.of(LIKE, NLIKE, ILIKE, NILIKE, SW, EW);

  private static final Set<QueryOperator> MULTI_TEXT_OPERATORS =
      EnumSet.of(LIKE, NLIKE, ILIKE, NILIKE, SW, EW, NE, NEQ, IN, EQ, IEQ, NULL, NNULL);

  private static final Set<QueryOperator> COMPARISON_OPERATORS = EnumSet.of(GT, GE, LT, LE);

  private static final Set<QueryOperator> UNARY_OPERATORS = EnumSet.of(NULL, NNULL);

  private final String value;

  private final boolean nullAllowed;

  /**
   * Indicates if operands should be cast to a different PostgreSQL data type other than the one
   * used for storage (string/text so far). This allows users to filter/compare using the semantic
   * of a value type like numeric instead of only text.
   *
   * <p>This information might also fit with the {@link ValueType#JAVA_TO_SQL_TYPES} to show what
   * data type has an implementation for each operator.
   */
  private final boolean castOperand;

  public static QueryOperator fromString(String string) {
    if (isBlank(string)) {
      return null;
    }

    if (string.trim().startsWith("!")) {
      return valueOf("N" + replaceOnce(string, "!", EMPTY).toUpperCase());
    }

    // To still support NE operator until it gets removed
    if (string.trim().equals("NE")) {
      return NEQ;
    }

    return valueOf(string.toUpperCase());
  }

  public boolean isEqualTo() {
    return EQ_OPERATORS.contains(this);
  }

  public boolean isNotEqualTo() {
    return NE_OPERATORS.contains(this);
  }

  public boolean isLike() {
    return LIKE_OPERATORS.contains(this);
  }

  /** Returns true if this query operator is implemented using the SQL {@code like} operator. */
  public boolean isLikeBased() {
    return LIKE_BASED_OPERATORS.contains(this);
  }

  public boolean isIn() {
    return IN == this;
  }

  public boolean isMultiTextSupported() {
    return MULTI_TEXT_OPERATORS.contains(this);
  }

  public boolean isComparison() {
    return COMPARISON_OPERATORS.contains(this);
  }

  public boolean isBinary() {
    return !isUnary();
  }

  public boolean isUnary() {
    return UNARY_OPERATORS.contains(this);
  }

  private static final Set<QueryOperator> TRACKER_OPERATORS =
      EnumSet.of(EQ, GT, GE, LT, LE, LIKE, IN, SW, EW, NULL, NNULL);

  public static Set<QueryOperator> getTrackerOperators() {
    return Collections.unmodifiableSet(TRACKER_OPERATORS);
  }
}
