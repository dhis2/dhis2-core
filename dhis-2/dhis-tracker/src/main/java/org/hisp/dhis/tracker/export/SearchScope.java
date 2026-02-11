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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.UserDetails;

/**
 * Bundles the user's org unit scopes and search context needed by ownership-based access control
 * clauses. Resolves the org unit selection mode at construction time so consumers don't need to
 * re-derive which scope applies.
 *
 * <p>An unrestricted scope (for super users or org unit mode ALL) produces no ownership SQL clause.
 *
 * @param userId the user's database ID, used for temporary ownership predicates
 * @param restricted whether ownership restrictions apply (false for super users or org unit mode
 *     ALL)
 * @param outsideCaptureScope whether the search extends beyond the user's capture scope (triggers
 *     max TE limit enforcement)
 * @param scope the effective scope for OPEN/AUDITED programs, pre-resolved from the org unit mode
 *     (search scope when mode is not CAPTURE, capture scope when mode is CAPTURE)
 * @param captureScope the user's capture scope, used for PROTECTED/CLOSED programs
 */
public record SearchScope(
    long userId,
    boolean restricted,
    boolean outsideCaptureScope,
    Set<OrganisationUnit> scope,
    Set<OrganisationUnit> captureScope) {

  /**
   * Creates a search scope by resolving the user's org unit scopes via the given resolver. Always
   * resolves the capture scope; skips the search scope fetch for superusers or org unit mode ALL
   * since no ownership SQL clause is emitted.
   *
   * @param orgUnitResolver resolves a set of org unit UIDs to their entities (typically {@code
   *     organisationUnitService::getOrganisationUnitsByUid})
   */
  public static SearchScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      boolean outsideCaptureScope,
      Function<Collection<String>, List<OrganisationUnit>> orgUnitResolver) {
    Set<OrganisationUnit> captureOrgUnits =
        Set.copyOf(orgUnitResolver.apply(user.getUserOrgUnitIds()));
    return of(user, mode, outsideCaptureScope, captureOrgUnits, orgUnitResolver);
  }

  /**
   * Creates a search scope using pre-resolved capture scope org units. Use this overload when the
   * caller has already resolved capture scope (e.g. for determining {@code outsideCaptureScope}).
   */
  public static SearchScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      boolean outsideCaptureScope,
      Set<OrganisationUnit> captureOrgUnits,
      Function<Collection<String>, List<OrganisationUnit>> orgUnitResolver) {
    if (mode == ALL || user.isSuper()) {
      return new SearchScope(0, false, false, Set.of(), captureOrgUnits);
    }
    Set<OrganisationUnit> searchOrgUnits =
        Set.copyOf(orgUnitResolver.apply(user.getUserEffectiveSearchOrgUnitIds()));
    Set<OrganisationUnit> scope = mode == CAPTURE ? captureOrgUnits : searchOrgUnits;
    return new SearchScope(user.getId(), true, outsideCaptureScope, scope, captureOrgUnits);
  }

  public Set<OrganisationUnit> forAccessLevel(AccessLevel accessLevel) {
    return switch (accessLevel) {
      case OPEN, AUDITED -> scope;
      case PROTECTED, CLOSED -> captureScope;
    };
  }
}
