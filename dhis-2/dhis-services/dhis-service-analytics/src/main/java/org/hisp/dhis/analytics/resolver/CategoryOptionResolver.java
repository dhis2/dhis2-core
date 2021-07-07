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
package org.hisp.dhis.analytics.resolver;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.expression.Expression.EXP_CLOSE;
import static org.hisp.dhis.expression.Expression.EXP_OPEN;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;

/**
 * @author Dusan Bernat
 */

@Service( "org.hisp.dhis.analytics.resolver.CategoryOptionResolver" )
public class CategoryOptionResolver implements ExpressionResolver
{
    private final ExpressionService expressionService;

    private final CategoryOptionStore categoryOptionStore;

    private static final String LEFT_BRACKET = "(";

    private static final String RIGHT_BRACKET = ")";

    private static final String CATEGORY_OPTION_PREFIX = "co:";

    private static final String EMPTY_STRING = "";

    public CategoryOptionResolver( ExpressionService expressionService, CategoryOptionStore categoryOptionStore )
    {
        checkNotNull( categoryOptionStore );
        checkNotNull( expressionService );

        this.expressionService = expressionService;
        this.categoryOptionStore = categoryOptionStore;
    }

    @Override
    public String resolve( String expression )
    {
        Set<DimensionalItemId> dimItemIds = expressionService.getExpressionDimensionalItemIds( expression,
            INDICATOR_EXPRESSION );

        Map<String, List<String>> resolvedOperands = new HashMap<>();

        dimItemIds.forEach( id -> {
            if ( id.getId1().contains( CATEGORY_OPTION_PREFIX ) )
            {
                CategoryOption co = categoryOptionStore
                    .getByUid( id.getId1().replace( CATEGORY_OPTION_PREFIX, EMPTY_STRING ) );

                String operand = EXP_OPEN + id.getId0() + SEPARATOR + id.getId1() +
                    (id.getId2() != null ? SEPARATOR + id.getId2() : EMPTY_STRING) + EXP_CLOSE;

                resolvedOperands.put( operand, co.getCategoryOptionCombos().stream()
                    .map( c -> operand.replace( id.getId1(), c.getUid() ) ).collect( Collectors.toList() ) );
            }
        } );

        for ( Map.Entry<String, List<String>> entry : resolvedOperands.entrySet() )
        {
            String operand = entry.getKey();

            List<String> resolved = entry.getValue();

            expression = expression.replace( operand,
                LEFT_BRACKET + Joiner.on( "+" ).join( resolved ) + RIGHT_BRACKET );
        }

        return expression;
    }
}
