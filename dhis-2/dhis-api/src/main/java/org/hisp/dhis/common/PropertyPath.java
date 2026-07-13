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
package org.hisp.dhis.common;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.Text;

/**
 * A property path as used in the schema model or generally when reflecting on a Java object model.
 *
 * <p>The main benefit of using {@link PropertyPath} over {@link String} is that it enforces only
 * valid paths are possible to construct which indirectly validates user supplied path values so
 * such inputs cannot be abused for e.g. SQL injection.
 *
 * <p>A {@linkplain PropertyPath} is a sequence of path {@link #segment}s. Each segment is an
 * alphanumerical {@link Text} (underscore is allowed to). One segment in the sequence may have a
 * {@link #isPathModifier(char)} to make the segment an {@link #isExclude()} or {@link #isPreset()}.
 *
 * <p>The {@code *} alias for {@code :all} is specially handled by {@link #of(CharSequence)} by
 * replacing it with {@code :all} so on the character level {@code * is not a valid character for a
 * path}. This means {@code new PropertyPath(null, Text.of("*"))} will result in an error.
 *
 * @author Jan Bernitt
 * @since 2.44
 * @param parent the parent path (or null for root)
 * @param segment last segment in the path (without the dots)
 */
public record PropertyPath(@CheckForNull PropertyPath parent, @Nonnull Text segment)
    implements Comparable<PropertyPath> {

  public PropertyPath {
    // enforce path pattern by construction
    requireNonNull(segment);
    requireNonEmpty(segment);
    requirePathChars(segment);
    requireExcludeIsExclusive(parent, segment);
    requirePresetIsTail(parent);
  }

  @Nonnull
  public static PropertyPath of(@CheckForNull CharSequence path) {
    if (path == null || path.isEmpty()) throw illegalEmpty();
    Text p = Text.of(path);
    if (p.contentEquals("*")) return of(":all");
    int start = 0;
    int end = path.length();
    if (end > MAX_LENGTH) throw illegalTooLong();
    int i = start;
    PropertyPath res = null;
    while (i < end) {
      if (isPathModifier(p.charAt(i))) i++;
      while (i < end && isPathChar(p.charAt(i))) i++;
      Text seg = p.subSequence(start, i);
      if (seg.isEmpty()) throw illegalEmptySegment();
      res = new PropertyPath(res, seg);
      if (i < end && p.charAt(i) != '.') throw illegalCharacter(p.charAt(i));
      i++;
      start = i;
    }
    if (res == null) throw illegalEmpty();
    return res;
  }

  public static PropertyPath of(CharSequence... segments) {
    if (segments == null || segments.length == 0) throw illegalEmpty();
    PropertyPath res = PropertyPath.of(segments[0]);
    for (int i = 1; i < segments.length; i++) res = res.concat(Text.of(segments[i]));
    return res;
  }

  /**
   * Value was chosen to give some protection against attacks abusing very long paths but long
   * enough to allow for even seriously nested paths with long segment names.
   */
  private static final int MAX_LENGTH = 1024;

  /** For lexicographical sort order */
  @Override
  public int compareTo(@Nonnull PropertyPath b) {
    PropertyPath a = this;
    PropertyPath aParent = a.parent;
    PropertyPath bParent = b.parent;
    if (aParent == null && bParent == null) return a.segment.compareTo(b.segment);
    if (aParent == null) return -1;
    if (bParent == null) return 1;
    int aLen = a.length();
    int bLen = b.length();
    if (aLen == bLen) {
      int res = aParent.compareTo(bParent);
      return res != 0 ? res : a.segment.compareTo(b.segment);
    }
    if (aLen < bLen) {
      for (int i = 0; i < bLen - aLen; i++) b = bParent;
      int res = aParent.compareTo(bParent);
      if (res == 0) res = a.segment.compareTo(b.segment);
      return res != 0 ? res : -1;
    }
    for (int i = 0; i < aLen - bLen; i++) a = aParent;
    int res = aParent.compareTo(bParent);
    if (res == 0) res = a.segment.compareTo(b.segment);
    return res != 0 ? res : 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PropertyPath other)) return false;
    return Objects.equals(parent, other.parent) && segment.equals(other.segment);
  }

  @Override
  public int hashCode() {
    int hash = parent == null ? 1 : parent.hashCode();
    return hash * 31 + segment.hashCode();
  }

  @Override
  public String toString() {
    if (parent == null) return segment.toString();
    return parent + "." + segment;
  }

  public boolean isNested() {
    return parent != null;
  }

  /** An exclude is marked with {@code !} or {@code -} (dash) either at head or tail segment. */
  public boolean isExclude() {
    return isExcludeModifier(segment.charAt(0)) || parent != null && parent.isExclude();
  }

  public boolean isPreset() {
    return isPresetModifier(segment.charAt(0));
  }

  public boolean isAll() {
    // note that * would have been changed to all during parsing
    return segment.contentEquals(":all");
  }

  /**
   * @return true if the last segment of the path is a valid UID as used for attribute properties
   */
  public boolean isUID() {
    return segment.length() == 11 && CodeGenerator.isValidUid(segment);
  }

  /**
   * @return the number of segments in this path, zero for the root (self)
   */
  public int length() {
    return parent == null ? 1 : parent.length() + 1;
  }

  /**
   * @return the property name the path ends with
   */
  public String property() {
    return isExcludeModifier(segment.charAt(0))
        ? segment.subSequence(1, segment.length()).toString()
        : segment.toString();
  }

  public Text head() {
    return parent == null ? segment : parent.head();
  }

  @Nonnull
  public List<Text> segments() {
    if (parent == null) return List.of(segment);
    int n = length();
    PropertyPath path = this;
    Text[] res = new Text[n];
    int i = n - 1;
    while (path != null) {
      res[i--] = path.segment;
      path = path.parent;
    }
    return Stream.of(res).toList();
  }

  @Nonnull
  public PropertyPath concat(@Nonnull Text segment) {
    if (segment.contentEquals("*")) return concat(Text.of(":all"));
    return new PropertyPath(this, segment);
  }

  @Nonnull
  public PropertyPath concat(@Nonnull PropertyPath subPath) {
    if (subPath.parent == null) return concat(subPath.segment);
    PropertyPath res = this;
    for (Text segment : subPath.segments()) res = new PropertyPath(res, segment);
    return res;
  }

  public PropertyPath withTail(@Nonnull String segment) {
    return new PropertyPath(parent, Text.of(segment));
  }

  public PropertyPath withTail(@Nonnull Text segment) {
    return new PropertyPath(parent, segment);
  }

  private static void requireNonEmpty(Text segment) {
    if (segment.isEmpty()) throw illegalEmptySegment();
  }

  private static void requirePathChars(Text segment) {
    char c0 = segment.charAt(0);
    if (!isPathChar(c0) && !isPathModifier(c0)) throw illegalCharacter(c0);
    int len = segment.length();
    if (len == 1 && !isPathChar(c0)) throw illegalEmptySegment();
    for (int i = 1; i < len; i++)
      if (!isPathChar(segment.charAt(i))) throw illegalCharacter(segment.charAt(i));
  }

  private static boolean isPathChar(char c) {
    return c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
  }

  private static boolean isPathModifier(char c) {
    return isPresetModifier(c) || isExcludeModifier(c);
  }

  private static boolean isPresetModifier(char c) {
    return c == ':';
  }

  private static boolean isExcludeModifier(char c) {
    return c == '-' || c == '!';
  }

  private static void requireExcludeIsExclusive(PropertyPath parent, Text segment) {
    if (parent == null) return;
    if (!isExcludeModifier(segment.charAt(0))) return;
    if (parent.isExclude())
      throw new IllegalArgumentException(
          "A property path can only have one exclude or preset modifier");
  }

  /** A preset modifier must always be at the tail segment of a path */
  private static void requirePresetIsTail(PropertyPath parent) {
    if (parent == null) return;
    if (parent.isPreset())
      throw new IllegalArgumentException(
          "A property path with preset must always feature the preset at the tail");
  }

  @Nonnull
  private static IllegalArgumentException illegalEmpty() {
    return new IllegalArgumentException("A property path cannot be null or empty");
  }

  @Nonnull
  private static IllegalArgumentException illegalTooLong() {
    return new IllegalArgumentException(
        "A property path cannot be longer than %d characters".formatted(MAX_LENGTH));
  }

  @Nonnull
  private static IllegalArgumentException illegalEmptySegment() {
    return new IllegalArgumentException("A property path segment cannot be empty");
  }

  @Nonnull
  private static IllegalArgumentException illegalCharacter(char c) {
    return new IllegalArgumentException("A property path contained an illegal character: " + c);
  }
}
