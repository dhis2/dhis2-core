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
package org.hisp.dhis.expression;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;

/**
 * Parameters to evaluate an expression in {@see ExpressionService}
 *
 * @author Jim Grace
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class ExpressionParams {
  public static final ExpressionParams DEFAULT_EXPRESSION_PARAMS;

  /**
   * Dummy data for sample periods, so in the absence of real sampled data the parser will still
   * traverse the contents of aggregation functions once for the purposes of such things as syntax
   * checking and getting an expression description. The actual date doesn't matter; a date was
   * chosen that is likely to not be confused with real data.
   */
  private static final List<Period> SAMPLE_PERIODS;

  static {
    Date genTheFirst99 =
        Date.from(
            LocalDate.of(1999, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    Period period = new Period();
    period.setPeriodType(new DailyPeriodType());
    period.setStartDate(genTheFirst99);
    period.setEndDate(genTheFirst99);
    SAMPLE_PERIODS = Collections.singletonList(period);
    DEFAULT_EXPRESSION_PARAMS = builder().build();
  }

  /** The expression to parse */
  @ToString.Include @EqualsAndHashCode.Include private final String expression;

  /** The type of expression to parse (Indicator, Predictor, etc.) */
  @ToString.Include @EqualsAndHashCode.Include private final ParseType parseType;

  /**
   * The expected return data type (often but not always determined by the type of expression to
   * parse).
   */
  @ToString.Include @EqualsAndHashCode.Include private final DataType dataType;

  /** A map from a parsed {@see DimensionalItemId} to its equivalent {@see DimensionalItemObject} */
  @Builder.Default
  private final Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();

  /** A map from a {@see DimensionalItemObject} to its value to use in evaluating the expression */
  @Builder.Default private final Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

  /** Map of organisation unit counts to use in evaluating the expression */
  @ToString.Include @EqualsAndHashCode.Include @Builder.Default
  private final Map<String, Integer> orgUnitCountMap = new HashMap<>();

  /** Map of organisation unit groups to use in evaluating the expression */
  @Builder.Default
  private final Map<String, OrganisationUnitGroup> orgUnitGroupMap = new HashMap<>();

  /** Map of data sets to use in evaluating the expression */
  @Builder.Default private Map<String, DataSet> dataSetMap = new HashMap<>();

  /** Map of programs to use in evaluating the expression */
  @Builder.Default private Map<String, Program> programMap = new HashMap<>();

  /** The periods (if any) for which the expression is evaluated. */
  @ToString.Include @EqualsAndHashCode.Include @Builder.Default
  private final List<Period> periods = emptyList();

  /**
   * The number of calendar days to be used in evaluating the expression. Defaults to zero for the
   * purpose of expression syntax checking.
   */
  @ToString.Include @EqualsAndHashCode.Include @Builder.Default private final Integer days = 0;

  /** The missing value strategy (what to do if data values are missing) */
  @ToString.Include @EqualsAndHashCode.Include @Builder.Default
  private final MissingValueStrategy missingValueStrategy = NEVER_SKIP;

  /** The current organisation unit the expression is being evaluated for */
  @ToString.Include @EqualsAndHashCode.Include private final OrganisationUnit orgUnit;

  /**
   * For predictors, a list of periods in which we will look for sampled data. Defaults to a single
   * dummy period when we don't have an actual list of periods, so we can do syntax checking.
   */
  @ToString.Include @EqualsAndHashCode.Include @Builder.Default
  private final List<Period> samplePeriods = SAMPLE_PERIODS;

  /**
   * For predictors, a value map from item to value, for each of the periods in which data is
   * present
   */
  @Builder.Default
  private final MapMap<Period, DimensionalItemObject, Object> periodValueMap = new MapMap<>();

  /**
   * Initial {@see ExpressionInfo} to be added onto, if any. This allows successive calls to {@see
   * ExpressionService#getExpressionInfo} to accumulate the information from multiple expressions
   * into the same {@see ExpressionInfo} instance.
   */
  @Builder.Default private final ExpressionInfo expressionInfo = new ExpressionInfo();

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public DataType getDataType() {
    return (dataType != null) ? dataType : parseType.getDataType();
  }
}
