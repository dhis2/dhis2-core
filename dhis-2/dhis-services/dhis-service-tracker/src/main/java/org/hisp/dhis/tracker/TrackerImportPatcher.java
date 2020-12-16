package org.hisp.dhis.tracker;

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

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.tracker.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.converter.PatchConverterService;
import org.hisp.dhis.tracker.converter.TrackedEntityTrackerConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Enrico Colasante
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrackerImportPatcher
{
    @NonNull
    private final TrackedEntityInstanceStore teiStore;

    @NonNull
    private final PatchConverterService<TrackedEntity, TrackedEntityInstance> trackedEntityTrackerConverterService;

    @NonNull
    private final ProgramInstanceStore piStore;

    @NonNull
    private final PatchConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;

    @NonNull
    private final ProgramStageInstanceStore psiStore;

    @NonNull
    private final PatchConverterService<Event, ProgramStageInstance> eventTrackerConverterService;

    @Transactional( readOnly = true )
    public String validatePatch( TrackerImportParams params )
    {
        if ( !params.getTrackedEntities().isEmpty() )
        {
            List<TrackedEntityInstance> trackedEntityInstancesByUid = teiStore
                .getTrackedEntityInstancesByUid(
                    Lists.newArrayList( params.getTrackedEntities().get( 0 ).getTrackedEntity() ),
                    params.getUser() );
            if ( trackedEntityInstancesByUid.isEmpty() )
            {
                return "ERROR";
            }

            TrackedEntity convertedEntity = trackedEntityTrackerConverterService
                .toForPatch( trackedEntityInstancesByUid.get( 0 ), params.getTrackedEntities().get( 0 ) );
            params.setTrackedEntities( Lists.newArrayList( convertedEntity ) );
            return "";
        }
        else if ( !params.getEnrollments().isEmpty() )
        {
            List<ProgramInstance> programInstances = piStore
                .getByUid( Lists.newArrayList( params.getEnrollments().get( 0 ).getEnrollment() ),
                    params.getUser() );
            if ( programInstances.isEmpty() )
            {
                return "ERROR";
            }

            Enrollment enrollment = enrollmentTrackerConverterService
                .toForPatch( programInstances.get( 0 ), params.getEnrollments().get( 0 ) );
            params.setEnrollments( Lists.newArrayList( enrollment ) );
            return "";
        }
        else if ( !params.getEvents().isEmpty() )
        {
            ProgramStageInstance programStageInstance = psiStore
                .getByUid( params.getEvents().get( 0 ).getEvent() );
            if ( programStageInstance == null )
            {
                return "ERROR";
            }

            Event event = eventTrackerConverterService
                .toForPatch( programStageInstance, params.getEvents().get( 0 ) );
            params.setEvents( Lists.newArrayList( event ) );
            params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );//To avoid new code to kick in
            return "";
        }
        else
        {
            return "ERROR";
        }
    }
}
