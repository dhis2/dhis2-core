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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataElementCategoryOptionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementCategoryService categoryService;
   
    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategoryOption categoryOptionC;
    
    private DataElementCategory categoryA;
    private DataElementCategory categoryB;
    private DataElementCategory categoryC;
    
    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        categoryOptionA = new DataElementCategoryOption( "CategoryOptionA" );
        categoryOptionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryOptionC = new DataElementCategoryOption( "CategoryOptionC" );
        
        int idA = categoryService.addDataElementCategoryOption( categoryOptionA );
        int idB = categoryService.addDataElementCategoryOption( categoryOptionB );
        int idC = categoryService.addDataElementCategoryOption( categoryOptionC );
        
        assertEquals( categoryOptionA, categoryService.getDataElementCategoryOption( idA ) );
        assertEquals( categoryOptionB, categoryService.getDataElementCategoryOption( idB ) );
        assertEquals( categoryOptionC, categoryService.getDataElementCategoryOption( idC ) );
    }

    @Test
    public void testDelete()
    {
        categoryOptionA = new DataElementCategoryOption( "CategoryOptionA" );
        categoryOptionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryOptionC = new DataElementCategoryOption( "CategoryOptionC" );

        int idA = categoryService.addDataElementCategoryOption( categoryOptionA );
        int idB = categoryService.addDataElementCategoryOption( categoryOptionB );
        int idC = categoryService.addDataElementCategoryOption( categoryOptionC );
        
        assertNotNull( categoryService.getDataElementCategoryOption( idA ) );
        assertNotNull( categoryService.getDataElementCategoryOption( idB ) );
        assertNotNull( categoryService.getDataElementCategoryOption( idC ) );
        
        categoryService.deleteDataElementCategoryOption( categoryOptionA );

        assertNull( categoryService.getDataElementCategoryOption( idA ) );
        assertNotNull( categoryService.getDataElementCategoryOption( idB ) );
        assertNotNull( categoryService.getDataElementCategoryOption( idC ) );

        categoryService.deleteDataElementCategoryOption( categoryOptionB );

        assertNull( categoryService.getDataElementCategoryOption( idA ) );
        assertNull( categoryService.getDataElementCategoryOption( idB ) );
        assertNotNull( categoryService.getDataElementCategoryOption( idC ) );
    }

    @Test
    public void testGetAll()
    {
        categoryOptionA = new DataElementCategoryOption( "CategoryOptionA" );
        categoryOptionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryOptionC = new DataElementCategoryOption( "CategoryOptionC" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );
        
        List<DataElementCategoryOption> categoryOptions = categoryService.getAllDataElementCategoryOptions();
        
        assertEquals( 4, categoryOptions.size() ); // Including default
        assertTrue( categoryOptions.contains( categoryOptionA ) );
        assertTrue( categoryOptions.contains( categoryOptionB ) );
        assertTrue( categoryOptions.contains( categoryOptionC ) );        
    }

    @Test
    public void testGetByCategory()
    {
        categoryOptionA = new DataElementCategoryOption( "CategoryOptionA" );
        categoryOptionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryOptionC = new DataElementCategoryOption( "CategoryOptionC" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );
        
        List<DataElementCategoryOption> optionsA = new ArrayList<>();
        List<DataElementCategoryOption> optionsB = new ArrayList<>();
           
        optionsA.add( categoryOptionA );
        optionsA.add( categoryOptionB );
        optionsB.add( categoryOptionC );
        
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION, optionsA );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION, optionsB );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION );
        
        Set<DataElementCategory> categoriesA = new HashSet<>();
        Set<DataElementCategory> categoriesB = new HashSet<>();
        
        categoriesA.add( categoryA );
        categoriesB.add( categoryB );
        
        categoryOptionA.setCategories( categoriesA );
        categoryOptionB.setCategories( categoriesA );
        categoryOptionC.setCategories( categoriesB );
        
        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );
        categoryService.addDataElementCategory( categoryC );
        
        List<DataElementCategoryOption> categoryOptions = categoryService.getDataElementCategoryOptions( categoryA );

        assertEquals( 2, categoryOptions.size() );
        
        categoryOptions = categoryService.getDataElementCategoryOptions( categoryB );

        assertEquals( 1, categoryOptions.size() );
        
        categoryOptions = categoryService.getDataElementCategoryOptions( categoryC );

        assertEquals( 0, categoryOptions.size() );        
    }
}
