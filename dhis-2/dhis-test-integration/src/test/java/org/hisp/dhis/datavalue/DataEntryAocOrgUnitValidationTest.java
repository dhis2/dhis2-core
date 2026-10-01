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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that data entry only accepts an attribute option combo (AOC) at org units within the
 * hierarchy of each of its org unit restricted category options. Org unit tree used by all tests:
 *
 * <pre>
 * R
 * ├── D
 * │   └── F
 * └── O
 * </pre>
 *
 * Category option A is restricted to D; category options B and C are not restricted. Category
 * option E is restricted to both D and O; category option G is restricted to O.
 *
 * <p>The expected semantics (matching {@link CategoryOptionCombo#getOrganisationUnits()}) are: an
 * org unit is valid for an AOC if, for <em>every</em> org unit restricted option of the AOC, it is
 * within the hierarchy of <em>any</em> of that option's org units. Unrestricted options are
 * ignored.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
class DataEntryAocOrgUnitValidationTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;

  @Autowired private DataEntryService dataEntryService;

  @Autowired private DataSetService dataSetService;

  private OrganisationUnit ouF;
  private OrganisationUnit ouO;

  private DataElement deSingle;
  private DataElement deMixed;
  private DataElement deMultiOu;
  private DataElement deCross;

  /** AOC of only the restricted option A. */
  private CategoryOptionCombo aocRestricted;

  /** AOC of only the unrestricted option B, of the same category combo as {@link #aocRestricted} */
  private CategoryOptionCombo aocUnrestricted;

  /** AOC of the restricted option A and the unrestricted option C. */
  private CategoryOptionCombo aocMixed;

  /** AOC of only option E, which is restricted to two org units (D and O). */
  private CategoryOptionCombo aocMultiOu;

  /** AOC of options A (restricted to D) and G (restricted to O), which have no OU in common. */
  private CategoryOptionCombo aocDisjoint;

  /** AOC of options A (restricted to D) and E (restricted to D and O), overlapping in D. */
  private CategoryOptionCombo aocOverlap;

  @BeforeEach
  void setUp() {
    OrganisationUnit ouR = createOrganisationUnit('R');
    OrganisationUnit ouD = createOrganisationUnit('D', ouR);
    ouF = createOrganisationUnit('F', ouD);
    ouO = createOrganisationUnit('O', ouR);
    manager.save(ouR);
    manager.save(ouD);
    manager.save(ouF);
    manager.save(ouO);

    CategoryOption coA = createCategoryOption('A');
    coA.addOrganisationUnit(ouD);
    CategoryOption coB = createCategoryOption('B');
    CategoryOption coC = createCategoryOption('C');
    manager.save(coA);
    manager.save(coB);
    manager.save(coC);
    CategoryOption coE = createCategoryOption('E');
    coE.addOrganisationUnit(ouD);
    coE.addOrganisationUnit(ouO);
    CategoryOption coG = createCategoryOption('G');
    coG.addOrganisationUnit(ouO);
    manager.save(coE);
    manager.save(coG);

    Category categoryAB = createCategory('X', coA, coB);
    categoryAB.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    Category categoryC = createCategory('Y', coC);
    categoryC.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(categoryAB);
    manager.save(categoryC);
    Category categoryEG = createCategory('Z', coE, coG);
    categoryEG.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(categoryEG);

    CategoryCombo comboSingle = createCategoryCombo('S', categoryAB);
    comboSingle.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    CategoryCombo comboMixed = createCategoryCombo('M', categoryAB, categoryC);
    comboMixed.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(comboSingle);
    manager.save(comboMixed);
    CategoryCombo comboMultiOu = createCategoryCombo('U', categoryEG);
    comboMultiOu.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    CategoryCombo comboCross = createCategoryCombo('V', categoryAB, categoryEG);
    comboCross.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(comboMultiOu);
    manager.save(comboCross);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(comboSingle);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(comboMixed);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(comboMultiOu);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(comboCross);

    aocRestricted = getOptionCombo(comboSingle, coA);
    aocUnrestricted = getOptionCombo(comboSingle, coB);
    aocMixed = getOptionCombo(comboMixed, coA, coC);
    aocMultiOu = getOptionCombo(comboMultiOu, coE);
    aocDisjoint = getOptionCombo(comboCross, coA, coG);
    aocOverlap = getOptionCombo(comboCross, coA, coE);

    deSingle = createDataElement('S');
    deMixed = createDataElement('M');
    manager.save(deSingle);
    manager.save(deMixed);
    deMultiOu = createDataElement('U');
    deCross = createDataElement('V');
    manager.save(deMultiOu);
    manager.save(deCross);

    DataSet dsSingle = createDataSet('S', new MonthlyPeriodType());
    dsSingle.setCategoryCombo(comboSingle);
    dsSingle.addDataSetElement(deSingle);
    dsSingle.addOrganisationUnit(ouF);
    dsSingle.addOrganisationUnit(ouO);
    DataSet dsMixed = createDataSet('M', new MonthlyPeriodType());
    dsMixed.setCategoryCombo(comboMixed);
    dsMixed.addDataSetElement(deMixed);
    dsMixed.addOrganisationUnit(ouF);
    dsMixed.addOrganisationUnit(ouO);
    dataSetService.addDataSet(dsSingle);
    dataSetService.addDataSet(dsMixed);
    DataSet dsMultiOu = createDataSet('U', new MonthlyPeriodType());
    dsMultiOu.setCategoryCombo(comboMultiOu);
    dsMultiOu.addDataSetElement(deMultiOu);
    dsMultiOu.addOrganisationUnit(ouF);
    dsMultiOu.addOrganisationUnit(ouO);
    DataSet dsCross = createDataSet('V', new MonthlyPeriodType());
    dsCross.setCategoryCombo(comboCross);
    dsCross.addDataSetElement(deCross);
    dsCross.addOrganisationUnit(ouF);
    dsCross.addOrganisationUnit(ouO);
    dataSetService.addDataSet(dsMultiOu);
    dataSetService.addDataSet(dsCross);
  }

  @Test
  void acceptsRestrictedAocWithinHierarchy() throws Exception {
    assertEquals(1, upsert(value(deSingle, ouF, aocRestricted)).succeeded());
  }

  @Test
  void rejectsRestrictedAocOutsideHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deSingle, ouO, aocRestricted));
  }

  @Test
  void acceptsUnrestrictedAocAlongsideRestrictedAoc() throws Exception {
    assertEquals(
        2,
        upsert(value(deSingle, ouF, aocRestricted), value(deSingle, ouO, aocUnrestricted))
            .succeeded());
  }

  @Test
  void acceptsAocMixingRestrictedAndUnrestrictedOptionsWithinHierarchy() throws Exception {
    assertEquals(1, upsert(value(deMixed, ouF, aocMixed)).succeeded());
  }

  @Test
  void rejectsAocMixingRestrictedAndUnrestrictedOptionsOutsideHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deMixed, ouO, aocMixed));
  }

  @Test
  void acceptsOptionRestrictedToMultipleOrgUnitsAtAnyOfThem() throws Exception {
    assertEquals(
        2,
        upsert(value(deMultiOu, ouF, aocMultiOu), value(deMultiOu, ouO, aocMultiOu)).succeeded());
  }

  @Test
  void rejectsAocWithDisjointRestrictedOptionsInsideFirstOptionHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deCross, ouF, aocDisjoint));
  }

  @Test
  void rejectsAocWithDisjointRestrictedOptionsInsideSecondOptionHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deCross, ouO, aocDisjoint));
  }

  @Test
  void acceptsAocWithOverlappingRestrictedOptionsWithinIntersection() throws Exception {
    assertEquals(1, upsert(value(deCross, ouF, aocOverlap)).succeeded());
  }

  @Test
  void rejectsAocWithOverlappingRestrictedOptionsOutsideIntersection() {
    assertAocNotUsableWithOrgUnit(value(deCross, ouO, aocOverlap));
  }

  private void assertAocNotUsableWithOrgUnit(DataEntryValue.Input value) {
    ConflictException ex = assertThrows(ConflictException.class, () -> upsert(value));
    assertEquals(ErrorCode.E8025, ex.getCode());
  }

  private DataEntrySummary upsert(DataEntryValue.Input... values) throws Exception {
    return dataEntryService.upsertGroup(
        new DataEntryGroup.Options(),
        dataEntryService.decodeGroup(new DataEntryGroup.Input(List.of(values))),
        transitory());
  }

  private static DataEntryValue.Input value(
      DataElement de, OrganisationUnit ou, CategoryOptionCombo aoc) {
    return new DataEntryValue.Input(
        de.getUid(), ou.getUid(), null, aoc.getUid(), "202501", "1", null);
  }

  private static CategoryOptionCombo getOptionCombo(
      CategoryCombo combo, CategoryOption... options) {
    Set<CategoryOption> expected = Set.of(options);
    return combo.getOptionCombos().stream()
        .filter(coc -> coc.getCategoryOptions().equals(expected))
        .findFirst()
        .orElseThrow();
  }
}
