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
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
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
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Lars Helge Overland
 */
public class EventQueryParams
    extends DataQueryParams
{
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
     * Indicates the event status.
     */
    private EventStatus eventStatus;

    /**
     * Indicates whether the data dimension items should be collapsed into a
     * single dimension.
     */
    private boolean collapseDataDimensions;

    /**
     * Indicates whether request is intended to fetch events with coordinates only.
     */
    private boolean coordinatesOnly;
    
    /**
     * Indicates whether request is intended to fetch events with geometry only.
     */
    private boolean geometryOnly;

    /**
     * Indicates whether the query originates from an aggregate data query.
     */
    private boolean aggregateData;
    
    /**
     * Size of cluster in meter.
     */
    private Long clusterSize;

    /**
     * Bounding box for events to include in clustering.
     */
    private String bbox;
    
    /**
     * Indicates whether to include underlying points for each cluster.
     */
    private boolean includeClusterPoints;
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    private EventQueryParams()
    {
    }
    
    @Override
    protected EventQueryParams instance()
    {
        EventQueryParams params = new EventQueryParams();

        params.dimensions = new ArrayList<>( this.dimensions );
        params.filters = new ArrayList<>( this.filters );
        params.includeNumDen = this.includeNumDen;
        params.displayProperty = this.displayProperty;
        params.aggregationType = this.aggregationType;
        params.skipRounding = this.skipRounding;
        params.startDate = this.startDate;
        params.endDate = this.endDate;

        params.partitions = new Partitions( this.partitions );
        params.periodType = this.periodType;

        params.program = this.program;
        params.programStage = this.programStage;
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
        params.eventStatus = this.eventStatus;
        params.collapseDataDimensions = this.collapseDataDimensions;
        params.coordinatesOnly = this.coordinatesOnly;
        params.geometryOnly = this.geometryOnly;
        params.aggregateData = this.aggregateData;
        params.clusterSize = this.clusterSize;
        params.bbox = this.bbox;
        params.includeClusterPoints = this.includeClusterPoints;

        params.periodType = this.periodType;

        return params;
    }

    public static EventQueryParams fromDataQueryParams( DataQueryParams dataQueryParams )
    {
        EventQueryParams params = new EventQueryParams();
        
        dataQueryParams.copyTo( params );
        
        EventQueryParams.Builder builder = new EventQueryParams.Builder( params );

        for ( DimensionalItemObject object : dataQueryParams.getProgramDataElements() )
        {
            ProgramDataElement element = (ProgramDataElement) object;
            DataElement dataElement = element.getDataElement(); 
            QueryItem item = new QueryItem( dataElement, dataElement.getLegendSet(), dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItem( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getProgramAttributes() )
        {
            ProgramTrackedEntityAttribute element = (ProgramTrackedEntityAttribute) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute, attribute.getLegendSet(), attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItem( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramDataElements() )
        {
            ProgramDataElement element = (ProgramDataElement) object;
            DataElement dataElement = element.getDataElement(); 
            QueryItem item = new QueryItem( dataElement, dataElement.getLegendSet(), dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItemFilter( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramAttributes() )
        {
            ProgramTrackedEntityAttribute element = (ProgramTrackedEntityAttribute) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute, attribute.getLegendSet(), attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
            builder.addItemFilter( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getProgramIndicators() )
        {
            ProgramIndicator programIndicator = (ProgramIndicator) object;
            builder.addItemProgramIndicator( programIndicator );
        }

        return builder
            .withAggregateData( true )
            .removeDimension( DATA_X_DIM_ID ).build();
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
    
    public boolean hasEventStatus()
    {
        return eventStatus != null;
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
    
    public boolean hasClusterSize()
    {
        return clusterSize != null;
    }
    
    public boolean hasBbox()
    {
        return bbox != null && !bbox.isEmpty();
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
        
        return map.toString(); //TODO
    }

    // -------------------------------------------------------------------------
    // Get methods
    // -------------------------------------------------------------------------

    public List<QueryItem> getItems()
    {
        return items;
    }

    public List<QueryItem> getItemFilters()
    {
        return itemFilters;
    }

    public DimensionalItemObject getValue()
    {
        return value;
    }

    public List<ProgramIndicator> getItemProgramIndicators()
    {
        return itemProgramIndicators;
    }

    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
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

    public List<String> getDesc()
    {
        return desc;
    }

    public OrganisationUnitSelectionMode getOrganisationUnitMode()
    {
        return organisationUnitMode;
    }

    public Integer getPage()
    {
        return page;
    }

    public Integer getPageSize()
    {
        return pageSize;
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

    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public boolean isCoordinatesOnly()
    {
        return coordinatesOnly;
    }

    public boolean isGeometryOnly()
    {
        return geometryOnly;
    }

    public void setGeometryOnly( boolean geometryOnly )
    {
        this.geometryOnly = geometryOnly; //TODO builder
    }

    public boolean isAggregateData()
    {
        return aggregateData;
    }

    public Long getClusterSize()
    {
        return clusterSize;
    }

    public String getBbox()
    {
        return bbox;
    }

    public boolean isIncludeClusterPoints()
    {
        return includeClusterPoints;
    }
    
    // -------------------------------------------------------------------------
    // Builder of immutable instances
    // -------------------------------------------------------------------------

    /**
     * Builder for {@link DataQueryParams} instances.
     */
    public static class Builder
    {
        private EventQueryParams params;
        
        public Builder()
        {
            this.params = new EventQueryParams();
        }

        public Builder( DataQueryParams dataQueryParams )
        {
            EventQueryParams eventQueryParams = EventQueryParams.fromDataQueryParams( dataQueryParams );
            
            this.params = eventQueryParams.instance();
        }

        public Builder( EventQueryParams params )
        {
            this.params = params.instance();
        }

        public Builder withProgram( Program program )
        {
            this.params.program = program;
            return this;
        }

        public Builder withProgramStage( ProgramStage programStage )
        {
            this.params.programStage = programStage;
            return this;
        }

        public Builder withStartDate( Date startDate )
        {
            this.params.startDate = startDate;
            return this;
        }

        public Builder withEndDate( Date endDate )
        {
            this.params.endDate = endDate;
            return this;
        }
        
        public Builder addDimension( DimensionalObject dimension )
        {
            this.params.addDimension( dimension );
            return this;
        }

        public Builder removeDimension( String dimension )
        {
            this.params.dimensions.remove( new BaseDimensionalObject( dimension ) );
            return this;
        }
        
        public Builder withOrganisationUnits( List<? extends DimensionalItemObject> organisationUnits )
        {
            this.params.setDimensionOptions( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, null, asList( organisationUnits ) );
            return this;
        }
        
        public Builder addFilter( DimensionalObject filter )
        {
            this.params.addFilter( filter );
            return this;
        }
        
        public Builder addItem( QueryItem item )
        {
            this.params.items.add( item );
            return this;
        }
        
        public Builder removeItems()
        {
            this.params.items.clear();
            return this;
        }
        
        public Builder addItemFilter( QueryItem item )
        {
            this.params.itemFilters.add( item );
            return this;
        }
        
        public Builder addItemProgramIndicator( ProgramIndicator programIndicator )
        {
            this.params.itemProgramIndicators.add( programIndicator );
            return this;
        }
        
        public Builder removeItemProgramIndicators()
        {
            this.params.itemProgramIndicators.clear();
            return this;
        }

        public Builder withValue( DimensionalItemObject value )
        {
            this.params.value = value;
            return this;
        }
        
        public Builder withProgramIndicator( ProgramIndicator programIndicator )
        {
            this.params.programIndicator = programIndicator;
            return this;
        }
        
        public Builder withOrganisationUnitMode( OrganisationUnitSelectionMode organisationUnitMode )
        {
            this.params.organisationUnitMode = organisationUnitMode; 
            return this;
        }

        public Builder withSkipMeta( boolean skipMeta )
        {
            this.params.skipMeta = skipMeta;
            return this;
        }

        public Builder withSkipData( boolean skipData )
        {
            this.params.skipData = skipData;
            return this;
        }
        
        public Builder withCompletedOnly( boolean completedOnly )
        {
            this.params.completedOnly = completedOnly;
            return this;
        }

        public Builder withHierarchyMeta( boolean hierarchyMeta )
        {
            this.params.hierarchyMeta = hierarchyMeta;
            return this;
        }

        public Builder withCoordinatesOnly( boolean coordinatesOnly )
        {
            this.params.coordinatesOnly = coordinatesOnly;
            return this;
        }

        public Builder withDisplayProperty( DisplayProperty displayProperty )
        {
            this.params.displayProperty = displayProperty;
            return this;
        }

        public Builder withPage( Integer page )
        {
            this.params.page = page;
            return this;
        }

        public Builder withPageSize( Integer pageSize )
        {
            this.params.pageSize = pageSize;
            return this;
        }
        
        public Builder withPartitions( Partitions partitions )
        {
            this.params.partitions = partitions;
            return this;
        }
        
        public Builder addAscSortItem( String sortItem )
        {
            this.params.asc.add( sortItem );
            return this;
        }
        
        public Builder addDescSortItem( String sortItem )
        {
            this.params.desc.add( sortItem );
            return this;
        }

        public Builder withAggregationType( AggregationType aggregationType )
        {
            this.params.aggregationType = aggregationType;
            return this;
        }
        
        public Builder withSkipRounding( boolean skipRounding )
        {
            this.params.skipRounding = skipRounding;
            return this;
        }
        
        public Builder withShowHierarchy( boolean showHierarchy )
        {
            this.params.showHierarchy = showHierarchy;
            return this;
        }

        public Builder withSortOrder( SortOrder sortOrder )
        {
            this.params.sortOrder = sortOrder;
            return this;
        }

        public Builder withLimit( Integer limit )
        {
            this.params.limit = limit;
            return this;
        }

        public Builder withOutputType( EventOutputType outputType )
        {
            this.params.outputType = outputType;
            return this;
        }
        
        public Builder withEventStatus( EventStatus eventStatus )
        {
            this.params.eventStatus = eventStatus;
            return this;
        }

        public Builder withCollapseDataDimensions( boolean collapseDataDimensions )
        {
            this.params.collapseDataDimensions = collapseDataDimensions;
            return this;
        }

        public Builder withAggregateData( boolean aggregateData )
        {
            this.params.aggregateData = aggregateData;
            return this;
        }
        
        public Builder withClusterSize( Long clusterSize )
        {
            this.params.clusterSize = clusterSize;
            return this;
        }
        
        public Builder withBbox( String bbox )
        {
            this.params.bbox = bbox;
            return this;
        }
        
        public Builder withIncludeClusterPoints( boolean includeClusterPoints )
        {
            this.params.includeClusterPoints = includeClusterPoints;
            return this;
        }

        public EventQueryParams build()
        {
            return params;
        }
    }
}
