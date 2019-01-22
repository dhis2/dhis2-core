package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.program.BaseProgramExpressionEvaluationService;
import org.hisp.dhis.program.ProgramD2Function;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Zubair Asghar.
 */
public class ProgramRuleExpressionEvaluationService extends BaseProgramExpressionEvaluationService
{
    private static final Map<String, String> VARIABLE_SAMPLE_VALUE_MAP = ImmutableMap.<String, String> builder()
        .put( VAR_COMPLETED_DATE, "'2017-07-08'" )
        .put( VAR_CURRENT_DATE, "'2017-07-08'" )
        .put( VAR_DUE_DATE, "'2017-07-08'" )
        .put( VAR_ENROLLMENT_COUNT, "1" )
        .put( VAR_ENROLLMENT_DATE, "'2017-07-08'" )
        .put( VAR_ENROLLMENT_STATUS, "'COMPLETED'" )
        .put( VAR_EVENT_COUNT, "1" )
        .put( VAR_EVENT_DATE, "'2017-07-08'" )
        .put( VAR_EXECUTION_DATE, "'2017-07-08'" )
        .put( VAR_INCIDENT_DATE, "'2017-07-08'" )
        .put( VAR_ANALYTICS_PERIOD_START, "'2017-07-01'" )
        .put( VAR_PROGRAM_STAGE_ID, "'WZbXY0S00lP'" )
        .put( VAR_PROGRAM_STAGE_NAME, "'First antenatal care visit'" )
        .put( VAR_TEI_COUNT, "1" )
        .put( VAR_VALUE_COUNT, "1" )
        .put( VAR_ENROLLMENT_ID, "WZbXY0S00sw" )
        .put( VAR_ENVIRONMENT, "SERVER" )
        .put( VAR_EVENT_ID, "WZbXY0S00qq" )
        .put( VAR_PROGRAM_NAME, "Child Program" )
        .put( VAR_ORGUNIT_CODE, "code1" )
        .put( VAR_ORGUNIT, "Sierra Leone" )
        .put( VAR_ANALYTICS_PERIOD_END, "'2017-07-07'" ).build();

    @Override
    protected Map<String, ProgramD2Function> getD2Functions()
    {
        return null;
    }

    @Override
    protected Map<String, SqlFunction> getSQLFunctions()
    {
        return new HashMap<>();
    }

    @Override
    protected Map<String, String> getSourceVariableMap()
    {
        return VARIABLE_SAMPLE_VALUE_MAP;
    }
}
