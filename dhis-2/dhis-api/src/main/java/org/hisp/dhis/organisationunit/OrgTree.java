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

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;

/**
 * @param pager pager information
 * @param organisationUnits list of OUs matching the query
 * @param ancestors additional ancestors for the query
 */
public record OrgTree(
    @CheckForNull Pager pager,
    @Nonnull Stream<OrgTreeEntry> organisationUnits,
    @Nonnull Stream<OrgTreeEntry> ancestors) {

  public record OrgTreeEntry(
      @Nonnull OrgUnitPath path, @Nonnull String displayName, int level, boolean leaf) {

    public OrgTreeEntry(@Nonnull OrgUnitPath path, @Nonnull String displayName, boolean leaf) {
      this(path, displayName, path.length(), leaf);
    }

    public OrgTreeEntry {
      requireNonNull(path);
      requireNonNull(displayName);
      if (path.length() != level)
        throw new IllegalArgumentException("Path length and level must match");
    }

    public UID id() {
      return path.toUID();
    }

    public boolean isDirectParent(OrgTreeEntry other) {
      OrgUnitPath parent = path.parent();
      return parent != null && parent.equals(other.path);
    }
  }

  public record Pager(int page, int pageSize, int total) {
    public int pageCount() {
      return 1 + (total / pageSize);
    }
  }

  /**
   * @param paths some path to check if they are not flat (all on the same level)
   * @return if given path are on different levels
   */
  public static boolean isHierarchical(List<OrgUnitPath> paths) {
    int len = paths.size();
    if (len <= 1) return false;
    int level = paths.get(0).length();
    return paths.stream().allMatch(path -> level == path.length());
  }

  /*
  Hierarchy Sorting
   */

  /**
   * @param entries must be sorted already by level as 1st or major sort order
   * @return the input sorted hierarchical, meaning children occur directly below their parents
   *     maintaining any 2nd order sorting the input had for the children
   */
  public static List<OrgTreeEntry> sortedHierarchical(List<OrgTreeEntry> entries) {
    int len = entries.size();
    if (len <= 1) return entries;
    OrgTreeEntry[] sorted = entries.toArray(OrgTreeEntry[]::new);
    int minMoveLevel = sorted[0].level + 1;
    int from = 1;
    while (from < len && sorted[from].level < minMoveLevel) from++;
    while (from < len) {
      // find to (insert position)
      int to = from - 1;
      OrgTreeEntry e = sorted[from];
      while (to >= 0 && !e.isDirectParent(sorted[to])) to--;
      if (to >= 0) { // parent found...
        OrgTreeEntry parent = sorted[to];
        to++; // move past parent
        // move past previously moved entries
        while (to < from && sorted[to].level == e.level && sorted[to].isDirectParent(parent)) to++;
        System.arraycopy(sorted, to, sorted, to + 1, from - to);
        sorted[to] = e;
      }
      from++;
    }
    return List.of(sorted);
  }
}
