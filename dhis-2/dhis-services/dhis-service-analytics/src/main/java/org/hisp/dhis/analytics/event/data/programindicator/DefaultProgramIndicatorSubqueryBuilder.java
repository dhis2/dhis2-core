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
package org.hisp.dhis.analytics.event.data.programindicator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;

import com.google.common.base.Strings;
import java.util.Date;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.relationship.RelationshipType;
import org.springframework.stereotype.Component;

@Component
public class DefaultProgramIndicatorSubqueryBuilder implements ProgramIndicatorSubqueryBuilder {
  private static final String ANALYTICS_TABLE_NAME = "analytics";

  private static final String SUBQUERY_TABLE_ALIAS = "subax";

  private final ProgramIndicatorService programIndicatorService;

  public DefaultProgramIndicatorSubqueryBuilder(ProgramIndicatorService programIndicatorService) {
    checkNotNull(programIndicatorService);

    this.programIndicatorService = programIndicatorService;
  }

  /** {@inheritDoc} */
  @Override
  public String getAggregateClauseForProgramIndicator(
      ProgramIndicator pi, AnalyticsType outerSqlEntity, Date earliestStartDate, Date latestDate) {
    return getAggregateClauseForPIandRelationshipType(
        pi, null, outerSqlEntity, earliestStartDate, latestDate);
  }

  /** {@inheritDoc} */
  @Override
  public String getAggregateClauseForProgramIndicator(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate) {
    return getAggregateClauseForPIandRelationshipType(
        programIndicator, relationshipType, outerSqlEntity, earliestStartDate, latestDate);
  }

  /**
   * Generate a subquery based on the result of a Program Indicator and an (optional) Relationship
   * Type
   *
   * @param programIndicator a {@see ProgramIndicator} object
   * @param relationshipType an optional {@see RelationshipType} object
   * @param outerSqlEntity a {@see AnalyticsType} object, representing the outer sql context
   * @param earliestStartDate reporting start date
   * @param latestDate reporting end date
   * @return a String containing a Program Indicator sub-query
   */
  private String getAggregateClauseForPIandRelationshipType(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate) {
    // Define aggregation function (avg, sum, ...) //
    String function =
        TextUtils.emptyIfEqual(
            programIndicator.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue());

    // Get sql construct from Program indicator expression //
    String aggregateSql =
        getPrgIndSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            earliestStartDate,
            latestDate);

    // closes the function parenthesis ( avg( ... ) )
    aggregateSql += ")";

    // Determine Table name from FROM clause
    aggregateSql += getFrom(programIndicator);

    // Determine JOIN
    String where = getWhere(outerSqlEntity, programIndicator, relationshipType);

    aggregateSql += where;

    // Get WHERE condition from Program indicator filter
    if (!Strings.isNullOrEmpty(programIndicator.getFilter())) {
      aggregateSql +=
          (where.isBlank() ? " WHERE " : " AND ")
              + "("
              + getPrgIndSql(
                  programIndicator.getFilter(),
                  BOOLEAN,
                  programIndicator,
                  earliestStartDate,
                  latestDate)
              + ")";
    }

    return "(SELECT " + function + " (" + aggregateSql + ")";
  }

  private String getFrom(ProgramIndicator pi) {
    return " FROM "
        + ANALYTICS_TABLE_NAME
        + "_"
        + pi.getAnalyticsType().getValue()
        + "_"
        + pi.getProgram().getUid().toLowerCase()
        + " as "
        + SUBQUERY_TABLE_ALIAS;
  }

  /**
   * Determine the join after the WHERE condition
   *
   * <p>Rules:
   *
   * <p>1) outer = event | inner = enrollment -> pi = ax.pi (enrollment is the enrollmennt linked to
   * the inline event) 2) outer = enrollment | inner = event -> pi = ax.pi 3) outer = event | inner
   * = event -> psi = ax.psi (inner operate on same event as outer) 4) outer = enrollemnt | inner =
   * enrollment -> pi = ax.pi (enrollment operates on the same enrollment as outer) 5) if
   * RelationshipType, call the RelationshipTypeJoinGenerator
   *
   * @param outerSqlEntity the outer sql type (enrollment or event)
   * @param pi a Program Indicator object
   * @param relationshipType a Relationship type (optional)
   * @return a sqk WHERE condition
   */
  private String getWhere(
      AnalyticsType outerSqlEntity, ProgramIndicator pi, RelationshipType relationshipType) {
    String condition = "";
    if (relationshipType != null) {
      condition =
          RelationshipTypeJoinGenerator.generate(
              SUBQUERY_TABLE_ALIAS, relationshipType, pi.getAnalyticsType());
    } else {
      if (isEnrollment(outerSqlEntity)) {
        condition = "pi = ax.pi";
      } else {
        if (isEvent(pi.getAnalyticsType())) {
          condition = "psi = ax.psi";
        }
      }
    }

    return isNotBlank(condition) ? " WHERE " + condition : condition;
  }

  private boolean isEnrollment(AnalyticsType outerSqlEntity) {
    return outerSqlEntity.equals(AnalyticsType.ENROLLMENT);
  }

  private boolean isEvent(AnalyticsType outerSqlEntity) {
    return outerSqlEntity.equals(AnalyticsType.EVENT);
  }

  private String getPrgIndSql(
      String expression,
      DataType dataType,
      ProgramIndicator pi,
      Date earliestStartDate,
      Date latestDate) {
    return this.programIndicatorService.getAnalyticsSql(
        expression, dataType, pi, earliestStartDate, latestDate, SUBQUERY_TABLE_ALIAS);
  }
}
