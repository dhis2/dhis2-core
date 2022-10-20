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

import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifierConverterSupport.fromFullDimensionId;

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
                emptyElementWithOffset(),
                dimensionId );
        }
        else
        {
            return DimensionIdentifier.of( emptyElementWithOffset(), emptyElementWithOffset(), dimensionId );
        }
    }

    private Optional<ProgramStage> extractProgramStageIfExists( Program program, StringUid programStageUid )
    {
        return program.getProgramStages().stream()
            .filter( programStage -> programStage.getUid().equals( programStageUid.getUid() ) )
            .findFirst();
    }
}
