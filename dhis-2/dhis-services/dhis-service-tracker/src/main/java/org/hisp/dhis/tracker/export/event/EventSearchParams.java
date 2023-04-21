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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;

/**
 * @author Lars Helge Overland
 */
public class EventSearchParams
{
    public static final String EVENT_ID = "event";

    public static final String EVENT_ENROLLMENT_ID = "enrollment";

    public static final String EVENT_CREATED_AT_ID = "createdAt";

    public static final String EVENT_CREATED_BY_ID = "createdBy";

    public static final String EVENT_UPDATED_AT_ID = "updatedAt";

    public static final String EVENT_UPDATED_BY = "updatedBy";

    public static final String EVENT_STORED_BY_ID = "storedBy";

    public static final String EVENT_COMPLETED_BY_ID = "completedBy";

    public static final String EVENT_COMPLETED_AT_ID = "completedAt";

    public static final String EVENT_SCHEDULE_AT_DATE_ID = "scheduleAt";

    public static final String EVENT_OCCURRED_AT_DATE_ID = "occurredAt";

    public static final String EVENT_ORG_UNIT_ID = "orgUnit";

    public static final String EVENT_ORG_UNIT_NAME = "orgUnitName";

    public static final String EVENT_STATUS_ID = "status";

    public static final String EVENT_LONGITUDE_ID = "longitude";

    public static final String EVENT_LATITUDE_ID = "latitude";

    public static final String EVENT_PROGRAM_STAGE_ID = "programStage";

    public static final String EVENT_PROGRAM_ID = "program";

    public static final String EVENT_ATTRIBUTE_OPTION_COMBO_ID = "attributeOptionCombo";

    public static final String EVENT_DELETED = "deleted";

    public static final String EVENT_GEOMETRY = "geometry";

    public static final String PAGER_META_KEY = "pager";

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 50;

    private Program program;

    private ProgramStage programStage;

    private ProgramStatus programStatus;

    private ProgramType programType;

    private Boolean followUp;

    private OrganisationUnit orgUnit;

    private OrganisationUnitSelectionMode orgUnitSelectionMode;

    private TrackedEntityInstance trackedEntity;

    private Date startDate;

    private Date endDate;

    private EventStatus eventStatus;

    private Date updatedAtStartDate;

    private Date updatedAtEndDate;

    /**
     * The last updated duration filter.
     */
    private String updatedAtDuration;

    private Date scheduleAtStartDate;

    private Date scheduleAtEndDate;

    private Date enrollmentEnrolledBefore;

    private Date enrollmentEnrolledAfter;

    private Date enrollmentOccurredBefore;

    private Date enrollmentOccurredAfter;

    private CategoryOptionCombo categoryOptionCombo;

    private IdSchemes idSchemes = new IdSchemes();

    private Integer page;

    private Integer pageSize;

    private boolean totalPages;

    private boolean skipPaging;

    private boolean includeRelationships;

    private final List<OrderParam> orders = new ArrayList<>();

    private final List<OrderParam> gridOrders = new ArrayList<>();

    private final List<OrderParam> attributeOrders = new ArrayList<>();

    private boolean includeAttributes;

    private boolean includeAllDataElements;

    private Set<String> events = new HashSet<>();

    private Boolean skipEventId;

    /**
     * Filters for the response.
     */
    private final List<QueryItem> filters = new ArrayList<>();

    private final List<QueryItem> filterAttributes = new ArrayList<>();

    /**
     * DataElements to be included in the response. Can be used to filter
     * response.
     */
    private Set<QueryItem> dataElements = new HashSet<>();

    private boolean includeDeleted;

    private Set<String> accessiblePrograms;

    private Set<String> accessibleProgramStages;

    private boolean synchronizationQuery;

    /**
     * Indicates a point in the time used to decide the data that should not be
     * synchronized
     */
    private Date skipChangedBefore;

    private Set<String> programInstances;

    @Getter
    private AssignedUserQueryParam assignedUserQueryParam = AssignedUserQueryParam.ALL;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public EventSearchParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isPaging()
    {
        return page != null || pageSize != null;
    }

    public int getPageWithDefault()
    {
        return page != null && page > 0 ? page : DEFAULT_PAGE;
    }

    public int getPageSizeWithDefault()
    {
        return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    public int getOffset()
    {
        return (getPageWithDefault() - 1) * getPageSizeWithDefault();
    }

    /**
     * Sets paging properties to default values.
     */
    public void setDefaultPaging()
    {
        this.page = DEFAULT_PAGE;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.skipPaging = false;
    }

    public boolean hasProgram()
    {
        return program != null;
    }

    public boolean hasProgramStage()
    {
        return programStage != null;
    }

    /**
     * Indicates whether this parameters specifies a last updated start date.
     */
    public boolean hasUpdatedAtStartDate()
    {
        return updatedAtStartDate != null;
    }

    /**
     * Indicates whether this parameters specifies a last updated end date.
     */
    public boolean hasUpdatedAtEndDate()
    {
        return updatedAtEndDate != null;
    }

    /**
     * Indicates whether this parameters has a UpdatedAtDuration filter.
     */
    public boolean hasUpdatedAtDuration()
    {
        return updatedAtDuration != null;
    }

    /**
     * Indicates whether this search params contain any filters.
     */
    public boolean hasFilters()
    {
        return !filters.isEmpty();
    }

    /**
     * Null-safe check for skip event ID parameter.
     */
    public boolean isSkipEventId()
    {
        return skipEventId != null && skipEventId;
    }

    /**
     * Returns a list of dataElements and filters combined.
     */
    public List<QueryItem> getDataElementsAndFilters()
    {
        List<QueryItem> items = new ArrayList<>();
        items.addAll( filters );

        for ( QueryItem de : dataElements )
        {
            if ( !items.contains( de ) )
            {
                items.add( de );
            }
        }

        return items;
    }

    public EventSearchParams addDataElements( List<QueryItem> des )
    {
        dataElements.addAll( des );
        return this;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Program getProgram()
    {
        return program;
    }

    public EventSearchParams setProgram( Program program )
    {
        this.program = program;
        return this;
    }

    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public EventSearchParams setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
        return this;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public EventSearchParams setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
        return this;
    }

    public ProgramType getProgramType()
    {
        return programType;
    }

    public EventSearchParams setProgramType( ProgramType programType )
    {
        this.programType = programType;
        return this;
    }

    public Boolean getFollowUp()
    {
        return followUp;
    }

    public EventSearchParams setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
        return this;
    }

    public OrganisationUnit getOrgUnit()
    {
        return orgUnit;
    }

    public EventSearchParams setOrgUnit( OrganisationUnit orgUnit )
    {
        this.orgUnit = orgUnit;
        return this;
    }

    public OrganisationUnitSelectionMode getOrgUnitSelectionMode()
    {
        return orgUnitSelectionMode;
    }

    public EventSearchParams setOrgUnitSelectionMode( OrganisationUnitSelectionMode orgUnitSelectionMode )
    {
        this.orgUnitSelectionMode = orgUnitSelectionMode;
        return this;
    }

    /**
     * Set assigned user selection mode, assigned users and the current user for
     * the query. Non-empty assigned users are only allowed with mode PROVIDED
     * (or null).
     *
     * @param mode assigned user mode
     * @param current current user with which query is made
     * @param assignedUsers assigned user uids
     * @return this
     */
    public EventSearchParams setUserWithAssignedUsers( AssignedUserSelectionMode mode, User current,
        Set<String> assignedUsers )
    {
        this.assignedUserQueryParam = new AssignedUserQueryParam( mode, current, assignedUsers );
        return this;
    }

    public TrackedEntityInstance getTrackedEntity()
    {
        return trackedEntity;
    }

    public EventSearchParams setTrackedEntity( TrackedEntityInstance trackedEntity )
    {
        this.trackedEntity = trackedEntity;
        return this;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public EventSearchParams setStartDate( Date startDate )
    {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public EventSearchParams setEndDate( Date endDate )
    {
        this.endDate = endDate;
        return this;
    }

    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public EventSearchParams setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
        return this;
    }

    public Date getUpdatedAtStartDate()
    {
        return updatedAtStartDate;
    }

    public EventSearchParams setUpdatedAtStartDate( Date updatedAtStartDate )
    {
        this.updatedAtStartDate = updatedAtStartDate;
        return this;
    }

    public Date getUpdatedAtEndDate()
    {
        return updatedAtEndDate;
    }

    public EventSearchParams setUpdatedAtEndDate( Date updatedAtEndDate )
    {
        this.updatedAtEndDate = updatedAtEndDate;
        return this;
    }

    public String getUpdatedAtDuration()
    {
        return updatedAtDuration;
    }

    public EventSearchParams setUpdatedAtDuration( String updatedAtDuration )
    {
        this.updatedAtDuration = updatedAtDuration;
        return this;
    }

    public Date getScheduleAtStartDate()
    {
        return scheduleAtStartDate;
    }

    public EventSearchParams setScheduleAtStartDate( Date scheduleAtStartDate )
    {
        this.scheduleAtStartDate = scheduleAtStartDate;
        return this;
    }

    public Date getScheduleAtEndDate()
    {
        return scheduleAtEndDate;
    }

    public EventSearchParams setScheduleAtEndDate( Date scheduleAtEndDate )
    {
        this.scheduleAtEndDate = scheduleAtEndDate;
        return this;
    }

    public Date getEnrollmentEnrolledBefore()
    {
        return enrollmentEnrolledBefore;
    }

    public EventSearchParams setEnrollmentEnrolledBefore( Date enrollmentEnrolledBefore )
    {
        this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
        return this;
    }

    public Date getEnrollmentEnrolledAfter()
    {
        return enrollmentEnrolledAfter;
    }

    public EventSearchParams setEnrollmentEnrolledAfter( Date enrollmentEnrolledAfter )
    {
        this.enrollmentEnrolledAfter = enrollmentEnrolledAfter;
        return this;
    }

    public Date getEnrollmentOccurredBefore()
    {
        return enrollmentOccurredBefore;
    }

    public EventSearchParams setEnrollmentOccurredBefore( Date enrollmentOccurredBefore )
    {
        this.enrollmentOccurredBefore = enrollmentOccurredBefore;
        return this;
    }

    public Date getEnrollmentOccurredAfter()
    {
        return enrollmentOccurredAfter;
    }

    public EventSearchParams setEnrollmentOccurredAfter( Date enrollmentOccurredAfter )
    {
        this.enrollmentOccurredAfter = enrollmentOccurredAfter;
        return this;
    }

    public IdSchemes getIdSchemes()
    {
        return idSchemes;
    }

    public EventSearchParams setIdSchemes( IdSchemes idSchemes )
    {
        this.idSchemes = idSchemes;
        return this;
    }

    public Integer getPage()
    {
        return page;
    }

    public EventSearchParams setPage( Integer page )
    {
        this.page = page;
        return this;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public EventSearchParams setPageSize( Integer pageSize )
    {
        this.pageSize = pageSize;
        return this;
    }

    public boolean isTotalPages()
    {
        return totalPages;
    }

    public EventSearchParams setTotalPages( boolean totalPages )
    {
        this.totalPages = totalPages;
        return this;
    }

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public EventSearchParams setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
        return this;
    }

    public boolean isIncludeAttributes()
    {
        return includeAttributes;
    }

    public EventSearchParams setIncludeAttributes( boolean includeAttributes )
    {
        this.includeAttributes = includeAttributes;
        return this;
    }

    public boolean isIncludeAllDataElements()
    {
        return includeAllDataElements;
    }

    public EventSearchParams setIncludeAllDataElements( boolean includeAllDataElements )
    {
        this.includeAllDataElements = includeAllDataElements;
        return this;
    }

    public List<OrderParam> getOrders()
    {
        return Collections.unmodifiableList( this.orders );
    }

    public EventSearchParams addOrders( List<OrderParam> orders )
    {
        this.orders.addAll( orders );
        return this;
    }

    public List<OrderParam> getGridOrders()
    {
        return Collections.unmodifiableList( this.gridOrders );
    }

    public EventSearchParams addGridOrders( List<OrderParam> gridOrders )
    {
        this.gridOrders.addAll( gridOrders );
        return this;
    }

    public List<OrderParam> getAttributeOrders()
    {
        return Collections.unmodifiableList( this.attributeOrders );
    }

    public EventSearchParams addAttributeOrders( List<OrderParam> attributeOrders )
    {
        this.attributeOrders.addAll( attributeOrders );
        return this;
    }

    public CategoryOptionCombo getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public EventSearchParams setCategoryOptionCombo( CategoryOptionCombo categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
        return this;
    }

    public Set<String> getEvents()
    {
        return events;
    }

    public EventSearchParams setEvents( Set<String> events )
    {
        this.events = events;
        return this;
    }

    public Boolean getSkipEventId()
    {
        return skipEventId;
    }

    public EventSearchParams setSkipEventId( Boolean skipEventId )
    {
        this.skipEventId = skipEventId;
        return this;
    }

    public List<QueryItem> getFilters()
    {
        return Collections.unmodifiableList( this.filters );
    }

    public EventSearchParams addFilter( QueryItem item )
    {
        this.filters.add( item );
        return this;
    }

    public EventSearchParams addFilters( List<QueryItem> items )
    {
        this.filters.addAll( items );
        return this;
    }

    public List<QueryItem> getFilterAttributes()
    {
        return Collections.unmodifiableList( this.filterAttributes );
    }

    public EventSearchParams addFilterAttributes( List<QueryItem> item )
    {
        this.filterAttributes.addAll( item );
        return this;
    }

    public EventSearchParams addFilterAttributes( QueryItem item )
    {
        this.filterAttributes.add( item );
        return this;
    }

    public EventSearchParams setIncludeDeleted( boolean includeDeleted )
    {
        this.includeDeleted = includeDeleted;
        return this;
    }

    public boolean isIncludeDeleted()
    {
        return this.includeDeleted;
    }

    public Set<QueryItem> getDataElements()
    {
        return dataElements;
    }

    public EventSearchParams setDataElements( Set<QueryItem> dataElements )
    {
        this.dataElements = dataElements;
        return this;
    }

    public Set<String> getAccessiblePrograms()
    {
        return accessiblePrograms;
    }

    public EventSearchParams setAccessiblePrograms( Set<String> accessiblePrograms )
    {
        this.accessiblePrograms = accessiblePrograms;
        return this;
    }

    public Set<String> getAccessibleProgramStages()
    {
        return accessibleProgramStages;
    }

    public EventSearchParams setAccessibleProgramStages( Set<String> accessibleProgramStages )
    {
        this.accessibleProgramStages = accessibleProgramStages;
        return this;
    }

    public boolean hasSecurityFilter()
    {
        return accessiblePrograms != null && accessibleProgramStages != null;
    }

    public boolean isSynchronizationQuery()
    {
        return synchronizationQuery;
    }

    public EventSearchParams setSynchronizationQuery( boolean synchronizationQuery )
    {
        this.synchronizationQuery = synchronizationQuery;
        return this;
    }

    public Date getSkipChangedBefore()
    {
        return skipChangedBefore;
    }

    public EventSearchParams setSkipChangedBefore( Date skipChangedBefore )
    {
        this.skipChangedBefore = skipChangedBefore;
        return this;
    }

    public Set<String> getProgramInstances()
    {
        return programInstances;
    }

    public EventSearchParams setProgramInstances( Set<String> programInstances )
    {
        this.programInstances = programInstances;
        return this;
    }

    public boolean isIncludeRelationships()
    {
        return includeRelationships;
    }

    public EventSearchParams setIncludeRelationships( boolean includeRelationships )
    {
        this.includeRelationships = includeRelationships;
        return this;
    }

    public boolean isOrganisationUnitMode( OrganisationUnitSelectionMode mode )
    {
        return orgUnitSelectionMode != null && orgUnitSelectionMode.equals( mode );
    }

    public boolean isPathOrganisationUnitMode()
    {
        return orgUnitSelectionMode != null && (orgUnitSelectionMode.equals( OrganisationUnitSelectionMode.DESCENDANTS )
            || orgUnitSelectionMode.equals( OrganisationUnitSelectionMode.CHILDREN ));
    }
}