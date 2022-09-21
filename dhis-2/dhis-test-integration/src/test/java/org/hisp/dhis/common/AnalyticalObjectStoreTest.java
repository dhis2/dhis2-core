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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MapViewRenderingStrategy;
import org.hisp.dhis.mapping.MapViewStore;
import org.hisp.dhis.mapping.ThematicMapType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Lars Helge Overland
 */
class AnalyticalObjectStoreTest extends TransactionalIntegrationTest
{

    private IndicatorType itA;

    private Indicator inA;

    private Indicator inB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private MapView mvA;

    private MapView mvB;

    private MapView mvC;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    @Qualifier( "org.hisp.dhis.mapping.MapViewStore" )
    private MapViewStore mapViewStore;

    @Override
    public void setUpTest()
    {
        itA = createIndicatorType( 'A' );
        idObjectManager.save( itA );
        inA = createIndicator( 'A', itA );
        inB = createIndicator( 'B', itA );
        idObjectManager.save( inA );
        idObjectManager.save( inB );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        mvA = createMapView( MapView.LAYER_THEMATIC1 );
        mvB = createMapView( MapView.LAYER_THEMATIC2 );
        mvC = createMapView( MapView.LAYER_THEMATIC3 );
        mvA.addDataDimensionItem( inA );
        mvA.getOrganisationUnits().add( ouA );
        mvB.addDataDimensionItem( inB );
        mvB.getOrganisationUnits().add( ouA );
        mvC.addDataDimensionItem( inA );
        mvC.getOrganisationUnits().add( ouB );

        mvA.setOrgUnitField( "OU_Coordinate_Field" );

        mapViewStore.save( mvA );
        mapViewStore.save( mvB );
        mapViewStore.save( mvC );
    }

    @Test
    void testGetByIndicator()
    {
        List<MapView> actual = mapViewStore.getAnalyticalObjects( inA );
        assertEquals( 2, actual.size() );
        assertTrue( actual.contains( mvA ) );
        assertTrue( actual.contains( mvC ) );
    }

    @Test
    void testGetByOrgansiationUnit()
    {
        List<MapView> actual = mapViewStore.getAnalyticalObjects( ouA );
        assertEquals( 2, actual.size() );
        assertTrue( actual.contains( mvA ) );
        assertTrue( actual.contains( mvB ) );
    }

    @Test
    void testAssertProperties()
    {
        MapView mapView = mapViewStore.getByUid( mvA.getUid() );
        assertEquals( AggregationType.SUM, mapView.getAggregationType() );
        assertEquals( ThematicMapType.CHOROPLETH, mapView.getThematicMapType() );
        assertEquals( ProgramStatus.COMPLETED, mapView.getProgramStatus() );
        assertEquals( OrganisationUnitSelectionMode.DESCENDANTS, mapView.getOrganisationUnitSelectionMode() );
        assertEquals( MapViewRenderingStrategy.SINGLE, mapView.getRenderingStrategy() );
        assertEquals( UserOrgUnitType.DATA_CAPTURE, mapView.getUserOrgUnitType() );
        assertEquals( "#ddeeff", mapView.getNoDataColor() );
    }

    @Test
    void testSaveOrganisationUnitCoordinateField()
    {
        MapView mapView = mapViewStore.getByUid( mvA.getUid() );
        assertEquals( "OU_Coordinate_Field", mapView.getOrgUnitField() );
    }
}
