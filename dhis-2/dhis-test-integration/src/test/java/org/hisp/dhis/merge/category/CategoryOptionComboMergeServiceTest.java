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
package org.hisp.dhis.merge.category;

import static org.hisp.dhis.dataapproval.DataApprovalAction.APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditStore;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorStore;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the tests in this class basically follow the same approach:
 *
 * <p>- Create metadata which have source CategoryOptionCombo references
 *
 * <p>- Perform a CategoryOption merge, passing a target CategoryOptionCombo
 *
 * <p>- Check that source CategoryOptionCombos have had their references removed/replaced with the
 * target CategoryOptionCombo
 */
@Transactional
class CategoryOptionComboMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionStore categoryOptionStore;
  @Autowired private CategoryComboStore categoryComboStore;
  @Autowired private DataElementOperandStore dataElementOperandStore;
  @Autowired private MinMaxDataElementStore minMaxDataElementStore;
  @Autowired private PredictorStore predictorStore;
  @Autowired private SMSCommandStore smsCommandStore;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryOptionComboMergeService;
  @Autowired private PeriodService periodService;
  @Autowired private DataValueStore dataValueStore;
  @Autowired private DataValueAuditStore dataValueAuditStore;
  @Autowired private DataApprovalAuditStore dataApprovalAuditStore;
  private Category cat1;
  private Category cat2;
  private Category cat3;
  private Category cat4;

  private CategoryCombo cc1;
  private CategoryCombo cc2;
  private CategoryOption co1A;
  private CategoryOption co1B;
  private CategoryOption co2A;
  private CategoryOption co2B;
  private CategoryOption co3A;
  private CategoryOption co3B;
  private CategoryOption co4A;
  private CategoryOption co4B;
  private CategoryOptionCombo cocSource1;
  private CategoryOptionCombo cocSource2;
  private CategoryOptionCombo cocTarget;
  private CategoryOptionCombo cocRandom;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private DataElement de1;
  private DataElement de2;
  private DataElement de3;

  @BeforeEach
  public void setUp() {
    // 8 category options
    co1A = createCategoryOption("1A", CodeGenerator.generateUid());
    co1B = createCategoryOption("1B", CodeGenerator.generateUid());
    co2A = createCategoryOption("2A", CodeGenerator.generateUid());
    co2B = createCategoryOption("2B", CodeGenerator.generateUid());
    co3A = createCategoryOption("3A", CodeGenerator.generateUid());
    co3B = createCategoryOption("3B", CodeGenerator.generateUid());
    co4A = createCategoryOption("4A", CodeGenerator.generateUid());
    co4B = createCategoryOption("4B", CodeGenerator.generateUid());
    categoryService.addCategoryOption(co1A);
    categoryService.addCategoryOption(co1B);
    categoryService.addCategoryOption(co2A);
    categoryService.addCategoryOption(co2B);
    categoryService.addCategoryOption(co3A);
    categoryService.addCategoryOption(co3B);
    categoryService.addCategoryOption(co4A);
    categoryService.addCategoryOption(co4B);

    // 4 categories (each with 2 category options)
    cat1 = createCategory('1', co1A, co1B);
    cat2 = createCategory('2', co2A, co2B);
    cat3 = createCategory('3', co3A, co3B);
    cat4 = createCategory('4', co4A, co4B);
    categoryService.addCategory(cat1);
    categoryService.addCategory(cat2);
    categoryService.addCategory(cat3);
    categoryService.addCategory(cat4);

    cc1 = createCategoryCombo('1', cat1, cat2);
    cc2 = createCategoryCombo('2', cat3, cat4);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);

    categoryService.generateOptionCombos(cc1);
    categoryService.generateOptionCombos(cc2);

    cocSource1 = getCocWithOptions("1A", "2A");
    cocSource2 = getCocWithOptions("1B", "2B");
    cocTarget = getCocWithOptions("3A", "4B");
    cocRandom = getCocWithOptions("3B", "4A");

    ou1 = createOrganisationUnit('A');
    ou2 = createOrganisationUnit('B');
    ou3 = createOrganisationUnit('C');
    manager.save(List.of(ou1, ou2, ou3));

    de1 = createDataElement('1');
    de2 = createDataElement('2');
    de3 = createDataElement('3');
    manager.save(List.of(de1, de2, de3));
  }

  // -----------------------------
  // ------ CategoryOption -------
  // -----------------------------
  @Test
  @DisplayName("CategoryOption refs to source CategoryOptionCombos are replaced, sources deleted")
  void categoryOptionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category option combo state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");
    assertEquals(9, allCategoryOptions.size(), "9 COs including 1 default");

    List<CategoryOption> coSourcesBefore =
        categoryOptionStore.getByCategoryOptionCombo(
            List.of(UID.of(cocSource1.getUid()), UID.of(cocSource2.getUid())));
    List<CategoryOption> coTargetBefore =
        categoryOptionStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget.getUid())));

    assertEquals(
        4,
        coSourcesBefore.size(),
        "Expect 4 category options with source category option combo refs");
    assertEquals(
        2,
        coTargetBefore.size(),
        "Expect 2 category options with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<CategoryOption> coSourcesAfter =
        categoryOptionStore.getByCategoryOptionCombo(
            List.of(UID.of(cocSource1), UID.of(cocSource2)));
    List<CategoryOption> coTargetAfter =
        categoryOptionStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, coSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        6, coTargetAfter.size(), "Expect 6 entries with target category option combo refs");

    assertTrue(
        categoryService.getCategoryOptionCombosByUid(UID.of(cocSource1, cocSource2)).isEmpty(),
        "There should be no source COCs after deletion during merge");
  }

  // -----------------------------
  // ------ CategoryCombo -------
  // -----------------------------
  @Test
  @DisplayName("CategoryCombo refs to source CategoryOptionCombos are replaced, sources deleted")
  void categoryComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category option combo state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();
    List<CategoryCombo> allCategoryCombos = categoryService.getAllCategoryCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");
    assertEquals(3, allCategoryCombos.size(), "3 CCs including 1 default");

    List<CategoryCombo> ccSourcesBefore =
        categoryComboStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<CategoryCombo> ccTargetBefore =
        categoryComboStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(
        1,
        ccSourcesBefore.size(),
        "Expect 1 category combo with source category option combo refs");
    assertEquals(
        1, ccTargetBefore.size(), "Expect 1 category combo with target category option combo refs");
    assertEquals(
        4,
        ccTargetBefore.get(0).getOptionCombos().size(),
        "Expect 4 COCs with target category combo");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();
    List<CategoryCombo> allCCsAfter = categoryService.getAllCategoryCombos();

    assertEquals(7, allCOCsAfter.size(), "7 COCs including 1 default");
    assertEquals(3, allCCsAfter.size(), "3 CCs including 1 default");

    // then
    List<CategoryCombo> ccSourcesAfter =
        categoryComboStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<CategoryCombo> ccTargetAfter =
        categoryComboStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget)));
    CategoryCombo catCombo1 = categoryComboStore.getByUid(cc1.getUid());

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, ccSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        1, ccTargetAfter.size(), "Expect 2 entries with target category option combo refs");
    assertEquals(5, catCombo1.getOptionCombos().size(), "Expect 5 COCs for CC1");
    assertEquals(
        4,
        ccTargetAfter.get(0).getOptionCombos().size(),
        "Expect 4 COCs with target category combo");

    assertTrue(
        categoryService.getCategoryOptionCombosByUid(UID.of(cocSource1, cocSource2)).isEmpty(),
        "There should be no source COCs after deletion during merge");
  }

  // -----------------------------
  // ---- DataElementOperand -----
  // -----------------------------
  @Test
  @DisplayName(
      "DataElementOperand refs to source CategoryOptionCombos are replaced, sources deleted")
  void dataElementOperandRefsReplacedSourcesDeletedTest() throws ConflictException {
    DataElementOperand deo1 = new DataElementOperand();
    deo1.setDataElement(de1);
    deo1.setCategoryOptionCombo(cocSource1);

    DataElementOperand deo2 = new DataElementOperand();
    deo2.setDataElement(de2);
    deo2.setCategoryOptionCombo(cocSource2);

    DataElementOperand deo3 = new DataElementOperand();
    deo3.setDataElement(de3);
    deo3.setCategoryOptionCombo(cocTarget);

    manager.save(List.of(de1, de2, de3));
    manager.save(List.of(deo1, deo2, deo3));

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");

    List<DataElementOperand> deoSourcesBefore =
        dataElementOperandStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<DataElementOperand> deoTargetBefore =
        dataElementOperandStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(
        2,
        deoSourcesBefore.size(),
        "Expect 2 data element operands with source category option combo refs");
    assertEquals(
        1,
        deoTargetBefore.size(),
        "Expect 1 data element operand with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCOCsAfter.size(), "7 COCs including 1 default");

    // then
    List<DataElementOperand> deoSourcesAfter =
        dataElementOperandStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<DataElementOperand> deoTargetAfter =
        dataElementOperandStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, deoSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        3, deoTargetAfter.size(), "Expect 3 entries with target category option combo refs");
  }

  // -----------------------------
  // ---- MinMaxDataElement -----
  // -----------------------------
  @Test
  @DisplayName(
      "MinMaxDataElement refs to source CategoryOptionCombos are replaced, sources deleted")
  void minMaxDataElementRefsReplacedSourcesDeletedTest() throws ConflictException {
    //    DataElement de1 = createDataElement('1');
    //    DataElement de2 = createDataElement('2');
    //    DataElement de3 = createDataElement('3');
    //    manager.save(List.of(de1, de2, de3));

    OrganisationUnit ou1 = createOrganisationUnit('1');
    OrganisationUnit ou2 = createOrganisationUnit('2');
    OrganisationUnit ou3 = createOrganisationUnit('3');
    manager.save(List.of(ou1, ou2, ou3));

    MinMaxDataElement mmde1 = new MinMaxDataElement(de1, ou1, cocSource1, 0, 100, false);
    MinMaxDataElement mmde2 = new MinMaxDataElement(de2, ou2, cocSource2, 0, 100, false);
    MinMaxDataElement mmde3 = new MinMaxDataElement(de3, ou3, cocTarget, 0, 100, false);
    minMaxDataElementStore.save(mmde1);
    minMaxDataElementStore.save(mmde2);
    minMaxDataElementStore.save(mmde3);

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");

    List<MinMaxDataElement> mmdeSourcesBefore =
        minMaxDataElementStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<MinMaxDataElement> mmdeTargetBefore =
        minMaxDataElementStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(
        2,
        mmdeSourcesBefore.size(),
        "Expect 2 min max data elements with source category option combo refs");
    assertEquals(
        1,
        mmdeTargetBefore.size(),
        "Expect 1 min max data element with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCOCsAfter.size(), "7 COCs including 1 default");

    // then
    List<MinMaxDataElement> mmdeSourcesAfter =
        minMaxDataElementStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<MinMaxDataElement> mmdeTargetAfter =
        minMaxDataElementStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, mmdeSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        3, mmdeTargetAfter.size(), "Expect 3 entries with target category option combo refs");
  }

  // ----------------------
  // ---- Predictor -----
  // ----------------------
  @Test
  @DisplayName("Predictor refs to source CategoryOptionCombos are replaced, sources deleted")
  void predictorRefsReplacedSourcesDeletedTest() throws ConflictException {
    OrganisationUnitLevel ouLevel = new OrganisationUnitLevel(1, "Level 1");
    manager.save(ouLevel);

    Expression exp1 = new Expression("#{uid00001}", de1.getUid());
    Expression exp2 = new Expression("#{uid00002}", de2.getUid());
    Expression exp3 = new Expression("#{uid00003}", de3.getUid());

    Predictor p1 =
        createPredictor(
            de1,
            cocSource1,
            "1",
            exp1,
            exp1,
            PeriodType.getPeriodTypeByName("Monthly"),
            ouLevel,
            0,
            1,
            1);

    Predictor p2 =
        createPredictor(
            de2,
            cocSource2,
            "2",
            exp2,
            exp2,
            PeriodType.getPeriodTypeByName("Monthly"),
            ouLevel,
            0,
            0,
            0);

    Predictor p3 =
        createPredictor(
            de3,
            cocTarget,
            "3",
            exp3,
            exp3,
            PeriodType.getPeriodTypeByName("Monthly"),
            ouLevel,
            1,
            3,
            2);

    manager.save(List.of(p1, p2, p3));

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");

    List<Predictor> pSourcesBefore =
        predictorStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<Predictor> pTargetBefore =
        predictorStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(
        2, pSourcesBefore.size(), "Expect 2 predictors with source category option combo refs");
    assertEquals(
        1, pTargetBefore.size(), "Expect 1 predictor with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCOCsAfter.size(), "7 COCs including 1 default");

    // then
    List<Predictor> pSourcesAfter =
        predictorStore.getByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<Predictor> pTargetAfter =
        predictorStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, pSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(3, pTargetAfter.size(), "Expect 3 entries with target category option combo refs");
  }

  // --------------------
  // ---- SMSCode -----
  // --------------------
  @Test
  @DisplayName("SMSCode refs to source CategoryOptionCombos are replaced, sources deleted")
  void smsCodeRefsReplacedSourcesDeletedTest() throws ConflictException {
    //    DataElement de1 = createDataElement('1');
    //    DataElement de2 = createDataElement('2');
    //    DataElement de3 = createDataElement('3');
    //    manager.save(List.of(de1, de2, de3));

    SMSCode smsCode1 = new SMSCode();
    smsCode1.setDataElement(de1);
    smsCode1.setOptionId(cocSource1);

    SMSCode smsCode2 = new SMSCode();
    smsCode2.setDataElement(de2);
    smsCode2.setOptionId(cocSource2);

    SMSCode smsCode3 = new SMSCode();
    smsCode3.setDataElement(de3);
    smsCode3.setOptionId(cocTarget);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3));

    smsCommandStore.save(smsCommand);

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");

    List<SMSCode> cSourcesBefore =
        smsCommandStore.getCodesByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<SMSCode> cTargetBefore =
        smsCommandStore.getCodesByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(2, cSourcesBefore.size(), "Expect 2 code with source category option combo refs");
    assertEquals(1, cTargetBefore.size(), "Expect 1 code with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCOCsAfter.size(), "7 COCs including 1 default");

    // then
    List<SMSCode> cSourcesAfter =
        smsCommandStore.getCodesByCategoryOptionCombo(UID.of(cocSource1, cocSource2));
    List<SMSCode> cTargetAfter =
        smsCommandStore.getCodesByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, cSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(3, cTargetAfter.size(), "Expect 3 entries with target category option combo refs");
  }

  // -------------------------------------
  // -- DataValue Category Option Combo --
  // -------------------------------------
  @Test
  @DisplayName(
      "Non-duplicate DataValues with references to source COCs are replaced with target COC using LAST_UPDATED strategy")
  void dataValueMergeCocLastUpdatedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocRandom, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocSource2, cocRandom, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocRandom, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByCatOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC refs");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "Duplicate DataValues with references to source COCs are replaced with target COC using LAST_UPDATED strategy")
  void duplicateDataValueMergeCocLastUpdatedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    // data values have the same (period, orgUnit, coc, aoc) triggering duplicate merge path
    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocRandom, "value1");
    dv1.setLastUpdated(DateUtils.parseDate("2024-6-8"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocSource2, cocRandom, "value2");
    dv2.setLastUpdated(DateUtils.parseDate("2021-6-18"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocTarget, cocRandom, "value3");
    dv3.setLastUpdated(DateUtils.parseDate("2022-4-15"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then there should be no source data values present
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(cocSource1, cocSource2));
    // and only 1 target data value (as 3 duplicates merged using last updated value)
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByCatOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC refs");
    assertEquals(
        DateUtils.parseDate("2024-6-8"),
        targetItems.get(0).getLastUpdated(),
        "It should be the latest lastUpdated value from duplicate data values");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are replaced with target COC using DISCARD strategy")
  void dataValueMergeCocDiscardTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocRandom, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocSource2, cocRandom, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocRandom, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByCatOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are replaced with target COC, source COCs are deleted")
  void dataValueMergeCocSourcesDeletedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocRandom, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocSource2, cocRandom, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocRandom, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByCatOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entries with target COC refs");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  // --------------------------------------
  // -- DataValue Attribute Option Combo --
  // --------------------------------------
  @Test
  @DisplayName(
      "Non-duplicate DataValues with references to source AOCs are replaced with target AOC using LAST_UPDATED strategy")
  void dataValueMergeAocLastUpdatedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocSource1, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocRandom, cocSource2, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocRandom, cocTarget, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC refs");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "Duplicate DataValues with references to source AOCs are replaced with target AOC using LAST_UPDATED strategy")
  void duplicateDataValueAocMergeLastUpdatedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    // data values have the same (period, orgUnit, coc, aoc) triggering duplicate merge path
    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocSource1, "value1");
    dv1.setLastUpdated(DateUtils.parseDate("2024-6-8"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocRandom, cocSource2, "value2");
    dv2.setLastUpdated(DateUtils.parseDate("2021-6-18"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocRandom, cocTarget, "value3");
    dv3.setLastUpdated(DateUtils.parseDate("2022-4-15"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then there should be no source data values present
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(cocSource1, cocSource2));
    // and only 1 target data value (as 3 duplicates merged using last updated value)
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC refs");
    assertEquals(
        DateUtils.parseDate("2024-6-8"),
        targetItems.get(0).getLastUpdated(),
        "It should be the latest lastUpdated value from duplicate data values");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are replaced with target AOC using DISCARD strategy")
  void dataValueMergeAocDiscardTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocSource1, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocRandom, cocSource2, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocRandom, cocTarget, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are replaced with target AOC, source AOCs are deleted")
  void dataValueMergeAocSourcesDeletedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    Period p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocSource1, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocRandom, cocSource2, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocRandom, cocTarget, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataValue> sourceItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(cocSource1, cocSource2));
    List<DataValue> targetItems =
        dataValueStore.getAllDataValuesByAttrOptCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entries with target COC refs");
    assertEquals(7, allCategoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(allCategoryOptionCombos.contains(cocSource2), "Source COC should not be present");
  }

  // ------------------------
  // -- DataValueAudit --
  // ------------------------
  @Test
  @DisplayName(
      "DataValueAudits with references to source DataElements are not changed or deleted when sources not deleted")
  void dataValueAuditMergeTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    DataValueAudit dva1 = createDataValueAudit(cocSource1, "1", p1);
    DataValueAudit dva2 = createDataValueAudit(cocSource1, "2", p1);
    DataValueAudit dva3 = createDataValueAudit(cocSource2, "1", p1);
    DataValueAudit dva4 = createDataValueAudit(cocSource2, "2", p1);
    DataValueAudit dva5 = createDataValueAudit(cocTarget, "1", p1);

    dataValueAuditStore.addDataValueAudit(dva1);
    dataValueAuditStore.addDataValueAudit(dva2);
    dataValueAuditStore.addDataValueAudit(dva3);
    dataValueAuditStore.addDataValueAudit(dva4);
    dataValueAuditStore.addDataValueAudit(dva5);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(false);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    DataValueAuditQueryParams source1DvaQueryParams = getQueryParams(cocSource1);
    DataValueAuditQueryParams source2DvaQueryParams = getQueryParams(cocSource2);
    DataValueAuditQueryParams targetDvaQueryParams = getQueryParams(cocTarget);

    List<DataValueAudit> source1Audits =
        dataValueAuditStore.getDataValueAudits(source1DvaQueryParams);
    List<DataValueAudit> source2Audits =
        dataValueAuditStore.getDataValueAudits(source2DvaQueryParams);

    List<DataValueAudit> targetItems = dataValueAuditStore.getDataValueAudits(targetDvaQueryParams);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        4, source1Audits.size() + source2Audits.size(), "Expect 4 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref");
  }

  @Test
  @DisplayName(
      "DataValueAudits with references to source DataElements are deleted when sources are deleted")
  void dataValueAuditMergeDeleteTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    DataValueAudit dva1 = createDataValueAudit(cocSource1, "1", p1);
    DataValueAudit dva2 = createDataValueAudit(cocSource1, "2", p1);
    DataValueAudit dva3 = createDataValueAudit(cocSource2, "1", p1);
    DataValueAudit dva4 = createDataValueAudit(cocSource2, "2", p1);
    DataValueAudit dva5 = createDataValueAudit(cocTarget, "1", p1);

    dataValueAuditStore.addDataValueAudit(dva1);
    dataValueAuditStore.addDataValueAudit(dva2);
    dataValueAuditStore.addDataValueAudit(dva3);
    dataValueAuditStore.addDataValueAudit(dva4);
    dataValueAuditStore.addDataValueAudit(dva5);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    DataValueAuditQueryParams source1DvaQueryParams = getQueryParams(cocSource1);
    DataValueAuditQueryParams source2DvaQueryParams = getQueryParams(cocSource2);
    DataValueAuditQueryParams targetDvaQueryParams = getQueryParams(cocTarget);

    List<DataValueAudit> source1Audits =
        dataValueAuditStore.getDataValueAudits(source1DvaQueryParams);
    List<DataValueAudit> source2Audits =
        dataValueAuditStore.getDataValueAudits(source2DvaQueryParams);

    List<DataValueAudit> targetItems = dataValueAuditStore.getDataValueAudits(targetDvaQueryParams);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, source1Audits.size() + source2Audits.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref");
  }

  // ------------------------
  // -- DataApprovalAudit --
  // ------------------------
  @Test
  @DisplayName(
      "DataApprovalAudits with references to source DataElements are not changed or deleted when sources not deleted")
  void dataApprovalAuditMergeTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    Period p2 = createPeriod(DateUtils.parseDate("2023-1-4"), DateUtils.parseDate("2023-1-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p2);

    DataApprovalWorkflow daw = new DataApprovalWorkflow();
    daw.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw.setName("DAW");
    daw.setCategoryCombo(cc1);
    manager.save(daw);

    DataApprovalAudit daa1 = createDataApprovalAudit(cocSource1, level1, daw, p1);
    DataApprovalAudit daa2 = createDataApprovalAudit(cocSource1, level2, daw, p2);
    DataApprovalAudit daa3 = createDataApprovalAudit(cocSource2, level1, daw, p1);
    DataApprovalAudit daa4 = createDataApprovalAudit(cocSource2, level2, daw, p2);
    DataApprovalAudit daa5 = createDataApprovalAudit(cocTarget, level1, daw, p1);

    dataApprovalAuditStore.save(daa1);
    dataApprovalAuditStore.save(daa2);
    dataApprovalAuditStore.save(daa3);
    dataApprovalAuditStore.save(daa4);
    dataApprovalAuditStore.save(daa5);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(false);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    DataApprovalAuditQueryParams targetDaaQueryParams =
        new DataApprovalAuditQueryParams()
            .setAttributeOptionCombos(new HashSet<>(Collections.singletonList(cocTarget)))
            .setLevels(Set.of(level1));

    List<DataApprovalAudit> sourceAudits = dataApprovalAuditStore.getAll();
    List<DataApprovalAudit> targetItems =
        dataApprovalAuditStore.getDataApprovalAudits(targetDaaQueryParams);

    assertFalse(report.hasErrorMessages());
    assertEquals(5, sourceAudits.size(), "Expect 4 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref");
  }

  @Test
  @DisplayName(
      "DataApprovalAudits with references to source DataElements are deleted when sources are deleted")
  void dataApprovalAuditMergeDeleteTest() throws ConflictException {
    // given
    DataApprovalLevel dataApprovalLevel = new DataApprovalLevel();
    dataApprovalLevel.setLevel(1);
    dataApprovalLevel.setName("DAL");
    manager.save(dataApprovalLevel);

    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    DataApprovalWorkflow daw = new DataApprovalWorkflow();
    daw.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw.setName("DAW");
    daw.setCategoryCombo(cc1);
    manager.save(daw);

    DataApprovalAudit daa1 = createDataApprovalAudit(cocSource1, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa2 = createDataApprovalAudit(cocSource1, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa3 = createDataApprovalAudit(cocSource2, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa4 = createDataApprovalAudit(cocSource2, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa5 = createDataApprovalAudit(cocTarget, dataApprovalLevel, daw, p1);

    dataApprovalAuditStore.save(daa1);
    dataApprovalAuditStore.save(daa2);
    dataApprovalAuditStore.save(daa3);
    dataApprovalAuditStore.save(daa4);
    dataApprovalAuditStore.save(daa5);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    DataApprovalAuditQueryParams source1DaaQueryParams =
        new DataApprovalAuditQueryParams()
            .setAttributeOptionCombos(new HashSet<>(Arrays.asList(cocSource1, cocSource2)));
    DataApprovalAuditQueryParams targetDaaQueryParams =
        new DataApprovalAuditQueryParams()
            .setAttributeOptionCombos(new HashSet<>(Collections.singletonList(cocTarget)));

    List<DataApprovalAudit> sourceAudits =
        dataApprovalAuditStore.getDataApprovalAudits(source1DaaQueryParams);
    List<DataApprovalAudit> targetItems =
        dataApprovalAuditStore.getDataApprovalAudits(targetDaaQueryParams);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceAudits.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref");
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(cocSource1.getUid(), cocSource2.getUid())));
    mergeParams.setTarget(UID.of(cocTarget.getUid()));
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);
    mergeParams.setDeleteSources(true);
    return mergeParams;
  }

  private CategoryOptionCombo getCocWithOptions(String co1, String co2) {
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    return allCategoryOptionCombos.stream()
        .filter(
            coc -> {
              List<String> categoryOptions =
                  coc.getCategoryOptions().stream().map(BaseIdentifiableObject::getName).toList();
              return categoryOptions.containsAll(List.of(co1, co2));
            })
        .toList()
        .get(0);
  }

  private DataValueAudit createDataValueAudit(CategoryOptionCombo coc, String value, Period p) {
    DataValueAudit dva = new DataValueAudit();
    dva.setDataElement(de1);
    dva.setValue(value);
    dva.setAuditType(ChangeLogType.CREATE);
    dva.setCreated(new Date());
    dva.setCategoryOptionCombo(coc);
    dva.setAttributeOptionCombo(coc);
    dva.setPeriod(p);
    dva.setOrganisationUnit(ou1);
    return dva;
  }

  private DataApprovalAudit createDataApprovalAudit(
      CategoryOptionCombo coc, DataApprovalLevel level, DataApprovalWorkflow workflow, Period p) {
    DataApprovalAudit daa = new DataApprovalAudit();
    daa.setAttributeOptionCombo(coc);
    daa.setOrganisationUnit(ou1);
    daa.setLevel(level);
    daa.setWorkflow(workflow);
    daa.setPeriod(p);
    daa.setAction(APPROVE);
    daa.setCreated(new Date());
    daa.setCreator(getCurrentUser());
    return daa;
  }

  private DataValueAuditQueryParams getQueryParams(CategoryOptionCombo coc) {
    return new DataValueAuditQueryParams().setCategoryOptionCombo(coc);
  }
}
