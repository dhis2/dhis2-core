package org.hisp.dhis.common;

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

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MapViewStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class AnalyticalObjectStoreTest
    extends DhisSpringTest
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

    @Resource( name = "org.hisp.dhis.mapping.MapViewStore" )
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
        
        mvA = new MapView( MapView.LAYER_THEMATIC1 );
        mvB = new MapView( MapView.LAYER_THEMATIC1 );
        mvC = new MapView( MapView.LAYER_THEMATIC1 );
        
        mvA.addDataDimensionItem( inA );
        mvA.getOrganisationUnits().add( ouA );
        mvB.addDataDimensionItem( inB );
        mvB.getOrganisationUnits().add( ouA );
        mvC.addDataDimensionItem( inA );
        mvC.getOrganisationUnits().add( ouB );

        mapViewStore.save( mvA );
        mapViewStore.save( mvB );
        mapViewStore.save( mvC );
    }
    
    @Test
    public void testGetByIndicator()
    {
        List<MapView> actual = mapViewStore.getAnalyticalObjects( inA );
        
        assertEquals( 2, actual.size() );
        
        assertTrue( actual.contains( mvA ) );
        assertTrue( actual.contains( mvC ) );
    }
    
    @Test
    public void testGetByOrgansiationUnit()
    {
        List<MapView> actual = mapViewStore.getAnalyticalObjects( ouA );
        
        assertEquals( 2, actual.size() );
        
        assertTrue( actual.contains( mvA ) );
        assertTrue( actual.contains( mvB ) );
    }
}
