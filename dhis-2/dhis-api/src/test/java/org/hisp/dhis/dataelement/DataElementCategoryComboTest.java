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

import java.util.List;

import org.hisp.dhis.common.DataDimensionType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class DataElementCategoryComboTest
{
    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategoryOption categoryOptionC;
    private DataElementCategoryOption categoryOptionD;
    private DataElementCategoryOption categoryOptionE;
    private DataElementCategoryOption categoryOptionF;
    
    private DataElementCategory categoryA;
    private DataElementCategory categoryB;
    private DataElementCategory categoryC;
    
    private DataElementCategoryCombo categoryCombo;
    
    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void before()
    {
        categoryOptionA = new DataElementCategoryOption( "OptionA" );
        categoryOptionB = new DataElementCategoryOption( "OptionB" );
        categoryOptionC = new DataElementCategoryOption( "OptionC" );
        categoryOptionD = new DataElementCategoryOption( "OptionD" );
        categoryOptionE = new DataElementCategoryOption( "OptionE" );
        categoryOptionF = new DataElementCategoryOption( "OptionF" );
        
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION );
        
        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );
        categoryC.getCategoryOptions().add( categoryOptionE );
        categoryC.getCategoryOptions().add( categoryOptionF );
        
        categoryOptionA.getCategories().add( categoryA );
        categoryOptionB.getCategories().add( categoryA );
        categoryOptionC.getCategories().add( categoryB );
        categoryOptionD.getCategories().add( categoryB );
        categoryOptionE.getCategories().add( categoryC );
        categoryOptionF.getCategories().add( categoryC );
        
        categoryCombo = new DataElementCategoryCombo( "CategoryCombo", DataDimensionType.DISAGGREGATION );
        
        categoryCombo.getCategories().add( categoryA );
        categoryCombo.getCategories().add( categoryB );
        categoryCombo.getCategories().add( categoryC );        
    }
    
    @Test
    public void testGenerateOptionCombosList()
    {
        List<DataElementCategoryOptionCombo> list = categoryCombo.generateOptionCombosList();
        
        assertNotNull( list );
        assertEquals( 8, list.size() );
        
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionA, categoryOptionC, categoryOptionE ), list.get( 0 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionA, categoryOptionC, categoryOptionF ), list.get( 1 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionA, categoryOptionD, categoryOptionE ), list.get( 2 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionA, categoryOptionD, categoryOptionF ), list.get( 3 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionB, categoryOptionC, categoryOptionE ), list.get( 4 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionB, categoryOptionC, categoryOptionF ), list.get( 5 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionB, categoryOptionD, categoryOptionE ), list.get( 6 ) );
        assertEquals( createCategoryOptionCombo( categoryCombo, categoryOptionB, categoryOptionD, categoryOptionF ), list.get( 7 ) );
    }
    
    @Test
    public void test()
    {
        List<DataElementCategoryOptionCombo> list = categoryCombo.generateOptionCombosList();
        
        categoryCombo.generateOptionCombos();
        
        assertEquals( list, categoryCombo.getSortedOptionCombos() );
    }
    
    private static DataElementCategoryOptionCombo createCategoryOptionCombo( DataElementCategoryCombo categoryCombo, DataElementCategoryOption... categoryOptions )
    {
        DataElementCategoryOptionCombo categoryOptionCombo = new DataElementCategoryOptionCombo();
        
        categoryOptionCombo.setCategoryCombo( categoryCombo );
        
        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            categoryOptionCombo.getCategoryOptions().add( categoryOption );
        }
        
        return categoryOptionCombo;
    }
}
