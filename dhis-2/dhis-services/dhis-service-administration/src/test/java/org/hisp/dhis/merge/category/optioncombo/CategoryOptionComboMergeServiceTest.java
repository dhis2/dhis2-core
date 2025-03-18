/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.merge.category.optioncombo;

import static org.hisp.dhis.merge.category.optioncombo.CategoryOptionComboMergeService.catOptCombosAreDuplicates;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class CategoryOptionComboMergeServiceTest {

  private CategoryCombo cc1;
  private CategoryCombo cc2;
  private CategoryOption co1;
  private CategoryOption co2;
  private CategoryOption co3;
  private CategoryOptionCombo coc1;
  private CategoryOptionCombo coc2;
  private CategoryOptionCombo coc3;

  @BeforeAll
  public void setup() {
    cc1 = new CategoryCombo();
    cc1.setName("cc1");
    cc1.setUid("UIDcatcom01");
    cc1.setCode("code1");

    cc2 = new CategoryCombo();
    cc2.setName("cc2");
    cc2.setUid("UIDcatcom02");
    cc2.setCode("code2");

    co1 = new CategoryOption();
    co1.setName("co1");
    co1.setUid("UIDcatopt01");
    co1.setCode("co1");
    co1.setShortName("co1");
    co1.setDescription("co1");

    co2 = new CategoryOption();
    co2.setName("co2");
    co2.setUid("UIDcatopt02");
    co2.setCode("co2");
    co2.setShortName("co2");
    co2.setDescription("co2");

    co3 = new CategoryOption();
    co3.setName("co3");
    co3.setUid("UIDcatopt03");
    co3.setCode("co3");
    co3.setShortName("co3");
    co3.setDescription("co3");

    coc1 = new CategoryOptionCombo();
    coc1.setName("coc1");
    coc1.setUid("UIDcoc00001");

    coc2 = new CategoryOptionCombo();
    coc2.setName("coc2");
    coc2.setUid("UIDcoc00002");

    coc3 = new CategoryOptionCombo();
    coc3.setName("coc3");
    coc3.setUid("UIDcoc00003");
  }

  @Test
  @DisplayName("COC with same CC and COs are detected as duplicates")
  void sameCcSameCoTest() {
    coc1.setCategoryCombo(cc1);
    coc2.setCategoryCombo(cc1);
    coc3.setCategoryCombo(cc1);
    coc1.setCategoryOptions(Set.of(co1, co2));
    coc2.setCategoryOptions(Set.of(co1, co2));
    coc3.setCategoryOptions(Set.of(co1, co2));

    assertTrue(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }

  @Test
  @DisplayName("COC with same CC and different number of COs are not detected as duplicates")
  void sameCcDiffCoTest() {
    coc1.setCategoryCombo(cc1);
    coc2.setCategoryCombo(cc1);
    coc3.setCategoryCombo(cc1);
    coc1.setCategoryOptions(Set.of(co1, co2));
    coc2.setCategoryOptions(Set.of(co1));
    coc3.setCategoryOptions(Set.of(co1, co2));

    assertFalse(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }

  @Test
  @DisplayName("COC with different CC and same COs are not detected as duplicates")
  void diffCcSameCoTest() {
    coc1.setCategoryCombo(cc1);
    coc2.setCategoryCombo(cc2);
    coc3.setCategoryCombo(cc1);
    coc1.setCategoryOptions(Set.of(co1, co2));
    coc2.setCategoryOptions(Set.of(co1, co2));
    coc3.setCategoryOptions(Set.of(co1, co2));

    assertFalse(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }

  @Test
  @DisplayName("COC with different CC and different COs are not detected as duplicates")
  void diffCcDiffCoTest() {
    coc1.setCategoryCombo(cc1);
    coc2.setCategoryCombo(cc2);
    coc3.setCategoryCombo(cc1);
    coc1.setCategoryOptions(Set.of(co2));
    coc2.setCategoryOptions(Set.of(co1, co2));
    coc3.setCategoryOptions(Set.of(co1));

    assertFalse(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }

  @Test
  @DisplayName("COC with different UIDs, same CC and same COs are detected as duplicates")
  void diffUidsCcCoTest() {
    coc1.setCategoryCombo(cc1);
    coc1.setUid("diff");
    coc2.setCategoryCombo(cc1);
    coc2.setUid("other");
    coc3.setCategoryCombo(cc1);
    coc3.setUid("more");
    coc1.setCategoryOptions(Set.of(co1, co2));
    coc2.setCategoryOptions(Set.of(co1, co2));
    coc3.setCategoryOptions(Set.of(co1, co2));

    assertTrue(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }

  @Test
  @DisplayName("COC with same UIDs, same CC and same COs are not detected as duplicates")
  void sameUidsCcCoTest() {
    coc1.setCategoryCombo(cc1);
    coc1.setUid("same");
    coc2.setCategoryCombo(cc1);
    coc2.setUid("same");
    coc3.setCategoryCombo(cc1);
    coc3.setUid("same");
    coc1.setCategoryOptions(Set.of(co1, co2));
    coc2.setCategoryOptions(Set.of(co1, co2));
    coc3.setCategoryOptions(Set.of(co1, co2));

    assertFalse(catOptCombosAreDuplicates(List.of(coc1, coc2), coc3));
  }
}
