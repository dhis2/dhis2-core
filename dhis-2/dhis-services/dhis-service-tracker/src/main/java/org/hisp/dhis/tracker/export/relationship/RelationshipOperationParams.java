/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.Order;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class RelationshipOperationParams {
  private final RelationshipFields fields;

  private final TrackerType type;

  private final UID identifier;

  private final Set<UID> relationships;

  private final List<Order> order;

  public static RelationshipOperationParamsBuilder builder(@Nonnull Set<UID> relationships) {
    if (relationships.isEmpty()) {
      throw new IllegalArgumentException("relationships must not be empty");
    }
    return new RelationshipOperationParamsBuilder().relationships(relationships);
  }

  public static RelationshipOperationParamsBuilder builder(@Nonnull TrackerEvent event) {
    return new RelationshipOperationParamsBuilder()
        .identifier(UID.of(event))
        .type(TrackerType.EVENT);
  }

  public static RelationshipOperationParamsBuilder builder(@Nonnull Enrollment enrollment) {
    return new RelationshipOperationParamsBuilder()
        .identifier(UID.of(enrollment))
        .type(TrackerType.ENROLLMENT);
  }

  public static RelationshipOperationParamsBuilder builder(@Nonnull TrackedEntity trackedEntity) {
    return new RelationshipOperationParamsBuilder()
        .identifier(UID.of(trackedEntity))
        .type(TrackerType.TRACKED_ENTITY);
  }

  public static RelationshipOperationParamsBuilder builder(
      @Nonnull TrackerType type, @Nonnull UID identifier) {
    return new RelationshipOperationParamsBuilder().identifier(identifier).type(type);
  }

  public static class RelationshipOperationParamsBuilder {
    private final List<Order> order = new ArrayList<>();
    private TrackerType type;
    private UID identifier;
    private Set<UID> relationships;
    private RelationshipFields fields = RelationshipFields.none();
    private boolean includeDeleted;

    RelationshipOperationParamsBuilder() {}

    public RelationshipOperationParamsBuilder orderBy(String field, SortDirection direction) {
      this.order.add(new Order(field, direction));
      return this;
    }

    private RelationshipOperationParamsBuilder identifier(UID uid) {
      this.identifier = uid;
      return this;
    }

    private RelationshipOperationParamsBuilder type(TrackerType type) {
      this.type = type;
      return this;
    }

    private RelationshipOperationParamsBuilder relationships(Set<UID> relationships) {
      this.relationships = relationships;
      return this;
    }

    public RelationshipOperationParamsBuilder includeDeleted(boolean includeDeleted) {
      this.includeDeleted = includeDeleted;
      return this;
    }

    public RelationshipOperationParamsBuilder fields(@Nonnull RelationshipFields fields) {
      this.fields = fields;
      return this;
    }

    public RelationshipOperationParams build() {
      return new RelationshipOperationParams(
          this.fields,
          this.type,
          this.identifier,
          this.relationships,
          this.order,
          this.includeDeleted);
    }
  }

  private boolean includeDeleted;
}
