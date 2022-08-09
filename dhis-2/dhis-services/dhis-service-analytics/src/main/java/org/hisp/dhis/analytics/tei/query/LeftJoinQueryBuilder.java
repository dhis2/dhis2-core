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

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class LeftJoinQueryBuilder
{

    public static Pair<Renderable, Renderable> of( AnalyticsSortingParams analyticsSortingParams,
        QueryContext queryContext )
    {
        return Pair.of(
            () -> "(" + getSelect( analyticsSortingParams, queryContext ) + ") \""
                + analyticsSortingParams.getOrderBy().toString() + "\"",
            () -> getCondition( analyticsSortingParams ) );
    }

    private static String getCondition( AnalyticsSortingParams analyticsSortingParams )
    {
        return TEI_ALIAS + ".trackedentityinstanceuid = \"" + analyticsSortingParams.getOrderBy().toString()
            + "\".trackedentityinstanceuid";
    }

    private static String getSelect( AnalyticsSortingParams analyticsSortingParams, QueryContext queryContext )
    {
        String programUid = analyticsSortingParams.getOrderBy().getProgram().getElement().getUid();
        String programStageUid = analyticsSortingParams.getOrderBy().getProgramStage().getElement().getUid();
        String dataValueUid = analyticsSortingParams.getOrderBy().getDimension().getUid();
        return "SELECT EVT.trackedentityinstanceuid, EVT.VALUE" +
            "      FROM (SELECT programinstanceuid" +
            "            FROM " + ANALYTICS_TEI_ENR + queryContext.getTeTTableSuffix() +
            "            WHERE programuid = " + queryContext.bindParamAndGetIndex( programUid ) +
            "            ORDER BY enrollmentdate DESC" +
            "            LIMIT 1 OFFSET 0) ENR," +
            "           (SELECT trackedentityinstanceuid," +
            "                   programinstanceuid," +
            "                   (eventdatavalues -> '" + dataValueUid + "' ->> 'value')::NUMERIC AS VALUE" +
            "            FROM " + ANALYTICS_TEI_EVT + queryContext.getTeTTableSuffix() +
            "            WHERE programuid = " + queryContext.bindParamAndGetIndex( programUid ) +
            "              AND programstageuid = " + queryContext.bindParamAndGetIndex( programStageUid ) +
            "            ORDER BY executiondate DESC" +
            "            LIMIT 1 OFFSET 0) EVT" +
            "      WHERE ENR.programinstanceuid = EVT.programinstanceuid";
    }
}
