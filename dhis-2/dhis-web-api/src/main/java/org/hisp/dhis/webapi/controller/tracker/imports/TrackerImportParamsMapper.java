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
package org.hisp.dhis.webapi.controller.tracker.imports;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.mapstruct.factory.Mappers;

public class TrackerImportParamsMapper
{
    private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER = Mappers.getMapper( TrackedEntityMapper.class );

    private static final EnrollmentMapper ENROLLMENT_MAPPER = Mappers.getMapper( EnrollmentMapper.class );

    private static final EventMapper EVENT_MAPPER = Mappers.getMapper( EventMapper.class );

    private static final RelationshipMapper RELATIONSHIP_MAPPER = Mappers.getMapper( RelationshipMapper.class );

    private TrackerImportParamsMapper()
    {
    }

    public static TrackerImportParams trackerImportParams( boolean isAsync, String jobId, String userId,
        RequestParams request,
        Body params )
    {
        // If user calls tracker import with empty idScheme /tracker?idScheme=
        // then StringToTrackerIdSchemeParamConverter will
        // return a null TrackerIdSchemeParam.
        // In that case we are setting the defaultIdSchemeParam to UID
        TrackerIdSchemeParam defaultIdSchemeParam = request.getIdScheme() == null ? TrackerIdSchemeParam.UID
            : request.getIdScheme();
        TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder()
            .idScheme( defaultIdSchemeParam )
            .programIdScheme( getIdSchemeParam( request.getProgramIdScheme(), defaultIdSchemeParam ) )
            .categoryOptionIdScheme( getIdSchemeParam( request.getCategoryOptionIdScheme(), defaultIdSchemeParam ) )
            .dataElementIdScheme( getIdSchemeParam( request.getDataElementIdScheme(), defaultIdSchemeParam ) )
            .orgUnitIdScheme( getIdSchemeParam( request.getOrgUnitIdScheme(), defaultIdSchemeParam ) )
            .programStageIdScheme( getIdSchemeParam( request.getProgramStageIdScheme(), defaultIdSchemeParam ) )
            .categoryOptionComboIdScheme(
                getIdSchemeParam( request.getCategoryOptionComboIdScheme(), defaultIdSchemeParam ) )
            .build();

        TrackerImportParams.TrackerImportParamsBuilder paramsBuilder = TrackerImportParams
            .builder()
            .validationMode( request.getValidationMode() )
            .importMode( request.getImportMode() )
            .idSchemes( idSchemeParams )
            .importStrategy( request.getImportStrategy() )
            .atomicMode( request.getAtomicMode() )
            .flushMode( request.getFlushMode() )
            .skipSideEffects( request.isSkipSideEffects() )
            .skipRuleEngine( request.isSkipRuleEngine() )
            .reportMode( request.getReportMode() )
            .userId( userId )
            .trackedEntities( TRACKED_ENTITY_MAPPER.fromCollection( params.getTrackedEntities(), idSchemeParams ) )
            .enrollments( ENROLLMENT_MAPPER.fromCollection( params.getEnrollments(), idSchemeParams ) )
            .events( EVENT_MAPPER.fromCollection( params.getEvents(), idSchemeParams ) )
            .relationships( RELATIONSHIP_MAPPER.fromCollection( params.getRelationships(), idSchemeParams ) );

        if ( !isAsync )
        {
            JobConfiguration jobConfiguration = new JobConfiguration(
                "",
                JobType.TRACKER_IMPORT_JOB,
                userId,
                false );
            jobConfiguration.setUid( jobId );
            paramsBuilder.jobConfiguration( jobConfiguration );
        }

        return paramsBuilder.build();
    }

    private static TrackerIdSchemeParam getIdSchemeParam( TrackerIdSchemeParam idScheme,
        TrackerIdSchemeParam defaultIdSchemeParam )
    {
        return idScheme == null ? defaultIdSchemeParam : idScheme;
    }
}
