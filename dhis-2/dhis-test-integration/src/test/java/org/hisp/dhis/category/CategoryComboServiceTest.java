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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
class CategoryComboServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryManager categoryManager;

    private CategoryOption categoryOptionA;

    private CategoryOption categoryOptionB;

    private CategoryOption categoryOptionC;

    private CategoryOption categoryOptionD;

    private CategoryOption categoryOptionE;

    private CategoryOption categoryOptionF;

    private CategoryOption categoryOptionG;

    private Category categoryA;

    private Category categoryB;

    private Category categoryC;

    private CategoryCombo categoryComboA;

    private CategoryCombo categoryComboB;

    private CategoryCombo categoryComboC;

    private List<Category> categories;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        categories = new ArrayList<>();
        categoryOptionA = new CategoryOption( "OptionA" );
        categoryOptionB = new CategoryOption( "OptionB" );
        categoryOptionC = new CategoryOption( "OptionC" );
        categoryOptionD = new CategoryOption( "OptionD" );
        categoryOptionE = new CategoryOption( "OptionE" );
        categoryOptionF = new CategoryOption( "OptionF" );
        categoryOptionG = new CategoryOption( "OptionG" );
        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        categoryService.addCategoryOption( categoryOptionD );
        categoryService.addCategoryOption( categoryOptionE );
        categoryService.addCategoryOption( categoryOptionF );
        categoryService.addCategoryOption( categoryOptionG );
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryB = createCategory( 'B', categoryOptionC, categoryOptionD );
        categoryC = createCategory( 'C', categoryOptionE, categoryOptionF );
        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        categoryService.addCategory( categoryC );
        categories.add( categoryA );
        categories.add( categoryB );
        categories.add( categoryC );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testAddGet()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new CategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        long idA = categoryService.addCategoryCombo( categoryComboA );
        long idB = categoryService.addCategoryCombo( categoryComboB );
        long idC = categoryService.addCategoryCombo( categoryComboC );
        assertEquals( categoryComboA, categoryService.getCategoryCombo( idA ) );
        assertEquals( categoryComboB, categoryService.getCategoryCombo( idB ) );
        assertEquals( categoryComboC, categoryService.getCategoryCombo( idC ) );
        assertEquals( categories, categoryService.getCategoryCombo( idA ).getCategories() );
        assertEquals( categories, categoryService.getCategoryCombo( idB ).getCategories() );
        assertEquals( categories, categoryService.getCategoryCombo( idC ).getCategories() );
    }

    @Test
    void testDelete()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new CategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        long idA = categoryService.addCategoryCombo( categoryComboA );
        long idB = categoryService.addCategoryCombo( categoryComboB );
        long idC = categoryService.addCategoryCombo( categoryComboC );
        assertNotNull( categoryService.getCategoryCombo( idA ) );
        assertNotNull( categoryService.getCategoryCombo( idB ) );
        assertNotNull( categoryService.getCategoryCombo( idC ) );
        categoryService.deleteCategoryCombo( categoryComboA );
        assertNull( categoryService.getCategoryCombo( idA ) );
        assertNotNull( categoryService.getCategoryCombo( idB ) );
        assertNotNull( categoryService.getCategoryCombo( idC ) );
        categoryService.deleteCategoryCombo( categoryComboB );
        assertNull( categoryService.getCategoryCombo( idA ) );
        assertNull( categoryService.getCategoryCombo( idB ) );
        assertNotNull( categoryService.getCategoryCombo( idC ) );
    }

    @Test
    void testDeleteCategoryComboLinkedToCategory()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        long idA = categoryService.addCategoryCombo( categoryComboA );
        long idB = categoryService.addCategoryCombo( categoryComboB );
        long catIdA = categoryA.getId();
        long catIdB = categoryB.getId();
        assertNotNull( categoryService.getCategoryCombo( idA ) );
        assertNotNull( categoryService.getCategoryCombo( idB ) );
        categoryA.setCategoryCombos( Lists.newArrayList( categoryComboA, categoryComboB ) );
        categoryB.setCategoryCombos( Lists.newArrayList( categoryComboA, categoryComboB ) );
        categoryService.updateCategory( categoryA );
        categoryService.updateCategory( categoryB );
        categoryService.deleteCategoryCombo( categoryComboA );
        assertNull( categoryService.getCategoryCombo( idA ) );
        assertNotNull( categoryService.getCategoryCombo( idB ) );
        assertNotNull( categoryService.getCategory( catIdA ) );
        assertNotNull( categoryService.getCategory( catIdB ) );
        assertTrue( categoryService.getCategoryCombo( idB ).getCategories().contains( categoryA ) );
        assertTrue( categoryService.getCategoryCombo( idB ).getCategories().contains( categoryB ) );
        assertFalse( categoryService.getCategory( catIdA ).getCategoryCombos().contains( categoryComboA ) );
        assertTrue( categoryService.getCategory( catIdA ).getCategoryCombos().contains( categoryComboB ) );
        assertFalse( categoryService.getCategory( catIdB ).getCategoryCombos().contains( categoryComboA ) );
        assertTrue( categoryService.getCategory( catIdB ).getCategoryCombos().contains( categoryComboB ) );
    }

    @Test
    void testDeleteCategory()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.addCategoryCombo( categoryComboB );
        categoryA.setCategoryCombos( Lists.newArrayList( categoryComboA, categoryComboB ) );
        categoryService.updateCategory( categoryA );
        assertThrows( DeleteNotAllowedException.class, () -> categoryService.deleteCategory( categoryA ) );
    }

    @Test
    void testGetAll()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new CategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.addCategoryCombo( categoryComboB );
        categoryService.addCategoryCombo( categoryComboC );
        List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();
        // Including default
        assertEquals( 4, categoryCombos.size() );
        assertTrue( categoryCombos.contains( categoryComboA ) );
        assertTrue( categoryCombos.contains( categoryComboB ) );
        assertTrue( categoryCombos.contains( categoryComboC ) );
    }

    @Test
    void testGenerateCategoryOptionCombos()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.generateOptionCombos( categoryComboA );
        Set<CategoryOptionCombo> optionCombos = categoryComboA.getOptionCombos();
        assertEquals( 8, optionCombos.size() );
        assertOptionCombos( optionCombos );
    }

    @Test
    void testUpdateCategoryOptionCombosA()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.generateOptionCombos( categoryComboA );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        categoryC.addCategoryOption( categoryOptionG );
        categoryService.updateCategory( categoryC );
        categoryService.updateOptionCombos( categoryComboA );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 12, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionG ) ) );
    }

    @Test
    void testUpdateCategoryOptionCombosB()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.generateOptionCombos( categoryComboA );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        categoryService.updateOptionCombos( categoryComboA );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
    }

    @Test
    void testUpdateCategoryOptionCombosC()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.generateOptionCombos( categoryComboA );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        categoryC.addCategoryOption( categoryOptionG );
        categoryService.updateCategory( categoryC );
        categoryService.updateOptionCombos( categoryC );
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 12, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionG ) ) );
    }

    @Test
    void testAddAndPruneCategoryOptionCombo()
    {
        categories.clear();
        categories.add( categoryA );
        categories.add( categoryB );
        CategoryCombo categoryComboT = new CategoryCombo( "CategoryComboT", DataDimensionType.DISAGGREGATION,
            categories );
        long id = categoryService.addCategoryCombo( categoryComboT );
        categoryComboT = categoryService.getCategoryCombo( id );
        assertNotNull( categoryComboT );
        assertEquals( categories, categoryComboT.getCategories() );
        categoryManager.addAndPruneAllOptionCombos();
        assertTrue( categoryComboT.getOptionCombos()
            .contains( createCategoryOptionCombo( categoryComboT, categoryOptionA, categoryOptionC ) ) );
        assertFalse( categoryComboT.getOptionCombos()
            .contains( createCategoryOptionCombo( categoryComboT, categoryOptionA, categoryOptionE ) ) );
        categoryB.removeCategoryOption( categoryOptionC );
        categoryB.addCategoryOption( categoryOptionE );
        categoryService.updateCategory( categoryB );
        categoryManager.addAndPruneAllOptionCombos();
        categoryComboT = categoryService.getCategoryCombo( id );
        assertFalse( categoryComboT.getOptionCombos()
            .contains( createCategoryOptionCombo( categoryComboT, categoryOptionA, categoryOptionC ) ) );
        assertTrue( categoryComboT.getOptionCombos()
            .contains( createCategoryOptionCombo( categoryComboT, categoryOptionA, categoryOptionE ) ) );
    }

    private void assertOptionCombos( Set<CategoryOptionCombo> optionCombos )
    {
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionE ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionF ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionE ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionF ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionE ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionF ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionE ) ) );
        assertTrue( optionCombos.contains(
            createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionF ) ) );
    }
}
