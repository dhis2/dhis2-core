package org.hisp.dhis.analytics;

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

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class DimensionOptionTest
    extends DhisConvenienceTest
{
    private DataElement deA;
    private Period peA;
    private OrganisationUnit ouA;
    
    private List<DimensionItem> options;
    
    @Before
    public void before()
    {
        deA = createDataElement( 'A', new DataElementCategoryCombo() );
        peA = createPeriod( "2000Q1" );
        ouA = createOrganisationUnit( 'A' );
        
        options = new ArrayList<>();
        options.add( new DimensionItem( DATA_X_DIM_ID, deA ) );
        options.add( new DimensionItem( PERIOD_DIM_ID, peA ) );
        options.add( new DimensionItem( ORGUNIT_DIM_ID, ouA ) );
    }
    
    @Test
    public void testAsOptionKey()
    {
        String expected = deA.getUid() + DIMENSION_SEP + peA.getUid() + DIMENSION_SEP + ouA.getUid();
        
        assertEquals( expected, DimensionItem.asItemKey( options ) );
        assertEquals( EMPTY, DimensionItem.asItemKey( null ) );
    }
    
    @Test
    public void testGetOptions()
    {
        String[] expected = { deA.getUid(), peA.getUid(), ouA.getUid() };
        
        assertArrayEquals( expected, DimensionItem.getItemIdentifiers( options ) );
        assertArrayEquals( new String[0], DimensionItem.getItemIdentifiers( null ) );
    }
    
    @Test
    public void testGetPeriodOption()
    {
        assertEquals( peA, DimensionItem.getPeriodItem( options ) );
        assertEquals( null, DimensionItem.getPeriodItem( null ) );
    }
}
