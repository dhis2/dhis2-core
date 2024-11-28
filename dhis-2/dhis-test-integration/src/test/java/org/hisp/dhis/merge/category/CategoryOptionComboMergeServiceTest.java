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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.datavalue.DataValue;
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
// @ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
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
    //    DataElement de1 = createDataElement('1');
    //    DataElement de2 = createDataElement('2');
    //    DataElement de3 = createDataElement('3');
    manager.save(ouLevel);

    Expression exp1 = new Expression("#{uid00001}", de1.getUid());
    Expression exp2 = new Expression("#{uid00002}", de2.getUid());
    Expression exp3 = new Expression("#{uid00003}", de3.getUid());
    Expression exp4 = new Expression("#{uid00004}", de1.getUid());

    Predictor p1 =
        createPredictor(
            de1,
            cocSource1,
            "1",
            exp1,
            exp2,
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
            exp3,
            exp4,
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
            exp1,
            exp4,
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

  // ------------------------
  // -- DataValue --
  // ------------------------
  @Test
  @DisplayName(
      "Non-duplicate DataValues with references to source COCs are replaced with target COC using LAST_UPDATED strategy")
  void dataValueMergeLastUpdatedTest() throws ConflictException {
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

    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocTarget, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocSource2, cocTarget, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocTarget, "value3");

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
  void duplicateDataValueMergeLastUpdatedTest() throws ConflictException {
    // given
    Period p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);

    // data values have the same (period, orgUnit, coc, aoc) triggering duplicate merge path
    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocSource1, "value1");
    dv1.setLastUpdated(DateUtils.parseDate("2024-6-8"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocSource2, cocSource1, "value2");
    dv2.setLastUpdated(DateUtils.parseDate("2021-6-18"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocTarget, cocSource1, "value3");
    dv3.setLastUpdated(DateUtils.parseDate("2022-4-15"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

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
    assertEquals(9, allCategoryOptionCombos.size(), "Expect 9 COCs present");
    assertTrue(allCategoryOptionCombos.containsAll(List.of(cocTarget, cocSource1, cocSource2)));
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are replaced with target COC using DISCARD strategy")
  void dataValueMergeDiscardTest() throws ConflictException {
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

    DataValue dv1 = createDataValue(de1, p1, ou1, cocSource1, cocTarget, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocSource2, cocTarget, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocTarget, "value3");

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
    assertTrue(allCategoryOptionCombos.contains(cocTarget));
    assertFalse(allCategoryOptionCombos.containsAll(List.of(cocSource1, cocSource2)));
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are replaced with target COC, source COCs are deleted")
  void dataValueMergeSourcesDeletedTest() throws ConflictException {
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

    DataValue dv1 = createDataValue(de1, p1, ou1, "value1", cocSource1);
    DataValue dv2 = createDataValue(de2, p2, ou1, "value2", cocSource2);
    DataValue dv3 = createDataValue(de3, p3, ou1, "value3", cocTarget);

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

    assertMergeSuccessfulSourcesDeleted(report, sourceItems, targetItems, allCategoryOptionCombos);
  }

  private void assertMergeSuccessfulSourcesNotDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<CategoryOptionCombo> categoryOptionCombos) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, target.size(), "Expect 3 entries with target COC refs");
    assertEquals(4, categoryOptionCombos.size(), "Expect 4 COC present");
    assertTrue(categoryOptionCombos.containsAll(List.of(cocTarget, cocSource1, cocSource2)));
  }

  private void assertMergeSuccessfulSourcesDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<CategoryOptionCombo> categoryOptionCombos) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, target.size(), "Expect 1 entries with target COC refs");
    assertEquals(7, categoryOptionCombos.size(), "Expect 7 COCs present");
    assertTrue(categoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(categoryOptionCombos.contains(cocSource1), "Source COC should not be present");
    assertFalse(categoryOptionCombos.contains(cocSource2), "Source COC should not be present");
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
}
