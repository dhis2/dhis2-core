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
package org.hisp.dhis.analytics.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.dimension.DimensionSpecification;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DimensionSpecificationService
{
    public static final Character DIMENSION_SEPARATOR = '.';

    public DimensionSpecification fromDimensionId( Collection<Program> programs, String fullDimensionId )
    {
        DimensionSpecificationUids dsUids = DimensionSpecificationUids.of( fullDimensionId );

        Optional<Program> programOptional = Optional.of( dsUids )
            .map( DimensionSpecificationUids::getProgram )
            .map( UidWithOffset::getUid )
            .flatMap( programUid -> programs.stream()
                .filter( program -> program.getUid().equals( programUid ) )
                .findFirst() );

        UidWithOffset programWithOffset = dsUids.getProgram();
        UidWithOffset programStageWithOffset = dsUids.getProgramStage();
        UidWithOffset dimensionIdWithOffset = dsUids.getDimension();

        if ( dimensionIdWithOffset.hasOffset() )
        {
            throw new IllegalArgumentException( "Only program and program stage can have offsets" );
        }

        if ( dsUids.hasProgramStage() )
        { // fully qualified DE/PI

            if ( programOptional.isPresent() )
            {
                Program program = programOptional.get();
                return extractProgramStageIfExists( program, programStageWithOffset.getUid() )
                    .map( programStage -> DimensionSpecification.of(
                        DimensionSpecification.ElementWithOffset.of( program, programWithOffset.getOffset() ),
                        DimensionSpecification.ElementWithOffset.of( programStage, programStageWithOffset.getOffset() ),
                        dimensionIdWithOffset.getUid() ) )
                    .orElseThrow( () -> new IllegalArgumentException(
                        "Program stage " + programStageWithOffset.getUid() + " is not defined in program "
                            + programWithOffset.getUid() ) );
            }
            else
            {
                throw new IllegalArgumentException( "Specified program " + programWithOffset + " does not exists" );
            }
        }
        else if ( dsUids.hasProgram() )
        { // example: {programUid}.PE

            Program program = programOptional
                .orElseThrow( () -> new IllegalArgumentException(
                    ("Specified program " + programWithOffset + " does not exists") ) );

            return DimensionSpecification.of(
                DimensionSpecification.ElementWithOffset.of( program, programWithOffset.getOffset() ),
                null,
                dimensionIdWithOffset.getUid() );

        }
        else
        {
            return DimensionSpecification.of( null, null, dsUids.getDimension().getUid() );
        }
    }

    private Optional<ProgramStage> extractProgramStageIfExists( Program program, String programStageUid )
    {
        return program.getProgramStages().stream()
            .filter( programStage -> programStage.getUid().equals( programStageUid ) )
            .findFirst();
    }

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    private static class DimensionSpecificationUids
    {

        private final UidWithOffset program;

        private final UidWithOffset programStage;

        private final UidWithOffset dimension;

        boolean hasProgram()
        {
            return Objects.nonNull( program );
        }

        boolean hasProgramStage()
        {
            return Objects.nonNull( programStage );
        }

        static DimensionSpecificationUids of( String fullDimensionId )
        {
            List<UidWithOffset> uidWithOffsets = parseFullDimensionId( fullDimensionId );

            if ( uidWithOffsets.size() == 3 )
            {
                return DimensionSpecificationUids.of( uidWithOffsets.get( 0 ), uidWithOffsets.get( 1 ),
                    uidWithOffsets.get( 2 ) );
            }
            if ( uidWithOffsets.size() == 2 )
            {
                return DimensionSpecificationUids.of( uidWithOffsets.get( 0 ), null, uidWithOffsets.get( 1 ) );

            }
            if ( uidWithOffsets.size() == 1 )
            {
                return DimensionSpecificationUids.of( null, null, uidWithOffsets.get( 0 ) );
            }

            throw new IllegalArgumentException( "Malformed parameter: " + fullDimensionId );

        }

        private static List<UidWithOffset> parseFullDimensionId( String fullDimensionId )
        {
            return Arrays.stream( StringUtils.split( fullDimensionId, DIMENSION_SEPARATOR ) )
                .map( UidWithOffset::of )
                .collect( Collectors.toList() );
        }
    }

    @Data
    @RequiredArgsConstructor( access = AccessLevel.PRIVATE )
    private static class UidWithOffset
    {
        private final String uid;

        private final String offset;

        static UidWithOffset of( String uidWithOffset )
        {
            String[] split = StringUtils.split( uidWithOffset, "[]" );
            if ( split.length == 2 )
            {
                return new UidWithOffset( split[0], split[1] );
            }
            else
                return new UidWithOffset( uidWithOffset, null );
        }

        public boolean hasOffset()
        {
            return Objects.nonNull( offset );
        }
    }
}
