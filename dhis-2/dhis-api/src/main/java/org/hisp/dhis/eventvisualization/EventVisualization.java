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
package org.hisp.dhis.eventvisualization;

import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.common.AnalyticsType.EVENT;
import static org.hisp.dhis.common.DimensionalObjectUtils.TITLE_ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getPrettyFilter;
import static org.hisp.dhis.common.DimensionalObjectUtils.setDimensionItemsForFilters;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.common.IdentifiableObjectUtils.join;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;
import static org.hisp.dhis.schema.PropertyType.CONSTANT;
import static org.hisp.dhis.schema.annotation.Property.Value.TRUE;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.EventDataType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.FontSize;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.HideEmptyItemStrategy;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.RegressionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendDisplayStrategy;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "eventVisualization", namespace = DXF_2_0 )
public class EventVisualization extends BaseAnalyticalObject
    implements MetadataObject, EventAnalyticalObject
{
    protected String domainAxisLabel;

    protected String rangeAxisLabel;

    protected EventVisualizationType type;

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

    // -------------------------------------------------------------------------
    // Dimensional properties
    // -------------------------------------------------------------------------

    private List<String> filterDimensions = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    protected transient I18nFormat format;

    protected transient List<Period> relativePeriods = new ArrayList<>();

    protected transient User relativeUser;

    protected transient List<OrganisationUnit> organisationUnitsAtLevel = new ArrayList<>();

    protected transient List<OrganisationUnit> organisationUnitsInGroups = new ArrayList<>();

    protected transient Grid dataItemGrid = null;

    /**
     * Program. Required.
     */
    private Program program;

    /**
     * Program stage.
     */
    private ProgramStage programStage;

    /**
     * Data element value dimension.
     */
    private DataElement dataElementValueDimension;

    /**
     * Attribute value dimension.
     */
    private TrackedEntityAttribute attributeValueDimension;

    /**
     * Dimensions to crosstabulate / use as columns.
     */
    private List<String> columnDimensions = new ArrayList<>();

    /**
     * Dimensions to use as rows.
     */
    private List<String> rowDimensions = new ArrayList<>();

    /**
     * The non-typed dimensions for this event visualization.
     */
    private List<SimpleDimension> simpleDimensions = new ArrayList<>();

    /**
     * Indicates output type.
     */
    private EventOutputType outputType;

    /**
     * Indicates whether to collapse all data dimensions into a single
     * dimension.
     */
    private boolean collapseDataDimensions;

    /**
     * Indicates whether to hide n/a data.
     */
    private boolean hideNaData;

    /**
     * Indicates whether this is a legacy row (EventChart or EventReport).
     */
    private boolean legacy;

    /**
     * The program status.
     */
    private ProgramStatus programStatus;

    /**
     * The event status.
     */
    private EventStatus eventStatus;

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    /**
     * Value dimension.
     */
    private transient DimensionalItemObject value;

    private EventDataType dataType;

    /**
     * Indicates rendering of sub-totals for the table.
     */
    private boolean rowTotals;

    /**
     * Indicates rendering of sub-totals for the table.
     */
    private boolean colTotals;

    /**
     * Indicates rendering of row sub-totals for the table.
     */
    private boolean rowSubTotals;

    /**
     * Indicates rendering of column sub-totals for the table.
     */
    private boolean colSubTotals;

    /**
     * Indicates rendering of empty rows for the table.
     */
    private boolean hideEmptyRows;

    /**
     * Indicates rendering of empty rows for the table.
     */
    private boolean showHierarchy;

    /**
     * The display density of the text in the table.
     */
    private DisplayDensity displayDensity;

    /**
     * The font size of the text in the table.
     */
    private FontSize fontSize;

    /**
     * The font size of the text in the table.
     */
    private boolean showDimensionLabels;

    public EventVisualization()
    {
    }

    public EventVisualization( final String name )
    {
        super.name = name;
    }

    @Override
    public void init( final User user, final Date date, final OrganisationUnit organisationUnit,
        final List<OrganisationUnit> organisationUnitsAtLevel, final List<OrganisationUnit> organisationUnitsInGroups,
        final I18nFormat format )
    {
        this.relativeUser = user;
        this.format = format;
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
        final List<String> titleItems = new ArrayList<>();

        for ( final String filter : filterDimensions )
        {
            final DimensionalObject object = getDimensionalObject( filter, relativePeriodDate, relativeUser, true,
                organisationUnitsAtLevel, organisationUnitsInGroups, format );

            if ( object != null )
            {
                final String item = join( object.getItems() );
                final String prettyFilter = getPrettyFilter( object.getFilter() );

                if ( item != null )
                {
                    titleItems.add( item );
                }

                if ( prettyFilter != null )
                {
                    titleItems.add( prettyFilter );
                }
            }
        }

        return join( titleItems, TITLE_ITEM_SEP );
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
        value = null;
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
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getDomainAxisLabel()
    {
        return domainAxisLabel;
    }

    public void setDomainAxisLabel( String domainAxisLabel )
    {
        this.domainAxisLabel = domainAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Translatable( propertyName = "domainAxisLabel" )
    public String getDisplayDomainAxisLabel()
    {
        return getTranslation( "domainAxisLabel", getDomainAxisLabel() );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getRangeAxisLabel()
    {
        return rangeAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Translatable( propertyName = "rangeAxisLabel" )
    public String getDisplayRangeAxisLabel()
    {
        return getTranslation( "rangeAxisLabel", getRangeAxisLabel() );
    }

    public void setRangeAxisLabel( String rangeAxisLabel )
    {
        this.rangeAxisLabel = rangeAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Property( value = CONSTANT, required = TRUE )
    public EventVisualizationType getType()
    {
        return type;
    }

    public void setType( EventVisualizationType type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideLegend()
    {
        return hideLegend;
    }

    public void setHideLegend( boolean hideLegend )
    {
        this.hideLegend = hideLegend;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isNoSpaceBetweenColumns()
    {
        return noSpaceBetweenColumns;
    }

    public void setNoSpaceBetweenColumns( boolean noSpaceBetweenColumns )
    {
        this.noSpaceBetweenColumns = noSpaceBetweenColumns;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public RegressionType getRegressionType()
    {
        return regressionType;
    }

    public void setRegressionType( RegressionType regressionType )
    {
        this.regressionType = regressionType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getTargetLineValue()
    {
        return targetLineValue;
    }

    public void setTargetLineValue( Double targetLineValue )
    {
        this.targetLineValue = targetLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getTargetLineLabel()
    {
        return targetLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Translatable( propertyName = "targetLineLabel" )
    public String getDisplayTargetLineLabel()
    {
        return getTranslation( "targetLineLabel", getTargetLineLabel() );
    }

    public void setTargetLineLabel( String targetLineLabel )
    {
        this.targetLineLabel = targetLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getBaseLineValue()
    {
        return baseLineValue;
    }

    public void setBaseLineValue( Double baseLineValue )
    {
        this.baseLineValue = baseLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getBaseLineLabel()
    {
        return baseLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Translatable( propertyName = "baseLineLabel" )
    public String getDisplayBaseLineLabel()
    {
        return getTranslation( "baseLineLabel", getBaseLineLabel() );
    }

    public void setBaseLineLabel( String baseLineLabel )
    {
        this.baseLineLabel = baseLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowData()
    {
        return showData;
    }

    public void setShowData( boolean showData )
    {
        this.showData = showData;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public HideEmptyItemStrategy getHideEmptyRowItems()
    {
        return hideEmptyRowItems;
    }

    public void setHideEmptyRowItems( HideEmptyItemStrategy hideEmptyRowItems )
    {
        this.hideEmptyRowItems = hideEmptyRowItems;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isPercentStackedValues()
    {
        return percentStackedValues;
    }

    public void setPercentStackedValues( boolean percentStackedValues )
    {
        this.percentStackedValues = percentStackedValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isCumulativeValues()
    {
        return cumulativeValues;
    }

    public void setCumulativeValues( boolean cumulativeValues )
    {
        this.cumulativeValues = cumulativeValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getRangeAxisMaxValue()
    {
        return rangeAxisMaxValue;
    }

    public void setRangeAxisMaxValue( Double rangeAxisMaxValue )
    {
        this.rangeAxisMaxValue = rangeAxisMaxValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Integer getRangeAxisSteps()
    {
        return rangeAxisSteps;
    }

    public void setRangeAxisSteps( Integer rangeAxisSteps )
    {
        this.rangeAxisSteps = rangeAxisSteps;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public LegendDisplayStrategy getLegendDisplayStrategy()
    {
        return legendDisplayStrategy;
    }

    public void setLegendDisplayStrategy( LegendDisplayStrategy legendDisplayStrategy )
    {
        this.legendDisplayStrategy = legendDisplayStrategy;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "filterDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "filterDimension", namespace = DXF_2_0 )
    public List<String> getFilterDimensions()
    {
        return filterDimensions;
    }

    public void setFilterDimensions( List<String> filterDimensions )
    {
        this.filterDimensions = filterDimensions;
    }

    // -------------------------------------------------------------------------
    // AnalyticalObject
    // -------------------------------------------------------------------------

    /**
     * Some EventVisualization's may not have columnDimensions.
     *
     * PIE, GAUGE and others don't not have rowDimensions.
     */
    @Override
    public void populateAnalyticalProperties()
    {
        super.populateDimensions( columnDimensions, columns, COLUMN, this );
        super.populateDimensions( rowDimensions, rows, ROW, this );
        super.populateDimensions( filterDimensions, filters, FILTER, this );

        value = firstNonNull( dataElementValueDimension, attributeValueDimension );
    }

    public void associateSimpleDimensions()
    {
        new SimpleDimensionHandler( this ).associateDimensions();
    }

    public List<DimensionalItemObject> series()
    {
        final String series = columnDimensions.get( 0 );
        return getItems( series );
    }

    public List<DimensionalItemObject> category()
    {
        final String category = rowDimensions.get( 0 );
        return getItems( category );
    }

    private List<DimensionalItemObject> getItems( final String dimension )
    {
        final DimensionalObject object = getDimensionalObject( dimension, relativePeriodDate, relativeUser, true,
            organisationUnitsAtLevel, organisationUnitsInGroups, format );

        setDimensionItemsForFilters( object, dataItemGrid, true );

        return object != null ? object.getItems() : null;
    }

    public AnalyticsType getAnalyticsType()
    {
        return EVENT;
    }

    // -------------------------------------------------------------------------
    // Getters and setters properties
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public DataElement getDataElementValueDimension()
    {
        return dataElementValueDimension;
    }

    @Override
    public void setDataElementValueDimension( DataElement dataElementValueDimension )
    {
        this.dataElementValueDimension = dataElementValueDimension;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public TrackedEntityAttribute getAttributeValueDimension()
    {
        return attributeValueDimension;
    }

    @Override
    public void setAttributeValueDimension( TrackedEntityAttribute attributeValueDimension )
    {
        this.attributeValueDimension = attributeValueDimension;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "columnDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "columnDimension", namespace = DXF_2_0 )
    public List<String> getColumnDimensions()
    {
        return columnDimensions;
    }

    public void setColumnDimensions( List<String> columnDimensions )
    {
        this.columnDimensions = columnDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "rowDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "rowDimension", namespace = DXF_2_0 )
    public List<String> getRowDimensions()
    {
        return rowDimensions;
    }

    public void setRowDimensions( List<String> rowDimensions )
    {
        this.rowDimensions = rowDimensions;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public List<SimpleDimension> getSimpleDimensions()
    {
        return simpleDimensions;
    }

    public void setSimpleDimensions( final List<SimpleDimension> simpleDimensions )
    {
        this.simpleDimensions = simpleDimensions;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public EventOutputType getOutputType()
    {
        return outputType;
    }

    public void setOutputType( EventOutputType outputType )
    {
        this.outputType = outputType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public void setCollapseDataDimensions( boolean collapseDataDimensions )
    {
        this.collapseDataDimensions = collapseDataDimensions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public void setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public void setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideNaData()
    {
        return hideNaData;
    }

    public void setHideNaData( boolean hideNaData )
    {
        this.hideNaData = hideNaData;
    }

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonDeserialize( as = BaseDimensionalItemObject.class )
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public DimensionalItemObject getValue()
    {
        return value;
    }

    public void setValue( DimensionalItemObject value )
    {
        this.value = value;
    }

    /**
     * This attribute and its accessors were replaced by "type", but will remain
     * here for backward-compatibility with EventReport.
     *
     * @return
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public EventDataType getDataType()
    {
        return dataType;
    }

    public void setDataType( EventDataType dataType )
    {
        this.dataType = dataType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isRowTotals()
    {
        return rowTotals;
    }

    public void setRowTotals( boolean rowTotals )
    {
        this.rowTotals = rowTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isColTotals()
    {
        return colTotals;
    }

    public void setColTotals( boolean colTotals )
    {
        this.colTotals = colTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isRowSubTotals()
    {
        return rowSubTotals;
    }

    public void setRowSubTotals( boolean rowSubTotals )
    {
        this.rowSubTotals = rowSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isColSubTotals()
    {
        return colSubTotals;
    }

    public void setColSubTotals( boolean colSubTotals )
    {
        this.colSubTotals = colSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }

    public void setHideEmptyRows( boolean hideEmptyRows )
    {
        this.hideEmptyRows = hideEmptyRows;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public void setShowHierarchy( boolean showHierarchy )
    {
        this.showHierarchy = showHierarchy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public DisplayDensity getDisplayDensity()
    {
        return displayDensity;
    }

    public void setDisplayDensity( DisplayDensity displayDensity )
    {
        this.displayDensity = displayDensity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontSize getFontSize()
    {
        return fontSize;
    }

    public void setFontSize( FontSize fontSize )
    {
        this.fontSize = fontSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowDimensionLabels()
    {
        return showDimensionLabels;
    }

    public void setShowDimensionLabels( boolean showDimensionLabels )
    {
        this.showDimensionLabels = showDimensionLabels;
    }

    public boolean isLegacy()
    {
        return legacy;
    }

    public void setLegacy( final boolean legacy )
    {
        this.legacy = legacy;
    }
}
