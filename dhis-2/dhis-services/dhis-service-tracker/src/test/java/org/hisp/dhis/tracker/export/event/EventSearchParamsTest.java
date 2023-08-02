/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Order;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventSearchParamsTest extends DhisConvenienceTest {

  private TrackedEntityAttribute tea1;

  private DataElement de1;

  @BeforeEach
  void setUp() {
    tea1 = createTrackedEntityAttribute('a');
    de1 = createDataElement('a');
  }

  @Test
  void shouldAddAttributeToOrderAndAttributesWhenOrderingByAttribute() {
    EventSearchParams params = new EventSearchParams();

    params.orderBy(tea1, SortDirection.DESC);

    assertEquals(List.of(new Order(tea1, SortDirection.DESC)), params.getOrder());
    assertEquals(Map.of(tea1, List.of()), params.getAttributes());
  }

  @Test
  void shouldKeepExistingAttributeFiltersWhenOrderingByAttribute() {
    EventSearchParams params = new EventSearchParams();

    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "summer day");
    params.addAttributeFilter(tea1, filter);

    assertEquals(Map.of(tea1, List.of(filter)), params.getAttributes());

    params.orderBy(tea1, SortDirection.DESC);

    assertEquals(Map.of(tea1, List.of(filter)), params.getAttributes());
  }

  @Test
  void shouldAddDataElementToOrderAndDataElementsWhenOrderingByDataElement() {
    EventSearchParams params = new EventSearchParams();

    params.orderBy(de1, SortDirection.ASC);

    assertEquals(List.of(new Order(de1, SortDirection.ASC)), params.getOrder());
    assertEquals(Map.of(de1, List.of()), params.getDataElements());
    assertFalse(params.hasDataElementFilter());
  }

  @Test
  void shouldKeepExistingDataElementFiltersWhenOrderingByDataElement() {
    EventSearchParams params = new EventSearchParams();

    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "summer day");
    params.addDataElementFilter(de1, filter);

    assertTrue(params.hasDataElementFilter());
    assertEquals(Map.of(de1, List.of(filter)), params.getDataElements());

    params.orderBy(de1, SortDirection.ASC);

    assertTrue(params.hasDataElementFilter());
    assertEquals(Map.of(de1, List.of(filter)), params.getDataElements());
  }
}
