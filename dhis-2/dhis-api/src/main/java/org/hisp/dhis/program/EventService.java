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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.Map;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Abyot Asalefew
 */
public interface EventService {
  String ID = EventService.class.getName();

  /**
   * Adds an {@link Event}
   *
   * @param event The Event to add.
   * @return A generated unique id of the added {@link Event}.
   */
  long addEvent(Event event);

  /** Soft deletes an {@link Event}. */
  void deleteEvent(Event event);

  /**
   * Checks whether an {@link Event} with the given identifier exists. Doesn't take into account the
   * deleted values.
   *
   * @param uid the identifier.
   */
  boolean eventExists(String uid);

  /**
   * Checks whether an {@link Event} with the given identifier exists. Takes into accound also the
   * deleted values.
   *
   * @param uid the identifier.
   */
  boolean eventExistsIncludingDeleted(String uid);

  /**
   * Returns the {@link Event} with the given UID.
   *
   * @param uid the UID.
   * @return the Event with the given UID, or null if no match.
   */
  Event getEvent(String uid);

  /**
   * Gets the number of events added since the given number of days.
   *
   * @param days number of days.
   * @return the number of events.
   */
  long getEventCount(int days);

  /**
   * Creates and saves an event.
   *
   * @param enrollment the Enrollment.
   * @param programStage the ProgramStage.
   * @param enrollmentDate the enrollment date.
   * @param incidentDate date of the incident.
   * @param organisationUnit the OrganisationUnit where the event took place.
   * @return Event the Event which was created.
   */
  Event createEvent(
      Enrollment enrollment,
      ProgramStage programStage,
      Date enrollmentDate,
      Date incidentDate,
      OrganisationUnit organisationUnit);

  /**
   * Validates EventDataValues, handles files for File EventDataValues and creates audit logs for
   * the upcoming create/save changes. DOES PERSIST the changes to the Event object.
   *
   * @param event the Event that EventDataValues belong to
   * @param dataElementEventDataValueMap the map of DataElements and related EventDataValues to
   *     update
   */
  void saveEventDataValuesAndSaveEvent(
      Event event, Map<DataElement, EventDataValue> dataElementEventDataValueMap);
}
