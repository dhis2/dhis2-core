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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * Specific component responsible mapping custom labels for specific cases where
 * the user is able to customize them.
 *
 * @author maikel arabori
 */
public class LabelMapper
{
    private LabelMapper()
    {
    }

    /**
     * Finds for a custom label for event date if one exists.
     *
     * @param programStage
     * @return the custom label, otherwise the default one
     */
    static String getEventDateLabel( final ProgramStage programStage, final String defaultLabel )
    {
        if ( programStage != null && isNotBlank( programStage.getDisplayExecutionDateLabel() ) )
        {
            return programStage.getDisplayExecutionDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Finds for a custom label for enrollment date if one exists.
     *
     * @param programStage
     * @return the custom label, otherwise the default one
     */
    static String getEnrollmentDateLabel( final ProgramStage programStage, final String defaultLabel )
    {
        if ( programStage != null && programStage.getProgram() != null
            && isNotBlank( programStage.getProgram().getDisplayEnrollmentDateLabel() ) )
        {
            return programStage.getProgram().getDisplayEnrollmentDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Finds for a custom label for incident date if one exists.
     *
     * @param programStage
     * @return the custom label, otherwise the default one
     */
    static String getIncidentDateLabel( final ProgramStage programStage, final String defaultLabel )
    {
        if ( programStage != null && programStage.getProgram() != null
            && isNotBlank( programStage.getProgram().getDisplayIncidentDateLabel() ) )
        {
            return programStage.getProgram().getDisplayIncidentDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Finds for a custom label for enrollment date if one exists.
     *
     * @param program
     * @return the custom label, otherwise the default one
     */
    static String getEnrollmentDateLabel( final Program program, final String defaultLabel )
    {
        if ( program != null && isNotBlank( program.getDisplayEnrollmentDateLabel() ) )
        {
            return program.getDisplayEnrollmentDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Finds for a custom label for incident date if one exists.
     *
     * @param program
     * @return the custom label, otherwise the default one
     */
    static String getIncidentDateLabel( final Program program, final String defaultLabel )
    {
        if ( program != null && program != null && isNotBlank( program.getDisplayIncidentDateLabel() ) )
        {
            return program.getDisplayIncidentDateLabel();
        }

        return defaultLabel;
    }
}
