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
package org.hisp.dhis.organisationunit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * @author Lars Helge Overland
 */
public interface OrganisationUnitGroupStore
    extends IdentifiableObjectStore<OrganisationUnitGroup>,
        OrganisationUnitGroupDataIntegrityProvider {
  List<OrganisationUnitGroup> getOrganisationUnitGroupsWithGroupSets();

  OrganisationUnitGroup getOrgUnitGroupInGroupSet(
      Set<OrganisationUnitGroup> groups, OrganisationUnitGroupSet groupSet);

  /**
   * Returns the number of members (organisation units) for each of the given organisation unit
   * groups, keyed by group UID. The count is computed with a {@code size}/{@code count} query and
   * does not initialise the (potentially very large) members collection.
   *
   * @param uids the UIDs of the organisation unit groups to count members for.
   * @return a map of organisation unit group UID to member count. Non-existent groups are omitted.
   */
  Map<String, Integer> getOrganisationUnitGroupMemberCounts(Set<String> uids);

  /**
   * Returns, for each organisation unit at the given hierarchy level, the number of members of each
   * of the given groups in its subtree. The subtree of an organisation unit includes the
   * organisation unit itself.
   *
   * @param uids the UIDs of the organisation unit groups to count members for.
   * @param level the hierarchy level of the organisation units to count members for.
   * @param rootUids if not empty, only count members in the subtrees of these organisation units.
   * @return a map of organisation unit UID to a map of organisation unit group UID to member count.
   *     Organisation units without members of a group in their subtree have no entry for that
   *     group.
   */
  Map<String, Map<String, Integer>> getOrganisationUnitGroupMemberCountsAtLevel(
      Set<String> uids, int level, Set<String> rootUids);
}
