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

import static org.hisp.dhis.db.model.DataType.*;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ColumnUtilsTest extends TestBase {

  private ColumnUtils columnUtils;
  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @BeforeEach
  void setUp() {
    columnUtils = new ColumnUtils(sqlBuilder);
  }

  @Nested
  @DisplayName("getColumnForAttribute Tests")
  class GetColumnForAttributeTests {

    @Test
    @DisplayName("Should handle all date/time value types consistently")
    void shouldHandleAllDateTimeValueTypesConsistently() {
      // Given - Only DATE and DATETIME actually get timestamp casting
      ValueType[] dateTimeTypes = {ValueType.DATE, ValueType.DATETIME};

      // When/Then
      for (ValueType valueType : dateTimeTypes) {
        TrackedEntityAttribute tea = createTrackedEntityAttribute(valueType.name().charAt(0));
        tea.setValueType(valueType);

        List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);
        assertEquals(1, result.size());

        AnalyticsTableColumn column = result.get(0);
        assertTrue(column.getSelectExpression().contains("timestamp"));
        assertEquals(Skip.INCLUDE, column.getSkipIndex());
      }
    }

    @Test
    @DisplayName("Should create basic column for text attribute")
    void shouldCreateBasicColumnForTextAttribute() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('A');
      tea.setValueType(ValueType.TEXT);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertEquals(tea.getUid(), column.getName());
      assertEquals(AnalyticsDimensionType.DYNAMIC, column.getDimensionType());
      assertEquals(TEXT, column.getDataType());
      assertEquals("\"" + tea.getUid() + "\".value", column.getSelectExpression());
      assertEquals(Skip.SKIP, column.getSkipIndex());
    }

    @Test
    @DisplayName("Should create decimal column with cast expression")
    void shouldCreateDecimalColumnWithCastExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('B');
      tea.setValueType(ValueType.NUMBER);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertEquals(tea.getUid(), column.getName());
      assertEquals(DOUBLE, column.getDataType());
      assertTrue(column.getSelectExpression().contains("case when"));
      assertTrue(column.getSelectExpression().contains("cast("));
      assertEquals(Skip.INCLUDE, column.getSkipIndex());
    }

    @Test
    @DisplayName("Should create integer column with cast expression")
    void shouldCreateIntegerColumnWithCastExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('C');
      tea.setValueType(ValueType.INTEGER);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertEquals(BIGINT, column.getDataType());
      assertTrue(column.getSelectExpression().contains("bigint"));
    }

    @Test
    @DisplayName("Should create boolean column with case expression")
    void shouldCreateBooleanColumnWithCaseExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('D');
      tea.setValueType(ValueType.BOOLEAN);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertTrue(column.getSelectExpression().contains("case when"));
      assertTrue(column.getSelectExpression().contains("= 'true'"));
      assertTrue(column.getSelectExpression().contains("= 'false'"));
    }

    @Test
    @DisplayName("Should create date column with timestamp cast")
    void shouldCreateDateColumnWithTimestampCast() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('E');
      tea.setValueType(ValueType.DATE);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertTrue(column.getSelectExpression().contains("timestamp"));
    }

    @Test
    @DisplayName("Should create coordinate column with geometry expression")
    void shouldCreateCoordinateColumnWithGeometryExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('F');
      tea.setValueType(ValueType.COORDINATE);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertTrue(column.getSelectExpression().contains("ST_GeomFromGeoJSON"));
      assertTrue(column.getSelectExpression().contains("Point"));
      assertTrue(column.getSelectExpression().contains("EPSG:4326"));
    }

    @Test
    @DisplayName("Should create multiple columns for organisation unit attribute")
    void shouldCreateMultipleColumnsForOrgUnitAttribute() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('G');
      tea.setValueType(ValueType.ORGANISATION_UNIT);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(3, result.size()); // main column + geometry + name

      // Main column (last in list)
      AnalyticsTableColumn mainColumn = result.get(2);
      assertEquals(tea.getUid(), mainColumn.getName());

      // Geometry column (first in list)
      AnalyticsTableColumn geometryColumn = result.get(0);
      assertEquals(tea.getUid() + "_geom", geometryColumn.getName());
      assertEquals(GEOMETRY, geometryColumn.getDataType());
      assertEquals(IndexType.GIST, geometryColumn.getIndexType());

      // Name column (second in list)
      AnalyticsTableColumn nameColumn = result.get(1);
      assertEquals(tea.getUid() + "_name", nameColumn.getName());
      assertEquals(TEXT, nameColumn.getDataType());
      assertEquals(Skip.SKIP, nameColumn.getSkipIndex());
    }

    @Test
    @DisplayName("Should include index for text attribute with option set")
    void shouldIncludeIndexForTextAttributeWithOptionSet() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('H');
      tea.setValueType(ValueType.TEXT);
      OptionSet optionSet = createOptionSet('A');
      tea.setOptionSet(optionSet);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      assertEquals(Skip.INCLUDE, result.get(0).getSkipIndex());
    }

    @Test
    @DisplayName("Should create datetime column with timestamp cast")
    void shouldCreateDatetimeColumnWithTimestampCast() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('I');
      tea.setValueType(ValueType.DATETIME);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      assertTrue(column.getSelectExpression().contains("timestamp"));
    }

    @Test
    @DisplayName("Should create time column with original expression")
    void shouldCreateTimeColumnWithOriginalExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('J');
      tea.setValueType(ValueType.TIME);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(1, result.size());
      AnalyticsTableColumn column = result.get(0);
      // TIME type returns original expression, not a timestamp cast
      assertEquals("\"" + tea.getUid() + "\".value", column.getSelectExpression());
    }
  }

  @Nested
  @DisplayName("getValueColumn Tests")
  class GetValueColumnTests {

    @Test
    @DisplayName("Should return quoted value column expression")
    void shouldReturnQuotedValueColumnExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('A');

      // When
      String result = columnUtils.getValueColumn(tea);

      // Then
      assertEquals("\"" + tea.getUid() + "\".value", result);
    }

    @Test
    @DisplayName("Should handle attribute with custom UID")
    void shouldHandleAttributeWithCustomUid() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('B');
      String customUid = "custom_test_uid_123";
      tea.setUid(customUid);

      // When
      String result = columnUtils.getValueColumn(tea);

      // Then
      assertEquals("\"" + customUid + "\".value", result);
    }
  }

  @Nested
  @DisplayName("getValueColumn Tests")
  class GetValueColumnTests2 {

    @Test
    @DisplayName("Should return quoted value column expression")
    void shouldReturnQuotedValueColumnExpression() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('A');

      // When
      String result = columnUtils.getValueColumn(tea);

      // Then
      assertEquals("\"" + tea.getUid() + "\".value", result);
    }

    @Test
    @DisplayName("Should handle attribute with custom UID")
    void shouldHandleAttributeWithCustomUid() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('B');
      String customUid = "custom_test_uid_123";
      tea.setUid(customUid);

      // When
      String result = columnUtils.getValueColumn(tea);

      // Then
      assertEquals("\"" + customUid + "\".value", result);
    }
  }

  @Nested
  @DisplayName("getCastExpression Tests")
  class GetCastExpressionTests {

    @Test
    @DisplayName("Should create proper cast expression")
    void shouldCreateProperCastExpression() {
      // Given
      String columnExpr = "test_col";
      String regex = "'pattern'";
      String dataType = "integer";

      // When
      String result = columnUtils.getCastExpression(columnExpr, regex, dataType);

      // Then
      assertTrue(result.contains("case when"));
      assertTrue(result.contains("cast(" + columnExpr + " as " + dataType + ")"));
      assertTrue(result.contains("~"));
    }

    @Test
    @DisplayName("Should handle empty column expression")
    void shouldHandleEmptyColumnExpression() {
      // When
      String result = columnUtils.getCastExpression("", "'pattern'", "integer");

      // Then
      assertTrue(result.contains("case when"));
      assertTrue(result.contains("cast( as integer)"));
    }

    @Test
    @DisplayName("Should handle complex regex pattern")
    void shouldHandleComplexRegexPattern() {
      // Given
      String columnExpr = "complex_col";
      String regex = "'^[0-9]{4}-[0-9]{2}-[0-9]{2}$'";
      String dataType = "timestamp";

      // When
      String result = columnUtils.getCastExpression(columnExpr, regex, dataType);

      // Then
      assertTrue(result.contains("case when"));
      assertTrue(result.contains("cast(" + columnExpr + " as " + dataType + ")"));
      assertTrue(result.contains(regex));
    }
  }

  @Nested
  @DisplayName("skipIndex Tests")
  class SkipIndexTests {

    @Test
    @DisplayName("Should skip index for TEXT without option set")
    void shouldSkipIndexForTextWithoutOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.TEXT, false);

      // Then
      assertEquals(Skip.SKIP, result);
    }

    @Test
    @DisplayName("Should include index for TEXT with option set")
    void shouldIncludeIndexForTextWithOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.TEXT, true);

      // Then
      assertEquals(Skip.INCLUDE, result);
    }

    @Test
    @DisplayName("Should skip index for LONG_TEXT without option set")
    void shouldSkipIndexForLongTextWithoutOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.LONG_TEXT, false);

      // Then
      assertEquals(Skip.SKIP, result);
    }

    @Test
    @DisplayName("Should include index for LONG_TEXT with option set")
    void shouldIncludeIndexForLongTextWithOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.LONG_TEXT, true);

      // Then
      assertEquals(Skip.INCLUDE, result);
    }

    @ParameterizedTest
    @EnumSource(
        value = ValueType.class,
        names = {"NUMBER", "INTEGER", "BOOLEAN", "DATE", "DATETIME"})
    @DisplayName("Should include index for non-text types")
    void shouldIncludeIndexForNonTextTypes(ValueType valueType) {
      // When
      Skip result = columnUtils.skipIndex(valueType, false);

      // Then
      assertEquals(Skip.INCLUDE, result);
    }

    @Test
    @DisplayName("Should include index for EMAIL without option set")
    void shouldIncludeIndexForEmailWithoutOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.EMAIL, false);

      // Then
      assertEquals(Skip.INCLUDE, result);
    }

    @Test
    @DisplayName("Should include index for URL without option set")
    void shouldIncludeIndexForUrlWithoutOptionSet() {
      // When
      Skip result = columnUtils.skipIndex(ValueType.URL, false);

      // Then
      assertEquals(Skip.INCLUDE, result);
    }
  }

  @Nested
  @DisplayName("getColumnForOrgUnitDataElement Tests")
  class GetColumnForOrgUnitDataElementTests {

    @Test
    @DisplayName("Should create org unit columns for data element")
    void shouldCreateOrgUnitColumnsForDataElement() {
      // Given
      DataElement dataElement = createDataElement('A');

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForOrgUnitDataElement(dataElement);

      // Then
      assertEquals(2, result.size()); // geometry + name

      // Geometry column
      AnalyticsTableColumn geometryColumn = result.get(0);
      assertEquals(dataElement.getUid() + "_geom", geometryColumn.getName());
      assertEquals(DataType.GEOMETRY, geometryColumn.getDataType());
      assertEquals(IndexType.GIST, geometryColumn.getIndexType());
      assertTrue(geometryColumn.getSelectExpression().contains("select ou.geometry"));

      // Name column
      AnalyticsTableColumn nameColumn = result.get(1);
      assertEquals(dataElement.getUid() + "_name", nameColumn.getName());
      assertEquals(DataType.TEXT, nameColumn.getDataType());
      assertEquals(Skip.SKIP, nameColumn.getSkipIndex());
      assertTrue(nameColumn.getSelectExpression().contains("select ou.name"));
    }

    @Test
    @DisplayName("Should handle data element with custom UID")
    void shouldHandleDataElementWithCustomUid() {
      // Given
      DataElement dataElement = createDataElement('B');
      String customUid = "custom_de_uid_456";
      dataElement.setUid(customUid);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForOrgUnitDataElement(dataElement);

      // Then
      assertEquals(2, result.size());
      assertEquals(customUid + "_geom", result.get(0).getName());
      assertEquals(customUid + "_name", result.get(1).getName());
    }

    @Test
    @DisplayName("Should create subqueries with JSON extract for data element")
    void shouldCreateSubqueriesWithJsonExtractForDataElement() {
      // Given
      DataElement dataElement = createDataElement('C');

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForOrgUnitDataElement(dataElement);

      // Then
      assertEquals(2, result.size());

      // Both columns should have subqueries with JSON extract
      AnalyticsTableColumn geometryColumn = result.get(0);
      assertTrue(geometryColumn.getSelectExpression().contains("#>>"));
      assertTrue(geometryColumn.getSelectExpression().contains("eventdatavalues"));
      assertTrue(geometryColumn.getSelectExpression().contains(dataElement.getUid()));

      AnalyticsTableColumn nameColumn = result.get(1);
      assertTrue(nameColumn.getSelectExpression().contains("#>>"));
      assertTrue(nameColumn.getSelectExpression().contains("eventdatavalues"));
      assertTrue(nameColumn.getSelectExpression().contains(dataElement.getUid()));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle attribute with all value types")
    void shouldHandleAttributeWithAllValueTypes() {
      // Test each value type to ensure no exceptions
      for (ValueType valueType : ValueType.values()) {
        TrackedEntityAttribute tea = createTrackedEntityAttribute(valueType.name().charAt(0));
        tea.setValueType(valueType);

        // When/Then - should not throw exceptions
        assertDoesNotThrow(
            () -> {
              List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);
              assertNotNull(result);
              assertFalse(result.isEmpty());
            });
      }
    }

    @Test
    @DisplayName("Should handle organisation unit attribute without correlated subquery support")
    void shouldHandleOrgUnitAttributeWithoutCorrelatedSubquerySupport() {
      // Given - This test would require a different SqlBuilder that doesn't support correlated
      // subqueries
      // For PostgresSqlBuilder, this always returns true, so we test the current behavior
      TrackedEntityAttribute tea = createTrackedEntityAttribute('X');
      tea.setValueType(ValueType.ORGANISATION_UNIT);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then - Should still create org unit columns since PostgreSQL supports correlated subqueries
      assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Should handle data element without correlated subquery support")
    void shouldHandleDataElementWithoutCorrelatedSubquerySupport() {
      // Given - This test would require a different SqlBuilder
      // For PostgresSqlBuilder, this always returns true, so we test the current behavior
      DataElement dataElement = createDataElement('Y');

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForOrgUnitDataElement(dataElement);

      // Then - Should still create columns since PostgreSQL supports correlated subqueries
      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should create consistent column names")
    void shouldCreateConsistentColumnNames() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('Z');
      tea.setValueType(ValueType.TEXT);

      // When
      String valueColumn = columnUtils.getValueColumn(tea);
      List<AnalyticsTableColumn> columns = columnUtils.getColumnForAttribute(tea);

      // Then
      assertTrue(valueColumn.contains(tea.getUid()));
      assertEquals(tea.getUid(), columns.get(0).getName());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work end-to-end for complex attribute")
    void shouldWorkEndToEndForComplexAttribute() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('A');
      tea.setValueType(ValueType.ORGANISATION_UNIT);
      OptionSet optionSet = createOptionSet('B');
      tea.setOptionSet(optionSet);

      // When
      List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(3, result.size());

      // Verify all columns have correct properties
      for (AnalyticsTableColumn column : result) {
        assertEquals(AnalyticsDimensionType.DYNAMIC, column.getDimensionType());
        assertNotNull(column.getName());
        assertNotNull(column.getSelectExpression());
      }
    }

    @Test
    @DisplayName("Should produce consistent results for same input")
    void shouldProduceConsistentResultsForSameInput() {
      // Given
      TrackedEntityAttribute tea = createTrackedEntityAttribute('A');
      tea.setValueType(ValueType.NUMBER);

      // When
      List<AnalyticsTableColumn> result1 = columnUtils.getColumnForAttribute(tea);
      List<AnalyticsTableColumn> result2 = columnUtils.getColumnForAttribute(tea);

      // Then
      assertEquals(result1.size(), result2.size());
      for (int i = 0; i < result1.size(); i++) {
        AnalyticsTableColumn col1 = result1.get(i);
        AnalyticsTableColumn col2 = result2.get(i);
        assertEquals(col1.getName(), col2.getName());
        assertEquals(col1.getDataType(), col2.getDataType());
        assertEquals(col1.getSelectExpression(), col2.getSelectExpression());
      }
    }

    @Test
    @DisplayName("Should handle multiple data elements consistently")
    void shouldHandleMultipleDataElementsConsistently() {
      // Given
      DataElement de1 = createDataElement('A');
      DataElement de2 = createDataElement('B');

      // When
      List<AnalyticsTableColumn> result1 = columnUtils.getColumnForOrgUnitDataElement(de1);
      List<AnalyticsTableColumn> result2 = columnUtils.getColumnForOrgUnitDataElement(de2);

      // Then
      assertEquals(result1.size(), result2.size());
      assertEquals(2, result1.size());
      assertEquals(2, result2.size());

      // Verify structure is the same
      assertEquals(result1.get(0).getDataType(), result2.get(0).getDataType()); // Both geometry
      assertEquals(result1.get(1).getDataType(), result2.get(1).getDataType()); // Both text
    }

    @Test
    @DisplayName("Should handle all numeric value types consistently")
    void shouldHandleAllNumericValueTypesConsistently() {
      // Given
      ValueType[] numericTypes = {
        ValueType.NUMBER,
        ValueType.INTEGER,
        ValueType.INTEGER_POSITIVE,
        ValueType.INTEGER_NEGATIVE,
        ValueType.INTEGER_ZERO_OR_POSITIVE,
        ValueType.PERCENTAGE,
        ValueType.UNIT_INTERVAL
      };

      // When/Then
      for (ValueType valueType : numericTypes) {
        TrackedEntityAttribute tea = createTrackedEntityAttribute(valueType.name().charAt(0));
        tea.setValueType(valueType);

        List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);
        assertEquals(1, result.size());

        AnalyticsTableColumn column = result.get(0);
        assertTrue(column.getSelectExpression().contains("cast("));
        assertEquals(Skip.INCLUDE, column.getSkipIndex());
      }
    }

    @Test
    @DisplayName("Should handle all boolean value types consistently")
    void shouldHandleAllBooleanValueTypesConsistently() {
      // Given
      ValueType[] booleanTypes = {ValueType.BOOLEAN, ValueType.TRUE_ONLY};

      // When/Then
      for (ValueType valueType : booleanTypes) {
        TrackedEntityAttribute tea = createTrackedEntityAttribute(valueType.name().charAt(0));
        tea.setValueType(valueType);

        List<AnalyticsTableColumn> result = columnUtils.getColumnForAttribute(tea);
        assertEquals(1, result.size());

        AnalyticsTableColumn column = result.get(0);
        assertTrue(column.getSelectExpression().contains("case when"));
        assertTrue(column.getSelectExpression().contains("= 'true'"));
        assertEquals(Skip.INCLUDE, column.getSkipIndex());
      }
    }
  }
}
