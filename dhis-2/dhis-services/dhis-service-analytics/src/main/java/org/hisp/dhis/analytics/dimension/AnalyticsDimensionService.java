package org.hisp.dhis.analytics.dimension;

import java.util.List;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalObject;

public interface AnalyticsDimensionService
{
    List<DimensionalObject> getRecommendedDimensions( DataQueryRequest request );
    
    List<DimensionalObject> getRecommendedDimensions( DataQueryParams params );
}
