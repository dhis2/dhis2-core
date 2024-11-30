/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;

import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.SqlBuilder;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class EventAnalyticsColumn {

  // Common columns that work across all databases

  private static final AnalyticsTableColumn EVENT =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.EVENT_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("ev.uid")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("en.uid")
          .build();
  public static final AnalyticsTableColumn TRACKED_ENTITY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .selectExpression("te.uid")
          .build();
  private static final AnalyticsTableColumn PS =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.PS_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("ps.uid")
          .build();
  private static final AnalyticsTableColumn AO =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.AO_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("acs.categoryoptioncombouid")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_DATE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("en.enrollmentdate")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_OCCURRED_DATE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("en.occurreddate")
          .build();
  private static final AnalyticsTableColumn OCCURRED_DATE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("ev.occurreddate")
          .build();
  private static final AnalyticsTableColumn SCHEDULED_DATE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("ev.scheduleddate")
          .build();
  private static final AnalyticsTableColumn COMPLETED_DATE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("ev.completeddate")
          .build();

  /*
   * DHIS2-14981: Use the client-side timestamp if available, otherwise
   * the server-side timestamp. Applies to both created and lastupdated.
   */
  private static final AnalyticsTableColumn CREATED =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.CREATED_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression(firstIfNotNullOrElse("ev.createdatclient", "ev.created"))
          .build();
  private static final AnalyticsTableColumn LAST_UPDATED =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression(firstIfNotNullOrElse("ev.lastupdatedatclient", "ev.lastupdated"))
          .build();
  private static final AnalyticsTableColumn STOREDBY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.STORED_BY_COLUMN_NAME)
          .dataType(VARCHAR_255)
          .selectExpression("ev.storedby")
          .build();

  private static final AnalyticsTableColumn EVENT_STATUS =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME)
          .dataType(VARCHAR_50)
          .selectExpression("ev.status")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_STATUS =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME)
          .dataType(VARCHAR_50)
          .selectExpression("en.status")
          .build();
  private static final AnalyticsTableColumn EVENT_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.EVENT_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("ev.geometry")
          .indexType(IndexType.GIST)
          .build();
  // TODO latitude and longitude deprecated in 2.30, remove in 2.33
  private static final AnalyticsTableColumn LONGITUDE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.LONGITUDE_COLUMN_NAME)
          .dataType(DOUBLE)
          .selectExpression(
              "CASE WHEN 'POINT' = GeometryType(ev.geometry) THEN ST_X(ev.geometry) ELSE null END")
          .build();
  private static final AnalyticsTableColumn LATITUDE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.LATITUDE_COLUMN_NAME)
          .dataType(DOUBLE)
          .selectExpression(
              "CASE WHEN 'POINT' = GeometryType(ev.geometry) THEN ST_Y(ev.geometry) ELSE null END")
          .build();
  private static final AnalyticsTableColumn OU =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OU_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("ou.uid")
          .build();
  private static final AnalyticsTableColumn OU_NAME =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OU_NAME_COLUMN_NAME)
          .dataType(TEXT)
          .nullable(NOT_NULL)
          .selectExpression("ou.name")
          .build();
  private static final AnalyticsTableColumn OU_CODE =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OU_CODE_COLUMN_NAME)
          .dataType(TEXT)
          .selectExpression("ou.code")
          .build();
  private static final AnalyticsTableColumn OU_LEVEL =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OU_LEVEL_COLUMN_NAME)
          .dataType(INTEGER)
          .selectExpression("ous.level")
          .build();
  private static final AnalyticsTableColumn OU_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.OU_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("ou.geometry")
          .indexType(IndexType.GIST)
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("en.geometry")
          .indexType(IndexType.GIST)
          .build();
  private static final AnalyticsTableColumn REGISTRATION_OU =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.REGISTRATION_OU_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("coalesce(registrationou.uid,ou.uid)")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_OU =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("coalesce(enrollmentou.uid,ou.uid)")
          .build();
  public static final AnalyticsTableColumn TRACKED_ENTITY_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.TRACKED_ENTITY_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("te.geometry")
          .build();

  private static final List<AnalyticsTableColumn> COMMON_COLUMNS =
      List.of(
          EVENT,
          ENROLLMENT,
          PS,
          AO,
          ENROLLMENT_DATE,
          ENROLLMENT_OCCURRED_DATE,
          OCCURRED_DATE,
          SCHEDULED_DATE,
          COMPLETED_DATE,
          CREATED,
          LAST_UPDATED,
          STOREDBY,
          EVENT_STATUS,
          ENROLLMENT_STATUS,
          OU,
          OU_NAME,
          OU_CODE,
          OU_LEVEL,
          REGISTRATION_OU,
          ENROLLMENT_OU);

  // Geometry-specific columns
  private static final List<AnalyticsTableColumn> GEOMETRY_COLUMNS =
      List.of(EVENT_GEOMETRY, OU_GEOMETRY, ENROLLMENT_GEOMETRY, LONGITUDE, LATITUDE);

  // JSON-specific columns (might vary by database)
  private static List<AnalyticsTableColumn> createJsonColumns(SqlBuilder sqlBuilder) {
    return List.of(
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_BY_USERNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.createdbyuserinfo", "username")
                    + " as createdbyusername")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_BY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.createdbyuserinfo", "firstName") + " as createdbyname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_BY_LASTNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.createdbyuserinfo", "surname") + " as createdbylastname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_BY_DISPLAYNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                getDisplayName("createdbyuserinfo", "ev", "createdbydisplayname", sqlBuilder))
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_BY_USERNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.lastupdatedbyuserinfo", "username")
                    + " as lastupdatedbyusername")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_BY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.lastupdatedbyuserinfo", "firstName")
                    + " as lastupdatedbyname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_BY_LASTNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("ev.lastupdatedbyuserinfo", "surname")
                    + " as lastupdatedbylastname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_BY_DISPLAYNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                getDisplayName(
                    "lastupdatedbyuserinfo", "ev", "lastupdatedbydisplayname", sqlBuilder))
            .skipIndex(Skip.SKIP)
            .build());
  }

  /** Returns the appropriate set of columns based on the SqlBuilder type */
  public static List<AnalyticsTableColumn> getColumns(SqlBuilder sqlBuilder) {
    List<AnalyticsTableColumn> columns = new ArrayList<>(COMMON_COLUMNS);

    // Add database-specific columns based on SqlBuilder capabilities
    if (sqlBuilder.supportsGeospatialData()) {
      columns.addAll(GEOMETRY_COLUMNS);
    }

    columns.addAll(createJsonColumns(sqlBuilder));

    return columns;
  }

  /**
   * Returns a SQL expression that returns the first argument if it is not null, otherwise the
   * second argument.
   *
   * @param first the first argument
   * @param second the second argument
   * @return a SQL expression
   */
  private static String firstIfNotNullOrElse(String first, String second) {
    return "case when " + first + " is not null then " + first + " else " + second + " end";
  }
}
