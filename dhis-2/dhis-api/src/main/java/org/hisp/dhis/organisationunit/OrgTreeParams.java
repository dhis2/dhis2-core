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

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;

/**
 * A query either uses {@link #search} to find OUs containing the search term in their display name
 * (or short name) or they use {@link #depth} in combination with {@link #roots} to page through
 * subtrees.
 *
 * <p>Both queries might be combined with {@link #groups} and {@link #groupSets} filters as well as
 * a {@link #level} and {@link #currentlyOpen} filters.
 *
 * @param page
 * @param pageSize
 * @param hierarchySize number of OUs in the user's search hierarchy (approximation)
 * @param roots
 * @param groups
 * @param level
 * @param search
 * @param shortName
 * @param depth
 */
public record OrgTreeParams(
    int page,
    int pageSize,
    int hierarchySize,
    @Nonnull Locale locale,
    @Nonnull List<UID> roots,
    @Nonnull List<UID> groups,
    @CheckForNull Integer level,
    @CheckForNull Boolean currentlyOpen,
    @CheckForNull String search,
    boolean shortName,
    @CheckForNull Integer depth) {

  public OrgTreeParams {
    requireNonNull(locale);
    requireNonNull(roots);
    requireNonNull(groups);
    if (page < 1) throw new IllegalArgumentException("Page must be positive");
    if (pageSize < 1) throw new IllegalArgumentException("Page size must be positive");
    if (level != null && level < 1) throw new IllegalArgumentException("Level must be positive");
    if (depth != null && depth < 1) throw new IllegalArgumentException("Depth must be positive");
  }

  public int offset() {
    return (page - 1) * pageSize;
  }

  /**
   * @return the {@link #search} as DB like expression
   */
  @CheckForNull
  public String searchLike() {
    return likePattern(search);
  }

  private static String likePattern(String pattern) {
    if (StringUtils.isBlank(pattern)) return null;
    int len = pattern.length();
    if (len > 2 && pattern.charAt(0) == '"' && pattern.charAt(len - 1) == '"')
      return likeEscape(pattern);
    if (pattern.indexOf('*') < 0) return "%" + likeEscape(pattern) + "%";
    return pattern.replace("*", "%").replace("?", "_").replace("\\", "\\\\");
  }

  private static String likeEscape(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /** The parameters as seen and provided from user input. */
  public record Input(

      // pager
      @CheckForNull Integer page,
      @CheckForNull Integer pageSize,

      // context
      @CheckForNull Locale locale,

      // filters
      @OpenApi.Description(
              """
  Includes only children to any of the given nodes.
  If an ID in the list is a children of another ID in the list the result is the same as ignoring that ID.
  If no root is given the current user's search hierarchy is used.
  """)
          @CheckForNull
          List<UID> roots,
      @OpenApi.Description(
              "Includes only matches that are member in any of the given group (if not empty).")
          @CheckForNull
          List<UID> groups,
      @OpenApi.Description(
              "Includes only matches that are member of any groups contained in any of the given group sets (if not empty).")
          @CheckForNull
          List<UID> groupSets,
      @OpenApi.Description("Includes only matches with the given level.") @CheckForNull
          Integer level,
      @OpenApi.Description(
              "Includes only matches that are currently open for data entry based on their `openingDate` and `closedDate`")
          @CheckForNull
          Boolean currentlyOpen,
      @OpenApi.Description(
              """
      Includes any OU where `displayName` has a substring match for the `search` term.
      When combined with `shortName` the `displayShortName` is searched instead.""")
          @CheckForNull
          @JsonAlias("q")
          String search,
      @OpenApi.Description(
              "Can be used with `search` to match on `displayShortName` instead of `displayName`")
          @CheckForNull
          Boolean shortName,
      @OpenApi.Description(
              """
      Includes the given number of levels of children for each of the given `roots`.
      Cannot be combined with `search`.""")
          @CheckForNull
          Integer depth) {

    public Input {
      if (depth != null && search != null)
        throw new IllegalArgumentException(
            "A query with `search` term cannot be limited by `depth` at the same time.");
    }
  }
}
