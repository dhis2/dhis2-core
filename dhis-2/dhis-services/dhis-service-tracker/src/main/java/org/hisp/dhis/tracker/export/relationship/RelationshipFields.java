/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.relationship;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * RelationshipFields indicates which of the relationship fields should be exported. This is used to
 * save retrieval of data that is not needed. Be specific in what you need to save resources!
 *
 * <p>This class uses the field names of our view layer in {@link #RelationshipFields(Predicate,
 * String)}. The field names currently match the field names of the entity classes in this module.
 * We could map the view names in the predicate if we wanted to.
 */
@Getter
@ToString
@EqualsAndHashCode
public class RelationshipFields {
  private final boolean includesFrom;
  private final RelationshipItemFields fromFields;

  private final boolean includesTo;
  private final RelationshipItemFields toFields;

  /**
   * Create fields class using the predicate to test if a given field should be exported. {@code
   * pathSeparator} is used to concatenate paths for nested fields. The field filtering service
   * which can be used as the predicate for example supports dot notation. That means you can use it
   * to test if a nested path like {@code "from.trackedEntity.enrollments"} is included in the users
   * {@code fields}. This allows us to compose fields classes in the same way as the structure the
   * fields class is supposed to represent.
   */
  private RelationshipFields(Predicate<String> includesFields, String pathSeparator) {
    if (includesFields.test("from")) {
      this.includesFrom = true;
      this.fromFields =
          RelationshipItemFields.of(
              f -> includesFields.test("from" + pathSeparator + f), pathSeparator);
    } else {
      this.includesFrom = false;
      this.fromFields = RelationshipItemFields.none();
    }

    if (includesFields.test("to")) {
      this.includesTo = true;
      this.toFields =
          RelationshipItemFields.of(
              f -> includesFields.test("to" + pathSeparator + f), pathSeparator);
    } else {
      this.includesTo = false;
      this.toFields = RelationshipItemFields.none();
    }
  }

  public static RelationshipFields of(
      @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
    return new RelationshipFields(includesFields, pathSeparator);
  }

  /** Use this if you do not want fields to be exported. */
  public static RelationshipFields none() {
    // the path separator does not matter as the predicate returns false regardless of the path
    return new RelationshipFields(f -> false, "x");
  }

  /** Use this if you do want all fields to be exported. This is potentially expensive! */
  public static RelationshipFields all() {
    // the path separator does not matter as the predicate returns true regardless of the path
    return new RelationshipFields(f -> true, "x");
  }
}
