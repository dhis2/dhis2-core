package org.hisp.dhis.mapgeneration;

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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.filter.OrganisationUnitWithCoordinatesFilter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.util.Assert;

/**
 * An implementation of MapGenerationService that uses GeoTools to generate
 * maps.
 * 
 * @author Kenneth Solbø Andersen <kennetsa@ifi.uio.no>
 * @author Kristin Simonsen <krissimo@ifi.uio.no>
 * @author Kjetil Andresen <kjetand@ifi.uio.no>
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class GeoToolsMapGenerationService
    implements MapGenerationService
{    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private AnalyticsService analyticsService;

    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }
    
    private I18nManager i18nManager;

    public void setI18nManager( I18nManager i18nManager )
    {
        this.i18nManager = i18nManager;
    }

    // -------------------------------------------------------------------------
    // MapGenerationService implementation
    // -------------------------------------------------------------------------

    @Override
    public BufferedImage generateMapImage( MapView mapView )
    {
        Map map = new Map();
        
        map.getMapViews().add( mapView );
        
        return generateMapImage( map );
    }

    @Override
    public BufferedImage generateMapImage( Map map )
    {
        return generateMapImage( map, new Date(), null, 512, null );
    }
    
    @Override
    public BufferedImage generateMapImage( Map map, Date date, OrganisationUnit unit, Integer width, Integer height )
    {
        Assert.isTrue( map != null );
        
        if ( width == null && height == null )
        {
            width = MapUtils.DEFAULT_MAP_WIDTH;
        }

        InternalMap internalMap = new InternalMap();
        
        List<MapView> mapViews = new ArrayList<>( map.getMapViews() );
        Collections.reverse( mapViews );
        
        User user = currentUserService.getCurrentUser();
        
        for ( MapView mapView : mapViews )
        {        
            InternalMapLayer mapLayer = getSingleInternalMapLayer( mapView, user, date );
            
            if ( mapLayer != null )
            {
                internalMap.getLayers().add( mapLayer );
            }
        }
        
        if ( internalMap.getLayers().isEmpty() )
        {
            return null;
        }
        
        InternalMapLayer dataLayer = internalMap.getFirstDataLayer();
        
        BufferedImage mapImage = MapUtils.render( internalMap, width, height );

        if ( dataLayer == null )
        {
            return mapImage;
        }
        else
        {         
            LegendSet legendSet = new LegendSet( dataLayer );

            BufferedImage legendImage = legendSet.render( i18nManager.getI18nFormat() );

            BufferedImage titleImage = MapUtils.renderTitle( map.getName(), getImageWidth( legendImage, mapImage ) );
            
            return combineLegendAndMapImages( titleImage, legendImage, mapImage );
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static final String DEFAULT_COLOR_HIGH = "#ff0000";
    private static final String DEFAULT_COLOR_LOW = "#ffff00";

    private static final float DEFAULT_OPACITY = 0.75f;

    private static final Integer DEFAULT_RADIUS_HIGH = 35;
    private static final Integer DEFAULT_RADIUS_LOW = 15;

    private InternalMapLayer getSingleInternalMapLayer( MapView mapView, User user, Date date )
    {
        if ( mapView == null )
        {
            return null;
        }

        List<OrganisationUnit> atLevels = new ArrayList<>();
        List<OrganisationUnit> inGroups = new ArrayList<>();
        
        if ( mapView.hasOrganisationUnitLevels() )
        {
            atLevels.addAll( organisationUnitService.getOrganisationUnitsAtLevels( mapView.getOrganisationUnitLevels(), mapView.getOrganisationUnits() ) );
        }
        
        if ( mapView.hasItemOrganisationUnitGroups() )
        {
            inGroups.addAll( organisationUnitService.getOrganisationUnits( mapView.getItemOrganisationUnitGroups(), mapView.getOrganisationUnits() ) );
        }

        mapView.init( user, date, null, atLevels, inGroups, null );
        
        List<OrganisationUnit> organisationUnits = mapView.getAllOrganisationUnits();

        FilterUtils.filter( organisationUnits, new OrganisationUnitWithCoordinatesFilter() );
        
        java.util.Map<String, OrganisationUnit> uidOuMap = new HashMap<>();
        
        for ( OrganisationUnit ou : organisationUnits )
        {
            uidOuMap.put( ou.getUid(), ou );
        }
        
        String name = mapView.getName();

        Period period = null;
        
        if ( !mapView.getPeriods().isEmpty() ) // TODO integrate with BaseAnalyticalObject
        {
            period = mapView.getPeriods().get( 0 );
        }
        else if ( mapView.getRelatives() != null )
        {
            period = mapView.getRelatives().getRelativePeriods( date, null, false ).get( 0 );
        }
        
        Integer radiusLow = mapView.getRadiusLow() != null ? mapView.getRadiusLow() : DEFAULT_RADIUS_LOW;
        Integer radiusHigh = mapView.getRadiusHigh() != null ? mapView.getRadiusHigh() : DEFAULT_RADIUS_HIGH;

        // Get the low and high colors, typically in hexadecimal form, e.g. #ff3200
        Color colorLow = MapUtils.createColorFromString( StringUtils.trimToNull( mapView.getColorLow() ) != null ? mapView.getColorLow()
            : DEFAULT_COLOR_LOW );
        Color colorHigh = MapUtils.createColorFromString( StringUtils.trimToNull( mapView.getColorHigh() ) != null ? mapView.getColorHigh()
            : DEFAULT_COLOR_HIGH );

        Float opacity = mapView.getOpacity() != null ? mapView.getOpacity().floatValue() : DEFAULT_OPACITY;

        boolean hasLegendSet = mapView.hasLegendSet();

        // Create and setup an internal layer
        InternalMapLayer mapLayer = new InternalMapLayer();
        mapLayer.setName( name );
        mapLayer.setPeriod( period );
        mapLayer.setMethod( mapView.getMethod() );
        mapLayer.setLayer( mapView.getLayer() );
        mapLayer.setRadiusLow( radiusLow );
        mapLayer.setRadiusHigh( radiusHigh );
        mapLayer.setColorLow( colorLow );
        mapLayer.setColorHigh( colorHigh );
        mapLayer.setOpacity( opacity );
        mapLayer.setClasses( mapView.getClasses() );

        if ( !mapView.isDataLayer() ) // Boundary (and facility) layer
        {
            for ( OrganisationUnit unit : organisationUnits )
            {
                mapLayer.addBoundaryMapObject( unit );
            }
        }
        else // Thematic layer
        {
            Collection<MapValue> mapValues = getAggregatedMapValues( mapView );
    
            if ( mapValues.isEmpty() )
            {
                return null;
            }
            
            // Build and set the internal GeoTools map objects for the layer
            
            for ( MapValue mapValue : mapValues )
            {
                OrganisationUnit orgUnit = uidOuMap.get( mapValue.getOu() );
                
                if ( orgUnit != null )
                {
                    mapLayer.addDataMapObject( mapValue.getValue(), orgUnit );
                }
            }
    
            if ( !mapLayer.hasMapObjects() )
            {
                return null;
            }
            
            // Create an interval set for this map layer that distributes its map
            // objects into their respective intervals
            
            if ( hasLegendSet )
            {
                mapLayer.setIntervalSetFromLegendSet( mapView.getLegendSet() );
                mapLayer.distributeAndUpdateMapObjectsInIntervalSet();
            }
            else
            {
                mapLayer.setAutomaticIntervalSet( mapLayer.getClasses() );
                mapLayer.distributeAndUpdateMapObjectsInIntervalSet();
            }
            
            // Update the radius of each map object in this map layer according to
            // its map object's highest and lowest values
            
            mapLayer.applyInterpolatedRadii();
        }

        return mapLayer;
    }
    
    /**
     * Returns a list of map values for the given map view. If the map view is
     * not a data layer, an empty list is returned.
     */
    private List<MapValue> getAggregatedMapValues( MapView mapView )
    {
        Grid grid = analyticsService.getAggregatedDataValues( mapView );

        return getMapValues( grid );
    }
    
    /**
     * Creates a list of aggregated map values.
     */
    private List<MapValue> getMapValues( Grid grid )
    {
        List<MapValue> mapValues = new ArrayList<>();

        for ( List<Object> row : grid.getRows() )
        {
            if ( row != null && row.size() >= 3 )
            {
                int ouIndex = row.size() - 2;
                int valueIndex = row.size() - 1;
                
                String ou = (String) row.get( ouIndex );
                Double value = (Double) row.get( ( valueIndex ) );
                
                mapValues.add( new MapValue( ou, value ) );
            }
        }

        return mapValues;
    }
    
    private BufferedImage combineLegendAndMapImages( BufferedImage titleImage, BufferedImage legendImage, BufferedImage mapImage )
    {
        Assert.isTrue( titleImage != null );
        Assert.isTrue( legendImage != null );
        Assert.isTrue( mapImage != null );
        Assert.isTrue( legendImage.getType() == mapImage.getType() );

        // Create image, note that image height cannot be less than legend
        
        int width = getImageWidth( legendImage, mapImage );
        int height = Math.max( titleImage.getHeight() + mapImage.getHeight(), ( legendImage.getHeight() + 1 ) );
        
        BufferedImage finalImage = new BufferedImage( width, height, mapImage.getType() );

        // Draw the two images onto the final image with the legend to the left
        // and the map to the right
        Graphics graphics = finalImage.getGraphics();
        graphics.drawImage( titleImage, 0, 0, null );
        graphics.drawImage( legendImage, 0, MapUtils.TITLE_HEIGHT, null );
        graphics.drawImage( mapImage, legendImage.getWidth(), MapUtils.TITLE_HEIGHT, null );

        return finalImage;
    }
    
    private int getImageWidth( BufferedImage legendImage, BufferedImage mapImage )
    {
        return ( legendImage != null ? legendImage.getWidth() : 0 ) + ( mapImage != null ? mapImage.getWidth() : 0 );
    }
}
