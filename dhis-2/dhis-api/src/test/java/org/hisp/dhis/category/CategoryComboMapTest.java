/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.hisp.dhis.category.CategoryComboMap.CategoryComboMapException;
import org.hisp.dhis.common.IdScheme;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit tests for {@link CategoryComboMap}.
 *
 * @author Jim Grace
 */
public class CategoryComboMapTest
{
    private CategoryOption coA;

    private CategoryOption coB;

    private CategoryOption coC;

    private CategoryOption coD;

    private Category cA;

    private Category cB;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocAC;

    private CategoryOptionCombo cocAD;

    private CategoryOptionCombo cocBC;

    private CategoryOptionCombo cocBD;

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

        coA.setCode( "coA" );
        coB.setCode( "coB" );
        coC.setCode( "coC" );
        coD.setCode( dCode );

        coA.setUid( "coAAAAAAAAA" );
        coB.setUid( "coBBBBBBBBB" );
        coC.setUid( "coCCCCCCCCC" );
        coD.setUid( dUid );

        cA = new Category( "Category A", DISAGGREGATION, Lists.newArrayList( coA, coB ) );
        cB = new Category( "Category B", DISAGGREGATION, Lists.newArrayList( coC, coD ) );

        cA.setCode( "cA" );
        cB.setCode( "cB" );

        cA.setUid( "cAAAAAAAAAA" );
        cB.setUid( "cBBBBBBBBBB" );

        ccA = new CategoryCombo( "CategoryCombo A", DISAGGREGATION, Lists.newArrayList( cA, cB ) );

        ccA.setCode( "ccA" );

        ccA.setUid( "ccAAAAAAAAA" );

        cocAC = new CategoryOptionCombo();
        cocAD = new CategoryOptionCombo();
        cocBC = new CategoryOptionCombo();
        cocBD = new CategoryOptionCombo();

        cocAC.setName( "CategoryOptionCombo AC" );
        cocAD.setName( "CategoryOptionCombo AD" );
        cocBC.setName( "CategoryOptionCombo BC" );
        cocBD.setName( "CategoryOptionCombo BD" );

        cocAC.setCode( "cocAC" );
        cocAD.setCode( "cocAD" );
        cocBC.setCode( "cocBC" );
        cocBD.setCode( "cocBD" );

        cocAC.setUid( "cocACAAAAAA" );
        cocAD.setUid( "cocADDDDDDD" );
        cocBC.setUid( "cocBCCCCCCC" );
        cocBD.setUid( "cocBDDDDDDD" );

        cocAC.setCategoryCombo( ccA );
        cocAD.setCategoryCombo( ccA );
        cocBC.setCategoryCombo( ccA );
        cocBD.setCategoryCombo( ccA );

        cocAC.setCategoryOptions( Sets.newHashSet( coA, coC ) );
        cocAD.setCategoryOptions( Sets.newHashSet( coA, coD ) );
        cocBC.setCategoryOptions( Sets.newHashSet( coB, coC ) );
        cocBD.setCategoryOptions( Sets.newHashSet( coB, coD ) );

        coA.setCategoryOptionCombos( Sets.newHashSet( cocAC, cocAD ) );
        coB.setCategoryOptionCombos( Sets.newHashSet( cocBC, cocBD ) );
        coC.setCategoryOptionCombos( Sets.newHashSet( cocAC, cocBC ) );
        coD.setCategoryOptionCombos( Sets.newHashSet( cocAD, cocBD ) );

        ccA.setOptionCombos( Sets.newHashSet( cocAC, cocAD, cocBC, cocBD ) );
    }

    @Test
    public void testGetCategoryOptionComboUid()
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
    public void testGetCategoryOptionComboCode()
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
    public void testGetCategoryOptionComboName()
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
    public void testGetCategoryOptionComboNoCategoryOption()
    {
        setUp();

        cB.getCategoryOptions().clear();

        expectException( UID, "No categoryOption in Category B matching CategoryOptionCombo AD" );
    }

    @Test
    public void testGetCategoryOptionComboNoUid()
    {
        setUp( "CategoryOption D", "coD", null );

        expectException( UID, "No UID identifier for CategoryOption: CategoryOption D" );
    }

    @Test
    public void testGetCategoryOptionComboNoCode()
    {
        setUp( "CategoryOption D", null, "coDDDDDDDDD" );

        expectException( CODE, "No CODE identifier for CategoryOption: CategoryOption D" );
    }

    @Test
    public void testGetCategoryOptionComboNoName()
    {
        setUp( null, "coD", "coDDDDDDDDD" );

        expectException( NAME, "No NAME identifier for CategoryOption: null" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void expectException( IdScheme idScheme, String message )
    {
        try
        {
            new CategoryComboMap( ccA, idScheme );

            fail( "Expected CategoryComboMapException." );
        }
        catch ( CategoryComboMapException e )
        {
            assertEquals( message, e.getMessage() );
        }
    }
}
