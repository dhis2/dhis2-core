package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;

/**
 * @author Lars Helge Overland
 */
public class TrackedEntityInstanceQueryParams
{
    public static final String TRACKED_ENTITY_INSTANCE_ID = "instance";
    public static final String CREATED_ID = "created";
    public static final String LAST_UPDATED_ID = "lastupdated";
    public static final String ORG_UNIT_ID = "ou";
    public static final String TRACKED_ENTITY_ID = "te";
    public static final String TRACKED_ENTITY_ATTRIBUTE_ID = "teattribute";
    public static final String TRACKED_ENTITY_ATTRIBUTE_VALUE_ID = "tevalue";
    public static final String INACTIVE_ID = "inactive";
    
    public static final String META_DATA_NAMES_KEY = "names";
    public static final String PAGER_META_KEY = "pager";
    
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 50;
    
    /**
     * Last updated for TEI.
     */
    private Date lastUpdated;

    /**
     * Query value, will apply to all relevant attributes.
     */
    private QueryFilter query;
    
    /**
     * Attributes to be included in the response. Can be used to filter response.
     */
    private List<QueryItem> attributes = new ArrayList<>();

    /**
     * Filters for the response.
     */
    private List<QueryItem> filters = new ArrayList<>();
    
    /**
     * Organisation units for which instances in the response were registered at.
     * Is related to the specified OrganisationUnitMode.
     */
    private Set<OrganisationUnit> organisationUnits = new HashSet<>();
    
    /**
     * Program for which instances in the response must be enrolled in.
     */
    private Program program;
    
    /**
     * Status of the tracked entity instance in the given program.
     */
    private ProgramStatus programStatus;
    
    /**
     * Indicates whether tracked entity instance is marked for follow up for the
     * specified program.
     */
    private Boolean followUp;
    
    /**
     * Start date for enrollment in the given program.
     */
    private Date programStartDate;
    
    /**
     * End date for enrollment in the given program.
     */
    private Date programEndDate;
    
    /**
     * Tracked entity of the instances in the response.
     */
    private TrackedEntity trackedEntity;
    
    /**
     * Selection mode for the specified organisation units.
     */
    private OrganisationUnitSelectionMode organisationUnitMode;
    
    /**
     * Status of any events in the specified program.
     */
    private EventStatus eventStatus;
    
    /**
     * Start date for event for the given program.
     */
    private Date eventStartDate;
    
    /**
     * End date for event for the given program.
     */
    private Date eventEndDate;

    /**
     * Indicates whether not to include meta data in the response.
     */
    private boolean skipMeta;

    /**
     * Page number.
     */
    private Integer page;
    
    /**
     * Page size.
     */
    private Integer pageSize;
    
    /**
     * Indicates whether to include the total number of pages in the paging response.
     */
    private boolean totalPages;
    
    /**
     * Indicates whether paging should be skipped.
     */
    private boolean skipPaging;
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityInstanceQueryParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    
    /**
     * Adds a query item as attribute to the parameters.
     */
    public void addAttribute( QueryItem attribute )
    {
        this.attributes.add( attribute );
    }
    
    /**
     * Adds a query item as filter to the parameters.
     */
    public void addFilter( QueryItem filter )
    {
        this.filters.add( filter );
    }
    
    /**
     * Adds an organisation unit to the parameters.
     */
    public void addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
    }

    /**
     * Performs a set of operations on this params.
     * 
     * <ul>
     * <li>
     * If a query item is specified as an attribute item as well as a filter 
     * item, the filter item will be removed. In that case, if the attribute 
     * item does not have any filters and the filter item has one or more filters, 
     * these will be applied to the attribute item. 
     * </li>
     * </ul> 
     */
    public void conform()
    {
        Iterator<QueryItem> filterIter = filters.iterator();
        
        while ( filterIter.hasNext() )
        {
            QueryItem filter = filterIter.next();
        
            int index = attributes.indexOf( filter ); // Filter present as attr
            
            if ( index >= 0 )
            {
                QueryItem attribute = attributes.get( index );
                
                if ( !attribute.hasFilter() && filter.hasFilter() )
                {
                    attribute.getFilters().addAll( filter.getFilters() );
                }
                
                filterIter.remove();
            }
        }
    }
    
    /**
     * Add the given attributes to this params if they are not already present.
     */
    public void addAttributesIfNotExist( List<QueryItem> attrs )
    {
        for ( QueryItem attr : attrs )
        {
            if ( attributes != null && !attributes.contains( attr ) )
            {
                attributes.add( attr );            
            }
        }
    }
    
    /**
     * Adds the given filters to this params if they are not already present.
     */
    public void addFiltersIfNotExist( List<QueryItem> filtrs )
    {
        for ( QueryItem filter : filtrs )
        {
            if ( filters != null && !filters.contains( filter ) )
            {
                filters.add( filter );
            }
        }
    }
    /**
     * Indicates whether this is a logical OR query, meaning that a query string
     * is specified and instances which matches this query on one or more attributes
     * should be included in the response. The opposite is an item-specific query,
     * where the instances which matches the specific attributes should be included.
     */
    public boolean isOrQuery()
    {
        return hasQuery();
    }
    
    /**
     * Indicates whether this params specifies a query.
     */
    public boolean hasQuery()
    {
        return query != null && query.isFilter();
    }
    
    /**
     * Returns a list of attributes and filters combined.
     */
    public List<QueryItem> getAttributesAndFilters()
    {
        List<QueryItem> items = new ArrayList<>();
        items.addAll( attributes );
        items.addAll( filters );
        return items;
    }

    /**
     * Returns a list of attributes which appear more than once.
     */
    public List<QueryItem> getDuplicateAttributes()
    {
        Set<QueryItem> items = new HashSet<>();
        List<QueryItem> duplicates = new ArrayList<>();
        
        for ( QueryItem item : getAttributes() )
        {
            if ( !items.add( item ) )
            {
                duplicates.add( item );
            }
        }
        
        return duplicates;
    }

    /**
     * Returns a list of attributes which appear more than once.
     */
    public List<QueryItem> getDuplicateFilters()
    {
        Set<QueryItem> items = new HashSet<>();
        List<QueryItem> duplicates = new ArrayList<>();
        
        for ( QueryItem item : getFilters() )
        {
            if ( !items.add( item ) )
            {
                duplicates.add( item );
            }
        }
        
        return duplicates;
    }
           
    /**
     * Indicates whether this params specifies any attributes and/or filters.
     */
    public boolean hasAttributesOrFilters()
    {
        return hasAttributes() || hasFilters();
    }

    /**
     * Indicates whether this params specifies any attributes.
     */
    public boolean hasAttributes()
    {
        return attributes != null && !attributes.isEmpty();
    }
    
    /**
     * Indicates whether this params specifies any filters.
     */
    public boolean hasFilters()
    {
        return filters != null && !filters.isEmpty();
    }

    /**
     * Indicates whether this params specifies any organisation units.
     */
    public boolean hasOrganisationUnits()
    {
        return organisationUnits != null && !organisationUnits.isEmpty();
    }
    
    /**
     * Indicates whether this params specifies a program.
     */
    public boolean hasProgram()
    {
        return program != null;
    }
    
    /**
     * Indicates whether this params specifies a program status.
     */
    public boolean hasProgramStatus()
    {
        return programStatus != null;
    }
    
    /**
     * Indicates whether this params specifies follow up for the given program.
     * Follow up can be specified as true or false.
     */
    public boolean hasFollowUp()
    {
        return followUp != null;
    }
    
    /**
     * Indicates whether this params specifies a program start date.
     */
    public boolean hasProgramStartDate()
    {
        return programStartDate != null;
    }
    
    /**
     * Indicates whether this params specifies a program end date.
     */
    public boolean hasProgramEndDate()
    {
        return programEndDate != null;
    }
    
    /**
     * Indicates whether this params specifies a tracked entity.
     */
    public boolean hasTrackedEntity()
    {
        return trackedEntity != null;
    }
    
    /**
     * Indicates whether this params is of the given organisation unit mode.
     */
    public boolean isOrganisationUnitMode( OrganisationUnitSelectionMode mode )
    {
        return organisationUnitMode != null && organisationUnitMode.equals( mode );
    }
    
    /**
     * Indicates whether this params specifies an event status.
     */
    public boolean hasEventStatus()
    {
        return eventStatus != null;
    }
    
    /**
     * Indicates whether the event status specified for the params is equal to
     * the given event status.
     */
    public boolean isEventStatus( EventStatus eventStatus )
    {
        return this.eventStatus != null && this.eventStatus.equals( eventStatus );
    }
    
    /**
     * Indicates whether this params specifies an event start date.
     */
    public boolean hasEventStartDate()
    {
        return eventStartDate != null;
    }
    
    /**
     * Indicates whether this params specifies an event end date.
     */
    public boolean hasEventEndDate()
    {
        return eventEndDate != null;
    }
    
    /**
     * Indicates whether paging is enabled.
     */
    public boolean isPaging()
    {
        return page != null || pageSize != null;
    }

    /**
     * Returns the page number, falls back to default value of 1 if not specified.
     */
    public int getPageWithDefault()
    {
        return page != null && page > 0 ? page : DEFAULT_PAGE;
    }
    
    /**
     * Returns the page size, falls back to default value of 50 if not specified.
     */
    public int getPageSizeWithDefault()
    {
        return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Returns the offset based on the page number and page size.
     */
    public int getOffset()
    {
        return ( getPageWithDefault() - 1 ) * getPageSizeWithDefault();
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

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "[Query: " + query + ", Attributes: " + attributes + ", filters: " + filters + 
            ", program: " + program + ", program status " + programStatus + ", follow up: " + followUp + 
            ", program start date: " + programStartDate + ", program end date: " + programEndDate + 
            ", tracked entity: " + trackedEntity + ", org unit mode: " + organisationUnitMode + 
            ", event start date: " + eventStartDate + ", event end date: " + eventEndDate + 
            ", event status: " + eventStatus + "]";
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    public QueryFilter getQuery()
    {
        return query;
    }

    public void setQuery( QueryFilter query )
    {
        this.query = query;
    }

    public List<QueryItem> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<QueryItem> attributes )
    {
        this.attributes = attributes;
    }

    public List<QueryItem> getFilters()
    {
        return filters;
    }

    public void setFilters( List<QueryItem> filters )
    {
        this.filters = filters;
    }

    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public void setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
    }

    public Boolean getFollowUp()
    {
        return followUp;
    }

    public void setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
    }

    public Date getProgramStartDate()
    {
        return programStartDate;
    }

    public void setProgramStartDate( Date programStartDate )
    {
        this.programStartDate = programStartDate;
    }

    public Date getProgramEndDate()
    {
    	return programEndDate != null ? DateUtils.addDays(programEndDate, 1) : programEndDate;
    }

    public void setProgramEndDate( Date programEndDate )
    {
        this.programEndDate = programEndDate;
    }

    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    public OrganisationUnitSelectionMode getOrganisationUnitMode()
    {
        return organisationUnitMode;
    }

    public void setOrganisationUnitMode( OrganisationUnitSelectionMode organisationUnitMode )
    {
        this.organisationUnitMode = organisationUnitMode;
    }

    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public void setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
    }

    public Date getEventStartDate()
    {
        return eventStartDate;
    }

    public void setEventStartDate( Date eventStartDate )
    {
        this.eventStartDate = eventStartDate;
    }

    public Date getEventEndDate()
    {
        return eventEndDate;
    }

    public void setEventEndDate( Date eventEndDate )
    {
        this.eventEndDate = eventEndDate;
    }

    public boolean isSkipMeta()
    {
        return skipMeta;
    }

    public void setSkipMeta( boolean skipMeta )
    {
        this.skipMeta = skipMeta;
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
}
