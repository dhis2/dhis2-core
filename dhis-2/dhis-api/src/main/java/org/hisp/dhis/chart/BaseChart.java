package org.hisp.dhis.chart;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.HideEmptyItemStrategy;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.RegressionType;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendDisplayStrategy;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.schema.annotation.Property.Value.TRUE;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "baseChart", namespace = DxfNamespaces.DXF_2_0 )
public abstract class BaseChart
    extends BaseAnalyticalObject
{
    protected String domainAxisLabel;

    protected String rangeAxisLabel;

    protected ChartType type;

    protected boolean hideLegend;

    protected boolean noSpaceBetweenColumns;

    protected RegressionType regressionType = RegressionType.NONE;

    protected Double targetLineValue;

    protected String targetLineLabel;

    protected Double baseLineValue;

    protected String baseLineLabel;

    protected boolean showData;

    protected HideEmptyItemStrategy hideEmptyRowItems = HideEmptyItemStrategy.NONE;

    protected boolean percentStackedValues;

    protected boolean cumulativeValues;

    protected Double rangeAxisMaxValue;

    protected Double rangeAxisMinValue;

    protected Integer rangeAxisSteps; // Minimum 1

    protected Integer rangeAxisDecimals;

    protected LegendSet legendSet;

    protected LegendDisplayStrategy legendDisplayStrategy;

    private List<String> yearlySeries = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Dimensional properties
    // -------------------------------------------------------------------------

    protected List<String> filterDimensions = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    protected transient I18nFormat format;

    protected transient List<Period> relativePeriods = new ArrayList<>();

    protected transient User relativeUser;

    protected transient List<OrganisationUnit> organisationUnitsAtLevel = new ArrayList<>();

    protected transient List<OrganisationUnit> organisationUnitsInGroups = new ArrayList<>();

    protected transient Grid dataItemGrid = null;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    public abstract List<DimensionalItemObject> series();

    public abstract List<DimensionalItemObject> category();

    public abstract AnalyticsType getAnalyticsType();

    protected abstract void clearTransientChartStateProperties();

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isType( ChartType type )
    {
        return this.type != null && this.type.equals( type );
    }

    public boolean isTargetLine()
    {
        return targetLineValue != null;
    }

    public boolean isBaseLine()
    {
        return baseLineValue != null;
    }

    public String generateTitle()
    {
        List<String> titleItems = new ArrayList<>();

        for ( String filter : filterDimensions )
        {
            DimensionalObject object = getDimensionalObject( filter, relativePeriodDate, relativeUser, true,
                organisationUnitsAtLevel, organisationUnitsInGroups, format );

            if ( object != null )
            {
                String item = IdentifiableObjectUtils.join( object.getItems() );
                String filt = DimensionalObjectUtils.getPrettyFilter( object.getFilter() );

                if ( item != null )
                {
                    titleItems.add( item );
                }

                if ( filt != null )
                {
                    titleItems.add( filt );
                }
            }
        }

        return join( titleItems, DimensionalObjectUtils.TITLE_ITEM_SEP );
    }

    public boolean isAnalyticsType( AnalyticsType type )
    {
        return getAnalyticsType().equals( type );
    }

    @Override
    protected void clearTransientStateProperties()
    {
        format = null;
        relativePeriods = new ArrayList<>();
        relativeUser = null;
        organisationUnitsAtLevel = new ArrayList<>();
        organisationUnitsInGroups = new ArrayList<>();
        dataItemGrid = null;

        clearTransientChartStateProperties();
    }

    public boolean isRegression()
    {
        return regressionType == null || RegressionType.NONE != regressionType;
    }

    // -------------------------------------------------------------------------
    // Getters and setters for transient properties
    // -------------------------------------------------------------------------

    @JsonIgnore
    public I18nFormat getFormat()
    {
        return format;
    }

    @JsonIgnore
    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    @JsonIgnore
    public List<Period> getRelativePeriods()
    {
        return relativePeriods;
    }

    @JsonIgnore
    public void setRelativePeriods( List<Period> relativePeriods )
    {
        this.relativePeriods = relativePeriods;
    }

    @JsonIgnore
    public Grid getDataItemGrid()
    {
        return dataItemGrid;
    }

    @JsonIgnore
    public void setDataItemGrid( Grid dataItemGrid )
    {
        this.dataItemGrid = dataItemGrid;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDomainAxisLabel()
    {
        return domainAxisLabel;
    }

    public void setDomainAxisLabel( String domainAxisLabel )
    {
        this.domainAxisLabel = domainAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getRangeAxisLabel()
    {
        return rangeAxisLabel;
    }

    public void setRangeAxisLabel( String rangeAxisLabel )
    {
        this.rangeAxisLabel = rangeAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( required = TRUE, value = PropertyType.CONSTANT )
    @PropertyRange( min = 1, max = 40)
    public ChartType getType()
    {
        return type;
    }

    public void setType( ChartType type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideLegend()
    {
        return hideLegend;
    }

    public void setHideLegend( boolean hideLegend )
    {
        this.hideLegend = hideLegend;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isNoSpaceBetweenColumns()
    {
        return noSpaceBetweenColumns;
    }

    public void setNoSpaceBetweenColumns( boolean noSpaceBetweenColumns )
    {
        this.noSpaceBetweenColumns = noSpaceBetweenColumns;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public RegressionType getRegressionType()
    {
        return regressionType;
    }

    public void setRegressionType( RegressionType regressionType )
    {
        this.regressionType = regressionType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getTargetLineValue()
    {
        return targetLineValue;
    }

    public void setTargetLineValue( Double targetLineValue )
    {
        this.targetLineValue = targetLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTargetLineLabel()
    {
        return targetLineLabel;
    }

    public void setTargetLineLabel( String targetLineLabel )
    {
        this.targetLineLabel = targetLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getBaseLineValue()
    {
        return baseLineValue;
    }

    public void setBaseLineValue( Double baseLineValue )
    {
        this.baseLineValue = baseLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getBaseLineLabel()
    {
        return baseLineLabel;
    }

    public void setBaseLineLabel( String baseLineLabel )
    {
        this.baseLineLabel = baseLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShowData()
    {
        return showData;
    }

    public void setShowData( boolean showData )
    {
        this.showData = showData;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public HideEmptyItemStrategy getHideEmptyRowItems()
    {
        return hideEmptyRowItems;
    }

    public void setHideEmptyRowItems( HideEmptyItemStrategy hideEmptyRowItems )
    {
        this.hideEmptyRowItems = hideEmptyRowItems;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isPercentStackedValues()
    {
        return percentStackedValues;
    }

    public void setPercentStackedValues( boolean percentStackedValues )
    {
        this.percentStackedValues = percentStackedValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCumulativeValues()
    {
        return cumulativeValues;
    }

    public void setCumulativeValues( boolean cumulativeValues )
    {
        this.cumulativeValues = cumulativeValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getRangeAxisMaxValue()
    {
        return rangeAxisMaxValue;
    }

    public void setRangeAxisMaxValue( Double rangeAxisMaxValue )
    {
        this.rangeAxisMaxValue = rangeAxisMaxValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = Integer.MIN_VALUE )
    public Double getRangeAxisMinValue()
    {
        return rangeAxisMinValue;
    }

    public void setRangeAxisMinValue( Double rangeAxisMinValue )
    {
        this.rangeAxisMinValue = rangeAxisMinValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRangeAxisSteps()
    {
        return rangeAxisSteps;
    }

    public void setRangeAxisSteps( Integer rangeAxisSteps )
    {
        this.rangeAxisSteps = rangeAxisSteps;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRangeAxisDecimals()
    {
        return rangeAxisDecimals;
    }

    public void setRangeAxisDecimals( Integer rangeAxisDecimals )
    {
        this.rangeAxisDecimals = rangeAxisDecimals;
    }

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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendDisplayStrategy getLegendDisplayStrategy()
    {
        return legendDisplayStrategy;
    }

    public void setLegendDisplayStrategy( LegendDisplayStrategy legendDisplayStrategy )
    {
        this.legendDisplayStrategy = legendDisplayStrategy;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "yearlySeries", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "yearlySerie", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getYearlySeries()
    {
        return yearlySeries;
    }

    public void setYearlySeries( List<String> yearlySeries )
    {
        this.yearlySeries = yearlySeries;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "filterDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "filterDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getFilterDimensions()
    {
        return filterDimensions;
    }

    public void setFilterDimensions( List<String> filterDimensions )
    {
        this.filterDimensions = filterDimensions;
    }
}
