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
package org.hisp.dhis.trackedentity;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerAccessManager {
  List<String> canRead(UserDetails user, TrackedEntity trackedEntity);

  List<String> canWrite(UserDetails user, TrackedEntity trackedEntity);

  /**
   * Check if a user has data access to the supplied program and tracked entity type. It also
   * validates user ownership to the TE/program pair.
   *
   * @return empty list if access is granted, list with errors otherwise
   */
  List<String> canRead(
      UserDetails user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck);

  /**
   * Check if a user has data access to the supplied program and tracked entity type. Does not
   * validate user ownership to the TE/program pair.
   *
   * @return empty list if access is granted, list with errors otherwise
   */
  List<String> canReadProgramAndTrackedEntityType(
      UserDetails user, TrackedEntity trackedEntity, Program program);

  List<String> canRead(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canCreate(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canUpdate(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canDelete(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, Event event, boolean skipOwnershipCheck);

  List<String> canCreate(UserDetails user, Event event, boolean skipOwnershipCheck);

  List<String> canUpdate(UserDetails user, Event event, boolean skipOwnershipCheck);

  List<String> canDelete(UserDetails user, Event event, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, Relationship relationship);

  List<String> canWrite(UserDetails user, Relationship relationship);

  /**
   * Checks the sharing read access to EventDataValue
   *
   * @param user User validated for write access
   * @param event Event under which the EventDataValue belongs
   * @param dataElement DataElement of EventDataValue
   * @return Empty list if read access allowed, list of errors otherwise.
   */
  List<String> canRead(
      UserDetails user, Event event, DataElement dataElement, boolean skipOwnershipCheck);

  /**
   * Checks the sharing write access to EventDataValue
   *
   * @param user User validated for write access
   * @param event Event under which the EventDataValue belongs
   * @param dataElement DataElement of EventDataValue
   * @return Empty list if write access allowed, list of errors otherwise.
   */
  List<String> canWrite(
      UserDetails user, Event event, DataElement dataElement, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, CategoryOptionCombo categoryOptionCombo);

  List<String> canWrite(UserDetails user, CategoryOptionCombo categoryOptionCombo);

  /**
   * Checks if user has access to organisation unit under defined tracker program protection level
   *
   * @param user the user to check access for
   * @param program program to check against protection level
   * @param orgUnit the org unit to be checked under user's scope and program protection
   * @return true if user has access to the org unit under the mentioned program context, otherwise
   *     return false
   */
  boolean canAccess(UserDetails user, Program program, OrganisationUnit orgUnit);

  /**
   * Checks if the user has access to the TE org unit owner in the specified program
   *
   * @return an error if the TE is not accessible, null otherwise
   */
  String canAccessProgramOwner(
      UserDetails user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck);
}
