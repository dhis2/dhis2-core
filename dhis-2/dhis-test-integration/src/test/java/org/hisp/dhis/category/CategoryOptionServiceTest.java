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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class CategoryOptionServiceTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private CategoryService categoryService;

    private CategoryOption categoryOptionA;

    private CategoryOption categoryOptionB;

    private CategoryOption categoryOptionC;

    private Category categoryA;

    private Category categoryB;

    private Category categoryC;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testAddGet()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );
        categoryOptionA.setDescription( "Test" );
        long idA = categoryService.addCategoryOption( categoryOptionA );
        long idB = categoryService.addCategoryOption( categoryOptionB );
        long idC = categoryService.addCategoryOption( categoryOptionC );
        assertEquals( categoryOptionA, categoryService.getCategoryOption( idA ) );
        assertEquals( categoryOptionB, categoryService.getCategoryOption( idB ) );
        assertEquals( categoryOptionC, categoryService.getCategoryOption( idC ) );
        assertEquals( "Test", categoryService.getCategoryOption( idA ).getDescription() );
    }

    @Test
    void testDelete()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );
        long idA = categoryService.addCategoryOption( categoryOptionA );
        long idB = categoryService.addCategoryOption( categoryOptionB );
        long idC = categoryService.addCategoryOption( categoryOptionC );
        assertNotNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOption( idC ) );
        categoryService.deleteCategoryOption( categoryOptionA );
        assertNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOption( idC ) );
        categoryService.deleteCategoryOption( categoryOptionB );
        assertNull( categoryService.getCategoryOption( idA ) );
        assertNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOption( idC ) );
    }

    @Test
    void testDeleteCategoryOptionInCategoryOptionGroup()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        long idA = categoryService.addCategoryOption( categoryOptionA );
        long idB = categoryService.addCategoryOption( categoryOptionB );
        CategoryOptionGroup categoryOptionGroup = createCategoryOptionGroup( 'A', categoryOptionA, categoryOptionB );
        long groupId = categoryService.saveCategoryOptionGroup( categoryOptionGroup );
        categoryOptionA.setGroups( Sets.newHashSet( categoryOptionGroup ) );
        categoryOptionB.setGroups( Sets.newHashSet( categoryOptionGroup ) );
        categoryService.updateCategoryOption( categoryOptionA );
        categoryService.updateCategoryOption( categoryOptionB );
        assertNotNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOptionGroup( groupId ) );
        categoryService.deleteCategoryOption( categoryOptionA );
        assertNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOptionGroup( groupId ) );
        assertFalse( categoryOptionGroup.getMembers().contains( categoryOptionA ) );
        assertTrue( categoryOptionGroup.getMembers().contains( categoryOptionB ) );
    }

    @Test
    void testDeleteCategoryOptionGroupInCategoryOptionGroupSet()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        long idA = categoryService.addCategoryOption( categoryOptionA );
        long idB = categoryService.addCategoryOption( categoryOptionB );
        CategoryOptionGroup categoryOptionGroupA = createCategoryOptionGroup( 'A', categoryOptionA );
        CategoryOptionGroup categoryOptionGroupB = createCategoryOptionGroup( 'B', categoryOptionB );
        long groupIdA = categoryService.saveCategoryOptionGroup( categoryOptionGroupA );
        long groupIdB = categoryService.saveCategoryOptionGroup( categoryOptionGroupB );
        categoryOptionA.setGroups( Sets.newHashSet( categoryOptionGroupA ) );
        categoryOptionB.setGroups( Sets.newHashSet( categoryOptionGroupB ) );
        categoryService.updateCategoryOption( categoryOptionA );
        categoryService.updateCategoryOption( categoryOptionB );
        CategoryOptionGroupSet categoryOptionGroupSet = createCategoryOptionGroupSet( 'A', categoryOptionGroupA,
            categoryOptionGroupB );
        long groupSetId = categoryService.saveCategoryOptionGroupSet( categoryOptionGroupSet );
        assertNotNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNotNull( categoryService.getCategoryOptionGroup( groupIdA ) );
        assertNotNull( categoryService.getCategoryOptionGroup( groupIdB ) );
        assertNotNull( categoryService.getCategoryOptionGroupSet( groupSetId ) );
        categoryService.deleteCategoryOptionGroup( categoryOptionGroupA );
        assertNotNull( categoryService.getCategoryOption( idA ) );
        assertNotNull( categoryService.getCategoryOption( idB ) );
        assertNull( categoryService.getCategoryOptionGroup( groupIdA ) );
        assertNotNull( categoryService.getCategoryOptionGroup( groupIdB ) );
        assertFalse( categoryOptionGroupSet.getMembers().contains( categoryOptionGroupA ) );
        assertTrue( categoryOptionGroupSet.getMembers().contains( categoryOptionGroupB ) );
    }

    @Test
    void testGetAll()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );
        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        List<CategoryOption> categoryOptions = categoryService.getAllCategoryOptions();
        // Including default
        assertEquals( 4, categoryOptions.size() );
        assertTrue( categoryOptions.contains( categoryOptionA ) );
        assertTrue( categoryOptions.contains( categoryOptionB ) );
        assertTrue( categoryOptions.contains( categoryOptionC ) );
    }

    @Test
    void testGetByCategory()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );
        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryB = createCategory( 'B', categoryOptionC );
        categoryC = createCategory( 'C' );
        Set<Category> categoriesA = new HashSet<>();
        Set<Category> categoriesB = new HashSet<>();
        categoriesA.add( categoryA );
        categoriesB.add( categoryB );
        categoryOptionA.setCategories( categoriesA );
        categoryOptionB.setCategories( categoriesA );
        categoryOptionC.setCategories( categoriesB );
        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        categoryService.addCategory( categoryC );
        List<CategoryOption> categoryOptions = categoryService.getCategoryOptions( categoryA );
        assertEquals( 2, categoryOptions.size() );
        categoryOptions = categoryService.getCategoryOptions( categoryB );
        assertEquals( 1, categoryOptions.size() );
        categoryOptions = categoryService.getCategoryOptions( categoryC );
        assertEquals( 0, categoryOptions.size() );
    }
}
