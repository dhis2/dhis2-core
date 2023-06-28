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
package org.hisp.dhis.common;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
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
@Getter
@Setter
public class QueryItem implements GroupableItem
{
    private UUID groupUUID;

    private DimensionalItemObject item; // TODO DimensionObject

    private LegendSet legendSet;

    private List<QueryFilter> filters = new ArrayList<>();

    private ValueType valueType;

    private AggregationType aggregationType;

    private OptionSet optionSet;

    private Program program;

    private ProgramStage programStage;

    private RepeatableStageParams repeatableStageParams;

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
        this( item );
        this.legendSet = legendSet;
        this.valueType = valueType;
        this.aggregationType = aggregationType;
        this.optionSet = optionSet;
    }

    public QueryItem( DimensionalItemObject item, LegendSet legendSet, ValueType valueType,
        AggregationType aggregationType, OptionSet optionSet, Boolean unique )
    {
        this( item, legendSet, valueType, aggregationType, optionSet );
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
        this( item );
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

        key.add( "item", getItemId() )
            .addIgnoreNull( "filter", getFiltersAsString() )
            .addIgnoreNull( "program", maybeGetProgramUid() );

        if ( legendSet != null )
        {
            key.add( "legendSet", legendSet.getUid() );
        }

        return key.build();
    }

    private String maybeGetProgramUid()
    {
        return program != null ? program.getUid() : null;
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

    public boolean hasAggregationType()
    {
        return aggregationType != null;
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
        return DimensionItemType.PROGRAM_INDICATOR == item.getDimensionItemType();
    }

    public boolean isUnique()
    {
        return unique != null && unique;
    }

    public boolean hasRelationshipType()
    {
        return relationshipType != null;
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
            .filter( Objects::nonNull )
            .map( IdentifiableObject::getUid )
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
     * Indicates whether a program stage and repeatable stage parameters which
     * is not a default object exists for this query item.
     */
    public boolean hasNonDefaultRepeatableProgramStageOffset()
    {
        return programStage != null && repeatableStageParams != null
            && !repeatableStageParams.isDefaultObject();
    }

    /**
     * Returns SQL filter for the given query filter and SQL encoded filter. If
     * the item value type is text-based, the filter is converted to lower-case.
     *
     * @param filter the query filter.
     * @param encodedFilter the SQL encoded filter.
     * @param isNullValueSubstitutionAllowed whether the text "NV" should be
     *        replaced by null in the query or not.
     */
    public String getSqlFilter( QueryFilter filter, String encodedFilter, boolean isNullValueSubstitutionAllowed )
    {
        return filter.getSqlFilter( encodedFilter, valueType, isNullValueSubstitutionAllowed );
    }

    /**
     * Returns the name that represents this object as a column in the analytics
     * response grid.
     *
     * @param displayProperty the {@link DisplayProperty}.
     * @param appendProgramStage if true, the program stage display name is
     *        appended.
     * @return the column name for this object.
     */
    public String getColumnName( DisplayProperty displayProperty, boolean appendProgramStage )
    {
        if ( getItem() != null )
        {
            String column = getItem().getDisplayProperty( displayProperty );

            if ( appendProgramStage && hasProgramStage() )
            {
                column = column + " - " + getProgramStage().getDisplayProperty( displayProperty );
            }

            return column;
        }

        return EMPTY;
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
        return Objects.hash( item, program, programStage, repeatableStageParams );
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
            Objects.equals( programStage, other.getProgramStage() ) &&
            Objects.equals( repeatableStageParams, other.getRepeatableStageParams() );
    }

    @Override
    public String toString()
    {
        return "[Item: " + item + ", legend set: " + legendSet + ", filters: " + filters
            + ", value type: " + valueType + ", optionSet: " + optionSet
            + ", program: " + program + ", program stage: " + programStage
            + "repeatable program stage params: "
            + (repeatableStageParams != null ? repeatableStageParams.toString() : null) + "]";
    }

    public boolean hasRepeatableStageParams()
    {
        return repeatableStageParams != null;
    }

    public int getProgramStageOffset()
    {
        return hasRepeatableStageParams() ? repeatableStageParams.getStartIndex() : 0;
    }
}
