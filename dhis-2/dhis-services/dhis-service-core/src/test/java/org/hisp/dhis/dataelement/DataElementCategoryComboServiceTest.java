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
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataElementCategoryComboServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementCategoryService categoryService;
    
    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategoryOption categoryOptionC;
    private DataElementCategoryOption categoryOptionD;
    private DataElementCategoryOption categoryOptionE;
    private DataElementCategoryOption categoryOptionF;
    private DataElementCategoryOption categoryOptionG;
    
    private DataElementCategory categoryA;
    private DataElementCategory categoryB;
    private DataElementCategory categoryC;
    
    private DataElementCategoryCombo categoryComboA;
    private DataElementCategoryCombo categoryComboB;
    private DataElementCategoryCombo categoryComboC;
    
    private List<DataElementCategory> categories;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {  
        categories = new ArrayList<>();
        
        categoryOptionA = new DataElementCategoryOption( "OptionA" );
        categoryOptionB = new DataElementCategoryOption( "OptionB" );
        categoryOptionC = new DataElementCategoryOption( "OptionC" );
        categoryOptionD = new DataElementCategoryOption( "OptionD" );
        categoryOptionE = new DataElementCategoryOption( "OptionE" );
        categoryOptionF = new DataElementCategoryOption( "OptionF" );
        categoryOptionG = new DataElementCategoryOption( "OptionG" );
        
        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );
        categoryService.addDataElementCategoryOption( categoryOptionD );
        categoryService.addDataElementCategoryOption( categoryOptionE );
        categoryService.addDataElementCategoryOption( categoryOptionF );
        categoryService.addDataElementCategoryOption( categoryOptionG );
        
        categoryA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION );
        categoryB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION );
        categoryC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION );
        
        categoryA.addCategoryOption( categoryOptionA );
        categoryA.addCategoryOption( categoryOptionB );
        categoryB.addCategoryOption( categoryOptionC );
        categoryB.addCategoryOption( categoryOptionD );
        categoryC.addCategoryOption( categoryOptionE );
        categoryC.addCategoryOption( categoryOptionF );
        
        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );
        categoryService.addDataElementCategory( categoryC );
        
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
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new DataElementCategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new DataElementCategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        
        int idA = categoryService.addDataElementCategoryCombo( categoryComboA );
        int idB = categoryService.addDataElementCategoryCombo( categoryComboB );
        int idC = categoryService.addDataElementCategoryCombo( categoryComboC );
        
        assertEquals( categoryComboA, categoryService.getDataElementCategoryCombo( idA ) );
        assertEquals( categoryComboB, categoryService.getDataElementCategoryCombo( idB ) );
        assertEquals( categoryComboC, categoryService.getDataElementCategoryCombo( idC ) );
        
        assertEquals( categories, categoryService.getDataElementCategoryCombo( idA ).getCategories() );
        assertEquals( categories, categoryService.getDataElementCategoryCombo( idB ).getCategories() );
        assertEquals( categories, categoryService.getDataElementCategoryCombo( idC ).getCategories() );        
    }

    @Test
    public void testDelete()
    {
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new DataElementCategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new DataElementCategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        
        int idA = categoryService.addDataElementCategoryCombo( categoryComboA );
        int idB = categoryService.addDataElementCategoryCombo( categoryComboB );
        int idC = categoryService.addDataElementCategoryCombo( categoryComboC );
        
        assertNotNull( categoryService.getDataElementCategoryCombo( idA ) );
        assertNotNull( categoryService.getDataElementCategoryCombo( idB ) );
        assertNotNull( categoryService.getDataElementCategoryCombo( idC ) );
        
        categoryService.deleteDataElementCategoryCombo( categoryComboA );

        assertNull( categoryService.getDataElementCategoryCombo( idA ) );
        assertNotNull( categoryService.getDataElementCategoryCombo( idB ) );
        assertNotNull( categoryService.getDataElementCategoryCombo( idC ) );
        
        categoryService.deleteDataElementCategoryCombo( categoryComboB );

        assertNull( categoryService.getDataElementCategoryCombo( idA ) );
        assertNull( categoryService.getDataElementCategoryCombo( idB ) );
        assertNotNull( categoryService.getDataElementCategoryCombo( idC ) );        
    }

    @Test
    public void testGetAll()
    {
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryComboB = new DataElementCategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION, categories );
        categoryComboC = new DataElementCategoryCombo( "CategoryComboC", DataDimensionType.DISAGGREGATION, categories );
        
        categoryService.addDataElementCategoryCombo( categoryComboA );
        categoryService.addDataElementCategoryCombo( categoryComboB );
        categoryService.addDataElementCategoryCombo( categoryComboC );
        
        List<DataElementCategoryCombo> categoryCombos = categoryService.getAllDataElementCategoryCombos();
        
        assertEquals( 4, categoryCombos.size() ); // Including default
        assertTrue( categoryCombos.contains( categoryComboA ) );
        assertTrue( categoryCombos.contains( categoryComboB ) );
        assertTrue( categoryCombos.contains( categoryComboC ) );        
    }

    @Test
    public void testGenerateCategoryOptionCombos()
    {        
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addDataElementCategoryCombo( categoryComboA );
        
        categoryService.generateOptionCombos( categoryComboA );
        
        Set<DataElementCategoryOptionCombo> optionCombos = categoryComboA.getOptionCombos();
        
        assertEquals( 8, optionCombos.size() );
        
        assertOptionCombos( optionCombos );
    }
    
    @Test
    public void testUpdateCategoryOptionCombosA()
    {
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addDataElementCategoryCombo( categoryComboA );
        
        categoryService.generateOptionCombos( categoryComboA );
        
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        
        categoryC.addCategoryOption( categoryOptionG );
        categoryService.updateDataElementCategory( categoryC );
        
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
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addDataElementCategoryCombo( categoryComboA );
        
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
        categoryComboA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION, categories );
        categoryService.addDataElementCategoryCombo( categoryComboA );
        
        categoryService.generateOptionCombos( categoryComboA );
        
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 8, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        
        categoryC.addCategoryOption( categoryOptionG );
        categoryService.updateDataElementCategory( categoryC );
        
        categoryService.updateOptionCombos( categoryC );
        
        assertNotNull( categoryComboA.getOptionCombos() );
        assertEquals( 12, categoryComboA.getOptionCombos().size() );
        assertOptionCombos( categoryComboA.getOptionCombos() );
        
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionD, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionC, categoryOptionG ) ) );
        assertTrue( categoryComboA.getOptionCombos().contains( createCategoryOptionCombo( categoryComboA, categoryOptionB, categoryOptionD, categoryOptionG ) ) );
    }
  
    private void assertOptionCombos( Set<DataElementCategoryOptionCombo> optionCombos )
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
