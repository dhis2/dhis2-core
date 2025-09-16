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
package org.hisp.dhis.tracker.export.fieldfiltering;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Fields represent the fields a user wants to be returned from an API usually specified via the
 * HTTP request parameter {@code fields}.
 *
 * <p>Fields ensures that
 *
 * <ul>
 *   <li>children of an excluded field {@code test(field)==false} are excluded as well
 *   <li>includesAll automatically includes children unless they are explicitly included/excluded
 *   <li>children are automatically included unless they are explicitly included/excluded
 * </ul>
 */
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public final class Fields implements Predicate<String> {
  public static final Fields ALL = all();
  public static final Fields NONE = none();

  /** True means "includes all except", whereas false means "includes only specified". */
  private final boolean includesAll;

  /**
   * Fields are either fields to be excluded in case {@link #includesAll} is true or included in
   * case {@link #includesAll} is false.
   */
  private final Set<String> fields;

  /** Children define which of a {@link #fields} fields children should be included or excluded. */
  private final Map<String, Fields> children;

  /** Transformations declare how a field in {@link #fields} should be returned. */
  private final Map<String, List<Transformation>> transformations;

  /** Creates Fields which includes all fields and all of its children with no transformations. */
  public static Fields all() {
    return new Fields(true, Set.of(), Map.of(), Map.of());
  }

  /** Creates Fields which includes no fields. */
  public static Fields none() {
    return new Fields(false, Set.of(), Map.of(), Map.of());
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
   * Returns the fields specification for a child object.
   *
   * @param field the field name
   * @return Fields specification for the child
   */
  @Nonnull
  public Fields getChildren(String field) {
    if (!test(field)) {
      return Fields.NONE; // children of an excluded parent are excluded
    }

    // explicit specifications take precedence
    // this handles cases like: fields=dataValues[value], fields=dataValues[!value],
    // fields=dataValues[*]
    return children.getOrDefault(field, Fields.ALL);
  }

  /**
   * Tests whether the dot separated field path is included. Serialization filters should use the
   * more performant {@link #test(String)}.
   *
   * <p>{@code fields.includes("group.group.code")} is like {@code
   * fields.getChildren("group").getChildren("group").test("code")}
   *
   * @param dotPath dot separated field path
   * @return true if the field should be included, false otherwise
   */
  public boolean includes(String dotPath) {
    String[] segments = dotPath.split("\\.");
    Fields current = this;
    for (int i = 0; i < segments.length - 1; i++) {
      current = current.getChildren(segments[i]);
    }
    String lastSegment = segments[segments.length - 1];
    return current.test(lastSegment);
  }

  /**
   * Returns the transformations for a field.
   *
   * @param field the field name
   * @return transformation for the field if any
   */
  @Nonnull
  public List<Transformation> getTransformations(String field) {
    if (!test(field)) {
      return List.of(); // field must be included for it to be transformed
    }

    return transformations.getOrDefault(field, List.of());
  }

  /**
   * Represents a single field transformation like {@code isNotEmpty} or {@code rename} in {@code
   * fields=dataSets~isNotEmpty~rename(hasDataSets)}.
   *
   * <p>Users are allowed to pass multiple arguments to transformers with {@code
   * fields=field::rename(one;two;three)}. Since our transformers use at most one we'll only forward
   * one to simplify their logic.
   */
  public record Transformation(
      String name, FieldsTransformer.Function transformer, String argument) {}
}
