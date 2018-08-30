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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class CategoryServiceTest
    extends DhisSpringTest
{
    private DataElement deA;
    private DataElement deB;
    
    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;
    private CategoryOption categoryOptionC;

    private Category categoryA;
    private Category categoryB;
    private Category categoryC;

    private CategoryCombo ccA;
    
    private List<CategoryOption> categoryOptions;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryStore categoryStore;
    
    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {        
        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );
        categoryOptionC = createCategoryOption( 'C' );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );

        categoryOptions = new ArrayList<>();

        categoryOptions.add( categoryOptionA );
        categoryOptions.add( categoryOptionB );
        categoryOptions.add( categoryOptionC );
    }

    // -------------------------------------------------------------------------
    // Category
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB, categoryOptionC );
        categoryB = createCategory( 'B', categoryOptionA, categoryOptionB, categoryOptionC );
        categoryC = createCategory( 'C', categoryOptionA, categoryOptionB, categoryOptionC );

        int idA = categoryService.addCategory( categoryA );
        int idB = categoryService.addCategory( categoryB );
        int idC = categoryService.addCategory( categoryC );

        assertEquals( categoryA, categoryService.getCategory( idA ) );
        assertEquals( categoryB, categoryService.getCategory( idB ) );
        assertEquals( categoryC, categoryService.getCategory( idC ) );

        assertEquals( categoryOptions, categoryService.getCategory( idA ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryService.getCategory( idB ).getCategoryOptions() );
        assertEquals( categoryOptions, categoryService.getCategory( idC ).getCategoryOptions() );
    }

    @Test
    public void testDelete()
    {
        categoryA = new Category( "CategoryA", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryB = new Category( "CategoryB", DataDimensionType.DISAGGREGATION, categoryOptions );
        categoryC = new Category( "CategoryC", DataDimensionType.DISAGGREGATION, categoryOptions );

        int idA = categoryService.addCategory( categoryA );
        int idB = categoryService.addCategory( categoryB );
        int idC = categoryService.addCategory( categoryC );

        assertNotNull( categoryService.getCategory( idA ) );
        assertNotNull( categoryService.getCategory( idB ) );
        assertNotNull( categoryService.getCategory( idC ) );

        categoryService.deleteCategory( categoryA );

        assertNull( categoryService.getCategory( idA ) );
        assertNotNull( categoryService.getCategory( idB ) );
        assertNotNull( categoryService.getCategory( idC ) );

        categoryService.deleteCategory( categoryB );

        assertNull( categoryService.getCategory( idA ) );
        assertNull( categoryService.getCategory( idB ) );
        assertNotNull( categoryService.getCategory( idC ) );
    }

    @Test
    public void testGetAll()
    {
        categoryA = createCategory( 'A' );
        categoryB = createCategory( 'B' );
        categoryC = createCategory( 'C' );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        categoryService.addCategory( categoryC );

        List<Category> categories = categoryService.getAllDataElementCategories();

        assertEquals( 4, categories.size() ); // Including default
        assertTrue( categories.contains( categoryA ) );
        assertTrue( categories.contains( categoryB ) );
        assertTrue( categories.contains( categoryC ) );
    }

    // -------------------------------------------------------------------------
    // CategoryOptionGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetCategoryGroup()
    {
        CategoryOptionGroup groupA = createCategoryOptionGroup( 'A' );
        CategoryOptionGroup groupB = createCategoryOptionGroup( 'B' );
        CategoryOptionGroup groupC = createCategoryOptionGroup( 'C' );

        groupA.getMembers().add( categoryOptionA );
        groupA.getMembers().add( categoryOptionB );
        groupB.getMembers().add( categoryOptionC );

        int idA = categoryService.saveCategoryOptionGroup( groupA );
        int idB = categoryService.saveCategoryOptionGroup( groupB );
        int idC = categoryService.saveCategoryOptionGroup( groupC );

        assertEquals( groupA, categoryService.getCategoryOptionGroup( idA ) );
        assertEquals( groupB, categoryService.getCategoryOptionGroup( idB ) );
        assertEquals( groupC, categoryService.getCategoryOptionGroup( idC ) );

        assertEquals( 2, categoryService.getCategoryOptionGroup( idA ).getMembers().size() );
        assertEquals( 1, categoryService.getCategoryOptionGroup( idB ).getMembers().size() );
        assertEquals( 0, categoryService.getCategoryOptionGroup( idC ).getMembers().size() );
    }

    // -------------------------------------------------------------------------
    // CategoryOptionGroupSet
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetCategoryGroupSet()
    {
        CategoryOptionGroup groupA = createCategoryOptionGroup( 'A' );
        CategoryOptionGroup groupB = createCategoryOptionGroup( 'B' );
        CategoryOptionGroup groupC = createCategoryOptionGroup( 'C' );

        groupA.getMembers().add( categoryOptionA );
        groupA.getMembers().add( categoryOptionB );
        groupB.getMembers().add( categoryOptionC );

        categoryService.saveCategoryOptionGroup( groupA );
        categoryService.saveCategoryOptionGroup( groupB );
        categoryService.saveCategoryOptionGroup( groupC );

        CategoryOptionGroupSet groupSetA = createCategoryOptionGroupSet( 'A' );
        CategoryOptionGroupSet groupSetB = createCategoryOptionGroupSet( 'B' );
        CategoryOptionGroupSet groupSetC = createCategoryOptionGroupSet( 'C' );

        groupSetA.getMembers().add( groupA );
        groupSetA.getMembers().add( groupB );
        groupSetB.getMembers().add( groupC );

        int idA = categoryService.saveCategoryOptionGroupSet( groupSetA );
        int idB = categoryService.saveCategoryOptionGroupSet( groupSetB );
        int idC = categoryService.saveCategoryOptionGroupSet( groupSetC );

        assertEquals( groupSetA, categoryService.getCategoryOptionGroupSet( idA ) );
        assertEquals( groupSetB, categoryService.getCategoryOptionGroupSet( idB ) );
        assertEquals( groupSetC, categoryService.getCategoryOptionGroupSet( idC ) );

        assertEquals( 2, categoryService.getCategoryOptionGroupSet( idA ).getMembers().size() );
        assertEquals( 1, categoryService.getCategoryOptionGroupSet( idB ).getMembers().size() );
        assertEquals( 0, categoryService.getCategoryOptionGroupSet( idC ).getMembers().size() );
    }

    // -------------------------------------------------------------------------
    // DataElementOperand
    // -------------------------------------------------------------------------

    @Test
    public void testGetOperands()
    {
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryB = createCategory( 'B', categoryOptionC );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        
        ccA = createCategoryCombo( 'A', categoryA, categoryB );
        
        categoryService.addCategoryCombo( ccA );
        
        categoryService.generateOptionCombos( ccA );
        
        List<CategoryOptionCombo> optionCombos = Lists.newArrayList( ccA.getOptionCombos() );

        deA = createDataElement( 'A', ccA );
        deB = createDataElement( 'B', ccA );
        
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        
        List<DataElementOperand> operands = categoryService.getOperands( Lists.newArrayList( deA, deB ) );
        
        assertEquals( 4, operands.size() );
        assertTrue( operands.contains( new DataElementOperand( deA, optionCombos.get( 0 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deA, optionCombos.get( 1 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deB, optionCombos.get( 0 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deB, optionCombos.get( 1 ) ) ) );
    }

    @Test
    public void testGetOperandsWithTotals()
    {
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryB = createCategory( 'B', categoryOptionC );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );
        
        ccA = createCategoryCombo( 'A', categoryA, categoryB );
        
        categoryService.addCategoryCombo( ccA );
        
        categoryService.generateOptionCombos( ccA );

        List<CategoryOptionCombo> optionCombos = Lists.newArrayList( ccA.getOptionCombos() );

        deA = createDataElement( 'A', ccA );
        deB = createDataElement( 'B', ccA );
        
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        
        List<DataElementOperand> operands = categoryService.getOperands( Lists.newArrayList( deA, deB ), true );
        
        assertEquals( 6, operands.size() );
        assertTrue( operands.contains( new DataElementOperand( deA ) ) );
        assertTrue( operands.contains( new DataElementOperand( deA, optionCombos.get( 0 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deA, optionCombos.get( 1 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deB ) ) );
        assertTrue( operands.contains( new DataElementOperand( deB, optionCombos.get( 0 ) ) ) );
        assertTrue( operands.contains( new DataElementOperand( deB, optionCombos.get( 1 ) ) ) );
    }

    @Test
    public void testGetDisaggregationCategoryCombos()
    {
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryB = createCategory( 'B', categoryOptionC );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );

        ccA = createCategoryCombo( 'A', categoryA, categoryB );

        categoryService.addCategoryCombo( ccA );

        assertEquals( 1, categoryService.getDisaggregationCategoryCombos().size() );
    }

    @Test
    public void testGetDisaggregationCategoryOptionGroupSetsNoAcl()
    {
        CategoryOptionGroup groupA = createCategoryOptionGroup( 'A' );
        groupA.setDataDimensionType( DataDimensionType.DISAGGREGATION );
        CategoryOptionGroup groupB = createCategoryOptionGroup( 'B' );
        groupB.setDataDimensionType( DataDimensionType.DISAGGREGATION );
        CategoryOptionGroup groupC = createCategoryOptionGroup( 'C' );
        groupC.setDataDimensionType( DataDimensionType.DISAGGREGATION );

        groupA.getMembers().add( categoryOptionA );
        groupA.getMembers().add( categoryOptionB );
        groupB.getMembers().add( categoryOptionC );

        categoryService.saveCategoryOptionGroup( groupA );
        categoryService.saveCategoryOptionGroup( groupB );
        categoryService.saveCategoryOptionGroup( groupC );

        CategoryOptionGroupSet groupSetA = createCategoryOptionGroupSet( 'A' );
        groupSetA.setDataDimensionType( DataDimensionType.DISAGGREGATION );

        groupSetA.getMembers().add( groupA );
        groupSetA.getMembers().add( groupB );
        groupSetA.getMembers().add( groupC );
        categoryService.saveCategoryOptionGroupSet( groupSetA );

        assertEquals( 1, categoryService.getDisaggregationCategoryOptionGroupSetsNoAcl().size() );
    }

    @Test
    public void testGetDisaggregationCategories()
    {
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB, categoryOptionC );
        categoryA.setDataDimensionType( DataDimensionType.DISAGGREGATION );

        categoryService.addCategory( categoryA );

        // Default Category is created so count should be equal 2
        assertEquals( 2, categoryService.getDisaggregationCategories().size() );

        assertEquals( 1, categoryStore.getCategories( DataDimensionType.DISAGGREGATION, true ).size() );

        assertEquals( 1, categoryStore.getCategoriesNoAcl( DataDimensionType.DISAGGREGATION, true ).size() );

    }
}