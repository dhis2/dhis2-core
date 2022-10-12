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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.shared.ValueTypeMapping.STRING;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PS_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.P_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.common.QueryOperator.IN;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.shared.query.*;
import org.hisp.dhis.analytics.tei.query.context.QueryContext;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * Provides methods responsible for generating SQL statements on top of
 * organization units, for events, enrollments and teis.
 */
@RequiredArgsConstructor( staticName = "of" )
public class OrganisationUnitCondition extends BaseRenderable
{
    private static final String OU_FIELD = "ou";

    private final DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    @Override
    public String render()
    {
        boolean filterForEvent = dimensionIdentifier.hasProgram() && dimensionIdentifier.hasProgramStage();
        boolean filterForEnrollment = dimensionIdentifier.hasProgram() && !dimensionIdentifier.hasProgramStage();
        boolean filterForTei = !dimensionIdentifier.hasProgram() && !dimensionIdentifier.hasProgramStage();

        if ( filterForEvent )
        {
            return conditionsForEvent();
        }

        if ( filterForEnrollment )
        {
            return conditionsForEnrollment();
        }

        if ( filterForTei )
        {
            return conditionsForTei();
        }

        return EMPTY;
    }

    /**
     * Renders the org. unit SQL conditions for a given enrollment. The SQL
     * output will look like:
     *
     * "ou" = :1
     *
     * @return the SQL statement
     */
    private String conditionsForTei()
    {
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.ofQuotedField( OU_FIELD ),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }
        return AndCondition.of( orgUnitConditions ).render();
    }

    /**
     * Renders the org. unit SQL conditions for a given enrollment. The SQL
     * output will look like:
     *
     * exists (select 1 from analytics_tei_enrollments_t2d3uj69rab enr where
     * enr.trackedentityinstanceuid = t_1.trackedentityinstanceuid and
     * enr.programuid = :1 and enr.ou in (:2) order by enr.enrollmentdate desc
     * limit 1 offset 0)
     *
     * @return the SQL statement
     */
    private String conditionsForEnrollment()
    {
        String programUid = dimensionIdentifier.getProgram().getElement().getUid();
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.of( ENR_ALIAS, () -> OU_FIELD, null ),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }

        return ExistsCondition.of( Query.builder()
            .select( Select.of( "1" ) )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_ENR + queryContext.getTetTableSuffix(), ENR_ALIAS ) )
            .where( Where.ofConditions(
                BinaryConditionRenderer.fieldsEqual( ENR_ALIAS, TEI_UID, TEI_ALIAS, TEI_UID ),
                BinaryConditionRenderer.of(
                    Field.of( ENR_ALIAS, () -> P_UID, null ),
                    QueryOperator.EQ,
                    () -> queryContext.bindParamAndGetIndex( programUid ) ),
                AndCondition.of( orgUnitConditions ) ) )
            .order( Order.ofOrder( Field.of( ENR_ALIAS, () -> "enrollmentdate", null ).render() + " desc" ) )
            .limit( LimitOffset.ofStrings( "1", "0" ) )
            .build() ).render();
    }

    /**
     * Renders the org. unit SQL conditions for a given event. The SQL output
     * will look like:
     *
     * exists (select 1 from analytics_tei_enrollments_t2d3uj69rab enr where
     * enr.trackedentityinstanceuid = t_1.trackedentityinstanceuid and
     * enr.programuid = :1 and exists (select 1 from
     * analytics_tei_events_t2d3uj69rab evt where evt.programinstanceuid =
     * enr.programinstanceuid and evt.programstageuid = :2 and enr.ou in (:3)
     * order by executiondate desc limit 1 offset 0) order by enr.enrollmentdate
     * desc limit 1 offset 0)
     *
     * @return the SQL statement
     */
    private String conditionsForEvent()
    {
        String programUid = dimensionIdentifier.getProgram().getElement().getUid();
        String programStageUid = dimensionIdentifier.getProgramStage().getElement().getUid();
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.of( ENR_ALIAS, () -> OU_FIELD, null ).render(),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }

        ExistsCondition eventInnerCondition = ExistsCondition.of( Query.builder()
            .select( Select.of( "1" ) )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_EVT + queryContext.getTetTableSuffix(), EVT_ALIAS ) )
            .where( Where.ofConditions(
                BinaryConditionRenderer.fieldsEqual( EVT_ALIAS, PI_UID, ENR_ALIAS, PI_UID ),
                BinaryConditionRenderer.of(
                    Field.of( EVT_ALIAS, () -> PS_UID, null ),
                    QueryOperator.EQ,
                    () -> queryContext.bindParamAndGetIndex( programStageUid ) ),
                AndCondition.of( orgUnitConditions ) ) )
            .order( Order.ofOrder( "executiondate desc" ) )
            .limit( LimitOffset.ofStrings( "1", "0" ) )
            .build() );

        return ExistsCondition.of( Query.builder()
            .select( Select.of( "1" ) )
            .from( From.ofSingleTableAndAlias( ANALYTICS_TEI_ENR + queryContext.getTetTableSuffix(), ENR_ALIAS ) )
            .where( Where.ofConditions(
                BinaryConditionRenderer.fieldsEqual( ENR_ALIAS, TEI_UID, TEI_ALIAS, TEI_UID ),
                BinaryConditionRenderer.of(
                    Field.of( ENR_ALIAS, () -> P_UID, null ),
                    QueryOperator.EQ,
                    () -> queryContext.bindParamAndGetIndex( programUid ) ),
                eventInnerCondition ) )
            .order( Order.ofOrder( Field.of( ENR_ALIAS, () -> "enrollmentdate", null ).render() + " desc" ) )
            .limit( LimitOffset.ofStrings( "1", "0" ) )
            .build() ).render();
    }
}
