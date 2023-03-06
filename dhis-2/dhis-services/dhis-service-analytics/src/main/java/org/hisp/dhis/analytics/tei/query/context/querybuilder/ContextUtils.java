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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.PS_UID;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.P_UID;

import lombok.NoArgsConstructor;

import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * Utility class for the
 * {@link org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder}.
 */
@NoArgsConstructor( access = PRIVATE )
class ContextUtils
{
    static String enrollmentSelect( ElementWithOffset<Program> program,
        TrackedEntityType trackedEntityType, SqlParameterManager sqlParameterManager )
    {
        return "select innermost_enr.*" +
            " from (select *," +
            " row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn " +
            " from " + ANALYTICS_TEI_ENR + trackedEntityType.getUid().toLowerCase() +
            " where " + P_UID + " = " + sqlParameterManager.bindParamAndGetIndex( program.getElement().getUid() )
            + ") innermost_enr" +
            " where innermost_enr.rn = 1";
    }

    static String eventSelect( ElementWithOffset<Program> program,
        ElementWithOffset<ProgramStage> programStage,
        TrackedEntityType trackedEntityType, SqlParameterManager sqlParameterManager )
    {
        return "select innermost_evt.*" +
            " from (select *," +
            " row_number() over (partition by programinstanceuid order by executiondate desc) as rn" +
            " from " + ANALYTICS_TEI_EVT + trackedEntityType.getUid().toLowerCase() +
            " where " + P_UID + " = " + sqlParameterManager.bindParamAndGetIndex( program.getElement().getUid() ) +
            " and " + PS_UID + " = " + sqlParameterManager.bindParamAndGetIndex( programStage.getElement().getUid() )
            + ") innermost_evt" +
            " where innermost_evt.rn = 1";
    }
}
