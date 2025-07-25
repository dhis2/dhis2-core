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
package org.hisp.dhis.fieldfiltering.better;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Fields represent the fields a user wants to be returned from an API usually specified via the
 * HTTP request parameter {@code fields}.
 *
 * <p>The structure is immutable once created, making it safe for concurrent access and caching. Use
 * {@link FieldsParser} to create instances from field expressions.
 */
// TODO(ivo) create our own constructor which copies?
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public final class Fields implements Predicate<String> {
  public static final Fields ALL = all();
  public static final Function<String, Fields> ALL_CHILDREN = (field) -> ALL;
  public static final Fields NONE = none();
  public static final Function<String, Fields> NO_CHILDREN = (field) -> NONE;

  /**
   * This flag determines the inclusion strategy. True means "include all except", whereas false
   * means "include only specified". {@link #fields} declares which fields to exclude or include.
   */
  private final boolean includesAll;

  /**
   * Fields are either fields to be excluded in case {@link #includesAll} is true or included in
   * case {@link #includesAll} is false.
   */
  private final Set<String> fields;

  private final Function<String, Fields> children;

  private final Map<String, Transformation> transformations;

  /** Creates Fields which includes all fields and all of its children with no transformations. */
  public static Fields all() {
    return new Fields(true, Set.of(), ALL_CHILDREN, Map.of());
  }

  /** Creates Fields which includes no fields and no children. */
  public static Fields none() {
    return new Fields(false, Set.of(), NO_CHILDREN, Map.of());
  }

  /**
   * Tests whether a field should be included in the result like JSON serialization.
   *
   * @param field the field name to test
   * @return true if the field should be included, false otherwise
   */
  @Override
  public boolean test(String field) {
    return includesAll ? !fields.contains(field) : fields.contains(field);
  }

  /**
   * Tests whether the dot separated field path is included. Serialization filters should use the
   * more performant {@link #test(String)}.
   *
   * @param dotPath dot separated field path
   * @return true if the field should be included, false otherwise
   */
  public boolean includes(String dotPath) {
    Fields current = this;
    String[] segments = dotPath.split("\\.");
    for (String segment : segments) {
      if (!current.test(segment)) {
        return false;
      }

      current = current.getChild(segment);
    }

    return true;
  }

  /**
   * Returns the fields specification for a child object.
   *
   * @param field the field name
   * @return Fields specification for the child, or null if no specific rules apply
   */
  @Nonnull
  public Fields getChild(String field) {
    return children.apply(field);
  }

  /**
   * Represents a field transformation like rename or isEmpty like {@code
   * "dataSets~isNotEmpty~rename(hasDataSets)}.
   */
  @EqualsAndHashCode
  public static final class Transformation {
    @Getter private final String name;
    private final String[] arguments;

    public Transformation(String name, String... arguments) {
      this.name = name;
      this.arguments = arguments.clone();
    }

    public String[] getArguments() {
      return arguments.clone();
    }

    @Override
    public String toString() {
      return name + "(" + String.join(",", arguments) + ")";
    }
  }
}
