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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DataElementCategoryServiceTest
    extends DhisSpringTest
{
    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategoryOption categoryOptionC;

    private DataElementCategory categoryA;
    private DataElementCategory categoryB;
    private DataElementCategory categoryC;

    private List<DataElementCategoryOption> categoryOptions;

    @Autowired
    private DataElementCategoryService categoryService;
    
    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );
        categoryOptionC = createCategoryOption( 'C' );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );

        categoryOptions = new ArrayList<>();

        categoryOptions.add( categoryOptionA );
        categoryOptions.add( categoryOptionB );
        categoryOptions.add( categoryOptionC );
    }

    // -------------------------------------------------------------------------
    // Category
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        categoryA = createDataElementCategory( 'A', categoryOptionA, categoryOptionB, categoryOptionC );
        categoryB = createDataElementCategory( 'B', categoryOptionA, categoryOptionB, categoryOptionC );
        categoryC = createDataElementCategory( 'C', categoryOptionA, categoryOptionB, categoryOptionC );

        int idA = categoryService.addDataElementCategory( categoryA );
        int idB = categoryService.addDataElementCategory( categoryB );
        int idC = categoryService.addDataElementCategory( categoryC );

        assertEquals( categoryA, categoryService.getDataElementCategory( idA ) );
        assertEquals( categoryB, categoryService.getDataElementCategory( idB ) );
        assertEquals( categoryC, categoryService.getDataElementCategory( idC ) );

        assertEquals( categoryOptions, categoryService.getDataElementCategory( idA ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryService.getDataElementCategory( idB ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryService.getDataElementCategory( idC ).getCategoryOptions() );
    }

    @Test
    public void testDelete()
    {
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION, categoryOptions );

        int idA = categoryService.addDataElementCategory( categoryA );
        int idB = categoryService.addDataElementCategory( categoryB );
        int idC = categoryService.addDataElementCategory( categoryC );

        assertNotNull( categoryService.getDataElementCategory( idA ) );
        assertNotNull( categoryService.getDataElementCategory( idB ) );
        assertNotNull( categoryService.getDataElementCategory( idC ) );

        categoryService.deleteDataElementCategory( categoryA );

        assertNull( categoryService.getDataElementCategory( idA ) );
        assertNotNull( categoryService.getDataElementCategory( idB ) );
        assertNotNull( categoryService.getDataElementCategory( idC ) );

        categoryService.deleteDataElementCategory( categoryB );

        assertNull( categoryService.getDataElementCategory( idA ) );
        assertNull( categoryService.getDataElementCategory( idB ) );
        assertNotNull( categoryService.getDataElementCategory( idC ) );
    }

    @Test
    public void testGetAll()
    {
        categoryA = createDataElementCategory( 'A' );
        categoryB = createDataElementCategory( 'B' );
        categoryC = createDataElementCategory( 'C' );

        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );
        categoryService.addDataElementCategory( categoryC );

        List<DataElementCategory> categories = categoryService.getAllDataElementCategories();

        assertEquals( 4, categories.size() ); // Including default
        assertTrue( categories.contains( categoryA ) );
        assertTrue( categories.contains( categoryB ) );
        assertTrue( categories.contains( categoryC ) );
    }

    // -------------------------------------------------------------------------
    // CategoryOptionGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetCategoryGroup()
    {
        CategoryOptionGroup groupA = createCategoryOptionGroup( 'A' );
        CategoryOptionGroup groupB = createCategoryOptionGroup( 'B' );
        CategoryOptionGroup groupC = createCategoryOptionGroup( 'C' );

        groupA.getMembers().add( categoryOptionA );
        groupA.getMembers().add( categoryOptionB );
        groupB.getMembers().add( categoryOptionC );

        int idA = categoryService.saveCategoryOptionGroup( groupA );
        int idB = categoryService.saveCategoryOptionGroup( groupB );
        int idC = categoryService.saveCategoryOptionGroup( groupC );

        assertEquals( groupA, categoryService.getCategoryOptionGroup( idA ) );
        assertEquals( groupB, categoryService.getCategoryOptionGroup( idB ) );
        assertEquals( groupC, categoryService.getCategoryOptionGroup( idC ) );

        assertEquals( 2, categoryService.getCategoryOptionGroup( idA ).getMembers().size() );
        assertEquals( 1, categoryService.getCategoryOptionGroup( idB ).getMembers().size() );
        assertEquals( 0, categoryService.getCategoryOptionGroup( idC ).getMembers().size() );
    }

    // -------------------------------------------------------------------------
    // CategoryOptionGroupSet
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetCategoryGroupSet()
    {
        CategoryOptionGroup groupA = createCategoryOptionGroup( 'A' );
        CategoryOptionGroup groupB = createCategoryOptionGroup( 'B' );
        CategoryOptionGroup groupC = createCategoryOptionGroup( 'C' );

        groupA.getMembers().add( categoryOptionA );
        groupA.getMembers().add( categoryOptionB );
        groupB.getMembers().add( categoryOptionC );

        categoryService.saveCategoryOptionGroup( groupA );
        categoryService.saveCategoryOptionGroup( groupB );
        categoryService.saveCategoryOptionGroup( groupC );

        CategoryOptionGroupSet groupSetA = createCategoryOptionGroupSet( 'A' );
        CategoryOptionGroupSet groupSetB = createCategoryOptionGroupSet( 'B' );
        CategoryOptionGroupSet groupSetC = createCategoryOptionGroupSet( 'C' );

        groupSetA.getMembers().add( groupA );
        groupSetA.getMembers().add( groupB );
        groupSetB.getMembers().add( groupC );

        int idA = categoryService.saveCategoryOptionGroupSet( groupSetA );
        int idB = categoryService.saveCategoryOptionGroupSet( groupSetB );
        int idC = categoryService.saveCategoryOptionGroupSet( groupSetC );

        assertEquals( groupSetA, categoryService.getCategoryOptionGroupSet( idA ) );
        assertEquals( groupSetB, categoryService.getCategoryOptionGroupSet( idB ) );
        assertEquals( groupSetC, categoryService.getCategoryOptionGroupSet( idC ) );

        assertEquals( 2, categoryService.getCategoryOptionGroupSet( idA ).getMembers().size() );
        assertEquals( 1, categoryService.getCategoryOptionGroupSet( idB ).getMembers().size() );
        assertEquals( 0, categoryService.getCategoryOptionGroupSet( idC ).getMembers().size() );
    }
}
