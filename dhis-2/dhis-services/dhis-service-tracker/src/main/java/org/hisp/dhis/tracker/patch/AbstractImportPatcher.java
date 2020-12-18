package org.hisp.dhis.tracker.patch;
/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;

import java.util.Optional;

public abstract class AbstractImportPatcher<T extends TrackerDto, V extends BaseIdentifiableObject>
{
    /**
     * Template method that can be used by classes extending this class to execute
     * the validation patch flow and enhance the payload entity with DB values
     *
     * @param params the payload to validate and edit
     * @return a {@link org.hisp.dhis.tracker.report.TrackerValidationReport}
     **/
    public TrackerValidationReport patch( TrackerImportParams params )
    {
        T payloadEntity = getPayloadEntity( params );
        Optional<V> entity = getEntityFromDB( payloadEntity, params );
        if ( !entity.isPresent() )
        {
            return createValidationError();
        }

        // Create a payload entity with all the values from the DB and the patched values from the payload
        T convertedEntity = convertForPatch( entity.get(), payloadEntity );
        updatePayload( params, convertedEntity );
        return new TrackerValidationReport();
    }

    private TrackerValidationReport createValidationError()
    {
        // TODO: create correct error
        TrackerErrorReport tre = new TrackerErrorReport( "", TrackerErrorCode.E1000, TrackerType.ENROLLMENT,
            "tre" );

        TrackerValidationReport trackerValidationReport = new TrackerValidationReport();
        trackerValidationReport.add( tre );
        return trackerValidationReport;
    }

    // // // // // // // //
    // // // // // // // //
    // TEMPLATE METHODS //
    // // // // // // // //
    // // // // // // // //

    /**
     * Extract entity from payload
     */
    protected abstract T getPayloadEntity( TrackerImportParams params );

    /**
     * Try to get the entity from the database
     */
    protected abstract Optional<V> getEntityFromDB( T payloadEntity, TrackerImportParams params );

    /**
     * Create a payload entity with all the values from the DB and the patched values from the payload
     */
    protected abstract T convertForPatch( V entity, T payloadEntity );

    /**
     * Update the payload with the enhanced entity with values from database
     */
    protected abstract void updatePayload( TrackerImportParams params, T payloadEntity );
}
