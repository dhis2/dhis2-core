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
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.tracker.export.PageRequestParams;
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
public class TrackedEntityRequestParams implements PageRequestParams {
  static final String DEFAULT_FIELDS_PARAM = "*,!relationships,!enrollments,!events,!programOwners";

  @OpenApi.Property(defaultValue = "1")
  private Integer page;

  @OpenApi.Property(defaultValue = "50")
  private Integer pageSize;

  @OpenApi.Property(defaultValue = "false")
  private Boolean totalPages = false;

  /**
   * @deprecated use {@link #paging} instead
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property(defaultValue = "false")
  private Boolean skipPaging;

  // TODO(tracker): set paging=true once skipPaging is removed. Both cannot have a default right
  // now. This would lead to invalid parameters if the user passes the other param i.e.
  // skipPaging==paging.
  // PageRequestParams.isPaged handles the default case of skipPaging==paging==null => paging
  // enabled
  @OpenApi.Property(defaultValue = "true")
  private Boolean paging;

  private List<OrderCriteria> order = new ArrayList<>();

  @Deprecated(forRemoval = true, since = "2.41")
  // Removed field without previous deprecation.
  // It is still here in order to be validated and warn the client about the removal
  private String query;

  @Deprecated(forRemoval = true, since = "2.41")
  // Removed field without previous deprecation.
  // It is still here in order to be validated and warn the client about the removal
  private String attribute;

  /** Comma separated list of attribute filters */
  private String filter;

  /**
   * Semicolon-delimited list of organisation unit UIDs.
   *
   * @deprecated use {@link #orgUnits} instead which is comma instead of semicolon separated.
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID[].class, OrganisationUnit.class})
  private String orgUnit;

  @OpenApi.Property({UID[].class, OrganisationUnit.class})
  private Set<UID> orgUnits = new HashSet<>();

  /**
   * @deprecated use {@link #orgUnitMode} instead.
   */
  @Deprecated(since = "2.41")
  private OrganisationUnitSelectionMode ouMode;

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

  /**
   * Semicolon-delimited list of tracked entity UIDs
   *
   * @deprecated use {@link #trackedEntities} instead which is comma instead of semicolon separated.
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID[].class, TrackedEntity.class})
  private String trackedEntity;

  @OpenApi.Property({UID[].class, TrackedEntity.class})
  private Set<UID> trackedEntities = new HashSet<>();

  /** Selection mode for user assignment of events. */
  private AssignedUserSelectionMode assignedUserMode;

  /**
   * Semicolon-delimited list of user UIDs to filter based on events assigned to the users.
   *
   * @deprecated use {@link #assignedUsers} instead which is comma instead of semicolon separated.
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID[].class, User.class})
  private String assignedUser;

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

  @Deprecated(forRemoval = true, since = "2.41")
  // Removed field without previous deprecation.
  // It is still here in order to be validated and warn the client about the removal
  private String includeAllAttributes;

  @OpenApi.Property(value = String[].class)
  private List<FieldPath> fields = FieldFilterParser.parse(DEFAULT_FIELDS_PARAM);
}
