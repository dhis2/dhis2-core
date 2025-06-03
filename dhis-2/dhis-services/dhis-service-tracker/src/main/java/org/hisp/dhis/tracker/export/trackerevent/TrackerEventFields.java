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
package org.hisp.dhis.tracker.export.trackerevent;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;

/**
 * EventFields indicates which of the event fields should be exported. This is used to save
 * retrieval of data that is not needed. Be specific in what you need to save resources!
 */
@Getter
@ToString
@EqualsAndHashCode
public class TrackerEventFields {
  private final boolean includesRelationships;
  private final RelationshipFields relationshipFields;

  private TrackerEventFields(Builder builder) {
    this.includesRelationships = builder.includesRelationships;
    this.relationshipFields =
        builder.includesRelationships ? builder.relationshipFields : RelationshipFields.none();
  }

  private TrackerEventFields(Predicate<String> includesFields, String pathSeparator) {
    if (includesFields.test("relationships")) {
      this.includesRelationships = true;
      this.relationshipFields =
          RelationshipFields.of(
              f -> includesFields.test("relationships" + pathSeparator + f), pathSeparator);
    } else {
      this.includesRelationships = false;
      this.relationshipFields = RelationshipFields.none();
    }
  }

  public static TrackerEventFields of(
      @Nonnull Predicate<String> includesFields, @Nonnull String pathSeparator) {
    return new TrackerEventFields(includesFields, pathSeparator);
  }

  /** Use this if you do not want fields to be exported. */
  public static TrackerEventFields none() {
    // the path separator does not matter as the predicate returns false regardless of the path
    return new TrackerEventFields(f -> false, "x");
  }

  /** Use this if you do want all fields to be exported. This is potentially expensive! */
  public static TrackerEventFields all() {
    // the path separator does not matter as the predicate returns true regardless of the path
    return new TrackerEventFields(f -> true, "x");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean includesRelationships;
    private RelationshipFields relationshipFields;

    private Builder() {}

    /** Indicates that relationships should be exported with the given {@code fields}. */
    public Builder includeRelationships(@Nonnull RelationshipFields fields) {
      this.includesRelationships = true;
      this.relationshipFields = fields;
      return this;
    }

    public TrackerEventFields build() {
      return new TrackerEventFields(this);
    }
  }
}
