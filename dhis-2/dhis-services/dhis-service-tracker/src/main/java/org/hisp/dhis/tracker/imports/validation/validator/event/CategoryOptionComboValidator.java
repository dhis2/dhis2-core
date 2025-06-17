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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1115;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1116;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CategoryOptionComboValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    Program program = bundle.getPreheat().getProgram(event.getProgram());

    boolean isValid = validateAttributeOptionComboExists(reporter, bundle.getPreheat(), event);
    isValid = validateCategoryOptionsExist(reporter, bundle.getPreheat(), event) && isValid;
    isValid =
        validateDefaultProgramCategoryCombo(reporter, bundle.getPreheat(), event, program)
            && isValid;
    if (!isValid) {
      // no need to do the next validations concerning relationships
      // between the AOC id, COs and the program CC
      // since not all AOC, COs exist or the AOC, COs violate programs
      // default CC rules
      return;
    }

    CategoryOptionCombo aoc = resolveAttributeOptionCombo(bundle.getPreheat(), event, program);
    // We should have an AOC by this point. Either the event has no AOC and
    // no COs and its program has the default
    // category combo, or we have an event.AOC. If the payload did not
    // contain an AOC, the preheat and preprocessor should
    // have found/set it.
    if (!validateAttributeOptionComboFound(reporter, event, program, aoc)) return;
    if (!validateAttributeOptionComboIsInProgramCategoryCombo(
        reporter, bundle.getPreheat(), event, program)) return;
    validateAttributeOptionComboMatchesCategoryOptions(
        reporter, bundle.getPreheat(), event, program, aoc);
  }

  private boolean validateAttributeOptionComboExists(
      Reporter reporter, TrackerPreheat preheat, Event event) {
    if (hasNoAttributeOptionComboSet(event)) {
      return true;
    }

    CategoryOptionCombo categoryOptionCombo =
        preheat.getCategoryOptionCombo(event.getAttributeOptionCombo());
    if (categoryOptionCombo == null) {
      reporter.addError(event, E1115, event.getAttributeOptionCombo());
      return false;
    }
    return true;
  }

  private boolean hasNoAttributeOptionComboSet(Event event) {
    return event.getAttributeOptionCombo().isBlank();
  }

  private boolean validateCategoryOptionsExist(
      Reporter reporter, TrackerPreheat preheat, Event event) {
    if (hasNoAttributeCategoryOptionsSet(event)) {
      return true;
    }

    boolean allCOsExist = true;
    for (MetadataIdentifier id : event.getAttributeCategoryOptions()) {
      if (preheat.getCategoryOption(id) == null) {
        reporter.addError(event, E1116, id);
        allCOsExist = false;
      }
    }
    return allCOsExist;
  }

  private boolean hasNoAttributeCategoryOptionsSet(Event event) {
    return event.getAttributeCategoryOptions().isEmpty();
  }

  /**
   * Validates that the event program has the default category combo if no AOC or COs are given or
   * if a default AOC is given.
   *
   * @param reporter validation error reporter
   * @param preheat tracker preheat
   * @param event event to validate
   * @param program event program from the preheat
   * @return return true if event program is default with valid aoc and co combinations
   */
  private boolean validateDefaultProgramCategoryCombo(
      Reporter reporter, TrackerPreheat preheat, Event event, Program program) {
    if (hasNoAttributeOptionComboSet(event)
        && hasNoAttributeCategoryOptionsSet(event)
        && !program.getCategoryCombo().isDefault()) {
      reporter.addError(event, ValidationCode.E1055);
      return false;
    }
    CategoryOptionCombo aoc = preheat.getCategoryOptionCombo(event.getAttributeOptionCombo());
    if (hasAttributeOptionComboSet(event)
        && aoc != null
        && aoc.getCategoryCombo().isDefault()
        && !program.getCategoryCombo().isDefault()) {
      reporter.addError(event, ValidationCode.E1055);
      return false;
    }

    return true;
  }

  private boolean hasAttributeOptionComboSet(Event event) {
    return !hasNoAttributeOptionComboSet(event);
  }

  private boolean validateAttributeOptionComboIsInProgramCategoryCombo(
      Reporter reporter, TrackerPreheat preheat, Event event, Program program) {
    if (hasNoAttributeOptionComboSet(event)) {
      return true;
    }

    CategoryOptionCombo aoc = preheat.getCategoryOptionCombo(event.getAttributeOptionCombo());
    if (!program.getCategoryCombo().equals(aoc.getCategoryCombo())) {
      reporter.addError(
          event, ValidationCode.E1054, event.getAttributeOptionCombo(), program.getCategoryCombo());
      return false;
    }

    return true;
  }

  private CategoryOptionCombo resolveAttributeOptionCombo(
      TrackerPreheat preheat, Event event, Program program) {

    CategoryOptionCombo aoc;
    if (hasNoAttributeOptionComboSet(event) && program.getCategoryCombo().isDefault()) {
      aoc = preheat.getDefault(CategoryOptionCombo.class);
    } else {
      // Note: there is a potential case when there are multiple AOCs in
      // the default CC
      // this should not happen, but it's technically possible. In this
      // case with event.AOC provided,
      // stick to the given AOC in the payload instead of
      // preheat.getDefault( CategoryOptionCombo.class )
      aoc = preheat.getCategoryOptionCombo(event.getAttributeOptionCombo());
    }
    return aoc;
  }

  private Set<CategoryOption> getCategoryOptions(TrackerPreheat preheat, Event event) {

    Set<CategoryOption> categoryOptions = new HashSet<>();
    for (MetadataIdentifier id : event.getAttributeCategoryOptions()) {
      categoryOptions.add(preheat.getCategoryOption(id));
    }
    return categoryOptions;
  }

  private boolean validateAttributeOptionComboFound(
      Reporter reporter, Event event, Program program, CategoryOptionCombo aoc) {
    if (aoc != null) {
      return true;
    }

    addAOCAndCOCombinationError(event, reporter, program);
    return false;
  }

  private void validateAttributeOptionComboMatchesCategoryOptions(
      Reporter reporter,
      TrackerPreheat preheat,
      Event event,
      Program program,
      CategoryOptionCombo aoc) {
    if (hasNoAttributeCategoryOptionsSet(event)) {
      return;
    }

    if (isNotAOCForCOs(preheat, event, aoc)) {
      addAOCAndCOCombinationError(event, reporter, program);
    }
  }

  private void addAOCAndCOCombinationError(Event event, Reporter reporter, Program program) {
    // we used the program CC in finding the AOC id, if the AOC id was not
    // provided in the payload
    if (hasNoAttributeOptionComboSet(event)) {
      reporter.addError(
          event,
          ValidationCode.E1117,
          program.getCategoryCombo(),
          event.getAttributeCategoryOptions().stream()
              .map(MetadataIdentifier::getIdentifierOrAttributeValue)
              .collect(Collectors.toSet())
              .toString());
    } else {
      reporter.addError(
          event,
          ValidationCode.E1117,
          event.getAttributeOptionCombo(),
          event.getAttributeCategoryOptions().stream()
              .map(MetadataIdentifier::getIdentifierOrAttributeValue)
              .collect(Collectors.toSet())
              .toString());
    }
  }

  private boolean isNotAOCForCOs(TrackerPreheat preheat, Event event, CategoryOptionCombo aoc) {
    return !isAOCForCOs(aoc, getCategoryOptions(preheat, event));
  }

  private boolean isAOCForCOs(CategoryOptionCombo aoc, Set<CategoryOption> categoryOptions) {
    return aoc.getCategoryOptions().containsAll(categoryOptions)
        && aoc.getCategoryOptions().size() == categoryOptions.size();
  }
}
