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
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import java.util.Arrays;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.tei.query.BaseRenderable;
import org.hisp.dhis.analytics.tei.query.From;
import org.hisp.dhis.analytics.tei.query.LimitOffset;
import org.hisp.dhis.analytics.tei.query.Order;
import org.hisp.dhis.analytics.tei.query.Query;
import org.hisp.dhis.analytics.tei.query.QueryContext;
import org.hisp.dhis.analytics.tei.query.Renderable;
import org.hisp.dhis.analytics.tei.query.Select;
import org.hisp.dhis.analytics.tei.query.Where;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
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
                () -> EVT_ALIAS + ".programstageinstanceuid" + "=" + EVT_1_ALIAS + ".programstageinstanceuid",
                getItemCondition() ) )
            .build();

        Query innerEventSubquery = Query.builder()
            .select( SELECT_1 )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_EVT + queryContext.getTeTTableSuffix(), EVT_ALIAS ) )
            .where( Where.ofConditions(
                () -> EVT_ALIAS + ".programinstanceuid" + "=" + ENR_ALIAS + ".programinstanceuid",
                () -> EVT_ALIAS + ".programstageuid = " + queryContext.bindParamAndGetIndex( getProgramStageUid() ),
                ExistsCondition.of( innermostEventSubQuery ) ) )
            // TODO: negative offset will require ASC instead of DESC
            .order( Order.ofOrder( "executiondate desc" ) )
            .limit( getProgramStageLimitOffset() )
            .build();

        return Query.builder()
            .select( SELECT_1 )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_ENR + queryContext.getTeTTableSuffix(), ENR_ALIAS ) )
            .where( Where.ofConditions(
                () -> ENR_ALIAS + ".trackedentityinstanceuid" + "=" + TEI_ALIAS
                    + ".trackedentityinstanceuid",
                () -> ENR_ALIAS + ".programuid" + "= " + queryContext.bindParamAndGetIndex( getProgramUid() ),
                ExistsCondition.of( innerEventSubquery ) ) )
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

    private Renderable getItemCondition()
    {
        ValueTypeMapping valueTypeMapping = ValueTypeMapping
            .fromValueType( dimensionIdentifier.getDimension().getValueType() );
        return () -> "(" + EVT_1_ALIAS + ".eventdatavalues -> '"
            + dimensionIdentifier.getDimension().getDimensionObjectUid() + "'" +
            " ->> 'value')::" + valueTypeMapping.name() + StringUtils.SPACE + renderItem();
    }

    private String renderItem()
    {
        DimensionParamItem item = dimensionIdentifier.getDimension().getItems().get( 0 );
        QueryOperator operator = item.getOperator();
        if ( operator == QueryOperator.IN )
        {
            return "IN (" + queryContext.bindParamAndGetIndex( item.getValues() ) + ")";
        }
        return operator.getValue() + queryContext.bindParamAndGetIndex( item.getValues().get( 0 ) );
    }

    private enum ValueTypeMapping
    {
        // TODO: adds mappings here
        NUMERIC( ValueType.INTEGER, ValueType.INTEGER_NEGATIVE, ValueType.INTEGER_POSITIVE,
            ValueType.INTEGER_ZERO_OR_POSITIVE ),
        STRING();

        private final ValueType[] valueTypes;

        ValueTypeMapping( ValueType... valueTypes )
        {
            this.valueTypes = valueTypes;
        }

        static ValueTypeMapping fromValueType( ValueType valueType )
        {
            return Arrays.stream( values() )
                .filter( valueTypeMapping -> valueTypeMapping.supports( valueType ) )
                .findFirst()
                .orElse( STRING );
        }

        private boolean supports( ValueType valueType )
        {
            return Arrays.stream( valueTypes )
                .anyMatch( vt -> vt == valueType );
        }

    }

}
