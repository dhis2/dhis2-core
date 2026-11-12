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
 * hierarchy of each of its org unit restricted category options.
 *
 * <h2>Rule under test</h2>
 *
 * An org unit is valid for an AOC if, for <em>every</em> org unit restricted category option of the
 * AOC, the org unit is (or is a descendant of) <em>any</em> of that option's org units. Category
 * options without org units are ignored; they neither restrict nor grant anything. This matches
 * {@link CategoryOptionCombo#getOrganisationUnits()}, which intersects the org unit sets of the
 * restricted options.
 *
 * <ul>
 *   <li>OR within one option: an option assigned to D and O is valid under either.
 *   <li>AND across options: an AOC of two restricted options is only valid where both are.
 * </ul>
 *
 * <h2>Fixture</h2>
 *
 * Org unit tree. Data is always entered at the leaf F (under D) or at O:
 *
 * <pre>
 * R
 * ├── D
 * │   └── F
 * └── O
 * </pre>
 *
 * Category options and their org unit restrictions:
 *
 * <pre>
 * option | restricted to | think of it as
 * -------+---------------+-----------------------------------------------
 * A      | D             | mechanism only implemented in one country
 * B      | (none)        | mechanism usable everywhere
 * C      | (none)        | option of a second, unrestricted category
 * E      | D and O       | mechanism implemented in two countries
 * G      | O             | mechanism only implemented in the other country
 * </pre>
 *
 * Categories, attribute category combos and the AOCs used by the tests. Each combo gets its own
 * data element and data set (assigned to both F and O) so that only the AOC-OU check can reject a
 * value:
 *
 * <pre>
 * category combo         | AOC field          | options | valid at F | valid at O
 * -----------------------+--------------------+---------+------------+-----------
 * S = [X(A,B)]           | aocRestricted      | A       | yes        | no
 *                        | aocUnrestricted    | B       | yes        | yes
 * M = [X(A,B), Y(C)]     | aocMixed           | A, C    | yes        | no
 * U = [Z(E,G)]           | aocMultiOu         | E       | yes        | yes
 * V = [X(A,B), Z(E,G)]   | aocDisjoint        | A, G    | no         | no
 *                        | aocOverlap         | A, E    | yes        | no
 * </pre>
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

  /** AOC of only option A (restricted to D). Valid at F, not at O. */
  private CategoryOptionCombo aocRestricted;

  /**
   * AOC of only the unrestricted option B, of the same category combo as {@link #aocRestricted}.
   * Valid everywhere.
   */
  private CategoryOptionCombo aocUnrestricted;

  /**
   * AOC of the restricted option A and the unrestricted option C. C adds no restriction, so this
   * behaves like A alone: valid at F, not at O.
   */
  private CategoryOptionCombo aocMixed;

  /**
   * AOC of only option E, which is restricted to two org units (D and O). Valid at F (via D) and at
   * O.
   */
  private CategoryOptionCombo aocMultiOu;

  /**
   * AOC of options A (restricted to D) and G (restricted to O). The two options have no org unit in
   * common, so the AOC is valid nowhere; this is a metadata misconfiguration.
   */
  private CategoryOptionCombo aocDisjoint;

  /**
   * AOC of options A (restricted to D) and E (restricted to D and O). The options overlap only in
   * D, so the AOC is valid at F but not at O, even though E alone would allow O.
   */
  private CategoryOptionCombo aocOverlap;

  @BeforeEach
  void setUp() {
    // org unit tree: R > D > F and R > O
    OrganisationUnit ouR = createOrganisationUnit('R');
    OrganisationUnit ouD = createOrganisationUnit('D', ouR);
    ouF = createOrganisationUnit('F', ouD);
    ouO = createOrganisationUnit('O', ouR);
    manager.save(ouR);
    manager.save(ouD);
    manager.save(ouF);
    manager.save(ouO);

    // category options; see the class Javadoc for what each restriction represents
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

    // attribute categories: X(A,B), Y(C), Z(E,G)
    Category categoryAB = createCategory('X', coA, coB);
    categoryAB.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    Category categoryC = createCategory('Y', coC);
    categoryC.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(categoryAB);
    manager.save(categoryC);
    Category categoryEG = createCategory('Z', coE, coG);
    categoryEG.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(categoryEG);

    // attribute category combos:
    // S = one restricted-capable category
    // M = a restricted-capable category plus an unrestricted one
    // U = one category whose options have multiple org units
    // V = two categories that both have restricted options
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

    // pick the AOCs used by the tests out of the generated combos
    aocRestricted = getOptionCombo(comboSingle, coA);
    aocUnrestricted = getOptionCombo(comboSingle, coB);
    aocMixed = getOptionCombo(comboMixed, coA, coC);
    aocMultiOu = getOptionCombo(comboMultiOu, coE);
    aocDisjoint = getOptionCombo(comboCross, coA, coG);
    aocOverlap = getOptionCombo(comboCross, coA, coE);

    // one data element and data set per combo; every data set is assigned to both F and O so the
    // data set assignment never rejects a value, leaving the AOC-OU check as the only one at play
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

  /** One restricted option (A, restricted to D): data at F is accepted because F is under D. */
  @Test
  void acceptsRestrictedAocWithinHierarchy() throws Exception {
    assertEquals(1, upsert(value(deSingle, ouF, aocRestricted)).succeeded());
  }

  /** One restricted option (A, restricted to D): data at O is rejected because O is not under D. */
  @Test
  void rejectsRestrictedAocOutsideHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deSingle, ouO, aocRestricted));
  }

  /**
   * A restricted AOC (A at F) and an unrestricted AOC (B at O) in the same import. Both are
   * accepted: B has no org units, so it can be used anywhere, and skipping unrestricted AOCs must
   * not cause the restricted one to be skipped as well (or vice versa).
   */
  @Test
  void acceptsUnrestrictedAocAlongsideRestrictedAoc() throws Exception {
    assertEquals(
        2,
        upsert(value(deSingle, ouF, aocRestricted), value(deSingle, ouO, aocUnrestricted))
            .succeeded());
  }

  /**
   * Two categories, one restricted option (A, restricted to D) and one unrestricted option (C).
   * Data at F is accepted: C is ignored, and F satisfies A.
   */
  @Test
  void acceptsAocMixingRestrictedAndUnrestrictedOptionsWithinHierarchy() throws Exception {
    assertEquals(1, upsert(value(deMixed, ouF, aocMixed)).succeeded());
  }

  /**
   * Two categories, one restricted option (A, restricted to D) and one unrestricted option (C).
   * Data at O is rejected: the unrestricted C must not be read as "no restriction" for the whole
   * AOC, so A's restriction still applies.
   */
  @Test
  void rejectsAocMixingRestrictedAndUnrestrictedOptionsOutsideHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deMixed, ouO, aocMixed));
  }

  /**
   * One option (E) restricted to two org units, D and O, such as a mechanism implemented in two
   * countries. Data is accepted at F (under D) and at O: the org unit only needs to be under
   * <em>one</em> of the option's org units, not all of them.
   */
  @Test
  void acceptsOptionRestrictedToMultipleOrgUnitsAtAnyOfThem() throws Exception {
    assertEquals(
        2,
        upsert(value(deMultiOu, ouF, aocMultiOu), value(deMultiOu, ouO, aocMultiOu)).succeeded());
  }

  /**
   * Two restricted options with no org unit in common: A (restricted to D) and G (restricted to O).
   * Data at F is rejected even though F satisfies A, because it does not satisfy G. Every
   * restricted option of the AOC must be satisfied, not just one of them.
   */
  @Test
  void rejectsAocWithDisjointRestrictedOptionsInsideFirstOptionHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deCross, ouF, aocDisjoint));
  }

  /**
   * Same disjoint AOC as above (A restricted to D, G restricted to O), now with data at O. Rejected
   * because O satisfies G but not A. Together with the previous test this shows the AOC is valid
   * nowhere.
   */
  @Test
  void rejectsAocWithDisjointRestrictedOptionsInsideSecondOptionHierarchy() {
    assertAocNotUsableWithOrgUnit(value(deCross, ouO, aocDisjoint));
  }

  /**
   * Two restricted options that overlap: A (restricted to D) and E (restricted to D and O). Data at
   * F is accepted because F is under D, which satisfies both options.
   */
  @Test
  void acceptsAocWithOverlappingRestrictedOptionsWithinIntersection() throws Exception {
    assertEquals(1, upsert(value(deCross, ouF, aocOverlap)).succeeded());
  }

  /**
   * Two restricted options that overlap: A (restricted to D) and E (restricted to D and O). Data at
   * O is rejected: O satisfies E, but not A. The AOC is only valid where the options overlap.
   */
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
