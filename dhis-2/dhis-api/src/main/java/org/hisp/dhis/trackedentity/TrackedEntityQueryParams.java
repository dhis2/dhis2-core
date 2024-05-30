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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
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
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;

/**
 * @author Lars Helge Overland
 */
public class TrackedEntityQueryParams {
  public static final String TRACKED_ENTITY_ID = "instance";

  public static final String CREATED_ID = "created";

  public static final String LAST_UPDATED_ID = "lastupdated";

  public static final String ORG_UNIT_ID = "ou";

  public static final String ORG_UNIT_NAME = "ouname";

  public static final String TRACKED_ENTITY_TYPE_ID = "te";

  public static final String INACTIVE_ID = "inactive";

  public static final String DELETED = "deleted";

  public static final String POTENTIAL_DUPLICATE = "potentialduplicate";

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

  /** Set of te uids to explicitly select. */
  private Set<String> trackedEntityUids = new HashSet<>();

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

  /** Indicates if there is a maximum te retrieval limit. 0 no limit. */
  private int maxTeLimit;

  /** Indicates whether to include soft-deleted elements. Default to false */
  private boolean includeDeleted = false;

  /**
   * Potential Duplicate query parameter value. If null, we don't check whether a TE is a
   * potentialDuplicate or not
   */
  private Boolean potentialDuplicate;

  /** TE order params */
  private List<OrderParam> orders = new ArrayList<>();

  /** Current user for query. */
  private transient User user;

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

  /**
   * Performs a set of operations on this params.
   *
   * <ul>
   *   <li>If a query item is specified as an attribute item as well as a filter item, the filter
   *       item will be removed. In that case, if the attribute item does not have any filters and
   *       the filter item has one or more filters, these will be applied to the attribute item.
   * </ul>
   */
  public void conform() {
    Iterator<QueryItem> filterIter = filters.iterator();

    while (filterIter.hasNext()) {
      QueryItem filter = filterIter.next();

      int index = attributes.indexOf(filter); // Filter present as attr

      if (index >= 0) {
        QueryItem attribute = attributes.get(index);

        if (!attribute.hasFilter() && filter.hasFilter()) {
          attribute.getFilters().addAll(filter.getFilters());
        }

        filterIter.remove();
      }
    }
  }

  /**
   * Prepares the organisation units of the given parameters to simplify querying. Mode ACCESSIBLE
   * is converted to DESCENDANTS for organisation units linked to the search scope of the given
   * user. Mode CAPTURE is converted to DESCENDANTS too, but using organisation units linked to the
   * user's capture scope, and mode CHILDREN is converted to SELECTED for organisation units
   * including all their children. Mode can be DESCENDANTS, SELECTED, ALL only after invoking this
   * method.
   */
  public void handleOrganisationUnits() {
    if (user != null && isOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)) {
      setOrgUnits(user.getTeiSearchOrganisationUnitsWithFallback());
      setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    } else if (user != null && isOrganisationUnitMode(OrganisationUnitSelectionMode.CAPTURE)) {
      setOrgUnits(user.getOrganisationUnits());
      setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    } else if (isOrganisationUnitMode(CHILDREN)) {
      Set<OrganisationUnit> orgUnits = new HashSet<>(getOrgUnits());

      for (OrganisationUnit organisationUnit : getOrgUnits()) {
        orgUnits.addAll(organisationUnit.getChildren());
      }

      setOrgUnits(orgUnits);
      setOrgUnitMode(OrganisationUnitSelectionMode.SELECTED);
    }
  }

  public boolean hasTrackedEntities() {
    return CollectionUtils.isNotEmpty(this.trackedEntityUids);
  }

  public void addAttributes(List<QueryItem> attrs) {
    attributes.addAll(attrs);
  }

  public boolean hasFilterForEvents() {
    return this.getAssignedUserQueryParam().getMode() != AssignedUserSelectionMode.ALL
        || hasEventStatus();
  }

  /** Add the given attributes to this params if they are not already present. */
  public void addAttributesIfNotExist(List<QueryItem> attrs) {
    for (QueryItem attr : attrs) {
      if (attributes != null && !attributes.contains(attr)) {
        attributes.add(attr);
      }
    }
  }

  /** Adds the given filters to this parameter if they are not already present. */
  public void addFiltersIfNotExist(List<QueryItem> filtrs) {
    for (QueryItem filter : filtrs) {
      if (filters != null && !filters.contains(filter)) {
        filters.add(filter);
      }
    }
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

  /** Indicates whether this parameter specifies a query. */
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

  /** Indicates whether this parameter specifies any attributes and/or filters. */
  public boolean hasAttributesOrFilters() {
    return hasAttributes() || hasFilters();
  }

  /** Indicates whether this parameter specifies any attributes. */
  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
  }

  /** Indicates whether this parameter specifies any filters. */
  public boolean hasFilters() {
    return filters != null && !filters.isEmpty();
  }

  /** Indicates whether this parameter specifies any organisation units. */
  public boolean hasOrganisationUnits() {
    return orgUnits != null && !orgUnits.isEmpty();
  }

  /** Indicates whether this parameter specifies a program. */
  public boolean hasProgram() {
    return program != null;
  }

  /** Indicates whether this parameter specifies a program status. */
  public boolean hasEnrollmentStatus() {
    return enrollmentStatus != null;
  }

  /**
   * Indicates whether this parameter specifies follow up for the given program. Follow up can be
   * specified as true or false.
   */
  public boolean hasFollowUp() {
    return followUp != null;
  }

  /** Indicates whether this parameter has a lastUpdatedDuration filter. */
  public boolean hasLastUpdatedDuration() {
    return lastUpdatedDuration != null;
  }

  /** Indicates whether this parameter specifies a tracked entity. */
  public boolean hasTrackedEntityType() {
    return trackedEntityType != null;
  }

  /** Indicates whether this parameter is of the given organisation unit mode. */
  public boolean isOrganisationUnitMode(OrganisationUnitSelectionMode mode) {
    return orgUnitMode != null && orgUnitMode.equals(mode);
  }

  /** Indicates whether this parameter specifies a programStage. */
  public boolean hasProgramStage() {
    return programStage != null;
  }

  /** Indicates whether this parameter specifies an event status. */
  public boolean hasEventStatus() {
    return eventStatus != null;
  }

  /**
   * Indicates whether the event status specified for the params is equal to the given event status.
   */
  public boolean isEventStatus(EventStatus eventStatus) {
    return this.eventStatus != null && this.eventStatus.equals(eventStatus);
  }

  /** Check whether we are filtering for potential duplicate property. */
  public boolean hasPotentialDuplicateFilter() {
    return potentialDuplicate != null;
  }

  /**
   * Checks if there is at least one unique filter in the params. In attributes or filters.
   *
   * @return true if there exist at least one unique filter in filters/attributes, false otherwise.
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
        .add("user", user)
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

  public void setMaxTeLimit(int maxTeLimit) {
    this.maxTeLimit = maxTeLimit;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public TrackedEntityQueryParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public User getUser() {
    return user;
  }

  public List<OrderParam> getOrders() {
    return orders;
  }

  public void setOrders(List<OrderParam> orders) {
    this.orders = orders;
  }

  public Set<String> getTrackedEntityUids() {
    return trackedEntityUids;
  }

  public void setTrackedEntityUids(Set<String> trackedEntityUids) {
    this.trackedEntityUids = trackedEntityUids;
  }

  /**
   * Set assigned user selection mode, assigned users and the current user for the query. Non-empty
   * assigned users are only allowed with mode PROVIDED (or null).
   *
   * @param mode assigned user mode
   * @param current current user with which query is made
   * @param assignedUsers assigned user uids
   */
  public void setUserWithAssignedUsers(
      AssignedUserSelectionMode mode, User current, Set<String> assignedUsers) {
    this.assignedUserQueryParam = new AssignedUserQueryParam(mode, current, assignedUsers);
    this.user = current;
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

    public boolean isPropertyEqualTo(String property) {
      return propName.equalsIgnoreCase(property);
    }

    /**
     * @return an Optional of an OrderColumn matching by property name
     */
    public static Optional<OrderColumn> findColumn(String property) {
      return Arrays.stream(values())
          .filter(orderColumn -> orderColumn.getPropName().equals(property))
          .findFirst();
    }

    /**
     * @return a Sql string composed by the actual table alias and column. In use for the inner
     *     query select fields and order by
     */
    public String getSqlColumnWithTableAlias() {
      return tableAlias + "." + column;
    }

    /**
     * @return a Sql string composed by the main query alias and column. In use for the outer query
     *     select fields and order by
     */
    public String getSqlColumnWithMainTable() {
      return MAIN_QUERY_ALIAS + "." + column;
    }
  }
}
