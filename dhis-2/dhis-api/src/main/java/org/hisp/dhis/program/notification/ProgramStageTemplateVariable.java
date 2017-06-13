package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.notification.TemplateVariable;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defines the variable expression names for a {@link ProgramNotificationTemplate}
 * on a {@link org.hisp.dhis.program.ProgramStage ProgramStage}.
 *
 * The supported variable names are:
 *
 * <ul>
 *     <li>program_name</li>
 *     <li>program_stage_name</li>
 *     <li>org_unit_name</li>
 *     <li>due_date</li>
 *     <li>days_since_due_date</li>
 *     <li>days_until_due_date</li>
 *     <li>current_date</li>
 * </ul>
 *
 * @author Halvdan Hoem Grelland
 */
public enum ProgramStageTemplateVariable
    implements TemplateVariable
{
    PROGRAM_NAME( "program_name" ),
    PROGRAM_STAGE_NAME( "program_stage_name" ),
    ORG_UNIT_NAME( "org_unit_name" ),
    DUE_DATE( "due_date" ),
    DAYS_SINCE_DUE_DATE( "days_since_due_date" ),
    DAYS_UNTIL_DUE_DATE( "days_until_due_date" ),
    CURRENT_DATE( "current_date" );

    private static final Map<String, ProgramStageTemplateVariable> variableNameMap =
        EnumSet.allOf( ProgramStageTemplateVariable.class ).stream()
            .collect( Collectors.toMap( ProgramStageTemplateVariable::getVariableName, e -> e ) );

    private final String variableName;

    ProgramStageTemplateVariable( String variableName )
    {
        this.variableName = variableName;
    }

    @Override
    public String getVariableName()
    {
        return variableName;
    }

    public static boolean isValidVariableName( String expressionName )
    {
        return variableNameMap.keySet().contains( expressionName );
    }

    public static ProgramStageTemplateVariable fromVariableName( String variableName )
    {
        return variableNameMap.get( variableName );
    }
}
