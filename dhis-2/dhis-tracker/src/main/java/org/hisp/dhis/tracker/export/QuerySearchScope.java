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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.UserDetails;

/**
 * Bundles the user's org unit scopes and search context needed by ownership-based access control
 * clauses. Resolves the org unit selection mode at construction time so consumers don't need to
 * re-derive which scope applies.
 *
 * <p>An unrestricted scope (for super users or org unit mode ALL) produces no ownership SQL clause.
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@ToString
public final class QuerySearchScope {

  /** The user's database ID, used for temporary ownership predicates. */
  private final long userId;

  /** Whether ownership restrictions apply (false for super users or org unit mode ALL). */
  private final boolean restricted;

  /**
   * Whether the search extends beyond the user's capture scope (triggers max TE limit enforcement).
   */
  private final boolean outsideCaptureScope;

  /**
   * The effective scope for OPEN/AUDITED programs, pre-resolved from the org unit mode (search
   * scope when mode is not CAPTURE, capture scope when mode is CAPTURE).
   */
  private final Set<OrganisationUnit> scope;

  /** The user's capture scope, used for PROTECTED/CLOSED programs. */
  private final Set<OrganisationUnit> captureScope;

  /**
   * Creates a search scope by resolving the user's org unit scopes via the given resolver. Use this
   * overload for endpoints that don't enforce the max TE limit (enrollments, events).
   *
   * @param orgUnitResolver resolves a set of org unit UIDs to their entities (typically {@code
   *     organisationUnitService::getOrganisationUnitsByUid})
   */
  public static QuerySearchScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      Function<Collection<String>, List<OrganisationUnit>> orgUnitResolver) {
    return of(user, mode, false, orgUnitResolver);
  }

  /**
   * Creates a search scope by resolving the user's org unit scopes via the given resolver. Skips
   * all org unit fetching for superusers or org unit mode ALL since no ownership SQL clause is
   * emitted.
   *
   * @param orgUnitResolver resolves a set of org unit UIDs to their entities (typically {@code
   *     organisationUnitService::getOrganisationUnitsByUid})
   */
  public static QuerySearchScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      boolean outsideCaptureScope,
      Function<Collection<String>, List<OrganisationUnit>> orgUnitResolver) {
    if (mode == ALL || user.isSuper()) {
      return new QuerySearchScope(0, false, false, Set.of(), Set.of());
    }
    Set<OrganisationUnit> captureOrgUnits =
        Set.copyOf(orgUnitResolver.apply(user.getUserOrgUnitIds()));
    return of(user, mode, outsideCaptureScope, captureOrgUnits, orgUnitResolver);
  }

  /**
   * Creates a search scope using pre-resolved capture scope org units. Use this overload when the
   * caller has already resolved capture scope (e.g. for determining {@code outsideCaptureScope}).
   */
  public static QuerySearchScope of(
      UserDetails user,
      OrganisationUnitSelectionMode mode,
      boolean outsideCaptureScope,
      Set<OrganisationUnit> captureOrgUnits,
      Function<Collection<String>, List<OrganisationUnit>> orgUnitResolver) {
    if (mode == ALL || user.isSuper()) {
      return new QuerySearchScope(0, false, false, Set.of(), captureOrgUnits);
    }
    Set<OrganisationUnit> searchOrgUnits =
        Set.copyOf(orgUnitResolver.apply(user.getUserEffectiveSearchOrgUnitIds()));
    Set<OrganisationUnit> resolvedScope = mode == CAPTURE ? captureOrgUnits : searchOrgUnits;
    return new QuerySearchScope(
        user.getId(), true, outsideCaptureScope, resolvedScope, captureOrgUnits);
  }

  public Set<OrganisationUnit> forAccessLevel(org.hisp.dhis.common.AccessLevel accessLevel) {
    return switch (accessLevel) {
      case OPEN, AUDITED -> scope;
      case PROTECTED, CLOSED -> captureScope;
    };
  }
}
