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
package org.hisp.dhis.tracker.export.enrollment;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.tracker.export.event.EventFields;

/**
 * EnrollmentFields indicates which of the enrollment fields should be exported. This is used to
 * save retrieval of data that is not needed. Be specific in what you need to save resources!
 *
 * <p>This class uses the field names of our view layer in {@link #EnrollmentFields(Predicate,
 * String)}. The field names currently match the field names of the entity classes in this module.
 * We could map the view names in the predicate if we wanted to.
 */
@Getter
@ToString
@EqualsAndHashCode
public class EnrollmentFields {
  private final boolean includesAttributes;
  private final boolean includesRelationships;

  private final boolean includesEvents;
  private final EventFields eventFields;

  private EnrollmentFields(Builder builder) {
    this.includesRelationships = builder.includesRelationships;
    this.includesAttributes = builder.includesAttributes;
    this.includesEvents = builder.includeEvents;
    this.eventFields = builder.includeEvents ? builder.eventFields : EventFields.none();
  }

  private EnrollmentFields(Predicate<String> includesFields, String pathSeparator) {
    this.includesAttributes = includesFields.test("attributes");
    this.includesRelationships = includesFields.test("relationships");

    if (includesFields.test("events")) {
      this.eventFields = EventFields.of(f -> includesFields.test("events" + pathSeparator + f));
      this.includesEvents = true;
    } else {
      this.eventFields = EventFields.none();
      this.includesEvents = false;
    }
  }

  public static EnrollmentFields of(
      @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
    return new EnrollmentFields(includesFields, pathSeparator);
  }

  /** Use this if you do not want fields to be exported. */
  public static EnrollmentFields none() {
    // the path separator does not matter as the predicate returns false regardless of the path
    return new EnrollmentFields(f -> false, "x");
  }

  /** Use this if you do want fields to be exported. This is potentially expensive! */
  public static EnrollmentFields all() {
    // the path separator does not matter as the predicate returns true regardless of the path
    return new EnrollmentFields(f -> true, "x");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean includesRelationships;
    private boolean includesAttributes;
    private boolean includeEvents;
    private EventFields eventFields;

    private Builder() {}

    public Builder includeRelationships() {
      this.includesRelationships = true;
      return this;
    }

    public Builder includeAttributes() {
      this.includesAttributes = true;
      return this;
    }

    /** Indicates that events should be exported with the given {@code fields}. */
    public Builder includeEvents(@Nonnull EventFields fields) {
      this.includeEvents = true;
      this.eventFields = fields;
      return this;
    }

    public EnrollmentFields build() {
      return new EnrollmentFields(this);
    }
  }
}
