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
package org.hisp.dhis.dxf2.events.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
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

    public static final String EVENT_CREATED_ID = "created";

    public static final String EVENT_CREATED_BY_USER_INFO_ID = "createdbyuserinfo";

    public static final String EVENT_LAST_UPDATED_ID = "lastUpdated";

    public static final String EVENT_LAST_UPDATED_BY_USER_INFO_ID = "lastUpdatedbyuserinfo";

    public static final String EVENT_STORED_BY_ID = "storedBy";

    public static final String EVENT_COMPLETED_BY_ID = "completedBy";

    public static final String EVENT_COMPLETED_DATE_ID = "completedDate";

    public static final String EVENT_DUE_DATE_ID = "dueDate";

    public static final String EVENT_EXECUTION_DATE_ID = "eventDate";

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

    private AssignedUserSelectionMode assignedUserSelectionMode;

    private Set<String> assignedUsers = new HashSet<>();

    private TrackedEntityInstance trackedEntityInstance;

    private Date startDate;

    private Date endDate;

    private EventStatus eventStatus;

    private Date lastUpdatedStartDate;

    private Date lastUpdatedEndDate;

    /**
     * The last updated duration filter.
     */
    private String lastUpdatedDuration;

    private Date dueDateStart;

    private Date dueDateEnd;

    private Date enrollmentEnrolledAfter;

    private Date enrollmentEnrolledBefore;

    private CategoryOptionCombo categoryOptionCombo;

    private IdSchemes idSchemes = new IdSchemes();

    private Integer page;

    private Integer pageSize;

    private boolean totalPages;

    private boolean skipPaging;

    private boolean includeRelationships;

    private List<OrderParam> orders;

    private List<OrderParam> gridOrders;

    private boolean includeAttributes;

    private boolean includeAllDataElements;

    private Set<String> events = new HashSet<>();

    private Boolean skipEventId;

    /**
     * Filters for the response.
     */
    private List<QueryItem> filters = new ArrayList<>();

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
    public boolean hasLastUpdatedStartDate()
    {
        return lastUpdatedStartDate != null;
    }

    /**
     * Indicates whether this parameters specifies a last updated end date.
     */
    public boolean hasLastUpdatedEndDate()
    {
        return lastUpdatedEndDate != null;
    }

    /**
     * Indicates whether this parameters has a lastUpdatedDuration filter.
     */
    public boolean hasLastUpdatedDuration()
    {
        return lastUpdatedDuration != null;
    }

    /**
     * Indicates whether this search params contain any filters.
     */
    public boolean hasFilters()
    {
        return filters != null && !filters.isEmpty();
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

    public AssignedUserSelectionMode getAssignedUserSelectionMode()
    {
        return assignedUserSelectionMode;
    }

    public EventSearchParams setAssignedUserSelectionMode( AssignedUserSelectionMode assignedUserSelectionMode )
    {
        this.assignedUserSelectionMode = assignedUserSelectionMode;
        return this;
    }

    public Set<String> getAssignedUsers()
    {
        return assignedUsers;
    }

    public EventSearchParams setAssignedUsers( Set<String> assignedUsers )
    {
        this.assignedUsers = assignedUsers;
        return this;
    }

    public TrackedEntityInstance getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public EventSearchParams setTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
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

    public Date getLastUpdatedStartDate()
    {
        return lastUpdatedStartDate;
    }

    public EventSearchParams setLastUpdatedStartDate( Date lastUpdatedStartDate )
    {
        this.lastUpdatedStartDate = lastUpdatedStartDate;
        return this;
    }

    public Date getLastUpdatedEndDate()
    {
        return lastUpdatedEndDate;
    }

    public EventSearchParams setLastUpdatedEndDate( Date lastUpdatedEndDate )
    {
        this.lastUpdatedEndDate = lastUpdatedEndDate;
        return this;
    }

    public String getLastUpdatedDuration()
    {
        return lastUpdatedDuration;
    }

    public EventSearchParams setLastUpdatedDuration( String lastUpdatedDuration )
    {
        this.lastUpdatedDuration = lastUpdatedDuration;
        return this;
    }

    public Date getDueDateStart()
    {
        return dueDateStart;
    }

    public EventSearchParams setDueDateStart( Date dueDateStart )
    {
        this.dueDateStart = dueDateStart;
        return this;
    }

    public Date getDueDateEnd()
    {
        return dueDateEnd;
    }

    public EventSearchParams setDueDateEnd( Date dueDateEnd )
    {
        this.dueDateEnd = dueDateEnd;
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

    public Date getEnrollmentEnrolledBefore()
    {
        return enrollmentEnrolledBefore;
    }

    public EventSearchParams setEnrollmentEnrolledBefore( Date enrollmentEnrolledBefore )
    {
        this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
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
        return this.orders;
    }

    public EventSearchParams setOrders( List<OrderParam> orders )
    {
        this.orders = orders;
        return this;
    }

    public List<OrderParam> getGridOrders()
    {
        return this.gridOrders;
    }

    public EventSearchParams setGridOrders( List<OrderParam> gridOrders )
    {
        this.gridOrders = gridOrders;
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
        return filters;
    }

    public EventSearchParams setFilters( List<QueryItem> filters )
    {
        this.filters = filters;
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

    public void handleCurrentUserSelectionMode( User currentUser )
    {
        if ( AssignedUserSelectionMode.CURRENT.equals( this.assignedUserSelectionMode ) && currentUser != null )
        {
            this.assignedUsers = Collections.singleton( currentUser.getUid() );
            this.assignedUserSelectionMode = AssignedUserSelectionMode.PROVIDED;
        }
    }

    public boolean hasAssignedUsers()
    {
        return this.assignedUsers != null && !this.assignedUsers.isEmpty();
    }

    public boolean isIncludeOnlyUnassignedEvents()
    {
        return AssignedUserSelectionMode.NONE.equals( this.assignedUserSelectionMode );
    }

    public boolean isIncludeOnlyAssignedEvents()
    {
        return AssignedUserSelectionMode.ANY.equals( this.assignedUserSelectionMode );
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
