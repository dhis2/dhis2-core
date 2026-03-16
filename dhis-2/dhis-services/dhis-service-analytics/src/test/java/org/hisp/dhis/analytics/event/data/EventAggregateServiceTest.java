/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.List;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

class EventAggregateServiceTest {

  private final EventAggregateService service =
      new EventAggregateService(null, null, null, null, null, null, null, null, null, null, null);

  @Test
  void shouldUseEnrollmentDateHeaderForStaticPeriodDateField() throws Exception {
    Program program = new Program();
    program.setEnrollmentDateLabel("Start of treatment date");

    GridHeader header =
        invokeAddDimensionHeaders(singleDateFieldParams("ENROLLMENT_DATE", program));

    assertEquals("enrollmentdate", header.getName());
    assertEquals("Start of treatment date", header.getColumn());
  }

  @Test
  void shouldUseIncidentDateHeaderForStaticPeriodDateField() throws Exception {
    Program program = new Program();
    program.setIncidentDateLabel("Incident date custom");

    GridHeader header = invokeAddDimensionHeaders(singleDateFieldParams("INCIDENT_DATE", program));

    assertEquals("incidentdate", header.getName());
    assertEquals("Incident date custom", header.getColumn());
  }

  @Test
  void shouldKeepPeHeaderForDefaultPeriod() throws Exception {
    GridHeader header = invokeAddDimensionHeaders(defaultPeriodParams());

    assertEquals("pe", header.getName());
    assertEquals("Period", header.getColumn());
  }

  @Test
  void shouldFallbackToPeHeaderWhenPeriodDateFieldsAreMixed() throws Exception {
    GridHeader header = invokeAddDimensionHeaders(mixedPeriodDateFieldsParams());

    assertEquals("pe", header.getName());
    assertEquals("Period", header.getColumn());
  }

  @Test
  void shouldKeepValueAndEnrollmentOuHeadersUnchanged() throws Exception {
    EventQueryParams params =
        new EventQueryParams.Builder(defaultPeriodParams())
            .withEnrollmentOuDimension(List.<DimensionalItemObject>of(createOrganisationUnit('A')))
            .build();

    Grid grid = new ListGrid();
    invokePrivate("addHeaders", EventQueryParams.class, Grid.class, params, grid);

    List<GridHeader> headers = grid.getHeaders();
    assertEquals("pe", headers.get(0).getName());
    assertEquals(ColumnHeader.ENROLLMENT_OU.getItem(), headers.get(1).getName());
    assertEquals(VALUE_ID, headers.get(2).getName());
  }

  private GridHeader invokeAddDimensionHeaders(EventQueryParams params) throws Exception {
    Grid grid = new ListGrid();
    invokePrivate("addDimensionHeaders", EventQueryParams.class, Grid.class, params, grid);
    return grid.getHeaders().get(0);
  }

  private void invokePrivate(
      String methodName, Class<?> arg0Type, Class<?> arg1Type, Object arg0, Object arg1)
      throws Exception {
    Method method = EventAggregateService.class.getDeclaredMethod(methodName, arg0Type, arg1Type);
    method.setAccessible(true);
    method.invoke(service, arg0, arg1);
  }

  private EventQueryParams singleDateFieldParams(String dateField, Program program) {
    PeriodDimension period = PeriodDimension.of("2021").setDateField(dateField);
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, "Period", List.of(period));

    return new EventQueryParams.Builder()
        .addDimension(periodDimension)
        .withProgram(program)
        .build();
  }

  private EventQueryParams defaultPeriodParams() {
    PeriodDimension period = PeriodDimension.of("2021");
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, "Period", List.of(period));

    return new EventQueryParams.Builder().addDimension(periodDimension).build();
  }

  private EventQueryParams mixedPeriodDateFieldsParams() {
    PeriodDimension enrollmentDatePeriod =
        PeriodDimension.of("2021").setDateField("ENROLLMENT_DATE");
    PeriodDimension defaultPeriod = PeriodDimension.of("2022");
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(
            PERIOD_DIM_ID, PERIOD, "Period", List.of(enrollmentDatePeriod, defaultPeriod));

    return new EventQueryParams.Builder().addDimension(periodDimension).build();
  }
}
