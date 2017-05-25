package org.hisp.dhis.mapping;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;

import java.util.List;

/**
 * @author Jan Henrik Overland
 */
public interface MappingService
    extends AnalyticalObjectService<MapView>
{
    String ID = MappingService.class.getName();

    String GEOJSON_DIR = "geojson";

    String MAP_LEGEND_SYMBOLIZER_COLOR = "color";
    String MAP_LEGEND_SYMBOLIZER_IMAGE = "image";

    String KEY_MAP_DATE_TYPE = "dateType";

    String MAP_DATE_TYPE_FIXED = "fixed";
    String MAP_DATE_TYPE_START_END = "start-end";

    String ORGANISATION_UNIT_SELECTION_TYPE_PARENT = "parent";
    String ORGANISATION_UNIT_SELECTION_TYPE_LEVEL = "level";

    String MAP_LAYER_TYPE_BASELAYER = "baselayer";
    String MAP_LAYER_TYPE_OVERLAY = "overlay";

    // -------------------------------------------------------------------------
    // Map
    // -------------------------------------------------------------------------

    int addMap( Map map );

    void updateMap( Map map );

    Map getMap( int id );

    Map getMap( String uid );

    Map getMapNoAcl( String uid );

    void deleteMap( Map map );

    List<Map> getAllMaps();

    // -------------------------------------------------------------------------
    // MapView
    // -------------------------------------------------------------------------

    int addMapView( MapView mapView );

    void updateMapView( MapView mapView );

    void deleteMapView( MapView view );

    MapView getMapView( int id );

    MapView getMapView( String uid );

    MapView getMapViewByName( String name );

    MapView getIndicatorLastYearMapView( String indicatorUid, String organisationUnitUid, int level );

    List<MapView> getMapViewsByOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet );
    
    List<MapView> getAllMapViews();

    int countMapViewMaps( MapView mapView );

    // -------------------------------------------------------------------------
    // ExternalMapLayer
    // -------------------------------------------------------------------------

    int addExternalMapLayer( ExternalMapLayer mapLayer );

    void updateExternalMapLayer( ExternalMapLayer mapLayer );

    void deleteExternalMapLayer( ExternalMapLayer mapLayer );

    ExternalMapLayer getExternalMapLayer( int id );

    ExternalMapLayer getExternalMapLayer( String uid );

    ExternalMapLayer getExternalMapLayerByName( String name );

    List<ExternalMapLayer> getAllExternalMapLayers();

}