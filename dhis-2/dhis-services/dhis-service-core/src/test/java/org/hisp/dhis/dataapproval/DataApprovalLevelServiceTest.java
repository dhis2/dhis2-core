package org.hisp.dhis.dataapproval;

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

import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_UNAPPROVED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
public class DataApprovalLevelServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private CategoryOptionGroupSet setA;
    private CategoryOptionGroupSet setB;
    private CategoryOptionGroupSet setC;
    private CategoryOptionGroupSet setD;

    private DataApprovalLevel level1;
    private DataApprovalLevel level1A;
    private DataApprovalLevel level1B;
    private DataApprovalLevel level1C;
    private DataApprovalLevel level1D;

    private DataApprovalLevel level2;
    private DataApprovalLevel level2A;
    private DataApprovalLevel level2B;
    private DataApprovalLevel level2C;
    private DataApprovalLevel level2D;

    private DataApprovalLevel level3;
    private DataApprovalLevel level3A;
    private DataApprovalLevel level3B;
    private DataApprovalLevel level3C;

    private DataApprovalLevel level4;
    private DataApprovalLevel level4A;
    private DataApprovalLevel level4B;
    private DataApprovalLevel level4D;

    private DataApprovalLevel level5;

    private OrganisationUnit organisationUnitA;
    private OrganisationUnit organisationUnitB;
    private OrganisationUnit organisationUnitC;
    private OrganisationUnit organisationUnitD;
    private OrganisationUnit organisationUnitE;
    private OrganisationUnit organisationUnitF;
    private OrganisationUnit organisationUnitG;
    private OrganisationUnit organisationUnitH;
    private OrganisationUnit organisationUnitI;
    private OrganisationUnit organisationUnitJ;
    private OrganisationUnit organisationUnitK;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        setA = new CategoryOptionGroupSet( "Set A" );
        setB = new CategoryOptionGroupSet( "Set B" );
        setC = new CategoryOptionGroupSet( "Set C" );
        setD = new CategoryOptionGroupSet( "Set D" );

        categoryService.saveCategoryOptionGroupSet( setA );
        categoryService.saveCategoryOptionGroupSet( setB );
        categoryService.saveCategoryOptionGroupSet( setC );
        categoryService.saveCategoryOptionGroupSet( setD );

        level1 = new DataApprovalLevel( "01", 1, null );
        level1A = new DataApprovalLevel( "1A", 1, setA );
        level1B = new DataApprovalLevel( "1B", 1, setB );
        level1C = new DataApprovalLevel( "1C", 1, setC );
        level1D = new DataApprovalLevel( "1D", 1, setD );

        level2 = new DataApprovalLevel( "02", 2, null );
        level2A = new DataApprovalLevel( "2A", 2, setA );
        level2B = new DataApprovalLevel( "2B", 2, setB );
        level2C = new DataApprovalLevel( "2C", 2, setC );
        level2D = new DataApprovalLevel( "2D", 2, setD );

        level3 = new DataApprovalLevel( "03", 3, null );
        level3A = new DataApprovalLevel( "3A", 3, setA );
        level3B = new DataApprovalLevel( "3B", 3, setB );
        level3C = new DataApprovalLevel( "3C", 3, setC );

        level4 = new DataApprovalLevel( "04", 4, null );
        level4A = new DataApprovalLevel( "4A", 4, setA );
        level4B = new DataApprovalLevel( "4B", 4, setB );
        level4D = new DataApprovalLevel( "4D", 4, setD );

        level5 = new DataApprovalLevel( "05", 5, null );

        //
        // Org       Organisation
        // unit      unit
        // level:    hierarchy:
        //
        //   1           A
        //               |
        //   2           B
        //             / | \
        //   3       C   F   I
        //           |   |   |
        //   4       D   G   J
        //           |   |   |
        //   5       E   H   K
        //
        // Note: E through K are optionally added by the test if desired.

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B', organisationUnitA );
        organisationUnitC = createOrganisationUnit( 'C', organisationUnitB );
        organisationUnitD = createOrganisationUnit( 'D', organisationUnitC );
        organisationUnitE = createOrganisationUnit( 'E', organisationUnitD );

        organisationUnitF = createOrganisationUnit( 'F', organisationUnitB );
        organisationUnitG = createOrganisationUnit( 'G', organisationUnitF );
        organisationUnitH = createOrganisationUnit( 'H', organisationUnitG );

        organisationUnitI = createOrganisationUnit( 'I', organisationUnitB );
        organisationUnitJ = createOrganisationUnit( 'J', organisationUnitI );
        organisationUnitK = createOrganisationUnit( 'K', organisationUnitJ );

        organisationUnitService.addOrganisationUnit( organisationUnitA );
        organisationUnitService.addOrganisationUnit( organisationUnitB );
        organisationUnitService.addOrganisationUnit( organisationUnitC );
        organisationUnitService.addOrganisationUnit( organisationUnitD );
    }
    
    // -------------------------------------------------------------------------
    // Basic DataApprovalLevel
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataApprovalLevel()
    {
        dataApprovalLevelService.addDataApprovalLevel( level2C, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level3B, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level4A, 4 );
        
        List<DataApprovalLevel> levels = dataApprovalLevelService.getAllDataApprovalLevels();
        assertEquals( 4, levels.size() );

        assertEquals( 2, levels.get( 0 ).getOrgUnitLevel() );
        assertEquals( "Set C", levels.get( 0 ).getCategoryOptionGroupSet().getName() );
        assertEquals( "2C", levels.get( 0 ).getName() );

        assertEquals( 3, levels.get( 1 ).getOrgUnitLevel() );
        assertNull( levels.get( 1 ).getCategoryOptionGroupSet() );
        assertEquals( "03", levels.get( 1 ).getName() );

        assertEquals( 3, levels.get( 2 ).getOrgUnitLevel() );
        assertEquals( "Set B", levels.get( 2 ).getCategoryOptionGroupSet().getName() );
        assertEquals( "3B", levels.get( 2 ).getName() );

        assertEquals( 4, levels.get( 3 ).getOrgUnitLevel() );
        assertEquals( "Set A", levels.get( 3 ).getCategoryOptionGroupSet().getName() );
        assertEquals( "4A", levels.get( 3 ).getName() );
    }

    @Test
    public void testDeleteDataApprovalLevel()
    {
        int id1 = dataApprovalLevelService.addDataApprovalLevel( level1A, 1 );
        int id2 = dataApprovalLevelService.addDataApprovalLevel( level2B, 2 );
        int id3 = dataApprovalLevelService.addDataApprovalLevel( level3C, 3 );
        int id4 = dataApprovalLevelService.addDataApprovalLevel( level4D, 4 );

        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id1 ) );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id2 ) );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id3 ) );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id4 ) );

        dataApprovalLevelService.deleteDataApprovalLevel( level2B );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id1 ) );
        assertNull( dataApprovalLevelService.getDataApprovalLevel( id2 ) );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id3 ) );
        assertNotNull( dataApprovalLevelService.getDataApprovalLevel( id4 ) );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getAllDataApprovalLevels();
        assertEquals( 3, levels.size() );
        assertEquals( 1, levels.get( 0 ).getLevel() );
        assertEquals( 2, levels.get( 1 ).getLevel() );
        assertEquals( 3, levels.get( 2 ).getLevel() );
    }

    @Test
    public void testExists()
    {
        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level1A, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level1B, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level2A, 5 );
        dataApprovalLevelService.addDataApprovalLevel( level2B, 6 );

        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level1A ) );
        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level1A ) );
        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level2A ) );
        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level2B ) );
        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level2 ) );
        assertTrue( dataApprovalLevelService.dataApprovalLevelExists( level1 ) );

        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level3 ) );
        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level4 ) );
        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level1C ) );
        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level1D ) );
        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level2C ) );
        assertFalse( dataApprovalLevelService.dataApprovalLevelExists( level2D ) );
    }

    @Test
    public void testCanMoveDown()
    {
        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level1A, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level2A, 5 );
        dataApprovalLevelService.addDataApprovalLevel( level2B, 6 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 7 );
        dataApprovalLevelService.addDataApprovalLevel( level3A, 8 );
        dataApprovalLevelService.addDataApprovalLevel( level3B, 9 );

        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( -1 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 0 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 1 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveDown( 2 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 3 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 4 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveDown( 5 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 6 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 7 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveDown( 8 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 9 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 10 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveDown( 11 ) );
    }

    @Test
    public void testCanMoveUp()
    {
        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level1A, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level2A, 5 );
        dataApprovalLevelService.addDataApprovalLevel( level2B, 6 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 7 );
        dataApprovalLevelService.addDataApprovalLevel( level3A, 8 );
        dataApprovalLevelService.addDataApprovalLevel( level3B, 9 );

        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( -1 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 0 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 1 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 2 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveUp( 3 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 4 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 5 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveUp( 6 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 7 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 8 ) );
        assertTrue( dataApprovalLevelService.canDataApprovalLevelMoveUp( 9 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 10 ) );
        assertFalse( dataApprovalLevelService.canDataApprovalLevelMoveUp( 11 ) );
    }

    @Test
    public void testMoveDown()
    {
        int id1 = dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        int id2 = dataApprovalLevelService.addDataApprovalLevel( level1A, 2 );
        int id3 = dataApprovalLevelService.addDataApprovalLevel( level1B, 3 );
        int id4 = dataApprovalLevelService.addDataApprovalLevel( level1C, 4 );
        int id5 = dataApprovalLevelService.addDataApprovalLevel( level1D, 5 );

        assertEquals( 1, dataApprovalLevelService.getDataApprovalLevel( id1 ).getLevel() );
        assertEquals( 2, dataApprovalLevelService.getDataApprovalLevel( id2 ).getLevel() );
        assertEquals( 3, dataApprovalLevelService.getDataApprovalLevel( id3 ).getLevel() );
        assertEquals( 4, dataApprovalLevelService.getDataApprovalLevel( id4 ).getLevel() );
        assertEquals( 5, dataApprovalLevelService.getDataApprovalLevel( id5 ).getLevel() );
        
        dataApprovalLevelService.moveDataApprovalLevelDown( 2 );

        assertEquals( 1, dataApprovalLevelService.getDataApprovalLevel( id1 ).getLevel() );
        assertEquals( 2, dataApprovalLevelService.getDataApprovalLevel( id3 ).getLevel() );
        assertEquals( 3, dataApprovalLevelService.getDataApprovalLevel( id2 ).getLevel() );
        assertEquals( 4, dataApprovalLevelService.getDataApprovalLevel( id4 ).getLevel() );
        assertEquals( 5, dataApprovalLevelService.getDataApprovalLevel( id5 ).getLevel() );
    }

    @Test
    public void testMoveUp()
    {
        int id1 = dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        int id2 = dataApprovalLevelService.addDataApprovalLevel( level1A, 2 );
        int id3 = dataApprovalLevelService.addDataApprovalLevel( level1B, 3 );
        int id4 = dataApprovalLevelService.addDataApprovalLevel( level1C, 4 );
        int id5 = dataApprovalLevelService.addDataApprovalLevel( level1D, 5 );

        assertEquals( 1, dataApprovalLevelService.getDataApprovalLevel( id1 ).getLevel() );
        assertEquals( 2, dataApprovalLevelService.getDataApprovalLevel( id2 ).getLevel() );
        assertEquals( 3, dataApprovalLevelService.getDataApprovalLevel( id3 ).getLevel() );
        assertEquals( 4, dataApprovalLevelService.getDataApprovalLevel( id4 ).getLevel() );
        assertEquals( 5, dataApprovalLevelService.getDataApprovalLevel( id5 ).getLevel() );
        
        dataApprovalLevelService.moveDataApprovalLevelUp( 5 );

        assertEquals( 1, dataApprovalLevelService.getDataApprovalLevel( id1 ).getLevel() );
        assertEquals( 2, dataApprovalLevelService.getDataApprovalLevel( id2 ).getLevel() );
        assertEquals( 3, dataApprovalLevelService.getDataApprovalLevel( id3 ).getLevel() );
        assertEquals( 4, dataApprovalLevelService.getDataApprovalLevel( id5 ).getLevel() );
        assertEquals( 5, dataApprovalLevelService.getDataApprovalLevel( id4 ).getLevel() );
    }

    @Test
    public void testGetUserReadApprovalLevels_1A()
    {
        //
        // Test 1: Like when a user may capture data within their own district
        // but view data in other districts within their province.
        //
        // Variation A: User does *not* have approval at lower levels authority.
        //
        organisationUnitService.addOrganisationUnit( organisationUnitE );
        organisationUnitService.addOrganisationUnit( organisationUnitF );
        organisationUnitService.addOrganisationUnit( organisationUnitG );
        organisationUnitService.addOrganisationUnit( organisationUnitH );

        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level4, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level5, 5 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitC );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        
        Map<OrganisationUnit, Integer> readApprovalLevels = dataApprovalLevelService.getUserReadApprovalLevels();
        assertEquals( 2, readApprovalLevels.size() );

        assertEquals( 4, (int) readApprovalLevels.get( organisationUnitC ) );
        assertEquals( 3, (int) readApprovalLevels.get( organisationUnitB ) );
    }

    @Test
    public void testGetUserReadApprovalLevels_1B()
    {
        //
        // Test 1: Like when a user may capture data within their own district
        // but view data in other districts within their province.
        //
        // Variation B: User *has* approval at lower levels authority.
        //
        organisationUnitService.addOrganisationUnit( organisationUnitE );
        organisationUnitService.addOrganisationUnit( organisationUnitF );
        organisationUnitService.addOrganisationUnit( organisationUnitG );
        organisationUnitService.addOrganisationUnit( organisationUnitH );

        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level4, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level5, 5 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitC );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        
        Map<OrganisationUnit, Integer> readApprovalLevels = dataApprovalLevelService.getUserReadApprovalLevels();
        assertEquals( 2, readApprovalLevels.size() );

        assertEquals( APPROVAL_LEVEL_UNAPPROVED, (int) readApprovalLevels.get( organisationUnitC ) );
        assertEquals( 3, (int) readApprovalLevels.get( organisationUnitB ) );
    }

    @Test
    public void testGetUserReadApprovalLevels_1C()
    {
        //
        // Test 1: Like when a user may capture data within their own district
        // but view data in other districts within their province.
        //
        // Variation C: No approval level for org unit level 4.
        //
        organisationUnitService.addOrganisationUnit( organisationUnitE );
        organisationUnitService.addOrganisationUnit( organisationUnitF );
        organisationUnitService.addOrganisationUnit( organisationUnitG );
        organisationUnitService.addOrganisationUnit( organisationUnitH );

        dataApprovalLevelService.addDataApprovalLevel( level1, 1 ); // 1st approval level
        dataApprovalLevelService.addDataApprovalLevel( level2, 2 ); // 2nd approval level
        dataApprovalLevelService.addDataApprovalLevel( level3, 3 ); // 3rd approval level
        dataApprovalLevelService.addDataApprovalLevel( level5, 4 ); // 4th approval level

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitC );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        
        Map<OrganisationUnit, Integer> readApprovalLevels = dataApprovalLevelService.getUserReadApprovalLevels();
        assertEquals( 2, readApprovalLevels.size() );

        assertEquals( 4, (int) readApprovalLevels.get( organisationUnitC ) );
        assertEquals( 3, (int) readApprovalLevels.get( organisationUnitB ) );
    }

    @Test
    public void testGetUserReadApprovalLevels_1D()
    {
        //
        // Test 1: Like when a user may capture data within their own district
        // but view data in other districts within their province.
        //
        // Variation D: User is assigned to two districts
        //
        organisationUnitService.addOrganisationUnit( organisationUnitE );
        organisationUnitService.addOrganisationUnit( organisationUnitF );
        organisationUnitService.addOrganisationUnit( organisationUnitG );
        organisationUnitService.addOrganisationUnit( organisationUnitH );
        organisationUnitService.addOrganisationUnit( organisationUnitI );
        organisationUnitService.addOrganisationUnit( organisationUnitJ );
        organisationUnitService.addOrganisationUnit( organisationUnitK );

        dataApprovalLevelService.addDataApprovalLevel( level1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( level2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( level3, 3 );
        dataApprovalLevelService.addDataApprovalLevel( level4, 4 );
        dataApprovalLevelService.addDataApprovalLevel( level5, 5 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitC );
        assignedOrgUnits.add( organisationUnitF );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        
        Map<OrganisationUnit, Integer> readApprovalLevels = dataApprovalLevelService.getUserReadApprovalLevels();
        assertEquals( 3, readApprovalLevels.size() );

        assertEquals( 4, (int) readApprovalLevels.get( organisationUnitC ) );
        assertEquals( 4, (int) readApprovalLevels.get( organisationUnitF ) );
        assertEquals( 3, (int) readApprovalLevels.get( organisationUnitB ) );
    }

    //TODO: add tests for getUserDataApprovalLevels where the user can access the CategoryOptionGroupSets

    @Test
    public void testGetUserDataApprovalLevelsApproveHere()
    {
        dataApprovalLevelService.addDataApprovalLevel( level4B );
        dataApprovalLevelService.addDataApprovalLevel( level4A );
        dataApprovalLevelService.addDataApprovalLevel( level4 );
        dataApprovalLevelService.addDataApprovalLevel( level3B );
        dataApprovalLevelService.addDataApprovalLevel( level3A );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        dataApprovalLevelService.addDataApprovalLevel( level2B );
        dataApprovalLevelService.addDataApprovalLevel( level2A );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B );
        dataApprovalLevelService.addDataApprovalLevel( level1A );
        dataApprovalLevelService.addDataApprovalLevel( level1 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitB );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_APPROVE );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        assertEquals( "02 2A 2B 03 3A 3B 04 4A 4B", levelNames( levels ) );
    }

    @Test
    public void testGetUserDataApprovalLevelsApproveLower()
    {
        dataApprovalLevelService.addDataApprovalLevel( level4B );
        dataApprovalLevelService.addDataApprovalLevel( level4A );
        dataApprovalLevelService.addDataApprovalLevel( level4 );
        dataApprovalLevelService.addDataApprovalLevel( level3B );
        dataApprovalLevelService.addDataApprovalLevel( level3A );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        dataApprovalLevelService.addDataApprovalLevel( level2B );
        dataApprovalLevelService.addDataApprovalLevel( level2A );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B );
        dataApprovalLevelService.addDataApprovalLevel( level1A );
        dataApprovalLevelService.addDataApprovalLevel( level1 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitB );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        assertEquals( "02 2A 2B 03 3A 3B 04 4A 4B", levelNames( levels ) );
    }

    @Test
    public void testGetUserDataApprovalLevelsApproveHereAndLower()
    {
        dataApprovalLevelService.addDataApprovalLevel( level4B );
        dataApprovalLevelService.addDataApprovalLevel( level4A );
        dataApprovalLevelService.addDataApprovalLevel( level4 );
        dataApprovalLevelService.addDataApprovalLevel( level3B );
        dataApprovalLevelService.addDataApprovalLevel( level3A );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        dataApprovalLevelService.addDataApprovalLevel( level2B );
        dataApprovalLevelService.addDataApprovalLevel( level2A );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B );
        dataApprovalLevelService.addDataApprovalLevel( level1A );
        dataApprovalLevelService.addDataApprovalLevel( level1 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitB );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        assertEquals( "02 2A 2B 03 3A 3B 04 4A 4B", levelNames( levels ) );
    }

    @Test
    public void testGetUserDataApprovalLevelsAcceptLower()
    {
        dataApprovalLevelService.addDataApprovalLevel( level4B );
        dataApprovalLevelService.addDataApprovalLevel( level4A );
        dataApprovalLevelService.addDataApprovalLevel( level4 );
        dataApprovalLevelService.addDataApprovalLevel( level3B );
        dataApprovalLevelService.addDataApprovalLevel( level3A );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        dataApprovalLevelService.addDataApprovalLevel( level2B );
        dataApprovalLevelService.addDataApprovalLevel( level2A );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level1B );
        dataApprovalLevelService.addDataApprovalLevel( level1A );
        dataApprovalLevelService.addDataApprovalLevel( level1 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitB );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        assertEquals( "02 2A 2B 03 3A 3B 04 4A 4B", levelNames( levels ) );
    }

    @Test
    public void testGetUserDataApprovalLevelsAcceptMuchLower()
    {
        dataApprovalLevelService.addDataApprovalLevel( level4 );

        Set<OrganisationUnit> assignedOrgUnits = new HashSet<>();
        assignedOrgUnits.add( organisationUnitB );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>();
        dataViewOrgUnits.add( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( assignedOrgUnits, dataViewOrgUnits, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        assertEquals( "04", levelNames( levels ) );
    }

    private String levelNames( List<DataApprovalLevel> levels )
    {
        String names = "";

        for ( DataApprovalLevel level : levels )
        {
            names += (names.isEmpty() ? "" : " ") + level.getName();
        }

        return names;
    }
}
