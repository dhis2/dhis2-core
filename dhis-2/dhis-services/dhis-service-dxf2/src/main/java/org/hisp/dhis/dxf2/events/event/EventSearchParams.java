package org.hisp.dhis.dxf2.events.event;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public class EventSearchParams
{             
    public static final String EVENT_ID = "event";
    public static final String EVENT_CREATED_ID = "created";
    public static final String EVENT_LAST_UPDATED_ID = "lastUpdated";
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

    private TrackedEntityInstance trackedEntityInstance;

    private Date startDate;

    private Date endDate;

    private EventStatus eventStatus;

    private Date lastUpdatedStartDate;
    
    private Date lastUpdatedEndDate;
    
    private Date dueDateStart;
    
    private Date dueDateEnd;

    private DataElementCategoryOptionCombo categoryOptionCombo;

    private IdSchemes idSchemes;

    private Integer page;

    private Integer pageSize;

    private boolean totalPages;

    private boolean skipPaging;

    private List<Order> orders;
    
    private List<String> gridOrders;

    private boolean includeAttributes;

    private Set<String> events = new HashSet<>();
    
    /**
     * Filters for the response.
     */
    private List<QueryItem> filters = new ArrayList<>();    

    /**
     * DataElements to be included in the response. Can be used to filter response.
     */    
    private List<QueryItem> dataElements = new ArrayList<>();

    private boolean includeDeleted;

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
    
    /**
     * Indicates whether this search params contain any filters.
     * 
     */
    public boolean hasFilters()
    {
        return filters != null && !filters.isEmpty();
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
            if ( items != null && !items.contains( de ) )
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

    public void setProgram( Program program )
    {
        this.program = program;
    }

    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public void setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
    }

    public ProgramType getProgramType()
    {
        return programType;
    }

    public void setProgramType( ProgramType programType )
    {
        this.programType = programType;
    }

    public Boolean getFollowUp()
    {
        return followUp;
    }

    public void setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
    }

    public OrganisationUnit getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( OrganisationUnit orgUnit )
    {
        this.orgUnit = orgUnit;
    }

    public OrganisationUnitSelectionMode getOrgUnitSelectionMode()
    {
        return orgUnitSelectionMode;
    }

    public void setOrgUnitSelectionMode( OrganisationUnitSelectionMode orgUnitSelectionMode )
    {
        this.orgUnitSelectionMode = orgUnitSelectionMode;
    }

    public TrackedEntityInstance getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public void setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
    }    

    public Date getLastUpdatedStartDate()
    {
        return lastUpdatedStartDate;
    }

    public void setLastUpdatedStartDate( Date lastUpdatedStartDate )
    {
        this.lastUpdatedStartDate = lastUpdatedStartDate;
    }

    public Date getLastUpdatedEndDate()
    {
        return lastUpdatedEndDate;
    }

    public void setLastUpdatedEndDate( Date lastUpdatedEndDate )
    {
        this.lastUpdatedEndDate = lastUpdatedEndDate;
    }

    public Date getDueDateStart()
    {
        return dueDateStart;
    }

    public void setDueDateStart( Date dueDateStart )
    {
        this.dueDateStart = dueDateStart;
    }

    public Date getDueDateEnd()
    {
        return dueDateEnd;
    }

    public void setDueDateEnd( Date dueDateEnd )
    {
        this.dueDateEnd = dueDateEnd;
    }

    public IdSchemes getIdSchemes()
    {
        return idSchemes;
    }

    public void setIdSchemes( IdSchemes idSchemes )
    {
        this.idSchemes = idSchemes;
    }

    public Integer getPage()
    {
        return page;
    }

    public void setPage( Integer page )
    {
        this.page = page;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public void setPageSize( Integer pageSize )
    {
        this.pageSize = pageSize;
    }

    public boolean isTotalPages()
    {
        return totalPages;
    }

    public void setTotalPages( boolean totalPages )
    {
        this.totalPages = totalPages;
    }

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public void setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
    }

    public boolean isIncludeAttributes()
    {
        return includeAttributes;
    }

    public void setIncludeAttributes( boolean includeAttributes )
    {
        this.includeAttributes = includeAttributes;
    }

    public List<Order> getOrders()
    {
        return this.orders;
    }

    public void setOrders( List<Order> orders )
    {
        this.orders = orders;
    }
    
    public List<String> getGridOrders()
    {
        return this.gridOrders;
    }

    public void setGridOrders( List<String> gridOrders )
    {
        this.gridOrders = gridOrders;
    }

    public DataElementCategoryOptionCombo getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public void setCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
    }

    public void setEvents( Set<String> events )
    {
        this.events = events;
    }

    public Set<String> getEvents()
    {
        return events;
    }

    public List<QueryItem> getFilters()
    {
        return filters;
    }

    public void setFilters( List<QueryItem> filters )
    {
        this.filters = filters;
    }

    public void setIncludeDeleted( boolean includeDeleted )
    {
        this.includeDeleted = includeDeleted;
    }

    public boolean isIncludeDeleted()
    {
        return this.includeDeleted;
    }

    public List<QueryItem> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( List<QueryItem> dataElements )
    {
        this.dataElements = dataElements;
    }

}