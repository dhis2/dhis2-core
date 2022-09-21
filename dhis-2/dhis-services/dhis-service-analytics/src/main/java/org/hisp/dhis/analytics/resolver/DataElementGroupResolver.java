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
package org.hisp.dhis.analytics.resolver;

import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupStore;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;

/**
 * @author Dusan Bernat
 */

@Service( "org.hisp.dhis.analytics.resolver.DataElementGroupResolver" )
@RequiredArgsConstructor
public class DataElementGroupResolver implements ExpressionResolver
{
    private final ExpressionService expressionService;

    private final DataElementGroupStore dataElementGroupStore;

    private static final String LEFT_BRACKET = "(";

    private static final String RIGHT_BRACKET = ")";

    private static final String DATA_ELEMENT_GROUP_PREFIX = "deGroup:";

    private static final String EMPTY_STRING = "";

    @Override
    public String resolve( String expression )
    {
        Set<DimensionalItemId> dimItemIds = expressionService.getExpressionDimensionalItemIds( expression,
            INDICATOR_EXPRESSION );

        for ( DimensionalItemId id : dimItemIds )
        {
            if ( id.getItem() != null && id.getId0() != null && id.getId0().startsWith( DATA_ELEMENT_GROUP_PREFIX ) )
            {
                DataElementGroup deGroup = dataElementGroupStore
                    .getByUid( id.getId0().replace( DATA_ELEMENT_GROUP_PREFIX, EMPTY_STRING ) );

                if ( deGroup != null )
                {
                    List<String> resolved = deGroup.getMembers()
                        .stream()
                        .map( de -> id.getItem().replace( id.getId0(), de.getUid() ) )
                        .collect( Collectors.toList() );

                    expression = expression.replace( id.getItem(),
                        LEFT_BRACKET + Joiner.on( "+" ).join( resolved ) + RIGHT_BRACKET );
                }
            }
        }

        return expression;
    }
}
