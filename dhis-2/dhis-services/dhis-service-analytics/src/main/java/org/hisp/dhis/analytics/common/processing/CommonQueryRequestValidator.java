/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.feedback.ErrorCode.E4014;
import static org.hisp.dhis.feedback.ErrorCode.E7136;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * Component responsible for generic validations on top of a
 * {@link CommonQueryRequest} object.
 */
@Component
public class CommonQueryRequestValidator implements Validator<CommonQueryRequest>
{
    /**
     * Runs a validation on the given query request object
     * {@link CommonQueryRequest}, preventing basic syntax and consistency
     * issues.
     *
     * @param commonQueryRequest the {@link CommonQueryRequest}.
     * @throws IllegalQueryException is some invalid state is found.
     */
    @Override
    public void validate( CommonQueryRequest commonQueryRequest )
    {
        if ( !commonQueryRequest.hasPrograms() )
        {
            throw new IllegalQueryException( new ErrorMessage( E7136 ) );
        }

        for ( String programUid : commonQueryRequest.getProgram() )
        {
            if ( !isValidUid( programUid ) )
            {
                throw new IllegalQueryException( new ErrorMessage( E4014, programUid, "program" ) );
            }
        }

        validateEnrollmentDate( commonQueryRequest.getEnrollmentDate() );
        validateEventDate( commonQueryRequest.getEventDate() );
    }

    /**
     * The event date should have a format like:
     * "IpHINAT79UW.A03MvHHogjR.LAST_YEAR"
     *
     * @param eventDate the date to validate.
     * @throws IllegalQueryException if the format is invalid.
     */
    private void validateEventDate( String eventDate )
    {
        if ( isNotBlank( eventDate ) )
        {
            boolean invalidPeriodValue = countMatches( eventDate, "." ) != 2;

            if ( invalidPeriodValue )
            {
                throw new IllegalQueryException( new ErrorMessage( E4014, eventDate, "eventDate" ) );
            }
        }
    }

    /**
     * The event date should have a format like: "IpHINAT79UW.LAST_YEAR".
     *
     * @param enrollmentDate the date to validate.
     * @throws IllegalQueryException if the format is invalid.
     */
    private void validateEnrollmentDate( String enrollmentDate )
    {
        if ( isNotBlank( enrollmentDate ) )
        {
            boolean invalidPeriodValue = countMatches( enrollmentDate, "." ) != 1;

            if ( invalidPeriodValue )
            {
                throw new IllegalQueryException( new ErrorMessage( E4014, enrollmentDate, "enrollmentDate" ) );
            }
        }
    }
}
