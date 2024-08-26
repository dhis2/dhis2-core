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
package org.hisp.dhis.trackedentity;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;

/**
 * @author Lars Helge Overland
 */
public class TrackedEntityQueryParams {
  public static final String CREATED_ID = "created";

  public static final String ORG_UNIT_NAME = "ouname";

  public static final String INACTIVE_ID = "inactive";

  public static final int DEFAULT_PAGE = 1;

  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final String MAIN_QUERY_ALIAS = "TE";

  public static final String ENROLLMENT_QUERY_ALIAS = "en";

  /** Query value, will apply to all relevant attributes. */
  private QueryFilter query;

  /** Attributes to be included in the response. Can be used to filter response. */
  private List<QueryItem> attributes = new ArrayList<>();

  /** Filters for the response. */
  private List<QueryItem> filters = new ArrayList<>();

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  private Set<OrganisationUnit> orgUnits = new HashSet<>();

  /** Program for which instances in the response must be enrolled in. */
  private Program program;

  /** Status of a tracked entities enrollment into a given program. */
  private EnrollmentStatus enrollmentStatus;

  /** Indicates whether tracked entity is marked for follow up for the specified program. */
  private Boolean followUp;

  /** The last updated duration filter. */
  private String lastUpdatedDuration;

  /** Tracked entity of the instances in the response. */
  private TrackedEntityType trackedEntityType;

  /** Tracked entity types to fetch. */
  private List<TrackedEntityType> trackedEntityTypes = new ArrayList<>();

  /** Selection mode for the specified organisation units, default is DESCENDANTS. */
  private OrganisationUnitSelectionMode orgUnitMode = OrganisationUnitSelectionMode.DESCENDANTS;

  private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  /** ProgramStage to be used in conjunction with event status. */
  private ProgramStage programStage;

  /** Status of any events in the specified program. */
  private EventStatus eventStatus;

  /** Indicates whether not to include metadata in the response. */
  private boolean skipMeta;

  /** Page number. */
  private Integer page;

  /** Page size. */
  private Integer pageSize;

  /** Indicates whether to include the total number of pages in the paging response. */
  private boolean totalPages;

  /** Indicates whether paging should be skipped. */
  private boolean skipPaging;

  /** Indicates whether to include soft-deleted elements. Default to false */
  private boolean includeDeleted = false;

  /**
   * Potential Duplicate query parameter value. If null, we don't check whether a TE is a
   * potentialDuplicate or not
   */
  private Boolean potentialDuplicate;

  /** TE order params */
  private List<OrderParam> orders = new ArrayList<>();

  public TrackedEntityQueryParams() {}

  /** Adds a query item as attribute to the parameters. */
  public TrackedEntityQueryParams addAttribute(QueryItem attribute) {
    this.attributes.add(attribute);
    return this;
  }

  /** Adds a query item as filter to the parameters. */
  public TrackedEntityQueryParams addFilter(QueryItem filter) {
    this.filters.add(filter);
    return this;
  }

  /** Adds an organisation unit to the parameters. */
  public TrackedEntityQueryParams addOrganisationUnit(OrganisationUnit unit) {
    this.orgUnits.add(unit);
    return this;
  }

  /** Indicates whether this parameter specifies any organisation units. */
  public boolean hasOrganisationUnits() {
    return orgUnits != null && !orgUnits.isEmpty();
  }

  /** Indicates whether this parameter specifies a program. */
  public boolean hasProgram() {
    return program != null;
  }

  /** Indicates whether this parameter is of the given organisation unit mode. */
  public boolean isOrganisationUnitMode(OrganisationUnitSelectionMode mode) {
    return orgUnitMode != null && orgUnitMode.equals(mode);
  }

  /** Indicates whether this parameter specifies a programStage. */
  public boolean hasProgramStage() {
    return programStage != null;
  }

  /** Indicates whether paging is enabled. */
  public boolean isPaging() {
    return !isSkipPaging();
  }

  /** Returns the page number, falls back to default value of 1 if not specified. */
  public int getPageWithDefault() {
    return page != null && page > 0 ? page : DEFAULT_PAGE;
  }

  /** Returns the page size, falls back to default value of 50 if not specified. */
  public int getPageSizeWithDefault() {
    return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
  }

  /** Returns the offset based on the page number and page size. */
  public int getOffset() {
    return (getPageWithDefault() - 1) * getPageSizeWithDefault();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("query", query)
        .add("attributes", attributes)
        .add("filters", filters)
        .add("orgUnits", orgUnits)
        .add("program", program)
        .add("enrollmentStatus", enrollmentStatus)
        .add("followUp", followUp)
        .add("lastUpdatedDuration", lastUpdatedDuration)
        .add("trackedEntityType", trackedEntityType)
        .add("orgUnitMode", orgUnitMode)
        .add("assignedUserQueryParam", assignedUserQueryParam)
        .add("eventStatus", eventStatus)
        .add("skipMeta", skipMeta)
        .add("page", page)
        .add("pageSize", pageSize)
        .add("totalPages", totalPages)
        .add("skipPaging", skipPaging)
        .add("includeDeleted", includeDeleted)
        .add("orders", orders)
        .add("potentialDuplicate", potentialDuplicate)
        .toString();
  }

  public QueryFilter getQuery() {
    return query;
  }

  public TrackedEntityQueryParams setQuery(QueryFilter query) {
    this.query = query;
    return this;
  }

  public List<QueryItem> getAttributes() {
    return attributes;
  }

  public TrackedEntityQueryParams setAttributes(List<QueryItem> attributes) {
    this.attributes = attributes;
    return this;
  }

  public List<QueryItem> getFilters() {
    return filters;
  }

  public TrackedEntityQueryParams setFilters(List<QueryItem> filters) {
    this.filters = filters;
    return this;
  }

  public Set<OrganisationUnit> getOrgUnits() {
    return orgUnits;
  }

  public TrackedEntityQueryParams setOrgUnits(Set<OrganisationUnit> orgUnits) {
    this.orgUnits = orgUnits;
    return this;
  }

  public Program getProgram() {
    return program;
  }

  public TrackedEntityQueryParams setProgram(Program program) {
    this.program = program;
    return this;
  }

  public ProgramStage getProgramStage() {
    return programStage;
  }

  public TrackedEntityQueryParams setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
    return this;
  }

  public EnrollmentStatus getEnrollmentStatus() {
    return enrollmentStatus;
  }

  public TrackedEntityQueryParams setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  public Boolean getFollowUp() {
    return followUp;
  }

  public TrackedEntityQueryParams setFollowUp(Boolean followUp) {
    this.followUp = followUp;
    return this;
  }

  public Boolean getPotentialDuplicate() {
    return this.potentialDuplicate;
  }

  public TrackedEntityQueryParams setPotentialDuplicate(Boolean potentialDuplicate) {
    this.potentialDuplicate = potentialDuplicate;
    return this;
  }

  public String getLastUpdatedDuration() {
    return lastUpdatedDuration;
  }

  public TrackedEntityQueryParams setLastUpdatedDuration(String lastUpdatedDuration) {
    this.lastUpdatedDuration = lastUpdatedDuration;
    return this;
  }

  public TrackedEntityType getTrackedEntityType() {
    return trackedEntityType;
  }

  public TrackedEntityQueryParams setTrackedEntityType(TrackedEntityType trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
    return this;
  }

  public OrganisationUnitSelectionMode getOrgUnitMode() {
    return orgUnitMode;
  }

  public TrackedEntityQueryParams setOrgUnitMode(OrganisationUnitSelectionMode orgUnitMode) {
    this.orgUnitMode = orgUnitMode;
    return this;
  }

  public AssignedUserQueryParam getAssignedUserQueryParam() {
    return this.assignedUserQueryParam;
  }

  public EventStatus getEventStatus() {
    return eventStatus;
  }

  public TrackedEntityQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public boolean isSkipMeta() {
    return skipMeta;
  }

  public TrackedEntityQueryParams setSkipMeta(boolean skipMeta) {
    this.skipMeta = skipMeta;
    return this;
  }

  public Integer getPage() {
    return page;
  }

  public TrackedEntityQueryParams setPage(Integer page) {
    this.page = page;
    return this;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public TrackedEntityQueryParams setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public boolean isTotalPages() {
    return totalPages;
  }

  public TrackedEntityQueryParams setTotalPages(boolean totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  public boolean isSkipPaging() {
    return skipPaging;
  }

  public TrackedEntityQueryParams setSkipPaging(boolean skipPaging) {
    this.skipPaging = skipPaging;
    return this;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public TrackedEntityQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public List<OrderParam> getOrders() {
    return orders;
  }

  public void setOrders(List<OrderParam> orders) {
    this.orders = orders;
  }

  /**
   * Set assigned user selection mode, assigned users and the current user for the query. Non-empty
   * assigned users are only allowed with mode PROVIDED (or null).
   *
   * @param mode assigned user mode
   * @param assignedUsers assigned user uids
   */
  public void setUserWithAssignedUsers(AssignedUserSelectionMode mode, Set<String> assignedUsers) {
    this.assignedUserQueryParam = new AssignedUserQueryParam(mode, assignedUsers);
  }

  public List<TrackedEntityType> getTrackedEntityTypes() {
    return trackedEntityTypes;
  }

  public void setTrackedEntityTypes(List<TrackedEntityType> trackedEntityTypes) {
    this.trackedEntityTypes = trackedEntityTypes;
  }

  @Getter
  @AllArgsConstructor
  public enum OrderColumn {
    TRACKEDENTITY("trackedEntity", "uid", MAIN_QUERY_ALIAS),
    CREATED_AT("createdAt", CREATED_ID, MAIN_QUERY_ALIAS),
    CREATED_AT_CLIENT("createdAtClient", "createdatclient", MAIN_QUERY_ALIAS),
    UPDATED_AT("updatedAt", "lastupdated", MAIN_QUERY_ALIAS),
    UPDATED_AT_CLIENT("updatedAtClient", "lastupdatedatclient", MAIN_QUERY_ALIAS),
    ENROLLED_AT("enrolledAt", "enrollmentdate", ENROLLMENT_QUERY_ALIAS),
    INACTIVE(INACTIVE_ID, "inactive", MAIN_QUERY_ALIAS);

    private final String propName;

    private final String column;

    private final String tableAlias;

    /**
     * @return an Optional of an OrderColumn matching by property name
     */
    public static Optional<OrderColumn> findColumn(String property) {
      return Arrays.stream(values())
          .filter(orderColumn -> orderColumn.getPropName().equals(property))
          .findFirst();
    }
  }
}
