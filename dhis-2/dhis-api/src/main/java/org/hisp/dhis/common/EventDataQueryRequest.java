package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018; University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms; with or without
 * modification; are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice; this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice;
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES; INCLUDING; BUT NOT LIMITED TO; THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT; INDIRECT; INCIDENTAL; SPECIAL; EXEMPLARY; OR CONSEQUENTIAL DAMAGES
 * (INCLUDING; BUT NOT LIMITED TO; PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE; DATA; OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY; WHETHER IN CONTRACT; STRICT LIABILITY; OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE; EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;

public class EventDataQueryRequest
{
    protected String program;

    protected String stage;

    protected Date startDate;

    protected Date endDate;

    protected Set<String> dimension;

    protected Set<String> filter;

    protected String value;

    protected AggregationType aggregationType;

    protected boolean skipMeta;

    protected boolean skipData;

    protected boolean skipRounding;

    protected boolean completedOnly;

    protected boolean hierarchyMeta;

    protected boolean showHierarchy;

    protected SortOrder sortOrder;

    protected Integer limit;

    protected EventOutputType outputType;

    protected EventStatus eventStatus;

    protected ProgramStatus programStatus;

    protected boolean collapseDataDimensions;

    protected boolean aggregateData;

    protected boolean includeMetadataDetails;

    protected DisplayProperty displayProperty;

    protected Date relativePeriodDate;

    protected String userOrgUnit;

    protected DhisApiVersion apiVersion;

    protected OrganisationUnitSelectionMode ouMode;

    protected Set<String> asc;

    protected Set<String> desc;

    protected boolean coordinatesOnly;

    protected String coordinateField;

    protected Integer page;

    protected Integer pageSize;

    public String getProgram()
    {
        return program;
    }

    public String getStage()
    {
        return stage;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public Set<String> getDimension()
    {
        return dimension;
    }

    public Set<String> getFilter()
    {
        return filter;
    }

    public String getValue()
    {
        return value;
    }

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public boolean isSkipMeta()
    {
        return skipMeta;
    }

    public boolean isSkipData()
    {
        return skipData;
    }

    public boolean isSkipRounding()
    {
        return skipRounding;
    }

    public boolean isCompletedOnly()
    {
        return completedOnly;
    }

    public boolean isHierarchyMeta()
    {
        return hierarchyMeta;
    }

    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public SortOrder getSortOrder()
    {
        return sortOrder;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public EventOutputType getOutputType()
    {
        return outputType;
    }

    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public boolean isAggregateData()
    {
        return aggregateData;
    }

    public boolean isIncludeMetadataDetails()
    {
        return includeMetadataDetails;
    }

    public DisplayProperty getDisplayProperty()
    {
        return displayProperty;
    }

    public Date getRelativePeriodDate()
    {
        return relativePeriodDate;
    }

    public String getUserOrgUnit()
    {
        return userOrgUnit;
    }

    public DhisApiVersion getApiVersion()
    {
        return apiVersion;
    }

    public OrganisationUnitSelectionMode getOuMode()
    {
        return ouMode;
    }

    public Set<String> getAsc()
    {
        return asc;
    }

    public Set<String> getDesc()
    {
        return desc;
    }

    public boolean isCoordinatesOnly()
    {
        return coordinatesOnly;
    }

    public String getCoordinateField()
    {
        return coordinateField;
    }

    public Integer getPage()
    {
        return page;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    /**
     * Copies all properties of this request onto the given request.
     * 
     * @param request the request to copy properties onto.
     * @return the given request with all properties of this request set.
     */
    public <T extends EventDataQueryRequest> T copyTo( T request )
    {
        request.program = this.program;
        request.stage = this.stage;
        request.startDate = this.startDate;
        request.endDate = this.endDate;
        request.dimension = new HashSet<>( this.dimension );
        request.filter = new HashSet<>( this.filter );
        request.value = this.value;
        request.aggregationType = this.aggregationType;
        request.skipMeta = this.skipMeta;
        request.skipData = this.skipData;
        request.skipRounding = this.skipRounding;
        request.completedOnly = this.completedOnly;
        request.hierarchyMeta = this.hierarchyMeta;
        request.showHierarchy = this.showHierarchy;
        request.sortOrder = this.sortOrder;
        request.limit = this.limit;
        request.outputType = this.outputType;
        request.eventStatus = this.eventStatus;
        request.programStatus = this.programStatus;
        request.collapseDataDimensions = this.collapseDataDimensions;
        request.aggregateData = this.aggregateData;
        request.includeMetadataDetails = this.includeMetadataDetails;
        request.displayProperty = this.displayProperty;
        request.relativePeriodDate = this.relativePeriodDate;
        request.userOrgUnit = this.userOrgUnit;
        request.apiVersion = this.apiVersion;
        request.ouMode = this.ouMode;
        request.asc = new HashSet<>( this.asc );
        request.desc = new HashSet<>( this.desc );
        request.coordinatesOnly = this.coordinatesOnly;
        request.coordinateField = this.coordinateField;
        request.page = this.page;
        request.pageSize = this.pageSize;
        return request;
    }

    public static EventDataQueryRequestBuilder newBuilder()
    {
        return new EventDataQueryRequest.EventDataQueryRequestBuilder();
    }

    protected EventDataQueryRequest()
    {
    }

    protected EventDataQueryRequest instance()
    {
        return copyTo( new EventDataQueryRequest() );
    }

    public static class EventDataQueryRequestBuilder
    {
        private EventDataQueryRequest request;

        protected EventDataQueryRequestBuilder()
        {
            this.request = new EventDataQueryRequest();
        }

        protected EventDataQueryRequestBuilder( EventDataQueryRequest request )
        {
            this.request = request.instance();
        }

        public EventDataQueryRequestBuilder program( String program )
        {
            this.request.program = program;
            return this;
        }

        public EventDataQueryRequestBuilder stage( String stage )
        {
            this.request.stage = stage;
            return this;
        }

        public EventDataQueryRequestBuilder startDate( Date startDate )
        {
            this.request.startDate = startDate;
            return this;
        }

        public EventDataQueryRequestBuilder endDate( Date endDate )
        {
            this.request.endDate = endDate;
            return this;
        }

        public EventDataQueryRequestBuilder dimension( Set<String> dimension )
        {
            this.request.dimension = dimension;
            return this;
        }

        public EventDataQueryRequestBuilder filter( Set<String> filter )
        {
            this.request.filter = filter;
            return this;
        }

        public EventDataQueryRequestBuilder value( String value )
        {
            this.request.value = value;
            return this;
        }

        public EventDataQueryRequestBuilder aggregationType( AggregationType aggregationType )
        {
            this.request.aggregationType = aggregationType;
            return this;
        }

        public EventDataQueryRequestBuilder skipMeta( boolean skipMeta )
        {
            this.request.skipMeta = skipMeta;
            return this;
        }

        public EventDataQueryRequestBuilder skipData( boolean skipData )
        {
            this.request.skipData = skipData;
            return this;
        }

        public EventDataQueryRequestBuilder skipRounding( boolean skipRounding )
        {
            this.request.skipRounding = skipRounding;
            return this;
        }

        public EventDataQueryRequestBuilder completedOnly( boolean completedOnly )
        {
            this.request.completedOnly = completedOnly;
            return this;
        }

        public EventDataQueryRequestBuilder hierarchyMeta( boolean hierarchyMeta )
        {
            this.request.hierarchyMeta = hierarchyMeta;
            return this;
        }

        public EventDataQueryRequestBuilder showHierarchy( boolean showHierarchy )
        {
            this.request.showHierarchy = showHierarchy;
            return this;
        }

        public EventDataQueryRequestBuilder sortOrder( SortOrder sortOrder )
        {
            this.request.sortOrder = sortOrder;
            return this;
        }

        public EventDataQueryRequestBuilder limit( Integer limit )
        {
            this.request.limit = limit;
            return this;
        }

        public EventDataQueryRequestBuilder outputType( EventOutputType outputType )
        {
            this.request.outputType = outputType;
            return this;
        }

        public EventDataQueryRequestBuilder eventStatus( EventStatus eventStatus )
        {
            this.request.eventStatus = eventStatus;
            return this;
        }

        public EventDataQueryRequestBuilder programStatus( ProgramStatus programStatus )
        {
            this.request.programStatus = programStatus;
            return this;
        }

        public EventDataQueryRequestBuilder collapseDataDimensions( boolean collapseDataDimensions )
        {
            this.request.collapseDataDimensions = collapseDataDimensions;
            return this;
        }

        public EventDataQueryRequestBuilder aggregateData( boolean aggregateData )
        {
            this.request.aggregateData = aggregateData;
            return this;
        }

        public EventDataQueryRequestBuilder includeMetadataDetails( boolean includeMetadataDetails )
        {
            this.request.includeMetadataDetails = includeMetadataDetails;
            return this;
        }

        public EventDataQueryRequestBuilder displayProperty( DisplayProperty displayProperty )
        {
            this.request.displayProperty = displayProperty;
            return this;
        }

        public EventDataQueryRequestBuilder relativePeriodDate( Date relativePeriodDate )
        {
            this.request.relativePeriodDate = relativePeriodDate;
            return this;
        }

        public EventDataQueryRequestBuilder userOrgUnit( String userOrgUnit )
        {
            this.request.userOrgUnit = userOrgUnit;
            return this;
        }

        public EventDataQueryRequestBuilder apiVersion( DhisApiVersion apiVersion )
        {
            this.request.apiVersion = apiVersion;
            return this;
        }

        public EventDataQueryRequestBuilder ouMode( OrganisationUnitSelectionMode ouMode )
        {
            this.request.ouMode = ouMode;
            return this;
        }

        public EventDataQueryRequestBuilder asc( Set<String> asc )
        {
            this.request.asc = asc;
            return this;
        }

        public EventDataQueryRequestBuilder desc( Set<String> desc )
        {
            this.request.desc = desc;
            return this;
        }

        public EventDataQueryRequestBuilder coordinatesOnly( boolean coordinatesOnly )
        {
            this.request.coordinatesOnly = coordinatesOnly;
            return this;
        }

        public EventDataQueryRequestBuilder coordinateField( String coordinateField )
        {
            this.request.coordinateField = coordinateField;
            return this;
        }

        public EventDataQueryRequestBuilder page( Integer page )
        {
            this.request.page = page;
            return this;
        }

        public EventDataQueryRequestBuilder pageSize( Integer pageSize )
        {
            this.request.pageSize = pageSize;
            return this;
        }

        public EventDataQueryRequest build()
        {
            return request;
        }

    }

}
