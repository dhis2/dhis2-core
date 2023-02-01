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
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PS_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.P_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;

@RequiredArgsConstructor
public abstract class AbstractCondition extends BaseRenderable
{
    private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    @Override
    public String render()
    {
        if ( dimensionIdentifier.isEnrollmentDimension() )
        {
            return enrollmentConditionInternal();
        }
        if ( dimensionIdentifier.isEventDimension() )
        {
            return conditionForEvent();
        }
        return conditionForTei();
    }

    private String conditionForTei()
    {
        return getTeiCondition().render();
    }

    protected abstract Renderable getTeiCondition();

    private String enrollmentConditionInternal()
    {
        String programUid = dimensionIdentifier.getProgram().getElement().getUid();
        return "exists (select 1 from " + getEnrollmentSubQuery( programUid ) + " where "
            + getEnrollmentCondition().render() + ")";
    }

    protected abstract Renderable getEnrollmentCondition();

    private String getEnrollmentSubQuery( String programUid )
    {
        return "(select *" +
            " from (select *, row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn"
            +
            " from " + ANALYTICS_TEI_ENR + queryContext.getTetTableSuffix() +
            " where " + P_UID + " = " + queryContext.bindParamAndGetIndex( programUid ) +
            " and " + TEI_ALIAS + ".trackedentityinstanceuid = trackedentityinstanceuid) innermost_enr" +
            " where innermost_enr.rn = 1) " + ENR_ALIAS;
    }

    private String conditionForEvent()
    {
        String programUid = dimensionIdentifier.getProgram().getElement().getUid();
        String programStageUid = dimensionIdentifier.getProgramStage().getElement().getUid();

        return "exists (select 1 from " +
            getEnrollmentSubQuery( programUid ) +
            " left join " +
            getEventSubQuery( programStageUid ) +
            " on " + ENR_ALIAS + ".programinstanceuid = " + EVT_ALIAS + ".programinstanceuid" +
            " where " + getEventCondition().render() + ")";
    }

    protected Renderable getEventCondition()
    {
        // Default implementation, subclasses will override where needed
        throw new IllegalStateException( "Not supported for event dimension" );
    }

    private String getEventSubQuery( String programStageUid )
    {
        return "(select *" +
            " from (select *, row_number() over (partition by programinstanceuid order by executiondate desc) as rn" +
            " from " + ANALYTICS_TEI_EVT + queryContext.getTetTableSuffix() +
            " where " + PS_UID + " = " + queryContext.bindParamAndGetIndex( programStageUid ) +
            " and " + TEI_ALIAS + ".trackedentityinstanceuid = trackedentityinstanceuid) innermost_enr" +
            " where innermost_enr.rn = 1) " + EVT_ALIAS;
    }
}
