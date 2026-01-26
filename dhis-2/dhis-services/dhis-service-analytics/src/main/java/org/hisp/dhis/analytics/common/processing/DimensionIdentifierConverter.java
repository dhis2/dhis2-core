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

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.fromFullDimensionId;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.params.dimension.StringDimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * Component responsible for converting string representations of dimensions into {@link
 * DimensionIdentifier} objects.
 */
@Component
public class DimensionIdentifierConverter {
  /**
   * Based on the given list of {@link Program} and the "fullDimensionId", this method will apply
   * some conversions in order to build a {@link DimensionIdentifier}.
   *
   * @param allowedPrograms list of programs allowed to be present in "fullDimensionId"
   * @param fullDimensionId string representation of a dimension. Examples: abcde[1].fghi[4].jklm,
   *     abcde.fghi.jklm, abcde.jklm or jklm
   * @return the built {@link DimensionIdentifier}
   * @throws IllegalArgumentException if the programUid in the "fullDimensionId" does not belong the
   *     list of "programsAllowed"
   */
  public DimensionIdentifier<StringUid> fromString(
      List<Program> allowedPrograms, String fullDimensionId) {
    StringDimensionIdentifier dimensionIdentifier = fromFullDimensionId(fullDimensionId);

    Optional<Program> programOptional =
        Optional.of(dimensionIdentifier)
            .map(StringDimensionIdentifier::getProgram)
            .map(ElementWithOffset::getElement)
            .flatMap(
                programUid ->
                    allowedPrograms.stream()
                        .filter(program -> program.getUid().equals(programUid.getUid()))
                        .findFirst());

    ElementWithOffset<StringUid> programWithOffset = dimensionIdentifier.getProgram();
    ElementWithOffset<StringUid> programStageWithOffset = dimensionIdentifier.getProgramStage();
    StringUid dimensionId = dimensionIdentifier.getDimension();

    if (!dimensionIdentifier.getProgramStage().isPresent()) {
      if (!dimensionIdentifier.getProgram().isPresent()) { // Contains only a dimension.
        return DimensionIdentifier.of(
            emptyElementWithOffset(), emptyElementWithOffset(), dimensionId);
      }

      // For event-level static dimensions (EVENT_DATE, SCHEDULED_DATE, OU, EVENT_STATUS),
      // the first element could be either a program UID or a program stage UID.
      // If the first element is a valid program UID, treat it as a program-level dimension.
      // If not, treat it as a program stage UID (event-level dimension).
      // Format: programStageUid.EVENT_DATE (event-level) or programUid.OU (program-level)
      if (isEventLevelStaticDimension(dimensionId.getUid()) && programOptional.isEmpty()) {
        return resolveEventLevelDimension(allowedPrograms, programWithOffset, dimensionId);
      }

      // If programOptional is empty but the first element is a stage UID,
      // check if the dimension is a static dimension that is NOT supported for stages.
      // This provides a better error message than "program does not exist"
      if (programOptional.isEmpty()
          && isProgramStageUid(allowedPrograms, programWithOffset.getElement().getUid())
          && StaticDimension.of(dimensionId.getUid()).isPresent()
          && !isEventLevelStaticDimension(dimensionId.getUid())) {
        throw new IllegalArgumentException(
            String.format(
                "Dimension `%s` is not supported for program stage `%s`. "
                    + "Only event-level dimensions (EVENT_DATE, SCHEDULED_DATE, EVENT_STATUS, OU) "
                    + "are supported for stage-specific scoping",
                dimensionId.getUid(), programWithOffset.getElement().getUid()));
      }

      Program program =
          programOptional.orElseThrow(
              () ->
                  new IllegalArgumentException(
                      ("Specified program " + programWithOffset + " does not exist")));

      // Validate that if the program UID is also a stage UID, the dimension is
      // a supported event-level dimension.
      // This prevents using enrollment-level dimensions (like ENROLLMENTDATE)
      // with a stage-specific prefix.
      validateStageSpecificDimension(allowedPrograms, programWithOffset, dimensionId);

      return DimensionIdentifier.of(
          ElementWithOffset.of(program, programWithOffset.getOffset()),
          emptyElementWithOffset(),
          dimensionId);
    }
    if (programOptional.isEmpty()) {
      throw new IllegalArgumentException(
          "Specified program " + programWithOffset + " does not exist");
    }

    Program program = programOptional.get();

    return extractProgramStageIfExists(program, programStageWithOffset.getElement())
        .map(
            programStage ->
                DimensionIdentifier.of(
                    ElementWithOffset.of(program, programWithOffset.getOffset()),
                    ElementWithOffset.of(programStage, programStageWithOffset.getOffset()),
                    dimensionId))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Program stage "
                        + programStageWithOffset
                        + " is not defined in program "
                        + programWithOffset));
  }

  /**
   * Extracts the {@link ProgramStage} object from the given {@link Program}, if any.
   *
   * @param program
   * @param programStageUid
   * @return the {@link ProgramStage} found or empty
   */
  private Optional<ProgramStage> extractProgramStageIfExists(
      Program program, StringUid programStageUid) {
    return program.getProgramStages().stream()
        .filter(programStage -> programStage.getUid().equals(programStageUid.getUid()))
        .findFirst();
  }

  /**
   * Checks if the given dimension UID is an event-level static dimension.
   *
   * @param dimensionUid the dimension UID to check
   * @return true if the dimension is an event-level static dimension
   */
  private boolean isEventLevelStaticDimension(String dimensionUid) {
    return StaticDimension.of(dimensionUid)
        .map(
            sd ->
                sd == StaticDimension.EVENT_DATE
                    || sd == StaticDimension.SCHEDULED_DATE
                    || sd == StaticDimension.EVENT_STATUS
                    || sd == StaticDimension.OU)
        .orElse(false);
  }

  /**
   * Resolves an event-level dimension by searching for the program stage across all allowed
   * programs. The first element (programWithOffset) is actually a program stage UID, not a program
   * UID.
   *
   * @param allowedPrograms the list of allowed programs
   * @param programStageWithOffset the program stage UID with offset (currently in "program"
   *     position)
   * @param dimensionId the dimension identifier
   * @return the built {@link DimensionIdentifier}
   */
  private DimensionIdentifier<StringUid> resolveEventLevelDimension(
      List<Program> allowedPrograms,
      ElementWithOffset<StringUid> programStageWithOffset,
      StringUid dimensionId) {
    String programStageUid = programStageWithOffset.getElement().getUid();

    Optional<ProgramStage> programStageOptional =
        allowedPrograms.stream()
            .flatMap(program -> program.getProgramStages().stream())
            .filter(stage -> stage.getUid().equals(programStageUid))
            .findFirst();

    ProgramStage programStage =
        programStageOptional.orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Specified program stage " + programStageUid + " does not exist"));

    Program program = programStage.getProgram();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, programStageWithOffset.getOffset()),
        ElementWithOffset.of(programStage, programStageWithOffset.getOffset()),
        dimensionId);
  }

  /**
   * Validates that if the given UID is also a program stage UID, the dimension is a supported
   * event-level dimension.
   *
   * <p>This prevents users from using enrollment-level dimensions (like ENROLLMENTDATE) with a
   * stage-specific prefix.
   *
   * @param allowedPrograms the list of allowed programs
   * @param programWithOffset the program UID with offset
   * @param dimensionId the dimension identifier
   * @throws IllegalArgumentException if the dimension is not supported for stage-specific scoping
   */
  private void validateStageSpecificDimension(
      List<Program> allowedPrograms,
      ElementWithOffset<StringUid> programWithOffset,
      StringUid dimensionId) {
    String programUid = programWithOffset.getElement().getUid();

    // Check if this UID is also a program stage UID in any program
    if (!isProgramStageUid(allowedPrograms, programUid)) {
      return;
    }

    // If it's a stage UID, validate that the dimension is a static dimension
    // AND is one of the supported event-level dimensions
    Optional<StaticDimension> staticDimension = StaticDimension.of(dimensionId.getUid());

    if (staticDimension.isPresent() && !isEventLevelStaticDimension(dimensionId.getUid())) {
      throw new IllegalArgumentException(
          String.format(
              "Dimension `%s` is not supported for program stage `%s`. "
                  + "Only event-level dimensions (EVENT_DATE, SCHEDULED_DATE, EVENT_STATUS, OU) "
                  + "are supported for stage-specific scoping",
              dimensionId.getUid(), programUid));
    }
  }

  /**
   * Checks if the given UID is a program stage UID in any of the allowed programs.
   *
   * @param allowedPrograms the list of allowed programs
   * @param uid the UID to check
   * @return true if the UID is a program stage UID, false otherwise
   */
  private boolean isProgramStageUid(List<Program> allowedPrograms, String uid) {
    return allowedPrograms.stream()
        .flatMap(program -> program.getProgramStages().stream())
        .anyMatch(stage -> stage.getUid().equals(uid));
  }
}
