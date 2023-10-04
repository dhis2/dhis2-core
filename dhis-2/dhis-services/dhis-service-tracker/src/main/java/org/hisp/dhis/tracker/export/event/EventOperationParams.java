/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventOperationParams {
  private String programUid;

  private String programStageUid;

  private ProgramStatus programStatus;

  private ProgramType programType;

  private Boolean followUp;

  private String orgUnitUid;

  private OrganisationUnitSelectionMode orgUnitMode;

  private AssignedUserSelectionMode assignedUserMode;

  private Set<String> assignedUsers;

  private String trackedEntityUid;

  private Date startDate;

  private Date endDate;

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

  private String attributeCategoryCombo;

  @Builder.Default private Set<String> attributeCategoryOptions = Collections.emptySet();

  private CategoryOptionCombo categoryOptionCombo;

  @Builder.Default private IdSchemes idSchemes = new IdSchemes();

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

  private boolean includeAttributes;

  private boolean includeAllDataElements;

  @Builder.Default private Set<String> events = new HashSet<>();

  /** Data element filters per data element UID. */
  @Builder.Default private Map<String, List<QueryFilter>> dataElementFilters = new HashMap<>();

  /** Tracked entity attribute filters per attribute UID. */
  @Builder.Default private Map<String, List<QueryFilter>> attributeFilters = new HashMap<>();

  private boolean includeDeleted;

  private Set<String> accessiblePrograms;

  private Set<String> accessibleProgramStages;

  private boolean synchronizationQuery;

  /** Indicates a point in the time used to decide the data that should not be synchronized */
  private Date skipChangedBefore;

  private Set<String> enrollments;

  public static class EventOperationParamsBuilder {

    private List<Order> order = new ArrayList<>();

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
  }
}
