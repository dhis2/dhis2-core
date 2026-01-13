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
package org.hisp.dhis.analytics.event.data.queryitem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateFilterHandlerTest {

  private DateFilterHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DateFilterHandler();
  }

  @Test
  void supports_withOccurredDateAndDateType_returnsTrue() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.DATE);

    assertTrue(handler.supports(queryItem));
  }

  @Test
  void supports_withScheduledDateAndDateType_returnsTrue() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.DATE);

    assertTrue(handler.supports(queryItem));
  }

  @Test
  void supports_withOtherItem_returnsFalse() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void supports_withOccurredDateNonDateType_returnsFalse() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.TEXT);

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void applyFilters_withDateRange_addsGEAndLEFilters() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "2025-01-01_2025-01-31"};

    handler.applyFilters(queryItem, filterParts, "EVENT_DATE:2025-01-01_2025-01-31", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GE));
    assertThat(filters.get(0).getFilter(), is("2025-01-01"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.LE));
    assertThat(filters.get(1).getFilter(), is("2025-01-31"));
  }

  @Test
  void applyFilters_withIsoPeriod_addsGEAndLEFilters() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "2025Q1"};

    handler.applyFilters(queryItem, filterParts, "EVENT_DATE:2025Q1", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GE));
    assertThat(filters.get(0).getFilter(), is("2025-01-01"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.LE));
    assertThat(filters.get(1).getFilter(), is("2025-03-31"));
  }

  @Test
  void applyFilters_withMonthlyPeriod_addsGEAndLEFilters() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "202501"};

    handler.applyFilters(queryItem, filterParts, "EVENT_DATE:202501", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GE));
    assertThat(filters.get(0).getFilter(), is("2025-01-01"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.LE));
    assertThat(filters.get(1).getFilter(), is("2025-01-31"));
  }

  @Test
  void applyFilters_withExplicitOperatorInSingleFilter_addsFilter() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "GT:2025-01-01"};

    handler.applyFilters(queryItem, filterParts, "EVENT_DATE:GT:2025-01-01", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GT));
    assertThat(filters.get(0).getFilter(), is("2025-01-01"));
  }

  @Test
  void applyFilters_withMultipleOperators_addsAllFilters() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "GT", "2025-01-01", "LT", "2025-12-31"};

    handler.applyFilters(queryItem, filterParts, "EVENT_DATE:GT:2025-01-01:LT:2025-12-31", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GT));
    assertThat(filters.get(0).getFilter(), is("2025-01-01"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.LT));
    assertThat(filters.get(1).getFilter(), is("2025-12-31"));
  }

  @Test
  void applyFilters_withNoFilterPart_throwsException() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_DATE", null));
  }

  @Test
  void applyFilters_withInvalidFilterString_throwsException() {
    QueryItem queryItem = createDateQueryItem();
    String[] filterParts = {"EVENT_DATE", "INVALID_VALUE"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_DATE:INVALID_VALUE", null));
  }

  private QueryItem createDateQueryItem() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.DATE);
    return queryItem;
  }
}
