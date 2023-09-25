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
import lombok.Getter;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

/**
 * @author Lars Helge Overland
 */
class EventQueryParams {
  private Program program;

  private ProgramStage programStage;

  private ProgramStatus programStatus;

  private ProgramType programType;

  private Boolean followUp;

  private OrganisationUnit orgUnit;

  private OrganisationUnitSelectionMode orgUnitMode;

  private TrackedEntity trackedEntity;

  private Date startDate;

  private Date endDate;

  private EventStatus eventStatus;

  private Date updatedAtStartDate;

  private Date updatedAtEndDate;

  /** The last updated duration filter. */
  private String updatedAtDuration;

  private Date scheduleAtStartDate;

  private Date scheduleAtEndDate;

  private Date enrollmentEnrolledBefore;

  private Date enrollmentEnrolledAfter;

  private Date enrollmentOccurredBefore;

  private Date enrollmentOccurredAfter;

  private CategoryOptionCombo categoryOptionCombo;

  private IdSchemes idSchemes = new IdSchemes();

  private boolean includeRelationships;

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

  private boolean includeAttributes;

  private boolean includeAllDataElements;

  private Set<String> events = new HashSet<>();

  /**
   * Each attribute will affect the final SQL query. Some attributes are filtered on, while
   * attributes added via {@link #orderBy(TrackedEntityAttribute, SortDirection)} will be ordered
   * by.
   */
  private final Map<TrackedEntityAttribute, List<QueryFilter>> attributes = new HashMap<>();

  /**
   * Each data element will affect the final SQL query. Some data elements are filtered on, while
   * data elements added via {@link #orderBy(DataElement, SortDirection)} will be ordered by.
   */
  private final Map<DataElement, List<QueryFilter>> dataElements = new HashMap<>();

  private boolean hasDataElementFilter;

  private boolean includeDeleted;

  private Set<String> accessiblePrograms;

  private Set<String> accessibleProgramStages;

  private boolean synchronizationQuery;

  /** Indicates a point in the time used to decide the data that should not be synchronized */
  private Date skipChangedBefore;

  private Set<String> enrollments;

  @Getter private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  public EventQueryParams() {}

  public boolean hasProgram() {
    return program != null;
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

  public Program getProgram() {
    return program;
  }

  public EventQueryParams setProgram(Program program) {
    this.program = program;
    return this;
  }

  public ProgramStage getProgramStage() {
    return programStage;
  }

  public EventQueryParams setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
    return this;
  }

  public ProgramStatus getProgramStatus() {
    return programStatus;
  }

  public EventQueryParams setProgramStatus(ProgramStatus programStatus) {
    this.programStatus = programStatus;
    return this;
  }

  public ProgramType getProgramType() {
    return programType;
  }

  public EventQueryParams setProgramType(ProgramType programType) {
    this.programType = programType;
    return this;
  }

  public Boolean getFollowUp() {
    return followUp;
  }

  public EventQueryParams setFollowUp(Boolean followUp) {
    this.followUp = followUp;
    return this;
  }

  public OrganisationUnit getOrgUnit() {
    return orgUnit;
  }

  public EventQueryParams setOrgUnit(OrganisationUnit orgUnit) {
    this.orgUnit = orgUnit;
    return this;
  }

  public OrganisationUnitSelectionMode getOrgUnitMode() {
    return orgUnitMode;
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

  public TrackedEntity getTrackedEntity() {
    return trackedEntity;
  }

  public EventQueryParams setTrackedEntity(TrackedEntity trackedEntity) {
    this.trackedEntity = trackedEntity;
    return this;
  }

  public Date getStartDate() {
    return startDate;
  }

  public EventQueryParams setStartDate(Date startDate) {
    this.startDate = startDate;
    return this;
  }

  public Date getEndDate() {
    return endDate;
  }

  public EventQueryParams setEndDate(Date endDate) {
    this.endDate = endDate;
    return this;
  }

  public EventStatus getEventStatus() {
    return eventStatus;
  }

  public EventQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public Date getUpdatedAtStartDate() {
    return updatedAtStartDate;
  }

  public EventQueryParams setUpdatedAtStartDate(Date updatedAtStartDate) {
    this.updatedAtStartDate = updatedAtStartDate;
    return this;
  }

  public Date getUpdatedAtEndDate() {
    return updatedAtEndDate;
  }

  public EventQueryParams setUpdatedAtEndDate(Date updatedAtEndDate) {
    this.updatedAtEndDate = updatedAtEndDate;
    return this;
  }

  public String getUpdatedAtDuration() {
    return updatedAtDuration;
  }

  public EventQueryParams setUpdatedAtDuration(String updatedAtDuration) {
    this.updatedAtDuration = updatedAtDuration;
    return this;
  }

  public Date getScheduleAtStartDate() {
    return scheduleAtStartDate;
  }

  public EventQueryParams setScheduleAtStartDate(Date scheduleAtStartDate) {
    this.scheduleAtStartDate = scheduleAtStartDate;
    return this;
  }

  public Date getScheduleAtEndDate() {
    return scheduleAtEndDate;
  }

  public EventQueryParams setScheduleAtEndDate(Date scheduleAtEndDate) {
    this.scheduleAtEndDate = scheduleAtEndDate;
    return this;
  }

  public Date getEnrollmentEnrolledBefore() {
    return enrollmentEnrolledBefore;
  }

  public EventQueryParams setEnrollmentEnrolledBefore(Date enrollmentEnrolledBefore) {
    this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
    return this;
  }

  public Date getEnrollmentEnrolledAfter() {
    return enrollmentEnrolledAfter;
  }

  public EventQueryParams setEnrollmentEnrolledAfter(Date enrollmentEnrolledAfter) {
    this.enrollmentEnrolledAfter = enrollmentEnrolledAfter;
    return this;
  }

  public Date getEnrollmentOccurredBefore() {
    return enrollmentOccurredBefore;
  }

  public EventQueryParams setEnrollmentOccurredBefore(Date enrollmentOccurredBefore) {
    this.enrollmentOccurredBefore = enrollmentOccurredBefore;
    return this;
  }

  public Date getEnrollmentOccurredAfter() {
    return enrollmentOccurredAfter;
  }

  public EventQueryParams setEnrollmentOccurredAfter(Date enrollmentOccurredAfter) {
    this.enrollmentOccurredAfter = enrollmentOccurredAfter;
    return this;
  }

  public IdSchemes getIdSchemes() {
    return idSchemes;
  }

  public EventQueryParams setIdSchemes(IdSchemes idSchemes) {
    this.idSchemes = idSchemes;
    return this;
  }

  public boolean isIncludeAttributes() {
    return includeAttributes;
  }

  public EventQueryParams setIncludeAttributes(boolean includeAttributes) {
    this.includeAttributes = includeAttributes;
    return this;
  }

  public boolean isIncludeAllDataElements() {
    return includeAllDataElements;
  }

  public EventQueryParams setIncludeAllDataElements(boolean includeAllDataElements) {
    this.includeAllDataElements = includeAllDataElements;
    return this;
  }

  public List<Order> getOrder() {
    return Collections.unmodifiableList(this.order);
  }

  /** Order by an event field of the given {@code field} name in given sort {@code direction}. */
  public EventQueryParams orderBy(String field, SortDirection direction) {
    this.order.add(new Order(field, direction));
    return this;
  }

  /** Order by the given data element {@code de} in given sort {@code direction}. */
  public EventQueryParams orderBy(DataElement de, SortDirection direction) {
    this.order.add(new Order(de, direction));
    this.dataElements.putIfAbsent(de, new ArrayList<>());
    return this;
  }

  /** Order by the given tracked entity attribute {@code tea} in given sort {@code direction}. */
  public EventQueryParams orderBy(TrackedEntityAttribute tea, SortDirection direction) {
    this.order.add(new Order(tea, direction));
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    return this;
  }

  public CategoryOptionCombo getCategoryOptionCombo() {
    return categoryOptionCombo;
  }

  public EventQueryParams setCategoryOptionCombo(CategoryOptionCombo categoryOptionCombo) {
    this.categoryOptionCombo = categoryOptionCombo;
    return this;
  }

  public Set<String> getEvents() {
    return events;
  }

  public EventQueryParams setEvents(Set<String> events) {
    this.events = events;
    return this;
  }

  public Map<TrackedEntityAttribute, List<QueryFilter>> getAttributes() {
    return this.attributes;
  }

  public Map<DataElement, List<QueryFilter>> getDataElements() {
    return this.dataElements;
  }

  public EventQueryParams filterBy(TrackedEntityAttribute tea, QueryFilter filter) {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    this.attributes.get(tea).add(filter);
    return this;
  }

  public EventQueryParams filterBy(TrackedEntityAttribute tea) {
    this.attributes.putIfAbsent(tea, new ArrayList<>());
    return this;
  }

  public EventQueryParams filterBy(DataElement de, QueryFilter filter) {
    this.dataElements.putIfAbsent(de, new ArrayList<>());
    this.dataElements.get(de).add(filter);
    this.hasDataElementFilter = true;
    return this;
  }

  public EventQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public boolean isIncludeDeleted() {
    return this.includeDeleted;
  }

  public Set<String> getAccessiblePrograms() {
    return accessiblePrograms;
  }

  public EventQueryParams setAccessiblePrograms(Set<String> accessiblePrograms) {
    this.accessiblePrograms = accessiblePrograms;
    return this;
  }

  public Set<String> getAccessibleProgramStages() {
    return accessibleProgramStages;
  }

  public EventQueryParams setAccessibleProgramStages(Set<String> accessibleProgramStages) {
    this.accessibleProgramStages = accessibleProgramStages;
    return this;
  }

  public boolean hasSecurityFilter() {
    return accessiblePrograms != null && accessibleProgramStages != null;
  }

  public boolean isSynchronizationQuery() {
    return synchronizationQuery;
  }

  public EventQueryParams setSynchronizationQuery(boolean synchronizationQuery) {
    this.synchronizationQuery = synchronizationQuery;
    return this;
  }

  public Date getSkipChangedBefore() {
    return skipChangedBefore;
  }

  public EventQueryParams setSkipChangedBefore(Date skipChangedBefore) {
    this.skipChangedBefore = skipChangedBefore;
    return this;
  }

  public Set<String> getEnrollments() {
    return enrollments;
  }

  public EventQueryParams setEnrollments(Set<String> enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  public boolean isIncludeRelationships() {
    return includeRelationships;
  }

  public EventQueryParams setIncludeRelationships(boolean includeRelationships) {
    this.includeRelationships = includeRelationships;
    return this;
  }

  public boolean isOrganisationUnitMode(OrganisationUnitSelectionMode mode) {
    return orgUnitMode != null && orgUnitMode.equals(mode);
  }

  public boolean isPathOrganisationUnitMode() {
    return orgUnitMode != null
        && (orgUnitMode.equals(OrganisationUnitSelectionMode.DESCENDANTS)
            || orgUnitMode.equals(OrganisationUnitSelectionMode.CHILDREN));
  }
}
