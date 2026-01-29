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
package org.hisp.dhis.merge.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.MergeReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CategoryMergeServiceTest {

  @Test
  @DisplayName(
      "When each Source Category's options don't match the target Category's options, an error is reported")
  void optionsDontMatchTest() {
    // given 2 source categories exist each with 1 different option
    Category c1 = new Category();
    c1.setUid(CodeGenerator.generateUid());
    CategoryOption co1 = new CategoryOption();
    co1.setUid(CodeGenerator.generateUid());
    c1.addCategoryOption(co1);

    Category c2 = new Category();
    c2.setUid(CodeGenerator.generateUid());
    CategoryOption co2 = new CategoryOption();
    co2.setUid(CodeGenerator.generateUid());
    c2.addCategoryOption(co2);

    // and 1 target category exist, with the same combined options as source 1 & 2
    Category c3 = new Category();
    c3.setUid(CodeGenerator.generateUid());
    c3.addCategoryOption(co1);
    c3.addCategoryOption(co2);

    MergeReport mergeReport = new MergeReport();

    // when Categories are checked that each source has the same options as the target
    CategoryMergeService.validateCategoryOptionsMatch(List.of(c1, c2), c3, mergeReport);

    // then an error is expected
    assertEquals(2, mergeReport.getMergeErrors().size());
  }

  @Test
  @DisplayName(
      "When each Source Category's options match the target's Category options, no error is reported")
  void optionsMatchTest() {
    // given 2 source categories exist each with 2 same options
    Category c1 = new Category();
    c1.setUid(CodeGenerator.generateUid());

    CategoryOption co1 = new CategoryOption();
    co1.setUid(CodeGenerator.generateUid());
    CategoryOption co2 = new CategoryOption();
    co2.setUid(CodeGenerator.generateUid());
    c1.addCategoryOption(co1);
    c1.addCategoryOption(co2);

    Category c2 = new Category();
    c2.setUid(CodeGenerator.generateUid());
    c2.addCategoryOption(co1);
    c2.addCategoryOption(co2);

    // and 1 target category exists, with the same options as source 1 & 2
    Category c3 = new Category();
    c3.setUid(CodeGenerator.generateUid());
    c3.addCategoryOption(co1);
    c3.addCategoryOption(co2);

    MergeReport mergeReport = new MergeReport();

    // when the categories are checked that each source has the same options as the target
    CategoryMergeService.validateCategoryOptionsMatch(List.of(c1, c2), c3, mergeReport);

    // then no error is expected
    assertEquals(0, mergeReport.getMergeErrors().size());
  }

  @Test
  @DisplayName(
      "When a Source Category's combo matches the target's Category combo, an error is reported")
  void combosMatchTest() {
    // given 2 source categories exist each with 2 different combos
    Category c1 = new Category();
    c1.setUid(CodeGenerator.generateUid());
    CategoryCombo cc1 = new CategoryCombo();
    cc1.setUid(CodeGenerator.generateUid());
    cc1.addCategory(c1);

    Category c2 = new Category();
    c2.setUid(CodeGenerator.generateUid());
    CategoryCombo cc2 = new CategoryCombo();
    cc2.setUid(CodeGenerator.generateUid());
    cc2.addCategory(c2);

    // and 1 target category exists, with the same combo as one source
    Category c3 = new Category();
    c3.setUid(CodeGenerator.generateUid());
    cc1.addCategory(c3);

    MergeReport mergeReport = new MergeReport();

    // when the categories are checked to see if each source has different combos than the target
    Set<UID> source1Ccs = getCcUids(c1);
    Set<UID> source2Ccs = getCcUids(c2);
    Set<UID> targetCcs = getCcUids(c3);

    Set<UID> allSourceCcUids = new HashSet<>(source1Ccs);
    allSourceCcUids.addAll(source2Ccs);
    CategoryMergeService.validateCategoryCombosAreDifferent(
        allSourceCcUids, targetCcs, mergeReport);

    // then 1 error is expected
    assertEquals(1, mergeReport.getMergeErrors().size());
    assertTrue(
        mergeReport
            .getMergeErrors()
            .get(0)
            .getMessage()
            .contains(
                "Source and target Categories cannot share a CategoryCombo. Shared CategoryCombos found"));
  }

  @Test
  @DisplayName(
      "When a Source Category's combo doesn't match the target's Category combo, no error is reported")
  void combosDontMatchTest() {
    // given 2 source categories exist each with 2 different combos
    Category c1 = new Category();
    c1.setUid(CodeGenerator.generateUid());
    CategoryCombo cc1 = new CategoryCombo();
    cc1.setUid(CodeGenerator.generateUid());
    cc1.addCategory(c1);

    Category c2 = new Category();
    c2.setUid(CodeGenerator.generateUid());
    CategoryCombo cc2 = new CategoryCombo();
    cc2.setUid(CodeGenerator.generateUid());
    cc2.addCategory(c2);

    // and 1 target category exists, with the same combo as one source
    Category c3 = new Category();
    c3.setUid(CodeGenerator.generateUid());
    CategoryCombo cc3 = new CategoryCombo();
    cc3.setUid(CodeGenerator.generateUid());
    cc3.addCategory(c3);

    MergeReport mergeReport = new MergeReport();

    // when the categories are checked to see if each source has different combos than the target
    Set<UID> source1Ccs = getCcUids(c1);
    Set<UID> source2Ccs = getCcUids(c2);
    Set<UID> targetCcs = getCcUids(c3);

    Set<UID> allSourceCcUids = new HashSet<>(source1Ccs);
    allSourceCcUids.addAll(source2Ccs);
    CategoryMergeService.validateCategoryCombosAreDifferent(
        allSourceCcUids, targetCcs, mergeReport);

    // then 1 error is expected
    assertEquals(0, mergeReport.getMergeErrors().size());
  }

  private Set<UID> getCcUids(Category c) {
    return c.getCategoryCombos().stream()
        .map(IdentifiableObject::getUidType)
        .collect(Collectors.toSet());
  }
}
