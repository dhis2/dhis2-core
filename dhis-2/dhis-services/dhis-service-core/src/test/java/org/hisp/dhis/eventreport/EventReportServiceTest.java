/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.eventreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.EventDataType;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class EventReportServiceTest extends DhisSpringTest {

  @Autowired private EventReportService eventReportService;

  @Autowired private ProgramService programService;

  private Program prA;

  @Override
  public void setUpTest() {
    prA = createProgram('A', null, null);
    programService.addProgram(prA);
  }

  @Test
  void testSaveGet() {
    EventReport erA = new EventReport("erA");
    erA.setProgram(prA);
    erA.setDataType(EventDataType.AGGREGATED_VALUES);
    erA.setType(EventVisualizationType.PIVOT_TABLE);
    EventReport erB = new EventReport("erB");
    erB.setProgram(prA);
    erB.setDataType(EventDataType.AGGREGATED_VALUES);
    erB.setType(EventVisualizationType.PIVOT_TABLE);
    EventReport erC = new EventReport("erC");
    erC.setProgram(prA);
    erC.setDataType(EventDataType.AGGREGATED_VALUES);
    erC.setType(EventVisualizationType.PIVOT_TABLE);
    long idA = eventReportService.saveEventReport(erA);
    long idB = eventReportService.saveEventReport(erB);
    long idC = eventReportService.saveEventReport(erC);
    assertEquals("erA", eventReportService.getEventReport(idA).getName());
    assertEquals("erB", eventReportService.getEventReport(idB).getName());
    assertEquals("erC", eventReportService.getEventReport(idC).getName());
  }

  @Test
  void testDelete() {
    EventReport erA = new EventReport("erA");
    erA.setProgram(prA);
    erA.setDataType(EventDataType.AGGREGATED_VALUES);
    erA.setType(EventVisualizationType.PIVOT_TABLE);
    EventReport erB = new EventReport("erB");
    erB.setProgram(prA);
    erB.setDataType(EventDataType.AGGREGATED_VALUES);
    erB.setType(EventVisualizationType.PIVOT_TABLE);
    EventReport erC = new EventReport("erC");
    erC.setProgram(prA);
    erC.setDataType(EventDataType.AGGREGATED_VALUES);
    erC.setType(EventVisualizationType.PIVOT_TABLE);
    long idA = eventReportService.saveEventReport(erA);
    long idB = eventReportService.saveEventReport(erB);
    long idC = eventReportService.saveEventReport(erC);
    assertNotNull(eventReportService.getEventReport(idA));
    assertNotNull(eventReportService.getEventReport(idB));
    assertNotNull(eventReportService.getEventReport(idC));
    eventReportService.deleteEventReport(erA);
    assertNull(eventReportService.getEventReport(idA));
    assertNotNull(eventReportService.getEventReport(idB));
    assertNotNull(eventReportService.getEventReport(idC));
    eventReportService.deleteEventReport(erB);
    assertNull(eventReportService.getEventReport(idA));
    assertNull(eventReportService.getEventReport(idB));
    assertNotNull(eventReportService.getEventReport(idC));
    eventReportService.deleteEventReport(erC);
    assertNull(eventReportService.getEventReport(idA));
    assertNull(eventReportService.getEventReport(idB));
    assertNull(eventReportService.getEventReport(idC));
  }
}
