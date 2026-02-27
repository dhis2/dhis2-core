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
package org.hisp.dhis.analytics.event.data;

// ABOUTME: Unit tests for AggregatedRowBuilder.
// ABOUTME: Tests row building logic for aggregated event analytics.

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramIndicator;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ou.OrgUnitRowAccess;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AggregatedRowBuilderTest {

  @Mock private SqlRowSet rowSet;

  @Spy private AnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private BiFunction<QueryItem, EventQueryParams, ColumnAndAlias> columnAliasResolver;
  private Function<EventQueryParams, String> itemIdProvider;

  private Program programA;

  @BeforeEach
  void setUp() {
    programA = createProgram('A');
    columnAliasResolver =
        (item, params) -> ColumnAndAlias.ofColumnAndAlias(item.getItemName(), item.getItemName());
    itemIdProvider = params -> "itemId";
  }

  @Test
  void testBuildRowWithTextQueryItem() {
    DataElement textElement = createDataElement('T');
    textElement.setValueType(ValueType.TEXT);

    QueryItem textItem =
        new QueryItem(textElement, programA, null, ValueType.TEXT, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(textItem).build();

    when(rowSet.getString(textElement.getUid())).thenReturn("textValue");
    when(rowSet.getInt("value")).thenReturn(10);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("textValue"));
    assertThat(row.get(1), is(10));
  }

  @Test
  void testBuildRowWithDateQueryItemRendersTimestamp() {
    DataElement dateElement = createDataElement('D');
    dateElement.setValueType(ValueType.DATE);

    QueryItem dateItem =
        new QueryItem(dateElement, programA, null, ValueType.DATE, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(dateItem).build();

    when(rowSet.getString(dateElement.getUid())).thenReturn("2024-01-15");
    when(rowSet.getInt("value")).thenReturn(1);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    verify(sqlBuilder).renderTimestamp("2024-01-15");
    assertThat(row, hasSize(2));
  }

  @Test
  void testBuildRowWithDateTimeQueryItemRendersTimestamp() {
    DataElement dateTimeElement = createDataElement('T');
    dateTimeElement.setValueType(ValueType.DATETIME);

    QueryItem dateTimeItem =
        new QueryItem(
            dateTimeElement, programA, null, ValueType.DATETIME, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(dateTimeItem).build();

    when(rowSet.getString(dateTimeElement.getUid())).thenReturn("2024-01-15T10:30:00");
    when(rowSet.getInt("value")).thenReturn(1);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    verify(sqlBuilder).renderTimestamp("2024-01-15T10:30:00");
    assertThat(row, hasSize(2));
  }

  @Test
  void testBuildRowWithAggregateDataAndValueDimension() {
    DataElement numericElement = createDataElement('N');
    numericElement.setValueType(ValueType.NUMBER);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withValue(numericElement)
            .withAggregateData(true)
            .build();

    when(rowSet.getDouble("value")).thenReturn(42.5);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("itemId"));
    assertThat(row.get(1), is(42.5));
  }

  @Test
  void testBuildRowWithProgramIndicatorDimension() {
    ProgramIndicator indicator = createProgramIndicator('I', programA, "1", "filter");
    indicator.setDecimals(2);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withProgramIndicator(indicator)
            .withAggregateData(true)
            .build();

    when(rowSet.getDouble("value")).thenReturn(3.14159);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is(indicator.getUid()));
  }

  @Test
  void testBuildRowWithDimensions() {
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(), ValueType.TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addDimension(periodDimension).build();

    when(rowSet.getString("pe")).thenReturn("2024Q1");
    when(rowSet.getInt("value")).thenReturn(100);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("2024Q1"));
    assertThat(row.get(1), is(100));
  }

  @Test
  void testBuildRowWithDateDimensionRendersTimestamp() {
    BaseDimensionalObject dateDimension =
        new BaseDimensionalObject("eventdate", DimensionType.PERIOD, List.of(), ValueType.DATE);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addDimension(dateDimension).build();

    when(rowSet.getString("eventdate")).thenReturn("2024-01-15");
    when(rowSet.getInt("value")).thenReturn(1);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    verify(sqlBuilder).renderTimestamp("2024-01-15");
    assertThat(row, hasSize(2));
  }

  @Test
  void testBuildRowWithDateValueDimensionRendersTimestamp() {
    DataElement dateElement = createDataElement('D');
    dateElement.setValueType(ValueType.DATE);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).withValue(dateElement).build();

    when(rowSet.getString("value")).thenReturn("2024-01-15");

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    verify(sqlBuilder).renderTimestamp("2024-01-15");
    assertThat(row, hasSize(1));
  }

  @Test
  void testBuildRowWithEmptyAliasFallsBackToItemName() {
    DataElement element = createDataElement('E');
    element.setValueType(ValueType.TEXT);

    QueryItem item =
        new QueryItem(element, programA, null, ValueType.TEXT, AggregationType.NONE, null);

    // Return empty alias to trigger fallback
    BiFunction<QueryItem, EventQueryParams, ColumnAndAlias> emptyAliasResolver =
        (qi, p) -> ColumnAndAlias.ofColumn(qi.getItemName());

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(item).build();

    when(rowSet.getString(element.getUid())).thenReturn("fallbackValue");
    when(rowSet.getInt("value")).thenReturn(1);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, emptyAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("fallbackValue"));
  }

  @Test
  void testBuildRowWithNumericValueDimensionSkipRounding() {
    DataElement numericElement = createDataElement('N');
    numericElement.setValueType(ValueType.NUMBER);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withValue(numericElement)
            .withSkipRounding(true)
            .build();

    when(rowSet.getDouble("value")).thenReturn(3.14159265359);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(1));
    assertThat(row.get(0), is(3.14159265359));
  }

  @Test
  void testBuildRowWithNumericValueDimensionWithRounding() {
    DataElement numericElement = createDataElement('N');
    numericElement.setValueType(ValueType.NUMBER);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withValue(numericElement)
            .withSkipRounding(false)
            .build();

    when(rowSet.getDouble("value")).thenReturn(3.14159265359);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(1));
    // getRounded rounds to 2 decimal places
    assertThat(row.get(0), is(3.14));
  }

  @Test
  void testBuildRowWithTextValueDimension() {
    DataElement textElement = createDataElement('T');
    textElement.setValueType(ValueType.TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).withValue(textElement).build();

    when(rowSet.getString("value")).thenReturn("textDimensionValue");

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(1));
    assertThat(row.get(0), is("textDimensionValue"));
  }

  @Test
  void testBuildRowWithMultipleQueryItems() {
    DataElement element1 = createDataElement('A');
    element1.setValueType(ValueType.TEXT);
    DataElement element2 = createDataElement('B');
    element2.setValueType(ValueType.INTEGER);

    QueryItem item1 =
        new QueryItem(element1, programA, null, ValueType.TEXT, AggregationType.NONE, null);
    QueryItem item2 =
        new QueryItem(element2, programA, null, ValueType.INTEGER, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(item1).addItem(item2).build();

    when(rowSet.getString(element1.getUid())).thenReturn("value1");
    when(rowSet.getString(element2.getUid())).thenReturn("42");
    when(rowSet.getInt("value")).thenReturn(100);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(3));
    assertThat(row.get(0), is("value1"));
    assertThat(row.get(1), is("42"));
    assertThat(row.get(2), is(100));
  }

  @Test
  void testBuildRowWithMultipleDimensions() {
    BaseDimensionalObject dim1 =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(), ValueType.TEXT);
    BaseDimensionalObject dim2 =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(), ValueType.TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .addDimension(dim1)
            .addDimension(dim2)
            .build();

    when(rowSet.getString("pe")).thenReturn("2024Q1");
    when(rowSet.getString("ou")).thenReturn("OrgUnitA");
    when(rowSet.getInt("value")).thenReturn(50);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(3));
    assertThat(row.get(0), is("2024Q1"));
    assertThat(row.get(1), is("OrgUnitA"));
    assertThat(row.get(2), is(50));
  }

  @Test
  void testBuildRowWithNullValueFromRowSet() {
    DataElement element = createDataElement('N');
    element.setValueType(ValueType.TEXT);

    QueryItem item =
        new QueryItem(element, programA, null, ValueType.TEXT, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(programA).addItem(item).build();

    when(rowSet.getString(element.getUid())).thenReturn(null);
    when(rowSet.getInt("value")).thenReturn(0);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    assertThat(row, hasSize(2));
    assertThat(row.get(0), is((Object) null));
  }

  @Test
  void testBuildRowWithOutputIdSchemeUid() {
    DataElement element = createDataElement('U');
    element.setValueType(ValueType.TEXT);

    QueryItem item =
        new QueryItem(element, programA, null, ValueType.TEXT, AggregationType.NONE, null);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .addItem(item)
            .withOutputIdScheme(IdScheme.UID)
            .build();

    when(rowSet.getString(element.getUid())).thenReturn("someValue");
    when(rowSet.getInt("value")).thenReturn(1);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    // When OutputIdScheme is UID, it goes through resolveOutputValue
    // which returns the original value if no option/legend match
    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("someValue"));
  }

  @Test
  void testBuildRowWithEnrollmentOuDimensionReadsEnrollmentOuColumn() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withEnrollmentOuDimension(List.of(createOrganisationUnit('A')))
            .build();

    when(rowSet.getString(OrgUnitRowAccess.enrollmentOuResultColumn())).thenReturn("ouUid");
    when(rowSet.getInt("value")).thenReturn(5);

    List<Object> row =
        AggregatedRowBuilder.create(params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider)
            .build();

    verify(rowSet).getString(OrgUnitRowAccess.enrollmentOuResultColumn());
    assertThat(row, hasSize(2));
    assertThat(row.get(0), is("ouUid"));
    assertThat(row.get(1), is(5));
  }
}
