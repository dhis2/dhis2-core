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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.DimensionalItemHolder;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

@Service
public class DimensionalItemFilteringAndPagingService
{
    private final static Comparator<BaseDimensionalItemObject> DEFAULT_COMPARATOR = Comparator
        .comparing( BaseIdentifiableObject::getCreated );

    private final static Map<String, Comparator<BaseDimensionalItemObject>> ORDERING_MAP = ImmutableMap.of(
        "lastUpdated", Comparator.comparing( BaseIdentifiableObject::getLastUpdated ),
        "code", Comparator.comparing( BaseIdentifiableObject::getCode ),
        "uid", Comparator.comparing( BaseIdentifiableObject::getUid ),
        "name", Comparator.comparing( BaseIdentifiableObject::getName ) );

    public PagingWrapper<DimensionalItemHolder<?>> pageAndFilter(
        Supplier<Collection<BaseDimensionalItemObject>> dimensionalItems,
        DimensionalItemCriteria dimensionalItemCriteria )
    {

        PagingWrapper<DimensionalItemHolder<?>> pagingWrapper = new PagingWrapper<>( "dimensions" );

        Collection<BaseDimensionalItemObject> filteredItems = filterStream( dimensionalItems.get().stream(),
            dimensionalItemCriteria )
                .collect( Collectors.toList() );

        pagingWrapper = pagingWrapper.withInstances(
            sortedAndPagedStream( filteredItems.stream(), dimensionalItemCriteria )
                .map( this::asDimensionalItemHolder )
                .collect( Collectors.toList() ) );

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

    private DimensionalItemHolder<BaseDimensionalItemObject> asDimensionalItemHolder(
        BaseDimensionalItemObject dimensionalItem )
    {
        return DimensionalItemHolder.<BaseDimensionalItemObject> builder()
            .item( dimensionalItem )
            .dimensionItemType( dimensionalItem.getDimensionItemType() )
            .build();
    }

    private Stream<BaseDimensionalItemObject> filterStream(
        Stream<BaseDimensionalItemObject> dimensionalItems,
        DimensionalItemCriteria criteria )
    {
        DimensionalItemFilters dimensionalItemFilters = Optional.of( criteria )
            .map( DimensionalItemCriteria::getFilter )
            .map( DimensionalItemFilters::of )
            .orElse( DimensionalItemFilters.EMPTY_DATA_DIMENSION_FILTER );

        return dimensionalItems.filter( dimensionalItemFilters );
    }

    private Stream<BaseDimensionalItemObject> sortedAndPagedStream(
        Stream<BaseDimensionalItemObject> dimensionalItems,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteria )
    {

        if ( Objects.nonNull( pagingAndSortingCriteria.getOrder() ) && !pagingAndSortingCriteria.getOrder().isEmpty() )
        {

            OrderCriteria orderCriteria = pagingAndSortingCriteria.getOrder().get( 0 );

            Comparator<BaseDimensionalItemObject> comparator = ORDERING_MAP.keySet().stream()
                .filter( key -> key.equalsIgnoreCase( orderCriteria.getField() ) )
                .map( ORDERING_MAP::get )
                .findFirst()
                .orElse( DEFAULT_COMPARATOR );

            dimensionalItems = dimensionalItems.sorted( comparator );

            if ( Objects.nonNull( orderCriteria.getDirection() ) && !orderCriteria.getDirection().isAscending() )
            {
                dimensionalItems = dimensionalItems
                    .sorted( Comparator.reverseOrder() );
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
