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
package org.hisp.dhis.program.variable;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.V_ZERO_POS_VALUE_COUNT;

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.program.ProgramExpressionItem;

import com.google.common.collect.ImmutableMap;

/**
 * Program indicator variable expression item
 *
 * @author Jim Grace
 */
public class ProgramVariableItem
    extends ProgramExpressionItem
{
    private final static ImmutableMap<Integer, ProgramVariable> PROGRAM_VARIABLES = ImmutableMap
        .<Integer, ProgramVariable> builder()
        .put( V_ANALYTICS_PERIOD_END, new vAnalyticsPeriodEnd() )
        .put( V_ANALYTICS_PERIOD_START, new vAnalyticsPeriodStart() )
        .put( V_CREATION_DATE, new vCreationDate() )
        .put( V_CURRENT_DATE, new vCurrentDate() )
        .put( V_COMPLETED_DATE, new vCompletedDate() )
        .put( V_DUE_DATE, new vDueDate() )
        .put( V_ENROLLMENT_COUNT, new vEnrollmentCount() )
        .put( V_ENROLLMENT_DATE, new vEnrollmentDate() )
        .put( V_ENROLLMENT_STATUS, new vEnrollmentStatus() )
        .put( V_EVENT_STATUS, new vEventStatus() )
        .put( V_EVENT_COUNT, new vEventCount() )
        .put( V_EXECUTION_DATE, new vEventDate() ) // Same as event date
        .put( V_EVENT_DATE, new vEventDate() )
        .put( V_INCIDENT_DATE, new vIncidentDate() )
        .put( V_ORG_UNIT_COUNT, new vOrgUnitCount() )
        .put( V_PROGRAM_STAGE_ID, new vProgramStageId() )
        .put( V_PROGRAM_STAGE_NAME, new vProgramStageName() )
        .put( V_SYNC_DATE, new vSyncDate() )
        .put( V_TEI_COUNT, new vTeiCount() )
        .put( V_VALUE_COUNT, new vValueCount() )
        .put( V_ZERO_POS_VALUE_COUNT, new vZeroPosValueCount() )
        .build();

    @Override
    public Object getDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        I18n i18n = visitor.getI18nSupplier().get();

        String variableName = i18n.getString( ctx.programVariable().getText() );

        visitor.getItemDescriptions().put( ctx.getText(), variableName );

        ProgramVariable programVariable = getProgramVariable( ctx );

        return programVariable.defaultVariableValue();
    }

    @Override
    public Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        ProgramVariable programVariable = getProgramVariable( ctx );

        return programVariable.getSql( visitor );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private ProgramVariable getProgramVariable( ExprContext ctx )
    {
        ProgramVariable programVariable = PROGRAM_VARIABLES.get( ctx.programVariable().var.getType() );

        if ( programVariable == null )
        {
            throw new ParserExceptionWithoutContext(
                "Can't find program variable " + ctx.programVariable().var.getText() );
        }

        return programVariable;
    }
}
