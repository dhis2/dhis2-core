package org.hisp.dhis.analytics.dimension;

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
