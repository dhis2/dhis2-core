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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.split;
import static org.hisp.dhis.feedback.ErrorCode.E2015;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.comparators.NullComparator;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Helper class responsible for providing sorting capabilities.
 */
public class OrderingHelper
{
    private static final int ORDERING_ATTRIBUTE = 0;

    private static final int ORDERING_VALUE = 1;

    private static final String DESC = "desc";

    /**
     * Sorts the given list based on the given sorting params.
     *
     * @param dimensionalItems
     * @param sortingParams
     */
    public static void sort( final List<BaseDimensionalItemObject> dimensionalItems, final OrderParams sortingParams )
    {
        if ( sortingParams != null && isNotEmpty( dimensionalItems ) )
        {
            final ComparatorChain<BaseDimensionalItemObject> chainOfComparators = new ComparatorChain<>();
            final Set<String> orderingPairs = sortingParams.getOrders();

            if ( sortingParams != null && isNotEmpty( orderingPairs ) )
            {
                for ( final String orderingPair : orderingPairs )
                {
                    chainOfComparators.addComparator( getComparator( orderingPair ) );
                }

                dimensionalItems.sort( chainOfComparators );
            }
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static Comparator<BaseDimensionalItemObject> getComparator( final String orderingParam )
    {
        final String[] orderingAttributes = split( orderingParam, ":" );
        final boolean hasValidOrderingAttributes = orderingAttributes != null & orderingAttributes.length == 2;

        if ( hasValidOrderingAttributes )
        {
            final BeanComparator<BaseDimensionalItemObject> comparator = new BeanComparator(
                orderingAttributes[ORDERING_ATTRIBUTE], new NullComparator<>( true ) );

            if ( DESC.equals( orderingAttributes[ORDERING_VALUE] ) )
            {
                return comparator.reversed();
            }

            return comparator;
        }
        else
        {
            throw new IllegalQueryException( new ErrorMessage( E2015, orderingParam ) );
        }
    }
}
