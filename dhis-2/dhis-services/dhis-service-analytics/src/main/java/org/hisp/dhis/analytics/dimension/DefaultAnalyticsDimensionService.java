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
package org.hisp.dhis.analytics.dimension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.dimension.AnalyticsDimensionService" )
@RequiredArgsConstructor
public class DefaultAnalyticsDimensionService
    implements AnalyticsDimensionService
{
    private final DataQueryService dataQueryService;

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager idObjectManager;

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
        }

        dimensions.addAll( idObjectManager.getDataDimensions( OrganisationUnitGroupSet.class ) );

        // TODO Filter org unit group sets

        return dimensions.stream()
            .filter( d -> aclService.canDataOrMetadataRead( user, d ) )
            .sorted()
            .collect( Collectors.toList() );
    }
}
