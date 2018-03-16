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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class CategoryOptionComboServiceTest
    extends DhisSpringTest
{
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttributeService attributeService;

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

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionB );

        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );

        int id = categoryService.addCategoryOptionCombo( categoryOptionComboA );

        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );

        assertNotNull( categoryOptionComboA );
        assertEquals( categoryComboA, categoryOptionComboA.getCategoryCombo() );
        assertEquals( categoryOptions, categoryOptionComboA.getCategoryOptions() );
    }

    @Test
    public void testGetCategoryOptionCombo()
    {
        categoryService.generateOptionCombos( categoryComboA );

        List<CategoryOption> catopts = new LinkedList<>();
        catopts.add( categoryOptionA );
        catopts.add( categoryOptionC );

        CategoryOptionCombo catoptcombo = categoryService.getCategoryOptionCombo( catopts );
        assertNotNull( catoptcombo );
    }

    @Test
    public void testUpdateGetCategoryOptionCombo()
    {
        categoryOptionComboA = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );

        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );

        int id = categoryService.addCategoryOptionCombo( categoryOptionComboA );

        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );

        assertNotNull( categoryOptionComboA );
        assertEquals( categoryComboA, categoryOptionComboA.getCategoryCombo() );
        assertEquals( categoryOptions, categoryOptionComboA.getCategoryOptions() );

        categoryOptionComboA.setCategoryCombo( categoryComboB );

        categoryService.updateCategoryOptionCombo( categoryOptionComboA );

        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );

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

        int idA = categoryService.addCategoryOptionCombo( categoryOptionComboA );
        int idB = categoryService.addCategoryOptionCombo( categoryOptionComboB );
        int idC = categoryService.addCategoryOptionCombo( categoryOptionComboC );

        assertNotNull( categoryService.getCategoryOptionCombo( idA ) );
        assertNotNull( categoryService.getCategoryOptionCombo( idB ) );
        assertNotNull( categoryService.getCategoryOptionCombo( idC ) );

        categoryService.deleteCategoryOptionCombo( categoryService.getCategoryOptionCombo( idA ) );

        assertNull( categoryService.getCategoryOptionCombo( idA ) );
        assertNotNull( categoryService.getCategoryOptionCombo( idB ) );
        assertNotNull( categoryService.getCategoryOptionCombo( idC ) );

        categoryService.deleteCategoryOptionCombo( categoryService.getCategoryOptionCombo( idB ) );

        assertNull( categoryService.getCategoryOptionCombo( idA ) );
        assertNull( categoryService.getCategoryOptionCombo( idB ) );
        assertNotNull( categoryService.getCategoryOptionCombo( idC ) );

        categoryService.deleteCategoryOptionCombo( categoryService.getCategoryOptionCombo( idC ) );

        assertNull( categoryService.getCategoryOptionCombo( idA ) );
        assertNull( categoryService.getCategoryOptionCombo( idB ) );
        assertNull( categoryService.getCategoryOptionCombo( idC ) );
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

        categoryService.addCategoryOptionCombo( categoryOptionComboA );
        categoryService.addCategoryOptionCombo( categoryOptionComboB );
        categoryService.addCategoryOptionCombo( categoryOptionComboC );

        List<CategoryOptionCombo> categoryOptionCombos =
            categoryService.getAllCategoryOptionCombos();

        assertNotNull( categoryOptionCombos );
        assertEquals( 4, categoryOptionCombos.size() ); // Including default category option combo
    }

    @Test
    public void testGetCategoryOptionComboName()
    {
        categoryOptionComboA = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionC );

        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );

        categoryService.addCategoryOptionCombo( categoryOptionComboA );

        String expected = "Male, 0-20";

        assertEquals( expected, categoryOptionComboA.getName() );
    }

    @Test
    public void testAddAttributeValue()
    {
        categoryOptionComboA = new CategoryOptionCombo();

        Set<CategoryOption> categoryOptions = Sets.newHashSet( categoryOptionA, categoryOptionB );

        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryOptionComboA.setCategoryOptions( categoryOptions );

        int id = categoryService.addCategoryOptionCombo( categoryOptionComboA );

        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );

        Attribute attribute1 = new Attribute( "attribute 1", ValueType.TEXT );
        attribute1.setCategoryOptionComboAttribute( true );
        attributeService.addAttribute( attribute1 );

        AttributeValue avA = new AttributeValue( "value 1" );
        avA.setAttribute( attribute1 );

        categoryOptionComboA.getAttributeValues().add( avA );
        categoryService.updateCategoryOptionCombo( categoryOptionComboA );

        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );
        assertFalse( categoryOptionComboA.getAttributeValues().isEmpty() );

        categoryOptionComboA.getAttributeValues().clear();
        categoryService.updateCategoryOptionCombo( categoryOptionComboA );
        categoryOptionComboA = categoryService.getCategoryOptionCombo( id );
        assertTrue( categoryOptionComboA.getAttributeValues().isEmpty() );
    }
}
