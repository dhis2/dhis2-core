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
package org.hisp.dhis.tracker.export.enrollment;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.Order;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Accessors(chain = true)
class EnrollmentQueryParams {

  /** Set of enrollment uids to explicitly select. */
  private Set<UID> enrollments = new HashSet<>();

  /** Last updated for enrollment. */
  private Date lastUpdated;

  /** The last updated duration filter. */
  private String lastUpdatedDuration;

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  private Set<OrganisationUnit> organisationUnits = new HashSet<>();

  /** Selection mode for the specified organisation units. */
  private OrganisationUnitSelectionMode organisationUnitMode;

  /** Program for which instances in the response must be enrolled in. */
  private Program program;

  /** Status of a tracked entities enrollment into a given program. */
  private EnrollmentStatus enrollmentStatus;

  /** Indicates whether tracked entity is marked for follow up for the specified program. */
  private Boolean followUp;

  /** Start date for enrollment in the given program. */
  private Date programStartDate;

  /** End date for enrollment in the given program. */
  private Date programEndDate;

  /** Tracked entity of the instances in the response. */
  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  /** Indicates whether to include soft-deleted enrollments */
  private boolean includeDeleted;

  private List<Order> order;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public EnrollmentQueryParams() {}

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Adds an organisation unit to the parameters. */
  public void addOrganisationUnit(OrganisationUnit unit) {
    this.organisationUnits.add(unit);
  }

  public void addOrganisationUnits(Set<OrganisationUnit> orgUnits) {
    this.organisationUnits.addAll(orgUnits);
  }

  /** Indicates whether this params specifies last updated. */
  public boolean hasLastUpdated() {
    return lastUpdated != null;
  }

  /** Indicates whether this parameters has a lastUpdatedDuration filter. */
  public boolean hasLastUpdatedDuration() {
    return lastUpdatedDuration != null;
  }

  /** Indicates whether this params specifies any organisation units. */
  public boolean hasOrganisationUnits() {
    return organisationUnits != null && !organisationUnits.isEmpty();
  }

  /** Indicates whether this params specifies a program. */
  public boolean hasProgram() {
    return program != null;
  }

  /** Indicates whether this params specifies an enrollment status. */
  public boolean hasEnrollmentStatus() {
    return enrollmentStatus != null;
  }

  /**
   * Indicates whether this params specifies follow up for the given program. Follow up can be
   * specified as true or false.
   */
  public boolean hasFollowUp() {
    return followUp != null;
  }

  /** Indicates whether this params specifies a program start date. */
  public boolean hasProgramStartDate() {
    return programStartDate != null;
  }

  /** Indicates whether this params specifies a program end date. */
  public boolean hasProgramEndDate() {
    return programEndDate != null;
  }

  /** Indicates whether this params specifies a tracked entity. */
  public boolean hasTrackedEntityType() {
    return trackedEntityType != null;
  }

  /** Indicates whether this params specifies a tracked entity. */
  public boolean hasTrackedEntity() {
    return this.trackedEntity != null;
  }

  public boolean hasEnrollmentUids() {
    return isNotEmpty(this.enrollments);
  }

  /** Indicates whether this params is of the given organisation unit mode. */
  public boolean isOrganisationUnitMode(OrganisationUnitSelectionMode mode) {
    return organisationUnitMode != null && organisationUnitMode.equals(mode);
  }

  /**
   * Order by an enrollment field of the given {@code field} name in given sort {@code direction}.
   */
  public EnrollmentQueryParams orderBy(String field, SortDirection direction) {
    this.order.add(new Order(field, direction));
    return this;
  }
}
