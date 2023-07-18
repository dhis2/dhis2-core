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

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.GenericAnalyticalObjectDeletionHandler;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component
public class EventReportDeletionHandler
    extends GenericAnalyticalObjectDeletionHandler<EventReport, EventReportService> {
  public EventReportDeletionHandler(EventReportService eventReportService) {
    super(new DeletionVeto(EventReport.class), eventReportService);
  }

  @Override
  protected void registerHandler() {
    // generic
    whenDeleting(Period.class, this::deletePeriod);
    whenVetoing(Period.class, this::allowDeletePeriod);
    whenDeleting(OrganisationUnit.class, this::deleteOrganisationUnit);
    whenDeleting(OrganisationUnitGroup.class, this::deleteOrganisationUnitGroup);
    whenDeleting(OrganisationUnitGroupSet.class, this::deleteOrganisationUnitGroupSet);
    // special
    whenDeleting(DataElement.class, this::deleteDataElementSpecial);
    whenDeleting(ProgramStage.class, this::deleteProgramStage);
    whenDeleting(Program.class, this::deleteProgram);
  }

  private void deleteDataElementSpecial(DataElement dataElement) {
    List<EventReport> eventReports = service.getAnalyticalObjectsByDataDimension(dataElement);

    for (EventReport report : eventReports) {
      report
          .getDataElementDimensions()
          .removeIf(
              trackedEntityDataElementDimension ->
                  trackedEntityDataElementDimension.getDataElement().equals(dataElement));

      service.update(report);
    }
  }

  private void deleteProgramStage(ProgramStage programStage) {
    Collection<EventReport> charts = service.getAllEventReports();

    for (EventReport chart : charts) {
      if (chart.getProgramStage().equals(programStage)) {
        service.deleteEventReport(chart);
      }
    }
  }

  private void deleteProgram(Program program) {
    Collection<EventReport> charts = service.getAllEventReports();

    for (EventReport chart : charts) {
      if (chart.getProgram().equals(program)) {
        service.deleteEventReport(chart);
      }
    }
  }
}
