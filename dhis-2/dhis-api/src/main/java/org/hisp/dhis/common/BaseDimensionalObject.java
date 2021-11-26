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

import static org.hisp.dhis.common.DisplayProperty.SHORTNAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

@JacksonXmlRootElement( localName = "dimension", namespace = DxfNamespaces.DXF_2_0 )
public class BaseDimensionalObject
    extends BaseNameableObject implements DimensionalObject
{
    /**
     * The type of this dimension.
     */
    private DimensionType dimensionType;

    /**
     * The data dimension type of this dimension. Can be null. Only applicable
     * for {@link DimensionType#CATEGORY}.
     */
    protected DataDimensionType dataDimensionType;

    /**
     * Indicates whether this object should be handled as a data dimension.
     */
    protected boolean dataDimension = true;

    /**
     * The name of this dimension. For the dynamic dimensions this will be equal
     * to dimension identifier. For the period dimension, this will reflect the
     * period type. For the org unit dimension, this will reflect the level.
     */
    private transient String dimensionName;

    /**
     * The display name to use for this dimension.
     */
    private transient String dimensionDisplayName;

    /**
     * Holds the value type of the parent dimension.
     */
    private transient ValueType valueType;

    /**
     * The option set associated with the dimension, if any.
     */
    private transient OptionSet optionSet;

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
     * The program stage for this dimension.
     */
    private ProgramStage programStage;

    /**
     * The aggregation type for this dimension.
     */
    protected AggregationType aggregationType;

    /**
     * Filter. Applicable for events. Contains operator and filter on this
     * format: <operator>:<filter>;<operator>:<filter> Operator and filter pairs
     * can be repeated any number of times.
     */
    private String filter;

    /**
     * A {@link DimensionItemKeywords} defines a pre-defined group of items. For
     * instance, all the OU withing a district
     */
    private DimensionItemKeywords dimensionalKeywords;

    /**
     * Indicates whether this dimension is fixed, meaning that the name of the
     * dimension will be returned as is for all dimension items in the response.
     */
    private boolean fixed;

    // --------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------

    // TODO displayName collides with translation solution, rename

    public BaseDimensionalObject()
    {
    }

    public BaseDimensionalObject( String dimension )
    {
        this.uid = dimension;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType,
        List<? extends DimensionalItemObject> items )
    {
        this.uid = dimension;
        this.dimensionType = dimensionType;
        this.items = new ArrayList<>( items );
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionDisplayName,
        List<? extends DimensionalItemObject> items )
    {
        this( dimension, dimensionType, items );
        this.dimensionDisplayName = dimensionDisplayName;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName,
        String dimensionDisplayName, List<? extends DimensionalItemObject> items )
    {
        this( dimension, dimensionType, items );
        this.dimensionName = dimensionName;
        this.dimensionDisplayName = dimensionDisplayName;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName,
        String dimensionDisplayName, List<? extends DimensionalItemObject> items,
        DimensionItemKeywords dimensionalKeywords )
    {
        this( dimension, dimensionType, dimensionName, dimensionDisplayName, items );
        this.dimensionalKeywords = dimensionalKeywords;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName,
        String dimensionDisplayName, List<? extends DimensionalItemObject> items, boolean allItems )
    {
        this( dimension, dimensionType, dimensionName, dimensionDisplayName, items );
        this.allItems = allItems;
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName,
        String dimensionDisplayName, LegendSet legendSet, ProgramStage programStage, String filter )
    {
        this( dimension, dimensionType, dimensionName, dimensionDisplayName, legendSet, programStage, filter, null,
            null );
    }

    public BaseDimensionalObject( String dimension, DimensionType dimensionType, String dimensionName,
        String dimensionDisplayName, LegendSet legendSet, ProgramStage programStage, String filter, ValueType valueType,
        OptionSet optionSet )
    {
        this( dimension );
        this.dimensionType = dimensionType;
        this.dimensionName = dimensionName;
        this.dimensionDisplayName = dimensionDisplayName;
        this.legendSet = legendSet;
        this.programStage = programStage;
        this.filter = filter;
        this.valueType = valueType;
        this.optionSet = optionSet;
    }

    // TODO aggregationType in constructors

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public DimensionalObject instance()
    {
        BaseDimensionalObject object = new BaseDimensionalObject( this.uid,
            this.dimensionType, this.dimensionName, this.dimensionDisplayName, this.items, this.allItems );

        object.legendSet = this.legendSet;
        object.aggregationType = this.aggregationType;
        object.filter = this.filter;
        object.dataDimension = this.dataDimension;
        object.fixed = this.fixed;
        object.dimensionalKeywords = this.dimensionalKeywords;
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
    public boolean hasProgramStage()
    {
        return getProgramStage() != null;
    }

    @Override
    public String getDimensionName()
    {
        return dimensionName != null ? dimensionName : uid;
    }

    @Override
    public String getDisplayProperty( DisplayProperty displayProperty )
    {
        if ( SHORTNAME.equals( displayProperty ) && getDisplayShortName() != null )
        {
            return getDisplayShortName();
        }
        else
        {
            return getDimensionDisplayName();
        }
    }

    @Override
    public AnalyticsType getAnalyticsType()
    {
        return DimensionType.PROGRAM_ATTRIBUTE.equals( dimensionType ) ||
            DimensionType.PROGRAM_DATA_ELEMENT.equals( dimensionType ) ? AnalyticsType.EVENT : AnalyticsType.AGGREGATE;
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

    @Override
    public String getKey()
    {
        QueryKey key = new QueryKey();

        key.add( "dimension", getDimension() );
        getItems().forEach( e -> key.add( "item", e.getDimensionItem() ) );

        return key
            .add( "allItems", allItems )
            .addIgnoreNull( "legendSet", legendSet )
            .addIgnoreNull( "aggregationType", aggregationType )
            .addIgnoreNull( "filter", filter ).asPlainKey();
    }

    // --------------------------------------------------------------------------
    // Getters and setters
    // --------------------------------------------------------------------------

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

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataDimensionType getDataDimensionType()
    {
        return dataDimensionType;
    }

    public void setDataDimensionType( DataDimensionType dataDimensionType )
    {
        this.dataDimensionType = dataDimensionType;
    }

    public void setDimensionName( String dimensionName )
    {
        this.dimensionName = dimensionName;
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
    public String getDimensionDisplayName()
    {
        return dimensionDisplayName;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return valueType;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OptionSet getOptionSet()
    {
        return optionSet;
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
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
    @JsonIgnore
    public boolean isFixed()
    {
        return fixed;
    }

    public void setFixed( boolean fixed )
    {
        this.fixed = fixed;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DimensionItemKeywords getDimensionItemKeywords()
    {
        return this.dimensionalKeywords;
    }

    public void setDimensionalKeywords( DimensionItemKeywords dimensionalKeywords )
    {
        this.dimensionalKeywords = dimensionalKeywords;
    }

    @Override
    public String toString()
    {
        List<String> itemStr = items.stream().map( item -> MoreObjects.toStringHelper( DimensionalItemObject.class )
            .add( "uid", item.getUid() )
            .add( "name", item.getName() )
            .toString() )
            .collect( Collectors.toList() );

        return MoreObjects.toStringHelper( this )
            .add( "dimension", uid )
            .add( "type", dimensionType )
            .add( "dimension display name", dimensionDisplayName )
            .add( "dimension value type", valueType )
            .add( "items", itemStr )
            .toString();
    }
}
