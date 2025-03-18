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

import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Ameen Mohamed
 */
public interface TrackerOwnershipManager {
  String OWNERSHIP_ACCESS_DENIED = "OWNERSHIP_ACCESS_DENIED";

  String PROGRAM_ACCESS_CLOSED = "PROGRAM_ACCESS_CLOSED";

  String NO_READ_ACCESS_TO_ORG_UNIT = "User has no read access to organisation unit";

  /**
   * @param trackedEntity The tracked entity object
   * @param program The program object
   * @param orgUnit The org unit that has to become the owner
   */
  void transferOwnership(TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit)
      throws ForbiddenException;

  /**
   * Check whether the user has access (as owner or has temporarily broken the glass) to the tracked
   * entity - program combination.
   *
   * @param user The user with which access has to be checked for.
   * @param trackedEntity The tracked entity.
   * @param program The program.
   * @return true if the user has access, false otherwise.
   */
  boolean hasAccess(UserDetails user, TrackedEntity trackedEntity, Program program);

  boolean hasAccess(
      UserDetails user, String trackedEntity, OrganisationUnit organisationUnit, Program program);

  /**
   * Grant temporary ownership for a user for a specific tracked entity - program combination
   *
   * @param trackedEntity The tracked entity object
   * @param program The program object
   * @param user The user for which temporary access is granted.
   * @param reason The reason for requesting temporary ownership
   */
  void grantTemporaryOwnership(
      TrackedEntity trackedEntity, Program program, UserDetails user, String reason)
      throws ForbiddenException;

  /**
   * Ownership check can be skipped if the user is superuser or if the program type is without
   * registration.
   *
   * @return true if ownership check can be skipped.
   */
  boolean canSkipOwnershipCheck(UserDetails user, Program program);

  /**
   * Ownership check can be skipped if the user is superuser or if the program type is without
   * registration.
   *
   * @return true if ownership check can be skipped.
   */
  boolean canSkipOwnershipCheck(UserDetails user, ProgramType programType);

  /**
   * Checks whether the owner of the TE/program pair resides within the user search scope.
   *
   * @return true if the owner is in the search scope, false otherwise
   */
  boolean isOwnerInUserSearchScope(UserDetails user, TrackedEntity trackedEntity, Program program);
}
