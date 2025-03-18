/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.program;

import static org.apache.commons.collections4.ListUtils.union;
import static org.hisp.dhis.feedback.ErrorCode.E4071;
import static org.hisp.dhis.feedback.ErrorCode.E4072;
import static org.hisp.dhis.feedback.ErrorCode.E4073;
import static org.hisp.dhis.feedback.ErrorCode.E4074;
import static org.hisp.dhis.feedback.ErrorCode.E4079;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ConflictException;
import org.springframework.stereotype.Service;

/**
 * This component resolves the {@link Category} and {@link CategoryOption} references by UID within
 * a {@link Set} of {@link ProgramCategoryMapping}.
 *
 * <p>Methods may throw the {@link ConflictException}. During metadata import this exception can be
 * caught and turned into an import error report. While servicing analytics queries this exception
 * can be caused when metadata has become inconsistent because other metadata has been modified.
 *
 * @author Jim Grace
 */
@Service
@RequiredArgsConstructor
public class ProgramCategoryMappingValidator {

  private final IdentifiableObjectManager idObjectManager;

  /**
   * Validates the UIDs for {@link Category} and {@link CategoryOption} within each {@link
   * ProgramCategoryMapping} of a {@link Program}.
   *
   * @param program the {@link Program} to resolve category mappings for
   */
  public void validateProgramCategoryMappings(Program program) throws ConflictException {
    validateCategoryMappings(program, program.getCategoryMappings());
  }

  /**
   * Gets and validates the category mappings within each {@link ProgramCategoryMapping} referenced
   * by a {@link ProgramIndicator}.
   *
   * @param programIndicator the {@link ProgramIndicator} to resolve category mappings for
   * @return the set of {@link ProgramCategoryMapping} with UIDs resolved
   */
  public Set<ProgramCategoryMapping> getAndValidateProgramIndicatorCategoryMappings(
      ProgramIndicator programIndicator) throws ConflictException {

    Set<ProgramCategoryMapping> mappings = getPiMappings(programIndicator);
    validateCategoryMappings(programIndicator.getProgram(), mappings);
    validateProgramIndicatorCategories(programIndicator, mappings);
    return mappings;
  }

  /**
   * Resolves a set of category mappings from a given program.
   *
   * <p>This may be all the program's mappings if resolving for a program, or only the mappings for
   * a program indicator if resolving for a program indicator.
   */
  private void validateCategoryMappings(
      Program program, Collection<ProgramCategoryMapping> mappings) throws ConflictException {
    Map<String, Category> categoryUidMap = getCategoryUidMap(mappings);
    Map<String, CategoryOption> optionUidMap = getOptionUidMap(mappings);

    for (ProgramCategoryMapping mapping : mappings) {
      validateCategoryMapping(program, mapping, categoryUidMap, optionUidMap);
    }
  }

  /**
   * Gets the (unresolved) category mappings for a program indicator. These are found within the
   * program using the UID of the category mappings.
   */
  private Set<ProgramCategoryMapping> getPiMappings(ProgramIndicator programIndicator)
      throws ConflictException {

    Set<ProgramCategoryMapping> programMappings =
        programIndicator.getProgram().getCategoryMappings();

    Map<String, ProgramCategoryMapping> programMappingMap =
        programMappings.stream()
            .collect(
                Collectors.toUnmodifiableMap(ProgramCategoryMapping::getId, mapping -> mapping));

    Set<ProgramCategoryMapping> programIndicatorMappings = new HashSet<>();
    for (String mappingId : programIndicator.getCategoryMappingIds()) {
      ProgramCategoryMapping mapping = programMappingMap.get(mappingId);
      if (mapping == null) {
        throw new ConflictException(
            E4071, programIndicator.getUid(), mappingId, programIndicator.getProgram().getUid());
      }
      programIndicatorMappings.add(mapping);
    }
    return programIndicatorMappings;
  }

  /**
   * Gets a {@link Map} of all {@link Category} objects referred to by UID in a collection of {@link
   * ProgramCategoryMapping}. To optimize performance, these are fetched in a single call to the
   * database.
   */
  private Map<String, Category> getCategoryUidMap(Collection<ProgramCategoryMapping> mappings) {

    List<String> categoryIds =
        mappings.stream().map(ProgramCategoryMapping::getCategoryId).distinct().toList();

    return idObjectManager.getByUidWithoutTransaction(Category.class, categoryIds).stream()
        .collect(Collectors.toUnmodifiableMap(IdentifiableObject::getUid, o -> o));
  }

  /**
   * Gets a {@link Map} of all {@link CategoryOption} objects referred to by UID in a collection of
   * {@link ProgramCategoryMapping}. To optimize performance, these are fetched in a single call to
   * the database.
   */
  private Map<String, CategoryOption> getOptionUidMap(Collection<ProgramCategoryMapping> mappings) {

    List<String> optionIds =
        mappings.stream()
            .flatMap(mapping -> mapping.getOptionMappings().stream())
            .map(ProgramCategoryOptionMapping::getOptionId)
            .distinct()
            .toList();

    return idObjectManager.getByUidWithoutTransaction(CategoryOption.class, optionIds).stream()
        .collect(Collectors.toUnmodifiableMap(IdentifiableObject::getUid, o -> o));
  }

  /**
   * Validates the {@link Category} and {@link CategoryOption} references within a single {@link
   * ProgramCategoryMapping}.
   */
  private void validateCategoryMapping(
      Program program,
      ProgramCategoryMapping mapping,
      Map<String, Category> categoryUidMap,
      Map<String, CategoryOption> optionUidMap)
      throws ConflictException {

    Category category = categoryUidMap.get(mapping.getCategoryId());
    if (category == null) {
      throw new ConflictException(
          E4072, program.getUid(), mapping.getId(), mapping.getCategoryId());
    }

    Set<String> mappedOptions = new HashSet<>();
    for (ProgramCategoryOptionMapping optionMapping : mapping.getOptionMappings()) {
      validateOptionMapping(program, mapping, optionMapping, optionUidMap);
      if (mappedOptions.contains(optionMapping.getOptionId())) {
        throw new ConflictException(
            E4079, program.getUid(), mapping.getId(), optionMapping.getOptionId());
      }
      mappedOptions.add(optionMapping.getOptionId());
    }
  }

  /**
   * Resolves the {@link CategoryOption} references within a single {@link
   * ProgramCategoryOptionMapping}.
   */
  private void validateOptionMapping(
      Program program,
      ProgramCategoryMapping mapping,
      ProgramCategoryOptionMapping optionMapping,
      Map<String, CategoryOption> optionUidMap)
      throws ConflictException {

    CategoryOption option = optionUidMap.get(optionMapping.getOptionId());
    if (option == null) {
      throw new ConflictException(
          E4073,
          program.getUid(),
          mapping.getCategoryId(),
          mapping.getId(),
          optionMapping.getOptionId());
    }
  }

  /**
   * Validates that every {@link Category} in a program indicator's disaggregation and attribute
   * {@link CategoryCombo}s has a corresponding {@link ProgramCategoryMapping} (unless it's the
   * default cat combo).
   */
  private void validateProgramIndicatorCategories(
      ProgramIndicator pi, Set<ProgramCategoryMapping> mappings) throws ConflictException {

    Set<String> mappedCategoryIds =
        mappings.stream()
            .map(ProgramCategoryMapping::getCategoryId)
            .collect(Collectors.toUnmodifiableSet());

    List<Category> neededCats =
        union(pi.getCategoryCombo().getCategories(), pi.getAttributeCombo().getCategories());

    for (Category cat : neededCats) {
      if (!cat.isDefault() && !mappedCategoryIds.contains(cat.getUid())) {
        throw new ConflictException(
            E4074, pi.getUid(), cat.getDataDimensionType().getValue().toLowerCase(), cat.getUid());
      }
    }
  }
}
