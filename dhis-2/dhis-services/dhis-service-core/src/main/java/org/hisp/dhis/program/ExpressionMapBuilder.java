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
package org.hisp.dhis.program;

import static org.hisp.dhis.parser.expression.ParserUtils.COMMON_EXPRESSION_ITEMS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AVG;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.A_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_CONDITION;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT_IF_CONDITION;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT_IF_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_DAYS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_HAS_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MAX_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MINUTES_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MIN_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MONTHS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_OIZP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_RELATIONSHIP_COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_WEEKS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_YEARS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_ZING;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_ZPVC;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.HASH_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MAX;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MIN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PS_EVENTDATE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STAGE_OFFSET;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.VARIANCE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.V_BRACE;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.function.RepeatableProgramStageOffset;
import org.hisp.dhis.parser.expression.function.VectorAvg;
import org.hisp.dhis.parser.expression.function.VectorCount;
import org.hisp.dhis.parser.expression.function.VectorMax;
import org.hisp.dhis.parser.expression.function.VectorMin;
import org.hisp.dhis.parser.expression.function.VectorStddevSamp;
import org.hisp.dhis.parser.expression.function.VectorSum;
import org.hisp.dhis.parser.expression.function.VectorVariance;
import org.hisp.dhis.program.dataitem.ProgramItemAttribute;
import org.hisp.dhis.program.dataitem.ProgramItemPsEventdate;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;
import org.hisp.dhis.program.function.D2Condition;
import org.hisp.dhis.program.function.D2Count;
import org.hisp.dhis.program.function.D2CountIfCondition;
import org.hisp.dhis.program.function.D2CountIfValue;
import org.hisp.dhis.program.function.D2DaysBetween;
import org.hisp.dhis.program.function.D2HasValue;
import org.hisp.dhis.program.function.D2MaxValue;
import org.hisp.dhis.program.function.D2MinValue;
import org.hisp.dhis.program.function.D2MinutesBetween;
import org.hisp.dhis.program.function.D2MonthsBetween;
import org.hisp.dhis.program.function.D2Oizp;
import org.hisp.dhis.program.function.D2RelationshipCount;
import org.hisp.dhis.program.function.D2WeeksBetween;
import org.hisp.dhis.program.function.D2YearsBetween;
import org.hisp.dhis.program.function.D2Zing;
import org.hisp.dhis.program.function.D2Zpvc;
import org.hisp.dhis.program.variable.ProgramVariableItem;

/**
 * This component encapsulates the creation of the immutable expressions map. The map contains all
 * the expression items that are used in the program expressions.
 */
@Getter
public class ExpressionMapBuilder {

  private final ImmutableMap<Integer, ExpressionItem> expressionItemMap;

  public ExpressionMapBuilder() {
    expressionItemMap =
        ImmutableMap.<Integer, ExpressionItem>builder()

            // Common functions

            .putAll(COMMON_EXPRESSION_ITEMS)

            // Program functions

            .put(D2_CONDITION, new D2Condition())
            .put(D2_COUNT, new D2Count())
            .put(D2_COUNT_IF_CONDITION, new D2CountIfCondition())
            .put(D2_COUNT_IF_VALUE, new D2CountIfValue())
            .put(D2_DAYS_BETWEEN, new D2DaysBetween())
            .put(D2_HAS_VALUE, new D2HasValue())
            .put(D2_MAX_VALUE, new D2MaxValue())
            .put(D2_MINUTES_BETWEEN, new D2MinutesBetween())
            .put(D2_MIN_VALUE, new D2MinValue())
            .put(D2_MONTHS_BETWEEN, new D2MonthsBetween())
            .put(D2_OIZP, new D2Oizp())
            .put(D2_RELATIONSHIP_COUNT, new D2RelationshipCount())
            .put(D2_WEEKS_BETWEEN, new D2WeeksBetween())
            .put(D2_YEARS_BETWEEN, new D2YearsBetween())
            .put(D2_ZING, new D2Zing())
            .put(D2_ZPVC, new D2Zpvc())

            // Program functions for custom aggregation

            .put(AVG, new VectorAvg())
            .put(COUNT, new VectorCount())
            .put(MAX, new VectorMax())
            .put(MIN, new VectorMin())
            .put(STDDEV, new VectorStddevSamp())
            .put(SUM, new VectorSum())
            .put(VARIANCE, new VectorVariance())

            // Data items

            .put(HASH_BRACE, new ProgramItemStageElement())
            .put(A_BRACE, new ProgramItemAttribute())
            .put(PS_EVENTDATE, new ProgramItemPsEventdate())

            // Program variables

            .put(V_BRACE, new ProgramVariableItem())

            // . functions
            .put(STAGE_OFFSET, new RepeatableProgramStageOffset())
            .build();
  }
}
