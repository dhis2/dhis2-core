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

import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.OperationType.AGGREGATE;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.OperationType.QUERY;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DefaultEnrollmentAnalyticsDimensionsService implements EnrollmentAnalyticsDimensionsService
{
    @NonNull
    private final ProgramService programService;

    @Override
    public List<BaseIdentifiableObject> getQueryDimensionsByProgramStageId( String programId )
    {
        return Optional.of( programId )
            .map( programService::getProgram )
            .filter( Program::isRegistration )
            .map( program -> collectDimensions(
                List.of(
                    program.getProgramIndicators(),
                    getProgramStageDataElements( QUERY, program ),
                    filterByValueType( QUERY, getTeasIfRegistrationAndNotConfidential( program ),
                        TrackedEntityAttribute::getValueType ) ) ) )
            .orElse( Collections.emptyList() );
    }

    private Collection<ProgramStageDataElement> getProgramStageDataElements(
        DimensionsServiceCommon.OperationType operationType, Program program )
    {
        return program.getProgramStages().stream()
            .map( ProgramStage::getProgramStageDataElements )
            .map( programStageDataElements -> filterByValueType(
                operationType,
                programStageDataElements,
                a -> a.getDataElement().getValueType() ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );
    }

    @Override
    public List<BaseIdentifiableObject> getAggregateDimensionsByProgramStageId( String programId )
    {
        return Optional.of( programId )
            .map( programService::getProgram )
            .map( program -> collectDimensions(
                List.of(
                    getProgramStageDataElements( AGGREGATE, program ),
                    filterByValueType( AGGREGATE,
                        program.getTrackedEntityAttributes(),
                        TrackedEntityAttribute::getValueType ) ) ) )
            .orElse( Collections.emptyList() );
    }

    private Collection<TrackedEntityAttribute> getTeasIfRegistrationAndNotConfidential( Program program )
    {
        return Optional.of( program )
            .filter( Program::isRegistration )
            .map( Program::getTrackedEntityAttributes )
            .orElse( Collections.emptyList() )
            .stream()
            .filter( this::isNotConfidential )
            .collect( Collectors.toList() );
    }

    private boolean isNotConfidential( TrackedEntityAttribute trackedEntityAttribute )
    {
        return !trackedEntityAttribute.isConfidentialBool();
    }

}
