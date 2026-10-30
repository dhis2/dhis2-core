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
 * RelationshipItemFields indicates which of the relationship item fields should be exported. This
 * is used to save retrieval of data that is not needed. Be specific in what you need to save
 * resources!
 *
 * <p>This class uses the field names of our view layer in {@link #RelationshipItemFields(Predicate,
 * String)}. The field names currently match the field names of the entity classes in this module.
 * We could map the view names in the predicate if we wanted to.
 *
 * <p>This class uses the same approach for the fields classes we use in the view layer mappers
 * which is duplicate the tracked entity, enrollment and event classes without the relationship
 * fields to break the cycle inherent in the data model.
 */
@Getter
@ToString
@EqualsAndHashCode
public class RelationshipItemFields {
  private final boolean includesTrackedEntity;
  private final TrackedEntityFields trackedEntityFields;

  private final boolean includesEnrollment;
  private final EnrollmentFields enrollmentFields;

  private final boolean includesEvent;

  private RelationshipItemFields(Predicate<String> includesFields, String pathSeparator) {
    if (includesFields.test("trackedEntity")) {
      this.includesTrackedEntity = true;
      this.trackedEntityFields =
          TrackedEntityFields.of(
              f -> includesFields.test("trackedEntity" + pathSeparator + f), pathSeparator);
    } else {
      this.includesTrackedEntity = false;
      this.trackedEntityFields = TrackedEntityFields.none();
    }

    if (includesFields.test("enrollment")) {
      this.includesEnrollment = true;
      this.enrollmentFields =
          EnrollmentFields.of(f -> includesFields.test("enrollment" + pathSeparator + f));
    } else {
      this.includesEnrollment = false;
      this.enrollmentFields = EnrollmentFields.none();
    }

    this.includesEvent = includesFields.test("event");
  }

  public static RelationshipItemFields of(
      @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
    return new RelationshipItemFields(includesFields, pathSeparator);
  }

  /** Use this if you do not want fields to be exported. */
  public static RelationshipItemFields none() {
    // the path separator does not matter as the predicate returns false regardless of the path
    return new RelationshipItemFields(f -> false, "x");
  }

  /** Use this if you do want all fields to be exported. This is potentially expensive! */
  public static RelationshipItemFields all() {
    // the path separator does not matter as the predicate returns true regardless of the path
    return new RelationshipItemFields(f -> true, "x");
  }

  @Getter
  @ToString
  @EqualsAndHashCode
  public static class TrackedEntityFields {
    private final boolean includesAttributes;
    private final boolean includesProgramOwners;

    private final boolean includesEnrollments;
    private final EnrollmentFields enrollmentFields;

    private TrackedEntityFields(Predicate<String> includesFields, String pathSeparator) {
      this.includesAttributes = includesFields.test("attributes");
      this.includesProgramOwners = includesFields.test("programOwners");

      if (includesFields.test("enrollments")) {
        this.includesEnrollments = true;
        this.enrollmentFields =
            EnrollmentFields.of(f -> includesFields.test("enrollments" + pathSeparator + f));
      } else {
        this.includesEnrollments = false;
        this.enrollmentFields = EnrollmentFields.none();
      }
    }

    public static TrackedEntityFields of(
        @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
      return new TrackedEntityFields(includesFields, pathSeparator);
    }

    public static TrackedEntityFields none() {
      // the path separator does not matter as the predicate returns false regardless of the path
      return new TrackedEntityFields(f -> false, "x");
    }
  }

  @Getter
  @ToString
  @EqualsAndHashCode
  public static class EnrollmentFields {
    private final boolean includesTrackedEntity;
    private final boolean includesAttributes;
    private final boolean includesEvents;

    private EnrollmentFields(Predicate<String> includesFields) {
      this.includesTrackedEntity = includesFields.test("trackedEntity");
      this.includesAttributes = includesFields.test("attributes");
      this.includesEvents = includesFields.test("events");
    }

    public static EnrollmentFields of(@Nonnull Predicate<String> includesFields) {
      return new EnrollmentFields(includesFields);
    }

    public static EnrollmentFields none() {
      return new EnrollmentFields(f -> false);
    }
  }
}
