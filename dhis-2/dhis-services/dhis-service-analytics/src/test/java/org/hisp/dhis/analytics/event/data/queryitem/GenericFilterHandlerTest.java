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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenericFilterHandlerTest {

  private GenericFilterHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GenericFilterHandler();
  }

  @Test
  void supports_alwaysReturnsTrue() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);

    assertTrue(handler.supports(queryItem));
  }

  @Test
  void applyFilters_withSingleOperatorValuePair_addsFilter() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "EQ", "value1"};

    handler.applyFilters(queryItem, filterParts, "deA:EQ:value1", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.EQ));
    assertThat(filters.get(0).getFilter(), is("value1"));
  }

  @Test
  void applyFilters_withMultipleOperatorValuePairs_addsAllFilters() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "GT", "5", "LT", "10"};

    handler.applyFilters(queryItem, filterParts, "deA:GT:5:LT:10", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(2));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.GT));
    assertThat(filters.get(0).getFilter(), is("5"));
    assertThat(filters.get(1).getOperator(), is(QueryOperator.LT));
    assertThat(filters.get(1).getFilter(), is("10"));
  }

  @Test
  void applyFilters_withINOperator_addsINFilter() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "IN", "val1;val2;val3"};

    handler.applyFilters(queryItem, filterParts, "deA:IN:val1;val2;val3", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.IN));
    assertThat(filters.get(0).getFilter(), is("val1;val2;val3"));
  }

  @Test
  void applyFilters_withLikeOperator_addsLikeFilter() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "LIKE", "pattern"};

    handler.applyFilters(queryItem, filterParts, "deA:LIKE:pattern", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getOperator(), is(QueryOperator.LIKE));
    assertThat(filters.get(0).getFilter(), is("pattern"));
  }

  @Test
  void applyFilters_withTimeDataElement_convertsDotToColon() {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.TIME);
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "EQ", "10.30"};

    handler.applyFilters(queryItem, filterParts, "deA:EQ:10.30", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(1));
    assertThat(filters.get(0).getFilter(), is("10:30"));
  }

  @Test
  void applyFilters_withEvenLengthArray_throwsException() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA", "EQ"};

    assertThrows(
        IllegalQueryException.class,
        () -> handler.applyFilters(queryItem, filterParts, "deA:EQ", null));
  }

  @Test
  void applyFilters_withNoFilters_doesNotAddFilters() {
    DataElement dataElement = createDataElement('A');
    QueryItem queryItem = new QueryItem(dataElement);
    String[] filterParts = {"deA"};

    handler.applyFilters(queryItem, filterParts, "deA", null);

    List<QueryFilter> filters = queryItem.getFilters();
    assertThat(filters, hasSize(0));
  }
}
