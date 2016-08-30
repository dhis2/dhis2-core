package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.legend.LegendSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

@JacksonXmlRootElement( localName = "dimension", namespace = DxfNamespaces.DXF_2_0 )
public class BaseDimensionalObject
    extends BaseNameableObject implements DimensionalObject
{
    /**
     * The type of this dimension.
     */
    private DimensionType dimensionType;

    /**
     * The name of this dimension. For the dynamic dimensions this will be equal
     * to dimension identifier. For the period dimension, this will reflect the
     * period type. For the org unit dimension, this will reflect the level.
     */
    private String dimensionName;

    /**
     * The dimensional items for this dimension.
     */
    private List<DimensionalItemObject> items = new ArrayList<>();

    /**
     * Indicates whether all available items in this dimension are included.
     */
    private boolean allItems;

    /**
     * The legend set for this dimension.
     */
    protected LegendSet legendSet;

    /**
     * The aggregation type for this dimension.
     */
    protected AggregationType aggregationType;

    /**
     * Filter. Applicable for events. Contains operator and filter on this format:
     * <operator>:<filter>;<operator>:<filter>
     * Operator and filter pairs can be repeated any number of times.
     */
    private String filter;

    //--------------------------------------------------------------------------
    // Persistent properties
    //--------------------------------------------------------------------------

    /**
     * Indicates whether this object should be handled as a data dimension.
     */
    protected boolean dataDimension = true;

    /**
     * Indicates whether this dimension is fixed, meaning that the name of the
     * dimension will be returned as is for all dimension items in the response.
     */
    private boolean fixed;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public BaseDimensionalObject()
    {
    }

    public BaseDimensionalObject( String dimension )
    {
        this.uid = dimension;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, List<? extends DimensionalItemObject> items )
    {
        this.uid = dimension;
        this.dimensionType = dimensionType;
        this.items = new ArrayList<>( items );
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName, String displayName, List<? extends DimensionalItemObject> items )
    {
        this( dimension, dimensionType, items );
        this.dimensionName = dimensionName;
        this.displayName = displayName;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName, String displayName, List<? extends DimensionalItemObject> items, boolean allItems )
    {
        this( dimension, dimensionType, dimensionName, displayName, items );
        this.allItems = allItems;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName, String displayName, LegendSet legendSet, String filter )
    {
        this.uid = dimension;
        this.dimensionType = dimensionType;
        this.dimensionName = dimensionName;
        this.displayName = displayName;
        this.legendSet = legendSet;
        this.filter = filter;
    }

    // TODO aggregationType in constructors

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public DimensionalObject instance()
    {
        BaseDimensionalObject object = new BaseDimensionalObject( this.uid,
            this.dimensionType, this.dimensionName, this.displayName, this.items, this.allItems );

        object.legendSet = this.legendSet;
        object.aggregationType = this.aggregationType;
        object.filter = this.filter;
        object.dataDimension = this.dataDimension;
        object.fixed = this.fixed;

        return object;
    }

    @Override
    public boolean hasItems()
    {
        return !getItems().isEmpty();
    }

    @Override
    public boolean hasLegendSet()
    {
        return getLegendSet() != null;
    }

    @Override
    public String getDimensionName()
    {
        return dimensionName != null ? dimensionName : uid;
    }

    @Override
    public AnalyticsType getAnalyticsType()
    {
        return
            DimensionType.PROGRAM_ATTRIBUTE.equals( dimensionType ) ||
                DimensionType.PROGRAM_DATA_ELEMENT.equals( dimensionType ) ?
                AnalyticsType.EVENT : AnalyticsType.AGGREGATE;
    }

    /**
     * Returns the items in the filter as a list. Order of items are preserved.
     * Requires that the filter has the IN operator and that at least one item
     * is specified in the filter, returns null if not.
     */
    public List<String> getFilterItemsAsList()
    {
        final String inOp = QueryOperator.IN.getValue().toLowerCase();
        final int opLen = inOp.length() + 1;

        if ( filter == null || !filter.toLowerCase().startsWith( inOp ) || filter.length() < opLen )
        {
            return null;
        }

        String filterItems = filter.substring( opLen, filter.length() );

        return new ArrayList<>( Arrays.asList( filterItems.split( DimensionalObject.OPTION_SEP ) ) );
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDimension()
    {
        return uid;
    }

    public void setDimension( String dimension )
    {
        this.uid = dimension;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DimensionType getDimensionType()
    {
        return dimensionType;
    }

    public void setDimensionType( DimensionType dimensionType )
    {
        this.dimensionType = dimensionType;
    }

    public void setDimensionName( String dimensionName )
    {
        this.dimensionName = dimensionName;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalItemObject.class )
    @JacksonXmlElementWrapper( localName = "items", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "item", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalItemObject> getItems()
    {
        return items;
    }

    public void setItems( List<DimensionalItemObject> items )
    {
        this.items = items;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isAllItems()
    {
        return allItems;
    }

    public void setAllItems( boolean allItems )
    {
        this.allItems = allItems;
    }

    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFilter()
    {
        return filter;
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDataDimension()
    {
        return dataDimension;
    }

    public void setDataDimension( boolean dataDimension )
    {
        this.dataDimension = dataDimension;
    }

    @Override
    @JsonIgnore
    public boolean isFixed()
    {
        return fixed;
    }

    public void setFixed( boolean fixed )
    {
        this.fixed = fixed;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DimensionalObject dimensionalObject = (DimensionalObject) other;

            dataDimension = dimensionalObject.isDataDimension();

            if ( mergeMode.isReplace() )
            {
                dimensionType = dimensionalObject.getDimensionType();
                dimensionName = dimensionalObject.getDimensionName();
                legendSet = dimensionalObject.getLegendSet();
                aggregationType = dimensionalObject.getAggregationType();
                filter = dimensionalObject.getFilter();
            }
            else if ( mergeMode.isMerge() )
            {
                dimensionType = dimensionalObject.getDimensionType() == null ? dimensionType : dimensionalObject.getDimensionType();
                dimensionName = dimensionalObject.getDimensionName() == null ? dimensionName : dimensionalObject.getDimensionName();
                legendSet = dimensionalObject.getLegendSet() == null ? legendSet : dimensionalObject.getLegendSet();
                aggregationType = dimensionalObject.getAggregationType() == null ? aggregationType : dimensionalObject.getAggregationType();
                filter = dimensionalObject.getFilter() == null ? filter : dimensionalObject.getFilter();
            }

            items.clear();
            items.addAll( dimensionalObject.getItems() );
        }
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "Dimension", uid )
            .add( "type", dimensionType )
            .add( "dimension name", dimensionName )
            .add( "display name", displayName )
            .add( "items", items )
            .add( "all items", allItems )
            .add( "legend set", legendSet )
            .add( "aggregation type", aggregationType )
            .add( "filter", filter ).toString();
    }
}
