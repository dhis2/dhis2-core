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

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.fromFullDimensionId;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.params.dimension.StringDimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * Component responsible for converting string representations of dimensions
 * into {@link DimensionIdentifier} objects.
 */
@Component
public class DimensionIdentifierConverter
{
    /**
     * Based on the given list of {@link Program} and the "fullDimensionId",
     * this method will apply some conversions in order to build a
     * {@link DimensionIdentifier}.
     *
     * @param allowedPrograms list of programs allowed to be present in
     *        "fullDimensionId"
     * @param fullDimensionId string representation of a dimension. Examples:
     *        abcde[1].fghi[4].jklm, abcde.fghi.jklm, abcde.jklm or jklm
     * @return the built {@link DimensionIdentifier}
     * @throws IllegalArgumentException if the programUid in the
     *         "fullDimensionId" does not belong the list of "programsAllowed"
     */
    public DimensionIdentifier<StringUid> fromString( List<Program> allowedPrograms, String fullDimensionId )
    {
        StringDimensionIdentifier dimensionIdentifier = fromFullDimensionId(
            fullDimensionId );

        Optional<Program> programOptional = Optional.of( dimensionIdentifier )
            .map( StringDimensionIdentifier::getProgram )
            .map( ElementWithOffset::getElement )
            .flatMap( programUid -> allowedPrograms.stream()
                .filter( program -> program.getUid().equals( programUid.getUid() ) )
                .findFirst() );

        ElementWithOffset<StringUid> programWithOffset = dimensionIdentifier.getProgram();
        ElementWithOffset<StringUid> programStageWithOffset = dimensionIdentifier.getProgramStage();
        StringUid dimensionId = dimensionIdentifier.getDimension();

        if ( !dimensionIdentifier.getProgramStage().isPresent() )
        {
            if ( !dimensionIdentifier.getProgram().isPresent() )
            { // Contains only a dimension.
                return DimensionIdentifier.of( emptyElementWithOffset(), emptyElementWithOffset(), dimensionId );
            }

            Program program = programOptional
                .orElseThrow( () -> new IllegalArgumentException(
                    ("Specified program " + programWithOffset + " does not exist") ) );

            return DimensionIdentifier.of(
                ElementWithOffset.of( program, programWithOffset.getOffset() ),
                emptyElementWithOffset(),
                dimensionId );
        }
        if ( programOptional.isEmpty() )
        {
            throw new IllegalArgumentException( "Specified program " + programWithOffset + " does not exist" );
        }

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

    /**
     * Extracts the {@link ProgramStage} object from the given {@link Program},
     * if any.
     *
     * @param program
     * @param programStageUid
     * @return the {@link ProgramStage} found or empty
     */
    private Optional<ProgramStage> extractProgramStageIfExists( Program program, StringUid programStageUid )
    {
        return program.getProgramStages().stream()
            .filter( programStage -> programStage.getUid().equals( programStageUid.getUid() ) )
            .findFirst();
    }
}
