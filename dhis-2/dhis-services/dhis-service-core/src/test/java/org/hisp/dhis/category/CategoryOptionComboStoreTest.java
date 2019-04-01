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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class CategoryOptionComboStoreTest
    extends DhisSpringTest
{
    @Autowired
    private CategoryOptionComboStore categoryOptionComboStore;
    
    @Autowired
    private CategoryService categoryService;
    
    private Category categoryA;
    private Category categoryB;
        
    private CategoryCombo categoryComboA;
    private CategoryCombo categoryComboB;
    
    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;
    private CategoryOption categoryOptionC;
    private CategoryOption categoryOptionD;
    
    private CategoryOptionCombo categoryOptionComboA;
    private CategoryOptionCombo categoryOptionComboB;
    private CategoryOptionCombo categoryOptionComboC;
    
    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {  
        categoryOptionA = new CategoryOption( "Male" );
        categoryOptionB = new CategoryOption( "Female" );
        categoryOptionC = new CategoryOption( "0-20" );
        categoryOptionD = new CategoryOption( "20-100" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        categoryService.addCategoryOption( categoryOptionD );
                
        categoryA = new Category( "Gender", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "Agegroup", DataDimensionType.DISAGGREGATION );
        
        categoryA.addCategoryOption( categoryOptionA );
        categoryA.addCategoryOption( categoryOptionB );
        categoryB.addCategoryOption( categoryOptionC );
        categoryB.addCategoryOption( categoryOptionD );
        
        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        
        categoryComboA = new CategoryCombo( "GenderAgegroup", DataDimensionType.DISAGGREGATION );
        categoryComboB = new CategoryCombo( "Gender", DataDimensionType.DISAGGREGATION );
        
        categoryComboA.addCategory( categoryA );
        categoryComboA.addCategory( categoryB );
        categoryComboB.addCategory( categoryA );
        
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.addCategoryCombo( categoryComboB ); 
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetCategoryOptionCombo()
    {
        categoryOptionComboA = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );        
        
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );        
        
        categoryOptionComboStore.save( categoryOptionComboA );
        long id = categoryOptionComboA.getId();

        categoryOptionComboA = categoryOptionComboStore.get( id );
        
        assertNotNull( categoryOptionComboA );
        assertEquals( categoryComboA, categoryOptionComboA.getCategoryCombo() );
        assertEquals( categoryOptions, categoryOptionComboA.getCategoryOptions() );
    }

    @Test
    public void testUpdateGetCategoryOptionCombo()
    {
        categoryOptionComboA = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );        
        
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );        
        
        categoryOptionComboStore.save( categoryOptionComboA );
        long id = categoryOptionComboA.getId();

        assertNotNull( categoryOptionComboStore.get( id ) );
        assertEquals( categoryComboA, categoryOptionComboA.getCategoryCombo() );
        assertEquals( categoryOptions, categoryOptionComboA.getCategoryOptions() );
        
        categoryOptionComboA.setCategoryCombo( categoryComboB );
        
        categoryOptionComboStore.update( categoryOptionComboA );
        
        categoryOptionComboA = categoryOptionComboStore.get( id );
        
        assertNotNull( categoryOptionComboA );
        assertEquals( categoryComboB, categoryOptionComboA.getCategoryCombo() );
        assertEquals( categoryOptions, categoryOptionComboA.getCategoryOptions() );
    }

    @Test
    public void testDeleteCategoryOptionCombo()
    {
        categoryOptionComboA = new CategoryOptionCombo();
        categoryOptionComboB = new CategoryOptionCombo();
        categoryOptionComboC = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );     
        
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboB.setCategoryCombo( categoryComboA );
        categoryOptionComboC.setCategoryCombo( categoryComboA );
        
        categoryOptionComboA.setCategoryOptions( categoryOptions );
        categoryOptionComboB.setCategoryOptions( categoryOptions );
        categoryOptionComboC.setCategoryOptions( categoryOptions );

        categoryOptionComboStore.save( categoryOptionComboA );
        long idA = categoryOptionComboA.getId();
        categoryOptionComboStore.save( categoryOptionComboB );
        long idB = categoryOptionComboB.getId();
        categoryOptionComboStore.save( categoryOptionComboC );
        long idC = categoryOptionComboC.getId();

        assertNotNull( categoryOptionComboStore.get( idA ) );
        assertNotNull( categoryOptionComboStore.get( idB ) );
        assertNotNull( categoryOptionComboStore.get( idC ) );
        
        categoryOptionComboStore.delete( categoryOptionComboStore.get( idA ) );

        assertNull( categoryOptionComboStore.get( idA ) );
        assertNotNull( categoryOptionComboStore.get( idB ) );
        assertNotNull( categoryOptionComboStore.get( idC ) );

        categoryOptionComboStore.delete( categoryOptionComboStore.get( idB ) );

        assertNull( categoryOptionComboStore.get( idA ) );
        assertNull( categoryOptionComboStore.get( idB ) );
        assertNotNull( categoryOptionComboStore.get( idC ) );

        categoryOptionComboStore.delete( categoryOptionComboStore.get( idC ) );

        assertNull( categoryOptionComboStore.get( idA ) );
        assertNull( categoryOptionComboStore.get( idB ) );
        assertNull( categoryOptionComboStore.get( idC ) );
    }

    @Test
    public void testGetAllCategoryOptionCombos()
    {
        categoryOptionComboA = new CategoryOptionCombo();
        categoryOptionComboB = new CategoryOptionCombo();
        categoryOptionComboC = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );     
        
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboB.setCategoryCombo( categoryComboA );
        categoryOptionComboC.setCategoryCombo( categoryComboA );
        
        categoryOptionComboA.setCategoryOptions( categoryOptions );
        categoryOptionComboB.setCategoryOptions( categoryOptions );
        categoryOptionComboC.setCategoryOptions( categoryOptions );

        categoryOptionComboStore.save( categoryOptionComboA );
        categoryOptionComboStore.save( categoryOptionComboB );
        categoryOptionComboStore.save( categoryOptionComboC );
        
        List<CategoryOptionCombo> categoryOptionCombos = 
            categoryOptionComboStore.getAll();
        
        assertNotNull( categoryOptionCombos );
        assertEquals( 4, categoryOptionCombos.size() ); // Including default
    }
    
    @Test
    public void testGenerateCategoryOptionCombos()
    {
        categoryService.generateOptionCombos( categoryComboA );
        categoryService.generateOptionCombos( categoryComboB );
        
        List<CategoryOptionCombo> optionCombos = categoryService.getAllCategoryOptionCombos();
        
        assertEquals( 7, optionCombos.size() ); // Including default
    }
    
    @Test
    public void testGetCategoryOptionCombo()
    {
        categoryService.generateOptionCombos( categoryComboA );
        categoryService.generateOptionCombos( categoryComboB );
        
        Set<CategoryOption> categoryOptions1 = new HashSet<>();
        categoryOptions1.add( categoryOptionA );
        categoryOptions1.add( categoryOptionC );

        Set<CategoryOption> categoryOptions2 = new HashSet<>();
        categoryOptions2.add( categoryOptionA );
        categoryOptions2.add( categoryOptionD );

        Set<CategoryOption> categoryOptions3 = new HashSet<>();
        categoryOptions3.add( categoryOptionB );
        categoryOptions3.add( categoryOptionC );

        Set<CategoryOption> categoryOptions4 = new HashSet<>();
        categoryOptions4.add( categoryOptionB );
        categoryOptions4.add( categoryOptionC );
        
        CategoryOptionCombo coc1 = categoryOptionComboStore.getCategoryOptionCombo( categoryComboA, categoryOptions1 );
        CategoryOptionCombo coc2 = categoryOptionComboStore.getCategoryOptionCombo( categoryComboA, categoryOptions2 );
        CategoryOptionCombo coc3 = categoryOptionComboStore.getCategoryOptionCombo( categoryComboA, categoryOptions3 );
        CategoryOptionCombo coc4 = categoryOptionComboStore.getCategoryOptionCombo( categoryComboA, categoryOptions4 );
        
        assertNotNull( coc1 );
        assertNotNull( coc2 );
        assertNotNull( coc3 );
        assertNotNull( coc4 );
        
        assertEquals( categoryComboA, coc1.getCategoryCombo() );
        assertEquals( categoryComboA, coc2.getCategoryCombo() );
        assertEquals( categoryComboA, coc3.getCategoryCombo() );
        assertEquals( categoryComboA, coc4.getCategoryCombo() );
        
        assertEquals( categoryOptions1, coc1.getCategoryOptions() );
        assertEquals( categoryOptions2, coc2.getCategoryOptions() );
        assertEquals( categoryOptions3, coc3.getCategoryOptions() );
        assertEquals( categoryOptions4, coc4.getCategoryOptions() );
    }
}
