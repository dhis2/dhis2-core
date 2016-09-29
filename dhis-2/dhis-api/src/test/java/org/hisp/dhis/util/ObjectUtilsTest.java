package org.hisp.dhis.util;

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

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class ObjectUtilsTest
{
    @Test
    public void testJoin()
    {
        DataElement deA = new DataElement( "DataElementA" );
        DataElement deB = new DataElement( "DataElementB" );
        DataElement deC = new DataElement( "DataElementC" );
        
        List<DataElement> elements = Lists.newArrayList( deA, deB, deC );
        
        String actual = ObjectUtils.join( elements, ", ", de -> de.getName() );
                
        assertEquals( "DataElementA, DataElementB, DataElementC", actual );
        assertEquals( null, ObjectUtils.join( null, ", ", null ) );
    }
    
    @Test
    public void testGetAll()
    {
        DataElementCategory ctA = new DataElementCategory( "CategoryA", DataDimensionType.DISAGGREGATION );
        DataElementCategory ctB = new DataElementCategory( "CategoryB", DataDimensionType.DISAGGREGATION );
        DataElementCategory ctC = new DataElementCategory( "CategoryC", DataDimensionType.DISAGGREGATION );
        DataElementCategory ctD = new DataElementCategory( "CategoryD", DataDimensionType.DISAGGREGATION );

        DataElementCategoryCombo ccA = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION );
        DataElementCategoryCombo ccB = new DataElementCategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION );
        
        ccA.addDataElementCategory( ctA );
        ccA.addDataElementCategory( ctB );
        ccB.addDataElementCategory( ctC );
        ccB.addDataElementCategory( ctD );
        
        List<DataElementCategoryCombo> ccs = Lists.newArrayList( ccA, ccB );
        
        Set<DataElementCategory> cts = ObjectUtils.getAll( ccs, cc -> cc.getCategories() );
        
        assertEquals( 4, cts.size() );
        assertTrue( cts.contains( ctA ) );
        assertTrue( cts.contains( ctB ) );
        assertTrue( cts.contains( ctC ) );
        assertTrue( cts.contains( ctD ) );        
    }
}
