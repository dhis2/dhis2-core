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
package org.hisp.dhis.merge.category.categorycombo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for CategoryCombo merge functionality. Tests verify that references to source
 * CategoryCombos are replaced with target CategoryCombo references.
 */
@Transactional
class CategoryComboMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryComboMergeService;
  @Autowired private DbmsManager dbmsManager;
  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  private CategoryCombo ccSource1;
  private CategoryCombo ccSource2;
  private CategoryCombo ccTarget;
  private Category category1;
  private Category category2;
  private CategoryOption co1;
  private CategoryOption co2;
  private CategoryOption co3;
  private CategoryOption co4;

  @BeforeEach
  void setUpCatModel() {
    // Create category model that satisfies validation constraints
    createCos();
    createCs();
    createCcs();
    generateCocs();
  }

  private void generateCocs() {
    categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccSource1);
    categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccSource2);
    categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccTarget);
  }

  private void createCcs() {
    ccSource1 = createCategoryCombo('A', category1, category2);
    ccSource2 = createCategoryCombo('B', category1, category2);
    ccTarget = createCategoryCombo('C', category1, category2);
    categoryService.addCategoryCombo(ccSource1);
    categoryService.addCategoryCombo(ccSource2);
    categoryService.addCategoryCombo(ccTarget);
  }

  private void createCs() {
    category1 = createCategory('1', co1, co2);
    category2 = createCategory('2', co3, co4);
    categoryService.addCategory(category1);
    categoryService.addCategory(category2);
  }

  private void createCos() {
    co1 = createCategoryOption('1');
    co2 = createCategoryOption('2');
    co3 = createCategoryOption('3');
    co4 = createCategoryOption('4');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);
    categoryService.addCategoryOption(co4);
  }

  // -----------------------------
  // ----- CategoryOptionCombo ---
  // -----------------------------
  @Test
  @DisplayName("CategoryOptionCombo refs to source CategoryCombos are replaced")
  void categoryOptionComboRefsReplacedTest() throws ConflictException {
    // given COCs exist for each category combo
    List<CategoryOptionCombo> sourceCocs1 = List.copyOf(ccSource1.getOptionCombos());
    List<CategoryOptionCombo> sourceCocs2 = List.copyOf(ccSource2.getOptionCombos());
    assertFalse(sourceCocs1.isEmpty(), "Source 1 should have COCs");
    assertFalse(sourceCocs2.isEmpty(), "Source 2 should have COCs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    // refresh and check that COCs now point to target
    for (CategoryOptionCombo coc : sourceCocs1) {
      CategoryOptionCombo refreshedCoc = categoryService.getCategoryOptionCombo(coc.getUid());
      if (refreshedCoc != null) {
        assertEquals(
            ccTarget.getUid(),
            refreshedCoc.getCategoryCombo().getUid(),
            "COC should point to target CategoryCombo");
      }
    }
  }

  // -----------------------------
  // ----- DataElement -----------
  // -----------------------------
  @Test
  @DisplayName("DataElement refs to source CategoryCombos are replaced")
  void dataElementRefsReplacedTest() throws ConflictException {
    // given DataElements with source CategoryCombos
    DataElement de1 = createDataElement('1');
    DataElement de2 = createDataElement('2');
    de1.setCategoryCombo(ccSource1);
    de2.setCategoryCombo(ccSource2);
    manager.save(de1);
    manager.save(de2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    DataElement refreshedDe1 = manager.get(DataElement.class, de1.getUid());
    DataElement refreshedDe2 = manager.get(DataElement.class, de2.getUid());
    assertEquals(ccTarget.getUid(), refreshedDe1.getCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedDe2.getCategoryCombo().getUid());
  }

  // -----------------------------
  // ----- DataSet ---------------
  // -----------------------------
  @Test
  @DisplayName("DataSet refs to source CategoryCombos are replaced")
  void dataSetRefsReplacedTest() throws ConflictException {
    // given DataSets with source CategoryCombos
    PeriodType periodType = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    DataSet ds1 = createDataSet('1', periodType);
    DataSet ds2 = createDataSet('2', periodType);
    ds1.setCategoryCombo(ccSource1);
    ds2.setCategoryCombo(ccSource2);
    manager.save(ds1);
    manager.save(ds2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    DataSet refreshedDs1 = manager.get(DataSet.class, ds1.getUid());
    DataSet refreshedDs2 = manager.get(DataSet.class, ds2.getUid());
    assertEquals(ccTarget.getUid(), refreshedDs1.getCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedDs2.getCategoryCombo().getUid());
  }

  // -----------------------------
  // ----- DataSetElement --------
  // -----------------------------
  @Test
  @DisplayName("DataSetElement refs to source CategoryCombos are replaced")
  void dataSetElementRefsReplacedTest() throws ConflictException {
    // given DataSetElements with source CategoryCombos
    PeriodType periodType = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    DataSet ds = createDataSet('1', periodType);
    ds.setCategoryCombo(ccTarget);
    manager.save(ds);

    DataElement de = createDataElement('1');
    de.setCategoryCombo(ccTarget);
    manager.save(de);

    DataSetElement dse = new DataSetElement(ds, de, ccSource1);
    ds.getDataSetElements().add(dse);
    manager.update(ds);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    DataSet refreshedDs = manager.get(DataSet.class, ds.getUid());
    DataSetElement refreshedDse = refreshedDs.getDataSetElements().iterator().next();
    assertEquals(ccTarget.getUid(), refreshedDse.getCategoryCombo().getUid());
  }

  // -----------------------------
  // ----- Program ---------------
  // -----------------------------
  @Test
  @DisplayName("Program categoryCombo refs are replaced")
  void programCategoryComboRefsReplacedTest() throws ConflictException {
    // given Programs with source CategoryCombos
    Program p1 = createProgram('1');
    Program p2 = createProgram('2');
    p1.setCategoryCombo(ccSource1);
    p2.setCategoryCombo(ccSource2);
    manager.save(p1);
    manager.save(p2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    Program refreshedP1 = manager.get(Program.class, p1.getUid());
    Program refreshedP2 = manager.get(Program.class, p2.getUid());
    assertEquals(ccTarget.getUid(), refreshedP1.getCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedP2.getCategoryCombo().getUid());
  }

  @Test
  @DisplayName("Program enrollmentCategoryCombo refs are replaced")
  void programEnrollmentCategoryComboRefsReplacedTest() throws ConflictException {
    // given Programs with source enrollment CategoryCombos
    Program p1 = createProgram('1');
    Program p2 = createProgram('2');
    p1.setProgramType(ProgramType.WITH_REGISTRATION);
    p2.setProgramType(ProgramType.WITH_REGISTRATION);
    p1.setCategoryCombo(ccTarget);
    p2.setCategoryCombo(ccTarget);
    p1.setEnrollmentCategoryCombo(ccSource1);
    p2.setEnrollmentCategoryCombo(ccSource2);
    manager.save(p1);
    manager.save(p2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    Program refreshedP1 = manager.get(Program.class, p1.getUid());
    Program refreshedP2 = manager.get(Program.class, p2.getUid());
    assertEquals(ccTarget.getUid(), refreshedP1.getEnrollmentCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedP2.getEnrollmentCategoryCombo().getUid());
  }

  // -----------------------------
  // ----- ProgramIndicator ------
  // -----------------------------
  @Test
  @DisplayName("ProgramIndicator categoryCombo refs are replaced")
  void programIndicatorCategoryComboRefsReplacedTest() throws ConflictException {
    // given ProgramIndicators with source CategoryCombos
    Program program = createProgram('P');
    program.setCategoryCombo(ccTarget);
    manager.save(program);

    ProgramIndicator pi1 = createProgramIndicator('1', program, "1+1", null);
    ProgramIndicator pi2 = createProgramIndicator('2', program, "2+2", null);
    pi1.setCategoryCombo(ccSource1);
    pi2.setCategoryCombo(ccSource2);
    manager.save(pi1);
    manager.save(pi2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    ProgramIndicator refreshedPi1 = manager.get(ProgramIndicator.class, pi1.getUid());
    ProgramIndicator refreshedPi2 = manager.get(ProgramIndicator.class, pi2.getUid());
    assertEquals(ccTarget.getUid(), refreshedPi1.getCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedPi2.getCategoryCombo().getUid());
  }

  @Test
  @DisplayName("ProgramIndicator attributeCombo refs are replaced")
  void programIndicatorAttributeComboRefsReplacedTest() throws ConflictException {
    // given ProgramIndicators with source attributeCombos
    Program program = createProgram('P');
    program.setCategoryCombo(ccTarget);
    manager.save(program);

    ProgramIndicator pi1 = createProgramIndicator('3', program, "3+3", null);
    ProgramIndicator pi2 = createProgramIndicator('4', program, "4+4", null);
    pi1.setAttributeCombo(ccSource1);
    pi2.setAttributeCombo(ccSource2);
    manager.save(pi1);
    manager.save(pi2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    ProgramIndicator refreshedPi1 = manager.get(ProgramIndicator.class, pi1.getUid());
    ProgramIndicator refreshedPi2 = manager.get(ProgramIndicator.class, pi2.getUid());
    assertEquals(ccTarget.getUid(), refreshedPi1.getAttributeCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedPi2.getAttributeCombo().getUid());
  }

  // -----------------------------
  // -- DataApprovalWorkflow -----
  // -----------------------------
  @Test
  @DisplayName("DataApprovalWorkflow refs to source CategoryCombos are replaced")
  void dataApprovalWorkflowRefsReplacedTest() throws ConflictException {
    // given DataApprovalWorkflows with source CategoryCombos
    PeriodType periodType = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    DataApprovalWorkflow wf1 =
        new DataApprovalWorkflow("Workflow1", periodType, ccSource1, Set.of());
    DataApprovalWorkflow wf2 =
        new DataApprovalWorkflow("Workflow2", periodType, ccSource2, Set.of());
    manager.save(wf1);
    manager.save(wf2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());

    DataApprovalWorkflow refreshedWf1 = manager.get(DataApprovalWorkflow.class, wf1.getUid());
    DataApprovalWorkflow refreshedWf2 = manager.get(DataApprovalWorkflow.class, wf2.getUid());
    assertEquals(ccTarget.getUid(), refreshedWf1.getCategoryCombo().getUid());
    assertEquals(ccTarget.getUid(), refreshedWf2.getCategoryCombo().getUid());
  }

  // -----------------------------
  // ----- Delete Sources --------
  // -----------------------------
  @Test
  @DisplayName("Sources are deleted when deleteSources=true")
  void sourcesDeletedTest() throws ConflictException {
    // given
    String source1Uid = ccSource1.getUid();
    String source2Uid = ccSource2.getUid();

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());
    assertNull(categoryService.getCategoryCombo(source1Uid), "Source 1 should be deleted");
    assertNull(categoryService.getCategoryCombo(source2Uid), "Source 2 should be deleted");
    assertNotNull(categoryService.getCategoryCombo(ccTarget.getUid()), "Target should still exist");
  }

  @Test
  @DisplayName("Sources are not deleted when deleteSources=false")
  void sourcesNotDeletedTest() throws ConflictException {
    // given
    String source1Uid = ccSource1.getUid();
    String source2Uid = ccSource2.getUid();

    // when
    MergeParams mergeParams = getMergeParams(List.of(ccSource1, ccSource2), ccTarget);
    mergeParams.setDeleteSources(false);
    MergeReport report = categoryComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());
    assertNotNull(categoryService.getCategoryCombo(source1Uid), "Source 1 should still exist");
    assertNotNull(categoryService.getCategoryCombo(source2Uid), "Source 2 should still exist");
  }

  // -----------------------------
  // ----- Validation ------------
  // -----------------------------
  @Test
  @DisplayName("Merge is rejected when source and target have different Categories")
  void differentCategoriesRejectedTest() {
    // given source and target with different categories
    Category differentCategory = createCategory('Z', co1);
    categoryService.addCategory(differentCategory);

    CategoryCombo differentCc = createCategoryCombo('Z', differentCategory);
    categoryService.addCategoryCombo(differentCc);

    // when/then
    MergeParams mergeParams = getMergeParams(List.of(differentCc), ccTarget);
    ConflictException exception =
        assertThrows(
            ConflictException.class, () -> categoryComboMergeService.processMerge(mergeParams));
    assertTrue(exception.getMessage().contains("Merge validation error"));
  }

  // -----------------------------
  // ----- Helper Methods --------
  // -----------------------------
  private MergeParams getMergeParams(List<CategoryCombo> sources, CategoryCombo target) {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(sources.toArray(new CategoryCombo[0])));
    mergeParams.setTarget(UID.of(target));
    mergeParams.setDeleteSources(true);
    return mergeParams;
  }
}
