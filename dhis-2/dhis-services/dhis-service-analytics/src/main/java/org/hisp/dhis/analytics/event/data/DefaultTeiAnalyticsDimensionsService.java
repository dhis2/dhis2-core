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

import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.OperationType.QUERY;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.filterByValueType;
import static org.hisp.dhis.common.PrefixedDimensions.ofItemsWithProgram;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.TeiAnalyticsDimensionsService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DefaultTeiAnalyticsDimensionsService implements TeiAnalyticsDimensionsService
{
    @NonNull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @NonNull
    private final EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

    @NonNull
    private final ProgramService programService;

    @Override
    public List<PrefixedDimension> getQueryDimensionsByTrackedEntityTypeId( String trackedEntityTypeId )
    {

        TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType( trackedEntityTypeId );

        if ( Objects.nonNull( trackedEntityType ) )
        {

            // dimensions by programs defined on given TET
            List<PrefixedDimension> dimensions = programService.getAllPrograms().stream()
                .filter( program -> isDefinedOnTrackedEntityType( program, trackedEntityTypeId ) )
                .map( program -> enrollmentAnalyticsDimensionsService
                    .getQueryDimensionsByProgramStageId( program.getUid() ) )
                .flatMap( Collection::stream ).collect( Collectors.toList() );

            List<PrefixedDimension> trackedEntityAttributes = filterByValueType( QUERY,
                /* Tracked Entity Attribute by type */
                ofItemsWithProgram( null, trackedEntityType.getTrackedEntityAttributes() )
                    .stream()
                    .filter( this::isNotConfidential )
                    .collect( Collectors.toList() ) ).stream()
                        .map( prefixedDimension -> prefixedDimension
                            .withDimensionType( "TRACKED_ENTITY_ATTRIBUTE" ) )
                        .collect( Collectors.toList() );

            return Stream.concat( dimensions.stream(), trackedEntityAttributes.stream() )
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

    private boolean isNotConfidential( PrefixedDimension prefixedDimension )
    {
        return !((TrackedEntityAttribute) prefixedDimension.getItem()).isConfidentialBool();
    }

}
