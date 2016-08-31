package org.hisp.dhis.mapping;

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

import java.util.List;

import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.GenericAnalyticalObjectService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.RelativePeriods;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan Henrik Overland
 */
@Transactional
public class DefaultMappingService
    extends GenericAnalyticalObjectService<MapView>
    implements MappingService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private MapStore mapStore;

    public void setMapStore( MapStore mapStore )
    {
        this.mapStore = mapStore;
    }

    private MapViewStore mapViewStore;

    public void setMapViewStore( MapViewStore mapViewStore )
    {
        this.mapViewStore = mapViewStore;
    }

    private MapLayerStore mapLayerStore;

    public void setMapLayerStore( MapLayerStore mapLayerStore )
    {
        this.mapLayerStore = mapLayerStore;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private IndicatorService indicatorService;

    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // MappingService implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectStore<MapView> getAnalyticalObjectStore()
    {
        return mapViewStore;
    }
    
    // -------------------------------------------------------------------------
    // Map
    // -------------------------------------------------------------------------

    @Override
    public int addMap( Map map )
    {
        return mapStore.save( map );
    }

    @Override
    public void updateMap( Map map )
    {
        mapStore.update( map );
    }

    @Override
    public Map getMap( int id )
    {
        return mapStore.get( id );
    }

    @Override
    public Map getMap( String uid )
    {
        return mapStore.getByUid( uid );
    }

    @Override
    public Map getMapNoAcl( String uid )
    {
        return mapStore.getByUidNoAcl( uid );
    }

    @Override
    public void deleteMap( Map map )
    {
        mapStore.delete( map );
    }

    @Override
    public List<Map> getAllMaps()
    {
        return mapStore.getAll();
    }

    @Override
    public List<Map> getMapsBetweenLikeName( String name, int first, int max )
    {
        return mapStore.getAllLikeName( name, first, max );
    }

    // -------------------------------------------------------------------------
    // MapView
    // -------------------------------------------------------------------------

    @Override
    public int addMapView( MapView mapView )
    {
        return mapViewStore.save( mapView );
    }

    @Override
    public void updateMapView( MapView mapView )
    {
        mapViewStore.update( mapView );
    }

    @Override
    public void deleteMapView( MapView mapView )
    {
        mapViewStore.delete( mapView );
    }

    @Override
    public MapView getMapView( int id )
    {
        return mapViewStore.get( id );
    }

    @Override
    public MapView getMapView( String uid )
    {
        MapView mapView = mapViewStore.getByUid( uid );

        return mapView;
    }

    @Override
    public MapView getMapViewByName( String name )
    {
        return mapViewStore.getByName( name );
    }
    
    @Override
    public MapView getIndicatorLastYearMapView( String indicatorUid, String organisationUnitUid, int level )
    {
        MapView mapView = new MapView();

        Period period = periodService.reloadPeriod( new RelativePeriods().setThisYear( true ).getRelativePeriods()
            .iterator().next() );

        Indicator indicator = indicatorService.getIndicator( indicatorUid );
        OrganisationUnit unit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

        mapView.addDataDimensionItem( indicator );
        mapView.getPeriods().add( period );
        mapView.getOrganisationUnits().add( unit );
        mapView.getOrganisationUnitLevels().add( level );
        mapView.setName( indicator.getName() );

        return mapView;
    }
    
    @Override
    public List<MapView> getMapViewsByOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        return mapViewStore.getByOrganisationUnitGroupSet( groupSet );
    }

    @Override
    public List<MapView> getAllMapViews()
    {
        return mapViewStore.getAll();
    }

    @Override
    public List<MapView> getMapViewsBetweenByName( String name, int first, int max )
    {
        return mapViewStore.getAllLikeName( name, first, max );
    }

    // -------------------------------------------------------------------------
    // MapLayer
    // -------------------------------------------------------------------------

    @Override
    public int addMapLayer( MapLayer mapLayer )
    {
        return mapLayerStore.save( mapLayer );
    }

    @Override
    public void updateMapLayer( MapLayer mapLayer )
    {
        mapLayerStore.update( mapLayer );
    }

    @Override
    public void addOrUpdateMapLayer( String name, String type, String url, String layers, String time,
        String fillColor, double fillOpacity, String strokeColor, int strokeWidth )
    {
        MapLayer mapLayer = mapLayerStore.getByName( name );

        if ( mapLayer != null )
        {
            mapLayer.setName( name );
            mapLayer.setType( type );
            mapLayer.setUrl( url );
            mapLayer.setLayers( layers );
            mapLayer.setTime( time );
            mapLayer.setFillColor( fillColor );
            mapLayer.setFillOpacity( fillOpacity );
            mapLayer.setStrokeColor( strokeColor );
            mapLayer.setStrokeWidth( strokeWidth );

            updateMapLayer( mapLayer );
        }
        else
        {
            addMapLayer( new MapLayer( name, type, url, layers, time, fillColor, fillOpacity, strokeColor, strokeWidth ) );
        }
    }

    @Override
    public void deleteMapLayer( MapLayer mapLayer )
    {
        mapLayerStore.delete( mapLayer );
    }

    @Override
    public MapLayer getMapLayer( int id )
    {
        return mapLayerStore.get( id );
    }

    @Override
    public MapLayer getMapLayer( String uid )
    {
        return mapLayerStore.getByUid( uid );
    }

    @Override
    public MapLayer getMapLayerByName( String name )
    {
        return mapLayerStore.getByName( name );
    }

    @Override
    public List<MapLayer> getMapLayersByType( String type )
    {
        return mapLayerStore.getMapLayersByType( type );
    }

    @Override
    public MapLayer getMapLayerByMapSource( String mapSource )
    {
        return mapLayerStore.getMapLayerByMapSource( mapSource );
    }

    @Override
    public List<MapLayer> getAllMapLayers()
    {
        return mapLayerStore.getAll();
    }

    @Override
    public int countMapViewMaps( MapView mapView )
    {
        return mapStore.countMapViewMaps( mapView );
    }
}
