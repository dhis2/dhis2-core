/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.event.HeaderName.ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.EVENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.INCIDENT_DATE;

import org.hisp.dhis.analytics.event.HeaderName;
import org.hisp.dhis.program.ProgramStage;

/**
 * Specific component responsible for overriding Grid headers based on internal
 * business rules.
 *
 * IMPORTANT: The Grid object is used and shared across many analytics features
 * and endpoints. The client/frontend also relies on the Grid objects for
 * several applications. If this component requires any changes be sure to check
 * what will be the impact.
 *
 * @author maikel arabori
 */
public class GridHeaderMapper
{
    private GridHeaderMapper()
    {
    }

    /**
     * This method returns the custom label for the given HeaderName, where the
     * custom label comes from the respective ProgramStage defined by the user
     * (if any). If no custom label is defined by the user for the given
     * HeaderName, this method will return the HeaderName value itself (default
     * value).
     *
     * @param programStage the ProgramStage used in the current query
     *        params/Grid
     * @param headerName the HeaderName where a custom label will be looked for
     *
     * @return the custom header name (if one is set), or empty (if none is set)
     */
    static String getHeaderName( final ProgramStage programStage, final HeaderName headerName )
    {
        if ( headerName != null )
        {
            switch ( headerName )
            {
            case EVENT_DATE:
                if ( programStage != null && isNotBlank( programStage.getExecutionDateLabel() ) )
                {
                    return programStage.getExecutionDateLabel();
                }
                return EVENT_DATE.value();

            case ENROLLMENT_DATE:
                if ( programStage != null && programStage.getProgram() != null
                    && isNotBlank( programStage.getProgram().getEnrollmentDateLabel() ) )
                {
                    return programStage.getProgram().getEnrollmentDateLabel();
                }
                return ENROLLMENT_DATE.value();

            case INCIDENT_DATE:
                if ( programStage != null && programStage.getProgram() != null
                    && isNotBlank( programStage.getProgram().getIncidentDateLabel() ) )
                {
                    return programStage.getProgram().getIncidentDateLabel();
                }
                return INCIDENT_DATE.value();

            default:
                // Do nothing
            }
        }

        return EMPTY;
    }
}
