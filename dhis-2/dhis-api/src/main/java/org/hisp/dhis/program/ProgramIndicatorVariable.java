/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.program;

/**
 * @author Luciano Fiandesio
 */
public enum ProgramIndicatorVariable {

    VAR_EVENT_DATE( "event_date", "executiondate", "'2017-07-08'" ),
    VAR_CREATION_DATE( "creation_date", "created", "'2017-07-08'" ),
    VAR_EXECUTION_DATE ( "execution_date", "executiondate", "'2017-07-08'" ),
    VAR_DUE_DATE ( "due_date", "duedate", "'2017-07-08'" ),
    VAR_ENROLLMENT_DATE ( "enrollment_date", "enrollmentdate", "'2017-07-08'" ),
    VAR_INCIDENT_DATE ( "incident_date", "incidentdate", "'2017-07-08'" ),
    VAR_SYNC_DATE ( "sync_date", "lastupdated", "'2017-07-08'" ),
    VAR_ENROLLMENT_STATUS ( "enrollment_status", "enrollmentstatus", "'COMPLETED'" ),
    VAR_CURRENT_DATE ( "current_date", "", "'2017-07-08'" ),
    VAR_VALUE_COUNT ( "value_count" , "", "1"),
    VAR_ZERO_POS_VALUE_COUNT ( "zero_pos_value_count" , "", "1"),
    VAR_EVENT_COUNT ( "event_count", "psi", "1" ),
    VAR_ENROLLMENT_COUNT ( "enrollment_count", "pi", "1"),
    VAR_TEI_COUNT ( "tei_count", "tei", "1" ),
    VAR_COMPLETED_DATE ( "completed_date", "completeddate", "'2017-07-08'" ),
    VAR_PROGRAM_STAGE_NAME ( "program_stage_name", "ps", "'First antenatal care visit'" ),
    VAR_PROGRAM_STAGE_ID ( "program_stage_id", "ps", "'WZbXY0S00lP'" ),
    VAR_ANALYTICS_PERIOD_START ( "analytics_period_start", "", "'2017-07-01'" ),
    VAR_ANALYTICS_PERIOD_END ( "analytics_period_end", "", "'2017-07-07'" ),
    VAR_UNDEFINED("", "", "");

    private String variableName;

    private String column;

    private String defaultValue;

    ProgramIndicatorVariable( String name, String column, String defaultValue )
    {
        this.variableName = name;
        this.column = column;
        this.defaultValue = defaultValue;
    }

    public String getVar()
    {
        return variableName;
    }

    public String getColumn()
    {
        return column;
    }

    public String getDefault() {
        return defaultValue;
    }

    public static String getColumnNameOrNull(String variableName )
    {
        for ( ProgramIndicatorVariable c : values() )
        {
            if ( c.variableName.equals( variableName ) )
            {
                return (c.column.equals( "" ) ? null : c.column);
            }
        }
        return null;
    }

    public static String getDefaultOrNull(String variableName )
    {
        for ( ProgramIndicatorVariable c : values() )
        {
            if ( c.variableName.equals( variableName ) )
            {
                return c.defaultValue;
            }
        }
        return null;
    }

    public static ProgramIndicatorVariable getFromVar(String variableName)
    {
        for ( ProgramIndicatorVariable c : values() )
        {
            if ( c.variableName.equals( variableName ) )
            {
                return c;
            }
        }
        return VAR_UNDEFINED;
    }
}