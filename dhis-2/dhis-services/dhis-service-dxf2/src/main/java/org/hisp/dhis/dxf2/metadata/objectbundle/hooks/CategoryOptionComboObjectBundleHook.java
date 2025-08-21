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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.Category;
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
      CategoryOptionCombo optionCombo, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    // get provided CO set
    Set<UID> providedCocCoSet =
        optionCombo.getCategoryOptions().stream()
            .map(co -> UID.of(co.getUid()))
            .collect(Collectors.toSet());

    // get all provided cocs with cc
    List<CategoryOptionCombo> persistedCocs = bundle.getObjects(CategoryOptionCombo.class, true);
    List<CategoryOptionCombo> newCocs = bundle.getObjects(CategoryOptionCombo.class, false);

    // all cocs provided in import
    List<CategoryOptionCombo> providedCocs =
        CollectionUtils.combinedUnmodifiableView(persistedCocs, newCocs);

    // get only cocs with same imported coc cc
    List<CategoryOptionCombo> allProvidedCocsForCc =
        providedCocs.stream()
            .filter(
                coc ->
                    coc.getCategoryCombo().getUid().equals(optionCombo.getCategoryCombo().getUid()))
            .toList();

    // get all generated cocs from cc
    CategoryCombo categoryCombo =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), optionCombo.getCategoryCombo());
    Set<CategoryOptionCombo> genCocs = categoryCombo.generateOptionCombosSet();

    ExpectedSizeResult expectedSizeResult =
        checkExpectedSize(
            allProvidedCocsForCc.size(), genCocs.size(), categoryCombo, bundle, addReports);

    // check state if expected size is true
    if (expectedSizeResult.isExpectedSize) {
      // expected
      List<List<UID>> expectedCos = expectedSizeResult.expectedCocs;
      Set<Set<UID>> expectedSetOfCos =
          expectedCos.stream().map(HashSet::new).collect(Collectors.toSet());

      // provided as List, keeping duplicates for now, will check later
      List<Set<UID>> providedListSetOfCos = cocsToCoUids(allProvidedCocsForCc);
      Set<HashSet<UID>> providedSetSetOfCos =
          providedListSetOfCos.stream().map(HashSet::new).collect(Collectors.toSet());

      if (!expectedSetOfCos.equals(providedSetSetOfCos)) {
        Set<Set<UID>> expected = new HashSet<>(expectedSetOfCos);
        Set<Set<UID>> unexpected = new HashSet<>(providedSetSetOfCos);
        // get expected
        expected.removeAll(unexpected);
        // get unexpected
        unexpected.removeAll(expectedSetOfCos);

        // add duplicate to unexpected
        Set<Set<UID>> duplicates = getDuplicates(providedListSetOfCos);
        if (!duplicates.isEmpty()) {
          unexpected.addAll(duplicates);
        }

        // only add error if this COC is in the unexpected set
        if (unexpected.contains(providedCocCoSet) || expected.contains(providedCocCoSet)) {
          addReports.accept(
              new ErrorReport(
                  CategoryOptionCombo.class,
                  ErrorCode.E1131,
                  providedCocCoSet,
                  categoryCombo.getUid(),
                  expected));
        }
      }
    }
  }

  private List<Set<UID>> cocsToCoUids(List<CategoryOptionCombo> allProvidedCocsForCc) {
    List<Set<UID>> providedSetOfCos = new ArrayList<>();
    for (CategoryOptionCombo coc : allProvidedCocsForCc) {
      providedSetOfCos.add(
          coc.getCategoryOptions().stream()
              .map(co -> UID.of(co.getUid()))
              .collect(Collectors.toSet()));
    }
    return providedSetOfCos;
  }

  private ExpectedSizeResult checkExpectedSize(
      int numProvidedCocs,
      int numExpectedCocs,
      CategoryCombo categoryCombo,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    CombinationGenerator<UID> generator =
        CombinationGenerator.newInstance(getCosAsLists(categoryCombo, bundle));
    List<List<UID>> bundleCombinations = generator.getCombinations();

    if (numExpectedCocs == 0) {
      // might be impossible to gen from new cc (has c but no co), get cos from bundle?
      // get cc from bundle
      if (bundleCombinations.size() != numProvidedCocs) {
        addSizeMismatchErrorReport(addReports, numProvidedCocs, bundleCombinations.size());
        return new ExpectedSizeResult(false, bundleCombinations);
      }
      // check if all provided match generated
    } else if (numExpectedCocs != numProvidedCocs) {
      addSizeMismatchErrorReport(addReports, numProvidedCocs, numExpectedCocs);
      return new ExpectedSizeResult(false, bundleCombinations);
    }
    return new ExpectedSizeResult(true, bundleCombinations);
  }

  private void addSizeMismatchErrorReport(
      Consumer<ErrorReport> addReports, int providedSize, int expectedSize) {
    addReports.accept(
        new ErrorReport(CategoryOptionCombo.class, ErrorCode.E1130, providedSize, expectedSize));
  }

  private List<List<UID>> getCosAsLists(CategoryCombo categoryCombo, ObjectBundle bundle) {
    // get categories from CC or bundle if empty
    List<Category> categories =
        bundle.getPreheat().getAll(bundle.getPreheatIdentifier(), categoryCombo.getCategories());
    List<List<UID>> categoryOptionLists = new ArrayList<>();

    if (categories.isEmpty()) {
      categories = categoryCombo.getCategories();
    }
    // get options from bundle

    for (Category category : categories) {
      // get options matching category uid
      categoryOptionLists.add(
          category.getCategoryOptions().stream().map(co -> UID.of(co.getUid())).toList());
    }
    // get list of cos from categories
    return categoryOptionLists;
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

  private Set<Set<UID>> getDuplicates(List<Set<UID>> uidSet) {
    Set<Set<UID>> uniques = new HashSet<>();
    Set<Set<UID>> duplicates = new HashSet<>();

    for (Set<UID> uids : uidSet) {
      if (!uniques.add(uids)) {
        duplicates.add(uids);
      }
    }
    return duplicates;
  }

  record ExpectedSizeResult(boolean isExpectedSize, List<List<UID>> expectedCocs) {}
}
