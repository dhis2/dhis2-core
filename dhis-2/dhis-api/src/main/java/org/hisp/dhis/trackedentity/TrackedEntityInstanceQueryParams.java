package org.hisp.dhis.trackedentity;

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

import org.apache.commons.lang.time.DateUtils;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;

/**
 * @author Lars Helge Overland
 */
public class TrackedEntityInstanceQueryParams
{
    public static final String TRACKED_ENTITY_INSTANCE_ID = "instance";
    public static final String CREATED_ID = "created";
    public static final String LAST_UPDATED_ID = "lastupdated";
    public static final String ORG_UNIT_ID = "ou";
    public static final String ORG_UNIT_NAME = "ouname";
    public static final String TRACKED_ENTITY_ID = "te";
    public static final String TRACKED_ENTITY_ATTRIBUTE_ID = "teattribute";
    public static final String TRACKED_ENTITY_ATTRIBUTE_VALUE_ID = "tevalue";
    public static final String INACTIVE_ID = "inactive";
    public static final String DELETED = "deleted";

    public static final String META_DATA_NAMES_KEY = "names";
    public static final String PAGER_META_KEY = "pager";

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 50;

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
     * Start date for last updated.
     */
    private Date lastUpdatedStartDate;

    /**
     * End date for last updated.
     */
    private Date lastUpdatedEndDate;

    /**
     * Start date for enrollment in the given program.
     */
    private Date programEnrollmentStartDate;

    /**
     * End date for enrollment in the given program.
     */
    private Date programEnrollmentEndDate;

    /**
     * Start date for incident in the given program.
     */
    private Date programIncidentStartDate;

    /**
     * End date for incident in the given program.
     */
    private Date programIncidentEndDate;

    /**
     * Tracked entity of the instances in the response.
     */
    private TrackedEntity trackedEntity;

    /**
     * Selection mode for the specified organisation units, default is ACCESSIBLE.
     */
    private OrganisationUnitSelectionMode organisationUnitMode = OrganisationUnitSelectionMode.DESCENDANTS;

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

    /**
     * Indicates whether to include soft-deleted elements
     */
    private boolean includeDeleted;

    /**
     * TEI order params
     */
    private List<String> orders;

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    /**
     * Current user for query.
     */
    private transient User user;

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
    public TrackedEntityInstanceQueryParams addAttribute( QueryItem attribute )
    {
        this.attributes.add( attribute );
        return this;
    }

    /**
     * Adds a query item as filter to the parameters.
     */
    public TrackedEntityInstanceQueryParams addFilter( QueryItem filter )
    {
        this.filters.add( filter );
        return this;
    }

    /**
     * Adds an organisation unit to the parameters.
     */
    public TrackedEntityInstanceQueryParams addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
        return this;
    }

    /**
     * Performs a set of operations on this params.
     * <p>
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
     * Prepares the organisation units of the given parameters to simplify querying.
     * Mode ACCESSIBLE is converted to DESCENDANTS for organisation units linked
     * to the given user, and mode CHILDREN is converted to CHILDREN for organisation
     * units including all their children. Mode can be DESCENDANTS, SELECTED, ALL
     * only after invoking this method.
     *
     * @param user the user.
     */
    public void handleOrganisationUnits()
    {
        if ( user != null && isOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE ) )
        {
            setOrganisationUnits( user.getTeiSearchOrganisationUnitsWithFallback() );
            setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );
        }
        else if ( isOrganisationUnitMode( CHILDREN ) )
        {
            Set<OrganisationUnit> organisationUnits = new HashSet<>( getOrganisationUnits() );

            for ( OrganisationUnit organisationUnit : getOrganisationUnits() )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }

            setOrganisationUnits( organisationUnits );
            setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );
        }
    }

    public TrackedEntityInstanceQueryParams addAttributes( List<QueryItem> attrs )
    {
        attributes.addAll( attrs );
        return this;
    }

    /**
     * Add the given attributes to this params if they are not already present.
     */
    public TrackedEntityInstanceQueryParams addAttributesIfNotExist( List<QueryItem> attrs )
    {
        for ( QueryItem attr : attrs )
        {
            if ( attributes != null && !attributes.contains( attr ) )
            {
                attributes.add( attr );
            }
        }

        return this;
    }

    /**
     * Adds the given filters to this parameters if they are not already present.
     */
    public TrackedEntityInstanceQueryParams addFiltersIfNotExist( List<QueryItem> filtrs )
    {
        for ( QueryItem filter : filtrs )
        {
            if ( filters != null && !filters.contains( filter ) )
            {
                filters.add( filter );
            }
        }

        return this;
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
     * Indicates whether this parameters specifies a query.
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
     * Indicates whether this parameters specifies any attributes and/or filters.
     */
    public boolean hasAttributesOrFilters()
    {
        return hasAttributes() || hasFilters();
    }

    /**
     * Indicates whether this parameters specifies any attributes.
     */
    public boolean hasAttributes()
    {
        return attributes != null && !attributes.isEmpty();
    }

    /**
     * Indicates whether this parameters specifies any filters.
     */
    public boolean hasFilters()
    {
        return filters != null && !filters.isEmpty();
    }

    /**
     * Indicates whether this parameters specifies any organisation units.
     */
    public boolean hasOrganisationUnits()
    {
        return organisationUnits != null && !organisationUnits.isEmpty();
    }

    /**
     * Indicates whether this parameters specifies a program.
     */
    public boolean hasProgram()
    {
        return program != null;
    }

    /**
     * Indicates whether this parameters specifies a program status.
     */
    public boolean hasProgramStatus()
    {
        return programStatus != null;
    }

    /**
     * Indicates whether this parameters specifies follow up for the given program.
     * Follow up can be specified as true or false.
     */
    public boolean hasFollowUp()
    {
        return followUp != null;
    }

    public boolean hasLastUpdatedStartDate()
    {
        return lastUpdatedStartDate != null;
    }

    public boolean hasLastUpdatedEndDate()
    {
        return lastUpdatedEndDate != null;
    }

    /**
     * Indicates whether this parameters specifies a program enrollment start date.
     */
    public boolean hasProgramEnrollmentStartDate()
    {
        return programEnrollmentStartDate != null;
    }

    /**
     * Indicates whether this parameters specifies a program enrollment end date.
     */
    public boolean hasProgramEnrollmentEndDate()
    {
        return programEnrollmentEndDate != null;
    }

    /**
     * Indicates whether this parameters specifies a program incident start date.
     */
    public boolean hasProgramIncidentStartDate()
    {
        return programIncidentStartDate != null;
    }

    /**
     * Indicates whether this parameters specifies a program incident end date.
     */
    public boolean hasProgramIncidentEndDate()
    {
        return programIncidentEndDate != null;
    }

    /**
     * Indicates whether this parameters specifies a tracked entity.
     */
    public boolean hasTrackedEntity()
    {
        return trackedEntity != null;
    }

    /**
     * Indicates whether this parameters is of the given organisation unit mode.
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
     * Indicates whether this parameters specifies an event start date.
     */
    public boolean hasEventStartDate()
    {
        return eventStartDate != null;
    }

    /**
     * Indicates whether this parameters specifies an event end date.
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

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "[Query: " + query + ", Attributes: " + attributes + ", filters: " + filters +
            ", program: " + program + ", program status " + programStatus + ", follow up: " + followUp +
            ", program enrollemnt start date: " + programEnrollmentStartDate + ", program enrollment end date: " + programEnrollmentEndDate +
            ", program incident start date: " + programIncidentStartDate + ", program incident end date: " + programIncidentEndDate +
            ", tracked entity: " + trackedEntity + ", org unit mode: " + organisationUnitMode +
            ", event start date: " + eventStartDate + ", event end date: " + eventEndDate +
            ", event status: " + eventStatus + "]";
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public QueryFilter getQuery()
    {
        return query;
    }

    public TrackedEntityInstanceQueryParams setQuery( QueryFilter query )
    {
        this.query = query;
        return this;
    }

    public List<QueryItem> getAttributes()
    {
        return attributes;
    }

    public TrackedEntityInstanceQueryParams setAttributes( List<QueryItem> attributes )
    {
        this.attributes = attributes;
        return this;
    }

    public List<QueryItem> getFilters()
    {
        return filters;
    }

    public TrackedEntityInstanceQueryParams setFilters( List<QueryItem> filters )
    {
        this.filters = filters;
        return this;
    }

    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public TrackedEntityInstanceQueryParams setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    public Program getProgram()
    {
        return program;
    }

    public TrackedEntityInstanceQueryParams setProgram( Program program )
    {
        this.program = program;
        return this;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public TrackedEntityInstanceQueryParams setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
        return this;
    }

    public Boolean getFollowUp()
    {
        return followUp;
    }

    public TrackedEntityInstanceQueryParams setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
        return this;
    }

    public Date getLastUpdatedStartDate()
    {
        return lastUpdatedStartDate;
    }

    public TrackedEntityInstanceQueryParams setLastUpdatedStartDate( Date lastUpdatedStartDate )
    {
        this.lastUpdatedStartDate = lastUpdatedStartDate;
        return this;
    }

    public Date getLastUpdatedEndDate()
    {
        return lastUpdatedEndDate;
    }

    public TrackedEntityInstanceQueryParams setLastUpdatedEndDate( Date lastUpdatedEndDate )
    {
        this.lastUpdatedEndDate = lastUpdatedEndDate;
        return this;
    }

    public Date getProgramEnrollmentStartDate()
    {
        return programEnrollmentStartDate;
    }

    public TrackedEntityInstanceQueryParams setProgramEnrollmentStartDate( Date programEnrollmentStartDate )
    {
        this.programEnrollmentStartDate = programEnrollmentStartDate;
        return this;
    }

    public Date getProgramEnrollmentEndDate()
    {
        return programEnrollmentEndDate != null ? DateUtils.addDays( programEnrollmentEndDate, 1 ) : programEnrollmentEndDate;
    }

    public TrackedEntityInstanceQueryParams setProgramEnrollmentEndDate( Date programEnrollmentEndDate )
    {
        this.programEnrollmentEndDate = programEnrollmentEndDate;
        return this;
    }

    public Date getProgramIncidentStartDate()
    {
        return programIncidentStartDate;
    }

    public TrackedEntityInstanceQueryParams setProgramIncidentStartDate( Date programIncidentStartDate )
    {
        this.programIncidentStartDate = programIncidentStartDate;
        return this;
    }

    public Date getProgramIncidentEndDate()
    {
        return programIncidentEndDate != null ? DateUtils.addDays( programIncidentEndDate, 1 ) : programIncidentEndDate;
    }

    public TrackedEntityInstanceQueryParams setProgramIncidentEndDate( Date programIncidentEndDate )
    {
        this.programIncidentEndDate = programIncidentEndDate;
        return this;
    }

    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public TrackedEntityInstanceQueryParams setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
        return this;
    }

    public OrganisationUnitSelectionMode getOrganisationUnitMode()
    {
        return organisationUnitMode;
    }

    public TrackedEntityInstanceQueryParams setOrganisationUnitMode( OrganisationUnitSelectionMode organisationUnitMode )
    {
        this.organisationUnitMode = organisationUnitMode;
        return this;
    }

    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public TrackedEntityInstanceQueryParams setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
        return this;
    }

    public Date getEventStartDate()
    {
        return eventStartDate;
    }

    public TrackedEntityInstanceQueryParams setEventStartDate( Date eventStartDate )
    {
        this.eventStartDate = eventStartDate;
        return this;
    }

    public Date getEventEndDate()
    {
        return eventEndDate;
    }

    public TrackedEntityInstanceQueryParams setEventEndDate( Date eventEndDate )
    {
        this.eventEndDate = eventEndDate;
        return this;
    }

    public boolean isSkipMeta()
    {
        return skipMeta;
    }

    public TrackedEntityInstanceQueryParams setSkipMeta( boolean skipMeta )
    {
        this.skipMeta = skipMeta;
        return this;
    }

    public Integer getPage()
    {
        return page;
    }

    public TrackedEntityInstanceQueryParams setPage( Integer page )
    {
        this.page = page;
        return this;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public TrackedEntityInstanceQueryParams setPageSize( Integer pageSize )
    {
        this.pageSize = pageSize;
        return this;
    }

    public boolean isTotalPages()
    {
        return totalPages;
    }

    public TrackedEntityInstanceQueryParams setTotalPages( boolean totalPages )
    {
        this.totalPages = totalPages;
        return this;
    }

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public TrackedEntityInstanceQueryParams setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
        return this;
    }

    public boolean isIncludeDeleted()
    {
        return includeDeleted;
    }

    public TrackedEntityInstanceQueryParams setIncludeDeleted( boolean includeDeleted )
    {
        this.includeDeleted = includeDeleted;

        return this;
    }

    public User getUser()
    {
        return user;
    }

    public TrackedEntityInstanceQueryParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    public List<String> getOrders()
    {
        return orders;
    }

    public void setOrders( List<String> orders )
    {
        this.orders = orders;
    }
}
