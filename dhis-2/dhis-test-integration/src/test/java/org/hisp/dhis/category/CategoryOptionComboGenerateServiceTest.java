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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class CategoryOptionComboGenerateServiceTest extends PostgresIntegrationTestBase {

  /** Lazy loads of one option combo's options, or of one option's option combos. */
  private static final String PER_ROW_COC_OPTION_LOAD = "from categoryoptioncombos_categoryoptions";

  @Autowired private CategoryOptionComboGenerateService generateService;
  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("Regenerating option combos does not lazy load per option combo")
  void regenerationQueryCountDoesNotScaleWithOptionCombos() {
    long small = countPerRowLoadsWhenAddingAnOption(5);
    long large = countPerRowLoadsWhenAddingAnOption(20);

    // 15 versus 60 persisted option combos. The persisted option combos are preloaded with their
    // options, and only the added option combos are linked back to their options, so the number of
    // per-row loads depends on the options of the added combos, not on how many combos exist. If
    // this grows, an option combo (or option) is being initialised one at a time again.
    assertEquals(
        small,
        large,
        "regenerating option combos must not lazy load per option combo: "
            + small
            + " -> "
            + large);
  }

  @Test
  @DisplayName("Regenerating option combos adds new combos, deletes obsolete ones, and is stable")
  void regenerationAddsDeletesAndIsStable() {
    Fixture fixture = createCategoryComboWithOptionCombos(4, 3);
    String ccUid = fixture.combo.getUid();
    assertEquals(12, optionComboNames(ccUid).size());

    // add an option: 3 option combos are added, named in category order
    CategoryOption added = saveOption("Added");
    Category first = manager.get(Category.class, fixture.first.getUid());
    first.addCategoryOption(added);
    manager.update(first);
    clearSessionAndCache();
    generateService.addAndPruneOptionCombosWithSummary(categoryService.getCategoryCombo(ccUid));
    clearSessionAndCache();

    Set<String> names = optionComboNames(ccUid);
    assertEquals(15, names.size());
    for (CategoryOption second : fixture.secondOptions) {
      String expected = added.getName() + ", " + second.getName();
      assertTrue(names.contains(expected), () -> "missing " + expected + " in " + names);
    }

    // regenerating again changes nothing
    ImportSummaries noop =
        generateService.addAndPruneOptionCombosWithSummary(categoryService.getCategoryCombo(ccUid));
    assertTrue(
        noop.getImportSummaries().isEmpty(),
        () -> "expected no changes, got " + noop.getImportSummaries());
    clearSessionAndCache();
    assertEquals(names, optionComboNames(ccUid));

    // remove the option again: its 3 option combos are obsolete and deleted
    first = manager.get(Category.class, fixture.first.getUid());
    first.removeCategoryOption(manager.get(CategoryOption.class, added.getUid()));
    manager.update(first);
    clearSessionAndCache();
    generateService.addAndPruneOptionCombosWithSummary(categoryService.getCategoryCombo(ccUid));
    clearSessionAndCache();
    assertEquals(12, optionComboNames(ccUid).size());
  }

  private long countPerRowLoadsWhenAddingAnOption(int optionsInFirstCategory) {
    Fixture fixture = createCategoryComboWithOptionCombos(optionsInFirstCategory, 3);
    Category first = manager.get(Category.class, fixture.first.getUid());
    first.addCategoryOption(saveOption("Added" + optionsInFirstCategory));
    manager.update(first);

    clearSessionAndCache();
    QueryCountDataSourceProxy.clearCapturedSql();
    generateService.addAndPruneOptionCombosWithSummary(
        categoryService.getCategoryCombo(fixture.combo.getUid()));
    long loads = QueryCountDataSourceProxy.countCapturedSqlMatching(PER_ROW_COC_OPTION_LOAD);

    clearSessionAndCache();
    assertEquals(
        (optionsInFirstCategory + 1) * 3,
        optionComboNames(fixture.combo.getUid()).size(),
        "the option combos of the added option must have been generated");
    return loads;
  }

  private record Fixture(Category first, List<CategoryOption> secondOptions, CategoryCombo combo) {}

  private Fixture createCategoryComboWithOptionCombos(int firstOptions, int secondOptions) {
    String id = CodeGenerator.generateUid();
    Category first = createCategory("First" + id, saveOptions("F" + id, firstOptions));
    Category second = createCategory("Second" + id, saveOptions("S" + id, secondOptions));
    manager.save(first);
    manager.save(second);
    CategoryCombo combo = createCategoryCombo(id, first, second);
    manager.save(combo);
    generateService.addAndPruneOptionCombos(combo);
    return new Fixture(first, second.getCategoryOptions(), combo);
  }

  private CategoryOption[] saveOptions(String prefix, int count) {
    List<CategoryOption> options = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      options.add(saveOption(prefix + "-" + i));
    }
    return options.toArray(new CategoryOption[0]);
  }

  private CategoryOption saveOption(String name) {
    CategoryOption option = createCategoryOption(name, CodeGenerator.generateUid());
    option.setShortName(name);
    manager.save(option);
    return option;
  }

  private Set<String> optionComboNames(String categoryComboUid) {
    return categoryService.getCategoryCombo(categoryComboUid).getOptionCombos().stream()
        .map(CategoryOptionCombo::getName)
        .collect(Collectors.toSet());
  }

  private void clearSessionAndCache() {
    clearSession();
    // the option combo collections are second level cached; evict them so lazy loads hit the DB
    entityManager.getEntityManagerFactory().getCache().evictAll();
  }
}
