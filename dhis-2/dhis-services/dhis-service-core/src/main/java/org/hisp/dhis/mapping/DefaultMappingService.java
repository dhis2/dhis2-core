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
package org.hisp.dhis.mapping;

import java.util.List;

import lombok.RequiredArgsConstructor;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan Henrik Overland
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.mapping.MappingService" )
public class DefaultMappingService
    extends GenericAnalyticalObjectService<MapView>
    implements MappingService
{
    private final MapStore mapStore;

    private final MapViewStore mapViewStore;

    private final ExternalMapLayerStore externalMapLayerStore;

    private final OrganisationUnitService organisationUnitService;

    private final IndicatorService indicatorService;

    private final PeriodService periodService;

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
    @Transactional
    public long addMap( Map map )
    {
        map.getMapViews().forEach( mapView -> mapView.setAutoFields() );

        mapStore.save( map );

        return map.getId();
    }

    @Override
    @Transactional
    public void updateMap( Map map )
    {
        map.getMapViews().forEach( mapView -> mapView.setAutoFields() );

        mapStore.update( map );
    }

    @Override
    @Transactional( readOnly = true )
    public Map getMap( long id )
    {
        return mapStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Map getMap( String uid )
    {
        return mapStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public Map getMapNoAcl( String uid )
    {
        return mapStore.getByUidNoAcl( uid );
    }

    @Override
    @Transactional
    public void deleteMap( Map map )
    {
        mapStore.delete( map );
    }

    // -------------------------------------------------------------------------
    // MapView
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addMapView( MapView mapView )
    {
        mapViewStore.save( mapView );
        return mapView.getId();
    }

    @Override
    @Transactional
    public void updateMapView( MapView mapView )
    {
        mapViewStore.update( mapView );
    }

    @Override
    @Transactional
    public void deleteMapView( MapView mapView )
    {
        mapViewStore.delete( mapView );
    }

    @Override
    @Transactional( readOnly = true )
    public MapView getMapView( long id )
    {
        return mapViewStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public MapView getMapView( String uid )
    {
        return mapViewStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public List<MapView> getMapViewsByOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        return mapViewStore.getByOrganisationUnitGroupSet( groupSet );
    }

    @Override
    @Transactional( readOnly = true )
    public int countMapViewMaps( MapView mapView )
    {
        return mapStore.countMapViewMaps( mapView );
    }

    // -------------------------------------------------------------------------
    // ExternalMapLayer
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addExternalMapLayer( ExternalMapLayer externalMapLayer )
    {
        externalMapLayerStore.save( externalMapLayer );
        return externalMapLayer.getId();
    }

    @Override
    @Transactional
    public void updateExternalMapLayer( ExternalMapLayer externalMapLayer )
    {
        externalMapLayerStore.update( externalMapLayer );
    }

    @Override
    @Transactional
    public void deleteExternalMapLayer( ExternalMapLayer externalMapLayer )
    {
        externalMapLayerStore.delete( externalMapLayer );
    }

    @Override
    @Transactional( readOnly = true )
    public ExternalMapLayer getExternalMapLayer( long id )
    {
        return externalMapLayerStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ExternalMapLayer getExternalMapLayer( String uid )
    {
        return externalMapLayerStore.getByUid( uid );
    }
}
