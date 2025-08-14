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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
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
      CategoryOptionCombo categoryOptionCombo,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {

    checkNonStandardDefaultCatOptionCombo(categoryOptionCombo, addReports);
    checkIsValid(categoryOptionCombo, addReports);

    //    checkCategoryComboUpdate(categoryOptionCombo, bundle, addReports);
    //    checkCategoryOptionsUpdate(categoryOptionCombo, bundle, addReports);
  }

  private void checkCategoryComboUpdate(
      CategoryOptionCombo updatedCoc, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    CategoryOptionCombo existingCoc =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), updatedCoc);
    if (existingCoc == null) return;

    CategoryCombo existingCategoryCombo =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), existingCoc.getCategoryCombo());

    // get updated CC from bundle category option combo
    CategoryCombo updatedCategoryCombo =
        categoryService.getCategoryCombo(updatedCoc.getCategoryCombo().getUid());

    // if no existing CC, try bundle in case new CC in update import
    if (updatedCategoryCombo == null) {
      updatedCategoryCombo =
          bundle.getPreheat().get(bundle.getPreheatIdentifier(), updatedCoc.getCategoryCombo());

      if (updatedCategoryCombo == null) {
        // nothing in bundle
      }
    }

    if (!existingCategoryCombo.equals(updatedCategoryCombo)) {
      addReports.accept(
          new ErrorReport(
              CategoryOptionCombo.class, ErrorCode.E1129, updatedCategoryCombo.getUid()));
    }
  }

  //  private void checkCategoryOptionsUpdate(
  //      CategoryOptionCombo updatedCategoryOptionCombo,
  //      ObjectBundle bundle,
  //      Consumer<ErrorReport> addReports) {
  //    Set<CategoryOption> existingCategoryOptions =
  // existingCategoryOptionCombo.getCategoryOptions();
  //
  //    // get existing COs from bundle category option combo
  //    Set<CategoryOption> updatedCategoryOptions =
  //        new HashSet<>(
  //            categoryService.getCategoryOptionsByUid(
  //                updatedCategoryOptionCombo.getCategoryOptions().stream()
  //                    .map(BaseIdentifiableObject::getUid)
  //                    .toList()));
  //
  //    if (!existingCategoryOptions.equals(updatedCategoryOptions)) {
  //      addReports.accept(
  //          new ErrorReport(
  //              CategoryOptionCombo.class, ErrorCode.E1130, updatedCategoryOptionCombo.getUid()));
  //    }
  //  }

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
}
