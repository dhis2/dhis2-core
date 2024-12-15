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
public final class EnrollmentAnalyticsColumn {

  public static final AnalyticsTableColumn TRACKED_ENTITY =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .selectExpression("te.uid")
          .build();
  public static final AnalyticsTableColumn TRACKED_ENTITY_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.TRACKED_ENTITY_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("te.geometry")
          .build();

  private static final AnalyticsTableColumn ENROLLMENT =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.ENROLLMENT_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("en.uid")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_DATE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("en.enrollmentdate")
          .build();
  private static final AnalyticsTableColumn OCCURRED_DATE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("en.occurreddate")
          .build();
  private static final AnalyticsTableColumn COMPLETED_DATE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("case en.status when 'COMPLETED' then en.completeddate end")
          .build();
  private static final AnalyticsTableColumn LAST_UPDATED =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME)
          .dataType(TIMESTAMP)
          .selectExpression("en.lastupdated")
          .build();
  private static final AnalyticsTableColumn STORED_BY =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.STORED_BY_COLUMN_NAME)
          .dataType(VARCHAR_255)
          .selectExpression("en.storedby")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_STATUS =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME)
          .dataType(VARCHAR_50)
          .selectExpression("en.status")
          .build();
  private static final AnalyticsTableColumn LONGITUDE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.LONGITUDE_COLUMN_NAME)
          .dataType(DOUBLE)
          .selectExpression(
              "CASE WHEN 'POINT' = GeometryType(en.geometry) THEN ST_X(en.geometry) ELSE null END")
          .build();
  private static final AnalyticsTableColumn LATITUDE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.LATITUDE_COLUMN_NAME)
          .dataType(DOUBLE)
          .selectExpression(
              "CASE WHEN 'POINT' = GeometryType(en.geometry) THEN ST_Y(en.geometry) ELSE null END")
          .build();
  private static final AnalyticsTableColumn OU =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.OU_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("ou.uid")
          .build();
  private static final AnalyticsTableColumn OU_NAME =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.OU_NAME_COLUMN_NAME)
          .dataType(TEXT)
          .nullable(NOT_NULL)
          .selectExpression("ou.name")
          .build();
  private static final AnalyticsTableColumn OU_CODE =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.OU_CODE_COLUMN_NAME)
          .dataType(TEXT)
          .selectExpression("ou.code")
          .build();
  private static final AnalyticsTableColumn OU_LEVEL =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.OU_LEVEL_COLUMN_NAME)
          .dataType(INTEGER)
          .selectExpression("ous.level")
          .build();
  private static final AnalyticsTableColumn ENROLLMENT_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME)
          .dataType(GEOMETRY)
          .selectExpression("en.geometry")
          .indexType(IndexType.GIST)
          .build();
  private static final AnalyticsTableColumn REGISTRATION_OU =
      AnalyticsTableColumn.builder()
          .name(EnrollmentAnalyticsColumnName.REGISTRATION_OU_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .nullable(NOT_NULL)
          .selectExpression("coalesce(registrationou.uid,ou.uid)")
          .build();

  private static final List<AnalyticsTableColumn> COMMON_COLUMNS =
      List.of(
          ENROLLMENT,
          ENROLLMENT_DATE,
          OCCURRED_DATE,
          COMPLETED_DATE,
          LAST_UPDATED,
          STORED_BY,
          ENROLLMENT_STATUS,
          OU,
          OU_NAME,
          OU_CODE,
          OU_LEVEL,
          REGISTRATION_OU);

  // Geometry-specific columns
  private static final List<AnalyticsTableColumn> GEOMETRY_COLUMNS =
      List.of(ENROLLMENT_GEOMETRY, LONGITUDE, LATITUDE);

  private static List<AnalyticsTableColumn> createJsonColumns(SqlBuilder sqlBuilder) {

    return List.of(
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.CREATED_BY_USERNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.createdbyuserinfo", "username")
                    + " as createdbyusername")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.CREATED_BY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.createdbyuserinfo", "firstName") + " as createdbyname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.CREATED_BY_LASTNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.createdbyuserinfo", "surname") + " as createdbylastname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.CREATED_BY_DISPLAY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                getDisplayName("createdbyuserinfo", "en", "createdbydisplayname", sqlBuilder))
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_USERNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.lastupdatedbyuserinfo", "username")
                    + " as lastupdatedbyusername")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.lastupdatedbyuserinfo", "firstName")
                    + " as lastupdatedbyname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_LASTNAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                sqlBuilder.jsonExtract("en.lastupdatedbyuserinfo", "surname")
                    + " as lastupdatedbylastname")
            .skipIndex(Skip.SKIP)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_DISPLAY_NAME_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression(
                getDisplayName(
                    "lastupdatedbyuserinfo", "en", "lastupdatedbydisplayname", sqlBuilder))
            .skipIndex(Skip.SKIP)
            .build());
  }

  public static List<AnalyticsTableColumn> getColumns(SqlBuilder sqlBuilder) {
    List<AnalyticsTableColumn> columns = new ArrayList<>(COMMON_COLUMNS);
    columns.addAll(createJsonColumns(sqlBuilder));
    // Add database-specific columns based on SqlBuilder capabilities
    if (sqlBuilder.supportsGeospatialData()) {
      columns.addAll(GEOMETRY_COLUMNS);
    }
    return columns;
  }
}
