package org.hisp.dhis.indicator;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "indicator", namespace = DxfNamespaces.DXF_2_0 )
public class Indicator
    extends BaseDimensionalItemObject
{
    private boolean annualized;

    /**
     * Number of decimals to use for indicator value, null implies default.
     */
    private Integer decimals;

    private IndicatorType indicatorType;

    private String numerator;

    private String numeratorDescription;

    private transient String explodedNumerator;

    private String denominator;

    private String denominatorDescription;

    private transient String explodedDenominator;

    private String url;

    private Set<IndicatorGroup> groups = new HashSet<>();

    private Set<DataSet> dataSets = new HashSet<>();

    public Indicator()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addIndicatorGroup( IndicatorGroup group )
    {
        groups.add( group );
        group.getMembers().add( this );
    }

    public void removeIndicatorGroup( IndicatorGroup group )
    {
        groups.remove( group );
        group.getMembers().remove( this );
    }

    public void updateIndicatorGroups( Set<IndicatorGroup> updates )
    {
        for ( IndicatorGroup group : new HashSet<>( groups ) )
        {
            if ( !updates.contains( group ) )
            {
                removeIndicatorGroup( group );
            }
        }

        for ( IndicatorGroup group : updates )
        {
            addIndicatorGroup( group );
        }
    }

    public void addDataSet( DataSet dataSet )
    {
        this.dataSets.add( dataSet );
        dataSet.getIndicators().add( this );
    }

    public void removeDataSet( DataSet dataSet )
    {
        this.dataSets.remove( dataSet );
        dataSet.getIndicators().remove( this );
    }

    public void removeAllAttributeValues()
    {
        attributeValues.clear();
    }

    public String getExplodedNumeratorFallback()
    {
        return explodedNumerator != null ? explodedNumerator : numerator;
    }

    public String getExplodedDenominatorFallback()
    {
        return explodedDenominator != null ? explodedDenominator : denominator;
    }

    public boolean hasDecimals()
    {
        return decimals != null && decimals >= 0;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.INDICATOR;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isAnnualized()
    {
        return annualized;
    }

    public void setAnnualized( boolean annualized )
    {
        this.annualized = annualized;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getDecimals()
    {
        return decimals;
    }

    public void setDecimals( Integer decimals )
    {
        this.decimals = decimals;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public IndicatorType getIndicatorType()
    {
        return indicatorType;
    }

    public void setIndicatorType( IndicatorType indicatorType )
    {
        this.indicatorType = indicatorType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNumerator()
    {
        return numerator;
    }

    public void setNumerator( String numerator )
    {
        this.numerator = numerator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNumeratorDescription()
    {
        return numeratorDescription;
    }

    public void setNumeratorDescription( String numeratorDescription )
    {
        this.numeratorDescription = numeratorDescription;
    }

    @JsonIgnore
    public String getExplodedNumerator()
    {
        return explodedNumerator;
    }

    public void setExplodedNumerator( String explodedNumerator )
    {
        this.explodedNumerator = explodedNumerator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDenominator()
    {
        return denominator;
    }

    public void setDenominator( String denominator )
    {
        this.denominator = denominator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDenominatorDescription()
    {
        return denominatorDescription;
    }

    public void setDenominatorDescription( String denominatorDescription )
    {
        this.denominatorDescription = denominatorDescription;
    }

    @JsonIgnore
    public String getExplodedDenominator()
    {
        return explodedDenominator;
    }

    public void setExplodedDenominator( String explodedDenominator )
    {
        this.explodedDenominator = explodedDenominator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.URL )
    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @JsonProperty( "indicatorGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "indicatorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicatorGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<IndicatorGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<IndicatorGroup> groups )
    {
        this.groups = groups;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataSet> getDataSets()
    {
        return dataSets;
    }

    public void setDataSets( Set<DataSet> dataSets )
    {
        this.dataSets = dataSets;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Indicator indicator = (Indicator) other;

            annualized = indicator.isAnnualized();

            if ( mergeMode.isReplace() )
            {
                decimals = indicator.getDecimals();
                denominator = indicator.getDenominator();
                denominatorDescription = indicator.getDenominatorDescription();
                numerator = indicator.getNumerator();
                numeratorDescription = indicator.getNumeratorDescription();
                explodedNumerator = indicator.getExplodedNumerator();
                explodedDenominator = indicator.getExplodedDenominator();
                indicatorType = indicator.getIndicatorType();
            }
            else if ( mergeMode.isMerge() )
            {
                decimals = indicator.getDecimals() == null ? decimals : indicator.getDecimals();
                denominator = indicator.getDenominator() == null ? denominator : indicator.getDenominator();
                denominatorDescription = indicator.getDenominatorDescription() == null ? denominatorDescription : indicator.getDenominatorDescription();
                numerator = indicator.getNumerator() == null ? numerator : indicator.getNumerator();
                numeratorDescription = indicator.getNumeratorDescription() == null ? numeratorDescription : indicator.getNumeratorDescription();
                explodedNumerator = indicator.getExplodedNumerator() == null ? explodedNumerator : indicator.getExplodedNumerator();
                explodedDenominator = indicator.getExplodedDenominator() == null ? explodedDenominator : indicator.getExplodedDenominator();
                indicatorType = indicator.getIndicatorType() == null ? indicatorType : indicator.getIndicatorType();
            }

            dataSets.clear();
            groups.clear();
        }
    }
}
