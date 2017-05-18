package org.hisp.dhis.dataelement;

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

import org.hisp.dhis.DhisTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

import java.util.List;

/**
 * Test needs to extend DhisTest in order to test the bidirectional group set
 * to group association from both sides as save transactions must commit.
 * 
 * @author Lars Helge Overland
 */
public class CategoryOptionGroupStoreTest
    extends DhisTest
{
    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private CategoryOptionGroupStore categoryOptionGroupStore;
    
    private DataElementCategoryOption coA;
    private DataElementCategoryOption coB;
    private DataElementCategoryOption coC;
    private DataElementCategoryOption coD;
    private DataElementCategoryOption coE;
    private DataElementCategoryOption coF;
    private DataElementCategoryOption coG;
    private DataElementCategoryOption coH;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    protected void setUpTest()
    {
        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );
        coC = createCategoryOption( 'C' );
        coD = createCategoryOption( 'D' );
        coE = createCategoryOption( 'E' );
        coF = createCategoryOption( 'F' );
        coG = createCategoryOption( 'G' );
        coH = createCategoryOption( 'H' );
        
        categoryService.addDataElementCategoryOption( coA );
        categoryService.addDataElementCategoryOption( coB );
        categoryService.addDataElementCategoryOption( coC );
        categoryService.addDataElementCategoryOption( coD );
        categoryService.addDataElementCategoryOption( coE );
        categoryService.addDataElementCategoryOption( coF );
        categoryService.addDataElementCategoryOption( coG );
        categoryService.addDataElementCategoryOption( coH );
    }

    @Override
    protected boolean emptyDatabaseAfterTest()
    {
        return true;
    }
    
    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        CategoryOptionGroup cogA = createCategoryOptionGroup( 'A', coA, coB );
        CategoryOptionGroup cogB = createCategoryOptionGroup( 'B', coC, coD );
        CategoryOptionGroup cogC = createCategoryOptionGroup( 'C', coE, coF );
        CategoryOptionGroup cogD = createCategoryOptionGroup( 'D', coG, coH );
        
        categoryOptionGroupStore.save( cogA );
        categoryOptionGroupStore.save( cogB );
        categoryOptionGroupStore.save( cogC );
        categoryOptionGroupStore.save( cogD );
        
        assertEquals( cogA, categoryOptionGroupStore.get( cogA.getId() ) );
        assertTrue( cogA.getMembers().contains( coA ) );
        assertTrue( cogA.getMembers().contains( coB ) );
        
        assertEquals( cogB, categoryOptionGroupStore.get( cogB.getId() ) );
        assertTrue( cogB.getMembers().contains( coC ) );
        assertTrue( cogB.getMembers().contains( coD ) );
        
        assertEquals( cogC, categoryOptionGroupStore.get( cogC.getId() ) );
        assertTrue( cogC.getMembers().contains( coE ) );
        assertTrue( cogC.getMembers().contains( coF ) );
        
        assertEquals( cogD, categoryOptionGroupStore.get( cogD.getId() ) );
        assertTrue( cogD.getMembers().contains( coG ) );
        assertTrue( cogD.getMembers().contains( coH ) );
    }

    @Test
    public void testGetByGroupSet()
    {
        CategoryOptionGroup cogA = createCategoryOptionGroup( 'A', coA, coB );
        CategoryOptionGroup cogB = createCategoryOptionGroup( 'B', coC, coD );
        CategoryOptionGroup cogC = createCategoryOptionGroup( 'C', coE, coF );
        CategoryOptionGroup cogD = createCategoryOptionGroup( 'D', coG, coH );
        
        categoryOptionGroupStore.save( cogA );
        categoryOptionGroupStore.save( cogB );
        categoryOptionGroupStore.save( cogC );
        categoryOptionGroupStore.save( cogD );
        
        CategoryOptionGroupSet cogsA = createCategoryOptionGroupSet( 'A', cogA, cogB );
        CategoryOptionGroupSet cogsB = createCategoryOptionGroupSet( 'B', cogC, cogD );
        
        categoryService.saveCategoryOptionGroupSet( cogsA );
        categoryService.saveCategoryOptionGroupSet( cogsB );
        
        assertEquals( 1, cogA.getGroupSets().size() );
        assertEquals( cogsA, cogA.getGroupSets().iterator().next() );
        assertEquals( 1, cogB.getGroupSets().size() );
        assertEquals( cogsA, cogB.getGroupSets().iterator().next() );
        
        List<CategoryOptionGroup> groupsA = categoryOptionGroupStore.getCategoryOptionGroups( cogsA );
        assertEquals( 2, groupsA.size() );
        assertTrue( groupsA.contains( cogA ) );
        assertTrue( groupsA.contains( cogB ) );

        List<CategoryOptionGroup> groupsB = categoryOptionGroupStore.getCategoryOptionGroups( cogsB );
        assertEquals( 2, groupsB.size() );
        assertTrue( groupsB.contains( cogC ) );
        assertTrue( groupsB.contains( cogD ) );        
    }
}

