/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.organisationunit;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadParameterException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrgTree.OrgTreeEntry;
import org.hisp.dhis.organisationunit.OrgTreeStore.StreamEntriesParams;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultOrgTreeService implements OrgTreeService {

  private final OrgTreeStore store;

  @Nonnull
  @Override
  @NonTransactional
  public OrgTreeParams decode(@Nonnull OrgTreeParams.RootInput params) throws BadRequestException {
    List<UID> roots = params.roots();
    if (roots == null || roots.isEmpty()) roots = currentUserOrgUnitSearch().toList();
    List<UID> groups = params.groups() == null ? List.of() : params.groups();
    String cursor = params.pageCursor();
    try {
      return new OrgTreeParams(
          params.page() == null ? 1 : params.page(),
          params.pageSize() == null ? 50 : params.pageSize(),
          cursor == null ? List.of() : Stream.of(cursor.split(",")).map(OrgUnitPath::of).toList(),
          true,
          decodeLocale(params.locale()),
          roots,
          groups,
          params.program(),
          params.dataset(),
          params.level(),
          params.currentlyOpen(),
          params.search(),
          Boolean.TRUE.equals(params.shortName()),
          params.depth());
    } catch (BadParameterException ex) {
      throw ex.toChecked();
    }
  }

  @Nonnull
  @Override
  @NonTransactional
  public OrgTreeParams decode(@Nonnull UID parent, @Nonnull OrgTreeParams.ChildrenInput params)
      throws BadRequestException {
    String cursor = params.pageCursor();
    try {
      return new OrgTreeParams(
          params.page() == null ? 1 : params.page(),
          params.pageSize() == null ? 51 : params.pageSize() + 1,
          cursor == null ? List.of() : Stream.of(cursor.split(",")).map(OrgUnitPath::of).toList(),
          false,
          decodeLocale(params.locale()),
          List.of(parent),
          List.of(),
          null,
          null,
          null,
          null,
          null,
          Boolean.TRUE.equals(params.shortName()),
          1);
    } catch (BadParameterException ex) {
      throw ex.toChecked();
    }
  }

  private static Stream<UID> currentUserOrgUnitSearch() {
    if (!CurrentUserUtil.hasCurrentUser()) return Stream.empty();
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    return currentUser.getUserEffectiveSearchOrgUnitIds().stream().map(UID::of);
  }

  private static Locale decodeLocale(Locale locale) {
    if (locale == null) return UserSettings.getCurrentSettings().getUserDbLocale();
    return locale;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public OrgTree query(@Nonnull OrgTreeParams params) throws ForbiddenException {
    checkUserAccess(params);
    List<OrgUnitPath> matches = store.queryPageMatches(params);
    Locale locale = params.locale();
    String property = params.translationPropertyKey();
    StreamEntriesParams matchesParams =
        new StreamEntriesParams(locale, property, false, matches.stream().map(OrgUnitPath::uid));
    Stream<OrgTreeEntry> entries = store.streamEntries(matchesParams);
    if (OrgTree.isHierarchical(matches))
      entries = OrgTree.sortedHierarchical(entries.toList()).stream();

    // handle ancestors
    Stream<OrgTree.OrgTreeEntry> ancestors = Stream.empty();
    if (params.ancestors()) {
      StreamEntriesParams ancestorsParams =
          new StreamEntriesParams(
              locale,
              property,
              true,
              OrgUnitPath.ofMissingAncestors(matches, params.roots()).stream()
                  .map(OrgUnitPath::uid));
      ancestors = store.streamEntries(ancestorsParams);
    } else {
      entries = entries.skip(1); // skip the parent
    }

    // pager
    int total = -1;
    int pageSize = params.pageSize();
    if (!params.ancestors()) pageSize--;
    int page = params.page();
    if (matches.size() < pageSize) total = matches.size() + ((page - 1) * pageSize);
    // only count if we don't already know from a partial page
    if (total < 0) total = store.countTotalMatches(params);
    if (!params.ancestors()) total--; // sub the parent
    List<OrgUnitPath> cursors = new ArrayList<>(params.pageCursors());
    int pageCount = OrgTree.Pager.pageCount(total, pageSize);
    if (page < pageCount) cursors.add(matches.get(matches.size() - 1));
    OrgTree.Pager pager = new OrgTree.Pager(page, pageSize, total, cursors);
    return new OrgTree(pager, entries, ancestors);
  }

  private void checkUserAccess(@Nonnull OrgTreeParams params) throws ForbiddenException {
    Set<UID> roots = currentUserOrgUnitSearch().collect(toSet());
    if (roots.isEmpty()) {
      boolean access =
          CurrentUserUtil.hasCurrentUser() && CurrentUserUtil.getCurrentUserDetails().isSuper();
      if (access) return;
      throw new ForbiddenException(ErrorCode.E3054, null, params.roots());
    }
    if (!roots.containsAll(params.roots())) {
      List<UID> notInHierarchy = store.queryNotInRoots(params.roots(), roots);
      if (!notInHierarchy.isEmpty()) {
        UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
        throw new ForbiddenException(ErrorCode.E3054, currentUser.getUid(), notInHierarchy);
      }
    }
  }
}
