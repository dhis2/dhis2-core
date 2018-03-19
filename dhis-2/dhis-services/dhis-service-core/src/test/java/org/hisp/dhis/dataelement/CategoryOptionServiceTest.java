package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class CategoryOptionServiceTest
    extends DhisSpringTest
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
    public void testAddGet()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );
        
        int idA = categoryService.addCategoryOption( categoryOptionA );
        int idB = categoryService.addCategoryOption( categoryOptionB );
        int idC = categoryService.addCategoryOption( categoryOptionC );
        
        assertEquals( categoryOptionA, categoryService.getCategoryOption( idA ) );
        assertEquals( categoryOptionB, categoryService.getCategoryOption( idB ) );
        assertEquals( categoryOptionC, categoryService.getCategoryOption( idC ) );
    }

    @Test
    public void testDelete()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );

        int idA = categoryService.addCategoryOption( categoryOptionA );
        int idB = categoryService.addCategoryOption( categoryOptionB );
        int idC = categoryService.addCategoryOption( categoryOptionC );
        
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
    public void testGetAll()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        
        List<CategoryOption> categoryOptions = categoryService.getAllCategoryOptions();
        
        assertEquals( 4, categoryOptions.size() ); // Including default
        assertTrue( categoryOptions.contains( categoryOptionA ) );
        assertTrue( categoryOptions.contains( categoryOptionB ) );
        assertTrue( categoryOptions.contains( categoryOptionC ) );        
    }

    @Test
    public void testGetByCategory()
    {
        categoryOptionA = new CategoryOption( "CategoryOptionA" );
        categoryOptionB = new CategoryOption( "CategoryOptionB" );
        categoryOptionC = new CategoryOption( "CategoryOptionC" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        
        List<CategoryOption> optionsA = new ArrayList<>();
        List<CategoryOption> optionsB = new ArrayList<>();
           
        optionsA.add( categoryOptionA );
        optionsA.add( categoryOptionB );
        optionsB.add( categoryOptionC );
        
        categoryA = new Category( "CategoryA", DataDimensionType.DISAGGREGATION, optionsA );
        categoryB = new Category( "CategoryB", DataDimensionType.DISAGGREGATION, optionsB );
        categoryC = new Category( "CategoryC", DataDimensionType.DISAGGREGATION );
        
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
