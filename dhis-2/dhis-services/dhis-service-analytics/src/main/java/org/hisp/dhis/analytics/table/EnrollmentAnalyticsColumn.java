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

  /**
   * Returns a list of {@link AnalyticsTableColumn}.
   *
   * @param sqlBuilder the {@link SqlBuilder}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public static List<AnalyticsTableColumn> getColumns(SqlBuilder sqlBuilder) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(getCommonColumns(sqlBuilder));
    columns.addAll(getJsonColumns(sqlBuilder));

    if (sqlBuilder.supportsGeospatialData()) {
      columns.addAll(getGeometryColumns(sqlBuilder));
    }

    return columns;
  }

  /**
   * Returns a list of {@link AnalyticsTableColumn}.
   *
   * @param sqlBuilder the {@link SqlBuilder}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private static List<AnalyticsTableColumn> getCommonColumns(SqlBuilder sqlBuilder) {
    return List.of(
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.ENROLLMENT_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("en.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("en.enrollmentdate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("en.occurreddate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression(sqlBuilder.ifThen("en.status = 'COMPLETED'", "en.completeddate"))
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression(
                sqlBuilder.ifThenElse(
                    "en.createdatclient is not null", "en.createdatclient", "en.created"))
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression(
                sqlBuilder.ifThenElse(
                    "en.lastupdatedatclient is not null",
                    "en.lastupdatedatclient",
                    "en.lastupdated"))
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.STORED_BY_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression("en.storedby")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME)
            .dataType(VARCHAR_50)
            .selectExpression("en.status")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.OU_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ou.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.OU_NAME_COLUMN_NAME)
            .dataType(TEXT)
            .nullable(NOT_NULL)
            .selectExpression("ou.name")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.OU_CODE_COLUMN_NAME)
            .dataType(TEXT)
            .selectExpression("ou.code")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.OU_LEVEL_COLUMN_NAME)
            .dataType(INTEGER)
            .selectExpression("ous.level")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.REGISTRATION_OU_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("coalesce(registrationou.uid,ou.uid)")
            .build());
  }

  /**
   * Returns a list of geometry {@link AnalyticsTableColumn}.
   *
   * @param sqlBuilder the {@link SqlBuilder}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private static List<AnalyticsTableColumn> getGeometryColumns(SqlBuilder sqlBuilder) {
    return List.of(
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME)
            .dataType(GEOMETRY)
            .selectExpression("en.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LONGITUDE_COLUMN_NAME)
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(en.geometry) THEN ST_X(en.geometry) ELSE null END")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EnrollmentAnalyticsColumnName.LATITUDE_COLUMN_NAME)
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(en.geometry) THEN ST_Y(en.geometry) ELSE null END")
            .build());
  }

  /**
   * Returns a list of {@link AnalyticsTableColumn}.
   *
   * @param sqlBuilder the {@link SqlBuilder}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private static List<AnalyticsTableColumn> getJsonColumns(SqlBuilder sqlBuilder) {
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
}
