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
package org.hisp.dhis.jdbc;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * @author Lars Helge Overland
 */
public interface StatementBuilder {

  /**
   * Returns the position of substring in string, or 0 if not there.
   *
   * @param substring string to search for
   * @param string string in which to search
   * @return position, or 0 if not found
   */
  String position(String substring, String string);

  /**
   * Returns a cast to timestamp statement for the given column.
   *
   * @param column the column name.
   * @return a cast to timestamp statement for the given column.
   */
  String getCastToDate(String column);

  /**
   * Returns a statement which calculates the number of days between the two given dates or columns
   * of type date.
   *
   * @param fromColumn the from date column.
   * @param toColumn the to date column.
   * @return statement which calculates the number of days between the given dates.
   */
  String getDaysBetweenDates(String fromColumn, String toColumn);

  String getDropPrimaryKey(String table);

  /**
   * Generates a derived table containing one column of literal strings.
   *
   * @param values (non-empty) String values for the derived table
   * @param table the desired table name alias
   * @param column the desired column name
   * @return the derived literal table
   */
  String literalStringTable(Collection<String> values, String table, String column);

  /**
   * Generates a derived table containing literals in two columns: integer and string.
   *
   * @param longValue (non-empty) Integer values for the derived table
   * @param strValues (same size) String values for the derived table
   * @param table the desired table name alias
   * @param longColumn the desired integer column name
   * @param strColumn the desired string column name
   * @return the derived literal table
   */
  String literalLongStringTable(
      List<Long> longValue,
      List<String> strValues,
      String table,
      String longColumn,
      String strColumn);

  /**
   * Generates a derived table containing literals in two columns: integer and integer.
   *
   * @param long1Values (non-empty) 1st integer column values for the table
   * @param long2Values (same size) 2nd integer column values for the table
   * @param table the desired table name alias
   * @param long1Column the desired 1st integer column name
   * @param long2Column the desired 2nd integer column name
   * @return the derived literal table
   */
  String literalLongLongTable(
      List<Long> long1Values,
      List<Long> long2Values,
      String table,
      String long1Column,
      String long2Column);

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
