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
package org.hisp.dhis.parser.expression;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AMPERSAND_2;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AND;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.C_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.DIV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.EQ;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.EXCLAMATION_POINT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.FIRST_NON_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.GEQ;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.GREATEST;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.GT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.IF;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.IS_NOT_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.IS_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.LEAST;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.LEQ;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.LOG;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.LOG10;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.LT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MINUS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MOD;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MUL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.NE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.NOT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.OR;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PAREN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PLUS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.POWER;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.VERTICAL_BAR_2;
import static org.hisp.dhis.util.DateUtils.parseDate;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.dataitem.ItemConstant;
import org.hisp.dhis.parser.expression.function.FunctionFirstNonNull;
import org.hisp.dhis.parser.expression.function.FunctionGreatest;
import org.hisp.dhis.parser.expression.function.FunctionIf;
import org.hisp.dhis.parser.expression.function.FunctionIsNotNull;
import org.hisp.dhis.parser.expression.function.FunctionIsNull;
import org.hisp.dhis.parser.expression.function.FunctionLeast;
import org.hisp.dhis.parser.expression.function.FunctionLog;
import org.hisp.dhis.parser.expression.function.FunctionLog10;
import org.hisp.dhis.parser.expression.operator.OperatorCompareEqual;
import org.hisp.dhis.parser.expression.operator.OperatorCompareGreaterThan;
import org.hisp.dhis.parser.expression.operator.OperatorCompareGreaterThanOrEqual;
import org.hisp.dhis.parser.expression.operator.OperatorCompareLessThan;
import org.hisp.dhis.parser.expression.operator.OperatorCompareLessThanOrEqual;
import org.hisp.dhis.parser.expression.operator.OperatorCompareNotEqual;
import org.hisp.dhis.parser.expression.operator.OperatorGroupingParentheses;
import org.hisp.dhis.parser.expression.operator.OperatorLogicalAnd;
import org.hisp.dhis.parser.expression.operator.OperatorLogicalNot;
import org.hisp.dhis.parser.expression.operator.OperatorLogicalOr;
import org.hisp.dhis.parser.expression.operator.OperatorMathDivide;
import org.hisp.dhis.parser.expression.operator.OperatorMathMinus;
import org.hisp.dhis.parser.expression.operator.OperatorMathModulus;
import org.hisp.dhis.parser.expression.operator.OperatorMathMultiply;
import org.hisp.dhis.parser.expression.operator.OperatorMathPlus;
import org.hisp.dhis.parser.expression.operator.OperatorMathPower;

/**
 * Utilities for ANTLR parsing
 *
 * @author Jim Grace
 */
public class ParserUtils {
  private ParserUtils() {}

  public static final double DOUBLE_VALUE_IF_NULL = 0.0;

  /**
   * These are common expression items that are used in ALL types of DHIS2 expressions. Items that
   * are only used in some types of expressions are defined elsewhere.
   */
  public static final ImmutableMap<Integer, ExpressionItem> COMMON_EXPRESSION_ITEMS =
      ImmutableMap.<Integer, ExpressionItem>builder()

          // Non-comparison operators

          .put(PAREN, new OperatorGroupingParentheses())
          .put(PLUS, new OperatorMathPlus())
          .put(MINUS, new OperatorMathMinus())
          .put(POWER, new OperatorMathPower())
          .put(MUL, new OperatorMathMultiply())
          .put(DIV, new OperatorMathDivide())
          .put(MOD, new OperatorMathModulus())
          .put(NOT, new OperatorLogicalNot())
          .put(EXCLAMATION_POINT, new OperatorLogicalNot())
          .put(AND, new OperatorLogicalAnd())
          .put(AMPERSAND_2, new OperatorLogicalAnd())
          .put(OR, new OperatorLogicalOr())
          .put(VERTICAL_BAR_2, new OperatorLogicalOr())

          // Comparison operators

          .put(EQ, new OperatorCompareEqual())
          .put(NE, new OperatorCompareNotEqual())
          .put(GT, new OperatorCompareGreaterThan())
          .put(LT, new OperatorCompareLessThan())
          .put(GEQ, new OperatorCompareGreaterThanOrEqual())
          .put(LEQ, new OperatorCompareLessThanOrEqual())

          // Functions

          .put(FIRST_NON_NULL, new FunctionFirstNonNull())
          .put(GREATEST, new FunctionGreatest())
          .put(IF, new FunctionIf())
          .put(IS_NOT_NULL, new FunctionIsNotNull())
          .put(IS_NULL, new FunctionIsNull())
          .put(LEAST, new FunctionLeast())
          .put(LOG, new FunctionLog())
          .put(LOG10, new FunctionLog10())

          // Data items

          .put(C_BRACE, new ItemConstant())
          .build();

  /** Default value for data type double. */
  public static final double DEFAULT_DOUBLE_VALUE = 1d;

  /** Default value for data type date. */
  public static final String DEFAULT_DATE_VALUE = "2017-07-08";

  /** Default value for data type boolean. */
  public static final boolean DEFAULT_BOOLEAN_VALUE = false;

  /**
   * Parse a date. The input format is guaranteed by the expression parser to be yyyy-m-d where m
   * and d may be either 1 or 2 digits each.
   *
   * @param dateString the date string
   * @return the parsed date
   */
  public static Date parseExpressionDate(String dateString) {
    String[] dateParts = dateString.split("-");

    String fixedDateString =
        dateParts[0]
            + "-"
            + (dateParts[1].length() == 1 ? "0" : "")
            + dateParts[1]
            + "-"
            + (dateParts[2].length() == 1 ? "0" : "")
            + dateParts[2];

    Date date;

    try {
      date = parseDate(fixedDateString);
    } catch (Exception e) {
      throw new ParserExceptionWithoutContext("Invalid date: " + dateString + " " + e.getMessage());
    }

    if (date == null) {
      throw new ParserExceptionWithoutContext("Invalid date: " + dateString);
    }

    return date;
  }

  /**
   * Assume that an item of the form #{...} has a syntax that could be used in a program indicator
   * expression for #{programStageUid.dataElementUid}
   *
   * @param ctx the item context
   */
  public static void assumeStageElementSyntax(ExprContext ctx) {
    if (ctx.uid0 == null || ctx.uid1 == null || ctx.uid2 != null || ctx.wild2 != null) {
      throw new ParserExceptionWithoutContext(
          "Invalid Program Stage / DataElement syntax: " + ctx.getText());
    }
  }

  /**
   * Assume that an item of the form A{...} has a syntax that could be used in an expression for
   * A{progamUid.attributeUid}
   *
   * @param ctx the item context
   */
  public static void assumeExpressionProgramAttribute(ExprContext ctx) {
    if (ctx.uid0 == null || ctx.uid1 == null) {
      throw new ParserExceptionWithoutContext(
          "Program attribute must have two UIDs: " + ctx.getText());
    }
  }

  /**
   * Assume that an item of the form A{...} has a syntax that could be used be used in an program
   * expression for A{attributeUid}
   *
   * @param ctx the item context
   */
  public static void assumeProgramExpressionProgramAttribute(ExprContext ctx) {
    if (ctx.uid0 == null || ctx.uid1 != null) {
      throw new ParserExceptionWithoutContext(
          "Program attribute must have one UID: " + ctx.getText());
    }
  }
}
