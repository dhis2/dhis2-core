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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventStatusFilterHandlerTest {

  private EventStatusFilterHandler handler;

  @BeforeEach
  void setUp() {
    handler = new EventStatusFilterHandler();
  }

  @Test
  void supports_withEventStatusAndProgramStage_returnsTrue() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));
    queryItem.setProgramStage(programStage);

    assertTrue(handler.supports(queryItem));
  }

  @Test
  void supports_withEventStatusWithoutProgramStage_returnsFalse() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void supports_withOtherItem_returnsFalse() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void applyFilters_withSingleStatus_addsINFilter() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "ACTIVE"};

    handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:ACTIVE", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ACTIVE"));
  }

  @Test
  void applyFilters_withMultipleStatuses_addsINFilterWithAllStatuses() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "ACTIVE;COMPLETED"};

    handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:ACTIVE;COMPLETED", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ACTIVE;COMPLETED"));
  }

  @Test
  void applyFilters_withAllValidStatuses_addsINFilterWithAllStatuses() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "ACTIVE;COMPLETED;SCHEDULE"};

    handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:ACTIVE;COMPLETED;SCHEDULE", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ACTIVE;COMPLETED;SCHEDULE"));
  }

  @Test
  void applyFilters_withLowercaseStatus_normalizesToUppercase() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "active;completed"};

    handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:active;completed", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getFilter(), is("ACTIVE;COMPLETED"));
  }

  @Test
  void applyFilters_withInvalidStatus_throwsException() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "INVALID"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:INVALID", null));
  }

  @Test
  void applyFilters_withMixedValidAndInvalidStatuses_throwsException() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "ACTIVE;INVALID"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:ACTIVE;INVALID", null));
  }

  @Test
  void applyFilters_withNoFilterPart_throwsException() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_STATUS", null));
  }

  @Test
  void applyFilters_withTooManyFilterParts_throwsException() {
    QueryItem queryItem = createEventStatusQueryItem();
    String[] filterParts = {"EVENT_STATUS", "IN", "ACTIVE"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "EVENT_STATUS:IN:ACTIVE", null));
  }

  private QueryItem createEventStatusQueryItem() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));
    queryItem.setProgramStage(programStage);
    return queryItem;
  }
}
