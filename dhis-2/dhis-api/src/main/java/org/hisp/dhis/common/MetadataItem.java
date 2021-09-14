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

import java.io.Serializable;
import java.util.Date;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Item part of meta data analytics response.
 *
 * @author Lars Helge Overland
 */
public class MetadataItem
    implements Serializable
{
    private String uid;

    private String code;

    private String name;

    private String description;

    private String legendSet;

    private DimensionType dimensionType;

    private DimensionItemType dimensionItemType;

    private ValueType valueType;

    private AggregationType aggregationType;

    private TotalAggregationType totalAggregationType;

    private IndicatorType indicatorType;

    private Date startDate;

    private Date endDate;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public MetadataItem( String name )
    {
        this.name = name;
    }

    public MetadataItem( String name, String uid, String code )
    {
        this.name = name;
        this.uid = uid;
        this.code = code;
    }

    public MetadataItem( String name, DimensionalItemObject dimensionalItemObject )
    {
        this.name = name;
        setDataItem( dimensionalItemObject );
    }

    public MetadataItem( String name, DimensionalObject dimensionalObject )
    {
        this.name = name;
        setDataItem( dimensionalObject );
    }

    public MetadataItem( String name, Program program )
    {
        this.name = name;

        if ( program == null )
        {
            return;
        }

        this.uid = program.getUid();
        this.code = program.getCode();
        this.description = program.getDescription();
    }

    public MetadataItem( String name, ProgramStage programStage )
    {
        this.name = name;

        if ( programStage == null )
        {
            return;
        }

        this.uid = programStage.getUid();
        this.code = programStage.getCode();
        this.description = programStage.getDescription();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    private void setDataItem( DimensionalItemObject dimensionalItemObject )
    {
        if ( dimensionalItemObject == null )
        {
            return;
        }

        this.uid = dimensionalItemObject.getUid();
        this.code = dimensionalItemObject.getCode();
        this.description = dimensionalItemObject.getDescription();
        this.dimensionItemType = dimensionalItemObject.getDimensionItemType();
        this.valueType = ValueType.NUMBER; // Default value
        this.aggregationType = dimensionalItemObject.getAggregationType();
        this.totalAggregationType = dimensionalItemObject.getTotalAggregationType();

        if ( dimensionalItemObject.hasLegendSet() )
        {
            this.legendSet = dimensionalItemObject.getLegendSet().getUid();
        }

        // TODO common interface

        if ( dimensionalItemObject instanceof DataElement )
        {
            DataElement dataElement = (DataElement) dimensionalItemObject;
            this.valueType = dataElement.getValueType().toSimplifiedValueType();
        }

        if ( dimensionalItemObject instanceof TrackedEntityAttribute )
        {
            TrackedEntityAttribute attribute = (TrackedEntityAttribute) dimensionalItemObject;
            this.valueType = attribute.getValueType().toSimplifiedValueType();
        }

        // TODO introduce start/end date marker interface instead

        if ( dimensionalItemObject instanceof Period )
        {
            Period period = (Period) dimensionalItemObject;
            this.startDate = period.getStartDate();
            this.endDate = period.getEndDate();
        }

        if ( dimensionalItemObject instanceof Indicator )
        {
            Indicator indicator = (Indicator) dimensionalItemObject;

            if ( indicator.getIndicatorType() != null )
            {
                this.indicatorType = indicator.getIndicatorType();
            }
        }
    }

    private void setDataItem( DimensionalObject dimensionalObject )
    {
        if ( dimensionalObject == null )
        {
            return;
        }

        this.uid = dimensionalObject.getUid();
        this.code = dimensionalObject.getCode();
        this.dimensionType = dimensionalObject.getDimensionType();
        this.description = dimensionalObject.getDescription();
        this.aggregationType = dimensionalObject.getAggregationType();
    }

    // -------------------------------------------------------------------------
    // Get and set
    // -------------------------------------------------------------------------

    @JsonProperty
    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    @JsonProperty
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    public String getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( String legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    public DimensionType getDimensionType()
    {
        return dimensionType;
    }

    public void setDimensionType( DimensionType type )
    {
        this.dimensionType = type;
    }

    @JsonProperty
    public DimensionItemType getDimensionItemType()
    {
        return dimensionItemType;
    }

    public void setDimensionItemType( DimensionItemType dimensionItemType )
    {
        this.dimensionItemType = dimensionItemType;
    }

    @JsonProperty
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType itemSpecificType )
    {
        this.aggregationType = itemSpecificType;
    }

    @JsonProperty
    @JsonSerialize( using = IndicatorTypeSerializer.class )
    public IndicatorType getIndicatorType()
    {
        return indicatorType;
    }

    public void setIndicatorType( IndicatorType indicatorType )
    {
        this.indicatorType = indicatorType;
    }

    @JsonProperty
    public TotalAggregationType getTotalAggregationType()
    {
        return totalAggregationType;
    }

    public void setTotalAggregationType( TotalAggregationType totalAggregationType )
    {
        this.totalAggregationType = totalAggregationType;
    }

    @JsonProperty
    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    @JsonProperty
    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }
}
