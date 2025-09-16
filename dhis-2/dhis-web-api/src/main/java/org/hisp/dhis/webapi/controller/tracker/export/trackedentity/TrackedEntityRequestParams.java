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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.tracker.FieldsRequestParam;
import org.hisp.dhis.webapi.controller.tracker.PageRequestParams;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;

/**
 * Represents query parameters sent to {@link TrackedEntitiesExportController}.
 *
 * @author Giuseppe Nespolino
 */
@OpenApi.Shared(name = "TrackedEntityRequestParams")
@OpenApi.Property
@Data
@NoArgsConstructor
public class TrackedEntityRequestParams implements PageRequestParams, FieldsRequestParam {
  static final String DEFAULT_FIELDS_PARAM = "*,!relationships,!enrollments,!events,!programOwners";
  static final Fields DEFAULT_FIELDS_PARAM_PARSED = FieldsParser.parse(DEFAULT_FIELDS_PARAM);

  @OpenApi.Description(
"""
Get the given page.
""")
  @OpenApi.Property(defaultValue = "1")
  private Integer page;

  @OpenApi.Description(
"""
Get given number of items per page.
""")
  @OpenApi.Property(defaultValue = "50")
  private Integer pageSize;

  @OpenApi.Description(
"""
Get the total number of items and pages in the pager.

**Only enable this if absolutely necessary as this is resource intensive.** Use the pagers
`prev/nextPage` to determine if there is a previous or a next page instead.
""")
  private boolean totalPages = false;

  @OpenApi.Description(
"""
Get all items by specifying `paging=false`. Requests are paginated by default.

**Be aware that the performance is directly related to the amount of data requested. Larger pages
will take more time to return.**
""")
  private boolean paging = true;

  private List<OrderCriteria> order = new ArrayList<>();

  /** Comma separated list of attribute filters */
  private String filter;

  @OpenApi.Property({UID[].class, OrganisationUnit.class})
  private Set<UID> orgUnits = new HashSet<>();

  private OrganisationUnitSelectionMode orgUnitMode;

  /** The program tracked entities are enrolled in. */
  @OpenApi.Property({UID.class, Program.class})
  private UID program;

  /**
   * @deprecated use {@link #enrollmentStatus} instead
   */
  @Deprecated(since = "2.42")
  private EnrollmentStatus programStatus;

  /** Indicates whether the tracked entity is marked for follow up for the specified program. */
  private Boolean followUp;

  /** Start date for last updated. */
  private StartDateTime updatedAfter;

  /** End date for last updated. */
  private EndDateTime updatedBefore;

  /** The last updated duration filter. */
  private String updatedWithin;

  private EnrollmentStatus enrollmentStatus;

  /** The given Program start date. */
  private StartDateTime enrollmentEnrolledAfter;

  /** The given Program end date. */
  private EndDateTime enrollmentEnrolledBefore;

  /** Start date for incident in the given program. */
  private StartDateTime enrollmentOccurredAfter;

  /** End date for incident in the given program. */
  private EndDateTime enrollmentOccurredBefore;

  /** Only returns tracked entities of this type. */
  @OpenApi.Property({UID.class, TrackedEntityType.class})
  private UID trackedEntityType;

  @OpenApi.Property({UID[].class, TrackedEntity.class})
  private Set<UID> trackedEntities = new HashSet<>();

  /** Selection mode for user assignment of events. */
  private AssignedUserSelectionMode assignedUserMode;

  @OpenApi.Property({UID[].class, User.class})
  private Set<UID> assignedUsers = new HashSet<>();

  /** Program Stage UID, used for filtering TEs based on the selected Program Stage */
  @OpenApi.Property({UID.class, ProgramStage.class})
  private UID programStage;

  /** Status of any events in the specified program. */
  private EventStatus eventStatus;

  /** Start date for Event for the given Program. */
  private StartDateTime eventOccurredAfter;

  /** End date for Event for the given Program. */
  private EndDateTime eventOccurredBefore;

  /** Indicates whether to include soft-deleted elements */
  private boolean includeDeleted = false;

  /**
   * Potential Duplicate value for TE. If null, we don't check whether a TE is a potentialDuplicate
   * or not
   */
  private Boolean potentialDuplicate;

  @OpenApi.Property(value = String[].class)
  private Fields fields = DEFAULT_FIELDS_PARAM_PARSED;
}
