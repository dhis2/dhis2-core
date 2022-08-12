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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.StringUid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DimensionIdentifierConverter}
 */
class DimensionIdentifierConverterTest
{

    @Test
    void fromStringWithSuccessUsingOffset()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        programStage1.setUid( "RaMbOrTys0n" );
        program1.setProgramStages( Set.of( programStage1 ) );

        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "lxAQ7Zs9VYR[1].RaMbOrTys0n[4].jklm";

        // When
        final DimensionIdentifier<Program, ProgramStage, StringUid> dimensionIdentifier = new DimensionIdentifierConverter()
            .fromString( programs, fullDimensionId );

        // Then
        assertEquals( "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm" );
        assertEquals( "lxAQ7Zs9VYR", dimensionIdentifier.getProgram().getElement().getUid(),
            "Program uid should be lxAQ7Zs9VYR" );
        assertEquals( "1", dimensionIdentifier.getProgram().getOffset(),
            "Program offset should be 1" );
        assertEquals( "RaMbOrTys0n", dimensionIdentifier.getProgramStage().getElement().getUid(),
            "Stage uid should be RaMbOrTys0n" );
        assertEquals( "4", dimensionIdentifier.getProgramStage().getOffset(),
            "Stage offset should be 4" );
    }

    @Test
    void fromStringWithSuccessNoOffset()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        programStage1.setUid( "RaMbOrTys0n" );
        program1.setProgramStages( Set.of( programStage1 ) );

        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "lxAQ7Zs9VYR.RaMbOrTys0n.jklm";

        // When
        final DimensionIdentifier<Program, ProgramStage, StringUid> dimensionIdentifier = new DimensionIdentifierConverter()
            .fromString( programs, fullDimensionId );

        // Then
        assertEquals( "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm" );
        assertEquals( "lxAQ7Zs9VYR", dimensionIdentifier.getProgram().getElement().getUid(),
            "Program uid should be lxAQ7Zs9VYR" );
        assertEquals( null, dimensionIdentifier.getProgram().getOffset(),
            "Program offset should be null" );
        assertEquals( "RaMbOrTys0n", dimensionIdentifier.getProgramStage().getElement().getUid(),
            "Stage uid should be RaMbOrTys0n" );
        assertEquals( null, dimensionIdentifier.getProgramStage().getOffset(),
            "Stage offset should be null" );
    }

    @Test
    void fromStringWithSuccessOnlyProgram()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );

        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "lxAQ7Zs9VYR.jklm";

        // When
        final DimensionIdentifier<Program, ProgramStage, StringUid> dimensionIdentifier = new DimensionIdentifierConverter()
            .fromString( programs, fullDimensionId );

        // Then
        assertEquals( "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm" );
        assertEquals( "lxAQ7Zs9VYR", dimensionIdentifier.getProgram().getElement().getUid(),
            "Program uid should be lxAQ7Zs9VYR" );
        assertEquals( null, dimensionIdentifier.getProgram().getOffset(),
            "Program offset should be null" );
        assertEquals( null, dimensionIdentifier.getProgramStage(), "Stage should be null" );
    }

    @Test
    void fromStringWithSuccessOnlyDimension()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );

        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "jklm";

        // When
        final DimensionIdentifier<Program, ProgramStage, StringUid> dimensionIdentifier = new DimensionIdentifierConverter()
            .fromString( programs, fullDimensionId );

        // Then
        assertEquals( "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm" );
        assertEquals( null, dimensionIdentifier.getProgram(), "Program should be null" );
        assertEquals( null, dimensionIdentifier.getProgramStage(), "Stage should be null" );
    }

    @Test
    void fromStringWithOnlyProgramWhenItDoesNotExist()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );

        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "non-existing-program.jklm";

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs, fullDimensionId ) );

        // Then
        assertEquals( "Specified program non-existing-program does not exist", thrown.getMessage(),
            "Exception message does not match." );
    }

    @Test
    void fromStringWhenProgramStageIsNotValidForProgram()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "lxAQ7Zs9VYR[1].invalid-stage[4].jklm";

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs, fullDimensionId ) );

        // Then
        assertEquals( "Program stage invalid-stage[4] is not defined in program lxAQ7Zs9VYR[1]", thrown.getMessage(),
            "Exception message does not match." );
    }

    @Test
    void fromStringWhenProgramDoesNotExistInList()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );
        final String fullDimensionId = "non-existing-program[1].fghi[4].jklm";

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs, fullDimensionId ) );

        // Then
        assertEquals( "Specified program non-existing-program[1] does not exist", thrown.getMessage(),
            "Exception message does not match." );
    }

    @Test
    void fromStringWhenOffsetIsPutInTheWrongPlace()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final List<Program> programs = List.of( program1, program2 );

        // When
        IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs,
                "lxAQ7Zs9VYR[1].fghi[4].jklm[2]" ) );
        // Then
        assertEquals( "Only program and program stage can have offsets", thrown.getMessage(),
            "Exception message does not match." );

        // When
        thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs,
                "lxAQ7Zs9VYR[1].jklm[2]" ) );
        // Then
        assertEquals( "Only program and program stage can have offsets", thrown.getMessage(),
            "Exception message does not match." );

        // When
        thrown = assertThrows( IllegalArgumentException.class,
            () -> new DimensionIdentifierConverter().fromString( programs,
                "jklm[2]" ) );
        // Then
        assertEquals( "Only program and program stage can have offsets", thrown.getMessage(),
            "Exception message does not match." );
    }
}
