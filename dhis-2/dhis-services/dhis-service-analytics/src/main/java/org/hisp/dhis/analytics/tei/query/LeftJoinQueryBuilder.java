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
package org.hisp.dhis.analytics.tei.query;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.shared.ValueTypeMapping.fromValueType;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;

import lombok.NoArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.shared.ValueTypeMapping;
import org.hisp.dhis.analytics.shared.query.BinaryCondition;
import org.hisp.dhis.analytics.shared.query.DoubleQuotingRenderable;
import org.hisp.dhis.analytics.shared.query.Renderable;

@NoArgsConstructor( access = PRIVATE )
public class LeftJoinQueryBuilder
{

    public static Pair<Renderable, Renderable> of( AnalyticsSortingParams orderBy, QueryContext queryContext )
    {
        return Pair.of(
            () -> "(" + getSelect( orderBy, queryContext ) + ") \"" + orderBy.getOrderBy().toString() + "\"",
            getCondition( orderBy ) );
    }

    private static Renderable getCondition( AnalyticsSortingParams sortingParams )
    {
        return BinaryCondition.fieldsEqual(
            TEI_ALIAS,
            TEI_UID,
            DoubleQuotingRenderable.of( sortingParams.getOrderBy().toString() ).render(),
            TEI_UID );
    }

    private static String getSelect( AnalyticsSortingParams sortingParams, QueryContext queryContext )
    {
        ValueTypeMapping vtMapping = fromValueType( sortingParams.getOrderBy().getDimension().getValueType() );

        String programUid = sortingParams.getOrderBy().getProgram().getElement().getUid();
        String programStageUid = sortingParams.getOrderBy().getProgramStage().getElement().getUid();
        String dataValueUid = sortingParams.getOrderBy().getDimension().getUid();

        return new StringBuilder( "select evt.trackedentityinstanceuid, evt.value" )
            .append( " from (select programinstanceuid" )
            .append( " from " + ANALYTICS_TEI_ENR + queryContext.getTetTableSuffix() )
            .append( " where programuid = " + queryContext.bindParamAndGetIndex( programUid ) )
            .append( " order by enrollmentdate desc" )
            .append( " limit 1 offset 0) enr," )
            .append( " (select trackedentityinstanceuid," )
            .append( " programinstanceuid," + RenderableDataValue.of( null, dataValueUid, vtMapping ).render() )
            .append( " as value" )
            .append( " from " + ANALYTICS_TEI_EVT + queryContext.getTetTableSuffix() )
            .append( " where programuid = " + queryContext.bindParamAndGetIndex( programUid ) )
            .append( " and programstageuid = " + queryContext.bindParamAndGetIndex( programStageUid ) )
            .append( " order by executiondate desc" )
            .append( " limit 1 offset 0) evt" )
            .append( " where enr.programinstanceuid = evt.programinstanceuid" ).toString();
    }
}
