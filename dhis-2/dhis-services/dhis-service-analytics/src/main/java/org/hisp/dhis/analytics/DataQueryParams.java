package org.hisp.dhis.analytics;

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

import static org.hisp.dhis.analytics.AggregationType.AVERAGE_INT_DISAGGREGATION;
import static org.hisp.dhis.analytics.AggregationType.AVERAGE_SUM_INT_DISAGGREGATION;
import static org.hisp.dhis.common.DimensionType.CATEGORYOPTION_GROUPSET;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATIONUNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATIONUNIT_GROUPSET;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.NameableObjectUtils.asList;
import static org.hisp.dhis.common.NameableObjectUtils.getList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.util.MathUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataQueryParams
{
    public static final String VALUE_ID = "value";    
    public static final String LEVEL_PREFIX = "uidlevel";
    public static final String KEY_DE_GROUP = "DE_GROUP-";
    
    public static final String DISPLAY_NAME_DATA_X = "Data";
    public static final String DISPLAY_NAME_CATEGORYOPTIONCOMBO = "Category option combo";
    public static final String DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO = "Attribute option combo";
    public static final String DISPLAY_NAME_PERIOD = "Period";
    public static final String DISPLAY_NAME_ORGUNIT = "Organisation unit";
    public static final String DISPLAY_NAME_LONGITUDE = "Longitude";
    public static final String DISPLAY_NAME_LATITUDE = "Latitude";

    public static final int DX_INDEX = 0;
    public static final int CO_INDEX = 1;

    public static final Set<Class<? extends IdentifiableObject>> DYNAMIC_DIM_CLASSES = ImmutableSet.<Class<? extends IdentifiableObject>>builder().
        add( OrganisationUnitGroupSet.class ).add( DataElementGroupSet.class ).add( CategoryOptionGroupSet.class ).add( DataElementCategory.class ).build();
    
    private static final List<String> DIMENSION_PERMUTATION_IGNORE_DIMS = Lists.newArrayList( 
        DATA_X_DIM_ID, CATEGORYOPTIONCOMBO_DIM_ID );    
    public static final List<DimensionType> COMPLETENESS_DIMENSION_TYPES = Lists.newArrayList( 
        DATA_X, PERIOD, ORGANISATIONUNIT, ORGANISATIONUNIT_GROUPSET, CATEGORYOPTION_GROUPSET );
    private static final List<DimensionType> COMPLETENESS_TARGET_DIMENSION_TYPES = Lists.newArrayList( 
        DATA_X, ORGANISATIONUNIT, ORGANISATIONUNIT_GROUPSET );
    
    private static final DimensionItem[] DIM_OPT_ARR = new DimensionItem[0];
    private static final DimensionItem[][] DIM_OPT_2D_ARR = new DimensionItem[0][];

    /**
     * The dimensions.
     */
    protected List<DimensionalObject> dimensions = new ArrayList<>();
    
    /**
     * The filters.
     */
    protected List<DimensionalObject> filters = new ArrayList<>();

    /**
     * The aggregation type.
     */
    protected AggregationType aggregationType;
    
    /**
     * The measure criteria, which is measure filters and corresponding values.
     */
    protected Map<MeasureFilter, Double> measureCriteria = new HashMap<>();
    
    /**
     * Indicates if the meta data part of the query response should be omitted.
     */
    protected boolean skipMeta;
    
    /**
     * Indicates if the data part of the query response should be omitted.
     */
    protected boolean skipData;

    /**
     * Indicates that full precision should be provided for values.
     */
    protected boolean skipRounding;

    /**
     * Indicates whether to include completed events only.
     */
    protected boolean completedOnly;
    
    /**
     * Indicates i) if the names of all ancestors of the organisation units part
     * of the query should be included in the "names" key and ii) if the hierarchy 
     * path of all organisation units part of the query should be included as a
     * "ouHierarchy" key in the meta-data part of the response.
     */
    protected boolean hierarchyMeta;
    
    /**
     * Indicates whether the maximum number of records to include the response
     * should be ignored.
     */
    protected boolean ignoreLimit;
    
    /**
     * Indicates whether rows with no values should be hidden in the response.
     * Applies to responses with table layout only. 
     */
    protected boolean hideEmptyRows;
    
    /**
     * Indicates whether the org unit hierarchy path should be displayed with the
     * org unit names on rows.
     */
    protected boolean showHierarchy;
    
    /**
     * Indicates which property to display for meta-data.
     */
    protected DisplayProperty displayProperty;
    
    /**
     * The property to use as identifier in the query response.
     */
    protected IdentifiableProperty outputIdScheme;
    
    /**
     * The required approval level identifier for data to be included in query response.
     */
    protected String approvalLevel;
    
    // -------------------------------------------------------------------------
    // Event properties
    // -------------------------------------------------------------------------
    
    /**
     * The program for events.
     */
    protected Program program;
    
    /**
     * The program stage for events.
     */
    protected ProgramStage programStage;
    
    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------
    
    /**
     * The partitions containing data relevant to this query.
     */
    protected transient Partitions partitions;

    /**
     * The data type for this query.
     */
    protected transient DataType dataType;
        
    /**
     * The aggregation period type for this query.
     */
    protected transient String periodType;
    
    /**
     * The period type of the data values to query.
     */
    protected transient PeriodType dataPeriodType;
    
    /**
     * Indicates whether to skip partitioning during query planning.
     */
    protected transient boolean skipPartitioning;
    
    /**
     * Mapping of organisation unit sub-hierarchy roots and lowest available data approval levels.
     */
    protected transient Map<OrganisationUnit, Integer> dataApprovalLevels = new HashMap<>();
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    
    public DataQueryParams()
    {
    }

    public DataQueryParams instance()
    {
        return copyTo( new DataQueryParams() );
    }
    
    public <T extends DataQueryParams> T copyTo( T params )
    {        
        params.dimensions = DimensionalObjectUtils.getCopies( this.dimensions );
        params.filters = DimensionalObjectUtils.getCopies( this.filters );
        params.aggregationType = this.aggregationType;
        params.measureCriteria = this.measureCriteria;
        params.skipMeta = this.skipMeta;
        params.skipData = this.skipData;
        params.skipRounding = this.skipRounding;
        params.completedOnly = this.completedOnly;
        params.hierarchyMeta = this.hierarchyMeta;
        params.ignoreLimit = this.ignoreLimit;
        params.hideEmptyRows = this.hideEmptyRows;
        params.showHierarchy = this.showHierarchy;
        params.displayProperty = this.displayProperty;
        params.outputIdScheme = this.outputIdScheme;
        params.approvalLevel = this.approvalLevel;
        params.program = this.program;
        params.programStage = this.programStage;
        
        params.partitions = new Partitions( this.partitions );
        params.dataType = this.dataType;
        params.periodType = this.periodType;
        params.dataPeriodType = this.dataPeriodType;
        params.skipPartitioning = this.skipPartitioning;
        params.dataApprovalLevels = new HashMap<>( this.dataApprovalLevels );
        
        return params;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    
    /**
     * Ensures conformity for this query. The category option combo dimension
     * can only be present if the data element dimension exists and the indicator 
     * and data set dimensions do not exist.
     */
    public DataQueryParams conform()
    {
        if ( !( !getDataElements().isEmpty() && getDataElementOperands().isEmpty() && getIndicators().isEmpty() && getDataSets().isEmpty() ) )
        {
            removeDimension( CATEGORYOPTIONCOMBO_DIM_ID );
        }
        
        //TODO program data elements / attributes
        
        return this;
    }
    
    /**
     * Returns a key representing a group of queries which should be run in 
     * sequence. Currently queries with different aggregation type are run in
     * sequence. It is not allowed for the implementation to differentiate on
     * dimensional objects. TODO test including tableName (partition)
     */
    public String getSequentialQueryGroupKey()
    {
        return aggregationType != null ? aggregationType.toString() : null;
    }
    
    /**
     * Indicates whether the filters of this query spans more than one partition.
     * If true it means that a period filter exists and that the periods span
     * multiple years.
     */
    public boolean spansMultiplePartitions()
    {
        return partitions != null && partitions.isMultiple();
    }
        
    /**
     * Creates a mapping between filter dimension identifiers and filter dimensions. 
     * Filters are guaranteed not to be null.
     */
    public ListMap<String, DimensionalObject> getDimensionFilterMap()
    {
        ListMap<String, DimensionalObject> map = new ListMap<>();
        
        for ( DimensionalObject filter : filters )
        {
            if ( filter != null )
            {
                map.putValue( filter.getDimension(), filter );
            }
        }
        
        return map;
    }
    
    /**
     * Creates a list of dimension indexes which are relevant to completeness queries.
     */
    public List<Integer> getCompletenessDimensionIndexes()
    {
        List<Integer> indexes = new ArrayList<>();
        
        for ( int i = 0; i < dimensions.size(); i++ )
        {
            if ( COMPLETENESS_TARGET_DIMENSION_TYPES.contains( dimensions.get( i ).getDimensionType() ) )
            {
                indexes.add( i );
            }
        }
        
        return indexes;
    }

    /**
     * Creates a list of filter indexes which are relevant to completeness queries.
     */
    public List<Integer> getCompletenessFilterIndexes()
    {
        List<Integer> indexes = new ArrayList<>();
        
        for ( int i = 0; i < filters.size(); i++ )
        {
            if ( COMPLETENESS_TARGET_DIMENSION_TYPES.contains( filters.get( i ).getDimensionType() ) )
            {
                indexes.add( i );
            }
        }
        
        return indexes;
    }
    
    /**
     * Removes all dimensions which are not of the given type from dimensions
     * and filters.
     */
    public DataQueryParams pruneToDimensionType( DimensionType type )
    {
        Iterator<DimensionalObject> dimensionIter = dimensions.iterator();
        
        while ( dimensionIter.hasNext() )
        {
            if ( !dimensionIter.next().getDimensionType().equals( type ) )
            {
                dimensionIter.remove();
            }
        }
        
        Iterator<DimensionalObject> filterIter = filters.iterator();
        
        while ( filterIter.hasNext() )
        {
            if ( !filterIter.next().getDimensionType().equals( type ) )
            {
                filterIter.remove();
            }
        }
        
        return this;
    }

    /**
     * Removes the dimension with the given identifier.
     */
    public DataQueryParams removeDimension( String dimension )
    {
        this.dimensions.remove( new BaseDimensionalObject( dimension ) );
        
        return this;
    }

    /**
     * Removes the dimensions with the given identifiers.
     */
    public DataQueryParams removeDimensions( String... dimension )
    {
        return removeDimensions( Sets.newHashSet( dimension ) );
    }

    /**
     * Removes the dimensions with the given identifiers.
     */
    public DataQueryParams removeDimensions( Collection<String> dimension )
    {
        if ( dimension != null )
        {
            for ( String dim : dimension )
            {
                removeDimension( dim );
            }
        }            
        
        return this;
    }

    /**
     * Removes the dimension or filter with the given identifier.
     */
    public DataQueryParams removeDimensionOrFilter( String dimension )
    {
        removeDimension( dimension );
        removeFilter( dimension );
        
        return this;
    }
    
    /**
     * Removes dimensions of the given type.
     */
    public void removeDimensions( DimensionType type )
    {
        Iterator<DimensionalObject> iterator = dimensions.iterator();
        
        while ( iterator.hasNext() )
        {
            DimensionalObject dimension = iterator.next();
            
            if ( DimensionType.CATEGORY.equals( dimension.getDimensionType() ) )
            {
                iterator.remove();
            }
        }
    }
    
    /**
     * Removes the filter with the given identifier.
     */
    public DataQueryParams removeFilter( String filter )
    {
        this.filters.remove( new BaseDimensionalObject( filter ) );
        
        return this;
    }
    
    /**
     * Returns the index of the category option combo dimension in the dimension map.
     */
    public int getCategoryOptionComboDimensionIndex()
    {
        return getDimensionIdentifiersAsList().indexOf( CATEGORYOPTIONCOMBO_DIM_ID );
    }
    
    /**
     * Returns the index of the period dimension in the dimension map.
     */
    public int getPeriodDimensionIndex()
    {
        return getDimensionIdentifiersAsList().indexOf( PERIOD_DIM_ID );
    }
    
    /**
     * Returns the dimensions which are part of dimensions and filters. If any
     * such dimensions exist this object is in an illegal state.
     */
    public Collection<DimensionalObject> getDimensionsAsFilters()
    {
        return CollectionUtils.intersection( dimensions, filters );
    }
    
    /**
     * Indicates whether periods are present as a dimension or as a filter. If
     * not this object is in an illegal state.
     */
    public boolean hasPeriods()
    {
        List<NameableObject> dimOpts = getDimensionOptions( PERIOD_DIM_ID );
        List<NameableObject> filterOpts = getFilterOptions( PERIOD_DIM_ID );
        
        return !dimOpts.isEmpty() || !filterOpts.isEmpty();
    }
    
    /**
     * Indicates whether organisation units are present as dimension or filter.
     */
    public boolean hasOrganisationUnits()
    {
        List<NameableObject> dimOpts = getDimensionOptions( ORGUNIT_DIM_ID );
        List<NameableObject> filterOpts = getFilterOptions( ORGUNIT_DIM_ID );
        
        return !dimOpts.isEmpty() || !filterOpts.isEmpty();
    }
    
    /**
     * Returns the period type of the first period specified as filter, or
     * null if there is no period filter.
     */
    public PeriodType getFilterPeriodType()
    {
        List<NameableObject> filterPeriods = getFilterPeriods();
        
        if ( !filterPeriods.isEmpty() )
        {
            return ( (Period) filterPeriods.get( 0 ) ).getPeriodType();
        }
        
        return null;
    }
    
    /**
     * Returns the first period specified as filter, or null if there is no
     * period filter.
     */
    public Period getFilterPeriod()
    {
        List<NameableObject> filterPeriods = getFilterPeriods();
        
        if ( !filterPeriods.isEmpty() )
        {
            return (Period) filterPeriods.get( 0 );
        }
        
        return null;
    }
        
    /**
     * Returns a list of dimensions which occur more than once, not including
     * the first duplicate.
     */
    public List<DimensionalObject> getDuplicateDimensions()
    {
        Set<DimensionalObject> dims = new HashSet<>();
        List<DimensionalObject> duplicates = new ArrayList<>();
        
        for ( DimensionalObject dim : dimensions )
        {
            if ( !dims.add( dim ) )
            {
                duplicates.add( dim );
            }
        }
        
        return duplicates;
    }
    
    /**
     * Returns a mapping between identifier and period type for all data sets
     * in this query.
     */
    public Map<String, PeriodType> getDataSetPeriodTypeMap()
    {
        Map<String, PeriodType> map = new HashMap<>();
        
        for ( NameableObject dataSet : getDataSets() )
        {
            DataSet ds = (DataSet) dataSet;
            
            map.put( ds.getUid(), ds.getPeriodType() );
        }
        
        return map;
    }
    
    /**
     * Returns the index of the category option combo dimension. Returns null
     * if this dimension is not present.
     */
    public Integer getCocIndex()
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID ) );
        
        return index == -1 ? null : index;
    }

    /**
     * Indicates whether this object is of the given data type.
     */
    public boolean isDataType( DataType dataType )
    {
        return this.dataType != null && this.dataType.equals( dataType );
    }
    
    /**
     * Indicates whether this object is of the given aggregation type.
     */
    public boolean isAggregationType( AggregationType aggregationType )
    {
        return this.aggregationType != null && this.aggregationType.equals( aggregationType );
    }
    
    /**
     * Indicates whether an aggregation type is specified.
     */
    public boolean hasAggregationType()
    {
        return this.aggregationType != null;
    }

    /**
     * Creates a mapping between the data periods, based on the data period type
     * for this query, and the aggregation periods for this query.
     */
    public ListMap<NameableObject, NameableObject> getDataPeriodAggregationPeriodMap()
    {
        ListMap<NameableObject, NameableObject> map = new ListMap<>();

        if ( dataPeriodType != null )
        {
            for ( NameableObject aggregatePeriod : getDimensionOrFilterItems( PERIOD_DIM_ID ) )
            {
                Period dataPeriod = dataPeriodType.createPeriod( ((Period) aggregatePeriod).getStartDate() );
                
                map.putValue( dataPeriod, aggregatePeriod );
            }
        }
        
        return map;
    }
    
    /**
     * Indicates whether the aggregation type is of type disaggregation.
     */
    public boolean isDisaggregation()
    {
        return isAggregationType( AVERAGE_SUM_INT_DISAGGREGATION ) || isAggregationType( AVERAGE_INT_DISAGGREGATION );
    }
    
    /**
     * Replaces the periods of this query with the corresponding data periods.
     * Sets the period type to the data period type. This method is relevant only 
     * when then the data period type has lower frequency than the aggregation 
     * period type. This is valid because disaggregation is allowed for data
     * with average aggregation operator.
     */
    public void replaceAggregationPeriodsWithDataPeriods( ListMap<NameableObject, NameableObject> dataPeriodAggregationPeriodMap )
    {
        if ( isDisaggregation() && dataPeriodType != null )
        {
            this.periodType = this.dataPeriodType.getName();
            
            if ( !getPeriods().isEmpty() ) // Period is dimension
            {
                setDimensionOptions( PERIOD_DIM_ID, DimensionType.PERIOD, dataPeriodType.getName().toLowerCase(), new ArrayList<>( dataPeriodAggregationPeriodMap.keySet() ) );
            }
            else // Period is filter
            {
                setFilterOptions( PERIOD_DIM_ID, DimensionType.PERIOD, dataPeriodType.getName().toLowerCase(), new ArrayList<>( dataPeriodAggregationPeriodMap.keySet() ) );
            }
        }
    }
    
    /**
     * Generates all permutations of the dimension options for this query.
     * Ignores the data element, category option combo and indicator dimensions.
     */
    public List<List<DimensionItem>> getDimensionItemPermutations()
    {
        List<DimensionItem[]> dimensionOptions = new ArrayList<>();
        
        for ( DimensionalObject dimension : dimensions )
        {
            if ( !DIMENSION_PERMUTATION_IGNORE_DIMS.contains( dimension.getDimension() ) )
            {
                List<DimensionItem> options = new ArrayList<>();
                
                for ( NameableObject option : dimension.getItems() )
                {
                    options.add( new DimensionItem( dimension.getDimension(), option ) );
                }
                
                dimensionOptions.add( options.toArray( DIM_OPT_ARR ) );
            }
        }
        
        CombinationGenerator<DimensionItem> generator = new CombinationGenerator<>( dimensionOptions.toArray( DIM_OPT_2D_ARR ) );
        
        List<List<DimensionItem>> permutations = generator.getCombinations();
        
        return permutations;
    }

    /**
     * Retrieves the options for all data-related (dx) dimensions and filters.
     * Returns an empty list if not present.
     */
    public List<NameableObject> getDataDimensionAndFilterOptions()
    {
        List<NameableObject> options = new ArrayList<>();
        options.addAll( getDimensionOptions( DATA_X_DIM_ID ) );
        options.addAll( getFilterOptions( DATA_X_DIM_ID ) );
        return options;
    }
    
    /**
     * Retrieves the options for the given dimension identifier. Returns an empty
     * list if the dimension is not present.
     */
    public List<NameableObject> getDimensionOptions( String dimension )
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( dimension ) );
        
        return index != -1 ? dimensions.get( index ).getItems() : new ArrayList<NameableObject>();
    }
    
    /**
     * Retrieves the dimension with the given dimension identifier. Returns null 
     * if the dimension is not present.
     */
    public DimensionalObject getDimension( String dimension )
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( dimension ) );
        
        return index != -1 ? dimensions.get( index ) : null;
    }

    /**
     * Retrieves the dimension or filter with the given dimension identifier. 
     * Returns null if the dimension or filter is not present.
     */
    public DimensionalObject getDimensionOrFilter( String dimension )
    {
        DimensionalObject dim = getDimension( dimension );
        
        return dim != null ? dim : getFilter( dimension );
    }
    
    /**
     * Sets the options for the given dimension.
     */
    public DataQueryParams setDimensionOptions( String dimension, DimensionType type, String dimensionName, List<NameableObject> options )
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( dimension ) );
        
        if ( index != -1 )
        {
            dimensions.set( index, new BaseDimensionalObject( dimension, type, dimensionName, null, options ) );
        }
        else
        {
            dimensions.add( new BaseDimensionalObject( dimension, type, dimensionName, null, options ) );
        }
        
        return this;
    }
    
    /**
     * Retrieves the options for the given filter. Returns an empty list if the
     * filter is not present.
     */
    public List<NameableObject> getFilterOptions( String filter )
    {
        int index = filters.indexOf( new BaseDimensionalObject( filter ) );
        
        return index != -1 ? filters.get( index ).getItems() : new ArrayList<NameableObject>();
    }

    /**
     * Retrieves the filter with the given filter identifier.
     */
    public DimensionalObject getFilter( String filter )
    {
        int index = filters.indexOf( new BaseDimensionalObject( filter ) );
        
        return index != -1 ? filters.get( index ) : null;
    }
    
    /**
     * Sets the options for the given filter.
     */
    public DataQueryParams setFilterOptions( String filter, DimensionType type, String dimensionName, List<NameableObject> options )
    {
        int index = filters.indexOf( new BaseDimensionalObject( filter ) );
        
        if ( index != -1 )
        {
            filters.set( index, new BaseDimensionalObject( filter, type, dimensionName, null, options ) );
        }
        else
        {
            filters.add( new BaseDimensionalObject( filter, type, dimensionName, null, options ) );
        }
        
        return this;
    }
    
    /**
     * Updates the options for the given filter.
     */
    public DataQueryParams updateFilterOptions( String filter, List<NameableObject> options )
    {
        int index = filters.indexOf( new BaseDimensionalObject( filter ) );
        
        if ( index != -1 )
        {
            DimensionalObject existing = filters.get( index );
            
            filters.set( index, new BaseDimensionalObject( existing.getDimension(), 
                existing.getDimensionType(), existing.getDimensionName(), existing.getDisplayName(), options ) );
        }
        
        return this;
    }

    /**
     * Get all filter items.
     */
    public List<NameableObject> getFilterItems()
    {
        List<NameableObject> filterItems = new ArrayList<>();
        
        for ( DimensionalObject filter : filters )
        {
            if ( filter != null && filter.hasItems() )
            {
                filterItems.addAll( filter.getItems() );
            }
        }
        
        return filterItems;
    }
    
    /**
     * Returns a list of dimensions and filters in the mentioned, preserved order.
     */
    public List<DimensionalObject> getDimensionsAndFilters()
    {
        List<DimensionalObject> list = new ArrayList<>();
        list.addAll( dimensions );
        list.addAll( filters );
        return list;
    }
    
    /**
     * Returns a list of dimensions and filters of the given dimension type.
     */
    public List<DimensionalObject> getDimensionsAndFilters( DimensionType dimensionType )
    {
        List<DimensionalObject> list = new ArrayList<>();
        
        if ( dimensionType != null )
        {
            for ( DimensionalObject dimension : getDimensionsAndFilters() )
            {
                if ( dimension.getDimensionType().equals( dimensionType ) )
                {
                    list.add( dimension );
                }
            }
        }
        
        return list;
    }

    /**
     * Retrieves the set of dimension types which are present in dimensions and
     * filters.
     */
    public Set<DimensionType> getDimensionTypes()
    {
        Set<DimensionType> types = new HashSet<>();
        
        for ( DimensionalObject dim : getDimensionsAndFilters() )
        {
            types.add( dim.getDimensionType() );
        }
        
        return types;
    }
    
    /**
     * Returns the number of days in the first dimension period in this query.
     * If no dimension periods exist, the frequency order of the period type of
     * the query is returned. If no period type exists, -1 is returned.
     * @return
     */
    public int getDaysInFirstPeriod()
    {
        List<NameableObject> periods = getPeriods();
        
        Period period = !periods.isEmpty() ? (Period) periods.get( 0 ) : null;
        
        return period != null ? period.getDaysInPeriod() : periodType != null ? 
            PeriodType.getPeriodTypeByName( periodType ).getFrequencyOrder() : -1;
    }
    
    /**
     * Indicates whether this query defines an identifier scheme different from
     * UID.
     */
    public boolean hasNonUidOutputIdScheme()
    {
        return outputIdScheme != null && !IdentifiableProperty.UID.equals( outputIdScheme );
    }

    /**
     * Indicates whether this query specifies data approval levels.
     */
    public boolean isDataApproval()
    {
        return dataApprovalLevels != null && !dataApprovalLevels.isEmpty();
    }
    
    /**
     * Indicates whether this query specifies a approval level.
     */
    public boolean hasApprovalLevel()
    {
        return approvalLevel != null;
    }
        
    /**
     * Ignore data approval constraints for this query.
     */
    public void ignoreDataApproval()
    {
        this.dataApprovalLevels = new HashMap<>();
    }
    
    /**
     * Indicates whether this query requires aggregation of data. No aggregation
     * takes place if aggregation type is none or if data type is text.
     */
    public boolean isAggregation()
    {
        return !( AggregationType.NONE.equals( aggregationType ) || DataType.TEXT.equals( dataType ) );
    }
    
    /**
     * Returns all dimension items.
     */
    public List<NameableObject> getAllDimensionItems()
    {
        List<NameableObject> items = new ArrayList<NameableObject>();
        
        for ( DimensionalObject dim : ListUtils.union( dimensions, filters ) )
        {
            items.addAll( dim.getItems() );
        }
        
        return items;
    }

    /**
     * Indicates whether this object has a program.
     */
    public boolean hasProgram()
    {
        return program != null;
    }

    /**
     * Indicates whether this object has a program stage.
     */
    public boolean hasProgramStage()
    {
        return programStage != null;
    }
        
    // -------------------------------------------------------------------------
    // Static methods
    // -------------------------------------------------------------------------

    /**
     * Populates a mapping of permutation keys and mappings of data element operands
     * and values based on the given mapping of dimension option keys and 
     * aggregated values. The data element dimension will be at index 0 and the
     * category option combo dimension will be at index 1, if category option
     * combinations is enabled.
     * 
     * @param permutationMap the map to populate with permutations.
     * @param aggregatedDataMap the aggregated data map.
     * @param cocEnabled indicates whether the given aggregated data map includes
     *        a category option combination dimension.
     */
    public static void putPermutationOperandValueMap( MapMap<String, DataElementOperand, Double> permutationMap, 
        Map<String, Double> aggregatedDataMap, boolean cocEnabled )
    {
        for ( String key : aggregatedDataMap.keySet() )
        {
            List<String> keys = Lists.newArrayList( key.split( DIMENSION_SEP ) );
            
            String de = keys.get( DX_INDEX );
            String coc = cocEnabled ? keys.get( CO_INDEX ) : null;

            DataElementOperand operand = new DataElementOperand( de, coc );
            
            ListUtils.removeAll( keys, DX_INDEX, ( cocEnabled ? CO_INDEX : -1 ) );
            
            String permKey = StringUtils.join( keys, DIMENSION_SEP );
            
            Double value = aggregatedDataMap.get( key );
            
            permutationMap.putEntry( permKey, operand, value );            
        }
    }
    
    /**
     * Returns a mapping of permutations keys (org unit id or null) and mappings
     * of org unit group and counts, based on the given mapping of dimension option
     * keys and counts.
     */
    public static Map<String, Map<String, Integer>> getPermutationOrgUnitGroupCountMap( Map<String, Double> orgUnitCountMap )
    {
        MapMap<String, String, Integer> countMap = new MapMap<>();
        
        for ( String key : orgUnitCountMap.keySet() )
        {
            List<String> keys = Lists.newArrayList( key.split( DIMENSION_SEP ) );
            
            // Org unit group always at last index, org unit potentially at first
            
            int ougInx = keys.size() - 1;
            
            String oug = keys.get( ougInx );
            
            ListUtils.removeAll( keys, ougInx );

            String permKey = StringUtils.trimToNull( StringUtils.join( keys, DIMENSION_SEP ) );
            
            Integer count = orgUnitCountMap.get( key ).intValue();
            
            countMap.putEntry( permKey, oug, count );
        }
        
        return countMap;
    }
    
    /**
     * Retrieves the measure criteria from the given string. Criteria are separated
     * by the option separator, while the criterion filter and value are separated
     * with the dimension name separator.
     */
    public static Map<MeasureFilter, Double> getMeasureCriteriaFromParam( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        Map<MeasureFilter, Double> map = new HashMap<>();
        
        String[] criteria = param.split( DimensionalObject.OPTION_SEP );
        
        for ( String c : criteria )
        {
            String[] criterion = c.split( DimensionalObject.DIMENSION_NAME_SEP );
            
            if ( criterion != null && criterion.length == 2 && MathUtils.isNumeric( criterion[1] ) )
            {
                MeasureFilter filter = MeasureFilter.valueOf( criterion[0] );
                Double value = Double.valueOf( criterion[1] );
                map.put( filter, value );
            }
        }
        
        return map;
    }
    
    /**
     * Adds the given dimensions to the dimensions of this query. If the dimension
     * is a data dimension it will be added to the beginning of the list of dimensions.
     */
    public void addDimensions( List<DimensionalObject> dimension )
    {
        for ( DimensionalObject dim : dimension )
        {
            addDimension( dim );
        }
    }

    /**
     * Adds the given dimension to the dimensions of this query. If the dimension
     * is a data dimension it will be added to the beginning of the list of dimensions.
     */
    public void addDimension( DimensionalObject dimension )
    {
        if ( DATA_X_DIM_ID.equals( dimension.getDimension() ) )
        {
            dimensions.add( DX_INDEX, dimension );
        }
        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension.getDimension() ) )
        {
            int index = !dimensions.isEmpty() && DATA_X_DIM_ID.equals( dimensions.get( 0 ).getDimension() ) ? CO_INDEX : DX_INDEX;
            
            dimensions.add( index, dimension );
        }
        else
        {
            dimensions.add( dimension );
        }
    }
        
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<String> getDimensionIdentifiersAsList()
    {
        List<String> list = new ArrayList<>();
        
        for ( DimensionalObject dimension : dimensions )
        {
            list.add( dimension.getDimension() );
        }
        
        return list;
    }
        
    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( dimensions == null ) ? 0 : dimensions.hashCode() );
        result = prime * result + ( ( filters == null ) ? 0 : filters.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }
        
        if ( object == null )
        {
            return false;
        }
        
        if ( getClass() != object.getClass() )
        {
            return false;
        }
        
        DataQueryParams other = (DataQueryParams) object;
        
        if ( dimensions == null )
        {
            if ( other.dimensions != null )
            {
                return false;
            }
        }
        else if ( !dimensions.equals( other.dimensions ) )
        {
            return false;
        }
        
        if ( filters == null )
        {
            if ( other.filters != null )
            {
                return false;
            }
        }
        else if ( !filters.equals( other.filters ) )
        {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString()
    {
        return "[Dimensions: " + dimensions + ", Filters: " + filters + "]";
    }
    
    // -------------------------------------------------------------------------
    // Get and set methods for serialized properties
    // -------------------------------------------------------------------------

    public List<DimensionalObject> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions( List<DimensionalObject> dimensions )
    {
        this.dimensions = dimensions;
    }

    public List<DimensionalObject> getFilters()
    {
        return filters;
    }

    public void setFilters( List<DimensionalObject> filters )
    {
        this.filters = filters;
    }

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    public Map<MeasureFilter, Double> getMeasureCriteria()
    {
        return measureCriteria;
    }

    public void setMeasureCriteria( Map<MeasureFilter, Double> measureCriteria )
    {
        this.measureCriteria = measureCriteria;
    }

    public boolean isSkipMeta()
    {
        return skipMeta;
    }

    public void setSkipMeta( boolean skipMeta )
    {
        this.skipMeta = skipMeta;
    }

    public boolean isSkipData()
    {
        return skipData;
    }

    public void setSkipData( boolean skipData )
    {
        this.skipData = skipData;
    }

    public boolean isSkipRounding()
    {
        return skipRounding;
    }

    public void setSkipRounding( boolean skipRounding )
    {
        this.skipRounding = skipRounding;
    }

    public boolean isCompletedOnly()
    {
        return completedOnly;
    }

    public void setCompletedOnly( boolean completedOnly )
    {
        this.completedOnly = completedOnly;
    }
    
    public boolean isHierarchyMeta()
    {
        return hierarchyMeta;
    }

    public void setHierarchyMeta( boolean hierarchyMeta )
    {
        this.hierarchyMeta = hierarchyMeta;
    }

    public boolean isIgnoreLimit()
    {
        return ignoreLimit;
    }

    public void setIgnoreLimit( boolean ignoreLimit )
    {
        this.ignoreLimit = ignoreLimit;
    }

    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }

    public void setHideEmptyRows( boolean hideEmptyRows )
    {
        this.hideEmptyRows = hideEmptyRows;
    }

    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public void setShowHierarchy( boolean showHierarchy )
    {
        this.showHierarchy = showHierarchy;
    }

    public DisplayProperty getDisplayProperty()
    {
        return displayProperty;
    }

    public void setDisplayProperty( DisplayProperty displayProperty )
    {
        this.displayProperty = displayProperty;
    }

    public IdentifiableProperty getOutputIdScheme()
    {
        return outputIdScheme;
    }

    public void setOutputIdScheme( IdentifiableProperty outputIdScheme )
    {
        this.outputIdScheme = outputIdScheme;
    }

    public String getApprovalLevel()
    {
        return approvalLevel;
    }

    public void setApprovalLevel( String approvalLevel )
    {
        this.approvalLevel = approvalLevel;
    }

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

    // -------------------------------------------------------------------------
    // Get and set methods for transient properties
    // -------------------------------------------------------------------------

    public Partitions getPartitions()
    {
        return partitions;
    }

    public void setPartitions( Partitions partitions )
    {
        this.partitions = partitions;
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public void setDataType( DataType dataType )
    {
        this.dataType = dataType;
    }

    public String getPeriodType()
    {
        return periodType;
    }

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
    }

    public PeriodType getDataPeriodType()
    {
        return dataPeriodType;
    }

    public void setDataPeriodType( PeriodType dataPeriodType )
    {
        this.dataPeriodType = dataPeriodType;
    }

    public boolean isSkipPartitioning()
    {
        return skipPartitioning;
    }

    public void setSkipPartitioning( boolean skipPartitioning )
    {
        this.skipPartitioning = skipPartitioning;
    }

    public Map<OrganisationUnit, Integer> getDataApprovalLevels()
    {
        return dataApprovalLevels;
    }

    public void setDataApprovalLevels( Map<OrganisationUnit, Integer> dataApprovalLevels )
    {
        this.dataApprovalLevels = dataApprovalLevels;
    }

    // -------------------------------------------------------------------------
    // Get and set helpers for dimensions or filter
    // -------------------------------------------------------------------------
  
    /**
     * Retrieves the options for the the dimension or filter with the given 
     * identifier. Returns an empty list if the dimension or filter is not present.
     */
    public List<NameableObject> getDimensionOrFilterItems( String key )
    {
        List<NameableObject> dimensionOptions = getDimensionOptions( key );
        
        return !dimensionOptions.isEmpty() ? dimensionOptions : getFilterOptions( key );
    }
    
    /**
     * Retrieves the options for the given dimension identifier. If the co 
     * dimension is specified, all category option combos for the first data 
     * element is returned. Returns an empty array if the dimension is not present.
     */
    public NameableObject[] getDimensionArrayExplodeCoc( String dimension )
    {
        List<NameableObject> items = new ArrayList<>();
        
        if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            List<NameableObject> des = getDataElements();
            
            if ( !des.isEmpty() )
            {
                Set<DataElementCategoryCombo> categoryCombos = des.stream().map( d -> ((DataElement) d).getCategoryCombo() ).collect( Collectors.toSet() );
                
                for ( DataElementCategoryCombo cc : categoryCombos )
                {
                    items.addAll( cc.getSortedOptionCombos() );
                }                
            }
        }
        else
        {
            items.addAll( getDimensionOptions( dimension ) );
        }
        
        return items.toArray( new NameableObject[0] );
    }
    
    /**
     * Indicates whether a dimension or filter with the given identifier exists.
     */
    public boolean hasDimensionOrFilter( String key )
    {
        return dimensions.indexOf( new BaseDimensionalObject( key ) ) != -1 || filters.indexOf( new BaseDimensionalObject( key ) ) != -1;
    }

    /**
     * Indicates whether a dimension or filter with the given identifier exists.
     */
    public boolean hasDimension( String key )
    {
        return dimensions.indexOf( new BaseDimensionalObject( key ) ) != -1;
    }

    /**
     * Indicates whether a dimension or filter which specifies dimension items 
     * with the given identifier exists.
     */
    public boolean hasDimensionOrFilterWithItems( String key )
    {
        return !getDimensionOrFilterItems( key ).isEmpty();
    }

    /**
     * Sets the given list of data dimension options. Replaces existing options
     * of the given data dimension type.
     * 
     * @param itemType the data dimension type.
     * @param options the data dimension options.
     */
    private void setDataDimensionOptions( DataDimensionItemType itemType, List<? extends NameableObject> options )
    {
        List<NameableObject> existing = AnalyticsUtils.getByDataDimensionType( itemType, getDimensionOptions( DATA_X_DIM_ID ) );
        DimensionalObject dimension = getDimension( DATA_X_DIM_ID );
        
        if ( dimension == null )
        {
            dimension = new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X, options );
            addDimension( dimension );
        }
        else
        {        
            dimension.getItems().removeAll( existing );
            dimension.getItems().addAll( options );
        }
    }
    
    // -------------------------------------------------------------------------
    // Get and set helpers for dimensions and filters
    // -------------------------------------------------------------------------

    public List<NameableObject> getAllIndicators()
    {
        return ImmutableList.copyOf( ListUtils.union( getIndicators(), getFilterIndicators() ) );
    }
    
    public List<NameableObject> getAllDataElements()
    {
        return ImmutableList.copyOf( ListUtils.union( getDataElements(), getFilterDataElements() ) );
    }

    public List<NameableObject> getAllDataSets()
    {
        return ImmutableList.copyOf( ListUtils.union( getDataSets(), getFilterDataSets() ) );
    }
    
    public List<NameableObject> getAllProgramAttributes()
    {
        return ImmutableList.copyOf( ListUtils.union( getProgramAttributes(), getFilterProgramAttributes() ) );
    }

    public List<NameableObject> getAllProgramDataElements()
    {
        return ImmutableList.copyOf( ListUtils.union( getProgramDataElements(), getFilterProgramDataElements() ) );
    }

    public List<NameableObject> getAllProgramDataElementsAndAttributes()
    {
        return ListUtils.union( getAllProgramAttributes(), getAllProgramDataElements() );
    }
    
    public DataQueryParams retainDataDimension( DataDimensionItemType itemType )
    {
        DimensionalObject dimension = getDimensionOrFilter( DATA_X_DIM_ID );
        
        List<NameableObject> items = AnalyticsUtils.getByDataDimensionType( itemType, dimension.getItems() );
        
        dimension.getItems().clear();
        dimension.getItems().addAll( items );
        
        return this;
    }
    
    public DataQueryParams retainDataDimensions( DataDimensionItemType... itemTypes )
    {
        DimensionalObject dimension = getDimensionOrFilter( DATA_X_DIM_ID );
        
        List<NameableObject> items = new ArrayList<>();
        
        for ( DataDimensionItemType itemType : itemTypes )
        {
            items.addAll( AnalyticsUtils.getByDataDimensionType( itemType, dimension.getItems() ) );
        }

        dimension.getItems().clear();
        dimension.getItems().addAll( items );
        
        return this;
    }
    
    // -------------------------------------------------------------------------
    // Get and set helpers for dimensions
    // -------------------------------------------------------------------------
  
    public List<NameableObject> getIndicators()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.INDICATOR, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setIndicators( List<? extends NameableObject> indicators )
    {
        setDataDimensionOptions( DataDimensionItemType.INDICATOR, indicators );
    }
    
    public List<NameableObject> getDataElements()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public List<NameableObject> getDataElementOperands()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.DATA_ELEMENT_OPERAND, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setDataElements( List<? extends NameableObject> dataElements )
    {
        setDataDimensionOptions( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, dataElements );
    }
    
    public List<NameableObject> getDataSets()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.DATA_SET, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }

    public void setDataSets( List<? extends NameableObject> dataSets )
    {
        setDataDimensionOptions( DataDimensionItemType.DATA_SET, dataSets );
    }

    public List<NameableObject> getProgramIndicators()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_INDICATOR, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setProgramIndicators( List<? extends NameableObject> programIndicators )
    {
        setDataDimensionOptions( DataDimensionItemType.PROGRAM_INDICATOR, programIndicators );
    }

    public List<NameableObject> getProgramDataElements()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_DATA_ELEMENT, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setProgramDataElements( List<? extends NameableObject> programDataElements )
    {
        setDataDimensionOptions( DataDimensionItemType.PROGRAM_DATA_ELEMENT, programDataElements );
    }
    
    public List<NameableObject> getProgramAttributes()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_ATTRIBUTE, getDimensionOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setProgramAttributes( List<? extends NameableObject> programAttributes )
    {
        setDataDimensionOptions( DataDimensionItemType.PROGRAM_ATTRIBUTE, programAttributes );
    }
    
    public List<NameableObject> getPeriods()
    {
        return getDimensionOptions( PERIOD_DIM_ID );
    }
    
    public void setPeriods( List<? extends NameableObject> periods )
    {
        setDimensionOptions( PERIOD_DIM_ID, DimensionType.PERIOD, null, asList( periods ) );
    }
    
    public void setPeriod( NameableObject period )
    {
        setPeriods( getList( period ) );
    }

    public List<NameableObject> getOrganisationUnits()
    {
        return getDimensionOptions( ORGUNIT_DIM_ID );
    }
    
    public void setOrganisationUnits( List<? extends NameableObject> organisationUnits )
    {
        setDimensionOptions( ORGUNIT_DIM_ID, DimensionType.ORGANISATIONUNIT, null, asList( organisationUnits ) );
    }
    
    public void setOrganisationUnit( NameableObject organisationUnit )
    {
        setOrganisationUnits( getList( organisationUnit ) );
    }

    public List<DimensionalObject> getDataElementGroupSets()
    {
        return ListUtils.union( dimensions, filters ).stream().
            filter( d -> DimensionType.DATAELEMENT_GROUPSET.equals( d.getDimensionType() ) ).collect( Collectors.toList() );
    }
    
    public void setDataElementGroupSet( DataElementGroupSet groupSet )
    {
        setDimensionOptions( groupSet.getUid(), DimensionType.DATAELEMENT_GROUPSET, null, new ArrayList<>( groupSet.getItems() ) );
    }
    
    public void setOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        setDimensionOptions( groupSet.getUid(), DimensionType.ORGANISATIONUNIT_GROUPSET, null, new ArrayList<>( groupSet.getItems() ) );
    }

    public void setCategory( DataElementCategory category )
    {
        setDimensionOptions( category.getUid(), DimensionType.CATEGORY, null, new ArrayList<>( category.getItems() ) );
    }
    
    public void setCategoryOptionCombos( List<? extends NameableObject> categoryOptionCombos )
    {
        setDimensionOptions( CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.CATEGORY_OPTION_COMBO, null, asList( categoryOptionCombos ) );
    }
    
    public boolean isCategoryOptionCombosEnabled()
    {
        return !getDimensionOrFilterItems( CATEGORYOPTIONCOMBO_DIM_ID ).isEmpty();
    }
    
    // -------------------------------------------------------------------------
    // Get and set helpers for filters
    // -------------------------------------------------------------------------

    public List<NameableObject> getFilterIndicators()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.INDICATOR, getFilterOptions( DATA_X_DIM_ID ) ) );
    }
    
    public List<NameableObject> getFilterDataElements()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, getFilterOptions( DATA_X_DIM_ID ) ) );
    }

    public List<NameableObject> getFilterDataSets()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.DATA_SET, getFilterOptions( DATA_X_DIM_ID ) ) );
    }
    
    public List<NameableObject> getFilterPeriods()
    {
        return getFilterOptions( PERIOD_DIM_ID );
    }
    
    public void setFilterPeriods( List<NameableObject> periods )
    {
        setFilterOptions( PERIOD_DIM_ID, DimensionType.PERIOD, null, periods );
    }
    
    public void setFilterPeriod( Period period )
    {
        setFilterPeriods( getList( period ) );
    }
    
    public List<NameableObject> getFilterOrganisationUnits()
    {
        return getFilterOptions( ORGUNIT_DIM_ID );
    }
    
    public void setFilterOrganisationUnits( List<NameableObject> organisationUnits )
    {
        setFilterOptions( ORGUNIT_DIM_ID, DimensionType.ORGANISATIONUNIT, null, organisationUnits );
    }
    
    public void setFilterOrganisationUnit( NameableObject organisationUnit )
    {
        setFilterOrganisationUnits( getList( organisationUnit ) );
    }

    public List<NameableObject> getFilterProgramDataElements()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_DATA_ELEMENT, getFilterOptions( DATA_X_DIM_ID ) ) );
    }
    
    public List<NameableObject> getFilterProgramAttributes()
    {
        return ImmutableList.copyOf( AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_ATTRIBUTE, getFilterOptions( DATA_X_DIM_ID ) ) );
    }
    
    public void setFilter( String filter, DimensionType type, NameableObject item )
    {
        setFilterOptions( filter, type, null, getList( item ) );
    }
}
