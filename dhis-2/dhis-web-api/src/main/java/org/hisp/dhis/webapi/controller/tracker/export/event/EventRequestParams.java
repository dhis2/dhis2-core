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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.tracker.export.PageRequestParams;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;

/**
 * Represents query parameters sent to {@link EventsExportController}.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@OpenApi.Shared(name = "EventRequestParams")
@OpenApi.Property
@Data
@NoArgsConstructor
public class EventRequestParams implements PageRequestParams {
  static final String DEFAULT_FIELDS_PARAM = "*,!relationships";

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

  @OpenApi.Property({UID.class, Program.class})
  private UID program;

  @OpenApi.Property({UID.class, ProgramStage.class})
  private UID programStage;

  /**
   * @deprecated use {@link #enrollmentStatus} instead
   */
  @Deprecated(since = "2.42")
  private EnrollmentStatus programStatus;

  private Boolean followUp;

  @OpenApi.Property({UID.class, TrackedEntity.class})
  private UID trackedEntity;

  @OpenApi.Property({UID.class, OrganisationUnit.class})
  private UID orgUnit;

  /**
   * @deprecated use {@link #orgUnitMode} instead.
   */
  @Deprecated(since = "2.41")
  private OrganisationUnitSelectionMode ouMode;

  private OrganisationUnitSelectionMode orgUnitMode;

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

  private StartDateTime occurredAfter;

  private EndDateTime occurredBefore;

  private StartDateTime scheduledAfter;

  private EndDateTime scheduledBefore;

  private StartDateTime updatedAfter;

  private EndDateTime updatedBefore;

  private String updatedWithin;

  private EnrollmentStatus enrollmentStatus;

  private StartDateTime enrollmentEnrolledAfter;

  private EndDateTime enrollmentEnrolledBefore;

  private StartDateTime enrollmentOccurredAfter;

  private EndDateTime enrollmentOccurredBefore;

  private EventStatus status;

  /**
   * @deprecated use {@link #attributeCategoryCombo}
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID.class, CategoryCombo.class})
  private UID attributeCc;

  @OpenApi.Property({UID.class, CategoryCombo.class})
  private UID attributeCategoryCombo;

  /**
   * Semicolon-delimited list of category option UIDs.
   *
   * @deprecated use {@link #attributeCategoryOptions} instead which is comma instead of semicolon
   *     separated.
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID[].class, CategoryOption.class})
  private String attributeCos;

  @OpenApi.Property({UID[].class, CategoryOption.class})
  private Set<UID> attributeCategoryOptions = new HashSet<>();

  private boolean includeDeleted = false;

  /**
   * Semicolon-delimited list of event UIDs.
   *
   * @deprecated use {@link #events} instead which is comma instead of semicolon separated.
   */
  @Deprecated(since = "2.41")
  @OpenApi.Property({UID[].class, Event.class})
  private String event;

  @OpenApi.Property({UID[].class, Event.class})
  private Set<UID> events = new HashSet<>();

  /** Comma separated list of data element filters */
  private String filter;

  /** Comma separated list of attribute filters */
  private String filterAttributes;

  @OpenApi.Property({UID[].class, Enrollment.class})
  private Set<UID> enrollments = new HashSet<>();

  @OpenApi.Property(value = String[].class)
  private List<FieldPath> fields = FieldFilterParser.parse(DEFAULT_FIELDS_PARAM);
}
