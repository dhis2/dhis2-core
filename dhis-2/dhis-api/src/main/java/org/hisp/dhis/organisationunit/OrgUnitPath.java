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
import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.Text;

/**
 * A value type for an OU path.
 *
 * <p>A path is a sequence of OU UIDs each prefixed with a slash.
 *
 * <p>Besides providing utility around working with OU paths the type also makes sure that only
 * valid path instances can be constructed which guards against mistakes and bad user input.
 *
 * <p>Examples:
 *
 * <pre>
 * /ou123456789
 * /ou123456789/ou234567890
 * /ou123456789/ou234567890/ou345678901
 * </pre>
 *
 * @since 2.44
 * @author Jan Bernitt
 * @param parent parent path
 * @param uid the tail ID of this path (which is the ID of the OU the path belongs to)
 */
public record OrgUnitPath(@CheckForNull OrgUnitPath parent, @Nonnull Text uid) {

  @Nonnull
  public static OrgUnitPath of(@Nonnull CharSequence path) {
    if (path.isEmpty()) throw new IllegalArgumentException("Path must not be empty");
    if (path.charAt(0) != '/') throw new IllegalArgumentException("Path must start with a slash");
    int len = path.length();
    if (len % 12 != 0)
      throw new IllegalArgumentException("Path must consist of UIDs segments each with a leading slash");
    Text p = Text.of(path);
    int s = 0;
    OrgUnitPath res = new OrgUnitPath(null, p.subSequence(s + 1, 12));
    s += 12;
    while (s < len) {
      res = new OrgUnitPath(res, p.subSequence(s+1, s + 12));
      s += 12;
    }
    return res;
  }

  @JsonCreator
  @CheckForNull
  public static OrgUnitPath ofNullable(@CheckForNull CharSequence path) {
    return path == null ? null : of(path);
  }

  /**
   * @param paths a set of paths
   * @return a set of all paths not contained in given set of path which are direct or distant
   *     parent paths of all the given paths
   */
  @Nonnull
  public static Set<OrgUnitPath> ofMissingAncestors(@Nonnull Collection<OrgUnitPath> paths) {
    if (paths.isEmpty()) return Set.of();
    Set<OrgUnitPath> contained = paths instanceof Set<OrgUnitPath> set ? set : new HashSet<>(paths);
    Set<OrgUnitPath> ancestors = new HashSet<>();
    for (OrgUnitPath m : contained) {
      OrgUnitPath p = m.parent();
      while (p != null) {
        if (!paths.contains(p)) ancestors.add(p);
        p = p.parent();
      }
    }
    return ancestors;
  }

  public OrgUnitPath {
    requireNonNull(uid);
    if (!isValidUid(uid))
      throw new IllegalArgumentException("Path id must be a valid UID but was: " + uid);
  }

  /**
   * @return the length or number of OU levels in the path starting with 1 for the root path
   */
  public int length() {
    return parent == null ? 1 : parent.length() + 1;
  }

  /**
   * @param other a potential parent path to compare with
   * @return true, when the given path is a direct or distant parent path of this path
   */
  public boolean isParent(@Nonnull OrgUnitPath other) {
    if (parent == null) return false;
    if (other.length() >= length()) return false;
    return parent.equals(other) || parent.isParent(other);
  }

  /**
   * @param uid an OU ID to compare to
   * @return true, if the given ID is a direct or distant parent of this path or in other words if
   *     this path contains the given ID in any parent segment of the path
   */
  public boolean isAncestor(@Nonnull UID uid) {
    return isAncestor(uid.getValue());
  }

  /**
   * @param uid an OU ID to compare to
   * @return true, if the given ID is a direct or distant parent of this path or in other words if
   *     this path contains the given ID in any parent segment of the path
   */
  public boolean isAncestor(@Nonnull CharSequence uid) {
    if (parent == null) return false;
    return parent.uid.contentEquals(uid) || parent.isAncestor(uid);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OrgUnitPath other
        && Objects.equals(parent, other.parent)
        && uid.equals(other.uid);
  }

  @Override
  public int hashCode() {
    return parent == null ? uid.hashCode() : parent.hashCode() ^ uid.hashCode();
  }

  @Override
  public String toString() {
    return parent == null ? "/" + uid : parent + "/" + uid;
  }

  @Nonnull
  public UID toUID() {
    return UID.of(uid);
  }
}
