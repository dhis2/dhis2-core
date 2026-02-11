/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;

import java.util.Set;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.UserDetails;

/**
 * Bundles the user's org unit scopes needed by ownership-based access control clauses. Resolves the
 * org unit selection mode at construction time so consumers don't need to re-derive which scope
 * applies.
 *
 * @param userId the user's database ID, used for temporary ownership predicates
 * @param scope the effective scope for OPEN/AUDITED programs, pre-resolved from the org unit mode
 *     (search scope when mode is not CAPTURE, capture scope when mode is CAPTURE)
 * @param captureScope the user's capture scope, used for PROTECTED/CLOSED programs
 */
public record OwnershipScope(
    long userId, Set<OrganisationUnit> scope, Set<OrganisationUnit> captureScope) {

  public static OwnershipScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      Set<OrganisationUnit> searchOrgUnits,
      Set<OrganisationUnit> captureOrgUnits) {
    Set<OrganisationUnit> scope = mode == CAPTURE ? captureOrgUnits : searchOrgUnits;
    return new OwnershipScope(user.getId(), scope, captureOrgUnits);
  }

  public Set<OrganisationUnit> forAccessLevel(AccessLevel accessLevel) {
    return switch (accessLevel) {
      case OPEN, AUDITED -> scope;
      case PROTECTED, CLOSED -> captureScope;
    };
  }
}
