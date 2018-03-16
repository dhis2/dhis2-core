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

import org.hisp.dhis.common.DataDimensionType;

import org.hisp.dhis.common.IdentifiableProperty;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hisp.dhis.common.IdentifiableProperty.NAME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author bobj
 */
public class CategoryComboMapTest
{
    private CategoryOption categoryOptionA;

    private CategoryOption categoryOptionB;

    private CategoryOption categoryOptionC;

    private CategoryOption categoryOptionD;

    private CategoryOption categoryOptionE;

    private CategoryOption categoryOptionF;

    private Category categoryA;

    private Category categoryB;

    private Category categoryC;

    private CategoryCombo categoryCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void before()
    {
        categoryOptionA = new CategoryOption( "OptionA" );
        categoryOptionB = new CategoryOption( "OptionB" );
        categoryOptionC = new CategoryOption( "OptionC" );
        categoryOptionD = new CategoryOption( "OptionD" );
        categoryOptionE = new CategoryOption( "OptionE" );
        categoryOptionF = new CategoryOption( "OptionF" );

        categoryOptionA.setAutoFields();
        categoryOptionB.setAutoFields();
        categoryOptionC.setAutoFields();
        categoryOptionD.setAutoFields();
        categoryOptionE.setAutoFields();
        categoryOptionF.setAutoFields();

        categoryA = new Category( "CategoryA", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "CategoryB", DataDimensionType.DISAGGREGATION );
        categoryC = new Category( "CategoryC", DataDimensionType.DISAGGREGATION );

        categoryA.setAutoFields();
        categoryB.setAutoFields();
        categoryC.setAutoFields();

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );
        categoryC.getCategoryOptions().add( categoryOptionE );
        categoryC.getCategoryOptions().add( categoryOptionF );

        categoryCombo = new CategoryCombo( "CategoryCombo", DataDimensionType.DISAGGREGATION );
        categoryCombo.setAutoFields();

        categoryCombo.addCategory( categoryA );
        categoryCombo.addCategory( categoryB );
        categoryCombo.addCategory( categoryC );

        categoryCombo.generateOptionCombos();
    }

    @Test
    public void testMapRetrievalByName()
    {
        try
        {
            CategoryComboMap map = new CategoryComboMap( categoryCombo, NAME );

            List<CategoryOption> catopts = new LinkedList<>();
            catopts.add( categoryOptionA );
            catopts.add( categoryOptionC );
            catopts.add( categoryOptionE );

            String key = "\"" + categoryOptionA.getName() + "\"" + "\"" + categoryOptionC.getName() + "\"" + "\""
                + categoryOptionE.getName() + "\"";
            CategoryOptionCombo catoptcombo = map.getCategoryOptionCombo( key );

            assertNotNull( catoptcombo );
            assertTrue( catoptcombo.getCategoryOptions().containsAll( catopts ) );
        }
        catch ( CategoryComboMap.CategoryComboMapException ex )
        {
            assertTrue( ex.getMessage(), false );
        }
    }

    @Test
    public void testMapRetrievalByUid()
    {
        try
        {
            CategoryComboMap map = new CategoryComboMap( categoryCombo, IdentifiableProperty.UID );

            List<CategoryOption> catopts = new LinkedList<>();
            catopts.add( categoryOptionA );
            catopts.add( categoryOptionC );
            catopts.add( categoryOptionE );

            String key = "\"" + categoryOptionA.getUid() + "\"" + "\"" + categoryOptionC.getUid() + "\"" + "\""
                + categoryOptionE.getUid() + "\"";

            CategoryOptionCombo catoptcombo = map.getCategoryOptionCombo( key );

            assertNotNull( catoptcombo );
            assertTrue( catoptcombo.getCategoryOptions().containsAll( catopts ) );
        }
        catch ( CategoryComboMap.CategoryComboMapException ex )
        {
            assertTrue( ex.getMessage(), false );
        }
    }
}
