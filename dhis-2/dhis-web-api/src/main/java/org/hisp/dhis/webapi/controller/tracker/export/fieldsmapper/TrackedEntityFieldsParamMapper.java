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
package org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper;

import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.FIELD_ATTRIBUTES;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.FIELD_RELATIONSHIPS;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.rootFields;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.dxf2.events.EnrollmentEventsParams;
import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceEnrollmentParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackedEntityFieldsParamMapper
{
    private final FieldFilterService fieldFilterService;

    private static final String FIELD_PROGRAM_OWNERS = "programOwners";

    private static final String FIELD_ENROLLMENTS = "enrollments";

    public TrackedEntityInstanceParams map( List<FieldPath> fields )
    {
        return map( fields, false );
    }

    public TrackedEntityInstanceParams map( List<FieldPath> fields, boolean includeDeleted )
    {
        Map<String, FieldPath> roots = rootFields( fields );

        TrackedEntityInstanceParams params = initUsingAllOrNoFields( roots );
        params = params.withIncludeRelationships(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, FIELD_RELATIONSHIPS ) );
        params = params.withIncludeProgramOwners(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, FIELD_PROGRAM_OWNERS ) );
        params = params.withIncludeAttributes(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, FIELD_ATTRIBUTES ) );

        EventParams eventParams = new EventParams(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, "enrollments.events.relationships" ) );
        EnrollmentEventsParams enrollmentEventsParams = new EnrollmentEventsParams(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, "enrollments.events" ),
            eventParams );
        EnrollmentParams enrollmentParams = new EnrollmentParams(
            enrollmentEventsParams,
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, "enrollments.relationships" ),
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, "enrollments.attributes" ),
            includeDeleted,
            false );
        TrackedEntityInstanceEnrollmentParams teiEnrollmentParams = new TrackedEntityInstanceEnrollmentParams(
            fieldFilterService.filterIncludes( TrackedEntity.class, fields, FIELD_ENROLLMENTS ),
            enrollmentParams );
        params = params.withTeiEnrollmentParams( teiEnrollmentParams );
        return params;
    }

    private static TrackedEntityInstanceParams initUsingAllOrNoFields( Map<String, FieldPath> roots )
    {
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;

        if ( roots.containsKey( FieldPreset.ALL ) )
        {
            FieldPath p = roots.get( FieldPreset.ALL );
            if ( p.isRoot() && !p.isExclude() )
            {
                params = TrackedEntityInstanceParams.TRUE;
            }
        }
        return params;
    }
}
