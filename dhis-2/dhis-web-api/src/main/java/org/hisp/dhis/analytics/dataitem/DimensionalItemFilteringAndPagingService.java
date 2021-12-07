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
package org.hisp.dhis.analytics.dataitem;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.event.DimensionWrapper;
import org.hisp.dhis.common.DimensionalItemCriteria;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

@Service
@RequiredArgsConstructor
public class DimensionalItemFilteringAndPagingService
{

    @NonNull
    private final FieldFilterService fieldFilterService;

    private final static Comparator<DimensionWrapper> DEFAULT_COMPARATOR = Comparator
        .comparing( DimensionWrapper::getCreated );

    private final static Map<String, Comparator<DimensionWrapper>> ORDERING_MAP = ImmutableMap.of(
        "lastUpdated", Comparator.comparing( DimensionWrapper::getLastUpdated ),
        "code", Comparator.comparing( DimensionWrapper::getCode ),
        "uid", Comparator.comparing( DimensionWrapper::getId ),
        "id", Comparator.comparing( DimensionWrapper::getId ),
        "name", Comparator.comparing( DimensionWrapper::getName ) );

    public PagingWrapper<ObjectNode> pageAndFilter(
        Collection<DimensionWrapper> dimensionalItems,
        DimensionalItemCriteria dimensionalItemCriteria,
        List<String> fields )
    {

        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>( "dimensions" );

        Collection<DimensionWrapper> filteredItems = filterStream( dimensionalItems.stream(),
            dimensionalItemCriteria )
                .collect( Collectors.toList() );

        var params = FieldFilterParams.of( sortedAndPagedStream( filteredItems.stream(), dimensionalItemCriteria )
            .collect( Collectors.toList() ), fields );

        var objectNodes = fieldFilterService.toObjectNodes( params );

        pagingWrapper = pagingWrapper.withInstances( objectNodes );

        if ( dimensionalItemCriteria.isPagingRequest() )
        {
            pagingWrapper = pagingWrapper.withPager( PagingWrapper.Pager.builder()
                .page( Optional.ofNullable( dimensionalItemCriteria.getPage() ).orElse( 1 ) )
                .pageSize( dimensionalItemCriteria.getPageSize() )
                .total( (long) filteredItems.size() )
                .build() );
        }

        return pagingWrapper;
    }

    private Stream<DimensionWrapper> filterStream(
        Stream<DimensionWrapper> dimensionalItems,
        DimensionalItemCriteria criteria )
    {
        DimensionalItemFilters dimensionalItemFilters = Optional.of( criteria )
            .map( DimensionalItemCriteria::getFilter )
            .map( DimensionalItemFilters::of )
            .orElse( DimensionalItemFilters.EMPTY_DATA_DIMENSION_FILTER );

        return dimensionalItems.filter( dimensionalItemFilters );
    }

    private Stream<DimensionWrapper> sortedAndPagedStream(
        Stream<DimensionWrapper> dimensionalItems,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteria )
    {

        if ( Objects.nonNull( pagingAndSortingCriteria.getOrder() ) && !pagingAndSortingCriteria.getOrder().isEmpty() )
        {

            OrderCriteria orderCriteria = pagingAndSortingCriteria.getOrder().get( 0 );

            Comparator<DimensionWrapper> comparator = ORDERING_MAP.keySet().stream()
                .filter( key -> key.equalsIgnoreCase( orderCriteria.getField() ) )
                .map( ORDERING_MAP::get )
                .findFirst()
                .orElse( DEFAULT_COMPARATOR );

            dimensionalItems = dimensionalItems.sorted( comparator );

            if ( Objects.nonNull( orderCriteria.getDirection() ) && !orderCriteria.getDirection().isAscending() )
            {
                dimensionalItems = dimensionalItems.sorted( comparator.reversed() );
            }
            else
            {
                dimensionalItems = dimensionalItems.sorted( comparator );
            }

        }
        else
        {
            dimensionalItems = dimensionalItems.sorted( DEFAULT_COMPARATOR );
        }

        if ( pagingAndSortingCriteria.isPagingRequest() )
        {
            dimensionalItems = dimensionalItems
                .skip( pagingAndSortingCriteria.getFirstResult() )
                .limit( pagingAndSortingCriteria.getPageSize() );
        }

        return dimensionalItems;
    }

}
