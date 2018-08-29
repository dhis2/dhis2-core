package org.hisp.dhis.analytics.dimension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultAnalyticsDimensionService
    implements AnalyticsDimensionService
{
    @Autowired
    private DataQueryService dataQueryService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public List<DimensionalObject> getRecommendedDimensions( DataQueryRequest request )
    {
        DataQueryParams params = dataQueryService.getFromRequest( request );
        
        return getRecommendedDimensions( params );
    }
    
    @Override
    public List<DimensionalObject> getRecommendedDimensions( DataQueryParams params )
    {
        User user = currentUserService.getCurrentUser();
        
        Set<DimensionalObject> dimensions = new HashSet<>();
        
        if ( !params.getDataElements().isEmpty() )
        {
            dimensions.addAll( params.getDataElements().stream()
                .map( de -> ((DataElement) de).getCategoryCombos() )
                .flatMap( cc -> cc.stream() )
                .map( cc -> cc.getCategories() )
                .flatMap( c -> c.stream() )
                .filter( Category::isDataDimension )
                .collect( Collectors.toSet() ) );
            
            dimensions.addAll( params.getDataElements().stream()
                .map( de -> ((DataElement) de).getDataSets() )
                .flatMap( ds -> ds.stream() )
                .map( ds -> ds.getCategoryCombo().getCategories() )
                .flatMap( c -> c.stream() )
                .filter( Category::isDataDimension )
                .collect( Collectors.toSet() ) );
            
            //TODO data set elements
        }
        
        return dimensions.stream()
            .filter( d -> aclService.canDataRead( user, d ) )
            .sorted()
            .collect( Collectors.toList() );
    }
}
