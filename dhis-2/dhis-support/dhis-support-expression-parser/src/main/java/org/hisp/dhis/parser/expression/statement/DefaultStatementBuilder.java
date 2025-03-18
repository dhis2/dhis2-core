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
package org.hisp.dhis.parser.expression.statement;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_EVENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_INCIDENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsConstants;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.util.Assert;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DefaultStatementBuilder implements StatementBuilder {
  protected static final String QUOTE = "\"";

  private final SqlBuilder sqlBuilder;

  @Override
  public String getProgramIndicatorDataValueSelectSql(
      String programStageUid,
      String dataElementUid,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    String columnName = this.columnQuote(dataElementUid);
    if (programIndicator.getAnalyticsType().equals(AnalyticsType.ENROLLMENT)) {
      return getProgramIndicatorEventColumnSql(
          programStageUid, columnName, reportingStartDate, reportingEndDate, programIndicator);
    } else {
      return getProgramIndicatorDataElementInEventSelectSql(columnName, programStageUid);
    }
  }

  @Override
  public String getProgramIndicatorEventColumnSql(
      String programStageUid,
      String columnName,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    if (programIndicator.getAnalyticsType().equals(AnalyticsType.ENROLLMENT)) {
      return getProgramIndicatorEventInEnrollmentSelectSql(
          columnName, programStageUid, reportingStartDate, reportingEndDate, programIndicator);
    } else {
      return columnName;
    }
  }

  @Override
  public String getProgramIndicatorEventColumnSql(
      String programStageUid,
      String stageOffset,
      String columnName,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    if (programIndicator.getAnalyticsType().equals(AnalyticsType.ENROLLMENT)) {
      return getProgramIndicatorEventInEnrollmentSelectSql(
          columnName,
          stageOffset,
          programStageUid,
          reportingStartDate,
          reportingEndDate,
          programIndicator);
    } else {
      return getProgramIndicatorDataElementInEventSelectSql(columnName, programStageUid);
    }
  }

  private String getProgramIndicatorDataElementInEventSelectSql(
      String columnName, String programStageUid) {
    String col = sqlBuilder.quote("ps");
    return format("case when ax.%s = '%s' then %s else null end", col, programStageUid, columnName);
  }

  private String getProgramIndicatorEventInEnrollmentSelectSql(
      String columnName,
      String programStageUid,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    return getProgramIndicatorEventInEnrollmentSelectSql(
        columnName, "0", programStageUid, reportingStartDate, reportingEndDate, programIndicator);
  }

  private String getProgramIndicatorEventInEnrollmentSelectSql(
      String columnName,
      String stageOffset,
      String programStageUid,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    String programStageCondition = "";
    if (programStageUid != null && programStageUid.length() == 11) {
      programStageCondition = "and ps = '" + programStageUid + "' ";
    }

    String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();
    return "(select "
        + columnName
        + " from "
        + eventTableName
        + " where "
        + eventTableName
        + ".enrollment = "
        + AnalyticsConstants.ANALYTICS_TBL_ALIAS
        + ".enrollment and "
        + columnName
        + " is not null "
        + (programIndicator.getEndEventBoundary() != null
            ? ("and "
                + getBoundaryCondition(
                    programIndicator.getEndEventBoundary(),
                    programIndicator,
                    null,
                    reportingStartDate,
                    reportingEndDate)
                + " ")
            : "")
        + (programIndicator.getStartEventBoundary() != null
            ? ("and "
                + getBoundaryCondition(
                    programIndicator.getStartEventBoundary(),
                    programIndicator,
                    null,
                    reportingStartDate,
                    reportingEndDate)
                + " ")
            : "")
        + programStageCondition
        + "order by "
        + keepOrderCompatibilityColumn(
            getBoundaryColumn(
                programIndicator.getEndEventBoundary(),
                programIndicator,
                null,
                reportingStartDate,
                reportingEndDate))
        + SPACE
        + createOrderTypeAndOffset(stageOffset)
        + " limit 1 )";
  }

  private String createOrderTypeAndOffset(String stageOffset) {
    int offset = Integer.parseInt(stageOffset);

    if (offset == 0) {
      return "desc";
    }
    if (offset < 0) {
      return "desc offset " + (-1 * offset);
    } else {
      return "asc offset " + (offset - 1);
    }
  }

  private String getBoundaryElementColumnSql(
      AnalyticsPeriodBoundary boundary,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    String columnSql = null;
    if (boundary.isDataElementCohortBoundary()) {
      Matcher matcher =
          AnalyticsPeriodBoundary.COHORT_HAVING_DATA_ELEMENT_PATTERN.matcher(
              boundary.getBoundaryTarget());
      Assert.isTrue(
          matcher.find(),
          "Can not parse data element pattern for analyticsPeriodBoundary "
              + boundary.getUid()
              + " - unknown boundaryTarget: "
              + boundary.getBoundaryTarget());
      String programStage = matcher.group(AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP);
      Assert.isTrue(
          programStage != null,
          "Can not find programStage for analyticsPeriodBoundary "
              + boundary.getUid()
              + " - boundaryTarget: "
              + boundary.getBoundaryTarget());
      String dataElement = matcher.group(AnalyticsPeriodBoundary.DATA_ELEMENT_REGEX_GROUP);
      Assert.isTrue(
          dataElement != null,
          "Can not find data element for analyticsPeriodBoundary "
              + boundary.getUid()
              + " - boundaryTarget: "
              + boundary.getBoundaryTarget());
      columnSql =
          getCastToDate(
              getProgramIndicatorDataValueSelectSql(
                  programStage,
                  dataElement,
                  reportingStartDate,
                  reportingEndDate,
                  programIndicator));
    } else if (boundary.isAttributeCohortBoundary()) {
      Matcher matcher =
          AnalyticsPeriodBoundary.COHORT_HAVING_ATTRIBUTE_PATTERN.matcher(
              boundary.getBoundaryTarget());
      Assert.isTrue(
          matcher.find(),
          "Can not parse attribute pattern for analyticsPeriodBoundary "
              + boundary.getUid()
              + " - unknown boundaryTarget: "
              + boundary.getBoundaryTarget());
      String attribute = matcher.group(AnalyticsPeriodBoundary.ATTRIBUTE_REGEX_GROUP);
      Assert.isTrue(
          attribute != null,
          "Can not find attribute for analyticsPeriodBoundary "
              + boundary.getUid()
              + " - boundaryTarget: "
              + boundary.getBoundaryTarget());
      columnSql = getCastToDate(this.columnQuote(attribute));
    }
    Assert.isTrue(
        columnSql != null,
        "Can not determine boundary type for analyticsPeriodBoundary "
            + boundary.getUid()
            + " - boundaryTarget: "
            + boundary.getBoundaryTarget());
    return columnSql;
  }

  @Override
  public String getBoundaryCondition(
      AnalyticsPeriodBoundary boundary,
      ProgramIndicator programIndicator,
      String timeField,
      Date reportingStartDate,
      Date reportingEndDate) {
    final String column =
        getBoundaryColumn(
            boundary, programIndicator, timeField, reportingStartDate, reportingEndDate);

    final SimpleDateFormat format = new SimpleDateFormat();
    format.applyPattern(Period.DEFAULT_DATE_FORMAT);

    return column
        + " "
        + (boundary.getAnalyticsPeriodBoundaryType().isEndBoundary() ? "<" : ">=")
        + " cast( '"
        + format.format(boundary.getBoundaryDate(reportingStartDate, reportingEndDate))
        + "' as date )";
  }

  protected String columnQuote(String column) {
    column = column.replace(QUOTE, (QUOTE + QUOTE));
    return sqlBuilder.quote(column);
  }

  /**
   * Based on the given arguments, this method returns the column associated to the boundary object.
   * This column should be used as part of the boundary SQL statement.
   *
   * @return the respective boundary column
   */
  private String getBoundaryColumn(
      final AnalyticsPeriodBoundary boundary,
      final ProgramIndicator programIndicator,
      final String timeField,
      final Date reportingStartDate,
      final Date reportingEndDate) {
    if (boundary == null) {
      return DB_EVENT_DATE;
    }

    return boundary.isEventDateBoundary()
        ? Optional.ofNullable(timeField).orElse(DB_EVENT_DATE)
        : boundary.isEnrollmentDateBoundary()
            ? DB_ENROLLMENT_DATE
            : boundary.isIncidentDateBoundary()
                ? DB_INCIDENT_DATE
                : boundary.isScheduledDateBoundary()
                    ? DB_SCHEDULED_DATE
                    : this.getBoundaryElementColumnSql(
                        boundary, reportingStartDate, reportingEndDate, programIndicator);
  }

  /**
   * This method is needed to keep the logic/code backward compatible. Previously, we didn't
   * consider statuses, as we always based on the "occurreddate" only (it means ACTIVE and COMPLETED
   * status).
   *
   * <p>Now, we also need to support SCHEDULE status for events. For this reason this method
   * compares the status. If the column is "scheduleddate", it means we only want SCHEDULE status,
   * so we return "scheduleddate". In all other cases we assume any other status different from
   * SCHEDULE (which makes it backward compatible). In this case the logic will remain based on
   * "occurreddate".
   *
   * @return the backwards compatible column
   */
  private String keepOrderCompatibilityColumn(final String column) {
    if (!DB_SCHEDULED_DATE.equals(column)) {
      return DB_EVENT_DATE;
    }

    return DB_SCHEDULED_DATE;
  }

  private String getCastToDate(String column) {
    return "cast(" + column + " as date)";
  }
}
