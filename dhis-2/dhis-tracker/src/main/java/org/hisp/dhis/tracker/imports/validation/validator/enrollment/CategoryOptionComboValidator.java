/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1115;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;

class CategoryOptionComboValidator implements Validator<Enrollment> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Enrollment enrollment) {
    Program program = bundle.getPreheat().getProgram(enrollment.getProgram());

    boolean isValid = validateAttributeOptionComboExists(reporter, bundle.getPreheat(), enrollment);
    isValid =
        validateDefaultProgramCategoryCombo(reporter, bundle.getPreheat(), enrollment, program)
            && isValid;
    if (!isValid) {
      // no need to do the next validations concerning relationships between the AOC id and the
      // program CC
      // since either AOC does not exist or the AOC violates programs default CC rules
      return;
    }

    CategoryOptionCombo aoc = resolveAttributeOptionCombo(bundle.getPreheat(), enrollment, program);
    if (!validateAttributeOptionComboFound(reporter, enrollment, program, aoc)) return;

    validateAttributeOptionComboIsInProgramCategoryCombo(
        reporter, bundle.getPreheat(), enrollment, program);
  }

  private boolean validateAttributeOptionComboExists(
      Reporter reporter, TrackerPreheat preheat, Enrollment enrollment) {
    if (hasNoAttributeOptionComboSet(enrollment)) {
      return true;
    }

    CategoryOptionCombo categoryOptionCombo =
        preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo());
    if (categoryOptionCombo == null) {
      reporter.addError(enrollment, E1115, enrollment.getAttributeOptionCombo());
      return false;
    }
    return true;
  }

  private boolean hasNoAttributeOptionComboSet(Enrollment enrollment) {
    return enrollment.getAttributeOptionCombo().isBlank();
  }

  /**
   * Validates that the enrollment program has the default category combo if no AOC are given or if
   * a default AOC is given.
   *
   * @param reporter validation error reporter
   * @param preheat tracker preheat
   * @param enrollment enrollment to validate
   * @param program program from the preheat
   * @return return true if program is default with valid aoc and co combinations
   */
  private boolean validateDefaultProgramCategoryCombo(
      Reporter reporter, TrackerPreheat preheat, Enrollment enrollment, Program program) {
    if (hasNoAttributeOptionComboSet(enrollment)
        && !program.getEnrollmentCategoryCombo().isDefault()) {
      reporter.addError(enrollment, ValidationCode.E1055);
      return false;
    }
    CategoryOptionCombo aoc = preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo());
    if (hasAttributeOptionComboSet(enrollment)
        && aoc != null
        && aoc.getCategoryCombo().isDefault()
        && !program.getEnrollmentCategoryCombo().isDefault()) {
      reporter.addError(enrollment, ValidationCode.E1055);
      return false;
    }

    return true;
  }

  private boolean hasAttributeOptionComboSet(Enrollment enrollment) {
    return !hasNoAttributeOptionComboSet(enrollment);
  }

  private boolean validateAttributeOptionComboIsInProgramCategoryCombo(
      Reporter reporter, TrackerPreheat preheat, Enrollment enrollment, Program program) {
    if (hasNoAttributeOptionComboSet(enrollment)) {
      return true;
    }

    CategoryOptionCombo aoc = preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo());
    if (!program.getEnrollmentCategoryCombo().equals(aoc.getCategoryCombo())) {
      reporter.addError(
          enrollment,
          ValidationCode.E1130,
          enrollment.getAttributeOptionCombo(),
          program.getEnrollmentCategoryCombo());
      return false;
    }

    return true;
  }

  private CategoryOptionCombo resolveAttributeOptionCombo(
      TrackerPreheat preheat, Enrollment enrollment, Program program) {

    CategoryOptionCombo aoc;
    if (hasNoAttributeOptionComboSet(enrollment)
        && program.getEnrollmentCategoryCombo().isDefault()) {
      aoc = preheat.getDefault(CategoryOptionCombo.class);
    } else {
      // Note: there is a potential case when there are multiple AOCs in
      // the default CC
      // this should not happen, but it's technically possible. In this
      // case with enrollment.AOC provided,
      // stick to the given AOC in the payload instead of
      // preheat.getDefault( CategoryOptionCombo.class )
      aoc = preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo());
    }
    return aoc;
  }

  private boolean validateAttributeOptionComboFound(
      Reporter reporter, Enrollment enrollment, Program program, CategoryOptionCombo aoc) {
    if (aoc != null) {
      return true;
    }

    addAOCAndCOCombinationError(enrollment, reporter, program);
    return false;
  }

  private void addAOCAndCOCombinationError(
      Enrollment enrollment, Reporter reporter, Program program) {
    // we used the program CC in finding the AOC id, if the AOC id was not provided in the payload
    if (hasNoAttributeOptionComboSet(enrollment)) {
      reporter.addError(enrollment, ValidationCode.E1129, program.getEnrollmentCategoryCombo());
    } else {
      reporter.addError(enrollment, ValidationCode.E1129, enrollment.getAttributeOptionCombo());
    }
  }
}
