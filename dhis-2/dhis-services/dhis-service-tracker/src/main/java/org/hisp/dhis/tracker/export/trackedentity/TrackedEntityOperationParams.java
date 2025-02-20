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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.ArrayList;
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
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.Order;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrackedEntityOperationParams {
  public static final int DEFAULT_PAGE = 1;

  public static final int DEFAULT_PAGE_SIZE = 50;

  @Builder.Default private TrackedEntityParams trackedEntityParams = TrackedEntityParams.FALSE;

  /** Tracked entity attribute filters per attribute UID. */
  @Builder.Default private Map<UID, List<QueryFilter>> filters = new HashMap<>();

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  @Builder.Default private Set<UID> organisationUnits = new HashSet<>();

  /** Program for which instances in the response must be enrolled in. */
  private UID program;

  /** Status of a tracked entities enrollment into a given program. */
  private EnrollmentStatus enrollmentStatus;

  /** Indicates whether tracked entity is marked for follow up for the specified program. */
  private Boolean followUp;

  /** Start date for last updated. */
  private Date lastUpdatedStartDate;

  /** End date for last updated. */
  private Date lastUpdatedEndDate;

  /** The last updated duration filter. */
  private String lastUpdatedDuration;

  /** Start date for enrollment in the given program. */
  private Date programEnrollmentStartDate;

  /** End date for enrollment in the given program. */
  private Date programEnrollmentEndDate;

  /** Start date for incident in the given program. */
  private Date programIncidentStartDate;

  /** End date for incident in the given program. */
  private Date programIncidentEndDate;

  /** Tracked entity type to fetch. */
  private UID trackedEntityType;

  @Builder.Default
  private OrganisationUnitSelectionMode orgUnitMode = OrganisationUnitSelectionMode.ACCESSIBLE;

  @Getter @Builder.Default
  private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  /** Set of te uids to explicitly select. */
  @Builder.Default private Set<UID> trackedEntities = new HashSet<>();

  /** ProgramStage to be used in conjunction with eventstatus. */
  private UID programStage;

  /** Status of any events in the specified program. */
  private EventStatus eventStatus;

  /** Start date for event for the given program. */
  private Date eventStartDate;

  /** End date for event for the given program. */
  private Date eventEndDate;

  /** Page number. */
  private Integer page;

  /** Page size. */
  private Integer pageSize;

  /** Indicates whether to include the total number of pages in the paging response. */
  private boolean totalPages;

  /** Indicates whether paging should be skipped. */
  private boolean skipPaging;

  /** Indicates if there is a maximum te retrieval limit. 0 no limit. */
  private int maxTeiLimit;

  /** Indicates whether to include soft-deleted elements. Default to false */
  @Builder.Default private boolean includeDeleted = false;

  /**
   * Indicates whether the search is internal triggered by the system. The system should trigger
   * superuser search to detect duplicates.
   */
  private boolean internalSearch;

  /** Indicates whether the search is for synchronization purposes (for Program Data sync job). */
  private boolean synchronizationQuery;

  /**
   * Potential Duplicate query parameter value. If null, we don't check whether a TE is a
   * potentialDuplicate or not
   */
  private Boolean potentialDuplicate;

  /**
   * Tracked entities can be ordered by field names (given as {@link String}) and tracked entity
   * attributes (given as {@link UID}). It is crucial for the order values to stay in one collection
   * as their order needs to be kept as provided by the user. We cannot come up with a type-safe
   * type that captures the above order features and that can be used in a generic collection such
   * as a List (see typesafe heterogeneous container). We therefore provide {@link
   * TrackedEntityOperationParamsBuilder#orderBy(String, SortDirection)} and {@link
   * TrackedEntityOperationParamsBuilder#orderBy(UID, SortDirection)} to advocate the types that can
   * be ordered by while storing the order in a single List of {@link Order}.
   */
  private List<Order> order;

  public static class TrackedEntityOperationParamsBuilder {

    private final List<Order> order = new ArrayList<>();

    // Do not remove this unused method. This hides the order field from the builder which Lombok
    // does not support. The repeated order field and private order method prevent access to order
    // via the builder.
    // Order should be added via the orderBy builder methods.
    private TrackedEntityOperationParamsBuilder order(List<Order> order) {
      return this;
    }

    public TrackedEntityOperationParamsBuilder orderBy(String field, SortDirection direction) {
      this.order.add(new Order(field, direction));
      return this;
    }

    public TrackedEntityOperationParamsBuilder orderBy(UID uid, SortDirection direction) {
      this.order.add(new Order(uid, direction));
      return this;
    }

    public TrackedEntityOperationParamsBuilder program(UID uid) {
      this.program = uid;
      return this;
    }

    public TrackedEntityOperationParamsBuilder program(Program program) {
      this.program = UID.of(program);
      return this;
    }

    public TrackedEntityOperationParamsBuilder programStage(UID uid) {
      this.programStage = uid;
      return this;
    }

    public TrackedEntityOperationParamsBuilder programStage(ProgramStage programStage) {
      this.programStage = UID.of(programStage);
      return this;
    }

    public TrackedEntityOperationParamsBuilder trackedEntityType(UID uid) {
      this.trackedEntityType = uid;
      return this;
    }

    public TrackedEntityOperationParamsBuilder trackedEntityType(
        TrackedEntityType trackedEntityType) {
      this.trackedEntityType = UID.of(trackedEntityType);
      return this;
    }

    public TrackedEntityOperationParamsBuilder trackedEntities(Set<UID> uids) {
      this.trackedEntities$value = uids;
      this.trackedEntities$set = true;
      return this;
    }

    public TrackedEntityOperationParamsBuilder trackedEntities(TrackedEntity... trackedEntities) {
      this.trackedEntities$value = UID.of(trackedEntities);
      this.trackedEntities$set = true;
      return this;
    }

    public TrackedEntityOperationParamsBuilder organisationUnits(Set<UID> uids) {
      this.organisationUnits$value = uids;
      this.organisationUnits$set = true;
      return this;
    }

    public TrackedEntityOperationParamsBuilder organisationUnits(
        OrganisationUnit... organisationUnits) {
      this.organisationUnits$value = UID.of(organisationUnits);
      this.organisationUnits$set = true;
      return this;
    }

    public TrackedEntityOperationParamsBuilder filter(
        TrackedEntityAttribute attribute, List<QueryFilter> queryFilters) {
      this.filters$value = Map.of(UID.of(attribute), queryFilters);
      this.filters$set = true;
      return this;
    }
  }
}
