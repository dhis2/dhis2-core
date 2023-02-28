/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.query;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.common.query.ConstantValuesRenderer.hasNullValue;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.GE;
import static org.hisp.dhis.common.QueryOperator.GT;
import static org.hisp.dhis.common.QueryOperator.LE;
import static org.hisp.dhis.common.QueryOperator.LT;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.common.QueryOperator.NILIKE;
import static org.hisp.dhis.common.QueryOperator.NLIKE;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.hisp.dhis.commons.util.TextUtils.SPACE;
import static org.hisp.dhis.feedback.ErrorCode.E2035;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryOperator;

/**
 * This class is responsible for rendering a binary condition.
 */
@RequiredArgsConstructor( staticName = "of" )
public class BinaryConditionRenderer extends BaseRenderable
{
    private final Renderable left;

    private final QueryOperator queryOperator;

    private final Renderable right;

    public static BinaryConditionRenderer fieldsEqual( String leftAlias, String left, String rightAlias, String right )
    {
        return BinaryConditionRenderer.of(
            Field.of( leftAlias, () -> left, EMPTY ), EQ, Field.of( rightAlias, () -> right, EMPTY ) );
    }

    public static BinaryConditionRenderer of( Renderable field, QueryOperator queryOperator, List<String> values,
        ValueTypeMapping valueTypeMapping, QueryContext queryContext )
    {
        return BinaryConditionRenderer.of( field, queryOperator,
            ConstantValuesRenderer.of( values, valueTypeMapping, queryContext ) );
    }

    private static final Collection<QueryOperator> comparisonOperators = Arrays.asList( GT, GE, LT, LE );

    @Nonnull
    @Override
    public String render()
    {
        // EQ / IN
        if ( QueryOperator.EQ == queryOperator || QueryOperator.IN == queryOperator )
        {
            return InOrEqConditionRenderer.of( left, right ).render();
        }

        // NE / NEQ
        if ( NEQ == queryOperator )
        {
            if ( hasNullValue( right ) )
            {
                return IsNullConditionRenderer.of( left, false ).render();
            }
            return OrCondition.of(
                List.of(
                    IsNullConditionRenderer.of( left, true ),
                    NotEqConditionRenderer.of( left, right ) ) )
                .render();
        }

        // LIKE / ILIKE
        if ( LikeOperatorMapper.likeOperators().contains( queryOperator ) )
        {
            return NullValueAwareConditionRenderer.of(
                LikeOperatorMapper.of( queryOperator ), left, right ).render();
        }

        // NLIKE / NILIKE
        if ( NLIKE == queryOperator || NILIKE == queryOperator )
        {
            if ( hasNullValue( right ) )
            {
                return IsNullConditionRenderer.of( left, false ).render();
            }

            return OrCondition.of(
                List.of(
                    IsNullConditionRenderer.of( left, true ),
                    NLIKE == queryOperator
                        ? NotLikeConditionRenderer.of( left, right )
                        : NotILikeConditionRenderer.of( left, right ) ) )
                .render();
        }

        if ( comparisonOperators.contains( queryOperator ) )
        {
            return left.render() + SPACE + queryOperator.getValue() + SPACE + right.render();
        }

        throw new IllegalQueryException( E2035, queryOperator );
    }

    /**
     * This class is responsible for mapping a "like" {@link QueryOperator}
     */
    @RequiredArgsConstructor
    private enum LikeOperatorMapper
    {
        LIKE( QueryOperator.LIKE, LikeConditionRenderer::of ),
        ILIKE( QueryOperator.ILIKE, ILikeConditionRenderer::of );

        private final QueryOperator queryOperator;

        private final BiFunction<Renderable, Renderable, Renderable> mapper;

        static BiFunction<Renderable, Renderable, Renderable> of( QueryOperator queryOperator )
        {
            return Arrays.stream( values() )
                .filter( likeOperatorMapper -> likeOperatorMapper.queryOperator == queryOperator )
                .map( likeOperatorMapper -> likeOperatorMapper.mapper )
                .findFirst()
                .orElseThrow( () -> new IllegalArgumentException( "Unsupported operator: " + queryOperator ) );
        }

        static Collection<QueryOperator> likeOperators()
        {
            return Arrays.stream( values() )
                .map( likeOperatorMapper -> likeOperatorMapper.queryOperator )
                .collect( toList() );
        }
    }
}
