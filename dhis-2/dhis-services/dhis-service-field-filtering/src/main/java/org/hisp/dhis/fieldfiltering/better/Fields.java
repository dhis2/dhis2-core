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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Fields represent the fields a user wants to be returned from an API usually specified via the
 * HTTP request parameter {@code fields}.
 *
 * <p>The structure is immutable once created, making it safe for concurrent access and caching. Use
 * {@link FieldsParser} to create instances from field expressions.
 */
@EqualsAndHashCode
public final class Fields implements Predicate<String> {
  private static final Fields ALL = all();
  private static final Fields NONE = none();

  /** True means "includes all except", whereas false means "includes only specified". */
  private final boolean includesAll;

  /**
   * Effective fields are either fields to be excluded in case {@link #includesAll()} is true or
   * fields to be included otherwise.
   */
  @Getter private final Set<String> effectiveFields;

  private final Map<String, Fields> children;
  private final Map<String, Transformation> transformations;

  public static Fields all() {
    return new Fields(true, Set.of(), Set.of(), Map.of(), Map.of());
  }

  public static Fields none() {
    return new Fields(false, Set.of(), Set.of(), Map.of(), Map.of());
  }

  /**
   * Creates Fields by computing effective fields from inclusions and exclusions.
   *
   * @param includesAll true for "includes all except" strategy, false for "includes only specified"
   * @param inclusions set of fields to includes
   * @param exclusions set of fields to exclude
   * @param children nested field specifications for child objects
   * @param transformations field transformations to apply during serialization
   */
  public Fields(
      boolean includesAll,
      Set<String> inclusions,
      Set<String> exclusions,
      Map<String, Fields> children,
      Map<String, Transformation> transformations) {
    this.includesAll = includesAll;
    this.effectiveFields = computeEffectiveFields(includesAll, inclusions, exclusions);
    this.children = Map.copyOf(children);
    this.transformations = Map.copyOf(transformations);
  }

  private static Set<String> computeEffectiveFields(
      boolean includesAll, Set<String> inclusions, Set<String> exclusions) {
    if (includesAll) {
      return Set.copyOf(exclusions);
    }

    Set<String> result = new HashSet<>(inclusions);
    result.removeAll(exclusions);
    return Set.copyOf(result);
  }

  /**
   * Tests whether a field should be included in the result like JSON serialization.
   *
   * @param field the field name to test
   * @return true if the field should be included, false otherwise
   */
  @Override
  public boolean test(String field) {
    return includesAll ? !effectiveFields.contains(field) : effectiveFields.contains(field);
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
   * Indicates if this fields has a fields defined for a child field. // TODO(ivo) double-check once
   * I fixed the remaining tests
   *
   * <p>false does not mean that the child is excluded! All fields and thus all children could still
   * be included.
   *
   * @param field the field name
   * @return Fields true if there fields are defined for the child, or false otherwise
   */
  public boolean containsChild(String field) {
    return children.containsKey(field);
  }

  /**
   * Returns the fields specification for a child object.
   *
   * @param field the field name
   * @return Fields specification for the child, or null if no specific rules apply
   */
  @Nonnull
  public Fields getChild(String field) {
    // TODO(ivo) make sure I am not distributing logic now between the parser and this class
    // specific children specs take precedence as they would for example contain explicit exclusions
    // like fields=program[!name]
    if (children.containsKey(field)) {
      return children.get(field);
    }

    // TODO(ivo) this is the default behavior for tracker while metadata would do fields=program
    // turns to fields=program[id]
    if (test(field)) {
      return ALL;
    }
    return NONE;
  }

  // TODO(ivo) how about returning a noop transformation?
  /**
   * Returns the transformation for a field, if any.
   *
   * @param field the field name
   * @return Transformation for the field, or null if no transformation applies
   */
  public Transformation getTransformation(String field) {
    return transformations.get(field);
  }

  /**
   * Returns whether this specification includes all fields except excluded fields specified in
   * {@link #getEffectiveFields()}.
   *
   * @return true if using "includes all except" strategy (e.g., "*,!code")
   */
  public boolean includesAll() {
    return includesAll;
  }

  @Override
  public String toString() {
    return "Fields[includesAll="
        + includesAll
        + ", effective="
        + effectiveFields
        + ", children="
        + children.keySet()
        + ", transformations="
        + transformations.keySet()
        + "]";
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
