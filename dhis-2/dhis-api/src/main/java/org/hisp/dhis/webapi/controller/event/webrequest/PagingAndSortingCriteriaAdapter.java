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
package org.hisp.dhis.webapi.controller.event.webrequest;

import static java.util.stream.Collectors.partitioningBy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;

/**
 * simplest implementation of PagingCriteria and SortingCriteria
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Data
@Slf4j
@NoArgsConstructor( access = AccessLevel.PROTECTED )
public abstract class PagingAndSortingCriteriaAdapter implements PagingCriteria, SortingCriteria
{

    /**
     * Page number to return.
     */
    private Integer page;

    /**
     * Page size.
     */
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Indicates whether to include the total number of pages in the paging
     * response.
     */
    private boolean totalPages;

    /**
     * Indicates whether paging should be skipped.
     */
    private boolean skipPaging;

    /**
     * order params
     */
    private List<OrderCriteria> order;

    /**
     * TODO: legacy flag can be removed when new tracker will have it's own
     * services. All new tracker export Criteria class extending this, will
     * override isLegacy returning false, so that it's true only for older ones.
     */
    private boolean isLegacy = true;

    private final Function<List<OrderCriteria>, List<OrderCriteria>> dtoNameToDatabaseNameTranslator = orderCriteria -> CollectionUtils
        .emptyIfNull( orderCriteria )
        .stream()
        .filter( Objects::nonNull )
        .map( oc -> OrderCriteria.of(
            translateField( oc.getField(), isLegacy() )
                .orElse( oc.getField() ),
            oc.getDirection() ) )
        .collect( Collectors.toList() );

    public boolean isPagingRequest()
    {
        return !isSkipPaging();
    }

    @Override
    public List<OrderCriteria> getOrder()
    {
        if ( getAllowedOrderingFields().isEmpty() )
        {
            return dtoNameToDatabaseNameTranslator.apply( order );
        }

        Map<Boolean, List<OrderCriteria>> orderCriteriaPartitionedByAllowance = CollectionUtils.emptyIfNull( order )
            .stream()
            .collect(
                partitioningBy( this::isAllowed ) );

        CollectionUtils.emptyIfNull( orderCriteriaPartitionedByAllowance.get( false ) )
            .forEach( disallowedOrderFieldConsumer() );

        return dtoNameToDatabaseNameTranslator.apply( orderCriteriaPartitionedByAllowance.get( true ) );
    }

    private boolean isAllowed( OrderCriteria orderCriteria )
    {
        return getAllowedOrderingFields().contains( orderCriteria.getField() );
    }

    protected Consumer<OrderCriteria> disallowedOrderFieldConsumer()
    {
        return orderCriteria -> log.warn( "Ordering by " + orderCriteria.getField() + " is not supported" );
    }

    public boolean isSortingRequest()
    {
        return !CollectionUtils.emptyIfNull( getOrder() ).isEmpty();
    }

    public interface EntityNameSupplier
    {
        String getEntityName();
    }

}
