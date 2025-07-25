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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.lang.Boolean.TRUE;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate;
import org.hisp.dhis.tracker.export.Order;

@ToString
public class TrackedEntityQueryParams {

  /** Each attribute will affect the final SQL query. Some attributes are filtered on. */
  @Getter
  private final Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> filters = new HashMap<>();

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  @Getter private Set<OrganisationUnit> orgUnits = new HashSet<>();

  /**
   * Tracker program the tracked entity must be enrolled in. This should not be set when {@link
   * #accessibleTrackerPrograms} is set. The user must have data read access to this program.
   */
  @Getter private Program enrolledInTrackerProgram;

  /**
   * Tracker programs the user has data read access to. This should not be set when {@link
   * #enrolledInTrackerProgram} is set.
   */
  @Getter private List<Program> accessibleTrackerPrograms = List.of();

  /** Status of a tracked entities enrollment into a given program. */
  @Getter private EnrollmentStatus enrollmentStatus;

  /** Indicates whether tracked entity is marked for follow up for the specified program. */
  @Getter private Boolean followUp;

  /** Start date for last updated. */
  @Getter private Date lastUpdatedStartDate;

  /** End date for last updated. */
  @Getter private Date lastUpdatedEndDate;

  /** The last updated duration filter. */
  @Getter private String lastUpdatedDuration;

  /** Start date for enrollment in the given program. */
  @Getter private Date programEnrollmentStartDate;

  /** End date for enrollment in the given program. */
  @Getter private Date programEnrollmentEndDate;

  /** Start date for incident in the given program. */
  @Getter private Date programIncidentStartDate;

  /** End date for incident in the given program. */
  @Getter private Date programIncidentEndDate;

  /** Tracked entity of the instances in the response. */
  @Getter private TrackedEntityType trackedEntityType;

  /** Tracked entity types to fetch. */
  @Getter private List<TrackedEntityType> trackedEntityTypes = Lists.newArrayList();

  /** Selection mode for the specified organisation units */
  @Getter private OrganisationUnitSelectionMode orgUnitMode;

  @Getter private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  /** Set of te uids to explicitly select. */
  @Getter private Set<UID> trackedEntities = new HashSet<>();

  /** ProgramStage to be used in conjunction with eventstatus. */
  @Getter private ProgramStage programStage;

  /** Status of any events in the specified program. */
  @Getter private EventStatus eventStatus;

  /** Start date for event for the given program. */
  @Getter private Date eventStartDate;

  /** End date for event for the given program. */
  @Getter private Date eventEndDate;

  /** Indicates whether to include soft-deleted elements. Default to false */
  @Getter private boolean includeDeleted = false;

  /**
   * Potential Duplicate query parameter value. If null, we don't check whether a TE is a
   * potentialDuplicate or not
   */
  @Getter private Boolean potentialDuplicate;

  @Getter private final List<Order> order = new ArrayList<>();

  @Setter private boolean isSearchOutsideCaptureScope = false;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityQueryParams() {}

  public boolean hasTrackedEntities() {
    return CollectionUtils.isNotEmpty(this.trackedEntities);
  }

  public boolean hasFilterForEvents() {
    return this.getAssignedUserQueryParam().getMode() != AssignedUserSelectionMode.ALL
        || hasEventStatus();
  }

  /** Returns a list of attributes and filters combined. */
  public Set<UID> getFilterIds() {
    return filters.keySet().stream().map(UID::of).collect(Collectors.toSet());
  }

  /** Indicates whether these parameters specify any filters. */
  public boolean hasFilters() {
    return !filters.isEmpty();
  }

  /** Indicates whether these parameters specify any organisation units. */
  public boolean hasOrganisationUnits() {
    return orgUnits != null && !orgUnits.isEmpty();
  }

  /** Indicates whether these parameters specify a program. */
  public boolean hasEnrolledInTrackerProgram() {
    return enrolledInTrackerProgram != null;
  }

  /** Indicates whether these parameters specify an enrollment status. */
  public boolean hasEnrollmentStatus() {
    return enrollmentStatus != null;
  }

  /**
   * Indicates whether these parameters specify follow up for the given program. Follow up can be
   * specified as true or false.
   */
  public boolean hasFollowUp() {
    return followUp != null;
  }

  /** Indicates whether these parameters specify a last updated start date. */
  public boolean hasLastUpdatedStartDate() {
    return lastUpdatedStartDate != null;
  }

  /** Indicates whether these parameters specify a last updated end date. */
  public boolean hasLastUpdatedEndDate() {
    return lastUpdatedEndDate != null;
  }

  /** Indicates whether these parameters have a lastUpdatedDuration filter. */
  public boolean hasLastUpdatedDuration() {
    return lastUpdatedDuration != null;
  }

  /** Indicates whether these parameters specify a program enrollment start date. */
  public boolean hasProgramEnrollmentStartDate() {
    return programEnrollmentStartDate != null;
  }

  /** Indicates whether these parameters specify a program enrollment end date. */
  public boolean hasProgramEnrollmentEndDate() {
    return programEnrollmentEndDate != null;
  }

  /** Indicates whether these parameters specify a program incident start date. */
  public boolean hasProgramIncidentStartDate() {
    return programIncidentStartDate != null;
  }

  /** Indicates whether these parameters specify a program incident end date. */
  public boolean hasProgramIncidentEndDate() {
    return programIncidentEndDate != null;
  }

  /** Indicates whether these parameters specify a tracked entity. */
  public boolean hasTrackedEntityType() {
    return trackedEntityType != null;
  }

  /** Indicates whether these parameters are of the given organisation unit mode. */
  public boolean isOrganisationUnitMode(OrganisationUnitSelectionMode mode) {
    return orgUnitMode != null && orgUnitMode.equals(mode);
  }

  /** Indicates whether these parameters specify a programStage. */
  public boolean hasProgramStage() {
    return programStage != null;
  }

  /** Indicates whether this params specifies an event status. */
  public boolean hasEventStatus() {
    return eventStatus != null;
  }

  /**
   * Indicates whether the event status specified for the params is equal to the given event status.
   */
  public boolean isEventStatus(EventStatus eventStatus) {
    return this.eventStatus == eventStatus;
  }

  /** Check whether we are filtering for potential duplicate property. */
  public boolean hasPotentialDuplicateFilter() {
    return potentialDuplicate != null;
  }

  /**
   * Checks if there is at least one unique filter in the params. In attributes or filters.
   *
   * @return true if there is at least one unique filter in filters/attributes, false otherwise.
   */
  public boolean hasUniqueFilter() {
    if (!hasFilters()) {
      return false;
    }

    for (TrackedEntityAttribute attribute : filters.keySet()) {
      if (TRUE.equals(attribute.isUnique())) {
        return true;
      }
    }

    return false;
  }

  /** Returns attributes that are only ordered by and not present in any filter. */
  public Set<TrackedEntityAttribute> getLeftJoinAttributes() {
    return SetUtils.union(getOrderAttributes(), filters.keySet());
  }

  public TrackedEntityQueryParams addOrgUnits(Set<OrganisationUnit> orgUnits) {
    this.orgUnits.addAll(orgUnits);
    return this;
  }

  public TrackedEntityQueryParams setOrgUnits(Set<OrganisationUnit> accessibleOrgUnits) {
    this.orgUnits = accessibleOrgUnits;
    return this;
  }

  public TrackedEntityQueryParams setEnrolledInTrackerProgram(Program enrolledInTrackerProgram) {
    this.enrolledInTrackerProgram = enrolledInTrackerProgram;
    return this;
  }

  public TrackedEntityQueryParams setAccessibleTrackerPrograms(
      List<Program> accessibleTrackerPrograms) {
    this.accessibleTrackerPrograms = accessibleTrackerPrograms;
    return this;
  }

  public TrackedEntityQueryParams setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
    return this;
  }

  public TrackedEntityQueryParams setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  public TrackedEntityQueryParams setFollowUp(Boolean followUp) {
    this.followUp = followUp;
    return this;
  }

  public TrackedEntityQueryParams setPotentialDuplicate(Boolean potentialDuplicate) {
    this.potentialDuplicate = potentialDuplicate;
    return this;
  }

  public TrackedEntityQueryParams setLastUpdatedStartDate(Date lastUpdatedStartDate) {
    this.lastUpdatedStartDate = lastUpdatedStartDate;
    return this;
  }

  public TrackedEntityQueryParams setLastUpdatedEndDate(Date lastUpdatedEndDate) {
    this.lastUpdatedEndDate = lastUpdatedEndDate;
    return this;
  }

  public TrackedEntityQueryParams setLastUpdatedDuration(String lastUpdatedDuration) {
    this.lastUpdatedDuration = lastUpdatedDuration;
    return this;
  }

  public TrackedEntityQueryParams setProgramEnrollmentStartDate(Date programEnrollmentStartDate) {
    this.programEnrollmentStartDate = programEnrollmentStartDate;
    return this;
  }

  public TrackedEntityQueryParams setProgramEnrollmentEndDate(Date programEnrollmentEndDate) {
    this.programEnrollmentEndDate = programEnrollmentEndDate;
    return this;
  }

  public TrackedEntityQueryParams setProgramIncidentStartDate(Date programIncidentStartDate) {
    this.programIncidentStartDate = programIncidentStartDate;
    return this;
  }

  public TrackedEntityQueryParams setProgramIncidentEndDate(Date programIncidentEndDate) {
    this.programIncidentEndDate = programIncidentEndDate;
    return this;
  }

  public TrackedEntityQueryParams setTrackedEntityType(TrackedEntityType trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
    return this;
  }

  public TrackedEntityQueryParams setOrgUnitMode(OrganisationUnitSelectionMode orgUnitMode) {
    this.orgUnitMode = orgUnitMode;
    return this;
  }

  public TrackedEntityQueryParams setAssignedUserQueryParam(
      AssignedUserQueryParam assignedUserQueryParam) {
    this.assignedUserQueryParam = assignedUserQueryParam;
    return this;
  }

  public TrackedEntityQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public TrackedEntityQueryParams setEventStartDate(Date eventStartDate) {
    this.eventStartDate = eventStartDate;
    return this;
  }

  public TrackedEntityQueryParams setEventEndDate(Date eventEndDate) {
    this.eventEndDate = eventEndDate;
    return this;
  }

  public TrackedEntityQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  /**
   * Filter the given tracked entity attribute {@code tea} using the specified {@link QueryFilter}
   * that consist of an operator and a value.
   */
  public TrackedEntityQueryParams filterBy(TrackedEntityAttribute tea, List<QueryFilter> filters)
      throws BadRequestException {
    this.filters.putIfAbsent(tea, new ArrayList<>());
    for (QueryFilter filter : filters) {
      FilterJdbcPredicate predicate = FilterJdbcPredicate.of(tea, filter);
      this.filters.get(tea).add(predicate);
    }

    return this;
  }

  /** Order by an event field of the given {@code field} name in given sort {@code direction}. */
  public TrackedEntityQueryParams orderBy(String field, SortDirection direction) {
    this.order.add(new Order(field, direction));
    return this;
  }

  /**
   * Order by the given tracked entity attribute {@code tea} in given sort {@code direction}.
   * Attributes are added to the attribute list if neither are present in the attribute list nor the
   * filter list.
   */
  public TrackedEntityQueryParams orderBy(TrackedEntityAttribute tea, SortDirection direction) {
    this.order.add(new Order(tea, direction));
    return this;
  }

  private Set<TrackedEntityAttribute> getOrderAttributes() {
    return order.stream()
        .filter(o -> o.getField() instanceof TrackedEntityAttribute)
        .map(o -> (TrackedEntityAttribute) o.getField())
        .collect(Collectors.toSet());
  }

  public TrackedEntityQueryParams setTrackedEntities(Set<UID> trackedEntities) {
    this.trackedEntities = trackedEntities;
    return this;
  }

  public TrackedEntityQueryParams setTrackedEntityTypes(
      List<TrackedEntityType> trackedEntityTypes) {
    this.trackedEntityTypes = trackedEntityTypes;
    return this;
  }

  public boolean isSearchOutsideCaptureScope() {
    return isSearchOutsideCaptureScope;
  }
}
