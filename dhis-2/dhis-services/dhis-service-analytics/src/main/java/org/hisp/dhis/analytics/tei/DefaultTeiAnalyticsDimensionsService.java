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
package org.hisp.dhis.analytics.tei;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

/**
 * Service to retrieve all dimensions that can be used in Cross Program Line
 * Listing given a TrackedEntityType.
 */
@Service
@RequiredArgsConstructor
class DefaultTeiAnalyticsDimensionsService implements TeiAnalyticsDimensionsService
{
    @Nonnull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @Nonnull
    private final EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

    @Nonnull
    private final ProgramService programService;

    @Override
    public List<PrefixedDimension> getQueryDimensionsByTrackedEntityTypeId( String trackedEntityTypeId,
        Set<String> programUids )
    {
        TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType( trackedEntityTypeId );

        if ( Objects.nonNull( trackedEntityType ) )
        {
            Stream<Program> programs;
            if ( CollectionUtils.isEmpty( programUids ) )
            {
                programs = programService.getAllPrograms().stream();
            }
            else
            {
                programs = programService.getPrograms( programUids ).stream();
            }

            // dimensions by programs defined on given TET
            return programs
                .filter( program -> isDefinedOnTrackedEntityType( program, trackedEntityTypeId ) )
                .map( program -> enrollmentAnalyticsDimensionsService
                    .getQueryDimensionsByProgramId( program.getUid() ) )
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
        }
        return Collections.emptyList();
    }

    private boolean isDefinedOnTrackedEntityType( Program program, String trackedEntityTypeId )
    {
        return Optional.of( program )
            .map( Program::getTrackedEntityType )
            .map( BaseIdentifiableObject::getUid )
            .filter( uid -> StringUtils.equals( uid, trackedEntityTypeId ) )
            .isPresent();
    }
}
