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

import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.hisp.dhis.category.CategoryComboMap.CategoryComboMapException;
import org.hisp.dhis.common.IdScheme;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit tests for {@link CategoryComboMap}.
 *
 * @author Jim Grace
 */
class CategoryComboMapTest
{

    private CategoryOption coA;

    private CategoryOption coB;

    private CategoryOption coC;

    private CategoryOption coD;

    private CategoryOption coX;

    private Category cA;

    private Category cB;

    private Category cX;

    private CategoryCombo ccA;

    private CategoryCombo ccX;

    private CategoryOptionCombo cocAC;

    private CategoryOptionCombo cocAD;

    private CategoryOptionCombo cocBC;

    private CategoryOptionCombo cocBD;

    private CategoryOptionCombo cocX;

    private CategoryComboMap ccm;

    private void setUp()
    {
        setUp( "CategoryOption D", "coD", "coDDDDDDDDD" );
    }

    private void setUp( String dName, String dCode, String dUid )
    {
        coA = new CategoryOption( "CategoryOption A" );
        coB = new CategoryOption( "CategoryOption B" );
        coC = new CategoryOption( "CategoryOption C" );
        coD = new CategoryOption( dName );
        coX = new CategoryOption( "CategoryOption X" );
        coA.setCode( "coA" );
        coB.setCode( "coB" );
        coC.setCode( "coC" );
        coD.setCode( dCode );
        coX.setCode( "coX" );
        coA.setUid( "coAAAAAAAAA" );
        coB.setUid( "coBBBBBBBBB" );
        coC.setUid( "coCCCCCCCCC" );
        coD.setUid( dUid );
        coX.setUid( "coXXXXXXXXX" );
        cA = new Category( "Category A", DISAGGREGATION, Lists.newArrayList( coA, coB ) );
        cB = new Category( "Category B", DISAGGREGATION, Lists.newArrayList( coC, coD ) );
        cX = new Category( "Category X", DISAGGREGATION, Lists.newArrayList( coX ) );
        cA.setCode( "cA" );
        cB.setCode( "cB" );
        cA.setUid( "cAAAAAAAAAA" );
        cB.setUid( "cBBBBBBBBBB" );
        cX.setUid( "cXXXXXXXXXX" );
        ccA = new CategoryCombo( "CategoryCombo A", DISAGGREGATION, Lists.newArrayList( cA, cB ) );
        ccA.setCode( "ccA" );
        ccA.setUid( "ccAAAAAAAAA" );
        ccX = new CategoryCombo( "CategoryCombo X", DISAGGREGATION, Lists.newArrayList( cX ) );
        cocAC = new CategoryOptionCombo();
        cocAD = new CategoryOptionCombo();
        cocBC = new CategoryOptionCombo();
        cocBD = new CategoryOptionCombo();
        cocX = new CategoryOptionCombo();
        cocAC.setName( "CategoryOptionCombo AC" );
        cocAD.setName( "CategoryOptionCombo AD" );
        cocBC.setName( "CategoryOptionCombo BC" );
        cocBD.setName( "CategoryOptionCombo BD" );
        cocX.setName( "CategoryOptionCombo X" );
        cocAC.setCode( "cocAC" );
        cocAD.setCode( "cocAD" );
        cocBC.setCode( "cocBC" );
        cocBD.setCode( "cocBD" );
        cocX.setCode( "cocX" );
        cocAC.setUid( "cocACAAAAAA" );
        cocAD.setUid( "cocADDDDDDD" );
        cocBC.setUid( "cocBCCCCCCC" );
        cocBD.setUid( "cocBDDDDDDD" );
        cocX.setUid( "cocXXXXXXXX" );
        cocAC.setCategoryCombo( ccA );
        cocAD.setCategoryCombo( ccA );
        cocBC.setCategoryCombo( ccA );
        cocBD.setCategoryCombo( ccA );
        cocX.setCategoryCombo( ccX );
        cocAC.setCategoryOptions( Sets.newHashSet( coA, coC ) );
        cocAD.setCategoryOptions( Sets.newHashSet( coA, coD ) );
        cocBC.setCategoryOptions( Sets.newHashSet( coB, coC ) );
        cocBD.setCategoryOptions( Sets.newHashSet( coB, coD ) );
        cocX.setCategoryOptions( Sets.newHashSet( coX ) );
        coA.setCategoryOptionCombos( Sets.newHashSet( cocAC, cocAD ) );
        coB.setCategoryOptionCombos( Sets.newHashSet( cocBC, cocBD ) );
        coC.setCategoryOptionCombos( Sets.newHashSet( cocAC, cocBC ) );
        coD.setCategoryOptionCombos( Sets.newHashSet( cocAD, cocBD ) );
        coX.setCategoryOptionCombos( Sets.newHashSet( cocX ) );
        ccA.setOptionCombos( Sets.newHashSet( cocAC, cocAD, cocBC, cocBD ) );
        ccX.setOptionCombos( Sets.newHashSet( cocX ) );
    }

    @Test
    void testGetCategoryOptionComboUid()
        throws CategoryComboMapException
    {
        setUp();
        ccm = new CategoryComboMap( ccA, IdScheme.UID );
        assertEquals( cocAC, ccm.getCategoryOptionCombo( "\"coAAAAAAAAA\"\"coCCCCCCCCC\"" ) );
        assertEquals( cocAD, ccm.getCategoryOptionCombo( "\"coAAAAAAAAA\"\"coDDDDDDDDD\"" ) );
        assertEquals( cocBC, ccm.getCategoryOptionCombo( "\"coBBBBBBBBB\"\"coCCCCCCCCC\"" ) );
        assertEquals( cocBD, ccm.getCategoryOptionCombo( "\"coBBBBBBBBB\"\"coDDDDDDDDD\"" ) );
    }

    @Test
    void testGetCategoryOptionComboCode()
        throws CategoryComboMapException
    {
        setUp();
        ccm = new CategoryComboMap( ccA, CODE );
        assertEquals( cocAC, ccm.getCategoryOptionCombo( "\"coA\"\"coC\"" ) );
        assertEquals( cocAD, ccm.getCategoryOptionCombo( "\"coA\"\"coD\"" ) );
        assertEquals( cocBC, ccm.getCategoryOptionCombo( "\"coB\"\"coC\"" ) );
        assertEquals( cocBD, ccm.getCategoryOptionCombo( "\"coB\"\"coD\"" ) );
    }

    @Test
    void testGetCategoryOptionComboName()
        throws CategoryComboMapException
    {
        setUp();
        ccm = new CategoryComboMap( ccA, NAME );
        assertEquals( cocAC, ccm.getCategoryOptionCombo( "\"CategoryOption A\"\"CategoryOption C\"" ) );
        assertEquals( cocAD, ccm.getCategoryOptionCombo( "\"CategoryOption A\"\"CategoryOption D\"" ) );
        assertEquals( cocBC, ccm.getCategoryOptionCombo( "\"CategoryOption B\"\"CategoryOption C\"" ) );
        assertEquals( cocBD, ccm.getCategoryOptionCombo( "\"CategoryOption B\"\"CategoryOption D\"" ) );
    }

    @Test
    void testGetCategoryOptionComboNoCategoryOption()
    {
        setUp();
        cX.getCategoryOptions().clear();
        expectException( UID, "No categoryOption in Category X matching CategoryOptionCombo X", ccX );
    }

    @Test
    void testGetCategoryOptionComboNoUid()
    {
        setUp( "CategoryOption D", "coD", null );
        expectException( UID, "No UID identifier for CategoryOption: CategoryOption D" );
    }

    @Test
    void testGetCategoryOptionComboNoCode()
    {
        setUp( "CategoryOption D", null, "coDDDDDDDDD" );
        expectException( CODE, "No CODE identifier for CategoryOption: CategoryOption D" );
    }

    @Test
    void testGetCategoryOptionComboNoName()
    {
        setUp( null, "coD", "coDDDDDDDDD" );
        expectException( NAME, "No NAME identifier for CategoryOption: null" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void expectException( IdScheme idScheme, String message )
    {
        expectException( idScheme, message, ccA );
    }

    private void expectException( IdScheme idScheme, String message, CategoryCombo cc )
    {
        try
        {
            new CategoryComboMap( cc, idScheme );
            fail( "Expected CategoryComboMapException." );
        }
        catch ( CategoryComboMapException e )
        {
            assertEquals( message, e.getMessage() );
        }
    }
}
