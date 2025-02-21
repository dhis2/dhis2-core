/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.program;

import static org.apache.commons.collections4.ListUtils.union;
import static org.hisp.dhis.feedback.ErrorCode.E4071;
import static org.hisp.dhis.feedback.ErrorCode.E4072;
import static org.hisp.dhis.feedback.ErrorCode.E4073;
import static org.hisp.dhis.feedback.ErrorCode.E4074;

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
import org.hisp.dhis.common.InconsistentMetadataException;
import org.springframework.stereotype.Service;

/**
 * This component resolves the {@link Category} and {@link CategoryOption} references by UID within
 * a {@link Set} of {@link ProgramCategoryMapping}.
 *
 * <p>Methods may throw the {@link InconsistentMetadataException}. During metadata import this
 * exception can be caught and turned into an import error report. While servicing analytics queries
 * this exception can be caused when metadata has become inconsistent because other metadata has
 * been modified.
 *
 * @author Jim Grace
 */
@Service
@RequiredArgsConstructor
public class ProgramCategoryMappingResolver {

  private final IdentifiableObjectManager idObjectManager;

  /**
   * Resolves the UIDs for {@link Category} and {@link CategoryOption} within each {@link
   * ProgramCategoryMapping} of a {@link Program}.
   *
   * @param program the {@link Program} to resolve category mappings for
   * @return the set of {@link ProgramCategoryMapping} with UIDs resolved
   */
  public Set<ProgramCategoryMapping> resolveProgramCategoryMappings(Program program) {
    return resolveCategoryMappings(program, program.getCategoryMappings());
  }

  /**
   * Resolves the UIDs for {@link Category} and {@link CategoryOption} within each {@link
   * ProgramCategoryMapping} of a {@link ProgramIndicator}.
   *
   * @param programIndicator the {@link ProgramIndicator} to resolve category mappings for
   * @return the set of {@link ProgramCategoryMapping} with UIDs resolved
   */
  public Set<ProgramCategoryMapping> resolveProgramIndicatorCategoryMappings(
      ProgramIndicator programIndicator) {

    Set<ProgramCategoryMapping> unresolvedMappings = getUnresolvedMappings(programIndicator);
    Set<ProgramCategoryMapping> mappings =
        resolveCategoryMappings(programIndicator.getProgram(), unresolvedMappings);

    validateProgramIndicatorCategories(programIndicator, mappings);

    return mappings;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Resolves a set of category mappings from a given program.
   *
   * <p>This may be all the program's mappings if resolving for a program, or only the mappings for
   * a program indicator if resolving for a program indicator.
   */
  private Set<ProgramCategoryMapping> resolveCategoryMappings(
      Program program, Collection<ProgramCategoryMapping> mappings) {
    Map<String, Category> categoryUidMap = getCategoryUidMap(mappings);
    Map<String, CategoryOption> optionUidMap = getOptionUidMap(mappings);

    return mappings.stream()
        .map(mapping -> resolveCategoryMapping(program, mapping, categoryUidMap, optionUidMap))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Gets the (unresolved) category mappings for a program indicator. These are found within the
   * program using the UID of the category mappings.
   */
  private Set<ProgramCategoryMapping> getUnresolvedMappings(ProgramIndicator programIndicator) {

    Map<String, ProgramCategoryMapping> mappingMap =
        programIndicator.getProgram().getCategoryMappings().stream()
            .collect(Collectors.toUnmodifiableMap(ProgramCategoryMapping::getId, mapping -> mapping));
    Set<ProgramCategoryMapping> mappings = new HashSet<>();

    for (String mappingId : programIndicator.getCategoryMappingIds()) {
      ProgramCategoryMapping mapping = mappingMap.get(mappingId);
      if (mapping == null) {
        throw new InconsistentMetadataException(
            E4071, programIndicator.getUid(), mappingId, programIndicator.getProgram().getUid());
      }
      mappings.add(mapping);
    }
    return mappings;
  }

  /**
   * Gets a {@link Map} of all {@link Category} objects referred to by UID in a collection of {@link
   * ProgramCategoryMapping}. To optimize performance, these are fetched in a single call to the
   * database.
   */
  private Map<String, Category> getCategoryUidMap(Collection<ProgramCategoryMapping> mappings) {

    List<String> categoryIds =
        mappings.stream().map(ProgramCategoryMapping::getCategoryId).distinct().toList();

    return idObjectManager.getByUid(Category.class, categoryIds).stream()
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

    return idObjectManager.getByUid(CategoryOption.class, optionIds).stream()
        .collect(Collectors.toUnmodifiableMap(IdentifiableObject::getUid, o -> o));
  }

  /**
   * Resolves the {@link Category} and {@link CategoryOption} references within a single {@link
   * ProgramCategoryMapping}.
   */
  private ProgramCategoryMapping resolveCategoryMapping(
      Program program,
      ProgramCategoryMapping mapping,
      Map<String, Category> categoryUidMap,
      Map<String, CategoryOption> optionUidMap) {

    Category category = categoryUidMap.get(mapping.getCategoryId());
    if (category == null) {
      throw new InconsistentMetadataException(
          E4072, program.getUid(), mapping.getId(), mapping.getCategoryId());
    }

    Set<ProgramCategoryOptionMapping> optionMappings =
        mapping.getOptionMappings().stream()
            .map(
                optionMapping ->
                    resolveOptionMapping(program, mapping, optionMapping, optionUidMap))
            .collect(Collectors.toUnmodifiableSet());
    return mapping.toBuilder().category(category).optionMappings(optionMappings).build();
  }

  /**
   * Resolves the {@link CategoryOption} references within a single {@link
   * ProgramCategoryOptionMapping}.
   */
  private ProgramCategoryOptionMapping resolveOptionMapping(
      Program program,
      ProgramCategoryMapping mapping,
      ProgramCategoryOptionMapping optionMapping,
      Map<String, CategoryOption> optionUidMap) {

    CategoryOption option = optionUidMap.get(optionMapping.getOptionId());
    if (option == null) {
      throw new InconsistentMetadataException(
          E4073,
          program.getUid(),
          mapping.getCategoryId(),
          mapping.getId(),
          optionMapping.getOptionId());
    }

    return optionMapping.toBuilder().option(option).build();
  }

  /**
   * Validates that every {@link Category} in a program indicator's disaggregation and attribute
   * {@link CategoryCombo}s has a corresponding {@link ProgramCategoryMapping} (unless it's the
   * default cat combo).
   */
  private void validateProgramIndicatorCategories(
      ProgramIndicator pi, Set<ProgramCategoryMapping> mappings) {

    Set<String> mappedCategoryIds =
        mappings.stream()
            .map(ProgramCategoryMapping::getCategoryId)
            .collect(Collectors.toUnmodifiableSet());

    List<Category> neededCats =
        union(pi.getCategoryCombo().getCategories(), pi.getAttributeCombo().getCategories());

    for (Category cat : neededCats) {
      if (!cat.isDefault() && !mappedCategoryIds.contains(cat.getUid())) {
        throw new InconsistentMetadataException(
            E4074, pi.getUid(), cat.getDataDimensionType().getValue().toLowerCase(), cat.getUid());
      }
    }
  }
}
