package org.hisp.dhis.indicator;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class IndicatorServiceTest
    extends DhisSpringTest
{
    @Autowired
    private IndicatorService indicatorService;

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private void assertEq( char uniqueCharacter, Indicator indicator )
    {
        assertEquals( "Indicator" + uniqueCharacter, indicator.getName() );
        assertEquals( "IndicatorShort" + uniqueCharacter, indicator.getShortName() );
        assertEquals( "IndicatorCode" + uniqueCharacter, indicator.getCode() );
        assertEquals( "IndicatorDescription" + uniqueCharacter, indicator.getDescription() );
    }

    // -------------------------------------------------------------------------
    // IndicatorType
    // -------------------------------------------------------------------------

    @Test
    public void testAddIndicatorType()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );

        int idA = indicatorService.addIndicatorType( typeA );
        int idB = indicatorService.addIndicatorType( typeB );

        typeA = indicatorService.getIndicatorType( idA );
        assertNotNull( typeA );
        assertEquals( idA, typeA.getId() );

        typeB = indicatorService.getIndicatorType( idB );
        assertNotNull( typeB );
        assertEquals( idB, typeB.getId() );
    }

    @Test
    public void testUpdateIndicatorType()
        throws Exception
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        int idA = indicatorService.addIndicatorType( typeA );
        typeA = indicatorService.getIndicatorType( idA );
        assertEquals( typeA.getName(), "IndicatorTypeA" );

        typeA.setName( "IndicatorTypeB" );
        indicatorService.updateIndicatorType( typeA );
        typeA = indicatorService.getIndicatorType( idA );
        assertNotNull( typeA );
        assertEquals( typeA.getName(), "IndicatorTypeB" );
    }

    @Test
    public void testGetAndDeleteIndicatorType()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );

        int idA = indicatorService.addIndicatorType( typeA );
        int idB = indicatorService.addIndicatorType( typeB );

        assertNotNull( indicatorService.getIndicatorType( idA ) );
        assertNotNull( indicatorService.getIndicatorType( idB ) );

        indicatorService.deleteIndicatorType( typeA );

        assertNull( indicatorService.getIndicatorType( idA ) );
        assertNotNull( indicatorService.getIndicatorType( idB ) );

        indicatorService.deleteIndicatorType( typeB );

        assertNull( indicatorService.getIndicatorType( idA ) );
        assertNull( indicatorService.getIndicatorType( idB ) );
    }

    @Test
    public void testGetAllIndicatorTypes()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );

        indicatorService.addIndicatorType( typeA );
        indicatorService.addIndicatorType( typeB );

        List<IndicatorType> types = indicatorService.getAllIndicatorTypes();

        assertEquals( types.size(), 2 );
        assertTrue( types.contains( typeA ) );
        assertTrue( types.contains( typeB ) );
    }

    @Test
    public void testGetIndicatorTypeByName()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );

        int idA = indicatorService.addIndicatorType( typeA );
        int idB = indicatorService.addIndicatorType( typeB );

        assertNotNull( indicatorService.getIndicatorType( idA ) );
        assertNotNull( indicatorService.getIndicatorType( idB ) );

        typeA = indicatorService.getIndicatorTypeByName( "IndicatorTypeA" );
        assertNotNull( typeA );
        assertEquals( typeA.getId(), idA );

        IndicatorType typeC = indicatorService.getIndicatorTypeByName( "IndicatorTypeC" );
        assertNull( typeC );
    }

    // -------------------------------------------------------------------------
    // IndicatorGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );

        int idA = indicatorService.addIndicatorGroup( groupA );
        int idB = indicatorService.addIndicatorGroup( groupB );

        groupA = indicatorService.getIndicatorGroup( idA );
        assertNotNull( groupA );
        assertEquals( idA, groupA.getId() );

        groupB = indicatorService.getIndicatorGroup( idB );
        assertNotNull( groupB );
        assertEquals( idB, groupB.getId() );
    }

    @Test
    public void testUpdateIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        int idA = indicatorService.addIndicatorGroup( groupA );
        groupA = indicatorService.getIndicatorGroup( idA );
        assertEquals( groupA.getName(), "IndicatorGroupA" );

        groupA.setName( "IndicatorGroupB" );
        indicatorService.updateIndicatorGroup( groupA );
        groupA = indicatorService.getIndicatorGroup( idA );
        assertNotNull( groupA );
        assertEquals( groupA.getName(), "IndicatorGroupB" );
    }

    @Test
    public void testGetAndDeleteIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );

        int idA = indicatorService.addIndicatorGroup( groupA );
        int idB = indicatorService.addIndicatorGroup( groupB );

        assertNotNull( indicatorService.getIndicatorGroup( idA ) );
        assertNotNull( indicatorService.getIndicatorGroup( idB ) );

        indicatorService.deleteIndicatorGroup( groupA );

        assertNull( indicatorService.getIndicatorGroup( idA ) );
        assertNotNull( indicatorService.getIndicatorGroup( idB ) );

        indicatorService.deleteIndicatorGroup( groupB );

        assertNull( indicatorService.getIndicatorGroup( idA ) );
        assertNull( indicatorService.getIndicatorGroup( idB ) );
    }

    @Test
    public void testGetAllIndicatorGroups()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );

        indicatorService.addIndicatorGroup( groupA );
        indicatorService.addIndicatorGroup( groupB );

        List<IndicatorGroup> groups = indicatorService.getAllIndicatorGroups();

        assertEquals( groups.size(), 2 );
        assertTrue( groups.contains( groupA ) );
        assertTrue( groups.contains( groupB ) );
    }

    @Test
    public void testGetIndicatorGroupByName()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );

        int idA = indicatorService.addIndicatorGroup( groupA );
        int idB = indicatorService.addIndicatorGroup( groupB );

        assertNotNull( indicatorService.getIndicatorGroup( idA ) );
        assertNotNull( indicatorService.getIndicatorGroup( idB ) );

        groupA = indicatorService.getIndicatorGroupByName( "IndicatorGroupA" ).get( 0 );
        assertNotNull( groupA );
        assertEquals( groupA.getId(), idA );

        assertTrue( indicatorService.getIndicatorGroupByName( "IndicatorGroupC" ).isEmpty() );
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    @Test
    public void testAddIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );

        int idA = indicatorService.addIndicator( indicatorA );
        int idB = indicatorService.addIndicator( indicatorB );

        indicatorA = indicatorService.getIndicator( idA );
        assertNotNull( indicatorA );
        assertEq( 'A', indicatorA );

        indicatorB = indicatorService.getIndicator( idB );
        assertNotNull( indicatorB );
        assertEq( 'B', indicatorB );
    }

    @Test
    public void testUpdateIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        int idA = indicatorService.addIndicator( indicatorA );
        indicatorA = indicatorService.getIndicator( idA );
        assertEq( 'A', indicatorA );

        indicatorA.setName( "IndicatorB" );
        indicatorService.updateIndicator( indicatorA );
        indicatorA = indicatorService.getIndicator( idA );
        assertNotNull( indicatorA );
        assertEquals( indicatorA.getName(), "IndicatorB" );
    }

    @Test
    public void testGetAndDeleteIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );

        int idA = indicatorService.addIndicator( indicatorA );
        int idB = indicatorService.addIndicator( indicatorB );

        assertNotNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );

        indicatorService.deleteIndicator( indicatorA );

        assertNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );

        indicatorService.deleteIndicator( indicatorB );

        assertNull( indicatorService.getIndicator( idA ) );
        assertNull( indicatorService.getIndicator( idB ) );
    }

    @Test
    public void testGetAllIndicators()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );

        indicatorService.addIndicator( indicatorA );
        indicatorService.addIndicator( indicatorB );

        List<Indicator> indicators = indicatorService.getAllIndicators();

        assertEquals( indicators.size(), 2 );
        assertTrue( indicators.contains( indicatorA ) );
        assertTrue( indicators.contains( indicatorB ) );
    }

    @Test
    public void testGetIndicatorByName()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );

        int idA = indicatorService.addIndicator( indicatorA );
        int idB = indicatorService.addIndicator( indicatorB );

        assertNotNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );

        indicatorA = indicatorService.getIndicatorByName( "IndicatorA" ).get( 0 );
        assertNotNull( indicatorA );
        assertEq( 'A', indicatorA );

        assertTrue( indicatorService.getIndicatorByName( "IndicatorC" ).isEmpty() );
    }

    @Test
    public void testGetIndicatorByShortName()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );

        indicatorService.addIndicatorType( type );

        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );

        int idA = indicatorService.addIndicator( indicatorA );
        int idB = indicatorService.addIndicator( indicatorB );

        assertNotNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );

        indicatorA = indicatorService.getIndicatorByShortName( "IndicatorShortA" ).get( 0 );
        assertNotNull( indicatorA );
        assertEq( 'A', indicatorA );

        assertTrue( indicatorService.getIndicatorByShortName( "IndicatorShortC" ).isEmpty() );
    }
}
