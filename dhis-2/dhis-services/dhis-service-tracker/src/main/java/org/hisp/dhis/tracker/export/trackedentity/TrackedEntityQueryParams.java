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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

@ToString
public class TrackedEntityQueryParams {

  public static final int DEFAULT_PAGE = 1;

  public static final int DEFAULT_PAGE_SIZE = 50;

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
  private Set<OrganisationUnit> accessibleOrgUnits = new HashSet<>();

  /** Program for which instances in the response must be enrolled in. */
  private Program program;

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

  /** Tracked entity of the instances in the response. */
  private TrackedEntityType trackedEntityType;

  /** Tracked entity types to fetch. */
  private List<TrackedEntityType> trackedEntityTypes = Lists.newArrayList();

  /** Selection mode for the specified organisation units, default is DESCENDANTS. */
  private OrganisationUnitSelectionMode orgUnitMode = OrganisationUnitSelectionMode.DESCENDANTS;

  private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

  /** Set of te uids to explicitly select. */
  private Set<String> trackedEntityUids = new HashSet<>();

  /** ProgramStage to be used in conjunction with eventstatus. */
  private ProgramStage programStage;

  /** Status of any events in the specified program. */
  private EventStatus eventStatus;

  /** Start date for event for the given program. */
  private Date eventStartDate;

  /** End date for event for the given program. */
  private Date eventEndDate;

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

  /** Indicates if there is a maximum te retrieval limit. 0 no limit. */
  private int maxTeLimit;

  /** Indicates whether to include soft-deleted elements. Default to false */
  private boolean includeDeleted = false;

  /** Indicates whether to include all TE attributes */
  private boolean includeAllAttributes;

  /**
   * Potential Duplicate query parameter value. If null, we don't check whether a TE is a
   * potentialDuplicate or not
   */
  private Boolean potentialDuplicate;

  private final List<Order> order = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  /** Current user for query. */
  private transient User user;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityQueryParams() {}

  public boolean hasTrackedEntities() {
    return CollectionUtils.isNotEmpty(this.trackedEntityUids);
  }

  public TrackedEntityQueryParams addAttributes(List<QueryItem> attrs) {
    attributes.addAll(attrs);
    return this;
  }

  public boolean hasFilterForEvents() {
    return this.getAssignedUserQueryParam().getMode() != AssignedUserSelectionMode.ALL
        || hasEventStatus();
  }

  /** Add the given attributes to this params if they are not already present. */
  public TrackedEntityQueryParams addAttributesIfNotExist(List<QueryItem> attrs) {
    for (QueryItem attr : attrs) {
      if (attributes != null && !attributes.contains(attr)) {
        attributes.add(attr);
      }
    }

    return this;
  }

  /** Adds the given filters to these parameters if they are not already present. */
  public TrackedEntityQueryParams addFiltersIfNotExist(List<QueryItem> filtrs) {
    for (QueryItem filter : filtrs) {
      if (filters != null && !filters.contains(filter)) {
        filters.add(filter);
      }
    }

    return this;
  }

  /**
   * Indicates whether this is a logical OR query, meaning that a query string is specified and
   * instances which matches this query on one or more attributes should be included in the
   * response. The opposite is an item-specific query, where the instances which matches the
   * specific attributes should be included.
   */
  public boolean isOrQuery() {
    return hasQuery();
  }

  /** Indicates whether these parameters specify a query. */
  public boolean hasQuery() {
    return query != null && query.isFilter();
  }

  /** Returns a list of attributes and filters combined. */
  public List<QueryItem> getAttributesAndFilters() {
    List<QueryItem> items = new ArrayList<>();
    items.addAll(attributes);
    items.addAll(filters);
    return items;
  }

  /** Returns a list of attributes and filters combined. */
  public Set<String> getAttributeAndFilterIds() {
    return getAttributesAndFilters().stream().map(QueryItem::getItemId).collect(Collectors.toSet());
  }

  /** Returns a list of attributes which appear more than once. */
  public List<QueryItem> getDuplicateAttributes() {
    Set<QueryItem> items = new HashSet<>();
    List<QueryItem> duplicates = new ArrayList<>();

    for (QueryItem item : getAttributes()) {
      if (!items.add(item)) {
        duplicates.add(item);
      }
    }

    return duplicates;
  }

  /** Returns a list of attributes which appear more than once. */
  public List<QueryItem> getDuplicateFilters() {
    Set<QueryItem> items = new HashSet<>();
    List<QueryItem> duplicates = new ArrayList<>();

    for (QueryItem item : getFilters()) {
      if (!items.add(item)) {
        duplicates.add(item);
      }
    }

    return duplicates;
  }

  /** Indicates whether these parameters specify any attributes and/or filters. */
  public boolean hasAttributesOrFilters() {
    return hasAttributes() || hasFilters();
  }

  /** Indicates whether these parameters specify any attributes. */
  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
  }

  /** Indicates whether these parameters specify any filters. */
  public boolean hasFilters() {
    return filters != null && !filters.isEmpty();
  }

  /** Indicates whether these parameters specify any organisation units. */
  public boolean hasAccessibleOrgUnits() {
    return !accessibleOrgUnits.isEmpty();
  }

  /** Indicates whether these parameters specify a program. */
  public boolean hasProgram() {
    return program != null;
  }

  /** Indicates whether these parameters specify a program status. */
  public boolean hasProgramStatus() {
    return programStatus != null;
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
    return this.eventStatus != null && this.eventStatus.equals(eventStatus);
  }

  /** Indicates whether these parameters specify an event start date. */
  public boolean hasEventStartDate() {
    return eventStartDate != null;
  }

  /** Indicates whether these parameters specify an event end date. */
  public boolean hasEventEndDate() {
    return eventEndDate != null;
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
    if (!hasFilters() && !hasAttributes()) {
      return false;
    }

    for (QueryItem filter : filters) {
      if (filter.isUnique()) {
        return true;
      }
    }

    for (QueryItem attribute : attributes) {
      if (attribute.isUnique() && attribute.hasFilter()) {
        return true;
      }
    }

    return false;
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

  public Set<OrganisationUnit> getAccessibleOrgUnits() {
    return accessibleOrgUnits;
  }

  public TrackedEntityQueryParams setAccessibleOrgUnits(Set<OrganisationUnit> accessibleOrgUnits) {
    this.accessibleOrgUnits = accessibleOrgUnits;
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

  public ProgramStatus getProgramStatus() {
    return programStatus;
  }

  public TrackedEntityQueryParams setProgramStatus(ProgramStatus programStatus) {
    this.programStatus = programStatus;
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

  public Date getLastUpdatedStartDate() {
    return lastUpdatedStartDate;
  }

  public TrackedEntityQueryParams setLastUpdatedStartDate(Date lastUpdatedStartDate) {
    this.lastUpdatedStartDate = lastUpdatedStartDate;
    return this;
  }

  public Date getLastUpdatedEndDate() {
    return lastUpdatedEndDate;
  }

  public TrackedEntityQueryParams setLastUpdatedEndDate(Date lastUpdatedEndDate) {
    this.lastUpdatedEndDate = lastUpdatedEndDate;
    return this;
  }

  public String getLastUpdatedDuration() {
    return lastUpdatedDuration;
  }

  public TrackedEntityQueryParams setLastUpdatedDuration(String lastUpdatedDuration) {
    this.lastUpdatedDuration = lastUpdatedDuration;
    return this;
  }

  public Date getProgramEnrollmentStartDate() {
    return programEnrollmentStartDate;
  }

  public TrackedEntityQueryParams setProgramEnrollmentStartDate(Date programEnrollmentStartDate) {
    this.programEnrollmentStartDate = programEnrollmentStartDate;
    return this;
  }

  public Date getProgramEnrollmentEndDate() {
    return programEnrollmentEndDate != null
        ? DateUtils.addDays(programEnrollmentEndDate, 1)
        : programEnrollmentEndDate;
  }

  public TrackedEntityQueryParams setProgramEnrollmentEndDate(Date programEnrollmentEndDate) {
    this.programEnrollmentEndDate = programEnrollmentEndDate;
    return this;
  }

  public Date getProgramIncidentStartDate() {
    return programIncidentStartDate;
  }

  public TrackedEntityQueryParams setProgramIncidentStartDate(Date programIncidentStartDate) {
    this.programIncidentStartDate = programIncidentStartDate;
    return this;
  }

  public Date getProgramIncidentEndDate() {
    return programIncidentEndDate != null
        ? DateUtils.addDays(programIncidentEndDate, 1)
        : programIncidentEndDate;
  }

  public TrackedEntityQueryParams setProgramIncidentEndDate(Date programIncidentEndDate) {
    this.programIncidentEndDate = programIncidentEndDate;
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

  public TrackedEntityQueryParams setAssignedUserQueryParam(
      AssignedUserQueryParam assignedUserQueryParam) {
    this.assignedUserQueryParam = assignedUserQueryParam;
    return this;
  }

  public TrackedEntityQueryParams setUser(User user) {
    this.user = user;
    return this;
  }

  public EventStatus getEventStatus() {
    return eventStatus;
  }

  public TrackedEntityQueryParams setEventStatus(EventStatus eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public Date getEventStartDate() {
    return eventStartDate;
  }

  public TrackedEntityQueryParams setEventStartDate(Date eventStartDate) {
    this.eventStartDate = eventStartDate;
    return this;
  }

  public Date getEventEndDate() {
    return eventEndDate;
  }

  public TrackedEntityQueryParams setEventEndDate(Date eventEndDate) {
    this.eventEndDate = eventEndDate;
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

  public int getMaxTeLimit() {
    return maxTeLimit;
  }

  public TrackedEntityQueryParams setMaxTeLimit(int maxTeLimit) {
    this.maxTeLimit = maxTeLimit;
    return this;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public TrackedEntityQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public boolean isIncludeAllAttributes() {
    return includeAllAttributes;
  }

  public TrackedEntityQueryParams setIncludeAllAttributes(boolean includeAllAttributes) {
    this.includeAllAttributes = includeAllAttributes;
    return this;
  }

  public User getUser() {
    return user;
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
    this.addAttributesIfNotExist(
        QueryItem.getQueryItems(List.of(tea)).stream()
            .filter(sAtt -> !this.getFilters().contains(sAtt))
            .toList());
    return this;
  }

  public List<Order> getOrder() {
    return order;
  }

  public Set<String> getTrackedEntityUids() {
    return trackedEntityUids;
  }

  public TrackedEntityQueryParams setTrackedEntityUids(Set<String> trackedEntityUids) {
    this.trackedEntityUids = trackedEntityUids;
    return this;
  }

  public List<TrackedEntityType> getTrackedEntityTypes() {
    return trackedEntityTypes;
  }

  public void setTrackedEntityTypes(List<TrackedEntityType> trackedEntityTypes) {
    this.trackedEntityTypes = trackedEntityTypes;
  }
}
