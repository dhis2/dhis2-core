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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.feedback.ErrorCode.E1130;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryOptionComboObjectBundleHook
    extends AbstractObjectBundleHook<CategoryOptionCombo> {

  private final CategoryService categoryService;

  @Override
  public void validate(
      CategoryOptionCombo combo, ObjectBundle bundle, Consumer<ErrorReport> addReports) {

    checkNonStandardDefaultCatOptionCombo(combo, addReports);
    checkIsValid(combo, addReports);
    checkIsExpectedState(combo, bundle, addReports);
  }

  private void checkIsExpectedState(
      CategoryOptionCombo combo, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    // get all provided cocs with cc
    List<CategoryOptionCombo> persistedCocs = bundle.getObjects(CategoryOptionCombo.class, true);
    List<CategoryOptionCombo> newCocs = bundle.getObjects(CategoryOptionCombo.class, false);

    // all cocs provided in import
    List<CategoryOptionCombo> providedCocs =
        CollectionUtils.combinedUnmodifiableView(persistedCocs, newCocs);

    // get only cocs with same imported coc cc
    // todo only use uid?? in case uninitialized
    List<CategoryOptionCombo> allProvidedCocsForCc =
        providedCocs.stream()
            .filter(
                coc -> coc.getCategoryCombo().getUid().equals(combo.getCategoryCombo().getUid()))
            .toList();

    // get all generated cocs from cc
    CategoryCombo categoryCombo =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), combo.getCategoryCombo());
    Set<CategoryOptionCombo> genCocs = categoryCombo.generateOptionCombosSet();

    // todo transform to coc DTO

    if (genCocs.isEmpty()) {
      // might be impossible to gen from new cc (has c but no co), get cos from bundle?
      // get cc from bundle
      CombinationGenerator<UID> generator =
          CombinationGenerator.newInstance(getCosAsLists(categoryCombo, bundle));
    }

    // check if all provided match generated
    if (genCocs.size() != allProvidedCocsForCc.size()) {
      addReports.accept(
          new ErrorReport(
              CategoryOptionCombo.class, E1130, allProvidedCocsForCc.size(), genCocs.size()));
    }
  }

  private List<List<UID>> getCosAsLists(CategoryCombo categoryCombo, ObjectBundle bundle) {
    // get categories from CC
    // get list of cos from categories
    return List.of();
  }

  private void checkIsValid(CategoryOptionCombo combo, Consumer<ErrorReport> addReports) {
    try {
      categoryService.validate(combo);
    } catch (ConflictException ex) {
      addReports.accept(new ErrorReport(CategoryOptionCombo.class, ex.getCode(), ex.getArgs()));
    }
  }

  private void checkNonStandardDefaultCatOptionCombo(
      CategoryOptionCombo categoryOptionCombo, Consumer<ErrorReport> addReports) {

    CategoryCombo categoryCombo = categoryOptionCombo.getCategoryCombo();
    CategoryCombo defaultCombo = categoryService.getDefaultCategoryCombo();
    if (!categoryCombo.getUid().equals(defaultCombo.getUid())) {
      return;
    }

    CategoryOptionCombo defaultCatOptionCombo = categoryService.getDefaultCategoryOptionCombo();

    if (!categoryOptionCombo.getUid().equals(defaultCatOptionCombo.getUid())) {
      addReports.accept(
          new ErrorReport(
              CategoryOptionCombo.class, ErrorCode.E1122, categoryOptionCombo.getName()));
    }
  }

  record CategoryOptionDto(UID uid, Set<UID> categories, Set<UID> categoryOptionCombos) {}

  record CategoryDto(UID uid, Set<UID> categoryCombos, Set<UID> categoryOptions) {}

  record CategoryComboDto(UID uid, Set<UID> categories, Set<UID> categoryOptionCombos) {}

  record CategoryOptionComboDto(UID uid, UID categoryCombo, Set<UID> categoryOptions) {}

  record CategoryModelDto(
      CategoryCombo categoryComboDto,
      Set<CategoryOptionDto> optionDtos,
      Set<CategoryDto> categoryDtos,
      Set<CategoryOptionComboDto> optionComboDtos) {}
}
