package org.hisp.dhis.organisationunit;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.user.User;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Kristian Nordal
 */
public class OrganisationUnitServiceTest
    extends DhisSpringTest
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    @Test
    public void testBasicOrganisationUnitCoarseGrained()
    {
        OrganisationUnit organisationUnit1 = createOrganisationUnit( 'A' );

        int id1 = organisationUnitService.addOrganisationUnit( organisationUnit1 );

        assertNotNull( organisationUnitService.getOrganisationUnit( id1 ) );

        assertNull( organisationUnitService.getOrganisationUnit( -1 ) );

        OrganisationUnit organisationUnit2 = createOrganisationUnit( 'B', organisationUnit1 );

        int id2 = organisationUnitService.addOrganisationUnit( organisationUnit2 );

        assertTrue( organisationUnitService.getOrganisationUnit( id2 ).getParent().getId() == id1 );

        organisationUnitService.deleteOrganisationUnit( organisationUnitService.getOrganisationUnit( id2 ) );

        assertNotNull( organisationUnitService.getOrganisationUnit( id1 ) );
        assertNull( organisationUnitService.getOrganisationUnit( id2 ) );
    }

    @Test
    public void testUpdateOrganisationUnit()
    {
        String updatedName = "updatedName";
        String updatedShortName = "updatedShortName";

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );

        int id = organisationUnitService.addOrganisationUnit( organisationUnit );

        organisationUnit.setName( updatedName );
        organisationUnit.setShortName( updatedShortName );

        organisationUnitService.updateOrganisationUnit( organisationUnit );

        OrganisationUnit updatedOrganisationUnit = organisationUnitService.getOrganisationUnit( id );

        assertEquals( updatedOrganisationUnit.getName(), updatedName );
        assertEquals( updatedOrganisationUnit.getShortName(), updatedShortName );
    }

    @Test
    public void testGetOrganisationUnitWithChildren()
        throws Exception
    {
        OrganisationUnit unit1 = createOrganisationUnit( 'A' );
        OrganisationUnit unit2 = createOrganisationUnit( 'B', unit1 );
        OrganisationUnit unit3 = createOrganisationUnit( 'C', unit2 );
        OrganisationUnit unit4 = createOrganisationUnit( 'D' );

        int id1 = organisationUnitService.addOrganisationUnit( unit1 );
        unit1.getChildren().add( unit2 );
        organisationUnitService.addOrganisationUnit( unit2 );
        organisationUnitService.addOrganisationUnit( unit3 );
        organisationUnitService.addOrganisationUnit( unit4 );

        List<OrganisationUnit> actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1 ) );

        assertEquals( 3, actual.size() );
        assertTrue( actual.contains( unit1 ) );
        assertTrue( actual.contains( unit2 ) );
    }

    @Test
    public void testGetOrganisationUnitWithChildrenB()
    {
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B', unitA );
        OrganisationUnit unitC = createOrganisationUnit( 'C', unitB );
        OrganisationUnit unitD = createOrganisationUnit( 'D', unitB );
        OrganisationUnit unitE = createOrganisationUnit( 'E', unitC );

        int idA = organisationUnitService.addOrganisationUnit( unitA );
        int idB = organisationUnitService.addOrganisationUnit( unitB );
        int idC = organisationUnitService.addOrganisationUnit( unitC );
        int idD = organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );

        List<OrganisationUnit> actualA = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( idA ) );
        List<OrganisationUnit> actualB = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( idB ) );
        List<OrganisationUnit> actualC = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( idC ) );
        List<OrganisationUnit> actualD = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( idD ) );

        assertEquals( 5, actualA.size() );
        assertEquals( 4, actualB.size() );
        assertEquals( 2, actualC.size() );
        assertEquals( 1, actualD.size() );
        
        assertEquals( Sets.newHashSet( unitB, unitC, unitD, unitE ), Sets.newHashSet( actualB ) );
        assertEquals( Sets.newHashSet( unitC, unitE ), Sets.newHashSet( actualC ) );
    }
    
    @Test
    public void testGetOrganisationUnitLevel()
    {
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B', unitA );
        OrganisationUnit unitC = createOrganisationUnit( 'C', unitB );
        OrganisationUnit unitD = createOrganisationUnit( 'D', unitB );
        OrganisationUnit unitE = createOrganisationUnit( 'E', unitC );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );

        assertEquals( 1, unitA.getLevel() );
        assertEquals( 2, unitB.getLevel() );
        assertEquals( 3, unitC.getLevel() );
        assertEquals( 3, unitD.getLevel() );
        assertEquals( 4, unitE.getLevel() );
    }

    @Test
    public void testGetOrganisationUnitWithChildrenMaxLevel()
    {
        OrganisationUnit unit1 = createOrganisationUnit( 'A' );
        OrganisationUnit unit2 = createOrganisationUnit( 'B', unit1 );
        OrganisationUnit unit3 = createOrganisationUnit( 'C', unit1 );
        OrganisationUnit unit4 = createOrganisationUnit( 'D', unit2 );
        OrganisationUnit unit5 = createOrganisationUnit( 'E', unit2 );
        OrganisationUnit unit6 = createOrganisationUnit( 'F', unit3 );
        OrganisationUnit unit7 = createOrganisationUnit( 'G', unit3 );

        unit1.getChildren().add( unit2 );
        unit1.getChildren().add( unit3 );
        unit2.getChildren().add( unit4 );
        unit2.getChildren().add( unit5 );
        unit3.getChildren().add( unit6 );
        unit3.getChildren().add( unit7 );
        
        int id1 = organisationUnitService.addOrganisationUnit( unit1 );        
        int id2 = organisationUnitService.addOrganisationUnit( unit2 );
        organisationUnitService.addOrganisationUnit( unit3 );
        int id4 = organisationUnitService.addOrganisationUnit( unit4 );
        organisationUnitService.addOrganisationUnit( unit5 );
        organisationUnitService.addOrganisationUnit( unit6 );
        organisationUnitService.addOrganisationUnit( unit7 );

        List<OrganisationUnit> actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1, 0 ) );
        assertEquals( 0, actual.size() );
        
        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1, 1 ) );
        assertEquals( 1, actual.size() );
        assertTrue( actual.contains( unit1 ) );

        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1, 2 ) );
        assertEquals( 3, actual.size() );
        assertTrue( actual.contains( unit1 ) );
        assertTrue( actual.contains( unit2 ) );
        assertTrue( actual.contains( unit3 ) );

        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1, 3 ) );
        assertEquals( 7, actual.size() );
        
        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id1, 8 ) );
        assertEquals( 7, actual.size() );
        
        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id2, 1 ) );
        assertEquals( 1, actual.size() );

        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id2, 2 ) );
        assertEquals( 3, actual.size() );

        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id4, 1 ) );
        assertEquals( 1, actual.size() );
        
        actual = new ArrayList<>( organisationUnitService.getOrganisationUnitWithChildren( id4, 8 ) );
        assertEquals( 1, actual.size() );
    }

    @Test
    public void testGetOrganisationUnitsByFields()
    {
        String oU1Name = "OU1name";
        String oU2Name = "OU2name";
        String oU3Name = "OU3name";
        String oU1ShortName = "OU1ShortName";
        String oU2ShortName = "OU2ShortName";
        String oU3ShortName = "OU3ShortName";
        String oU1Code = "OU1Code";
        String oU2Code = "OU2Code";
        String oU3Code = "OU3Code";

        OrganisationUnit organisationUnit1 = new OrganisationUnit( oU1Name, null, oU1ShortName, oU1Code, new Date(), null, null );
        OrganisationUnit organisationUnit2 = new OrganisationUnit( oU2Name, null, oU2ShortName, oU2Code, new Date(), null, null );
        OrganisationUnit organisationUnit3 = new OrganisationUnit( oU3Name, null, oU3ShortName, oU3Code, new Date(), null, null );
        
        organisationUnitService.addOrganisationUnit( organisationUnit1 );
        organisationUnitService.addOrganisationUnit( organisationUnit2 );
        organisationUnitService.addOrganisationUnit( organisationUnit3 );

        OrganisationUnit unit1 = organisationUnitService.getOrganisationUnitByName( oU1Name ).get( 0 );
        assertEquals( unit1.getName(), oU1Name );

        List<OrganisationUnit> units = organisationUnitService.getOrganisationUnitByName( "foo" );
        assertTrue( units.isEmpty() );

        unit1 = organisationUnitService.getOrganisationUnitByCode( oU1Code );
        assertEquals( unit1.getName(), oU1Name );

        OrganisationUnit unit4 = organisationUnitService.getOrganisationUnitByCode( "foo" );
        assertNull( unit4 );
    }

    @Test
    public void testGetAllOrganisationUnitsAndGetRootOrganisationUnit()
    {
        // creating a tree with two roots ( id1 and id4 )

        OrganisationUnit unit1 = createOrganisationUnit( 'A' );
        OrganisationUnit unit2 = createOrganisationUnit( 'B', unit1 );
        OrganisationUnit unit3 = createOrganisationUnit( 'C', unit1 );
        OrganisationUnit unit4 = createOrganisationUnit( 'D' );
        OrganisationUnit unit5 = createOrganisationUnit( 'E', unit4 );

        organisationUnitService.addOrganisationUnit( unit1 );
        organisationUnitService.addOrganisationUnit( unit2 );
        organisationUnitService.addOrganisationUnit( unit3 );
        organisationUnitService.addOrganisationUnit( unit4 );
        organisationUnitService.addOrganisationUnit( unit5 );

        List<OrganisationUnit> units = organisationUnitService.getAllOrganisationUnits();

        assertNotNull( units );
        assertEquals( 5, units.size() );
        assertTrue( units.contains( unit1 ) );
        assertTrue( units.contains( unit2 ) );
        assertTrue( units.contains( unit3 ) );
        assertTrue( units.contains( unit4 ) );
        assertTrue( units.contains( unit5 ) );

        units = organisationUnitService.getRootOrganisationUnits();

        assertNotNull( units );
        assertEquals( 2, units.size() );
        assertTrue( units.contains( unit1 ) );
        assertTrue( units.contains( unit4 ) );
    }

    @Test
    public void testGetOrganisationUnitsAtLevel()
    {
        OrganisationUnitLevel levelA = new OrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = new OrganisationUnitLevel( 2, "Province" );
        OrganisationUnitLevel levelC = new OrganisationUnitLevel( 3, "County" );
        OrganisationUnitLevel levelD = new OrganisationUnitLevel( 4, "District" );

        organisationUnitService.addOrganisationUnitLevel( levelA );
        organisationUnitService.addOrganisationUnitLevel( levelB );
        organisationUnitService.addOrganisationUnitLevel( levelC );
        organisationUnitService.addOrganisationUnitLevel( levelD );

        OrganisationUnit unit1 = createOrganisationUnit( '1' );
        organisationUnitService.addOrganisationUnit( unit1 );

        OrganisationUnit unit2 = createOrganisationUnit( '2', unit1 );
        unit1.getChildren().add( unit2 );
        organisationUnitService.addOrganisationUnit( unit2 );

        OrganisationUnit unit3 = createOrganisationUnit( '3', unit2 );
        unit2.getChildren().add( unit3 );
        organisationUnitService.addOrganisationUnit( unit3 );

        OrganisationUnit unit4 = createOrganisationUnit( '4', unit2 );
        unit2.getChildren().add( unit4 );
        organisationUnitService.addOrganisationUnit( unit4 );

        OrganisationUnit unit5 = createOrganisationUnit( '5', unit2 );
        unit2.getChildren().add( unit5 );
        organisationUnitService.addOrganisationUnit( unit5 );

        OrganisationUnit unit6 = createOrganisationUnit( '6', unit3 );
        unit3.getChildren().add( unit6 );
        organisationUnitService.addOrganisationUnit( unit6 );

        OrganisationUnit unit7 = createOrganisationUnit( '7' );
        organisationUnitService.addOrganisationUnit( unit7 );

        // unit1
        // unit1 . unit2
        // unit1 . unit2 . unit3
        // unit1 . unit2 . unit4
        // unit1 . unit2 . unit5
        // unit1 . unit2 . unit3 . unit6
        // unit7

        assertEquals( 2, organisationUnitService.getOrganisationUnitsAtLevel( 1 ).size() );
        assertEquals( 3, organisationUnitService.getOrganisationUnitsAtLevel( 3 ).size() );
        assertEquals( 4, organisationUnitService.getNumberOfOrganisationalLevels() );
        assertTrue( unit4.getLevel() == 3 );
        assertTrue( unit1.getLevel() == 1 );
        assertTrue( unit6.getLevel() == 4 );

        assertEquals( 3, organisationUnitService.getOrganisationUnitsAtLevels( Lists.newArrayList( 3 ),  Lists.newArrayList( unit1 ) ).size() );
        assertEquals( 3, organisationUnitService.getOrganisationUnitsAtLevels( Lists.newArrayList( 3 ),  Lists.newArrayList( unit2 ) ).size() );
        assertEquals( 0, organisationUnitService.getOrganisationUnitsAtLevels( Lists.newArrayList( 3 ),  Lists.newArrayList( unit7 ) ).size() );
        assertEquals( 4, organisationUnitService.getOrganisationUnitsAtLevels( Lists.newArrayList( 3, 4 ),  Lists.newArrayList( unit1 ) ).size() );
        assertEquals( 4, organisationUnitService.getOrganisationUnitsAtLevels( Lists.newArrayList( 3, 4 ),  Lists.newArrayList( unit1, unit7 ) ).size() );

        assertEquals( 3, organisationUnitService.getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( levelC ),  Lists.newArrayList( unit1 ) ).size() );
        assertEquals( 3, organisationUnitService.getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( levelC ),  Lists.newArrayList( unit2 ) ).size() );
        assertEquals( 0, organisationUnitService.getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( levelC ),  Lists.newArrayList( unit7 ) ).size() );
        assertEquals( 4, organisationUnitService.getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( levelC, levelD ),  Lists.newArrayList( unit1 ) ).size() );
        assertEquals( 4, organisationUnitService.getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( levelC, levelD ),  Lists.newArrayList( unit1, unit7 ) ).size() );
    }

    @Test
    public void testGetNumberOfOrganisationalLevels()
    {
        assertEquals( 0, organisationUnitService.getNumberOfOrganisationalLevels() );
        
        OrganisationUnit unit1 = createOrganisationUnit( '1' );
        organisationUnitService.addOrganisationUnit( unit1 );

        OrganisationUnit unit2 = createOrganisationUnit( '2', unit1 );
        unit1.getChildren().add( unit2 );
        organisationUnitService.addOrganisationUnit( unit2 );

        assertEquals( 2, organisationUnitService.getNumberOfOrganisationalLevels() );

        OrganisationUnit unit3 = createOrganisationUnit( '3', unit2 );
        unit2.getChildren().add( unit3 );
        organisationUnitService.addOrganisationUnit( unit3 );

        OrganisationUnit unit4 = createOrganisationUnit( '4', unit2 );
        unit2.getChildren().add( unit4 );
        organisationUnitService.addOrganisationUnit( unit4 );

        assertEquals( 3, organisationUnitService.getNumberOfOrganisationalLevels() );
    }
    
    @Test
    public void testIsDescendantSet()
    {
        OrganisationUnit unit1 = createOrganisationUnit( '1' );
        organisationUnitService.addOrganisationUnit( unit1 );

        OrganisationUnit unit2 = createOrganisationUnit( '2', unit1 );
        unit1.getChildren().add( unit2 );
        organisationUnitService.addOrganisationUnit( unit2 );

        OrganisationUnit unit3 = createOrganisationUnit( '3', unit2 );
        unit2.getChildren().add( unit3 );
        organisationUnitService.addOrganisationUnit( unit3 );

        OrganisationUnit unit4 = createOrganisationUnit( '4' );
        organisationUnitService.addOrganisationUnit( unit4 );
        
        assertTrue( unit1.isDescendant( Sets.newHashSet( unit1 ) ) );
        assertTrue( unit2.isDescendant( Sets.newHashSet( unit1 ) ) );
        assertTrue( unit3.isDescendant( Sets.newHashSet( unit1 ) ) );
        assertTrue( unit2.isDescendant( Sets.newHashSet( unit1, unit3 ) ) );
        
        assertFalse( unit2.isDescendant( Sets.newHashSet( unit3 ) ) );
        assertFalse( unit4.isDescendant( Sets.newHashSet( unit1 ) ) );
    }

    @Test
    public void testIsDescendantObject()
    {
        OrganisationUnit unit1 = createOrganisationUnit( '1' );
        organisationUnitService.addOrganisationUnit( unit1 );

        OrganisationUnit unit2 = createOrganisationUnit( '2', unit1 );
        unit1.getChildren().add( unit2 );
        organisationUnitService.addOrganisationUnit( unit2 );

        OrganisationUnit unit3 = createOrganisationUnit( '3', unit2 );
        unit2.getChildren().add( unit3 );
        organisationUnitService.addOrganisationUnit( unit3 );

        OrganisationUnit unit4 = createOrganisationUnit( '4' );
        organisationUnitService.addOrganisationUnit( unit4 );
        
        assertTrue( unit1.isDescendant( unit1 ) );
        assertTrue( unit2.isDescendant( unit1 ) );
        assertTrue( unit3.isDescendant( unit1 ) );
        
        assertFalse( unit2.isDescendant( unit3 ) );
        assertFalse( unit4.isDescendant( unit1 ) );
    }
    
    @Test
    public void testGetOrganisationUnitAtLevelAndBranch()
        throws Exception
    {
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B', unitA );
        OrganisationUnit unitC = createOrganisationUnit( 'C', unitA );
        OrganisationUnit unitD = createOrganisationUnit( 'D', unitB );
        OrganisationUnit unitE = createOrganisationUnit( 'E', unitB );
        OrganisationUnit unitF = createOrganisationUnit( 'F', unitC );
        OrganisationUnit unitG = createOrganisationUnit( 'G', unitC );
        OrganisationUnit unitH = createOrganisationUnit( 'H', unitD );
        OrganisationUnit unitI = createOrganisationUnit( 'I', unitD );
        OrganisationUnit unitJ = createOrganisationUnit( 'J', unitE );
        OrganisationUnit unitK = createOrganisationUnit( 'K', unitE );
        OrganisationUnit unitL = createOrganisationUnit( 'L', unitF );
        OrganisationUnit unitM = createOrganisationUnit( 'M', unitF );
        OrganisationUnit unitN = createOrganisationUnit( 'N', unitG );
        OrganisationUnit unitO = createOrganisationUnit( 'O', unitG );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );
        organisationUnitService.addOrganisationUnit( unitF );
        organisationUnitService.addOrganisationUnit( unitG );
        organisationUnitService.addOrganisationUnit( unitH );
        organisationUnitService.addOrganisationUnit( unitI );
        organisationUnitService.addOrganisationUnit( unitJ );
        organisationUnitService.addOrganisationUnit( unitK );
        organisationUnitService.addOrganisationUnit( unitL );
        organisationUnitService.addOrganisationUnit( unitM );
        organisationUnitService.addOrganisationUnit( unitN );
        organisationUnitService.addOrganisationUnit( unitO );

        assertEquals( Sets.newHashSet( unitB ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevel( 2, unitB ) ) );
        assertEquals( Sets.newHashSet( unitD, unitE ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevel( 3, unitB ) ) );
        assertEquals( Sets.newHashSet( unitH, unitI, unitJ, unitK ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevel( 4, unitB ) ) );

        assertEquals( 2, unitB.getLevel() );
        assertEquals( 3, unitD.getLevel() );
        assertEquals( 3, unitE.getLevel() );
        assertEquals( 4, unitH.getLevel() );
        assertEquals( 4, unitI.getLevel() );
        assertEquals( 4, unitJ.getLevel() );
        assertEquals( 4, unitK.getLevel() );
    }

    @Test
    public void testGetOrganisationUnitAtLevelAndBranches()
    {
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B', unitA );
        OrganisationUnit unitC = createOrganisationUnit( 'C', unitA );
        OrganisationUnit unitD = createOrganisationUnit( 'D', unitB );
        OrganisationUnit unitE = createOrganisationUnit( 'E', unitB );
        OrganisationUnit unitF = createOrganisationUnit( 'F', unitC );
        OrganisationUnit unitG = createOrganisationUnit( 'G', unitC );
        OrganisationUnit unitH = createOrganisationUnit( 'H', unitD );
        OrganisationUnit unitI = createOrganisationUnit( 'I', unitD );
        OrganisationUnit unitJ = createOrganisationUnit( 'J', unitE );
        OrganisationUnit unitK = createOrganisationUnit( 'K', unitE );
        OrganisationUnit unitL = createOrganisationUnit( 'L', unitF );
        OrganisationUnit unitM = createOrganisationUnit( 'M', unitF );
        OrganisationUnit unitN = createOrganisationUnit( 'N', unitG );
        OrganisationUnit unitO = createOrganisationUnit( 'O', unitG );

        unitA.getChildren().add( unitB );
        unitA.getChildren().add( unitC );
        unitB.getChildren().add( unitD );
        unitB.getChildren().add( unitE );
        unitC.getChildren().add( unitF );
        unitC.getChildren().add( unitG );
        unitD.getChildren().add( unitH );
        unitD.getChildren().add( unitI );
        unitE.getChildren().add( unitJ );
        unitE.getChildren().add( unitK );
        unitF.getChildren().add( unitL );
        unitF.getChildren().add( unitM );
        unitG.getChildren().add( unitN );
        unitG.getChildren().add( unitO );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );
        organisationUnitService.addOrganisationUnit( unitF );
        organisationUnitService.addOrganisationUnit( unitG );
        organisationUnitService.addOrganisationUnit( unitH );
        organisationUnitService.addOrganisationUnit( unitI );
        organisationUnitService.addOrganisationUnit( unitJ );
        organisationUnitService.addOrganisationUnit( unitK );
        organisationUnitService.addOrganisationUnit( unitL );
        organisationUnitService.addOrganisationUnit( unitM );
        organisationUnitService.addOrganisationUnit( unitN );
        organisationUnitService.addOrganisationUnit( unitO );

        List<OrganisationUnit> unitsA = new ArrayList<>( Arrays.asList( unitB, unitC ) );
        List<OrganisationUnit> unitsB = new ArrayList<>( Arrays.asList( unitD, unitE ) );

        assertEquals( Sets.newHashSet( unitD, unitE, unitF, unitG ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevels( Sets.newHashSet( 3 ), unitsA ) ) );
        assertEquals( Sets.newHashSet( unitH, unitI, unitJ, unitK, unitL, unitM, unitN, unitO ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevels( Sets.newHashSet( 4 ), unitsA ) ) );
        assertEquals( Sets.newHashSet( unitH, unitI, unitJ, unitK ), Sets.newHashSet( organisationUnitService.getOrganisationUnitsAtLevels( Sets.newHashSet( 4 ), unitsB ) ) );

        assertEquals( 2, unitB.getLevel() );
        assertEquals( 3, unitD.getLevel() );
        assertEquals( 3, unitE.getLevel() );
        assertEquals( 4, unitH.getLevel() );
        assertEquals( 4, unitI.getLevel() );
        assertEquals( 4, unitJ.getLevel() );
        assertEquals( 4, unitK.getLevel() );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndDelOrganisationUnitGroup()
    {
        OrganisationUnitGroup organisationUnitGroup1 = new OrganisationUnitGroup( "OUGname" );

        int id1 = organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup1 );

        // assert getOrganisationUnitGroup
        assertNotNull( organisationUnitGroupService.getOrganisationUnitGroup( id1 ) );

        assertEquals( organisationUnitGroupService.getOrganisationUnitGroup( id1 ).getName(), "OUGname" );

        organisationUnitGroupService.deleteOrganisationUnitGroup( organisationUnitGroupService
            .getOrganisationUnitGroup( id1 ) );

        // assert delOrganisationUnitGroup
        assertNull( organisationUnitGroupService.getOrganisationUnitGroup( id1 ) );
    }

    @Test
    @Ignore
    public void testUpdateOrganisationUnitGroup()
    {
        OrganisationUnitGroup organisationUnitGroup = new OrganisationUnitGroup( "OUGname" );

        OrganisationUnit organisationUnit1 = new OrganisationUnit( "OU1name", null, "OU1sname", "OU1code", null, null, null );
        OrganisationUnit organisationUnit2 = new OrganisationUnit( "OU2name", null, "OU2sname", "OU2code", null, null, null );

        organisationUnitGroup.getMembers().add( organisationUnit1 );
        organisationUnitGroup.getMembers().add( organisationUnit2 );

        organisationUnitService.addOrganisationUnit( organisationUnit1 );
        organisationUnitService.addOrganisationUnit( organisationUnit2 );

        int ougid = organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup );

        assertTrue( organisationUnitGroupService.getOrganisationUnitGroup( ougid ).getMembers().size() == 2 );

        organisationUnitGroup.getMembers().remove( organisationUnit1 );

        organisationUnitGroupService.updateOrganisationUnitGroup( organisationUnitGroup );

        assertTrue( organisationUnitGroupService.getOrganisationUnitGroup( ougid ).getMembers().size() == 1 );
    }

    @Test
    public void testGetAllOrganisationUnitGroups()
    {
        OrganisationUnitGroup group1 = new OrganisationUnitGroup( "organisationUnitGroupName1" );
        OrganisationUnitGroup group2 = new OrganisationUnitGroup( "organisationUnitGroupName2" );
        OrganisationUnitGroup group3 = new OrganisationUnitGroup( "organisationUnitGroupName3" );
        OrganisationUnitGroup group4 = new OrganisationUnitGroup( "organisationUnitGroupName4" );

        List<OrganisationUnitGroup> groups = new ArrayList<>();
        groups.add( group1 );
        groups.add( group2 );
        groups.add( group3 );
        groups.add( group4 );

        ArrayList<Integer> groupIds = new ArrayList<>();

        for ( OrganisationUnitGroup group : groups )
        {
            groupIds.add( organisationUnitGroupService.addOrganisationUnitGroup( group ) );
        }

        List<OrganisationUnitGroup> fetchedGroups = organisationUnitGroupService.getAllOrganisationUnitGroups();

        ArrayList<Integer> fetchedGroupIds = new ArrayList<>();

        for ( OrganisationUnitGroup group : fetchedGroups )
        {
            fetchedGroupIds.add( group.getId() );
        }

        assertTrue( fetchedGroups.size() == 4 );
        assertTrue( fetchedGroups.containsAll( groups ));

        assertTrue( fetchedGroupIds.size() == 4 );
        assertTrue( fetchedGroupIds.containsAll( groupIds ) );
    }

    @Test
    public void testGetOrganisationUnitGroupByName()
    {
        String oUG1Name = "OUG1Name";
        String oUG2Name = "OUG2Name";

        OrganisationUnitGroup organisationUnitGroup1 = new OrganisationUnitGroup( oUG1Name );
        OrganisationUnitGroup organisationUnitGroup2 = new OrganisationUnitGroup( oUG2Name );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup1 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup2 );

        OrganisationUnitGroup group1 = organisationUnitGroupService.getOrganisationUnitGroupByName( oUG1Name ).get( 0 );
        assertEquals( group1.getName(), oUG1Name );

        OrganisationUnitGroup group2 = organisationUnitGroupService.getOrganisationUnitGroupByName( oUG2Name ).get( 0 );
        assertEquals( group2.getName(), oUG2Name );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitHierarchy
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetOrganisationUnitHierarchy()
    {
        // creates a tree
        OrganisationUnit unit1 = new OrganisationUnit( "orgUnitName1", "shortName1", "organisationUnitCode1",
            new Date(), new Date(), "comment" );
        OrganisationUnit unit2 = new OrganisationUnit( "orgUnitName2", unit1, "shortName2", "organisationUnitCode2",
            new Date(), new Date(), "comment" );
        OrganisationUnit unit3 = new OrganisationUnit( "orgUnitName3", unit1, "shortName3", "organisationUnitCode3",
            new Date(), new Date(), "comment" );
        OrganisationUnit unit4 = new OrganisationUnit( "orgUnitName4", unit2, "shortName4", "organisationUnitCode4",
            new Date(), new Date(), "comment" );
        OrganisationUnit unit5 = new OrganisationUnit( "orgUnitName5", unit2, "shortName5", "organisationUnitCode5",
            new Date(), new Date(), "comment" );
        OrganisationUnit unit6 = new OrganisationUnit( "orgUnitName6", unit5, "shortName6", "organisationUnitCode6",
            new Date(), new Date(), "comment" );

        organisationUnitService.addOrganisationUnit( unit1 );
        int id2 = organisationUnitService.addOrganisationUnit( unit2 );
        organisationUnitService.addOrganisationUnit( unit3 );
        int id4 = organisationUnitService.addOrganisationUnit( unit4 );
        int id5 = organisationUnitService.addOrganisationUnit( unit5 );
        int id6 = organisationUnitService.addOrganisationUnit( unit6 );

        OrganisationUnitHierarchy hierarchy = organisationUnitService.getOrganisationUnitHierarchy();

        // retrieves children from hierarchyVersion ver_id and parentId id2
        Collection<Integer> children1 = hierarchy.getChildren( unit2.getId() );

        // assert 4, 5, 6 are children of 2
        assertEquals( 4, children1.size() );
        assertTrue( children1.contains( id2 ) );
        assertTrue( children1.contains( id4 ) );
        assertTrue( children1.contains( id5 ) );
        assertTrue( children1.contains( id6 ) );

        // retrieves children from hierarchyVersion ver_id and parentId id1
        Collection<Integer> children2 = hierarchy.getChildren( unit1.getId() );

        // assert the number of children
        assertTrue( children2.size() == 6 );

        // retrieves children from hierarchyVersion ver_id and parentId id5
        Collection<Integer> children3 = hierarchy.getChildren( unit5.getId() );

        // assert 6 is children of 5
        assertEquals( 2, children3.size() );
        assertTrue( children3.contains( id5 ) );
        assertTrue( children3.contains( id6 ) );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitGroupSets
    // -------------------------------------------------------------------------

    @Test
    public void testOrganisationUnitGroupSetsBasic()
    {
        OrganisationUnitGroup organisationUnitGroup1 = new OrganisationUnitGroup();
        organisationUnitGroup1.setName( "oug1" );
        OrganisationUnitGroup organisationUnitGroup2 = new OrganisationUnitGroup();
        organisationUnitGroup2.setName( "oug2" );
        OrganisationUnitGroup organisationUnitGroup3 = new OrganisationUnitGroup();
        organisationUnitGroup3.setName( "oug3" );
        OrganisationUnitGroup organisationUnitGroup4 = new OrganisationUnitGroup();
        organisationUnitGroup4.setName( "oug4" );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup1 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup2 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup3 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup4 );

        OrganisationUnitGroupSet organisationUnitGroupSet1 = new OrganisationUnitGroupSet();
        organisationUnitGroupSet1.setName( "ougs1" );
        organisationUnitGroupSet1.setCompulsory( true );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup1 );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup2 );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup3 );

        int id1 = organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSet1 );

        // assert add
        assertNotNull( organisationUnitGroupService.getOrganisationUnitGroupSet( id1 ) );

        assertEquals( organisationUnitGroupService.getOrganisationUnitGroupSet( id1 ).getName(), "ougs1" );

        assertTrue( organisationUnitGroupService.getOrganisationUnitGroupSet( id1 ).getOrganisationUnitGroups().size() == 3 );

        organisationUnitGroupSet1.getOrganisationUnitGroups().remove( organisationUnitGroup3 );

        organisationUnitGroupService.updateOrganisationUnitGroupSet( organisationUnitGroupSet1 );

        // assert update
        assertTrue( organisationUnitGroupService.getOrganisationUnitGroupSet( id1 ).getOrganisationUnitGroups().size() == 2 );

        OrganisationUnitGroupSet organisationUnitGroupSet2 = new OrganisationUnitGroupSet();
        organisationUnitGroupSet2.setName( "ougs2" );
        organisationUnitGroupSet2.setCompulsory( true );
        organisationUnitGroupSet2.getOrganisationUnitGroups().add( organisationUnitGroup4 );

        int id2 = organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSet2 );

        // assert getAllOrderedName
        assertTrue( organisationUnitGroupService.getAllOrganisationUnitGroupSets().size() == 2 );

        organisationUnitGroupService.deleteOrganisationUnitGroupSet( organisationUnitGroupSet1 );
        organisationUnitGroupService.deleteOrganisationUnitGroupSet( organisationUnitGroupSet2 );

        assertNull( organisationUnitGroupService.getOrganisationUnitGroupSet( id1 ) );
        assertNull( organisationUnitGroupService.getOrganisationUnitGroupSet( id2 ) );
    }

    @Test
    public void testGetOrganisationUnitGroupSetsByName()
    {
        OrganisationUnitGroup organisationUnitGroup1 = new OrganisationUnitGroup();
        organisationUnitGroup1.setName( "oug1" );
        OrganisationUnitGroup organisationUnitGroup2 = new OrganisationUnitGroup();
        organisationUnitGroup2.setName( "oug2" );
        OrganisationUnitGroup organisationUnitGroup3 = new OrganisationUnitGroup();
        organisationUnitGroup3.setName( "oug3" );
        OrganisationUnitGroup organisationUnitGroup4 = new OrganisationUnitGroup();
        organisationUnitGroup4.setName( "oug4" );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup1 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup2 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup3 );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup4 );

        String ougs1 = "ougs1";
        String ougs2 = "ougs2";

        OrganisationUnitGroupSet organisationUnitGroupSet1 = new OrganisationUnitGroupSet();
        organisationUnitGroupSet1.setName( ougs1 );
        organisationUnitGroupSet1.setCompulsory( true );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup1 );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup2 );
        organisationUnitGroupSet1.getOrganisationUnitGroups().add( organisationUnitGroup3 );

        OrganisationUnitGroupSet organisationUnitGroupSet2 = new OrganisationUnitGroupSet();
        organisationUnitGroupSet2.setName( ougs2 );
        organisationUnitGroupSet2.setCompulsory( false );
        organisationUnitGroupSet2.getOrganisationUnitGroups().add( organisationUnitGroup4 );

        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSet1 );
        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSet2 );

        OrganisationUnitGroupSet set1 = organisationUnitGroupService.getOrganisationUnitGroupSetByName( ougs1 ).get( 0 );
        OrganisationUnitGroupSet set2 = organisationUnitGroupService.getOrganisationUnitGroupSetByName( ougs2 ).get( 0 );

        assertEquals( set1.getName(), ougs1 );
        assertEquals( set2.getName(), ougs2 );

        List<OrganisationUnitGroupSet> compulsorySets = organisationUnitGroupService
            .getCompulsoryOrganisationUnitGroupSets();
        assertEquals( compulsorySets.size(), 1 );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitLevel
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetOrganisationUnitLevel()
    {
        OrganisationUnitLevel levelA = new OrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = new OrganisationUnitLevel( 2, "District" );

        int idA = organisationUnitService.addOrganisationUnitLevel( levelA );
        int idB = organisationUnitService.addOrganisationUnitLevel( levelB );

        assertEquals( levelA, organisationUnitService.getOrganisationUnitLevel( idA ) );
        assertEquals( levelB, organisationUnitService.getOrganisationUnitLevel( idB ) );
    }

    @Test
    public void testGetOrganisationUnitLevels()
    {
        OrganisationUnitLevel level1 = new OrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel level2 = new OrganisationUnitLevel( 2, "District" );
        OrganisationUnitLevel level4 = new OrganisationUnitLevel( 4, "PHU" );

        organisationUnitService.addOrganisationUnitLevel( level1 );
        organisationUnitService.addOrganisationUnitLevel( level2 );
        organisationUnitService.addOrganisationUnitLevel( level4 );

        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B', unitA );
        OrganisationUnit unitC = createOrganisationUnit( 'C', unitB );
        OrganisationUnit unitD = createOrganisationUnit( 'D', unitC );

        unitA.getChildren().add( unitB );
        unitB.getChildren().add( unitC );
        unitC.getChildren().add( unitD );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );

        Iterator<OrganisationUnitLevel> actual = organisationUnitService.getOrganisationUnitLevels().iterator();

        assertNotNull( actual );
        assertEquals( level1, actual.next() );
        assertEquals( level2, actual.next() );

        level4 = actual.next();

        assertEquals( 4, level4.getLevel() );
        assertEquals( "PHU", level4.getName() );
    }

    @Test
    public void testRemoveOrganisationUnitLevel()
    {
        OrganisationUnitLevel levelA = new OrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = new OrganisationUnitLevel( 2, "District" );

        int idA = organisationUnitService.addOrganisationUnitLevel( levelA );
        int idB = organisationUnitService.addOrganisationUnitLevel( levelB );

        assertNotNull( organisationUnitService.getOrganisationUnitLevel( idA ) );
        assertNotNull( organisationUnitService.getOrganisationUnitLevel( idB ) );

        organisationUnitService.deleteOrganisationUnitLevel( levelA );

        assertNull( organisationUnitService.getOrganisationUnitLevel( idA ) );
        assertNotNull( organisationUnitService.getOrganisationUnitLevel( idB ) );

        organisationUnitService.deleteOrganisationUnitLevel( levelB );

        assertNull( organisationUnitService.getOrganisationUnitLevel( idA ) );
        assertNull( organisationUnitService.getOrganisationUnitLevel( idB ) );
    }

    @Test
    public void testIsInUserHierarchy()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B', ouA );
        OrganisationUnit ouC = createOrganisationUnit( 'C', ouA );
        OrganisationUnit ouD = createOrganisationUnit( 'D', ouB );
        OrganisationUnit ouE = createOrganisationUnit( 'E', ouB );
        OrganisationUnit ouF = createOrganisationUnit( 'F', ouC );
        OrganisationUnit ouG = createOrganisationUnit( 'G', ouC );

        ouA.getChildren().add( ouB );
        ouA.getChildren().add( ouC );
        ouB.getChildren().add( ouD );
        ouB.getChildren().add( ouE );
        ouC.getChildren().add( ouF );
        ouC.getChildren().add( ouG );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );
        organisationUnitService.addOrganisationUnit( ouF );
        organisationUnitService.addOrganisationUnit( ouG );

        User user = createUser( 'A' );
        Set<OrganisationUnit> organisationUnits = Sets.newHashSet( ouB );
        user.setOrganisationUnits( organisationUnits );
        
        assertTrue( organisationUnitService.isInUserHierarchy( ouB.getUid(), organisationUnits ) );
        assertTrue( organisationUnitService.isInUserHierarchy( ouD.getUid(), organisationUnits ) );
        assertTrue( organisationUnitService.isInUserHierarchy( ouE.getUid(), organisationUnits ) );

        assertFalse( organisationUnitService.isInUserHierarchy( ouA.getUid(), organisationUnits ) );
        assertFalse( organisationUnitService.isInUserHierarchy( ouC.getUid(), organisationUnits ) );
        assertFalse( organisationUnitService.isInUserHierarchy( ouF.getUid(), organisationUnits ) );
        assertFalse( organisationUnitService.isInUserHierarchy( ouG.getUid(), organisationUnits ) );        
    }

    @Test
    public void testGetAncestorUids()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B', ouA );
        OrganisationUnit ouC = createOrganisationUnit( 'C', ouB );
        OrganisationUnit ouD = createOrganisationUnit( 'D', ouC );

        ouA.getChildren().add( ouB );
        ouA.getChildren().add( ouC );
        ouB.getChildren().add( ouD );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        
        List<String> expected = IdentifiableObjectUtils.getUids( Arrays.asList( ouA, ouB, ouC ) );

        assertEquals( expected, ouD.getAncestorUids( null ) );
        assertEquals( expected, ouD.getAncestorUids( Sets.newHashSet( ouA.getUid() ) ) );
        
        expected = IdentifiableObjectUtils.getUids( Arrays.asList( ouB, ouC ) );

        assertEquals( expected, ouD.getAncestorUids( Sets.newHashSet( ouB.getUid() ) ) );
    }
    
    @Test
    public void testGetParentGraph()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B', ouA );
        OrganisationUnit ouC = createOrganisationUnit( 'C', ouB );
        OrganisationUnit ouD = createOrganisationUnit( 'D', ouC );

        ouA.getChildren().add( ouB );
        ouA.getChildren().add( ouC );
        ouB.getChildren().add( ouD );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        
        String expected = ouA.getUid() + "/" + ouB.getUid() + "/" + ouC.getUid();
        
        assertEquals( expected, ouD.getParentGraph( null ) );
        assertEquals( expected, ouD.getParentGraph( Sets.newHashSet( ouA ) ) );

        expected = ouB.getUid() + "/" + ouC.getUid();
        
        assertEquals( expected, ouD.getParentGraph( Sets.newHashSet( ouB ) ) );        
    }
}
