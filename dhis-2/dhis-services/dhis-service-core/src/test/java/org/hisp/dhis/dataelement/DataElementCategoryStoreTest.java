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

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataElementCategoryStoreTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.dataelement.CategoryStore")
    private GenericIdentifiableObjectStore<DataElementCategory> categoryStore;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    private DataElementCategoryOption categoryOptionA;

    private DataElementCategoryOption categoryOptionB;

    private DataElementCategoryOption categoryOptionC;

    private DataElementCategory categoryA;

    private DataElementCategory categoryB;

    private DataElementCategory categoryC;

    private List<DataElementCategoryOption> categoryOptions;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        categoryOptionA = new DataElementCategoryOption( "CategoryOptionA" );
        categoryOptionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryOptionC = new DataElementCategoryOption( "CategoryOptionC" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );

        categoryOptions = new ArrayList<>();

        categoryOptions.add( categoryOptionA );
        categoryOptions.add( categoryOptionB );
        categoryOptions.add( categoryOptionC );

    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION, categoryOptions );

        int idA = categoryStore.save( categoryA );
        int idB = categoryStore.save( categoryB );
        int idC = categoryStore.save( categoryC );

        assertEquals( categoryA, categoryStore.get( idA ) );
        assertEquals( categoryB, categoryStore.get( idB ) );
        assertEquals( categoryC, categoryStore.get( idC ) );

        assertEquals( categoryOptions, categoryStore.get( idA ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryStore.get( idB ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryStore.get( idC ).getCategoryOptions() );
    }

    @Test
    public void testDelete()
    {
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION, categoryOptions );

        int idA = categoryStore.save( categoryA );
        int idB = categoryStore.save( categoryB );
        int idC = categoryStore.save( categoryC );

        assertNotNull( categoryStore.get( idA ) );
        assertNotNull( categoryStore.get( idB ) );
        assertNotNull( categoryStore.get( idC ) );

        categoryStore.delete( categoryA );

        assertNull( categoryStore.get( idA ) );
        assertNotNull( categoryStore.get( idB ) );
        assertNotNull( categoryStore.get( idC ) );

        categoryStore.delete( categoryB );

        assertNull( categoryStore.get( idA ) );
        assertNull( categoryStore.get( idB ) );
        assertNotNull( categoryStore.get( idC ) );
    }

    @Test
    public void testGetAll()
    {
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION, categoryOptions );

        categoryStore.save( categoryA );
        categoryStore.save( categoryB );
        categoryStore.save( categoryC );

        List<DataElementCategory> categories = categoryStore.getAll();

        assertEquals( 4, categories.size() ); // Including default
        assertTrue( categories.contains( categoryA ) );
        assertTrue( categories.contains( categoryB ) );
        assertTrue( categories.contains( categoryC ) );
    }
}
