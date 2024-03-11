package org.hisp.dhis.analytics.dimension;

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

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsDimensionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private AnalyticsDimensionService dimensionService;
    
    @Test
    public void testGetRecommendedDimensions()
    {
        CategoryOption coA = createCategoryOption( 'A' );
        CategoryOption coB = createCategoryOption( 'B' );
        CategoryOption coC = createCategoryOption( 'C' );
        CategoryOption coD = createCategoryOption( 'D' );
        
        Category caA = createCategory( 'A', coA );
        Category caB = createCategory( 'B', coB );
        Category caC = createCategory( 'C', coC );
        Category caD = createCategory( 'D', coD );
        caD.setDataDimension( false );
        
        CategoryCombo ccA = createCategoryCombo( 'A', caA, caB );
        CategoryCombo ccB = createCategoryCombo( 'A', caC, caD );
        
        DataSet dsA = createDataSet( 'A' );
        dsA.setCategoryCombo( ccB );
        
        DataElement deA = createDataElement( 'A', ccA );
        DataElement deB = createDataElement( 'B', ccA );
        
        dsA.addDataSetElement( deB );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA, deB ) )
            .build();
        
        List<DimensionalObject> dimensons = dimensionService.getRecommendedDimensions( params );
        
        assertEquals( 3, dimensons.size() );
        assertTrue( dimensons.contains( caA ) );
        assertTrue( dimensons.contains( caB ) );
        assertTrue( dimensons.contains( caC ) );
        
        params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA ) )
            .build();
        
        dimensons = dimensionService.getRecommendedDimensions( params );

        assertEquals( 2, dimensons.size() );
        assertTrue( dimensons.contains( caA ) );
        assertTrue( dimensons.contains( caB ) );
    }
}
