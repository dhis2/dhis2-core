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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TrackedEntityOperationParams {
  public static final int DEFAULT_PAGE = 1;

  public static final int DEFAULT_PAGE_SIZE = 50;

  @Builder.Default private TrackedEntityParams trackedEntityParams = TrackedEntityParams.FALSE;

  /** Query value, will apply to all relevant attributes. */
  private QueryFilter query;

  /** Attributes to be included in the response. Can be used to filter response. */
  private String attributes;

  /** Filters for the response. */
  private String filters;

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  @Builder.Default private Set<String> organisationUnits = new HashSet<>();

  /** Program for which instances in the response must be enrolled in. */
  private String programUid;

  /** Status of the tracked entity instance in the given program. */
  private ProgramStatus programStatus;

  /**
   * Indicates whether tracked entity instance is marked for follow up for the specified program.
   */
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
  private String trackedEntityTypeUid;

  @Builder.Default
  private OrganisationUnitSelectionMode orgUnitMode = OrganisationUnitSelectionMode.DESCENDANTS;

  @Getter @Builder.Default
  private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  /** Set of te uids to explicitly select. */
  @Builder.Default private Set<String> trackedEntityUids = new HashSet<>();

  /** ProgramStage to be used in conjunction with eventstatus. */
  private String programStageUid;

  /** Status of any events in the specified program. */
  private EventStatus eventStatus;

  /** Start date for event for the given program. */
  private Date eventStartDate;

  /** End date for event for the given program. */
  private Date eventEndDate;

  /** Indicates whether not to include meta data in the response. */
  private boolean skipMeta;

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

  /** Indicates whether to include all te attributes */
  private boolean includeAllAttributes;

  /**
   * Indicates whether the search is internal triggered by the system. The system should trigger
   * superuser search to detect duplicates.
   */
  private boolean internalSearch;

  /** Indicates whether the search is for synchronization purposes (for Program Data sync job). */
  private boolean synchronizationQuery;

  /** Indicates a point in the time used to decide the data that should not be synchronized */
  private Date skipChangedBefore;

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

  private User user;

  public static class TrackedEntityOperationParamsBuilder {

    private List<Order> order = new ArrayList<>();

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
  }
}
