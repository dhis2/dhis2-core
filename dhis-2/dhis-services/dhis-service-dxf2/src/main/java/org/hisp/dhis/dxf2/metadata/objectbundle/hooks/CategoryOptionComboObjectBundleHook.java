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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CategoryOptionComboObjectBundleHook
    extends AbstractObjectBundleHook<CategoryOptionCombo> {
  private final CategoryService categoryService;

  static boolean haveEqualCatComboCatOptionReferenceIds(
      CategoryOptionCombo one, CategoryOptionCombo other) {
    if (one == null || other == null) {
      return false;
    }

    if (one.getCategoryCombo() == null || other.getCategoryCombo() == null) {
      return false;
    }

    if (one.getCategoryOptions() == null || other.getCategoryOptions() == null) {
      return false;
    }

    if (!one.getCategoryCombo().getUid().equals(other.getCategoryCombo().getUid())) {
      return false;
    }

    Set<String> oneCategoryOptionUids =
        one.getCategoryOptions().stream().map(CategoryOption::getUid).collect(Collectors.toSet());

    Set<String> otherCategoryOptionUids =
        other.getCategoryOptions().stream().map(CategoryOption::getUid).collect(Collectors.toSet());

    return oneCategoryOptionUids.equals(otherCategoryOptionUids);
  }

  private void checkDuplicateCategoryOptionCombos(
      CategoryOptionCombo categoryOptionCombo,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {

    if (bundle.isPersisted(categoryOptionCombo)) {
      return; // Only check for duplicates if the object is not persisted
    }

    List<CategoryOptionCombo> categoryOptionCombos =
        categoryService.getAllCategoryOptionCombos().stream()
            .filter(
                coc ->
                    coc.getCategoryCombo()
                        .getUid()
                        .equals(categoryOptionCombo.getCategoryCombo().getUid()))
            .toList();

    // This could be an update or re-import of the same object.
    if (categoryOptionCombos.stream()
        .anyMatch(coc -> coc.getUid().equals(categoryOptionCombo.getUid()))) {
      return;
    }
    // Check to see if the COC already exists in the list of COCs by comparing reference ids
    for (CategoryOptionCombo existingCategoryOptionCombo : categoryOptionCombos) {
      if (haveEqualCatComboCatOptionReferenceIds(categoryOptionCombo, existingCategoryOptionCombo)
          && !categoryOptionCombo.getUid().equals(existingCategoryOptionCombo.getUid())) {
        addReports.accept(
            new ErrorReport(
                CategoryOptionCombo.class,
                ErrorCode.E1122,
                categoryOptionCombo.getName(),
                existingCategoryOptionCombo.getName()));
      }
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
              CategoryOptionCombo.class, ErrorCode.E1124, categoryOptionCombo.getName()));
    }
  }

  @Override
  public void validate(
      CategoryOptionCombo categoryOptionCombo,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {

    checkNonStandardDefaultCatOptionCombo(categoryOptionCombo, addReports);
    checkDuplicateCategoryOptionCombos(categoryOptionCombo, bundle, addReports);
  }
}
