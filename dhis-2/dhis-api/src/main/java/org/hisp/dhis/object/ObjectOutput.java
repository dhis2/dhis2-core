/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.object;

import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility for generic output handling. In this context this means converting form domain objects to
 * typical output formats like JSON or CSV.
 *
 * @author Jan Bernitt
 * @since 2.43
 * @apiNote This acts as namespace for "common" data types and functions that are useful in
 *     generating web API output in general.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObjectOutput {

  /**
   * A simplified "generic" type descriptor based on what the DHIS2 {@code Schema} has to offer.
   *
   * @param rawType the {@link Class} type, usually that is the exact type
   * @param elementType in case the property is a collection this is the collections element type,
   *     otherwise null
   */
  public record Type(@Nonnull Class<?> rawType, @CheckForNull Class<?> elementType) {

    public static final Type //
        OBJECT = new Type(Object.class),
        STRING = new Type(String.class),
        BOOLEAN = new Type(Boolean.class),
        INTEGER = new Type(Integer.class);

    public Type {
      requireNonNull(rawType);
      // for parameterized types the element type is required as well
      if (rawType.isInterface() && rawType.getTypeParameters().length > 0)
        requireNonNull(elementType);
    }

    public Type(@Nonnull Class<?> rawType) {
      this(rawType, null);
    }
  }

  /**
   * A property in an object as it is presented to the output (web API).
   *
   * @implNote Usually this would be based on the DHIS2 {@code Schema} but it is intentionally
   *     decoupled from it and kept as simple as possible so any consumer only has to deal with very
   *     few cases.
   * @param path the name of the property (as an absolute path using dot for nesting)
   * @param type the type of the property
   */
  public record Property(String path, Type type) {

    public Property {
      requireNonNull(path);
      requireNonNull(type);
    }

    /**
     * @return the simple name of the property {@link #path()} (without the {@link #parentPath()}
     *     part)
     */
    public String name() {
      return name(path);
    }

    /**
     * @return the segments of the {@link #path()} that make drill down into the parent (owner) of
     *     this property
     */
    public List<String> parentPath() {
      return parentPath(path);
    }

    @Nonnull
    public static List<String> parentPath(@Nonnull String path) {
      int at = path.lastIndexOf('.');
      if (at < 0) return List.of();
      List<String> segments = List.of(path.split("\\."));
      return segments.subList(0, segments.size() - 1);
    }

    @Nonnull
    public static String name(@Nonnull String path) {
      int at = path.lastIndexOf('.');
      return at < 0 ? path : path.substring(at + 1);
    }
  }
}
