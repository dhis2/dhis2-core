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

import static org.apache.commons.lang3.StringUtils.appendIfMissing;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
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
@Getter
@Setter
public class MetadataItem
    implements Serializable
{
    @JsonProperty
    private String uid;

    @JsonProperty
    private String code;

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    @JsonProperty
    private String legendSet;

    @JsonProperty
    private DimensionType dimensionType;

    @JsonProperty
    private DimensionItemType dimensionItemType;

    @JsonProperty
    private ValueType valueType;

    @JsonProperty
    private AggregationType aggregationType;

    @JsonProperty
    private TotalAggregationType totalAggregationType;

    @JsonProperty
    @JsonSerialize( using = IndicatorTypeSerializer.class )
    private IndicatorType indicatorType;

    @JsonProperty
    private Date startDate;

    @JsonProperty
    private Date endDate;

    @JsonProperty
    private String expression;

    @JsonProperty
    private ObjectStyle style;

    private transient String serverBaseUrl;

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

    public MetadataItem( String name, String serverBaseUrl, DimensionalItemObject dimensionalItemObject )
    {
        this.name = name;
        this.serverBaseUrl = serverBaseUrl;
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
        else if ( dimensionalItemObject instanceof DataElementOperand )
        {
            DataElementOperand operand = (DataElementOperand) dimensionalItemObject;
            this.valueType = operand.getValueType().toSimplifiedValueType();
        }
        else if ( dimensionalItemObject instanceof TrackedEntityAttribute )
        {
            TrackedEntityAttribute attribute = (TrackedEntityAttribute) dimensionalItemObject;
            this.valueType = attribute.getValueType().toSimplifiedValueType();
        }
        else if ( dimensionalItemObject instanceof Period )
        {
            Period period = (Period) dimensionalItemObject;
            this.startDate = period.getStartDate();
            this.endDate = period.getEndDate();
        }
        else if ( dimensionalItemObject instanceof Indicator )
        {
            Indicator indicator = (Indicator) dimensionalItemObject;

            if ( indicator.getIndicatorType() != null )
            {
                this.indicatorType = HibernateProxyUtils.unproxy( indicator.getIndicatorType() );
            }

            if ( indicator.getStyle() != null )
            {
                // Override icon path.
                ObjectStyle indicatorStyle = indicator.getStyle();
                indicator.getStyle().setIcon( getFullIconUrl( indicatorStyle.getIcon() ) );

                this.style = indicator.getStyle();
            }
        }
        else if ( dimensionalItemObject instanceof ExpressionDimensionItem )
        {
            ExpressionDimensionItem expressionDimensionItem = (ExpressionDimensionItem) dimensionalItemObject;

            this.expression = expressionDimensionItem.getExpression();
        }
    }

    /**
     * It returns the full icon URL for the given icon name. The full URL is
     * based on the Icons' API. See the controller
     * {@link org.hisp.dhis.webapi.controller.IconController} for more details.
     *
     * @param iconName the icon name.
     * @return the icon's full path.
     */
    private String getFullIconUrl( String iconName )
    {
        String absoluteUrl = appendIfMissing( serverBaseUrl, "/" );
        return absoluteUrl + "api/icons/" + iconName + "/icon.svg";
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
}
