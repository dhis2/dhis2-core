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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrgTree.OrgTreeEntry;
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
  @Transactional(readOnly = true)
  public OrgTreeParams decode(@Nonnull OrgTreeParams.Input params) {
    List<UID> roots = params.roots();
    if (roots == null) roots = List.of();
    if (roots.isEmpty() && CurrentUserUtil.hasCurrentUser()) {
      UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
      roots = currentUser.getUserEffectiveSearchOrgUnitIds().stream().map(UID::of).toList();
    }
    Locale locale =
        params.locale() == null
            ? UserSettings.getCurrentSettings().getUserDbLocale()
            : params.locale();
    List<UID> groups = params.groups() == null ? List.of() : params.groups();
    int hierarchySize =
        store.countOrgUnitsInScope(
            new OrgTreeStore.OrgTreeScope(roots, groups, params.level(), params.depth()));

    // TODO validate and/or filter root/group IDs?
    return new OrgTreeParams(
        params.page() == null ? 1 : params.page(),
        params.pageSize() == null ? 50 : params.pageSize(),
        hierarchySize,
        locale,
        roots,
        groups,
        params.level(),
        params.currentlyOpen(),
        params.search(),
        Boolean.TRUE.equals(params.shortName()),
        params.depth());
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public OrgTree query(@Nonnull OrgTreeParams params) {
    List<OrgUnitPath> matches = store.queryPageMatches(params);
    Set<OrgUnitPath> ancestors = OrgUnitPath.ofMissingAncestors(matches);
    Locale locale = params.locale();
    Stream<OrgTreeEntry> entries =
        store.streamEntries(locale, matches.stream().map(OrgUnitPath::toUID));
    if (OrgTree.isHierarchical(matches))
      entries = OrgTree.sortedHierarchical(entries.toList()).stream();
    int total = -1;
    int pageSize = params.pageSize();
    int page = params.page();
    if (matches.size() < pageSize) total = matches.size() + ((page - 1) * pageSize);
    // only count if we don't already know from a partial page
    if (total < 0) total = store.countTotalMatches(params);
    OrgTree.Pager pager = new OrgTree.Pager(page, pageSize, total);
    return new OrgTree(
        pager, entries, store.streamEntries(locale, ancestors.stream().map(OrgUnitPath::toUID)));
  }
}
