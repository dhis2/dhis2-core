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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentFields;

/**
 * TrackedEntityFields indicates which of the tracked entity fields should be exported. This is used
 * to save retrieval of data that is not needed. Be specific in what you need to save resources!
 *
 * <p>This class uses the field names of our view layer in {@link #TrackedEntityFields(Predicate,
 * String)}. The field names currently match the field names of the entity classes in this module.
 * We could map the view names in the predicate if we wanted to.
 */
@Getter
@ToString
@EqualsAndHashCode
public class TrackedEntityFields {
  private final boolean includesAttributes;
  private final boolean includesRelationships;
  private final boolean includesProgramOwners;

  private final boolean includesEnrollments;
  private final EnrollmentFields enrollmentFields;

  private TrackedEntityFields(Builder builder) {
    this.includesAttributes = builder.includesAttributes;
    this.includesRelationships = builder.includesRelationships;
    this.includesProgramOwners = builder.includesProgramOwners;
    this.includesEnrollments = builder.includesEnrollments;
    this.enrollmentFields =
        builder.includesEnrollments ? builder.enrollmentFields : EnrollmentFields.none();
  }

  private TrackedEntityFields(Predicate<String> includesFields, String pathSeparator) {
    this.includesAttributes = includesFields.test("attributes");
    this.includesRelationships = includesFields.test("relationships");
    this.includesProgramOwners = includesFields.test("programOwners");

    if (includesFields.test("enrollments")) {
      this.enrollmentFields =
          EnrollmentFields.of(
              f -> includesFields.test("enrollments" + pathSeparator + f), pathSeparator);
      this.includesEnrollments = true;
    } else {
      this.enrollmentFields = EnrollmentFields.none();
      this.includesEnrollments = false;
    }
  }

  public static TrackedEntityFields of(
      @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
    return new TrackedEntityFields(includesFields, pathSeparator);
  }

  /** Use this if you do not want fields to be exported. */
  public static TrackedEntityFields none() {
    // the path separator does not matter as the predicate returns false regardless of the path
    return new TrackedEntityFields(f -> false, "x");
  }

  /** Use this if you do want fields to be exported. This is potentially expensive! */
  public static TrackedEntityFields all() {
    // the path separator does not matter as the predicate returns true regardless of the path
    return new TrackedEntityFields(f -> true, "x");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean includesRelationships;
    private boolean includesAttributes;
    private boolean includesProgramOwners;
    private boolean includesEnrollments;
    private EnrollmentFields enrollmentFields;

    private Builder() {}

    public Builder includeRelationships() {
      this.includesRelationships = true;
      return this;
    }

    public Builder includeAttributes() {
      this.includesAttributes = true;
      return this;
    }

    public Builder includeProgramOwners() {
      this.includesProgramOwners = true;
      return this;
    }

    /** Indicates that enrollments should be exported with the given {@code fields}. */
    public Builder includeEnrollments(@Nonnull EnrollmentFields fields) {
      this.includesEnrollments = true;
      this.enrollmentFields = fields;
      return this;
    }

    public TrackedEntityFields build() {
      return new TrackedEntityFields(this);
    }
  }
}
