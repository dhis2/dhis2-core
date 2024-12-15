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

  public static final AnalyticsTableColumn TRACKED_ENTITY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME)
          .dataType(CHARACTER_11)
          .selectExpression("te.uid")
          .build();
  public static final AnalyticsTableColumn TRACKED_ENTITY_GEOMETRY =
      AnalyticsTableColumn.builder()
          .name(EventAnalyticsColumnName.TRACKED_ENTITY_GEOMETRY_COLUMN_NAME)
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
            .name(EventAnalyticsColumnName.EVENT_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ev.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("en.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.PS_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ps.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.AO_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("acs.categoryoptioncombouid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("en.enrollmentdate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("en.occurreddate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("ev.occurreddate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("ev.scheduleddate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression("ev.completeddate")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.CREATED_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression(firstIfNotNullOrElse("ev.createdatclient", "ev.created"))
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME)
            .dataType(TIMESTAMP)
            .selectExpression(firstIfNotNullOrElse("ev.lastupdatedatclient", "ev.lastupdated"))
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.STORED_BY_COLUMN_NAME)
            .dataType(VARCHAR_255)
            .selectExpression("ev.storedby")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME)
            .dataType(VARCHAR_50)
            .selectExpression("ev.status")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME)
            .dataType(VARCHAR_50)
            .selectExpression("en.status")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OU_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ou.uid")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OU_NAME_COLUMN_NAME)
            .dataType(TEXT)
            .nullable(NOT_NULL)
            .selectExpression("ou.name")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OU_CODE_COLUMN_NAME)
            .dataType(TEXT)
            .selectExpression("ou.code")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OU_LEVEL_COLUMN_NAME)
            .dataType(INTEGER)
            .selectExpression("ous.level")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.REGISTRATION_OU_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("coalesce(registrationou.uid,ou.uid)")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME)
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("coalesce(enrollmentou.uid,ou.uid)")
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
            .name(EventAnalyticsColumnName.EVENT_GEOMETRY_COLUMN_NAME)
            .dataType(GEOMETRY)
            .selectExpression("ev.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.OU_GEOMETRY_COLUMN_NAME)
            .dataType(GEOMETRY)
            .selectExpression("ou.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME)
            .dataType(GEOMETRY)
            .selectExpression("en.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LONGITUDE_COLUMN_NAME)
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(ev.geometry) THEN ST_X(ev.geometry) ELSE null END")
            .build(),
        AnalyticsTableColumn.builder()
            .name(EventAnalyticsColumnName.LATITUDE_COLUMN_NAME)
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(ev.geometry) THEN ST_Y(ev.geometry) ELSE null END")
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
