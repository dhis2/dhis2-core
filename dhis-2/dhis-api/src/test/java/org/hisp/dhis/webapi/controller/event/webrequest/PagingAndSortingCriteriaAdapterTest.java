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

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Test;

class PagingAndSortingCriteriaAdapterTest
{
    /**
     * Should not fail when paging=true and pageSize is null
     */
    @Test
    void shouldNotThrowExceptionWhenPagingTrueAndPageSizeIsNull()
    {
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter = new PagingAndSortingCriteriaAdapter()
        {
            @Override
            public Integer getPageSize()
            {
                // Redundant just to make test more readable
                return null;
            }
        };
        try
        {
            pagingAndSortingCriteriaAdapter.isPagingRequest();
        }
        catch ( Exception e )
        {
            fail( "Test was not meant to throw exception. Thrown exception is: " + e.getMessage() );
        }
    }

    @Test
    void pagingIsEnabledByDefault()
    {
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter = new PagingAndSortingCriteriaAdapter()
        {
        };
        assertFalse( toBooleanDefaultIfNull( pagingAndSortingCriteriaAdapter.isSkipPaging(), false ) );
        assertTrue( pagingAndSortingCriteriaAdapter.isPagingRequest() );
    }

    @Test
    @SneakyThrows
    void verifyGetOrder()
    {
        PagingAndSortingCriteriaAdapter tested = new PagingAndSortingCriteriaAdapter()
        {

            @Override
            public List<String> getAllowedOrderingFields()
            {
                return List.of( "field1", "field2" );
            }

            @Override
            public Optional<String> translateField( String dtoFieldName, boolean isLegacy )
            {
                return Optional.of( dtoFieldName.equals( "field1" ) ? "translatedField1" : dtoFieldName );
            }
        };
        tested.setOrder( List.of( OrderCriteria.of( "field1", SortDirection.ASC ),
            OrderCriteria.of( "field2", SortDirection.ASC ),
            OrderCriteria.of( "field3", SortDirection.ASC ) ) );
        Collection<String> orderField = tested.getOrder().stream().map( OrderCriteria::getField )
            .collect( Collectors.toList() );
        assertThat( orderField, hasSize( 2 ) );
        assertThat( orderField, containsInAnyOrder( "translatedField1", "field2" ) );
    }
}
