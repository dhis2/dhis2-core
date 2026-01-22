/*
 * Copyright (c) 2004-2004, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DimensionIdentifierConverter} */
class DimensionIdentifierConverterTest {
  private DimensionIdentifierConverter converter = new DimensionIdentifierConverter();

  @Test
  void fromStringWithSuccessUsingOffset() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    programStage1.setUid("RaMbOrTys0n");
    program1.setProgramStages(Set.of(programStage1));

    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "lxAQ7Zs9VYR[1].RaMbOrTys0n[4].jklm";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        new DimensionIdentifierConverter().fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be lxAQ7Zs9VYR");
    assertEquals(1, dimensionIdentifier.getProgram().getOffset(), "Program offset should be 1");
    assertEquals(
        "RaMbOrTys0n",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be RaMbOrTys0n");
    assertEquals(4, dimensionIdentifier.getProgramStage().getOffset(), "Stage offset should be 4");
  }

  @Test
  void fromStringWithSuccessNoOffset() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    programStage1.setUid("RaMbOrTys0n");
    program1.setProgramStages(Set.of(programStage1));

    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "lxAQ7Zs9VYR.RaMbOrTys0n.jklm";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        new DimensionIdentifierConverter().fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be lxAQ7Zs9VYR");
    assertNull(dimensionIdentifier.getProgram().getOffset(), "Program offset should be null");
    assertEquals(
        "RaMbOrTys0n",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be RaMbOrTys0n");
    assertNull(dimensionIdentifier.getProgramStage().getOffset(), "Stage offset should be null");
  }

  @Test
  void fromStringWithSuccessOnlyProgram() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");

    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "lxAQ7Zs9VYR.jklm";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        new DimensionIdentifierConverter().fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be lxAQ7Zs9VYR");
    assertNull(dimensionIdentifier.getProgram().getOffset(), "Program offset should be null");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgramStage(), "Stage should be null");
  }

  @Test
  void fromStringWithSuccessOnlyDimension() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");

    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "jklm";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        new DimensionIdentifierConverter().fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "jklm", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be jklm");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgram(), "Program should be empty");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgramStage(), "Stage should be null");
  }

  @Test
  void fromStringWithOnlyProgramWhenItDoesNotExist() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("zxAQ7Zs9VYR");

    Program program2 = new Program("prg-2");
    program2.setUid("qr1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "non-existing-program.jklm";

    // When
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> converter.fromString(programs, fullDimensionId));

    // Then
    assertEquals(
        "Specified program non-existing-program does not exist",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void fromStringWhenProgramStageIsNotValidForProgram() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "lxAQ7Zs9VYR[1].invalid-stage[4].jklm";

    // When
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> converter.fromString(programs, fullDimensionId));

    // Then
    assertEquals(
        "Program stage invalid-stage[4] is not defined in program lxAQ7Zs9VYR[1]",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void fromStringWhenProgramDoesNotExistInList() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "non-existing-program[1].fghi[4].jklm";

    // When
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> converter.fromString(programs, fullDimensionId));

    // Then
    assertEquals(
        "Specified program non-existing-program[1] does not exist",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void fromStringWhenOffsetIsPutInTheWrongPlace() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    List<Program> programs = List.of(program1, program2);

    // When
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromString(programs, "lxAQ7Zs9VYR[1].fghi[4].jklm[2]"));
    // Then
    assertEquals(
        "Only program and program stage can have offset",
        thrown.getMessage(),
        "Exception message does not match.");

    // When
    thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromString(programs, "lxAQ7Zs9VYR[1].jklm[2]"));
    // Then
    assertEquals(
        "Only program and program stage can have offset",
        thrown.getMessage(),
        "Exception message does not match.");

    // When
    thrown =
        assertThrows(
            IllegalArgumentException.class, () -> converter.fromString(programs, "jklm[2]"));
    // Then
    assertEquals(
        "Only program and program stage can have offset",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void fromStringWithEventDateDimensionUsingProgramStageUid() {
    // Given - for event-level dimensions like EVENT_DATE, the first element is a program stage UID
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "ZkbAXlQUYJG.eventdate";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then - should resolve program from program stage
    assertEquals(
        "eventdate",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be eventdate");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be resolved from program stage");
    assertEquals(
        "ZkbAXlQUYJG",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be ZkbAXlQUYJG");
  }

  @Test
  void fromStringWithScheduledDateDimensionUsingProgramStageUid() {
    // Given
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "ZkbAXlQUYJG.scheduleddate";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "scheduleddate",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be scheduleddate");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be resolved from program stage");
    assertEquals(
        "ZkbAXlQUYJG",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be ZkbAXlQUYJG");
  }

  @Test
  void fromStringWithEventStatusDimensionUsingProgramStageUid() {
    // Given
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "ZkbAXlQUYJG.eventstatus";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then
    assertEquals(
        "eventstatus",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be eventstatus");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be resolved from program stage");
    assertEquals(
        "ZkbAXlQUYJG",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be ZkbAXlQUYJG");
  }

  @Test
  void fromStringWithOuDimensionUsingProgramStageUid() {
    // Given - OU dimension at event level
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "ZkbAXlQUYJG.ou";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then
    assertEquals("ou", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be ou");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be resolved from program stage");
    assertEquals(
        "ZkbAXlQUYJG",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be ZkbAXlQUYJG");
  }

  @Test
  void fromStringWithEventDateDimensionWhenProgramStageDoesNotExist() {
    // Given - program stage UID that doesn't exist in any program
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "non-existing-stage.eventdate";

    // When
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> converter.fromString(programs, fullDimensionId));

    // Then
    assertEquals(
        "Specified program stage non-existing-stage does not exist",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void fromStringWithEventDateDimensionResolvesFromMultiplePrograms() {
    // Given - program stage exists in one of multiple programs
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    programStage1.setUid("stage1");
    program1.setProgramStages(Set.of(programStage1));

    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");
    ProgramStage programStage2 = new ProgramStage("ps-2", program2);
    programStage2.setUid("ZkbAXlQUYJG");
    program2.setProgramStages(Set.of(programStage2));

    List<Program> programs = List.of(program1, program2);
    String fullDimensionId = "ZkbAXlQUYJG.eventdate";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then - should resolve to program2 which contains the program stage
    assertEquals(
        "eventdate",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be eventdate");
    assertEquals(
        "ur1Edk5Oe2n",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be ur1Edk5Oe2n (program containing the stage)");
    assertEquals(
        "ZkbAXlQUYJG",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be ZkbAXlQUYJG");
  }

  @Test
  void fromStringWithEnrollmentDateDimensionStillUsesProgramUid() {
    // Given - enrollment-level dimensions should still use program UID (not program stage UID)
    Program program = new Program("prg-1");
    program.setUid("lxAQ7Zs9VYR");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "lxAQ7Zs9VYR.enrollmentdate";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then - enrollment-level dimensions still use program UID as first element
    assertEquals(
        "enrollmentdate",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be enrollmentdate");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be lxAQ7Zs9VYR");
    assertEquals(
        emptyElementWithOffset(),
        dimensionIdentifier.getProgramStage(),
        "Stage should be empty for enrollment-level dimensions");
  }

  @Test
  void fromStringWithProgramLevelOuDimensionShouldNotBeTreatedAsEventLevel() {
    // Given - OU dimension with program UID (not program stage UID)
    // This is a program-level (enrollment) OU dimension, not an event-level OU dimension
    Program program = new Program("prg-1");
    program.setUid("IpHINAT79UW");
    ProgramStage programStage = new ProgramStage("ps-1", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));

    List<Program> programs = List.of(program);
    String fullDimensionId = "IpHINAT79UW.ou";

    // When
    DimensionIdentifier<StringUid> dimensionIdentifier =
        converter.fromString(programs, fullDimensionId);

    // Then - should be treated as a program-level OU dimension, not an event-level one
    assertEquals("ou", dimensionIdentifier.getDimension().getUid(), "Dimension uid should be ou");
    assertEquals(
        "IpHINAT79UW",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be IpHINAT79UW");
    assertEquals(
        emptyElementWithOffset(),
        dimensionIdentifier.getProgramStage(),
        "Stage should be empty for program-level OU dimensions");
  }
}
