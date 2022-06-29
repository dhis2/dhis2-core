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
package org.hisp.dhis.analytics.event;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.OrgUnitFieldType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.FallbackCoordinateFieldType.OU_GEOMETRY;
import static org.hisp.dhis.common.FallbackCoordinateFieldType.PSI_GEOMETRY;
import static org.hisp.dhis.event.EventStatus.ACTIVE;
import static org.hisp.dhis.event.EventStatus.COMPLETED;
import static org.hisp.dhis.event.EventStatus.SCHEDULE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.analytics.QueryParamsBuilder;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Class representing query parameters for retrieving event data from the event
 * analytics service. Example instantiation:
 *
 * <pre>
 * {
 *     &#64;code
 *     EventQueryParams params = new EventQueryParams.Builder()
 *         .addItem( qiA )
 *         .addItemFilter( qiB )
 *         .withOrganisationUnits( ouA, ouB )
 *         .build();
 * }
 * </pre>
 *
 * @author Lars Helge Overland
 */
public class EventQueryParams
    extends DataQueryParams
{
    public static final String EVENT_COORDINATE_FIELD = "EVENT";

    public static final String ENROLLMENT_COORDINATE_FIELD = "ENROLLMENT";

    public static final ImmutableSet<FallbackCoordinateFieldType> FALLBACK_COORDINATE_FIELD_TYPES = ImmutableSet.of(
        OU_GEOMETRY, PSI_GEOMETRY );

    private static final Set<EventStatus> DEFAULT_EVENT_STATUS = new LinkedHashSet<>( asList( ACTIVE, COMPLETED ) );

    /**
     * The query items.
     */
    private List<QueryItem> items = new ArrayList<>();

    /**
     * The query item filters.
     */
    private List<QueryItem> itemFilters = new ArrayList<>();

    /**
     * TODO Change to List? TODO Add Javadoc
     */
    protected Set<String> headers = new LinkedHashSet<>();

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
    private List<QueryItem> asc = new ArrayList<>();

    /**
     * Columns to sort descending.
     */
    private List<QueryItem> desc = new ArrayList<>();

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
     * The paging flag.
     */
    private boolean paging;

    /**
     * The total pages flag.
     */
    private boolean totalPages;

    /**
     * The value sort order.
     */
    private SortOrder sortOrder;

    /**
     * The max limit of records to return.
     */
    private Integer limit;

    /**
     * Indicates the event output type which can be by event, enrollment type or
     * tracked entity instance.
     */
    private EventOutputType outputType;

    /**
     * Indicates the event status.
     */
    private Set<EventStatus> eventStatus = new LinkedHashSet<>();

    /**
     * Indicates whether the data dimension items should be collapsed into a
     * single dimension.
     */
    private boolean collapseDataDimensions;

    /**
     * Indicates whether request is intended to fetch events with coordinates
     * only.
     */
    private boolean coordinatesOnly;

    /**
     * Indicates whether request is intended to fetch events with geometry only.
     */
    private boolean geometryOnly;

    /**
     * Indicates whether request is intended to fetch events with alternative
     * coordinates.
     */
    private boolean coordinateOuFallback;

    /**
     * Indicates whether the query originates from an aggregate data query.
     */
    private boolean aggregateData;

    /**
     * Size of cluster in meter.
     */
    private Long clusterSize;

    /**
     * The coordinate field to use as basis for spatial event analytics.
     */
    private String coordinateField;

    /**
     * The fallback coordinate field to use as basis for spatial event
     * analytics, SQL COALESCE applied on coordinate fields.
     */
    private String fallbackCoordinateField;

    /**
     * Bounding box for events to include in clustering.
     */
    private String bbox;

    /**
     * Indicates whether to include underlying points for each cluster.
     */
    private boolean includeClusterPoints;

    /**
     * Indicates the program status
     */
    private Set<ProgramStatus> programStatus = new LinkedHashSet<>();

    /**
     * Indicates whether to include metadata details to response
     */
    protected boolean includeMetadataDetails;

    /**
     * Identifier scheme to use for data and attribute values. Applies to data
     * elements with option sets and legend sets, which are stored as codes and
     * UIDs respectively.
     */
    protected IdScheme dataIdScheme;

    /**
     * a map holding for each time field a range of dates
     */
    @Getter
    protected Map<AnalyticsDateFilter, DateRange> dateRangeByDateFilter = new HashMap<>();

    /**
     * flag to enable enhanced OR conditions
     */
    @Getter
    protected boolean enhancedCondition = false;

    @Getter
    protected RequestTypeAware.EndpointItem endpointItem;

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
        params.headers = new LinkedHashSet<>( this.headers );
        params.includeNumDen = this.includeNumDen;
        params.displayProperty = this.displayProperty;
        params.aggregationType = this.aggregationType;
        params.hierarchyMeta = this.hierarchyMeta;
        params.skipRounding = this.skipRounding;
        params.startDate = this.startDate;
        params.endDate = this.endDate;
        params.timeField = this.timeField;
        params.orgUnitField = this.orgUnitField;
        params.apiVersion = this.apiVersion;
        params.skipData = this.skipData;
        params.skipMeta = this.skipMeta;
        params.partitions = new Partitions( this.partitions );
        params.tableName = this.tableName;
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
        params.paging = this.paging;
        params.totalPages = this.totalPages;
        params.sortOrder = this.sortOrder;
        params.limit = this.limit;
        params.outputType = this.outputType;
        params.outputIdScheme = this.outputIdScheme;
        params.eventStatus = new LinkedHashSet<>( this.eventStatus );
        params.collapseDataDimensions = this.collapseDataDimensions;
        params.coordinatesOnly = this.coordinatesOnly;
        params.coordinateOuFallback = this.coordinateOuFallback;
        params.geometryOnly = this.geometryOnly;
        params.aggregateData = this.aggregateData;
        params.clusterSize = this.clusterSize;
        params.coordinateField = this.coordinateField;
        params.fallbackCoordinateField = this.fallbackCoordinateField;
        params.bbox = this.bbox;
        params.includeClusterPoints = this.includeClusterPoints;
        params.programStatus = new LinkedHashSet<>( this.programStatus );
        params.includeMetadataDetails = this.includeMetadataDetails;
        params.dataIdScheme = this.dataIdScheme;
        params.periodType = this.periodType;
        params.explainOrderId = this.explainOrderId;
        params.dateRangeByDateFilter = this.dateRangeByDateFilter;
        params.skipPartitioning = this.skipPartitioning;
        params.enhancedCondition = this.enhancedCondition;
        params.endpointItem = this.endpointItem;
        return params;
    }

    public static EventQueryParams fromDataQueryParams( DataQueryParams dataQueryParams )
    {
        EventQueryParams params = new EventQueryParams();

        dataQueryParams.copyTo( params );

        EventQueryParams.Builder builder = new EventQueryParams.Builder( params );

        for ( DimensionalItemObject object : dataQueryParams.getProgramDataElements() )
        {
            ProgramDataElementDimensionItem element = (ProgramDataElementDimensionItem) object;
            DataElement dataElement = element.getDataElement();
            QueryItem item = new QueryItem( dataElement,
                (dataElement.getLegendSets().isEmpty() ? null : dataElement.getLegendSets().get( 0 )),
                dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItem( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getProgramAttributes() )
        {
            ProgramTrackedEntityAttributeDimensionItem element = (ProgramTrackedEntityAttributeDimensionItem) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute,
                (attribute.getLegendSets().isEmpty() ? null : attribute.getLegendSets().get( 0 )),
                attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItem( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramDataElements() )
        {
            ProgramDataElementDimensionItem element = (ProgramDataElementDimensionItem) object;
            DataElement dataElement = element.getDataElement();
            QueryItem item = new QueryItem( dataElement,
                (dataElement.getLegendSets().isEmpty() ? null : dataElement.getLegendSets().get( 0 )),
                dataElement.getValueType(), dataElement.getAggregationType(), dataElement.getOptionSet() );
            item.setProgram( element.getProgram() );
            builder.addItemFilter( item );
        }

        for ( DimensionalItemObject object : dataQueryParams.getFilterProgramAttributes() )
        {
            ProgramTrackedEntityAttributeDimensionItem element = (ProgramTrackedEntityAttributeDimensionItem) object;
            TrackedEntityAttribute attribute = element.getAttribute();
            QueryItem item = new QueryItem( attribute,
                (attribute.getLegendSets().isEmpty() ? null : attribute.getLegendSets().get( 0 )),
                attribute.getValueType(), attribute.getAggregationType(), attribute.getOptionSet() );
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

    /**
     * Returns a unique key representing this query. The key is suitable for
     * caching.
     */
    @Override
    public String getKey()
    {
        QueryKey key = new QueryKey()
            .add( super.getKey() );

        items.forEach( e -> key.add( "item", "[" + e.getKey() + "]" ) );
        itemFilters.forEach( e -> key.add( "itemFilter", "[" + e.getKey() + "]" ) );
        headers.forEach( header -> key.add( "headers", "[" + header + "]" ) );
        itemProgramIndicators.forEach( e -> key.add( "itemProgramIndicator", e.getUid() ) );
        eventStatus.forEach( status -> key.add( "eventStatus", "[" + status + "]" ) );
        asc.forEach( e -> e.getItem().getUid() );
        desc.forEach( e -> e.getItem().getUid() );

        return key
            .addIgnoreNull( "value", value, () -> value.getUid() )
            .addIgnoreNull( "programIndicator", programIndicator, () -> programIndicator.getUid() )
            .addIgnoreNull( "organisationUnitMode", organisationUnitMode )
            .addIgnoreNull( "page", page )
            .addIgnoreNull( "pageSize", pageSize )
            .addIgnoreNull( "paging", paging )
            .addIgnoreNull( "sortOrder", sortOrder )
            .addIgnoreNull( "limit", limit )
            .addIgnoreNull( "outputType", outputType )
            .addIgnoreNull( "outputIdScheme", outputIdScheme )
            .addIgnoreNull( "collapseDataDimensions", collapseDataDimensions )
            .addIgnoreNull( "coordinatesOnly", coordinatesOnly )
            .addIgnoreNull( "coordinateOuFallback", coordinateOuFallback )
            .addIgnoreNull( "geometryOnly", geometryOnly )
            .addIgnoreNull( "aggregateData", aggregateData )
            .addIgnoreNull( "clusterSize", clusterSize )
            .addIgnoreNull( "coordinateField", coordinateField )
            .addIgnoreNull( "fallbackCoordinateField", fallbackCoordinateField )
            .addIgnoreNull( "bbox", bbox )
            .addIgnoreNull( "includeClusterPoints", includeClusterPoints )
            .addIgnoreNull( "programStatus", programStatus )
            .addIgnoreNull( "includeMetadataDetails", includeMetadataDetails )
            .addIgnoreNull( "dataIdScheme", dataIdScheme )
            .build();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Replaces periods with start and end dates, using the earliest start date
     * from the periods as start date and the latest end date from the periods
     * as end date. Remove the period dimension or filter.
     *
     * When heterogeneous date fields are specified, set a specific start/date
     * pair for each of them
     */
    private void replacePeriodsWithStartEndDates()
    {
        List<Period> periods = asTypedList( getDimensionOrFilterItems( PERIOD_DIM_ID ) );

        for ( Period period : periods )
        {
            if ( Objects.isNull( period.getDateField() ) )
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
            else
            {
                Optional<AnalyticsDateFilter> dateFilter = AnalyticsDateFilter.of( period.getDateField() );
                if ( dateFilter.isPresent() )
                {
                    updateStartForDateFilterIfNecessary( dateFilter.get(), period.getStartDate() );
                    updateEndForDateFilterIfNecessary( dateFilter.get(), period.getEndDate() );
                }
            }
        }

        removeDimensionOrFilter( PERIOD_DIM_ID );
    }

    private void updateStartForDateFilterIfNecessary( AnalyticsDateFilter dateFilter, Date start )
    {
        if ( dateRangeByDateFilter.get( dateFilter ) != null )
        {
            Date startDateInMap = dateRangeByDateFilter.get( dateFilter ).getStartDate();
            if ( startDateInMap == null || (start != null && start.before( startDateInMap )) )
            {
                dateRangeByDateFilter.get( dateFilter ).setStartDate( start );
            }
        }
        else
        {
            dateRangeByDateFilter.put( dateFilter, new DateRange( start, null ) );
        }
    }

    private void updateEndForDateFilterIfNecessary( AnalyticsDateFilter dateFilter, Date end )
    {
        if ( dateRangeByDateFilter.get( dateFilter ) != null )
        {
            Date endDateInMap = dateRangeByDateFilter.get( dateFilter ).getEndDate();
            if ( endDateInMap == null || (end != null && end.after( endDateInMap )) )
            {
                dateRangeByDateFilter.get( dateFilter ).setEndDate( end );
            }
        }
        else
        {
            dateRangeByDateFilter.put( dateFilter, new DateRange( null, end ) );
        }
    }

    public boolean containsScheduledDatePeriod()
    {
        return dateRangeByDateFilter != null && dateRangeByDateFilter.containsKey( AnalyticsDateFilter.SCHEDULED_DATE );
    }

    /**
     * Indicates whether we should use start/end dates in SQL query instead of
     * periods.
     */
    public boolean useStartEndDates()
    {
        return hasStartEndDate() || !getDateRangeByDateFilter().isEmpty();
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
    public Set<Legend> getItemLegends()
    {
        return getItemsAndItemFilters().stream()
            .filter( QueryItem::hasLegendSet )
            .map( i -> i.getLegendSet().getLegends() )
            .flatMap( i -> i.stream() )
            .collect( Collectors.toSet() );
    }

    /**
     * Get options for option sets part of items and item filters.
     */
    public Set<Option> getItemOptions()
    {
        return getItemsAndItemFilters().stream()
            .filter( QueryItem::hasOptionSet )
            .map( q -> q.getOptionSet().getOptions() )
            .flatMap( q -> q.stream() )
            .collect( Collectors.toSet() );
    }

    /**
     * Indicates whether the given time field is valid, i.e. whether it is
     * either a fixed time field or matches the identifier of an attribute or
     * data element of date value type part of the query program.
     */
    public boolean timeFieldIsValid()
    {
        if ( timeField == null )
        {
            return true;
        }

        if ( TimeField.fieldIsValid( timeField ) )
        {
            return true;
        }

        if ( program.getTrackedEntityAttributes().stream()
            .anyMatch( at -> at.getValueType().isDate() && timeField.equals( at.getUid() ) ) )
        {
            return true;
        }

        if ( program.getDataElements().stream()
            .anyMatch( de -> de.getValueType().isDate() && timeField.equals( de.getUid() ) ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Indicates whether the given organisation unit field is valid, i.e.
     * whether it matches the identifier of an attribute or data element of
     * organisation unit value type part of the query program.
     */
    public boolean orgUnitFieldIsValid()
    {
        if ( !(orgUnitField.getType() == ATTRIBUTE) )
        {
            return true;
        }

        if ( program != null )
        {
            return validateProgramHasOrgUnitField( program );
        }

        if ( !itemProgramIndicators.isEmpty() )
        {
            // Fail validation if at least one program indicator is invalid

            return !itemProgramIndicators.stream().anyMatch( pi -> !validateProgramHasOrgUnitField( pi.getProgram() ) );
        }

        return false;
    }

    /**
     * Indicates whether the given fallbackCoordinateField is valid, i.e.
     * whether it matches to the query geometry.
     */
    public boolean fallbackCoordinateFieldIsValid()
    {
        return EventQueryParams.FALLBACK_COORDINATE_FIELD_TYPES.stream()
            .anyMatch( t -> t.getValue().equals( fallbackCoordinateField ) );
    }

    private boolean validateProgramHasOrgUnitField( Program program )
    {
        String orgUnitColumn = orgUnitField.getField();

        if ( program.getTrackedEntityAttributes().stream()
            .anyMatch( at -> at.getValueType().isOrganisationUnit() && orgUnitColumn.equals( at.getUid() ) ) )
        {
            return true;
        }

        if ( program.getDataElements().stream()
            .anyMatch( at -> at.getValueType().isOrganisationUnit() && orgUnitColumn.equals( at.getUid() ) ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Gets program status
     */
    public Set<ProgramStatus> getProgramStatus()
    {
        return programStatus;
    }

    /**
     * Removes items and item filters of type program indicators.
     *
     * TODO add support for program indicators in aggregate event analytics and
     * remove this method.
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
    public AnalyticsAggregationType getAggregationTypeFallback()
    {
        if ( hasAggregationType() )
        {
            return aggregationType;
        }
        else if ( hasValueDimension() && value.getAggregationType() != null )
        {
            return AnalyticsAggregationType.fromAggregationType( value.getAggregationType() );
        }

        return AnalyticsAggregationType.AVERAGE;
    }

    /**
     * Indicates whether this object is of the given aggregation type. Based on
     * {@link getAggregationTypeFallback}.
     */
    @Override
    public boolean isAggregationType( AggregationType type )
    {
        AnalyticsAggregationType typeFallback = getAggregationTypeFallback();

        return typeFallback != null && type.equals( typeFallback.getAggregationType() );
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
     * Returns true if an aggregation type is defined, and this is type is
     * {@link AggregationType} LAST
     */
    public boolean isLastPeriodAggregationType()
    {
        return getAggregationType() != null && getAggregationType().isLastPeriodAggregationType();
    }

    /**
     * Returns true if an aggregation type is defined, and this is type is
     * {@link AggregationType} FIRST
     */
    public boolean isFirstPeriodAggregationType()
    {
        return getAggregationType() != null && getAggregationType().isFirstPeriodAggregationType();
    }

    public boolean isFirstOrLastPeriodAggregationType()
    {
        return isFirstPeriodAggregationType() || isLastPeriodAggregationType();
    }

    /**
     * Returns true if a program indicator exists with non-default analytics
     * period boundaries.
     */
    public boolean hasNonDefaultBoundaries()
    {
        return hasProgramIndicatorDimension() && getProgramIndicator().hasNonDefaultBoundaries();
    }

    public boolean hasAnalyticsVariables()
    {
        return hasProgramIndicatorDimension() && getProgramIndicator().hasAnalyticsVariables();
    }

    public boolean useIndividualQuery()
    {
        return this.hasAnalyticsVariables() || this.hasNonDefaultBoundaries();
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
        return paging && (page != null || pageSize != null);
    }

    public boolean isTotalPages()
    {
        return totalPages;
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
        return isNotEmpty( eventStatus );
    }

    public boolean hasValueDimension()
    {
        return value != null;
    }

    @Override
    public boolean hasProgramIndicatorDimension()
    {
        return programIndicator != null;
    }

    public boolean hasEventProgramIndicatorDimension()
    {
        return programIndicator != null &&
            AnalyticsType.EVENT.equals( programIndicator.getAnalyticsType() );
    }

    public boolean hasEnrollmentProgramIndicatorDimension()
    {
        return programIndicator != null &&
            AnalyticsType.ENROLLMENT.equals( programIndicator.getAnalyticsType() );
    }

    /**
     * Indicates whether the EventQueryParams has exactly one Period dimension.
     *
     * @return true when exactly one Period dimension exists.
     */
    public boolean hasSinglePeriod()
    {
        return getPeriods().size() == 1;
    }

    /**
     * Indicates whether the EventQueryParams has Period filters.
     *
     * @return true when any Period filters exists.
     */
    public boolean hasFilterPeriods()
    {
        return getFilterPeriods().size() > 0;
    }

    public boolean hasHeaders()
    {
        return isNotEmpty( getHeaders() );
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

    public boolean hasProgramStatus()
    {
        return isNotEmpty( programStatus );
    }

    public boolean hasBbox()
    {
        return bbox != null && !bbox.isEmpty();
    }

    public boolean hasDataIdScheme()
    {
        return dataIdScheme != null;
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
        return MoreObjects.toStringHelper( this )
            .add( "Program", program )
            .add( "Stage", programStage )
            .add( "Start date", startDate )
            .add( "End date", endDate )
            .add( "Items", items )
            .add( "Item filters", itemFilters )
            .add( "Value", value )
            .add( "Item program indicators", itemProgramIndicators )
            .add( "Program indicator", programIndicator )
            .add( "Aggregation type", aggregationType )
            .add( "Dimensions", dimensions )
            .add( "Filters", filters )
            .toString();
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

    public Set<String> getHeaders()
    {
        return headers;
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

    public List<QueryItem> getAsc()
    {
        return asc;
    }

    @Override
    public List<DimensionalObject> getDimensions()
    {
        return dimensions;
    }

    public List<QueryItem> getDesc()
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

    public boolean getPaging()
    {
        return paging;
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

    @Override
    public IdScheme getOutputIdScheme()
    {
        return outputIdScheme;
    }

    public Set<EventStatus> getEventStatus()
    {
        if ( isNotEmpty( eventStatus ) )
        {
            return eventStatus;
        }

        if ( TimeField.fieldIsValid( timeField ) )
        {
            final Optional<TimeField> time = TimeField.of( timeField );

            if ( time.isPresent() )
            {
                switch ( time.get() )
                {
                case SCHEDULED_DATE:
                    return Set.of( SCHEDULE );
                case LAST_UPDATED:
                    final Set<EventStatus> statuses = new LinkedHashSet<>( DEFAULT_EVENT_STATUS );
                    statuses.add( SCHEDULE );
                    return statuses;
                default:
                    return DEFAULT_EVENT_STATUS;
                }
            }
        }
        else if ( containsScheduledDatePeriod() )
        {
            return Set.of( SCHEDULE );
        }

        return DEFAULT_EVENT_STATUS;
    }

    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public boolean isCoordinatesOnly()
    {
        return coordinatesOnly;
    }

    public boolean isCoordinateOuFallback()
    {
        return coordinateOuFallback;
    }

    public boolean isGeometryOnly()
    {
        return geometryOnly;
    }

    public boolean isAggregateData()
    {
        return aggregateData;
    }

    public Long getClusterSize()
    {
        return clusterSize;
    }

    public String getCoordinateField()
    {
        return coordinateField;
    }

    public String getFallbackCoordinateField()
    {
        return fallbackCoordinateField;
    }

    public String getBbox()
    {
        return bbox;
    }

    public boolean isIncludeClusterPoints()
    {
        return includeClusterPoints;
    }

    @Override
    public boolean isIncludeMetadataDetails()
    {
        return includeMetadataDetails;
    }

    public IdScheme getDataIdScheme()
    {
        return dataIdScheme;
    }

    // -------------------------------------------------------------------------
    // Builder of immutable instances
    // -------------------------------------------------------------------------

    /**
     * Builder for {@link DataQueryParams} instances.
     */
    public static class Builder
        implements QueryParamsBuilder
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

        public Builder withPeriods( List<? extends DimensionalItemObject> periods, String periodType )
        {
            this.params.setDimensionOptions( PERIOD_DIM_ID, DimensionType.PERIOD, periodType.toLowerCase(),
                asList( periods ) );
            this.params.periodType = periodType;
            return this;
        }

        @Override
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

        @Override
        public Builder removeDimensionOrFilter( String dimension )
        {
            this.params.dimensions.remove( new BaseDimensionalObject( dimension ) );
            this.params.filters.remove( new BaseDimensionalObject( dimension ) );
            return this;
        }

        public Builder withOrganisationUnits( List<? extends DimensionalItemObject> organisationUnits )
        {
            this.params.setDimensionOptions( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, null,
                asList( organisationUnits ) );
            return this;
        }

        @Override
        public Builder addFilter( DimensionalObject filter )
        {
            this.params.addFilter( filter );
            return this;
        }

        public Builder withHeaders( Set<String> headers )
        {
            if ( isNotEmpty( headers ) )
            {
                this.params.headers.addAll( headers );
            }

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

        public Builder withCoordinateOuFallback( boolean coordinateOuFallback )
        {
            this.params.coordinateOuFallback = coordinateOuFallback;
            return this;
        }

        public Builder withGeometryOnly( boolean geometryOnly )
        {
            this.params.geometryOnly = geometryOnly;
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

        public Builder withPaging( boolean paging )
        {
            this.params.paging = paging;
            return this;
        }

        public Builder withTotalPages( boolean totalPages )
        {
            this.params.totalPages = totalPages;
            return this;
        }

        public Builder withPartitions( Partitions partitions )
        {
            this.params.partitions = partitions;
            return this;
        }

        public Builder withTableName( String tableName )
        {
            this.params.tableName = tableName;
            return this;
        }

        public Builder addAscSortItem( QueryItem sortItem )
        {
            this.params.asc.add( sortItem );
            return this;
        }

        public Builder addDescSortItem( QueryItem sortItem )
        {
            this.params.desc.add( sortItem );
            return this;
        }

        public Builder withAggregationType( AnalyticsAggregationType aggregationType )
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

        public Builder withEventStatuses( Set<EventStatus> eventStatuses )
        {
            if ( isNotEmpty( eventStatuses ) )
            {
                this.params.eventStatus.addAll( eventStatuses );
            }

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

        public Builder withTimeField( String timeField )
        {
            this.params.timeField = timeField;
            return this;
        }

        public Builder withOrgUnitField( OrgUnitField orgUnitField )
        {
            this.params.orgUnitField = orgUnitField;
            return this;
        }

        public Builder withClusterSize( Long clusterSize )
        {
            this.params.clusterSize = clusterSize;
            return this;
        }

        public Builder withCoordinateField( String coordinateField )
        {
            this.params.coordinateField = coordinateField;
            return this;
        }

        public Builder withFallbackCoordinateField( String fallbackCoordinateField )
        {
            this.params.fallbackCoordinateField = fallbackCoordinateField;
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

        public Builder withProgramStatuses( Set<ProgramStatus> programStatuses )
        {
            if ( isNotEmpty( programStatuses ) )
            {
                this.params.programStatus.addAll( programStatuses );
            }

            return this;
        }

        public Builder withStartEndDatesForPeriods()
        {
            this.params.replacePeriodsWithStartEndDates();
            return this;
        }

        public Builder withApiVersion( DhisApiVersion apiVersion )
        {
            this.params.apiVersion = apiVersion;
            return this;
        }

        public Builder withIncludeMetadataDetails( boolean includeMetadataDetails )
        {
            this.params.includeMetadataDetails = includeMetadataDetails;
            return this;
        }

        public Builder withDataIdScheme( IdScheme dataIdScheme )
        {
            this.params.dataIdScheme = dataIdScheme;
            return this;
        }

        public Builder withOutputIdScheme( IdScheme outputIdScheme )
        {
            this.params.outputIdScheme = outputIdScheme;
            return this;
        }

        public Builder withAnalyzeOrderId()
        {
            this.params.explainOrderId = UUID.randomUUID().toString();
            return this;
        }

        public void withSkipPartitioning( boolean skipPartitioning )
        {
            this.params.skipPartitioning = skipPartitioning;
        }

        public Builder withEnhancedConditions( boolean enhancedConditions )
        {
            this.params.enhancedCondition = enhancedConditions;
            return this;
        }

        public EventQueryParams build()
        {
            return params;
        }

        public Builder withEndpointItem( RequestTypeAware.EndpointItem endpointItem )
        {
            this.params.endpointItem = endpointItem;
            return this;
        }
    }
}
