package org.hisp.dhis.analytics.event;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Lars Helge Overland
 */
public class EventQueryParams
    extends DataQueryParams
{
    /**
     * The start date for the period dimension, can be null.
     */
    private Date startDate;

    /**
     * The end date fore the period dimension, can be null.
     */
    private Date endDate;

    /**
     * The query items.
     */
    private List<QueryItem> items = new ArrayList<>();

    /**
     * The query item filters.
     */
    private List<QueryItem> itemFilters = new ArrayList<>();

    /**
     * The dimensional object for which to produce aggregated data.
     */
    private DimensionalItemObject value;

    /**
     * Program indicators specified as dimensional items of the data dimension.
     */
    private List<ProgramIndicator> itemProgramIndicators = new ArrayList<>();

    /**
     * The program indicator for which to produce aggregated data.
     */
    private ProgramIndicator programIndicator;

    /**
     * Columns to sort ascending.
     */
    private List<String> asc = new ArrayList<>();

    /**
     * Columns to sort descending.
     */
    private List<String> desc = new ArrayList<>();
    
    /**
     * The organisation unit selection mode.
     */
    private OrganisationUnitSelectionMode organisationUnitMode;

    /**
     * The page number.
     */
    private Integer page;

    /**
     * The page size.
     */
    private Integer pageSize;

    /**
     * The value sort order.
     */
    private SortOrder sortOrder;

    /**
     * The max limit of records to return.
     */
    private Integer limit;

    /**
     * Indicates the event output type which can be by event, enrollment type
     * or tracked entity instance.
     */
    private EventOutputType outputType;

    /**
     * Indicates whether the data dimension items should be collapsed into a
     * single dimension.
     */
    private boolean collapseDataDimensions;

    /**
     * Indicates whether request is intended to fetch coordinates only.
     */
    private boolean coordinatesOnly;

    /**
     * Indicates whether the query originates from an aggregate data query.
     */
    private boolean aggregateData;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public EventQueryParams()
    {
    }

    @Override
    public EventQueryParams instance()
    {
        EventQueryParams params = new EventQueryParams();

        params.dimensions = new ArrayList<>( this.dimensions );
        params.filters = new ArrayList<>( this.filters );
        params.displayProperty = this.displayProperty;
        params.aggregationType = this.aggregationType;
        params.skipRounding = this.skipRounding;

        params.partitions = new Partitions( this.partitions );
        params.periodType = this.periodType;

        params.program = this.program;
        params.programStage = this.programStage;
        params.startDate = this.startDate;
        params.endDate = this.endDate;
        params.items = new ArrayList<>( this.items );
        params.itemFilters = new ArrayList<>( this.itemFilters );
        params.value = this.value;
        params.itemProgramIndicators = new ArrayList<>( this.itemProgramIndicators );
        params.programIndicator = this.programIndicator;
        params.asc = new ArrayList<>( this.asc );
        params.desc = new ArrayList<>( this.desc );
        params.completedOnly = this.completedOnly;
        params.organisationUnitMode = this.organisationUnitMode;
        params.page = this.page;
        params.pageSize = this.pageSize;
        params.sortOrder = this.sortOrder;
        params.limit = this.limit;
        params.outputType = this.outputType;
        params.collapseDataDimensions = this.collapseDataDimensions;
        params.coordinatesOnly = this.coordinatesOnly;
        params.aggregateData = this.aggregateData;

        params.periodType = this.periodType;

        return params;
    }

    public static EventQueryParams fromDataQueryParams( DataQueryParams dataQueryParams )
    {
        EventQueryParams params = new EventQueryParams();

        dataQueryParams.copyTo( params );

        for ( DimensionalItemObject object : dataQueryParams.getProgramDataElements() )
        {
            ProgramDataElement element = (ProgramDataElement) object;
            DataElement dataElement = element.getDataElement(); 
            QueryItem item = new QueryItem( dataElement, dataElement.getLegendSet(), dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            params.getItems().add( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getProgramAttributes() )
        {
            ProgramTrackedEntityAttribute element = (ProgramTrackedEntityAttribute) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute, attribute.getLegendSet(), attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
            item.setProgram( element.getProgram() );
            params.getItems().add( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramDataElements() )
        {
            ProgramDataElement element = (ProgramDataElement) object;
            DataElement dataElement = element.getDataElement(); 
            QueryItem item = new QueryItem( dataElement, dataElement.getLegendSet(), dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            params.getItemFilters().add( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramAttributes() )
        {
            ProgramTrackedEntityAttribute element = (ProgramTrackedEntityAttribute) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute, attribute.getLegendSet(), attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
            params.getItemFilters().add( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getProgramIndicators() )
        {
            ProgramIndicator programIndicator = (ProgramIndicator) object;
            params.getItemProgramIndicators().add( programIndicator );
        }

        params.setAggregateData( true );
        params.removeDimension( DATA_X_DIM_ID );

        return params;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Replaces periods with start and end dates, using the earliest start date
     * from the periods as start date and the latest end date from the periods
     * as end date. Remove the period dimension or filter.
     */
    public void replacePeriodsWithStartEndDates()
    {
        List<Period> periods = asTypedList( getDimensionOrFilterItems( PERIOD_DIM_ID ) );

        for ( Period period : periods )
        {
            Date start = period.getStartDate();
            Date end = period.getEndDate();

            if ( startDate == null || (start != null && start.before( startDate )) )
            {
                startDate = start;
            }

            if ( endDate == null || (end != null && end.after( endDate )) )
            {
                endDate = end;
            }
        }

        removeDimensionOrFilter( PERIOD_DIM_ID );
    }

    /**
     * Returns a list of query items which occur more than once, not including
     * the first duplicate.
     */
    public List<QueryItem> getDuplicateQueryItems()
    {
        Set<QueryItem> dims = new HashSet<>();
        List<QueryItem> duplicates = new ArrayList<>();

        for ( QueryItem dim : items )
        {
            if ( !dims.add( dim ) )
            {
                duplicates.add( dim );
            }
        }

        return duplicates;
    }
    
    /**
     * Returns a list of items and item filters.
     */
    public List<QueryItem> getItemsAndItemFilters()
    {
        return ListUtils.union( items, itemFilters );
    }

    /**
     * Get nameable objects part of items and item filters.
     */
    public Set<DimensionalItemObject> getDimensionalObjectItems()
    {
        Set<DimensionalItemObject> objects = new HashSet<>();

        for ( QueryItem item : ListUtils.union( items, itemFilters ) )
        {
            objects.add( item.getItem() );
        }

        return objects;
    }

    /**
     * Get legend sets part of items and item filters.
     */
    public Set<Legend> getLegends()
    {
        Set<Legend> legends = new HashSet<>();

        for ( QueryItem item : ListUtils.union( items, itemFilters ) )
        {
            if ( item.hasLegendSet() )
            {
                legends.addAll( item.getLegendSet().getLegends() );
            }
        }

        return legends;
    }

    /**
     * Get option sets part of items.
     */
    public Set<OptionSet> getItemOptionSets()
    {
        Set<OptionSet> optionSets = new HashSet<>();

        for ( QueryItem item : items )
        {
            if ( item.hasOptionSet() )
            {
                optionSets.add( item.getOptionSet() );
            }
        }

        return optionSets;
    }

    /**
     * Removes items and item filters of type program indicators.
     */
    public EventQueryParams removeProgramIndicatorItems()
    {
        items = items.stream().filter( item -> !item.isProgramIndicator() ).collect( Collectors.toList() );
        itemFilters = itemFilters.stream().filter( item -> !item.isProgramIndicator() ).collect( Collectors.toList() );
        return this;
    }

    /**
     * Returns the aggregation type for this query, first by looking at the
     * aggregation type of the query, second by looking at the aggregation type
     * of the value dimension, third by returning AVERAGE;
     */
    public AggregationType getAggregationTypeFallback()
    {
        if ( hasAggregationType() )
        {
            return aggregationType;
        }
        else if ( hasValueDimension() && value.getAggregationType() != null )
        {
            return value.getAggregationType();
        }

        return AggregationType.AVERAGE;
    }

    /**
     * Indicates whether this object is of the given aggregation type. Based on
     * {@link getAggregationTypeFallback}.
     */
    @Override
    public boolean isAggregationType( AggregationType aggregationType )
    {
        AggregationType type = getAggregationTypeFallback();

        return type != null && type.equals( aggregationType );
    }

    /**
     * Indicates whether this query is of the given organisation unit mode.
     */
    public boolean isOrganisationUnitMode( OrganisationUnitSelectionMode mode )
    {
        return organisationUnitMode != null && organisationUnitMode == mode;
    }

    /**
     * Indicates whether any items or item filters are present.
     */
    public boolean hasItemsOrItemFilters()
    {
        return !items.isEmpty() || !itemFilters.isEmpty();
    }

    /**
     * Indicates whether this query has a start and end date.
     */
    public boolean hasStartEndDate()
    {
        return startDate != null && endDate != null;
    }

    public Set<OrganisationUnit> getOrganisationUnitChildren()
    {
        Set<OrganisationUnit> children = new HashSet<>();

        for ( DimensionalItemObject object : getDimensionOrFilterItems( DimensionalObject.ORGUNIT_DIM_ID ) )
        {
            OrganisationUnit unit = (OrganisationUnit) object;
            children.addAll( unit.getChildren() );
        }

        return children;
    }

    public boolean isSorting()
    {
        return (asc != null && !asc.isEmpty()) || (desc != null && !desc.isEmpty());
    }

    public boolean isPaging()
    {
        return page != null || pageSize != null;
    }

    public int getPageWithDefault()
    {
        return page != null && page > 0 ? page : 1;
    }

    public int getPageSizeWithDefault()
    {
        return pageSize != null && pageSize >= 0 ? pageSize : 50;
    }

    public int getOffset()
    {
        return (getPageWithDefault() - 1) * getPageSizeWithDefault();
    }

    public boolean hasSortOrder()
    {
        return sortOrder != null;
    }

    public boolean hasLimit()
    {
        return limit != null && limit > 0;
    }

    public boolean hasValueDimension()
    {
        return value != null;
    }

    public boolean hasProgramIndicatorDimension()
    {
        return programIndicator != null;
    }

    /**
     * Indicates whether the program of this query requires registration of
     * tracked entity instances.
     */
    public boolean isProgramRegistration()
    {
        return program != null && program.isRegistration();
    }

    /**
     * Returns a negative integer in case of ascending sort order, a positive in
     * case of descending sort order and 0 in case of no sort order.
     */
    public int getSortOrderAsInt()
    {
        return SortOrder.ASC.equals( sortOrder ) ? -1 : SortOrder.DESC.equals( sortOrder ) ? 1 : 0;
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new HashMap<>();
        
        map.put( "Program", program );
        map.put( "Stage", programStage );
        map.put( "Start date", startDate );
        map.put( "End date", endDate );
        map.put( "Items", items );
        map.put( "Item filters", itemFilters );
        map.put( "Value", value );
        map.put( "Item program indicators", itemProgramIndicators );
        map.put( "Program indicator", programIndicator );
        map.put( "Aggregation type", aggregationType );
        map.put( "Dimensions", dimensions );
        map.put( "Filters", filters );
        
        return JacksonUtils.toJsonAsStringSilent( map );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

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

    public List<QueryItem> getItems()
    {
        return items;
    }

    public void setItems( List<QueryItem> items )
    {
        this.items = items;
    }

    public List<QueryItem> getItemFilters()
    {
        return itemFilters;
    }

    public void setItemFilters( List<QueryItem> itemFilters )
    {
        this.itemFilters = itemFilters;
    }

    public DimensionalItemObject getValue()
    {
        return value;
    }

    public void setValue( DimensionalItemObject value )
    {
        this.value = value;
    }

    public List<ProgramIndicator> getItemProgramIndicators()
    {
        return itemProgramIndicators;
    }

    public void setItemProgramIndicators( List<ProgramIndicator> itemProgramIndicators )
    {
        this.itemProgramIndicators = itemProgramIndicators;
    }

    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator( ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }

    public List<String> getAsc()
    {
        return asc;
    }

    @Override
    public List<DimensionalObject> getDimensions()
    {
        return dimensions;
    }

    @Override
    public void setDimensions( List<DimensionalObject> dimensions )
    {
        this.dimensions = dimensions;
    }

    public void setAsc( List<String> asc )
    {
        this.asc = asc;
    }

    public List<String> getDesc()
    {
        return desc;
    }

    public void setDesc( List<String> desc )
    {
        this.desc = desc;
    }

    public OrganisationUnitSelectionMode getOrganisationUnitMode()
    {
        return organisationUnitMode;
    }

    public void setOrganisationUnitMode( OrganisationUnitSelectionMode organisationUnitMode )
    {
        this.organisationUnitMode = organisationUnitMode;
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

    public SortOrder getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( SortOrder sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public void setLimit( Integer limit )
    {
        this.limit = limit;
    }

    public EventOutputType getOutputType()
    {
        return outputType;
    }

    public void setOutputType( EventOutputType outputType )
    {
        this.outputType = outputType;
    }

    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public void setCollapseDataDimensions( boolean collapseDataDimensions )
    {
        this.collapseDataDimensions = collapseDataDimensions;
    }

    public boolean isCoordinatesOnly()
    {
        return coordinatesOnly;
    }

    public void setCoordinatesOnly( boolean coordinatesOnly )
    {
        this.coordinatesOnly = coordinatesOnly;
    }

    public boolean isAggregateData()
    {
        return aggregateData;
    }

    public void setAggregateData( boolean aggregateData )
    {
        this.aggregateData = aggregateData;
    }
}
