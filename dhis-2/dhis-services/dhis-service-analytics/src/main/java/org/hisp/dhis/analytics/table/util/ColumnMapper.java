/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.table.util;

import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.analytics.table.ColumnRegex.NUMERIC_REGEXP;
import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_NAME_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.TEXT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This component is responsible for mapping {@link TrackedEntityAttribute} and {@link DataElement}
 * to {@link AnalyticsTableColumn} objects, which are used in analytics tables.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColumnMapper {

  private final SqlBuilder sqlBuilder;
  private final SystemSettingsProvider settingsProvider;
  private static final EnumSet<ValueType> NO_INDEX_VAL_TYPES =
      EnumSet.of(ValueType.TEXT, ValueType.LONG_TEXT);

  /**
   * Matches the following patterns:
   *
   * <ul>
   *   <li>1999-12-12
   *   <li>1999-12-12T
   *   <li>1999-12-12T10:10:10
   *   <li>1999-10-10 10:10:10
   *   <li>1999-10-10 10:10
   *   <li>2021-12-14T11:45:00.000Z
   *   <li>2021-12-14T11:45:00.000
   * </ul>
   */
  protected static final String DATE_REGEXP =
      "'^[0-9]{4}-[0-9]{2}-[0-9]{2}(\\s|T)?(([0-9]{2}:)([0-9]{2}:)?([0-9]{2}))?(|.([0-9]{3})|.([0-9]{3})Z)?$'";

  /**
   * Returns a list of columns for the given {@link TrackedEntityAttribute}.
   *
   * @param attribute the {@link TrackedEntityAttribute} from which to map columns.
   * @return a list of {@link AnalyticsTableColumn} mapped to the {@link TrackedEntityAttribute}.
   */
  public List<AnalyticsTableColumn> getColumnsForAttribute(TrackedEntityAttribute attribute) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String valueColumn = getValueColumn(attribute);
    DataType dataType = getColumnType(attribute.getValueType(), isGeospatialSupport());
    String selectExpression = getColumnExpression(attribute.getValueType(), valueColumn);
    Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

    if (attribute.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnForOrgUnitAttribute(attribute));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(attribute.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(selectExpression)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  public String getValueColumn(TrackedEntityAttribute attribute) {
    return String.format("%s.%s", quote(attribute.getUid()), "value");
  }

  /**
   * Maps a {@link DataElement} to a list of {@link AnalyticsTableColumn}.
   *
   * @param dataElement the {@link DataElement}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public List<AnalyticsTableColumn> getColumnsForOrgUnitDataElement(DataElement dataElement) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

    return buildOrgUnitColumns(
        dataElement.getUid(), column -> getOrgUnitSelectSubquery(dataElement, column));
  }

  /**
   * Quotes the given relation.
   *
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  public String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  public boolean isGeospatialSupport() {
    return sqlBuilder.supportsGeospatialData();
  }

  /**
   * Returns a column expression, potentially with a cast statement, based on the given value type.
   *
   * @param valueType the {@link ValueType} to represent as database column type.
   * @param columnExpression the expression or name of the column to be selected.
   * @return a select expression appropriate for the given value type and context.
   */
  public String getColumnExpression(ValueType valueType, String columnExpression) {
    if (valueType.isDecimal()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeDouble());
    } else if (valueType.isInteger()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeBigInt());
    } else if (valueType.isBoolean()) {
      return sqlBuilder.ifThenElse(
          columnExpression + " = 'true'", "1", columnExpression + " = 'false'", "0", "null");
    } else if (valueType.isDate()) {
      return getCastExpression(columnExpression, DATE_REGEXP, sqlBuilder.dataTypeTimestamp());
    } else if (valueType.isGeo() && isGeospatialSupport()) {
      return String.format(
          """
                        ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || (%s) || \
                        ', "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}')""",
          columnExpression);
    } else {
      return columnExpression;
    }
  }

  /**
   * Returns a cast expression which includes a value filter for the given value type.
   *
   * @param columnExpression the column expression.
   * @param filterRegex the value type filter regular expression.
   * @param dataType the SQL data type.
   * @return a cast and validate expression.
   */
  public String getCastExpression(String columnExpression, String filterRegex, String dataType) {
    String filter = sqlBuilder.regexpMatch(columnExpression, filterRegex);
    String result = String.format("cast(%s as %s)", columnExpression, dataType);

    return sqlBuilder.ifThen(filter, result);
  }

  /**
   * Indicates whether creating an index should be skipped.
   *
   * @param valueType the {@link ValueType}.
   * @param hasOptionSet whether an option set exists.
   * @return a {@link Skip}.
   */
  public Skip skipIndex(ValueType valueType, boolean hasOptionSet) {
    boolean skipIndex = NO_INDEX_VAL_TYPES.contains(valueType) && !hasOptionSet;
    return skipIndex ? Skip.SKIP : Skip.INCLUDE;
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForOrgUnitAttribute(
      TrackedEntityAttribute attribute) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

    Validate.isTrue(attribute.getValueType().isOrganisationUnit());

    return buildOrgUnitColumns(
        attribute.getUid(), column -> getOrgUnitSelectSubquery(attribute, column));
  }

  /**
   * Builds organization unit columns (geometry and name) for the given UID.
   *
   * @param uid the unique identifier.
   * @param subqueryProvider function that provides the select subquery for a given column.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> buildOrgUnitColumns(
      String uid, UnaryOperator<String> subqueryProvider) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (isGeospatialSupport()) {
      columns.add(
          AnalyticsTableColumn.builder()
              .name(uid + OU_GEOMETRY_COL_SUFFIX)
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(subqueryProvider.apply("geometry"))
              .indexType(IndexType.GIST)
              .build());
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(uid + OU_NAME_COL_SUFFIX)
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(subqueryProvider.apply("name"))
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Wraps the base query format with ST_Centroid if the column is geometry and the current settings
   * allow it.
   *
   * @param column the column name.
   * @param baseFormat the base SQL format string.
   * @return the format string, optionally wrapped with ST_Centroid.
   */
  private String wrapWithCentroid(String column, String baseFormat) {
    return column.equals("geometry")
            && this.settingsProvider.getCurrentSettings().getOrgUnitCentroidsInEventsAnalytics()
        ? "ST_Centroid(" + baseFormat + ")"
        : baseFormat;
  }

  /**
   * Builds an organization unit select subquery.
   *
   * @param column the column name.
   * @param columnExpression the column expression for the where clause.
   * @param alias the alias for the result.
   * @return an org unit select query.
   */
  private String buildOrgUnitSelectSubquery(String column, String columnExpression, String alias) {
    String baseFormat =
        "(select ou.${column} from ${organisationunit} ou where ou.uid = ${columnExpression})";
    String finalFormat = wrapWithCentroid(column, baseFormat) + " as ${alias}";

    return replaceQualify(
        sqlBuilder,
        finalFormat,
        Map.of(
            "column", column,
            "columnExpression", columnExpression,
            "alias", alias));
  }

  /**
   * Returns a org unit select query.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param column the column name.
   * @return an org unit select query.
   */
  private String getOrgUnitSelectSubquery(TrackedEntityAttribute attribute, String column) {
    String valueColumn = getValueColumn(attribute);
    String columnExpression = getColumnExpression(attribute.getValueType(), valueColumn);
    String alias = quote(attribute.getUid());

    return buildOrgUnitSelectSubquery(column, columnExpression, alias);
  }

  /**
   * Returns a org unit select query.
   *
   * @param dataElement the {@link DataElement}.
   * @param column the column name.
   * @return an org unit select query.
   */
  private String getOrgUnitSelectSubquery(DataElement dataElement, String column) {
    String columnExpression =
        sqlBuilder.jsonExtract("eventdatavalues", dataElement.getUid(), "value");
    String alias = quote(dataElement.getUid());

    return buildOrgUnitSelectSubquery(column, columnExpression, alias);
  }
}
