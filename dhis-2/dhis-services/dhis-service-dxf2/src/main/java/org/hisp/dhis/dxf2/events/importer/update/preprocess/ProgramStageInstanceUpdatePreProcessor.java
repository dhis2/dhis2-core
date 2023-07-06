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
package org.hisp.dhis.dxf2.events.importer.update.preprocess;

import static org.hisp.dhis.event.EventStatus.ACTIVE;
import static org.hisp.dhis.event.EventStatus.COMPLETED;
import static org.hisp.dhis.event.EventStatus.SCHEDULE;
import static org.hisp.dhis.event.EventStatus.SKIPPED;
import static org.hisp.dhis.util.DateUtils.parseDate;

import java.util.Date;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventUtils;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author maikel arabori
 */
@Component
public class ProgramStageInstanceUpdatePreProcessor implements Processor {
  @Override
  public void process(final Event event, final WorkContext ctx) {
    final ProgramStageInstance programStageInstance =
        ctx.getProgramStageInstanceMap().get(event.getEvent());
    final OrganisationUnit organisationUnit = ctx.getOrganisationUnitMap().get(event.getUid());
    final CategoryOptionCombo categoryOptionCombo =
        ctx.getCategoryOptionComboMap().get(event.getUid());

    if (programStageInstance != null) {
      if (event.getEventDate() != null) {
        programStageInstance.setExecutionDate(parseDate(event.getEventDate()));
      }

      if (categoryOptionCombo != null) {
        programStageInstance.setAttributeOptionCombo(categoryOptionCombo);
      }

      final String storedBy =
          EventUtils.getValidUsername(event.getStoredBy(), ctx.getImportOptions());

      setStatus(programStageInstance, event, ctx);

      programStageInstance.setStoredBy(storedBy);

      if (organisationUnit != null) {
        programStageInstance.setOrganisationUnit(organisationUnit);
      }
      programStageInstance.setGeometry(event.getGeometry());

      if (programStageInstance.getProgramStage() != null
          && programStageInstance.getProgramStage().isEnableUserAssignment()) {
        programStageInstance.setAssignedUser(ctx.getAssignedUserMap().get(event.getUid()));
      }
    }
  }

  private void setStatus(
      ProgramStageInstance programStageInstance, final Event event, WorkContext ctx) {
    if (event.getStatus() == ACTIVE) {
      programStageInstance.setStatus(ACTIVE);
      programStageInstance.setCompletedBy(null);
      programStageInstance.setCompletedDate(null);
    } else if (programStageInstance.getStatus() != event.getStatus()
        && event.getStatus() == COMPLETED) {
      final String completedBy =
          EventUtils.getValidUsername(event.getCompletedBy(), ctx.getImportOptions());

      programStageInstance.setCompletedBy(completedBy);

      Date completedDate = new Date();

      if (event.getCompletedDate() != null) {
        completedDate = DateUtils.parseDate(event.getCompletedDate());
      }

      programStageInstance.setCompletedDate(completedDate);
      programStageInstance.setStatus(COMPLETED);
    } else if (event.getStatus() == SKIPPED) {
      programStageInstance.setStatus(SKIPPED);
    } else if (event.getStatus() == SCHEDULE) {
      programStageInstance.setStatus(SCHEDULE);
    }
  }
}
