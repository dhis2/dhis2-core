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

import java.util.Date;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * @author Lars Helge Overland
 */
public interface StatementBuilder {

  /**
   * Get SQL where-condition for a single analyticsPeriodBoundary in a program indicator.
   *
   * @param boundary the boundary to get where-condition for
   * @param programIndicator the program indicator context
   * @param reportingStartDate the date of the start of the reporting period
   * @param reportingEndDate the date of the end of the reporting period
   * @return SQL to use in where clause.
   */
  default String getBoundaryCondition(
      AnalyticsPeriodBoundary boundary,
      ProgramIndicator programIndicator,
      Date reportingStartDate,
      Date reportingEndDate) {
    return getBoundaryCondition(
        boundary, programIndicator, null, reportingStartDate, reportingEndDate);
  }

  /**
   * Get SQL where-condition for a single analyticsPeriodBoundary in a program indicator.
   *
   * @param boundary the boundary to get where-condition for
   * @param programIndicator the program indicator context
   * @param reportingStartDate the date of the start of the reporting period
   * @param reportingEndDate the date of the end of the reporting period
   * @return SQL to use in where clause.
   */
  String getBoundaryCondition(
      AnalyticsPeriodBoundary boundary,
      ProgramIndicator programIndicator,
      String timeField,
      Date reportingStartDate,
      Date reportingEndDate);

  /**
   * Get a SQL for selecting a single data value in a program indicator expression, abiding to
   * boundaries. Internally adds quotes to the param dataElementUid and calls the {@link
   * StatementBuilder#getProgramIndicatorEventColumnSql(String, String, Date, Date,
   * ProgramIndicator)} function.
   *
   * @param programStageUid the program stage to get data for
   * @param dataElementUid the data element to get data for
   * @param reportingStartDate the reporting start date
   * @param reportingEndDate the reporting end date
   * @param programIndicator the program indicator context
   * @return
   */
  String getProgramIndicatorDataValueSelectSql(
      String programStageUid,
      String dataElementUid,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator);

  /**
   * Get a SQL for selecting a single column from events in a program indicators, abiding to
   * boundaries.
   *
   * @param programStageUid the program stage to get data for
   * @param columnName the column to get data for
   * @param reportingStartDate the reporting start date
   * @param reportingEndDate the reporting end date
   * @param programIndicator the program indicator context
   * @return
   */
  String getProgramIndicatorEventColumnSql(
      String programStageUid,
      String columnName,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator);

  /**
   * Get a SQL for selecting a single data value in a program indicator expression, abiding to
   * boundaries. Internally adds quotes to the param dataElementUid and calls the {@link
   * StatementBuilder#getProgramIndicatorEventColumnSql(String, String, Date, Date,
   * ProgramIndicator)} function.
   *
   * @param programStageUid the program stage to get data for
   * @param stageOffset the program stage offset to get data (repeatable stages)
   * @param columnName the column to get data for
   * @param reportingStartDate the reporting start date
   * @param reportingEndDate the reporting end date
   * @param programIndicator the program indicator context
   * @return
   */
  default String getProgramIndicatorEventColumnSql(
      String programStageUid,
      String stageOffset,
      String columnName,
      Date reportingStartDate,
      Date reportingEndDate,
      ProgramIndicator programIndicator) {
    return getProgramIndicatorDataValueSelectSql(
        programStageUid, columnName, reportingStartDate, reportingEndDate, programIndicator);
  }
}
