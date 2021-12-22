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
package org.hisp.dhis.eventreport;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.EventDataType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.FontSize;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.eventvisualization.SimpleDimension;
import org.hisp.dhis.eventvisualization.SimpleDimensionHandler;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @deprecated THIS IS BEING DEPRECATED IN FAVOUR OF THE EventVisualization
 *             MODEL. WE SHOULD AVOID CHANGES ON THIS CLASS AS MUCH AS POSSIBLE.
 *             NEW FEATURES SHOULD BE ADDED ON TOP OF EventVisualization.
 *
 * @author Jan Henrik Overland
 */
@Deprecated
@JacksonXmlRootElement( localName = "eventReport", namespace = DxfNamespaces.DXF_2_0 )
public class EventReport
    extends BaseAnalyticalObject
    implements EventAnalyticalObject, MetadataObject
{
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
     * Type of data, can be aggregated values and individual cases.
     */
    private EventDataType dataType;

    /**
     * Dimensions to crosstabulate / use as columns.
     */
    private List<String> columnDimensions = new ArrayList<>();

    /**
     * Dimensions to use as rows.
     */
    private List<String> rowDimensions = new ArrayList<>();

    /**
     * Dimensions to use as filter.
     */
    private List<String> filterDimensions = new ArrayList<>();

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
     * Indicates output type.
     */
    private EventOutputType outputType;

    /**
     * Indicates whether to collapse all data dimensions into a single
     * dimension.
     */
    private boolean collapseDataDimensions;

    /**
     * Indicates rendering of empty rows for the table.
     */
    private boolean hideEmptyRows;

    /**
     * Indicates rendering of empty rows for the table.
     */
    private boolean hideNaData;

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
     * The program status.
     */
    private ProgramStatus programStatus;

    /**
     * The event status.
     */
    private EventStatus eventStatus;

    /**
     * The font size of the text in the table.
     */
    private boolean showDimensionLabels;

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    /**
     * Value dimension.
     */
    private transient DimensionalItemObject value;

    /**
     * The non-typed dimensions for this event report.
     */
    private List<SimpleDimension> simpleDimensions = new ArrayList<>();

    // -------------------------------------------------------------------------
    // BACKWARD compatible attributes.
    // They are not exposed and should be always false for EventChart.
    // Needed to enable backward compatibility with EventVisualization.
    // Cannot be removed until EventReport if fully deprecated.
    // -------------------------------------------------------------------------

    private boolean hideLegend;

    private boolean noSpaceBetweenColumns;

    private boolean showData;

    private boolean percentStackedValues;

    private boolean cumulativeValues;

    /**
     * Default to true, as this entity is always legacy.
     */
    private boolean legacy = true;

    private EventVisualizationType type;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public EventReport()
    {
    }

    public EventReport( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // AnalyticalObject
    // -------------------------------------------------------------------------

    @Override
    public void init( User user, Date date, OrganisationUnit organisationUnit,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups,
        I18nFormat format )
    {
    }

    @Override
    public void populateAnalyticalProperties()
    {
        populateDimensions( columnDimensions, columns, COLUMN, this );
        populateDimensions( rowDimensions, rows, ROW, this );
        populateDimensions( filterDimensions, filters, FILTER, this );

        value = ObjectUtils.firstNonNull( dataElementValueDimension, attributeValueDimension );
    }

    @Override
    protected void clearTransientStateProperties()
    {
        value = null;
    }

    public void associateSimpleDimensions()
    {
        new SimpleDimensionHandler( this ).associateDimensions();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventDataType getDataType()
    {
        return dataType;
    }

    public void setDataType( EventDataType dataType )
    {
        this.dataType = dataType;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "columnDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "columnDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getColumnDimensions()
    {
        return columnDimensions;
    }

    public void setColumnDimensions( List<String> columnDimensions )
    {
        this.columnDimensions = columnDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "rowDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "rowDimension", namespace = DxfNamespaces.DXF_2_0 )
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRowTotals()
    {
        return rowTotals;
    }

    public void setRowTotals( boolean rowTotals )
    {
        this.rowTotals = rowTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isColTotals()
    {
        return colTotals;
    }

    public void setColTotals( boolean colTotals )
    {
        this.colTotals = colTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRowSubTotals()
    {
        return rowSubTotals;
    }

    public void setRowSubTotals( boolean rowSubTotals )
    {
        this.rowSubTotals = rowSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isColSubTotals()
    {
        return colSubTotals;
    }

    public void setColSubTotals( boolean colSubTotals )
    {
        this.colSubTotals = colSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }

    public void setHideEmptyRows( boolean hideEmptyRows )
    {
        this.hideEmptyRows = hideEmptyRows;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideNaData()
    {
        return hideNaData;
    }

    public void setHideNaData( boolean hideNaData )
    {
        this.hideNaData = hideNaData;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventOutputType getOutputType()
    {
        return outputType;
    }

    public void setOutputType( EventOutputType outputType )
    {
        this.outputType = outputType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCollapseDataDimensions()
    {
        return collapseDataDimensions;
    }

    public void setCollapseDataDimensions( boolean collapseDataDimensions )
    {
        this.collapseDataDimensions = collapseDataDimensions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public void setShowHierarchy( boolean showHierarchy )
    {
        this.showHierarchy = showHierarchy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DisplayDensity getDisplayDensity()
    {
        return displayDensity;
    }

    public void setDisplayDensity( DisplayDensity displayDensity )
    {
        this.displayDensity = displayDensity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FontSize getFontSize()
    {
        return fontSize;
    }

    public void setFontSize( FontSize fontSize )
    {
        this.fontSize = fontSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public void setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public void setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShowDimensionLabels()
    {
        return showDimensionLabels;
    }

    public void setShowDimensionLabels( boolean showDimensionLabels )
    {
        this.showDimensionLabels = showDimensionLabels;
    }

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonDeserialize( as = BaseDimensionalItemObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DimensionalItemObject getValue()
    {
        return value;
    }

    public void setValue( DimensionalItemObject value )
    {
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // BACKWARD compatible attributes.
    // They are not exposed and should be always be set.
    // Needed to enable backward compatibility with EventVisualization.
    // Cannot be removed until EventReport if fully deprecated.
    // The rule to populate "type" is:
    // if dataType == EVENTS then "type" = LINE_LIST
    // if dataType == AGGREGATED_VALUES then "type" = PIVOT_TABLE
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventVisualizationType getType()
    {
        return type;
    }

    public void setType( EventVisualizationType type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLegacy()
    {
        return legacy;
    }

    public void setLegacy( final boolean legacy )
    {
        this.legacy = legacy;
    }
}
