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

/** Converts string representations of dimensions into {@link DimensionIdentifier} objects. */
@Component
public class DimensionIdentifierConverter {

  /**
   * Converts a dimension string to a {@link DimensionIdentifier}.
   *
   * @param allowedPrograms list of programs allowed in the dimension
   * @param fullDimensionId string like "programUid[1].stageUid[4].dimId", "programUid.dimId", or
   *     "dimId"
   * @return the built {@link DimensionIdentifier}
   * @throws IllegalArgumentException if the dimension cannot be resolved against allowed programs
   */
  public DimensionIdentifier<StringUid> fromString(
      List<Program> allowedPrograms, String fullDimensionId) {

    StringDimensionIdentifier parsed = fromFullDimensionId(fullDimensionId);

    // Case 1: dimension only (e.g., "jklm")
    if (!parsed.getProgram().isPresent()) {
      return handleDimensionOnly(parsed);
    }

    // Case 2: fully scoped (e.g., "programUid.stageUid.dimensionId")
    if (parsed.getProgramStage().isPresent()) {
      return handleFullyScoped(allowedPrograms, parsed);
    }

    // Case 3 & 4: two-part format (e.g., "xxx.dimension")
    return handleTwoPartFormat(allowedPrograms, parsed);
  }

  /** Handles dimension-only case where no program or stage is specified. */
  private DimensionIdentifier<StringUid> handleDimensionOnly(StringDimensionIdentifier parsed) {
    return DimensionIdentifier.of(
        emptyElementWithOffset(), emptyElementWithOffset(), parsed.getDimension());
  }

  /** Handles fully scoped case with program, stage, and dimension. */
  private DimensionIdentifier<StringUid> handleFullyScoped(
      List<Program> allowedPrograms, StringDimensionIdentifier parsed) {

    ElementWithOffset<StringUid> programElement = parsed.getProgram();
    ElementWithOffset<StringUid> stageElement = parsed.getProgramStage();

    Program program =
        findProgram(allowedPrograms, programElement.getElement().getUid())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Specified program " + programElement + " does not exist"));

    ProgramStage programStage =
        findProgramStageInProgram(program, stageElement.getElement().getUid())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Program stage "
                            + stageElement
                            + " is not defined in program "
                            + programElement));

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, programElement.getOffset()),
        ElementWithOffset.of(programStage, stageElement.getOffset()),
        parsed.getDimension());
  }

  /** Handles two-part format where first element could be program UID or stage UID. */
  private DimensionIdentifier<StringUid> handleTwoPartFormat(
      List<Program> allowedPrograms, StringDimensionIdentifier parsed) {

    ElementWithOffset<StringUid> firstElement = parsed.getProgram();
    StringUid dimension = parsed.getDimension();
    String firstUid = firstElement.getElement().getUid();

    Optional<Program> programOpt = findProgram(allowedPrograms, firstUid);

    // If first element is a valid program, handle as program-scoped
    if (programOpt.isPresent()) {
      return handleProgramScoped(allowedPrograms, programOpt.get(), firstElement, dimension);
    }

    // Event-level static dimensions always follow stage-scoped handling.
    if (isEventLevelStaticDimension(dimension.getUid())) {
      return handleStageScoped(allowedPrograms, firstElement, dimension);
    }

    // If first element is a stage UID, infer the program from the stage.
    // Non-event-level static dimensions remain unsupported for stage-specific scoping.
    if (isProgramStageUid(allowedPrograms, firstUid)) {
      if (isNonEventLevelStaticDimension(dimension.getUid())) {
        throw unsupportedStageDimensionError(dimension.getUid(), firstUid);
      }
      return handleStageScoped(allowedPrograms, firstElement, dimension);
    }

    // Otherwise, program doesn't exist
    throw new IllegalArgumentException("Specified program " + firstElement + " does not exist");
  }

  /** Handles program-scoped dimension (program + dimension, no stage). */
  private DimensionIdentifier<StringUid> handleProgramScoped(
      List<Program> allowedPrograms,
      Program program,
      ElementWithOffset<StringUid> programElement,
      StringUid dimension) {

    // Validate ambiguous UID case: if program UID is also a stage UID
    validateAmbiguousUid(allowedPrograms, programElement.getElement().getUid(), dimension);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, programElement.getOffset()),
        emptyElementWithOffset(),
        dimension);
  }

  /** Handles stage-scoped dimension (stage + event-level dimension, program inferred). */
  private DimensionIdentifier<StringUid> handleStageScoped(
      List<Program> allowedPrograms,
      ElementWithOffset<StringUid> stageElement,
      StringUid dimension) {

    String stageUid = stageElement.getElement().getUid();

    ProgramStage programStage =
        findProgramStage(allowedPrograms, stageUid)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Specified program stage " + stageUid + " does not exist"));

    Program program = programStage.getProgram();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, stageElement.getOffset()),
        ElementWithOffset.of(programStage, stageElement.getOffset()),
        dimension);
  }

  /** Validates that if a UID exists as both program and stage, certain dimensions are rejected. */
  private void validateAmbiguousUid(
      List<Program> allowedPrograms, String uid, StringUid dimension) {

    if (!isProgramStageUid(allowedPrograms, uid)) {
      return;
    }

    if (isNonEventLevelStaticDimension(dimension.getUid())) {
      throw unsupportedStageDimensionError(dimension.getUid(), uid);
    }
  }

  private boolean isNonEventLevelStaticDimension(String dimensionUid) {
    return StaticDimension.of(dimensionUid).isPresent()
        && !isEventLevelStaticDimension(dimensionUid);
  }

  private IllegalArgumentException unsupportedStageDimensionError(
      String dimensionUid, String stageUid) {
    return new IllegalArgumentException(
        String.format(
            "Dimension `%s` is not supported for program stage `%s`. "
                + "Only event-level dimensions (EVENT_DATE, SCHEDULED_DATE, EVENT_STATUS, OU) "
                + "are supported for stage-specific scoping",
            dimensionUid, stageUid));
  }

  private Optional<Program> findProgram(List<Program> programs, String uid) {
    return programs.stream().filter(p -> p.getUid().equals(uid)).findFirst();
  }

  private Optional<ProgramStage> findProgramStage(List<Program> programs, String stageUid) {
    return programs.stream()
        .flatMap(p -> p.getProgramStages().stream())
        .filter(stage -> stage.getUid().equals(stageUid))
        .findFirst();
  }

  private Optional<ProgramStage> findProgramStageInProgram(Program program, String stageUid) {
    return program.getProgramStages().stream()
        .filter(stage -> stage.getUid().equals(stageUid))
        .findFirst();
  }

  private boolean isProgramStageUid(List<Program> programs, String uid) {
    return findProgramStage(programs, uid).isPresent();
  }

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
}
