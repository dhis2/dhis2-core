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
package org.hisp.dhis.tracker.imports.converter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service
public class EventTrackerConverterService
    implements TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event> {
  private final NotesConverterService notesConverterService;

  @Override
  public Event from(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event event, UserDetails user) {
    Event result = preheat.getEvent(event.getEvent());
    ProgramStage programStage = preheat.getProgramStage(event.getProgramStage());
    Program program = preheat.getProgram(event.getProgram());
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(event.getOrgUnit());

    Date now = new Date();

    if (isNewEntity(result)) {
      result = new Event();
      result.setUid(!StringUtils.isEmpty(event.getEvent()) ? event.getEvent() : event.getUid());
      result.setCreated(now);
      result.setStoredBy(event.getStoredBy());
      result.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }
    result.setCreatedAtClient(DateUtils.fromInstant(event.getCreatedAtClient()));
    result.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    result.setLastUpdated(now);
    result.setDeleted(false);
    result.setLastUpdatedAtClient(DateUtils.fromInstant(event.getUpdatedAtClient()));
    result.setEnrollment(getEnrollment(preheat, event.getEnrollment(), program));
    result.setProgramStage(programStage);
    result.setOrganisationUnit(organisationUnit);
    result.setOccurredDate(DateUtils.fromInstant(event.getOccurredAt()));
    result.setScheduledDate(DateUtils.fromInstant(event.getScheduledAt()));

    if (event.getAttributeOptionCombo().isNotBlank()) {
      result.setAttributeOptionCombo(
          preheat.getCategoryOptionCombo(event.getAttributeOptionCombo()));
    } else {
      result.setAttributeOptionCombo(preheat.getDefault(CategoryOptionCombo.class));
    }

    result.setGeometry(event.getGeometry());

    EventStatus currentStatus = event.getStatus();
    EventStatus previousStatus = result.getStatus();

    if (currentStatus != previousStatus && currentStatus == EventStatus.COMPLETED) {
      result.setCompletedDate(now);
      result.setCompletedBy(user.getUsername());
    }

    if (currentStatus != EventStatus.COMPLETED) {
      result.setCompletedDate(null);
      result.setCompletedBy(null);
    }

    result.setStatus(currentStatus);

    if (Boolean.TRUE.equals(programStage.isEnableUserAssignment())
        && event.getAssignedUser() != null
        && !event.getAssignedUser().isEmpty()) {
      Optional<org.hisp.dhis.user.User> assignedUser =
          preheat.getUserByUsername(event.getAssignedUser().getUsername());
      assignedUser.ifPresent(result::setAssignedUser);
    }

    if (program.isRegistration()
        && result.getScheduledDate() == null
        && result.getOccurredDate() != null) {
      result.setScheduledDate(result.getOccurredDate());
    }

    for (DataValue dataValue : event.getDataValues()) {
      EventDataValue eventDataValue = new EventDataValue();
      eventDataValue.setValue(dataValue.getValue());
      eventDataValue.setCreated(DateUtils.fromInstant(dataValue.getCreatedAt()));
      eventDataValue.setLastUpdated(now);
      eventDataValue.setProvidedElsewhere(dataValue.isProvidedElsewhere());
      // ensure dataElement is referred to by UID as multiple
      // dataElementIdSchemes are supported
      DataElement dataElement = preheat.getDataElement(dataValue.getDataElement());
      eventDataValue.setDataElement(dataElement.getUid());
      eventDataValue.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
      eventDataValue.setCreatedByUserInfo(UserInfoSnapshot.from(user));

      result.getEventDataValues().add(eventDataValue);
    }

    if (isNotEmpty(event.getNotes())) {
      result
          .getNotes()
          .addAll(
              event.getNotes().stream()
                  .map(note -> notesConverterService.from(preheat, note, user))
                  .collect(Collectors.toSet()));
    }

    return result;
  }

  private Enrollment getEnrollment(TrackerPreheat preheat, String enrollment, Program program) {
    if (ProgramType.WITH_REGISTRATION == program.getProgramType()) {
      return preheat.getEnrollment(enrollment);
    }

    if (ProgramType.WITHOUT_REGISTRATION == program.getProgramType()) {
      return preheat.getEnrollmentsWithoutRegistration(program.getUid());
    }

    // no valid enrollment given and program not single event, just return
    // null
    return null;
  }
}
