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
import org.hisp.dhis.feedback.BadParameterException;
import org.hisp.dhis.feedback.ErrorCode;

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
    @Nonnull List<OrgUnitPath> pageCursors,
    boolean ancestors,
    @Nonnull Locale locale,
    @Nonnull List<UID> roots,
    @Nonnull List<UID> groups,
    @CheckForNull UID program,
    @CheckForNull UID dataset,
    @CheckForNull Integer level,
    @CheckForNull Boolean currentlyOpen,
    @CheckForNull String search,
    boolean shortName,
    @CheckForNull Integer depth) {

  public OrgTreeParams {
    // these should have been initialized
    // so null is a programming errors not user input error
    requireNonNull(locale);
    requireNonNull(roots);
    requireNonNull(groups);
    requireNonNull(pageCursors);
    // these are user input errors...
    if (page < 1) throw new BadParameterException(ErrorCode.E8301, "page", page);
    if (pageSize < 1) throw new BadParameterException(ErrorCode.E8301, "pageSize", pageSize);
    if (level != null && level < 1)
      throw new BadParameterException(ErrorCode.E8301, "level", level);
    if (depth != null && depth < 1)
      throw new BadParameterException(ErrorCode.E8301, "depth", depth);
    if (program != null && dataset != null)
      throw new BadParameterException(ErrorCode.E8304, "program", "dataset");
    if (depth != null && search != null)
      throw new BadParameterException(ErrorCode.E8304, "depth", "search");
    if (roots.isEmpty()) throw new BadParameterException(ErrorCode.E8305, "roots", 1, roots);
    if (page - 2 >= pageCursors.size())
      throw new BadParameterException(ErrorCode.E8305, "pageCursor", page - 1, pageCursors);
  }

  @CheckForNull
  public String pageCursor() {
    return page == 1 ? null : pageCursors.get(page - 2).toString();
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
    // when length 2 always match literally
    if (len == 2) return likeEscape(pattern);
    // when quoted match literally
    if (len > 2 && pattern.charAt(0) == '"' && pattern.charAt(len - 1) == '"')
      return likeEscape(pattern.substring(1, pattern.length() - 1));
    // automatic "contains" when there are no wildcards
    if (pattern.indexOf('*') < 0) return "%" + likeEscape(pattern) + "%";
    // replace wildcards with DB ones and match pattern
    return pattern.replace("*", "%").replace("?", "_").replace("\\", "\\\\");
  }

  private static String likeEscape(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  public String translationPropertyKey() {
    return shortName ? "SHORT_NAME" : "NAME";
  }

  /** The parameters as seen and provided from user input. */
  public record RootInput(

      // pager
      @CheckForNull Integer page,
      @CheckForNull Integer pageSize,
      @CheckForNull String pageCursor,

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
      @CheckForNull UID program,
      @CheckForNull UID dataset,
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
          Integer depth) {}

  public record ChildrenInput(
      // pager
      @CheckForNull Integer page,
      @CheckForNull Integer pageSize,
      @CheckForNull String pageCursor,

      // context
      @CheckForNull Locale locale,
      @CheckForNull Boolean shortName) {}
}
