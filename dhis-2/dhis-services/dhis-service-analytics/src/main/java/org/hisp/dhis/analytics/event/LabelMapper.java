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
package org.hisp.dhis.analytics.event;

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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a custom label for the event date if it exists, otherwise the
     * given default label.
     *
     * @param programStage the {@link ProgramStage}.
     * @param defaultLabel the default label.
     * @return the custom label, otherwise the default label.
     */
    public static String getEventDateLabel( ProgramStage programStage, String defaultLabel )
    {
        if ( programStage != null && isNotBlank( programStage.getDisplayExecutionDateLabel() ) )
        {
            return programStage.getDisplayExecutionDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Returns a custom label for the schedule date if it exists, otherwise the
     * given default label.
     *
     * @param programStage the {@link ProgramStage}.
     * @param defaultLabel the default label.
     * @return the custom label, otherwise the default label.
     */
    public static String getScheduleDateLabel( ProgramStage programStage, String defaultLabel )
    {
        if ( programStage != null && isNotBlank( programStage.getDisplayDueDateLabel() ) )
        {
            return programStage.getDisplayDueDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Returns a custom label for enrollment date if one exists, otherwise the
     * given default label.
     *
     * @param program the {@link Program}.
     * @return the custom label, otherwise the default label.
     */
    public static String getEnrollmentDateLabel( Program program, String defaultLabel )
    {
        if ( program != null && isNotBlank( program.getDisplayEnrollmentDateLabel() ) )
        {
            return program.getDisplayEnrollmentDateLabel();
        }

        return defaultLabel;
    }

    /**
     * Returns a custom label for incident date if one exists, otherwise the
     * given default label.
     *
     * @param program the {@link Program}.
     * @return the custom label, otherwise the default label.
     */
    public static String getIncidentDateLabel( Program program, String defaultLabel )
    {
        if ( program != null && isNotBlank( program.getDisplayIncidentDateLabel() ) )
        {
            return program.getDisplayIncidentDateLabel();
        }

        return defaultLabel;
    }
}
