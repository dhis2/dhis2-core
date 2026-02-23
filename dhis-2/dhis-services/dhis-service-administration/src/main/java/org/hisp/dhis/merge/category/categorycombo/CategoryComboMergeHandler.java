/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.merge.category.categorycombo;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataapproval.DataApprovalWorkflowStore;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.program.ProgramIndicatorStore;
import org.hisp.dhis.program.ProgramStore;
import org.springframework.stereotype.Component;

/**
 * Merge handler for CategoryCombo metadata entities.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryComboMergeHandler {

  private final CategoryService categoryService;
  private final DataElementStore dataElementStore;
  private final DataSetStore dataSetStore;
  private final ProgramStore programStore;
  private final ProgramIndicatorStore programIndicatorStore;
  private final DataApprovalWorkflowStore dataApprovalWorkflowStore;

  /**
   * Handles removing source {@link CategoryCombo} references with target in {@link Category}s.
   * Removes source combos from categories. No adding of target {@link CategoryCombo} refs is
   * carried out as source & target {@link CategoryCombo}s have to have the same categories to start
   * with (early validation).
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleCategories(List<CategoryCombo> sources, CategoryCombo target) {
    for (CategoryCombo source : sources) {
      for (Category category : source.getCategories()) {
        category.removeCategoryCombo(source);
      }
    }
    log.info("{} Categories updated by removing source CategoryCombos", sources.size());
  }

  /**
   * Updates CategoryOptionCombo references to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleCategoryOptionCombos(List<CategoryCombo> sources, CategoryCombo target) {
    int cocsUpdated = 0;
    for (CategoryCombo srcCc : sources) {
      Iterator<CategoryOptionCombo> srcCocIterator = srcCc.getOptionCombos().iterator();
      while (srcCocIterator.hasNext()) {
        CategoryOptionCombo srcCoc = srcCocIterator.next();
        srcCoc.setCategoryCombo(target);
        srcCocIterator.remove();
        target.addCategoryOptionCombo(srcCoc);
        categoryService.updateCategoryOptionCombo(srcCoc);
        ++cocsUpdated;
      }
      categoryService.updateCategoryCombo(srcCc);
    }
    log.info("{} CategoryOptionCombos updated with target CategoryCombo ref", cocsUpdated);
  }

  /**
   * Updates DataElement references to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleDataElements(List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated = dataElementStore.updateCategoryComboRefs(sourceIds, target.getId());
    log.info("{} DataElements updated with target CategoryCombo ref", updated);
  }

  /**
   * Updates DataSet references to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleDataSets(List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated = dataSetStore.updateCategoryComboRefs(sourceIds, target.getId());
    log.info("{} DataSets updated with target CategoryCombo ref", updated);
  }

  /**
   * Updates DataSetElement references to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleDataSetElements(List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated = dataSetStore.updateDataSetElementCategoryComboRefs(sourceIds, target.getId());
    log.info("{} DataSetElements updated with target CategoryCombo ref", updated);
  }

  /**
   * Updates Program.categoryCombo and Program.enrollmentCategoryCombo references to point to the
   * target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handlePrograms(List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated =
        programStore.updateCategoryComboAndEnrollmentCategoryComboRefs(sourceIds, target.getId());
    log.info(
        "{} Programs (categorycombo and/or enrollment categorycombo) updated with target CategoryCombo ref",
        updated);
  }

  /**
   * Updates DataApprovalWorkflow references to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleDataApprovalWorkflows(List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated = dataApprovalWorkflowStore.updateCategoryComboRefs(sourceIds, target.getId());
    log.info("{} DataApprovalWorkflows updated with target CategoryCombo ref", updated);
  }

  /**
   * Updates ProgramIndicator.categoryCombo and ProgramIndicator.attributeCategoryCombo references
   * to point to the target CategoryCombo.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   */
  public void handleProgramIndicatorCategoryCombos(
      List<CategoryCombo> sources, CategoryCombo target) {
    Set<Long> sourceIds =
        sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());
    int updated =
        programIndicatorStore.updateCategoryComboAndAttributeComboRefs(sourceIds, target.getId());
    log.info(
        "{} ProgramIndicator (categoryCombo and/or attributeCategoryCombo) updated with target CategoryCombo ref",
        updated);
  }
}
