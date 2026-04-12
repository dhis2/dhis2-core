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
package org.hisp.dhis.tracker.acl;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerAccessManager {
  /**
   * Checks data read access to the TET and ownership of a tracked entity across programs for which
   * the user has data read access.
   *
   * @return No errors if the user has TET data read access and ownership in at least one program.
   */
  List<ErrorMessage> canRead(UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks capture scope and data write permissions to the TET of a given tracked entity.
   *
   * @return No errors if the user has capture scope access and write access to the TET.
   */
  List<ErrorMessage> canCreate(@Nonnull UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks data write access to the TET and ownership of the tracked entity across programs for
   * which the user has data write access. When the payload org unit differs from the stored tracked
   * entity's org unit, capture scope access to the new org unit is also required.
   *
   * @param user the user whose access is being validated.
   * @param trackedEntity the stored tracked entity to update.
   * @param payloadTrackedEntityOrgUnit the org unit from the update payload; {@code null} if
   *     unchanged.
   * @return No errors if the user has all required access rights to update the tracked entity.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user, TrackedEntity trackedEntity, OrganisationUnit payloadTrackedEntityOrgUnit);

  /**
   * Like {@link #canUpdate(UserDetails, TrackedEntity, OrganisationUnit)}, but also requires
   * capture scope access.
   */
  List<ErrorMessage> canDelete(UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks data read access to the program and TET, and ownership of the enrollment.
   *
   * @return No errors if the user has data read access to the program and TET, and has ownership.
   */
  List<ErrorMessage> canRead(UserDetails user, Enrollment enrollment);

  /**
   * Checks data write access to the program, data read access to the TET, ownership, capture scope,
   * and category option combo write access for the enrollment.
   *
   * @return No errors if the user has all required access rights to create the enrollment.
   */
  List<ErrorMessage> canCreate(UserDetails user, Enrollment enrollment);

  /**
   * Checks data write access to the program, data read access to the TET, ownership, and category
   * option combo write access. When the payload org unit differs from the stored enrollment's org
   * unit, capture scope access to the new org unit is also required.
   *
   * @param user the user whose access is being validated.
   * @param enrollment the stored enrollment to update.
   * @param payloadEnrollmentOrgUnit the org unit from the update payload; {@code null} if
   *     unchanged.
   * @return No errors if the user has all required access rights to update the enrollment.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user, Enrollment enrollment, OrganisationUnit payloadEnrollmentOrgUnit);

  /**
   * Like {@link #canCreate(UserDetails, Enrollment)}, but also requires the {@code
   * F_ENROLLMENT_CASCADE_DELETE} authority when the enrollment has non-deleted events.
   *
   * @param hasNonDeletedEvents whether the enrollment has at least one non-deleted event.
   */
  List<ErrorMessage> canDelete(
      UserDetails user, Enrollment enrollment, boolean hasNonDeletedEvents);

  /**
   * Checks data read access to the program, program stage, and TET, ownership of the enrolled
   * tracked entity, and data read access to the category option combo.
   *
   * @return No errors if the user has data read access to the program, program stage, and TET, has
   *     ownership, and has data read access to the category option combo.
   */
  List<ErrorMessage> canRead(UserDetails user, TrackerEvent event);

  /**
   * Checks data write access to the program stage, data read access to the program and TET,
   * ownership, data write access to the category option combo, and org unit scope access. The org
   * unit is checked against the search scope if the event is creatable in search scope, otherwise
   * against the capture scope.
   *
   * @return No errors if the user has all required access rights to create the event.
   */
  List<ErrorMessage> canCreate(UserDetails user, TrackerEvent event);

  /**
   * Checks data write access to the program stage, data read access to the program and TET,
   * ownership, and data write access to the category option combo. When the payload org unit
   * differs from the stored event's org unit, capture scope access to the new org unit is also
   * required. When the event status is {@link EventStatus#COMPLETED} and the payload requests a
   * different status, the {@code F_UNCOMPLETE_EVENT} authority is required.
   *
   * @param user the user whose access is being validated.
   * @param event the stored event to update.
   * @param payloadEventOrgUnit the org unit from the update payload; {@code null} if unchanged.
   * @param payloadEventStatus the event status from the update payload; {@code null} if unchanged.
   * @return No errors if the user has all required access rights to update the event.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user,
      TrackerEvent event,
      OrganisationUnit payloadEventOrgUnit,
      EventStatus payloadEventStatus);

  /** Like {@link #canCreate(UserDetails, TrackerEvent)}. */
  List<ErrorMessage> canDelete(UserDetails user, TrackerEvent event);

  List<String> canRead(UserDetails user, SingleEvent event);

  List<String> canCreate(UserDetails user, SingleEvent event);

  List<String> canRead(UserDetails user, Relationship relationship);

  List<String> canCreate(UserDetails user, Relationship relationship);

  List<String> canDelete(UserDetails user, @Nonnull Relationship relationship);

  /**
   * Checks the sharing read access to EventDataValue
   *
   * @param user User validated for write access
   * @param event SingleEvent under which the EventDataValue belongs
   * @param dataElement DataElement of EventDataValue
   * @return Empty list if read access allowed, list of errors otherwise.
   */
  List<String> canRead(UserDetails user, SingleEvent event, DataElement dataElement);
}
