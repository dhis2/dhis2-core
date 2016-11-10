package org.hisp.dhis.common;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * Class which encapsulates a query parameter and value. Operator and filter
 * are inherited from QueryFilter.
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

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public QueryItem( DimensionalItemObject item )
    {
        this.item = item;
    }

    public QueryItem( DimensionalItemObject item, LegendSet legendSet, ValueType valueType, AggregationType aggregationType, OptionSet optionSet )
    {
        this.item = item;
        this.legendSet = legendSet;
        this.valueType = valueType;
        this.aggregationType = aggregationType;
        this.optionSet = optionSet;
    }

    public QueryItem( DimensionalItemObject item, QueryOperator operator, String filter, ValueType valueType, AggregationType aggregationType, OptionSet optionSet )
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

    public String getTypeAsString()
    {
        return valueType.getJavaClass().getName();
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

    public String getLegendSetUid()
    {
        return legendSet != null ? legendSet.getUid() : null;
    }

    public String getOptionSetUid()
    {
        return optionSet != null ? optionSet.getUid() : null;
    }

    public boolean hasFilter()
    {
        return filters != null && !filters.isEmpty();
    }
    
    public boolean hasProgram()
    {
        return program != null;
    }

    public boolean isProgramIndicator()
    {
        return DimensionItemType.PROGRAM_INDICATOR.equals( item.getDimensionItemType() );
    }
        
    public static List<QueryItem> getQueryItems( Collection<TrackedEntityAttribute> attributes )
    {
        List<QueryItem> queryItems = new ArrayList<>();

        for ( TrackedEntityAttribute attribute : attributes )
        {
            queryItems.add( new QueryItem( attribute, attribute.getLegendSet(), attribute.getValueType(), attribute.getAggregationType(), attribute.hasOptionSet() ? attribute.getOptionSet() : null ) );
        }

        return queryItems;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return item.hashCode();
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

        return item.equals( other.getItem() );
    }

    @Override
    public String toString()
    {
        return "[Item: " + item + ", legend set: " + legendSet + ", filters: " + filters + ", value type: " + valueType + ", optionSet: " + optionSet + "]";
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
}
