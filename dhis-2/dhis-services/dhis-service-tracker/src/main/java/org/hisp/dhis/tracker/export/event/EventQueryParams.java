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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.JdbcPredicate;
import org.hisp.dhis.tracker.export.Order;

/**
 * @author Lars Helge Overland
 */
class EventQueryParams {
  /**
   * Program the event must be enrolled in. This should not be set when {@link #accessiblePrograms}
   * is set. The user must have data read access to this program.
   */
  @Getter private Program enrolledInProgram;

  /**
   * Programs the user has data read access to. This should not be set when {@link
   * #enrolledInProgram} is set.
   */
  @Getter private List<Program> accessiblePrograms = List.of();

  /**
   * Program stage the event must be enrolled in. This should not be set when {@link
   * #accessiblePrograms} is set. The user must have data read access to this program.
   */
  @Getter private ProgramStage programStage;

  /**
   * Program stages the user has data read access to. This should not be set when {@link
   * #programStage} is set.
   */
  @Getter private List<ProgramStage> accessibleProgramStages = List.of();

  @Getter private EnrollmentStatus enrollmentStatus;

  @Getter private ProgramType programType;

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
  private final Map<TrackedEntityAttribute, List<JdbcPredicate>> attributes = new HashMap<>();

  /**
   * Each data element will affect the final SQL query. Some data elements are filtered on, while
   * data elements added via {@link #orderBy(DataElement, SortDirection)} will be ordered by.
   */
  @Getter private final Map<DataElement, List<JdbcPredicate>> dataElements = new HashMap<>();

  private boolean hasDataElementFilter;

  @Getter private boolean includeDeleted;

  @Getter private Set<UID> enrollments;

  @Getter private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  @Getter private TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder().build();

  public EventQueryParams() {}

  public boolean hasEnrolledInProgram() {
    return enrolledInProgram != null;
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

  public EventQueryParams setEnrolledInProgram(Program enrolledInProgram) {
    this.enrolledInProgram = enrolledInProgram;
    return this;
  }

  public EventQueryParams setAccessiblePrograms(List<Program> accessiblePrograms) {
    this.accessiblePrograms = accessiblePrograms;
    return this;
  }

  public EventQueryParams setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
    return this;
  }

  public EventQueryParams setAccessibleProgramStages(List<ProgramStage> accessibleProgramStages) {
    this.accessibleProgramStages = accessibleProgramStages;
    return this;
  }

  public EventQueryParams setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  public EventQueryParams setProgramType(ProgramType programType) {
    this.programType = programType;
    return this;
  }

  public EventQueryParams setFollowUp(Boolean followUp) {
    this.followUp = followUp;
    return this;
  }

  public EventQueryParams setOrgUnit(OrganisationUnit orgUnit) {
    this.orgUnit = orgUnit;
    return this;
  }

  public EventQueryParams setOrgUnitMode(OrganisationUnitSelectionMode orgUnitMode) {
    this.orgUnitMode = orgUnitMode;
    return this;
  }

  /**
   * Assigns the user query params
   *
   * @param assignedUserQueryParam assigned user query params
   * @return this
   */
  public EventQueryParams setAssignedUserQueryParam(AssignedUserQueryParam assignedUserQueryParam) {
    this.assignedUserQueryParam = assignedUserQueryParam;
    return this;
  }

  public EventQueryParams setTrackedEntity(TrackedEntity trackedEntity) {
    this.trackedEntity = trackedEntity;
    return this;
  }

  public EventQueryParams setOccurredStartDate(Date occurredStartDate) {
    this.occurredStartDate = occurredStartDate;
    return this;
  }

  public EventQueryParams setOccurredEndDate(Date occurredEndDate) {
    this.occurredEndDate = occurredEndDate;
    return this;
  }

  public EventQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public EventQueryParams setUpdatedAtStartDate(Date updatedAtStartDate) {
    this.updatedAtStartDate = updatedAtStartDate;
    return this;
  }

  public EventQueryParams setUpdatedAtEndDate(Date updatedAtEndDate) {
    this.updatedAtEndDate = updatedAtEndDate;
    return this;
  }

  public EventQueryParams setUpdatedAtDuration(String updatedAtDuration) {
    this.updatedAtDuration = updatedAtDuration;
    return this;
  }

  public EventQueryParams setScheduledStartDate(Date scheduleAtStartDate) {
    this.scheduleAtStartDate = scheduleAtStartDate;
    return this;
  }

  public EventQueryParams setScheduledEndDate(Date scheduleAtEndDate) {
    this.scheduleAtEndDate = scheduleAtEndDate;
    return this;
  }

  public EventQueryParams setEnrollmentEnrolledBefore(Date enrollmentEnrolledBefore) {
    this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
    return this;
  }

  public EventQueryParams setEnrollmentEnrolledAfter(Date enrollmentEnrolledAfter) {
    this.enrollmentEnrolledAfter = enrollmentEnrolledAfter;
    return this;
  }

  public EventQueryParams setEnrollmentOccurredBefore(Date enrollmentOccurredBefore) {
    this.enrollmentOccurredBefore = enrollmentOccurredBefore;
    return this;
  }

  public EventQueryParams setEnrollmentOccurredAfter(Date enrollmentOccurredAfter) {
    this.enrollmentOccurredAfter = enrollmentOccurredAfter;
    return this;
  }

  public List<Order> getOrder() {
    return Collections.unmodifiableList(this.order);
  }

  private Map<TrackedEntityAttribute, List<JdbcPredicate>> getOrderAttributes() {
    return order.stream()
        .filter(o -> o.getField() instanceof TrackedEntityAttribute)
        .map(o -> (TrackedEntityAttribute) o.getField())
        .collect(Collectors.toMap(tea -> tea, tea -> List.of()));
  }

  /** Order by an event field of the given {@code field} name in given sort {@code direction}. */
  public EventQueryParams orderBy(String field, SortDirection direction) {
    this.order.add(new Order(field, direction));
    return this;
  }

  /** Order by the given data element {@code de} in given sort {@code direction}. */
  public EventQueryParams orderBy(DataElement de, SortDirection direction) {
    this.order.add(new Order(de, direction));
    return this;
  }

  /** Order by the given tracked entity attribute {@code tea} in given sort {@code direction}. */
  public EventQueryParams orderBy(TrackedEntityAttribute tea, SortDirection direction) {
    this.order.add(new Order(tea, direction));
    return this;
  }

  public EventQueryParams setCategoryOptionCombo(CategoryOptionCombo categoryOptionCombo) {
    this.categoryOptionCombo = categoryOptionCombo;
    return this;
  }

  public EventQueryParams setEvents(Set<UID> events) {
    this.events = events;
    return this;
  }

  /** Returns attributes that are only ordered by and not present in any filter. */
  public Set<TrackedEntityAttribute> leftJoinAttributes() {
    return SetUtils.union(getOrderAttributes().keySet(), this.attributes.keySet());
  }

  public EventQueryParams filterBy(
      @Nonnull TrackedEntityAttribute tea, @Nonnull QueryFilter filter) {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    this.attributes.get(tea).add(JdbcPredicate.of(tea, filter));
    return this;
  }

  public EventQueryParams filterBy(@Nonnull TrackedEntityAttribute tea) {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    return this;
  }

  public EventQueryParams filterBy(@Nonnull DataElement de, @Nonnull QueryFilter filter) {
    this.dataElements.putIfAbsent(de, new ArrayList<>());
    this.dataElements.get(de).add(JdbcPredicate.of(de, filter, "ev"));
    this.hasDataElementFilter = true;
    return this;
  }

  public EventQueryParams filterBy(DataElement de) {
    this.dataElements.putIfAbsent(
        de, List.of(JdbcPredicate.of(de, new QueryFilter(QueryOperator.NNULL), "ev")));
    return this;
  }

  public EventQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public Set<UID> getEnrollments() {
    return enrollments;
  }

  public EventQueryParams setEnrollments(Set<UID> enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  public EventQueryParams setIdSchemeParams(TrackerIdSchemeParams idSchemeParams) {
    this.idSchemeParams = idSchemeParams;
    return this;
  }
}
