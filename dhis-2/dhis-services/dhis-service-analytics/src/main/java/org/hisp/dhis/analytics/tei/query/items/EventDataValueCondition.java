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
package org.hisp.dhis.analytics.tei.query.items;

import static org.hisp.dhis.analytics.tei.query.From.ofSingleTableAndAlias;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_1_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PSI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PS_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.tei.query.BaseRenderable;
import org.hisp.dhis.analytics.tei.query.Field;
import org.hisp.dhis.analytics.tei.query.From;
import org.hisp.dhis.analytics.tei.query.LimitOffset;
import org.hisp.dhis.analytics.tei.query.Order;
import org.hisp.dhis.analytics.tei.query.Query;
import org.hisp.dhis.analytics.tei.query.QueryContext;
import org.hisp.dhis.analytics.tei.query.QueryContextConstants;
import org.hisp.dhis.analytics.tei.query.Renderable;
import org.hisp.dhis.analytics.tei.query.Select;
import org.hisp.dhis.analytics.tei.query.Where;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

@RequiredArgsConstructor( staticName = "of" )
public class EventDataValueCondition extends BaseRenderable
{
    private static final Select SELECT_1 = Select.of( "1" );

    private final DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    @Override
    public String render()
    {
        return ExistsCondition.of( getSubQuery() ).render();
    }

    private Renderable getSubQuery()
    {
        Query innermostEventSubQuery = Query.builder()
            .select( SELECT_1 )
            .from( ofSingleTableAndAlias( ANALYTICS_TEI_EVT + queryContext.getTeTTableSuffix(), EVT_1_ALIAS ) )
            .where( Where.ofConditions(
                BinaryCondition.fieldsEqual( EVT_ALIAS, PSI_UID, EVT_1_ALIAS, PSI_UID ),
                getItemCondition() ) )
            .build();

        Query innerEventSubQuery = Query.builder()
            .select( SELECT_1 )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_EVT + queryContext.getTeTTableSuffix(), EVT_ALIAS ) )
            .where( Where.ofConditions(
                BinaryCondition.fieldsEqual( EVT_ALIAS, PI_UID, ENR_ALIAS, PI_UID ),
                BinaryCondition.of(
                    Field.of( EVT_ALIAS, () -> PS_UID, null ),
                    QueryOperator.EQ,
                    () -> queryContext.bindParamAndGetIndex( getProgramStageUid() ) ),
                ExistsCondition.of( innermostEventSubQuery ) ) )
            // TODO: negative offset will require ASC instead of DESC
            .order( Order.ofOrder( "executiondate desc" ) )
            .limit( getProgramStageLimitOffset() )
            .build();

        return Query.builder()
            .select( SELECT_1 )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_ENR + queryContext.getTeTTableSuffix(), ENR_ALIAS ) )
            .where( Where.ofConditions(
                BinaryCondition.fieldsEqual( ENR_ALIAS, TEI_UID, TEI_ALIAS, TEI_UID ),
                BinaryCondition.of(
                    Field.of( ENR_ALIAS, () -> QueryContextConstants.P_UID, null ),
                    QueryOperator.EQ,
                    () -> queryContext.bindParamAndGetIndex( getProgramUid() ) ),
                ExistsCondition.of( innerEventSubQuery ) ) )
            // TODO: negative offset will require ASC instead of DESC
            .order( Order.ofOrder( "enrollmentdate desc" ) )
            .limit( getProgramLimitOffset() )
            .build();
    }

    private String getProgramUid()
    {
        return dimensionIdentifier.getProgram().getElement().getUid();
    }

    private String getProgramStageUid()
    {
        return dimensionIdentifier.getProgramStage().getElement().getUid();
    }

    private LimitOffset getProgramStageLimitOffset()
    {
        // TODO: at the moment we only support integer index as offset
        return LimitOffset.ofStrings( "1", Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getProgramStage )
            .map( DimensionIdentifier.ElementWithOffset::getOffset )
            .orElse( "0" ) );
    }

    private LimitOffset getProgramLimitOffset()
    {
        // TODO: at the moment we only support integer index as offset
        return LimitOffset.ofStrings( "1", Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getProgram )
            .map( DimensionIdentifier.ElementWithOffset::getOffset )
            .orElse( "0" ) );
    }

    private BinaryCondition getItemCondition()
    {
        ValueTypeMapping valueTypeMapping = ValueTypeMapping
            .fromValueType( dimensionIdentifier.getDimension().getValueType() );

        DimensionParamItem item = dimensionIdentifier.getDimension().getItems().get( 0 );
        String doUid = dimensionIdentifier.getDimension().getDimensionObjectUid();

        Renderable value = item.getOperator().equals( QueryOperator.IN )
            ? () -> queryContext.bindParamAndGetIndex( convertToType( valueTypeMapping, item.getValues() ) )
            : () -> queryContext.bindParamAndGetIndex( convertToType( valueTypeMapping, item.getValues().get( 0 ) ) );

        return BinaryCondition.of(
            RenderableDataValue.of( EVT_1_ALIAS, doUid, valueTypeMapping ),
            item.getOperator(),
            value );
    }

    private Object convertToType( ValueTypeMapping valueTypeMapping, String s )
    {
        if ( valueTypeMapping.equals( ValueTypeMapping.NUMERIC ) )
        {
            return new BigDecimal( s );
        }
        return s;
    }

    private Object convertToType( ValueTypeMapping valueTypeMapping, List<String> values )
    {
        if ( valueTypeMapping.equals( ValueTypeMapping.NUMERIC ) )
        {
            return values.stream()
                .map( BigDecimal::new )
                .collect( Collectors.toList() );
        }
        return values;
    }

}
