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
import static org.hisp.dhis.analytics.shared.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;

import java.util.List;

import lombok.NoArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.shared.ValueTypeMapping;
import org.hisp.dhis.analytics.shared.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.shared.query.Renderable;

@NoArgsConstructor( access = PRIVATE )
public class LeftJoinQueryBuilder
{
    public static List<Pair<Renderable, Renderable>> of( AnalyticsSortingParams orderBy, QueryContext queryContext )
    {
        if ( orderBy.getOrderBy().getDimension()
            .getDimensionParamObjectType() == DimensionParamObjectType.STATIC_DIMENSION &&
            orderBy.getOrderBy().getProgramStage().getElement() != null )
        {
            // static event
            return List.of(
                Pair.of(
                    () -> "(" + getEnrollmentSelectWithStaticDimension( orderBy, queryContext ) + ") \"" + ENR_ALIAS
                        + "\"",
                    getEnrollmentConditionWithStaticDimension() ),
                Pair.of(
                    () -> "(" + getEventSelectWithStaticDimension( orderBy, queryContext ) + ") \"" + EVT_ALIAS + "\"",
                    getEventConditionWithStaticDimension() ) );

        }
        else if ( orderBy.getOrderBy().getDimension()
            .getDimensionParamObjectType() == DimensionParamObjectType.STATIC_DIMENSION &&
            orderBy.getOrderBy().getProgramStage().getElement() == null )
        {
            // static enrollment
            return List.of(
                Pair.of(
                    () -> "(" + getEnrollmentSelectWithStaticDimension( orderBy, queryContext ) + ") \"" + ENR_ALIAS
                        + "\"",
                    getEnrollmentConditionWithStaticDimension() ) );
        }

        // default
        return List.of( Pair.of(
            () -> "(" + getSelect( orderBy, queryContext ) + ") \"" + orderBy.getOrderBy().toString() + "\"",
            getCondition( orderBy ) ) );
    }

    private static Renderable getEnrollmentConditionWithStaticDimension()
    {
        return BinaryConditionRenderer.fieldsEqual(
            TEI_ALIAS,
            TEI_UID,
            ENR_ALIAS,
            TEI_UID );
    }

    private static Renderable getEventConditionWithStaticDimension()
    {
        return BinaryConditionRenderer.fieldsEqual(
            ENR_ALIAS,
            PI_UID,
            EVT_ALIAS,
            PI_UID );
    }

    private static Renderable getCondition( AnalyticsSortingParams sortingParams )
    {
        return BinaryConditionRenderer.fieldsEqual(
            TEI_ALIAS,
            TEI_UID,
            doubleQuote( sortingParams.getOrderBy().toString() ),
            TEI_UID );
    }

    private static String getEventSelectWithStaticDimension( AnalyticsSortingParams sortingParams,
        QueryContext queryContext )
    {
        String staticDimension = sortingParams.getOrderBy().getDimension().getUid();

        String programUid = sortingParams.getOrderBy().getProgram().getElement().getUid();

        String programStageUid = sortingParams.getOrderBy().getProgramStage().getElement().getUid();

        return new StringBuilder( "select innermost_evt.programinstanceuid, innermost_evt." + staticDimension )
            .append( " from (select programinstanceuid," )
            .append( " " + staticDimension + "," )
            .append( " row_number() over (partition by programinstanceuid order by incidentdate desc) as rn" )
            .append( " from " + ANALYTICS_TEI_EVT + queryContext.getTetTableSuffix() )
            .append( " where programuid = '" + programUid + "'" )
            .append( " and programstageuid = '" + programStageUid + "') innermost_evt" )
            .append( " where innermost_evt.rn = 1" )
            .toString();
    }

    private static String getEnrollmentSelectWithStaticDimension( AnalyticsSortingParams sortingParams,
        QueryContext queryContext )
    {
        String staticDimension = sortingParams.getOrderBy().getDimension().getUid();

        String programUid = sortingParams.getOrderBy().getProgram().getElement().getUid();

        return new StringBuilder(
            "select innermost_enr.trackedentityinstanceuid, innermost_enr.programinstanceuid, innermost_enr."
                + staticDimension )
                    .append( " from (select trackedentityinstanceuid, " )
                    .append( " programinstanceuid, " )
                    .append( " " + staticDimension + "," )
                    .append(
                        " row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn " )
                    .append( " from " + ANALYTICS_TEI_ENR + queryContext.getTetTableSuffix() )
                    .append( " where programuid = '" + programUid + "') innermost_enr" )
                    .append( " where innermost_enr.rn = 1" )
                    .toString();
    }

    private static String getSelect( AnalyticsSortingParams sortingParams, QueryContext queryContext )
    {
        ValueTypeMapping vtMapping = fromValueType( sortingParams.getOrderBy().getDimension().getValueType() );

        String programUid = sortingParams.getOrderBy().getProgram().getElement().getUid();

        String programStageUid = sortingParams.getOrderBy().getProgramStage().getElement() == null ? null
            : sortingParams.getOrderBy().getProgramStage().getElement().getUid();

        String dataValueUid = sortingParams.getOrderBy().getDimension().getUid();

        // TODO: do it declarately

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
            .append( programStageUid == null ? ""
                : " and programstageuid = " + queryContext.bindParamAndGetIndex( programStageUid ) )
            .append( " order by executiondate desc" )
            .append( " limit 1 offset 0) evt" )
            .append( " where enr.programinstanceuid = evt.programinstanceuid" ).toString();
    }
}
