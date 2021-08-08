/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * Class which encapsulates a query parameter and value. Operator and filter are
 * inherited from QueryFilter.
 *
 * @author Lars Helge Overland
 */
public class QueryItem
{
    private DimensionalItemObject item; // TODO DimensionObject

    private LegendSet legendSet;

    private List<QueryFilter> filters = new ArrayList<>();

    private ValueType valueType;

    private AggregationType aggregationType;

    private OptionSet optionSet;

    private Program program;

    private ProgramStage programStage;

    private Boolean unique = false;

    private RelationshipType relationshipType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public QueryItem( DimensionalItemObject item )
    {
        this.item = item;
    }

    public QueryItem( DimensionalItemObject item, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet )
    {
        this.item = item;
        this.legendSet = legendSet;
        this.valueType = valueType;
        this.aggregationType = aggregationType;
        this.optionSet = optionSet;
    }

    public QueryItem( DimensionalItemObject item, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet, Boolean unique )
    {
        this.item = item;
        this.legendSet = legendSet;
        this.valueType = valueType;
        this.aggregationType = aggregationType;
        this.optionSet = optionSet;
        this.unique = unique;
    }

    public QueryItem( DimensionalItemObject item, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet, RelationshipType relationshipType )
    {
        this( item, legendSet, valueType, aggregationType, optionSet );
        this.relationshipType = relationshipType;
    }

    public QueryItem( DimensionalItemObject item, Program program, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet )
    {
        this( item, legendSet, valueType, aggregationType, optionSet );

        this.program = program;
    }

    public QueryItem( DimensionalItemObject item, Program program, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet, RelationshipType relationshipType )
    {
        this( item, program, legendSet, valueType, aggregationType, optionSet );

        this.relationshipType = relationshipType;
    }

    public QueryItem( DimensionalItemObject item, QueryOperator operator, String filter, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet )
    {
        this.item = item;
        this.valueType = valueType;
        this.aggregationType = aggregationType;
        this.optionSet = optionSet;

        if ( operator != null && filter != null )
        {
            this.filters.add( new QueryFilter( operator, filter ) );
        }
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public String getItemId()
    {
        return item.getUid();
    }

    public String getItemName()
    {
        String itemName = item.getUid();

        if ( legendSet != null )
        {
            itemName += "_" + legendSet.getUid();
        }

        return itemName;
    }

    public boolean addFilter( QueryFilter filter )
    {
        return filters.add( filter );
    }

    public String getKey()
    {
        QueryKey key = new QueryKey();

        key.add( "item", getItemId() ).addIgnoreNull( "filter", getFiltersAsString() );

        if ( legendSet != null )
        {
            key.add( "legendSet", legendSet.getUid() );
        }

        return key.build();
    }

    /**
     * Returns a string representation of the query filters. Returns null if
     * item has no query items.
     */
    public String getFiltersAsString()
    {
        if ( filters.isEmpty() )
        {
            return null;
        }

        List<String> filterStrings = filters.stream().map( QueryFilter::getFilterAsString )
            .collect( Collectors.toList() );
        return StringUtils.join( filterStrings, ", " );
    }

    public boolean isNumeric()
    {
        return valueType.isNumeric();
    }

    public boolean isText()
    {
        return valueType.isText();
    }

    public boolean hasLegendSet()
    {
        return legendSet != null;
    }

    public boolean hasOptionSet()
    {
        return optionSet != null;
    }

    public boolean hasFilter()
    {
        return filters != null && !filters.isEmpty();
    }

    public boolean hasProgram()
    {
        return program != null;
    }

    public boolean hasProgramStage()
    {
        return programStage != null;
    }

    public boolean isProgramIndicator()
    {
        return DimensionItemType.PROGRAM_INDICATOR.equals( item.getDimensionItemType() );
    }

    public boolean hasRelationshipType()
    {
        return this.getRelationshipType() != null;
    }

    /**
     * Returns filter items for all filters associated with this query item. If
     * no filter items are specified, return all items part of the legend set.
     * If not legend set is specified, returns null.
     */
    public List<String> getLegendSetFilterItemsOrAll()
    {
        if ( !hasLegendSet() )
        {
            return null;
        }

        return hasFilter() ? getQueryFilterItems() : IdentifiableObjectUtils.getUids( legendSet.getSortedLegends() );
    }

    /**
     * Returns filter items for all filters associated with this query item. If
     * no filter items are specified, return all items part of the option set.
     * If not option set is specified, returns null.
     */
    public List<String> getOptionSetFilterItemsOrAll()
    {
        if ( !hasOptionSet() )
        {
            return null;
        }

        return hasFilter() ? getOptionSetQueryFilterItems() : IdentifiableObjectUtils.getUids( optionSet.getOptions() );
    }

    /**
     * Returns option filter items. Options are specified by code but returned
     * as identifiers, so the codes are mapped to options and then to
     * identifiers.
     *
     * //TODO clean up and standardize on identifier.
     */
    private List<String> getOptionSetQueryFilterItems()
    {
        return getQueryFilterItems().stream()
            .map( code -> optionSet.getOptionByCode( code ) )
            .filter( option -> option != null )
            .map( option -> option.getUid() )
            .collect( Collectors.toList() );
    }

    /**
     * Returns filter items for all filters associated with this query item.
     */
    public List<String> getQueryFilterItems()
    {
        List<String> filterItems = new ArrayList<>();
        filters.forEach( f -> filterItems.addAll( QueryFilter.getFilterItems( f.getFilter() ) ) );
        return filterItems;
    }

    /**
     * Returns SQL filter for the given query filter and SQL encoded filter. If
     * the item value type is text-based, the filter is converted to lower-case.
     *
     * @param filter the query filter.
     * @param encodedFilter the SQL encoded filter.
     */
    public String getSqlFilter( QueryFilter filter, String encodedFilter )
    {
        String sqlFilter = filter.getSqlFilter( encodedFilter );

        return isText() ? sqlFilter.toLowerCase() : sqlFilter;
    }

    // -------------------------------------------------------------------------
    // Static utilities
    // -------------------------------------------------------------------------

    public static List<QueryItem> getQueryItems( Collection<TrackedEntityAttribute> attributes )
    {
        List<QueryItem> queryItems = new ArrayList<>();

        for ( TrackedEntityAttribute attribute : attributes )
        {
            queryItems.add( new QueryItem( attribute,
                (attribute.getLegendSets().isEmpty() ? null : attribute.getLegendSets().get( 0 )),
                attribute.getValueType(), attribute.getAggregationType(),
                attribute.hasOptionSet() ? attribute.getOptionSet() : null ) );
        }

        return queryItems;
    }

    public static List<QueryItem> getDataElementQueryItems( Collection<DataElement> dataElements )
    {
        List<QueryItem> queryItems = new ArrayList<>();

        for ( DataElement dataElement : dataElements )
        {
            queryItems.add( new QueryItem( dataElement, dataElement.getLegendSet(), dataElement.getValueType(),
                dataElement.getAggregationType(), dataElement.hasOptionSet() ? dataElement.getOptionSet() : null ) );
        }

        return queryItems;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return Objects.hash( item, program, programStage );
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

        final QueryItem other = (QueryItem) object;

        return Objects.equals( item, other.getItem() ) &&
            Objects.equals( program, other.getProgram() ) &&
            Objects.equals( programStage, other.getProgramStage() );
    }

    @Override
    public String toString()
    {
        return "[Item: " + item + ", legend set: " + legendSet + ", filters: " + filters +
            ", value type: " + valueType + ", optionSet: " + optionSet +
            ", program: " + program + ", program stage: " + programStage + "]";
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public DimensionalItemObject getItem()
    {
        return item;
    }

    public void setItem( DimensionalItemObject item )
    {
        this.item = item;
    }

    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    public List<QueryFilter> getFilters()
    {
        return filters;
    }

    public void setFilters( List<QueryFilter> filters )
    {
        this.filters = filters;
    }

    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    public OptionSet getOptionSet()
    {
        return optionSet;
    }

    public void setOptionSet( OptionSet optionSet )
    {
        this.optionSet = optionSet;
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

    public Boolean isUnique()
    {
        return unique;
    }

    public void setUnique( Boolean unique )
    {
        this.unique = unique;
    }

    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }
}
