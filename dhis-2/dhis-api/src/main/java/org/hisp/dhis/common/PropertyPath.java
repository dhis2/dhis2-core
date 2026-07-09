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
 * @param parent the parent path (or null for root)
 * @param segment last segment in the path (without the dots)
 */
public record PropertyPath(@CheckForNull PropertyPath parent, @Nonnull Text segment) {

  public PropertyPath {
    // enforce path pattern by construction
    requireNonNull(segment);
    requireNonEmpty(segment);
    requirePathChars(segment);
  }

  @Nonnull
  public static PropertyPath of(@CheckForNull String path) {
    if (path == null || path.isEmpty())
      throw new IllegalArgumentException("A property path cannot be null or empty");
    if ("*".equals(path)) return of(":all");
    Text p = Text.of(path);
    int start = 0;
    int end = path.length();
    int i = start;
    PropertyPath res = null;
    while (i < end) {
      while (i < end && isPathChar(p.charAt(i))) i++;
      Text seg = p.subSequence(start, i);
      if (seg.isEmpty()) throw illegalEmptySegment();
      res = new PropertyPath(res, seg);
      if (i < end && p.charAt(i) != '.') throw illegalCharacter(p.charAt(i));
      i++;
      start = i;
    }
    return res;
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

  public boolean isExclude() {
    char c0 = segment.charAt(0);
    return c0 == '-' || c0 == '!';
  }

  public boolean isPreset() {
    return parent == null && segment.charAt(0) == ':';
  }

  public boolean isAll() {
    // note that * would have been changed to all during parsing
    return parent == null && segment.contentEquals(":all");
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

  public Text head() {
    return parent == null ? segment : parent.head();
  }

  @Nonnull
  public List<Text> segments() {
    if (parent == null) return List.of(segment);
    int n = length();
    PropertyPath p = this;
    Text[] res = new Text[n];
    for (int i = n - 1; i >= 0; i--) {
      res[i] = p.segment;
      p = p.parent;
    }
    return List.of(res);
  }

  @Nonnull
  public PropertyPath concat(@Nonnull Text segment) {
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
    for (int i = 0; i < segment.length(); i++)
      if (!isPathChar(segment.charAt(i))) throw illegalCharacter(segment.charAt(i));
  }

  private static boolean isPathChar(char c) {
    return c == '_'
        || c == '-'
        || c >= 'a' && c <= 'z'
        || c >= 'A' && c <= 'Z'
        || c >= '0' && c <= '9'
        // presets and excludes need this so we allow it
        || c == ':'
        || c == '!';
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
