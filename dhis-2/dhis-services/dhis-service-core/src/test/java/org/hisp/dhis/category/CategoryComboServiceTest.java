package org.hisp.dhis.category;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class CategoryComboServiceTest
    extends DhisSpringTest
{
    @Autowired
    private CategoryService categoryService;
    
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
        
        categoryA = new Category( "CategoryA", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "CategoryB", DataDimensionType.DISAGGREGATION );
        categoryC = new Category( "CategoryC", DataDimensionType.DISAGGREGATION );
        
        categoryA.addCategoryOption( categoryOptionA );
        categoryA.addCategoryOption( categoryOptionB );
        categoryB.addCategoryOption( categoryOptionC );
        categoryB.addCategoryOption( categoryOptionD );
        categoryC.addCategoryOption( categoryOptionE );
        categoryC.addCategoryOption( categoryOptionF );
        
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
    public void testAddGet()
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
    public void testDelete()
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
    public void testGetAll()
    {
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new CategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.addCategoryCombo( categoryComboB );
        categoryService.addCategoryCombo( categoryComboC );
        
        List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();
        
        assertEquals( 4, categoryCombos.size() ); // Including default
        assertTrue( categoryCombos.contains( categoryComboA ) );
        assertTrue( categoryCombos.contains( categoryComboB ) );
        assertTrue( categoryCombos.contains( categoryComboC ) );        
    }

    @Test
    public void testGenerateCategoryOptionCombos()
    {        
        categoryComboA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addCategoryCombo( categoryComboA );
        
        categoryService.generateOptionCombos( categoryComboA );
        
        Set<CategoryOptionCombo> optionCombos = categoryComboA.getOptionCombos();
        
        assertEquals( 8, optionCombos.size() );
        
        assertOptionCombos( optionCombos );
    }
    
    @Test
    public void testUpdateCategoryOptionCombosA()
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
        
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionG ) ) );
    }

    @Test
    public void testUpdateCategoryOptionCombosB()
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
    public void testUpdateCategoryOptionCombosC()
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
        
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionG ) ) );
    }
  
    private void assertOptionCombos( Set<CategoryOptionCombo> optionCombos )
    {
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionE ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionF ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionE ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionF ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionE ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionF ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionE ) ) );
        assertTrue( optionCombos.contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionF ) ) );
    }
}
