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
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
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

  /**
   * Validates the expected state of a {@link CategoryOptionCombo}. This can only be done by
   * validating it with the whole expected set of {@link CategoryOptionCombo} for a {@link
   * CategoryCombo}.
   *
   * <p>This check will validate 2 things:
   *
   * <ul>
   *   <li>that the expected number of {@link CategoryOptionCombo}s (the full set) matches what has
   *       been provided
   *   <li>that the expected {@link CategoryOptionCombo}s match what has been provided. By 'match',
   *       this means that the expected generated set of {@link CategoryOptionCombo} should contain
   *       every {@link CategoryOptionCombo} provided (expected {@link CategoryCombo} and {@link
   *       CategoryOption} set).
   * </ul>
   *
   * <p>The validation uses {@link UID} matching.
   *
   * @param optionCombo option combo being validated
   * @param bundle bundle
   * @param addReports reports to add errors to
   */
  private void checkIsExpectedState(
      CategoryOptionCombo optionCombo, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    // get provided CC UID
    UID providedCc = UID.of(optionCombo.getCategoryCombo().getUid());

    // get provided CO set
    Set<UID> providedCoSet =
        optionCombo.getCategoryOptions().stream()
            .map(co -> UID.of(co.getUid()))
            .collect(Collectors.toSet());

    // get all provided COCs in bundle with same provided CC
    List<CategoryOptionCombo> persistedCocs =
        bundle.getObjects(CategoryOptionCombo.class, true).stream()
            .filter(coc -> hasSameCcUid.test(coc, providedCc))
            .toList();

    List<CategoryOptionCombo> newCocs =
        bundle.getObjects(CategoryOptionCombo.class, false).stream()
            .filter(coc -> hasSameCcUid.test(coc, providedCc))
            .toList();

    // all COCs provided in import
    List<CategoryOptionCombo> allProvidedCocsForCc =
        CollectionUtils.combinedUnmodifiableView(persistedCocs, newCocs);

    // get CC
    CategoryCombo bundleCategoryCombo =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), optionCombo.getCategoryCombo());

    // check size
    ExpectedSizeResult expectedSizeResult =
        checkExpectedSize(allProvidedCocsForCc.size(), bundleCategoryCombo, bundle, addReports);

    // perform state checks if expected size matches
    if (expectedSizeResult.isExpectedSize) {
      checkExistingCocs(optionCombo, bundleCategoryCombo, newCocs, addReports);
      checkExpectedState(
          expectedSizeResult, allProvidedCocsForCc, addReports, providedCoSet, bundleCategoryCombo);
    }
  }

  private void checkExistingCocs(
      CategoryOptionCombo optionCombo,
      CategoryCombo bundleCategoryCombo,
      List<CategoryOptionCombo> newCocs,
      Consumer<ErrorReport> addReports) {
    Set<UID> existingCocs =
        bundleCategoryCombo.getOptionCombos().stream()
            .map(coc -> UID.of(coc.getUid()))
            .collect(Collectors.toSet());

    if (!existingCocs.isEmpty() && !newCocs.isEmpty()) {
      addReports.accept(
          new ErrorReport(
              CategoryOptionCombo.class,
              ErrorCode.E1132,
              optionCombo.getUid(),
              existingCocs,
              bundleCategoryCombo.getUid()));
    }
  }

  private void checkExpectedState(
      ExpectedSizeResult expectedSizeResult,
      List<CategoryOptionCombo> allProvidedCocsForCc,
      Consumer<ErrorReport> addReports,
      Set<UID> providedCoSet,
      CategoryCombo bundleCategoryCombo) {
    Set<Set<UID>> expectedSetOfCos =
        expectedSizeResult.expectedCocs.stream().map(HashSet::new).collect(Collectors.toSet());

    // provided COs as List, keeping duplicates for now, duplicates checked below
    List<Set<UID>> providedListSetOfCos = cocsToCoUids(allProvidedCocsForCc);
    Set<HashSet<UID>> providedSetSetOfCos =
        providedListSetOfCos.stream().map(HashSet::new).collect(Collectors.toSet());

    if (!expectedSetOfCos.equals(providedSetSetOfCos)) {
      // find outlying set of COs
      Set<Set<UID>> expectedCos = new HashSet<>(expectedSetOfCos);
      Set<Set<UID>> unexpectedCos = new HashSet<>(providedSetSetOfCos);

      // get unexpected
      expectedCos.removeAll(unexpectedCos);
      unexpectedCos.removeAll(expectedSetOfCos);

      // add duplicate to unexpected
      Set<Set<UID>> duplicates = CollectionUtils.findDuplicates(providedListSetOfCos);
      if (!duplicates.isEmpty()) {
        unexpectedCos.addAll(duplicates);
      }

      // only add error if the provided CO set is in the unexpected set
      if (unexpectedCos.contains(providedCoSet)) {
        addReports.accept(
            new ErrorReport(
                CategoryOptionCombo.class,
                ErrorCode.E1131,
                providedCoSet,
                bundleCategoryCombo.getUid(),
                expectedCos));
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

  private final BiPredicate<CategoryOptionCombo, UID> hasSameCcUid =
      (coc, cc) -> coc.getCategoryCombo().getUid().equals(cc.getValue());

  private ExpectedSizeResult checkExpectedSize(
      int numProvidedCocs,
      CategoryCombo categoryCombo,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    CombinationGenerator<UID> generator =
        CombinationGenerator.newInstance(getCosAsUidLists(categoryCombo, bundle));
    List<List<UID>> bundleCombinations = generator.getCombinations();

    if (bundleCombinations.size() != numProvidedCocs) {
      addReports.accept(
          new ErrorReport(
              CategoryOptionCombo.class,
              ErrorCode.E1130,
              numProvidedCocs,
              bundleCombinations.size(),
              categoryCombo.getUid()));
      return new ExpectedSizeResult(false, bundleCombinations);
    }
    return new ExpectedSizeResult(true, bundleCombinations);
  }

  /**
   * Gets a List of List of {@link CategoryOption}s from the category model. Depending what objects
   * are contained in the import/preheat, the {@link Category}s are either obtained from:
   *
   * <ul>
   *   <li>the bundle, or if empty
   *   <li>the {@link CategoryCombo} directly.
   * </ul>
   *
   * <p>Once the {@link Category}s are retrieved, then the {@link CategoryOption}s are retrieved
   * from them.
   *
   * @param categoryCombo combo
   * @param bundle bundle
   * @return retrieved List of List<{@link Category}/>
   */
  private List<List<UID>> getCosAsUidLists(CategoryCombo categoryCombo, ObjectBundle bundle) {
    List<Category> categories =
        bundle.getPreheat().getAll(bundle.getPreheatIdentifier(), categoryCombo.getCategories());

    if (categories.isEmpty()) {
      categories = categoryCombo.getCategories();
    }

    List<List<UID>> categoryOptionLists = new ArrayList<>();

    for (Category category : categories) {
      categoryOptionLists.add(
          category.getCategoryOptions().stream().map(co -> UID.of(co.getUid())).toList());
    }
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

  record ExpectedSizeResult(boolean isExpectedSize, List<List<UID>> expectedCocs) {}
}
