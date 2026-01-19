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

class OrgUnitFilterHandlerTest {

  private OrgUnitFilterHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OrgUnitFilterHandler();
  }

  @Test
  void supports_withOrgUnitAndProgramStage_returnsTrue() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME));
    queryItem.setProgramStage(programStage);

    assertTrue(handler.supports(queryItem));
  }

  @Test
  void supports_withOrgUnitWithoutProgramStage_returnsFalse() {
    QueryItem queryItem =
        new QueryItem(new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME));

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void supports_withOtherItem_returnsFalse() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);

    assertFalse(handler.supports(queryItem));
  }

  @Test
  void applyFilters_withSingleOrgUnit_addsINFilter() {
    QueryItem queryItem = createOrgUnitQueryItem();
    String[] filterParts = {"ou", "ouA"};

    handler.applyFilters(queryItem, filterParts, "ou:ouA", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ouA"));
  }

  @Test
  void applyFilters_withMultipleOrgUnits_addsINFilterWithAllOrgUnits() {
    QueryItem queryItem = createOrgUnitQueryItem();
    String[] filterParts = {"ou", "ouA;ouB;ouC"};

    handler.applyFilters(queryItem, filterParts, "ou:ouA;ouB;ouC", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ouA;ouB;ouC"));
  }

  @Test
  void applyFilters_withExplicitOperator_addsFilter() {
    QueryItem queryItem = createOrgUnitQueryItem();
    String[] filterParts = {"ou", "EQ", "ouA"};

    handler.applyFilters(queryItem, filterParts, "ou:EQ:ouA", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.EQ));
    assertThat(filters.get(0).getFilter(), is("ouA"));
  }

  @Test
  void applyFilters_withMultipleOperators_addsAllFilters() {
    QueryItem queryItem = createOrgUnitQueryItem();
    String[] filterParts = {"ou", "IN", "ouA;ouB", "NLIKE", "ouC"};

    handler.applyFilters(queryItem, filterParts, "ou:IN:ouA;ouB:NLIKE:ouC", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("ouA;ouB"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.NLIKE));
    assertThat(filters.get(1).getFilter(), is("ouC"));
  }

  @Test
  void applyFilters_withNoFilterPart_throwsException() {
    QueryItem queryItem = createOrgUnitQueryItem();
    String[] filterParts = {"ou"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "ou", null));
  }

  private QueryItem createOrgUnitQueryItem() {
    ProgramStage programStage = TestBase.createProgramStage('A', (Program) null);
    QueryItem queryItem =
        new QueryItem(new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME));
    queryItem.setProgramStage(programStage);
    return queryItem;
  }
}
