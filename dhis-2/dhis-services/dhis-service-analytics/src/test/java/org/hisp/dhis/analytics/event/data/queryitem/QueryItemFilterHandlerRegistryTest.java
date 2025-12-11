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

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryItemFilterHandlerRegistryTest {

  private QueryItemFilterHandlerRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new QueryItemFilterHandlerRegistry();
  }

  @Test
  void handlerFor_withOccurredDateQueryItem_returnsDateFilterHandler() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.DATE);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(DateFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withScheduledDateQueryItem_returnsDateFilterHandler() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.DATE);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(DateFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withEventStatusAndProgramStage_returnsEventStatusFilterHandler() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));
    queryItem.setProgramStage(programStage);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(EventStatusFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withOrgUnitAndProgramStage_returnsOrgUnitFilterHandler() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME));
    queryItem.setProgramStage(programStage);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(OrgUnitFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withGenericDataElement_returnsGenericFilterHandler() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(GenericFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withEventStatusWithoutProgramStage_returnsGenericFilterHandler() {
    // EVENT_STATUS without program stage should fall through to generic
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME));

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(GenericFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withOrgUnitWithoutProgramStage_returnsGenericFilterHandler() {
    // OU without program stage should fall through to generic
    QueryItem queryItem =
        new QueryItem(new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME));

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(GenericFilterHandler.class, handler);
  }

  @Test
  void handlerFor_withOccurredDateNonDateValueType_returnsGenericFilterHandler() {
    // OCCURRED_DATE with non-DATE value type should fall through to generic
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    queryItem.setValueType(ValueType.TEXT);

    QueryItemFilterHandler handler = registry.handlerFor(queryItem);

    assertInstanceOf(GenericFilterHandler.class, handler);
  }
}
