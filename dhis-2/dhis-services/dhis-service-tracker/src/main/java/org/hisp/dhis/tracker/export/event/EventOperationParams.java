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
package org.hisp.dhis.tracker.export.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.Order;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventOperationParams {
  private UID program;

  private UID programStage;

  private EnrollmentStatus enrollmentStatus;

  private Boolean followUp;

  private UID orgUnit;

  @Builder.Default
  private OrganisationUnitSelectionMode orgUnitMode = OrganisationUnitSelectionMode.ACCESSIBLE;

  private AssignedUserSelectionMode assignedUserMode;

  private Set<UID> assignedUsers;

  private UID trackedEntity;

  private Date occurredAfter;

  private Date occurredBefore;

  private EventStatus eventStatus;

  private Date updatedAfter;

  private Date updatedBefore;

  /** The last updated duration filter. */
  private String updatedWithin;

  private Date scheduledAfter;

  private Date scheduledBefore;

  private Date enrollmentEnrolledBefore;

  private Date enrollmentEnrolledAfter;

  private Date enrollmentOccurredBefore;

  private Date enrollmentOccurredAfter;

  private UID attributeCategoryCombo;

  @Builder.Default private Set<UID> attributeCategoryOptions = Collections.emptySet();

  private CategoryOptionCombo categoryOptionCombo;

  private boolean includeRelationships;

  /**
   * Events can be ordered by field names (given as {@link String}), data element (given as {@link
   * UID}) and tracked entity attribute (given as {@link UID}). It is crucial for the order values
   * to stay in one collection as their order needs to be kept as provided by the user. We cannot
   * come up with a type-safe type that captures the above order features and that can be used in a
   * generic collection such as a List (see typesafe heterogeneous container). We therefore provide
   * {@link EventOperationParamsBuilder#orderBy(String, SortDirection)} and {@link
   * EventOperationParamsBuilder#orderBy(UID, SortDirection)} to advocate the types that can be
   * ordered by while storing the order in a single List of {@link Order}.
   */
  private List<Order> order;

  /** Data element filters per data element UID. */
  private final Map<UID, List<QueryFilter>> dataElementFilters;

  /** Tracked entity attribute filters per attribute UID. */
  private final Map<UID, List<QueryFilter>> attributeFilters;

  @Builder.Default private Set<UID> events = new HashSet<>();

  private boolean includeDeleted;

  private boolean synchronizationQuery;

  /** Indicates a point in the time used to decide the data that should not be synchronized */
  private Date skipChangedBefore;

  private Set<UID> enrollments;

  @Builder.Default private EventFields fields = EventFields.none();

  @Builder.Default
  private TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder().build();

  public static class EventOperationParamsBuilder {

    private final List<Order> order = new ArrayList<>();

    private Map<UID, List<QueryFilter>> dataElementFilters = new HashMap<>();

    private Map<UID, List<QueryFilter>> attributeFilters = new HashMap<>();

    // Do not remove this unused method. This hides the order field from the builder which Lombok
    // does not support. The repeated order field and private order method prevent access to order
    // via the builder.
    // Order should be added via the orderBy builder methods.
    private EventOperationParamsBuilder order(List<Order> order) {
      return this;
    }

    public EventOperationParamsBuilder orderBy(String field, SortDirection direction) {
      this.order.add(new Order(field, direction));
      return this;
    }

    public EventOperationParamsBuilder orderBy(UID uid, SortDirection direction) {
      this.order.add(new Order(uid, direction));
      return this;
    }

    public EventOperationParamsBuilder program(UID uid) {
      this.program = uid;
      return this;
    }

    public EventOperationParamsBuilder program(Program program) {
      this.program = UID.of(program);
      return this;
    }

    public EventOperationParamsBuilder programStage(UID uid) {
      this.programStage = uid;
      return this;
    }

    public EventOperationParamsBuilder programStage(ProgramStage programStage) {
      this.programStage = UID.of(programStage);
      return this;
    }

    public EventOperationParamsBuilder orgUnit(UID uid) {
      this.orgUnit = uid;
      return this;
    }

    public EventOperationParamsBuilder orgUnit(OrganisationUnit orgUnit) {
      this.orgUnit = UID.of(orgUnit);
      return this;
    }

    public EventOperationParamsBuilder trackedEntity(UID uid) {
      this.trackedEntity = uid;
      return this;
    }

    public EventOperationParamsBuilder trackedEntity(TrackedEntity trackedEntity) {
      this.trackedEntity = UID.of(trackedEntity);
      return this;
    }

    // Do not remove this unused method. This hides the data element filters field from the builder
    // which Lombok
    // does not support. The repeated field and private method prevent access to
    // the filter map via the builder.
    // Filters should be added via the filterByDataElement builder methods.
    private EventOperationParamsBuilder dataElementFilters(
        Map<UID, List<QueryFilter>> dataElementFilters) {
      return this;
    }

    // Do not remove this unused method. This hides the attribute filters field from the builder
    // which Lombok
    // does not support. The repeated field and private method prevent access to the filter map via
    // the builder.
    // Filters should be added via the filterByAttribute builder methods.
    private EventOperationParamsBuilder attributeFilters(
        Map<UID, List<QueryFilter>> attributeFilters) {
      return this;
    }

    public EventOperationParamsBuilder filterByDataElement(
        @Nonnull UID attribute, @Nonnull List<QueryFilter> queryFilters) {
      this.dataElementFilters.putIfAbsent(attribute, new ArrayList<>());
      this.dataElementFilters.get(attribute).addAll(queryFilters);
      return this;
    }

    public EventOperationParamsBuilder filterByDataElement(@Nonnull UID dataElement) {
      this.dataElementFilters.putIfAbsent(
          dataElement, List.of(new QueryFilter(QueryOperator.NNULL)));
      return this;
    }

    public EventOperationParamsBuilder filterByAttribute(
        @Nonnull UID attribute, @Nonnull List<QueryFilter> queryFilters) {
      this.attributeFilters.putIfAbsent(attribute, new ArrayList<>());
      this.attributeFilters.get(attribute).addAll(queryFilters);
      return this;
    }

    public EventOperationParamsBuilder filterByAttribute(@Nonnull UID attribute) {
      this.attributeFilters.putIfAbsent(attribute, List.of(new QueryFilter(QueryOperator.NNULL)));
      return this;
    }
  }
}
