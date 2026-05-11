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
import org.hisp.dhis.category.CategoryOptionCombo;
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
  List<ErrorMessage> canCreate(UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks data write access to the TET and ownership of the tracked entity across programs for
   * which the user has data write access. When {@code orgUnit} differs from the stored tracked
   * entity's org unit, capture scope access to it is also required.
   *
   * @param user the user whose access is being validated.
   * @param trackedEntity the stored tracked entity to update.
   * @param orgUnit the org unit the caller intends to move the entity to; if no org unit change is
   *     intended, pass the entity's existing org unit.
   * @return No errors if the user has all required access rights to update the tracked entity.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user, TrackedEntity trackedEntity, OrganisationUnit orgUnit);

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
   * Checks data write access to the program, data read access to the TET, ownership, and data write
   * access to the stored enrollment's category option combo. When {@code orgUnit} differs from the
   * stored enrollment's org unit, capture scope access to it is also required. When {@code
   * categoryOptionCombo} differs from the stored enrollment's category option combo, data write
   * access to it is also required.
   *
   * @param user the user whose access is being validated.
   * @param enrollment the stored enrollment to update.
   * @param orgUnit the org unit the caller intends to move the entity to; if no org unit change is
   *     intended, pass the entity's existing org unit.
   * @param categoryOptionCombo the category option combo the caller intends to set on the
   *     enrollment; if no category option combo change is intended, pass the entity's existing
   *     category option combo.
   * @return No errors if the user has all required access rights to update the enrollment.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user,
      Enrollment enrollment,
      OrganisationUnit orgUnit,
      CategoryOptionCombo categoryOptionCombo);

  /** Like {@link #canCreate(UserDetails, Enrollment)}. */
  List<ErrorMessage> canDelete(UserDetails user, Enrollment enrollment);

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
   * ownership, and data write access to the stored event's category option combo. When {@code
   * orgUnit} differs from the stored event's org unit, capture scope access to it is also required.
   * When {@code attributeOptionCombo} differs from the stored event's category option combo, data
   * write access to it is also required.
   *
   * @param user the user whose access is being validated.
   * @param event the stored event to update.
   * @param orgUnit the org unit the caller intends to move the entity to; if no org unit change is
   *     intended, pass the entity's existing org unit.
   * @param attributeOptionCombo the category option combo the caller intends to set on the event;
   *     if no category option combo change is intended, pass the entity's existing category option
   *     combo.
   * @return No errors if the user has all required access rights to update the event.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user,
      TrackerEvent event,
      OrganisationUnit orgUnit,
      CategoryOptionCombo attributeOptionCombo);

  /** Like {@link #canCreate(UserDetails, TrackerEvent)}. */
  List<ErrorMessage> canDelete(UserDetails user, TrackerEvent event);

  /**
   * Checks org unit scope access, data read access to the program, and data read access to the
   * category option combo.
   *
   * @return No errors if the user has org unit scope access, data read access to the program, and
   *     data read access to the category option combo.
   */
  List<ErrorMessage> canRead(UserDetails user, SingleEvent event);

  /**
   * Checks capture scope access, data write access to the program, and data write access to the
   * category option combo.
   *
   * @return No errors if the user has all required access rights to create the event.
   */
  List<ErrorMessage> canCreate(UserDetails user, SingleEvent event);

  /**
   * Checks capture scope access, data write access to the program, and data write access to the
   * stored event's category option combo. When {@code orgUnit} differs from the stored event's org
   * unit, capture scope access to it is also required. When {@code categoryOptionCombo} differs
   * from the stored event's category option combo, data write access to it is also required.
   *
   * @param user the user whose access is being validated.
   * @param event the stored event to update.
   * @param orgUnit the org unit the caller intends to move the entity to; if no org unit change is
   *     intended, pass the entity's existing org unit.
   * @param categoryOptionCombo the category option combo the caller intends to set on the event; if
   *     no category option combo change is intended, pass the entity's existing category option
   *     combo.
   * @return No errors if the user has all required access rights to update the event.
   */
  List<ErrorMessage> canUpdate(
      UserDetails user,
      SingleEvent event,
      OrganisationUnit orgUnit,
      CategoryOptionCombo categoryOptionCombo);

  /** Like {@link #canCreate(UserDetails, SingleEvent)}. */
  List<ErrorMessage> canDelete(UserDetails user, SingleEvent event);

  /**
   * Checks data read access to the relationship type, and data read access to both the {@code from}
   * and {@code to} items.
   *
   * @return No errors if the user has data read access to the relationship type and to both items.
   */
  List<ErrorMessage> canRead(UserDetails user, Relationship relationship);

  /**
   * Checks data write access to the relationship type and data write access to the {@code from}
   * item. For non-bidirectional relationship types, also checks data read access to the {@code to}
   * item. For bidirectional relationship types, also checks data write access to the {@code to}
   * item.
   *
   * @return No errors if the user has all required access rights to create the relationship.
   */
  List<ErrorMessage> canCreate(UserDetails user, Relationship relationship);

  /**
   * Checks data write access to the relationship type and data write access to the {@code from}
   * item. For bidirectional relationship types, also checks data write access to the {@code to}
   * item.
   *
   * @return No errors if the user has all required access rights to delete the relationship.
   */
  List<ErrorMessage> canDelete(UserDetails user, Relationship relationship);
}
