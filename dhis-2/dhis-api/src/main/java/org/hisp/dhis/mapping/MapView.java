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
package org.hisp.dhis.mapping;

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.eventvisualization.SimpleDimension;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;

/**
 * For analytical data, organisation units and indicators/data elements are
 * dimensions, and period is filter.
 *
 * @author Jan Henrik Overland
 */
@JacksonXmlRootElement( localName = "mapView", namespace = DxfNamespaces.DXF_2_0 )
public class MapView
    extends BaseAnalyticalObject
    implements EventAnalyticalObject, MetadataObject, EmbeddedObject
{
    public static final String LAYER_BOUNDARY = "boundary";

    public static final String LAYER_FACILITY = "facility";

    public static final String LAYER_SYMBOL = "symbol";

    public static final String LAYER_EVENT = "event";

    public static final String LAYER_THEMATIC1 = "thematic1";

    public static final String LAYER_THEMATIC2 = "thematic2";

    public static final String LAYER_THEMATIC3 = "thematic3";

    public static final String LAYER_THEMATIC4 = "thematic4";

    public static final String LAYER_EARTH_ENGINE = "earthEngine";

    public static final Integer METHOD_EQUAL_INTERVALS = 2;

    public static final Integer METHOD_EQUAL_COUNTS = 3;

    public static final ImmutableList<String> DATA_LAYERS = ImmutableList.<String> builder().add(
        LAYER_THEMATIC1, LAYER_THEMATIC2, LAYER_THEMATIC3, LAYER_THEMATIC4 ).build();

    private Program program;

    private ProgramStage programStage;

    private Date startDate;

    private Date endDate;

    /**
     * Tracked entity instance layer.
     */
    private TrackedEntityType trackedEntityType;

    private ProgramStatus programStatus;

    private Boolean followUp;

    private OrganisationUnitSelectionMode organisationUnitSelectionMode;

    /**
     * Dimensions to use as columns.
     */
    private List<String> columnDimensions = new ArrayList<>();

    /**
     * Dimensions to use as filter.
     */
    private List<String> filterDimensions = new ArrayList<>();

    private String layer;

    private Integer method;

    private Integer classes;

    private String colorLow;

    private String colorHigh;

    /**
     * Comma-separated value of hex colors.
     */
    private String colorScale;

    private LegendSet legendSet;

    /**
     * Color in hex format to use for features with no corresponding data. Must
     * be exactly 7 characters.
     */
    private String noDataColor;

    /**
     * Color in hex format.
     */
    private String organisationUnitColor;

    private Integer radiusLow;

    private Integer radiusHigh;

    private Double opacity;

    private OrganisationUnitGroupSet organisationUnitGroupSet;

    private Integer areaRadius;

    private Boolean hidden;

    private Boolean labels;

    private String labelFontSize;

    private String labelFontWeight;

    private String labelFontStyle;

    private String labelFontColor;

    private boolean eventClustering;

    private String eventCoordinateField;

    private String eventPointColor;

    private int eventPointRadius;

    private MapViewRenderingStrategy renderingStrategy;

    private ThematicMapType thematicMapType;

    private EventStatus eventStatus;

    /**
     * General configuration property for JSON values used to store information
     * for layers with arbitrary configuration needs.
     */
    private String config;

    private Object styleDataItem;

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    private transient I18nFormat format;

    private transient String parentGraph;

    private transient int parentLevel;

    private transient List<OrganisationUnit> organisationUnitsAtLevel = new ArrayList<>();

    private transient List<OrganisationUnit> organisationUnitsInGroups = new ArrayList<>();

    public MapView()
    {
        this.renderingStrategy = MapViewRenderingStrategy.SINGLE;
    }

    public MapView( String layer )
    {
        this();
        this.layer = layer;
    }

    // -------------------------------------------------------------------------
    // AnalyticalObject
    // -------------------------------------------------------------------------

    @Override
    public void init( User user, Date date, OrganisationUnit organisationUnit,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups,
        I18nFormat format )
    {
        this.relativePeriodDate = date;
        this.relativeOrganisationUnit = organisationUnit;
        this.organisationUnitsAtLevel = organisationUnitsAtLevel;
        this.organisationUnitsInGroups = organisationUnitsInGroups;
        this.setCreatedBy( user );
    }

    /**
     * Populates analytical properties. Organisation unit dimension is fixed to
     * "rows" currently.
     */
    @Override
    public void populateAnalyticalProperties()
    {
        for ( final String column : columnDimensions )
        {
            final Optional<DimensionalObject> dimensionalObject = getDimensionalObject( column );
            if ( dimensionalObject.isPresent() )
            {
                columns.add( dimensionalObject.get() );
            }
        }

        final Optional<DimensionalObject> orgUnitDimension = getDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID );
        if ( orgUnitDimension.isPresent() )
        {
            rows.add( orgUnitDimension.get() );
        }

        for ( final String filter : filterDimensions )
        {
            final Optional<DimensionalObject> dimensionalObject = getDimensionalObject( filter );
            if ( dimensionalObject.isPresent() )
            {
                filters.add( dimensionalObject.get() );
            }
        }
    }

    @Override
    protected void clearTransientStateProperties()
    {
        format = null;
        parentGraph = null;
        parentLevel = 0;
        organisationUnitsAtLevel = new ArrayList<>();
        organisationUnitsInGroups = new ArrayList<>();
    }

    public List<OrganisationUnit> getAllOrganisationUnits()
    {
        DimensionalObject object = getDimensionalObject( ORGUNIT_DIM_ID, relativePeriodDate, getUser(), true,
            organisationUnitsAtLevel, organisationUnitsInGroups, format );

        return object != null ? DimensionalObjectUtils.asTypedList( object.getItems() ) : null;
    }

    public boolean hasLegendSet()
    {
        return legendSet != null;
    }

    public boolean hasColors()
    {
        return colorLow != null && !colorLow.trim().isEmpty() && colorHigh != null && !colorHigh.trim().isEmpty();
    }

    public boolean isDataLayer()
    {
        return DATA_LAYERS.contains( layer );
    }

    public boolean isEventLayer()
    {
        return LAYER_EVENT.equals( layer );
    }

    @Override
    public String getName()
    {
        if ( !dataDimensionItems.isEmpty() &&
            dataDimensionItems.get( 0 ) != null &&
            dataDimensionItems.get( 0 ).getDimensionalItemObject() != null )
        {
            return dataDimensionItems.get( 0 ).getDimensionalItemObject().getName();
        }

        return uid;
    }

    // -------------------------------------------------------------------------
    // EventAnalyticalObject
    // -------------------------------------------------------------------------

    @Override
    public EventOutputType getOutputType()
    {
        return EventOutputType.EVENT;
    }

    @Override
    public DimensionalItemObject getValue()
    {
        return null;
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

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getStartDate()
    {
        return startDate;
    }

    @Override
    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEndDate()
    {
        return endDate;
    }

    @Override
    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityType getTrackedEntityType()
    {
        return trackedEntityType;
    }

    public void setTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        this.trackedEntityType = trackedEntityType;
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
    public Boolean getFollowUp()
    {
        return followUp;
    }

    public void setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitSelectionMode getOrganisationUnitSelectionMode()
    {
        return organisationUnitSelectionMode;
    }

    public void setOrganisationUnitSelectionMode( OrganisationUnitSelectionMode organisationUnitSelectionMode )
    {
        this.organisationUnitSelectionMode = organisationUnitSelectionMode;
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

    /**
     * This method is not used/implemented in MapView.
     */
    @Override
    public List<SimpleDimension> getSimpleDimensions()
    {
        return Collections.emptyList();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLayer()
    {
        return layer;
    }

    public void setLayer( String layer )
    {
        this.layer = layer;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getMethod()
    {
        return method;
    }

    public void setMethod( Integer method )
    {
        this.method = method;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getClasses()
    {
        return classes;
    }

    public void setClasses( Integer classes )
    {
        this.classes = classes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getColorLow()
    {
        return colorLow;
    }

    public void setColorLow( String colorLow )
    {
        this.colorLow = colorLow;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getColorHigh()
    {
        return colorHigh;
    }

    public void setColorHigh( String colorHigh )
    {
        this.colorHigh = colorHigh;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getColorScale()
    {
        return colorScale;
    }

    public void setColorScale( String colorScale )
    {
        this.colorScale = colorScale;
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
    @PropertyRange( min = 7, max = 7 )
    public String getNoDataColor()
    {
        return noDataColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 7, max = 7 )
    public void setNoDataColor( String noDataColor )
    {
        this.noDataColor = noDataColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 7, max = 7 )
    public String getOrganisationUnitColor()
    {
        return organisationUnitColor;
    }

    public void setOrganisationUnitColor( String organisationUnitColor )
    {
        this.organisationUnitColor = organisationUnitColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRadiusLow()
    {
        return radiusLow;
    }

    public void setRadiusLow( Integer radiusLow )
    {
        this.radiusLow = radiusLow;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getRadiusHigh()
    {
        return radiusHigh;
    }

    public void setRadiusHigh( Integer radiusHigh )
    {
        this.radiusHigh = radiusHigh;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getOpacity()
    {
        return opacity;
    }

    public void setOpacity( Double opacity )
    {
        this.opacity = opacity;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitGroupSet getOrganisationUnitGroupSet()
    {
        return organisationUnitGroupSet;
    }

    public void setOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        this.organisationUnitGroupSet = organisationUnitGroupSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getAreaRadius()
    {
        return areaRadius;
    }

    public void setAreaRadius( Integer areaRadius )
    {
        this.areaRadius = areaRadius;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getHidden()
    {
        return hidden;
    }

    public void setLabels( Boolean labels )
    {
        this.labels = labels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getLabels()
    {
        return labels;
    }

    public void setHidden( Boolean hidden )
    {
        this.hidden = hidden;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontSize()
    {
        return labelFontSize;
    }

    public void setLabelFontSize( String labelFontSize )
    {
        this.labelFontSize = labelFontSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontWeight()
    {
        return labelFontWeight;
    }

    public void setLabelFontWeight( String labelFontWeight )
    {
        this.labelFontWeight = labelFontWeight;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontStyle()
    {
        return labelFontStyle;
    }

    public void setLabelFontStyle( String labelFontStyle )
    {
        this.labelFontStyle = labelFontStyle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLabelFontColor()
    {
        return labelFontColor;
    }

    public void setLabelFontColor( String labelFontColor )
    {
        this.labelFontColor = labelFontColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isEventClustering()
    {
        return eventClustering;
    }

    public void setEventClustering( boolean eventClustering )
    {
        this.eventClustering = eventClustering;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getEventCoordinateField()
    {
        return eventCoordinateField;
    }

    public void setEventCoordinateField( String eventCoordinateField )
    {
        this.eventCoordinateField = eventCoordinateField;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getEventPointColor()
    {
        return eventPointColor;
    }

    public void setEventPointColor( String eventPointColor )
    {
        this.eventPointColor = eventPointColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getEventPointRadius()
    {
        return eventPointRadius;
    }

    public void setEventPointRadius( int eventPointRadius )
    {
        this.eventPointRadius = eventPointRadius;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MapViewRenderingStrategy getRenderingStrategy()
    {
        return renderingStrategy;
    }

    public void setRenderingStrategy( MapViewRenderingStrategy renderingStrategy )
    {
        this.renderingStrategy = renderingStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ThematicMapType getThematicMapType()
    {
        return thematicMapType;
    }

    public void setThematicMapType( ThematicMapType thematicMapType )
    {
        this.thematicMapType = thematicMapType;
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
    public String getConfig()
    {
        return config;
    }

    public void setConfig( String config )
    {
        this.config = config;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Object getStyleDataItem()
    {
        return styleDataItem;
    }

    public void setStyleDataItem( Object styleDataItem )
    {
        this.styleDataItem = styleDataItem;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getParentGraph()
    {
        return parentGraph;
    }

    public void setParentGraph( String parentGraph )
    {
        this.parentGraph = parentGraph;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getParentLevel()
    {
        return parentLevel;
    }

    public void setParentLevel( int parentLevel )
    {
        this.parentLevel = parentLevel;
    }
}
