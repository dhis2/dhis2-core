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
package org.hisp.dhis.tracker.export.trackerevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.collections4.SetUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate;
import org.hisp.dhis.tracker.export.Order;

/**
 * @author Lars Helge Overland
 */
class TrackerEventQueryParams {
  /**
   * Tracker program the event tracked entity must be enrolled in. This should not be set when
   * {@link #accessibleTrackerPrograms} is set. The user must have data read access to this program.
   */
  @Getter private Program enrolledInTrackerProgram;

  /**
   * Tracker programs the user has data read access to. This should not be set when {@link
   * #enrolledInTrackerProgram} is set.
   */
  @Getter private List<Program> accessibleTrackerPrograms = List.of();

  /**
   * Program stage the event must be tracked entity must be enrolled in. This should not be set when
   * {@link #accessibleTrackerPrograms} is set. The user must have data read access to this program.
   */
  // TODO Is this name good enough?
  @Getter private ProgramStage programStage;

  /**
   * Program stages the user has data read access to. This should not be set when {@link
   * #programStage} is set.
   */
  @Getter private List<ProgramStage> accessibleTrackerProgramStages = List.of();

  @Getter private EnrollmentStatus enrollmentStatus;

  @Getter private Boolean followUp;

  @Getter private OrganisationUnit orgUnit;

  @Getter private OrganisationUnitSelectionMode orgUnitMode;

  @Getter private TrackedEntity trackedEntity;

  @Getter private Date occurredStartDate;

  @Getter private Date occurredEndDate;

  @Getter private EventStatus eventStatus;

  @Getter private Date updatedAtStartDate;

  @Getter private Date updatedAtEndDate;

  /** The last updated duration filter. */
  @Getter private String updatedAtDuration;

  @Getter private Date scheduleAtStartDate;

  @Getter private Date scheduleAtEndDate;

  @Getter private Date enrollmentEnrolledBefore;

  @Getter private Date enrollmentEnrolledAfter;

  @Getter private Date enrollmentOccurredBefore;

  @Getter private Date enrollmentOccurredAfter;

  @Getter private CategoryOptionCombo categoryOptionCombo;

  /**
   * Events can be ordered by field names (given as {@link String}), data elements (given as {@link
   * DataElement}) and tracked entity attributes (given as {@link TrackedEntityAttribute}). It is
   * crucial for the order values to stay in one collection as their order needs to be kept as
   * provided by the user. We cannot come up with a type-safe type that captures the above order
   * features and that can be used in a generic collection such as a {@link List} (see typesafe
   * heterogeneous container). We therefore provide {@link #orderBy(String, SortDirection)}, {@link
   * #orderBy(DataElement, SortDirection)} and {@link #orderBy(TrackedEntityAttribute,
   * SortDirection)} to advocate the types that can be ordered by while storing the order in a
   * single List of {@link Order}.
   */
  private final List<Order> order = new ArrayList<>();

  @Getter private Set<UID> events = new HashSet<>();

  /** Each attribute will affect the final SQL query. Some attributes are filtered on. */
  @Getter
  private final Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> attributes = new HashMap<>();

  /**
   * Each data element will affect the final SQL query. Some data elements are filtered on, while
   * data elements added via {@link #orderBy(DataElement, SortDirection)} will be ordered by.
   */
  @Getter private final Map<DataElement, List<FilterJdbcPredicate>> dataElements = new HashMap<>();

  private boolean hasDataElementFilter;

  @Getter private boolean includeDeleted;

  @Getter private Set<UID> accessiblePrograms;

  @Getter private Set<UID> accessibleProgramStages;

  @Getter private Set<UID> enrollments;

  @Getter private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  @Getter private TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder().build();

  public TrackerEventQueryParams() {}

  public boolean hasEnrolledInTrackerProgram() {
    return enrolledInTrackerProgram != null;
  }

  public boolean hasProgramStage() {
    return programStage != null;
  }

  /** Indicates whether this parameters specifies a last updated start date. */
  public boolean hasUpdatedAtStartDate() {
    return updatedAtStartDate != null;
  }

  /** Indicates whether this parameters specifies a last updated end date. */
  public boolean hasUpdatedAtEndDate() {
    return updatedAtEndDate != null;
  }

  /** Indicates whether this parameters has a UpdatedAtDuration filter. */
  public boolean hasUpdatedAtDuration() {
    return updatedAtDuration != null;
  }

  /**
   * Returns true if any data element filter has been added using {@link #filterBy(DataElement,
   * QueryFilter)}.
   */
  public boolean hasDataElementFilter() {
    return this.hasDataElementFilter;
  }

  public TrackerEventQueryParams setEnrolledInTrackerProgram(Program enrolledInTrackerProgram) {
    this.enrolledInTrackerProgram = enrolledInTrackerProgram;
    return this;
  }

  public TrackerEventQueryParams setAccessibleTrackerPrograms(
      List<Program> accessibleTrackerPrograms) {
    this.accessibleTrackerPrograms = accessibleTrackerPrograms;
    return this;
  }

  public TrackerEventQueryParams setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
    return this;
  }

  public TrackerEventQueryParams setAccessibleTrackerProgramStages(
      List<ProgramStage> accessibleTrackerProgramStages) {
    this.accessibleTrackerProgramStages = accessibleTrackerProgramStages;
    return this;
  }

  public TrackerEventQueryParams setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  public TrackerEventQueryParams setFollowUp(Boolean followUp) {
    this.followUp = followUp;
    return this;
  }

  public TrackerEventQueryParams setOrgUnit(OrganisationUnit orgUnit) {
    this.orgUnit = orgUnit;
    return this;
  }

  public TrackerEventQueryParams setOrgUnitMode(OrganisationUnitSelectionMode orgUnitMode) {
    this.orgUnitMode = orgUnitMode;
    return this;
  }

  /**
   * Assigns the user query params
   *
   * @param assignedUserQueryParam assigned user query params
   * @return this
   */
  public TrackerEventQueryParams setAssignedUserQueryParam(
      AssignedUserQueryParam assignedUserQueryParam) {
    this.assignedUserQueryParam = assignedUserQueryParam;
    return this;
  }

  public TrackerEventQueryParams setTrackedEntity(TrackedEntity trackedEntity) {
    this.trackedEntity = trackedEntity;
    return this;
  }

  public TrackerEventQueryParams setOccurredStartDate(Date occurredStartDate) {
    this.occurredStartDate = occurredStartDate;
    return this;
  }

  public TrackerEventQueryParams setOccurredEndDate(Date occurredEndDate) {
    this.occurredEndDate = occurredEndDate;
    return this;
  }

  public TrackerEventQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public TrackerEventQueryParams setUpdatedAtStartDate(Date updatedAtStartDate) {
    this.updatedAtStartDate = updatedAtStartDate;
    return this;
  }

  public TrackerEventQueryParams setUpdatedAtEndDate(Date updatedAtEndDate) {
    this.updatedAtEndDate = updatedAtEndDate;
    return this;
  }

  public TrackerEventQueryParams setUpdatedAtDuration(String updatedAtDuration) {
    this.updatedAtDuration = updatedAtDuration;
    return this;
  }

  public TrackerEventQueryParams setScheduledStartDate(Date scheduleAtStartDate) {
    this.scheduleAtStartDate = scheduleAtStartDate;
    return this;
  }

  public TrackerEventQueryParams setScheduledEndDate(Date scheduleAtEndDate) {
    this.scheduleAtEndDate = scheduleAtEndDate;
    return this;
  }

  public TrackerEventQueryParams setEnrollmentEnrolledBefore(Date enrollmentEnrolledBefore) {
    this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
    return this;
  }

  public TrackerEventQueryParams setEnrollmentEnrolledAfter(Date enrollmentEnrolledAfter) {
    this.enrollmentEnrolledAfter = enrollmentEnrolledAfter;
    return this;
  }

  public TrackerEventQueryParams setEnrollmentOccurredBefore(Date enrollmentOccurredBefore) {
    this.enrollmentOccurredBefore = enrollmentOccurredBefore;
    return this;
  }

  public TrackerEventQueryParams setEnrollmentOccurredAfter(Date enrollmentOccurredAfter) {
    this.enrollmentOccurredAfter = enrollmentOccurredAfter;
    return this;
  }

  public List<Order> getOrder() {
    return Collections.unmodifiableList(this.order);
  }

  private Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> getOrderAttributes() {
    return order.stream()
        .filter(o -> o.getField() instanceof TrackedEntityAttribute)
        .map(o -> (TrackedEntityAttribute) o.getField())
        .collect(Collectors.toMap(tea -> tea, tea -> List.of()));
  }

  /** Order by an event field of the given {@code field} name in given sort {@code direction}. */
  public TrackerEventQueryParams orderBy(String field, SortDirection direction) {
    this.order.add(new Order(field, direction));
    return this;
  }

  /** Order by the given data element {@code de} in given sort {@code direction}. */
  public TrackerEventQueryParams orderBy(DataElement de, SortDirection direction) {
    this.order.add(new Order(de, direction));
    return this;
  }

  /** Order by the given tracked entity attribute {@code tea} in given sort {@code direction}. */
  public TrackerEventQueryParams orderBy(TrackedEntityAttribute tea, SortDirection direction) {
    this.order.add(new Order(tea, direction));
    return this;
  }

  public TrackerEventQueryParams setCategoryOptionCombo(CategoryOptionCombo categoryOptionCombo) {
    this.categoryOptionCombo = categoryOptionCombo;
    return this;
  }

  public TrackerEventQueryParams setEvents(Set<UID> events) {
    this.events = events;
    return this;
  }

  /** Returns attributes that are only ordered by and not present in any filter. */
  public Set<TrackedEntityAttribute> leftJoinAttributes() {
    return SetUtils.union(getOrderAttributes().keySet(), this.attributes.keySet());
  }

  public TrackerEventQueryParams filterBy(
      @Nonnull TrackedEntityAttribute tea, @Nonnull QueryFilter filter) throws BadRequestException {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    this.attributes.get(tea).add(FilterJdbcPredicate.of(tea, filter));
    return this;
  }

  public TrackerEventQueryParams filterBy(@Nonnull TrackedEntityAttribute tea) {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    return this;
  }

  public TrackerEventQueryParams filterBy(@Nonnull DataElement de, @Nonnull QueryFilter filter)
      throws BadRequestException {
    this.dataElements.putIfAbsent(de, new ArrayList<>());
    this.dataElements.get(de).add(FilterJdbcPredicate.of(de, filter, "ev"));
    this.hasDataElementFilter = true;
    return this;
  }

  public TrackerEventQueryParams filterBy(DataElement de) throws BadRequestException {
    this.dataElements.putIfAbsent(
        de, List.of(FilterJdbcPredicate.of(de, new QueryFilter(QueryOperator.NNULL), "ev")));
    return this;
  }

  public TrackerEventQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public TrackerEventQueryParams setAccessiblePrograms(Set<UID> accessiblePrograms) {
    this.accessiblePrograms = accessiblePrograms;
    return this;
  }

  public TrackerEventQueryParams setAccessibleProgramStages(Set<UID> accessibleProgramStages) {
    this.accessibleProgramStages = accessibleProgramStages;
    return this;
  }

  public boolean hasSecurityFilter() {
    return accessiblePrograms != null && accessibleProgramStages != null;
  }

  public TrackerEventQueryParams setEnrollments(Set<UID> enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  public TrackerEventQueryParams setIdSchemeParams(TrackerIdSchemeParams idSchemeParams) {
    this.idSchemeParams = idSchemeParams;
    return this;
  }
}
