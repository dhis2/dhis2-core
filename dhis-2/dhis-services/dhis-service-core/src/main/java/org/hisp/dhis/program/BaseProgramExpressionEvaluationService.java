package org.hisp.dhis.program;

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

import org.hisp.dhis.commons.sqlfunc.SqlFunction;

import java.util.Map;

/**
 * @Author Zubair Asghar.
 */
public abstract class BaseProgramExpressionEvaluationService implements ProgramExpressionEvaluationService
{
    protected static final String VAR_EVENT_DATE = "event_date";
    protected static final String VAR_EXECUTION_DATE = "execution_date";
    protected static final String VAR_DUE_DATE = "due_date";
    protected static final String VAR_ENROLLMENT_DATE = "enrollment_date";
    protected static final String VAR_INCIDENT_DATE = "incident_date";
    protected static final String VAR_ENROLLMENT_STATUS = "enrollment_status";
    protected static final String VAR_CURRENT_DATE = "current_date";
    protected static final String VAR_VALUE_COUNT = "value_count";
    protected static final String VAR_ZERO_POS_VALUE_COUNT = "zero_pos_value_count";
    protected static final String VAR_EVENT_COUNT = "event_count";
    protected static final String VAR_ENROLLMENT_COUNT = "enrollment_count";
    protected static final String VAR_TEI_COUNT = "tei_count";
    protected static final String VAR_COMPLETED_DATE = "completed_date";
    protected static final String VAR_PROGRAM_STAGE_NAME = "program_stage_name";
    protected static final String VAR_PROGRAM_STAGE_ID = "program_stage_id";
    protected static final String VAR_ANALYTICS_PERIOD_START = "analytics_period_start";
    protected static final String VAR_ANALYTICS_PERIOD_END = "analytics_period_end";

    @Override
    public boolean expressionIsValid( String expression )
    {
        return false;
    }

    @Override
    public String getExpressionDescription( String expression )
    {
        return null;
    }

    protected abstract Map<String, ProgramD2Function> getD2Functions();

    protected abstract Map<String, SqlFunction> getSQLFunctions();

    protected abstract Map<String, String> getSourceVariableMap();
}
