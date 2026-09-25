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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;

/**
 * API to support OU tree searching and fetching.
 *
 * @since 2.44
 * @author Jan Bernitt
 */
public interface OrgTreeStore {

  record OrgTreeScope(
      @Nonnull List<UID> roots,
      @Nonnull List<UID> groups,
      @CheckForNull Integer level,
      @CheckForNull Integer depth) {}

  /**
   * @param params criteria of OUs to include in the matches
   * @return the number of org units that can be potential matches for a search
   */
  int countOrgUnitsInScope(OrgTreeScope params);

  /**
   * Note that it is crucial that the results are order by path so that matches within the same
   * subtree do not unnecessarily occur on random pages but instead are either all on one page or on
   * successive pages.
   *
   * @param params criteria of OUs to include in the matches
   * @return The paths of all matches to the query params ordered by path
   */
  List<OrgUnitPath> queryPageMatches(OrgTreeParams params);

  /**
   * @param params criteria of OUs to include in the count
   * @return number of total rows matching the given criteria
   */
  int countTotalMatches(OrgTreeParams params);

  /**
   * @param locale to use when resolving the display name
   * @param orgUnits of the OU to fetch
   * @return A stream of entries for the given IDs sorted by level first, display name second
   */
  Stream<OrgTree.OrgTreeEntry> streamEntries(Locale locale, Stream<UID> orgUnits);
}
