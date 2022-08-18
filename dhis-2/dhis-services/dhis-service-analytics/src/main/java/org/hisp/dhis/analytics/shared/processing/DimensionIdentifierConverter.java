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
package org.hisp.dhis.analytics.shared.processing;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset;
import org.hisp.dhis.analytics.common.dimension.StringUid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * Component responsible for converting string representations of dimensions
 * into DimensionIdentifier objects.
 */
@Component
public class DimensionIdentifierConverter
{
    private static final Character DIMENSION_SEPARATOR = '.';

    /**
     *
     * @param programs list of programs that are permitted
     * @param fullDimensionId string representation of a dimension: example ->
     *        abcde[1].fghi[4].jklm
     * @return a DimensionIdentifier
     */
    public DimensionIdentifier<Program, ProgramStage, StringUid> fromString( List<Program> programs,
        String fullDimensionId )
    {
        DimensionIdentifier<StringUid, StringUid, StringUid> dimensionIdentifier = fromFullDimensionId(
            fullDimensionId );

        Optional<Program> programOptional = Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getProgram )
            .map( ElementWithOffset::getElement )
            .flatMap( programUid -> programs.stream()
                .filter( program -> program.getUid().equals( programUid.getUid() ) )
                .findFirst() );

        ElementWithOffset<StringUid> programWithOffset = dimensionIdentifier.getProgram();
        ElementWithOffset<StringUid> programStageWithOffset = dimensionIdentifier.getProgramStage();
        StringUid dimensionId = dimensionIdentifier.getDimension();

        if ( dimensionIdentifier.hasProgramStage() )
        { // fully qualified DE/PI. ie.: {programUid}.{programStageUid}.DE
            if ( programOptional.isPresent() )
            {
                Program program = programOptional.get();

                return extractProgramStageIfExists( program, programStageWithOffset.getElement() )
                    .map( programStage -> DimensionIdentifier.of(
                        ElementWithOffset.of( program, programWithOffset.getOffset() ),
                        ElementWithOffset.of( programStage, programStageWithOffset.getOffset() ),
                        dimensionId ) )
                    .orElseThrow( () -> new IllegalArgumentException(
                        "Program stage " + programStageWithOffset + " is not defined in program "
                            + programWithOffset ) );
            }
            else
            {
                throw new IllegalArgumentException( "Specified program " + programWithOffset + " does not exist" );
            }
        }
        else if ( dimensionIdentifier.hasProgram() )
        { // example: {programUid}.PE
            Program program = programOptional
                .orElseThrow( () -> new IllegalArgumentException(
                    ("Specified program " + programWithOffset + " does not exist") ) );

            return DimensionIdentifier.of(
                ElementWithOffset.of( program, programWithOffset.getOffset() ),
                null,
                dimensionId );
        }
        else
        {
            return DimensionIdentifier.of( null, null, dimensionId );
        }
    }

    private DimensionIdentifier<StringUid, StringUid, StringUid> fromFullDimensionId( String fullDimensionId )
    {
        List<ElementWithOffset<StringUid>> uidWithOffsets = parseFullDimensionId( fullDimensionId );

        if ( uidWithOffsets.size() == 3 ) // ie.: abcde[1].fghi[4].jklm
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 2 ) );
            return DimensionIdentifier.of( uidWithOffsets.get( 0 ), uidWithOffsets.get( 1 ),
                uidWithOffsets.get( 2 ).getElement() );
        }

        if ( uidWithOffsets.size() == 2 ) // ie.: abcde[1].hfjg
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 1 ) );
            return DimensionIdentifier.of( uidWithOffsets.get( 0 ), null, uidWithOffsets.get( 1 ).getElement() );

        }

        if ( uidWithOffsets.size() == 1 ) // ie.: fgrg
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 0 ) );
            return DimensionIdentifier.of( null, null, uidWithOffsets.get( 0 ).getElement() );
        }

        throw new IllegalArgumentException( "Malformed parameter: " + fullDimensionId );
    }

    private List<ElementWithOffset<StringUid>> parseFullDimensionId( String fullDimensionId )
    {
        return stream( split( fullDimensionId, DIMENSION_SEPARATOR ) )
            .map( this::elementWithOffsetByString )
            .collect( toList() );
    }

    private void assertDimensionIdHasNoOffset(
        ElementWithOffset<StringUid> dimensionIdWithOffset )
    {
        if ( dimensionIdWithOffset.hasOffset() )
        {
            throw new IllegalArgumentException( "Only program and program stage can have offsets" );
        }
    }

    private ElementWithOffset<StringUid> elementWithOffsetByString( String elementWithOffset )
    {
        String[] split = split( elementWithOffset, "[]" );
        if ( split.length == 2 )
        {
            return ElementWithOffset.of( StringUid.of( split[0] ), split[1] );
        }
        else
        {
            return ElementWithOffset.of( StringUid.of( elementWithOffset ), null );
        }
    }

    private Optional<ProgramStage> extractProgramStageIfExists( Program program, StringUid programStageUid )
    {
        return program.getProgramStages().stream()
            .filter( programStage -> programStage.getUid().equals( programStageUid.getUid() ) )
            .findFirst();
    }
}
