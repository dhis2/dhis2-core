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
package org.hisp.dhis.merge.category;

import static org.hisp.dhis.dataapproval.DataApprovalAction.APPROVE;
import static org.hisp.dhis.feedback.ErrorCode.E1540;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditStore;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalStore;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
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
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
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
 * <p>- Perform a CategoryOptionCombo merge, passing a target CategoryOptionCombo
 *
 * <p>- Check that source CategoryOptionCombos have had their references removed/replaced with the
 * target CategoryOptionCombo
 */
@Transactional
class CategoryOptionComboMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;
  @Autowired private CategoryOptionComboStore categoryOptionComboStore;
  @Autowired private DataElementOperandStore dataElementOperandStore;
  @Autowired private MinMaxDataElementStore minMaxDataElementStore;
  @Autowired private PredictorStore predictorStore;
  @Autowired private SMSCommandStore smsCommandStore;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryOptionComboMergeService;
  @Autowired private PeriodService periodService;
  @Autowired private DataValueStore dataValueStore;
  @Autowired private CompleteDataSetRegistrationStore completeDataSetRegistrationStore;
  @Autowired private DataValueAuditStore dataValueAuditStore;
  @Autowired private DataApprovalAuditStore dataApprovalAuditStore;
  @Autowired private DataApprovalStore dataApprovalStore;
  @Autowired private EventStore eventStore;
  @Autowired private IndicatorService indicatorService;
  @Autowired private DbmsManager dbmsManager;
  @Autowired private ExpressionService expressionService;

  private TestCategoryMetadata categoryMetadata;
  private CategoryOptionCombo cocTarget;
  private CategoryOptionCombo cocRandom;
  private CategoryOptionCombo cocDuplicate;
  private CategoryOptionCombo cocDuplicate2;
  private static int catCounter;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private DataElement de1;
  private DataElement de2;
  private DataElement de3;
  private Program program;
  private Period p1;
  private Period p2;
  private Period p3;

  @BeforeEach
  public void setUp() {
    categoryMetadata = setupCategoryMetadata("cocm " + ++catCounter);
    cocTarget = categoryMetadata.coc3();
    cocRandom = categoryMetadata.coc4();

    cocDuplicate = new CategoryOptionCombo();
    cocDuplicate.setCategoryCombo(cocTarget.getCategoryCombo());
    cocDuplicate.setCategoryOptions(new HashSet<>(cocTarget.getCategoryOptions()));
    manager.save(cocDuplicate);

    cocDuplicate2 = new CategoryOptionCombo();
    cocDuplicate2.setCategoryCombo(cocTarget.getCategoryCombo());
    cocDuplicate2.setCategoryOptions(new HashSet<>(cocTarget.getCategoryOptions()));
    manager.save(cocDuplicate2);

    ou1 = createOrganisationUnit('A');
    ou2 = createOrganisationUnit('B');
    ou3 = createOrganisationUnit('C');
    manager.save(List.of(ou1, ou2, ou3));

    de1 = createDataElement('1');
    de2 = createDataElement('2');
    de3 = createDataElement('3');
    manager.save(List.of(de1, de2, de3));

    program = createProgram('q');
    manager.save(program);

    p1 = createPeriod(DateUtils.parseDate("2024-1-4"), DateUtils.parseDate("2024-1-4"));
    p1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    p2 = createPeriod(DateUtils.parseDate("2024-2-4"), DateUtils.parseDate("2024-2-4"));
    p2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    p3 = createPeriod(DateUtils.parseDate("2024-3-4"), DateUtils.parseDate("2024-3-4"));
    p3.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    periodService.addPeriod(p1);
    periodService.addPeriod(p2);
    periodService.addPeriod(p3);

    dbmsManager.clearSession();
  }

  @Test
  @DisplayName("Merging non-duplicate COCs results in an error")
  void nonDuplicateResultsInErrorTest() {
    MergeParams mergeParams = getMergeParams();
    mergeParams.setSources(Set.of(UID.of(cocRandom)));

    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> categoryOptionComboMergeService.processMerge(mergeParams));

    assertTrue(
        conflictException.getMergeReport().getMergeErrors().stream()
            .map(ErrorMessage::getMessage)
            .collect(Collectors.toSet())
            .contains(E1540.getMessage()));
  }

  // -------------------------
  // ------ Indicators -------
  // -------------------------
  @Test
  @DisplayName("Indicator numerator denominators are updated during merge")
  void indicatorMergeTest() throws ConflictException {
    // given
    IndicatorType indType = createIndicatorType('c');
    manager.save(indType);
    Indicator i1 = createIndicator('a', indType);
    i1.setDimensionItemType(DimensionItemType.INDICATOR);
    i1.setNumerator("#{%s.RanDOmUID01}".formatted(cocDuplicate.getUid()));

    Indicator i2 = createIndicator('b', indType);
    i2.setDimensionItemType(DimensionItemType.INDICATOR);
    i2.setDenominator("#{RanDOmUID01.%s}".formatted(cocDuplicate.getUid()));

    Indicator i3 = createIndicator('c', indType);
    i3.setDimensionItemType(DimensionItemType.INDICATOR);
    i3.setNumerator("#{%s.RanDOmUID01}".formatted(cocDuplicate.getUid()));
    i3.setDenominator(
        "#{%s.RanDOmUID01}.%s".formatted(cocDuplicate.getUid(), cocDuplicate.getUid()));

    Indicator i4 = createIndicator('d', indType);
    i4.setDimensionItemType(DimensionItemType.INDICATOR);
    i4.setNumerator("#{%s.RanDOmUID01}".formatted(cocTarget.getUid()));

    manager.save(List.of(i1, i2, i3, i4));
    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());
    Indicator ind1 = indicatorService.getIndicator(i1.getUid());
    assertEquals("#{%s.RanDOmUID01}".formatted(cocTarget.getUid()), ind1.getNumerator());

    Indicator ind2 = indicatorService.getIndicator(i2.getUid());
    assertEquals("#{RanDOmUID01.%s}".formatted(cocTarget.getUid()), ind2.getDenominator());

    Indicator ind3 = indicatorService.getIndicator(i3.getUid());
    assertEquals("#{%s.RanDOmUID01}".formatted(cocTarget.getUid()), ind3.getNumerator());
    assertEquals(
        "#{%s.RanDOmUID01}.%s".formatted(cocTarget.getUid(), cocTarget.getUid()),
        ind3.getDenominator());

    Indicator ind4 = indicatorService.getIndicator(i4.getUid());
    assertEquals("#{%s.RanDOmUID01}".formatted(cocTarget.getUid()), ind4.getNumerator());
  }

  // -------------------------
  // ------ Expression -------
  // -------------------------
  @Test
  @DisplayName("Expressions have any source ref replaced with target during merge")
  void expressionMergeTest() throws ConflictException {
    // given
    Expression e1 = createExpression2('a', "#{%s.RanDOmUID01}".formatted(cocDuplicate.getUid()));
    Expression e2 =
        createExpression2(
            'b', "#{%s.RanDOmUID01}.%s".formatted(cocDuplicate.getUid(), cocDuplicate.getUid()));
    Expression e3 = createExpression2('c', "#{RanDOmUID01}");
    Expression e4 = createExpression2('d', "#{RanDOmUID01}.%s".formatted(cocTarget.getUid()));

    long e1Id = expressionService.addExpression(e1);
    long e2Id = expressionService.addExpression(e2);
    long e3Id = expressionService.addExpression(e3);
    long e4Id = expressionService.addExpression(e4);
    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    assertFalse(report.hasErrorMessages());
    Expression exp1 = expressionService.getExpression(e1Id);
    assertEquals("#{%s.RanDOmUID01}".formatted(cocTarget.getUid()), exp1.getExpression());
    Expression exp2 = expressionService.getExpression(e2Id);
    assertEquals(
        "#{%s.RanDOmUID01}.%s".formatted(cocTarget.getUid(), cocTarget.getUid()),
        exp2.getExpression());
    Expression exp3 = expressionService.getExpression(e3Id);
    assertEquals("#{RanDOmUID01}", exp3.getExpression());
    Expression exp4 = expressionService.getExpression(e4Id);
    assertEquals("#{RanDOmUID01}.%s".formatted(cocTarget.getUid()), exp4.getExpression());
  }

  // -----------------------------
  // ------ CategoryOption -------
  // -----------------------------
  @Test
  @DisplayName("CategoryOption refs to source CategoryOptionCombos are replaced")
  void categoryOptionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category option combo state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");
    assertEquals(5, allCategoryOptions.size(), "5 COs including 1 default");

    long coSourcesBefore =
        categoryOptionComboStore.getAll().stream()
            .filter(coc -> coc.getId() == cocDuplicate.getId())
            .mapToLong(coc -> coc.getCategoryOptions().size())
            .sum();
    long coTargetBefore =
        categoryOptionComboStore.getAll().stream()
            .filter(coc -> coc.getId() == cocTarget.getId())
            .mapToLong(coc -> coc.getCategoryOptions().size())
            .sum();

    assertEquals(
        2, coSourcesBefore, "Expect 2 category options with source category option combo refs");
    assertEquals(
        2, coTargetBefore, "Expect 2 category options with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    entityManager.flush();

    // then
    long coSourcesAfter =
        categoryOptionComboStore.getAll().stream()
            .filter(coc -> coc.getId() == cocDuplicate.getId())
            .mapToLong(coc -> coc.getCategoryOptions().size())
            .sum();
    long coTargetAfter =
        categoryOptionComboStore.getAll().stream()
            .filter(coc -> coc.getId() == cocTarget.getId())
            .mapToLong(coc -> coc.getCategoryOptions().size())
            .sum();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, coSourcesAfter, "Expect 0 entries with source category option combo refs");
    assertEquals(2, coTargetAfter, "Expect 2 entries with target category option combo refs");

    assertTrue(
        categoryService
            .getCategoryOptionCombosByUid(List.of(UID.of(cocDuplicate.getUid())))
            .isEmpty(),
        "There should be no source COCs after deletion during merge");

    assertCocCountAfterAutoGenerate(5);
  }

  private void assertCocCountAfterAutoGenerate(int expectedCocCount) {
    entityManager.flush();
    entityManager.clear();
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();
    assertEquals(expectedCocCount, allCategoryOptionCombos.size());
  }

  // -----------------------------
  // ------ CategoryCombo -------
  // -----------------------------
  @Test
  @DisplayName("CategoryCombo refs to source CategoryOptionCombos are replaced")
  void categoryComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category option combo state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();
    List<CategoryCombo> allCategoryCombos = categoryService.getAllCategoryCombos();

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");
    assertEquals(2, allCategoryCombos.size(), "2 CCs including 1 default");

    long ccSourcesBefore =
        categoryService.getAllCategoryOptionCombos().stream()
            .filter(coc -> coc.getId() == cocDuplicate.getId())
            .filter(coc -> coc.getCategoryCombo().getId() == categoryMetadata.cc1().getId())
            .count();
    long ccTargetBefore =
        categoryService.getAllCategoryOptionCombos().stream()
            .filter(coc -> coc.getId() == cocTarget.getId())
            .filter(coc -> coc.getCategoryCombo().getId() == categoryMetadata.cc1().getId())
            .count();

    assertEquals(
        1, ccSourcesBefore, "Expect 1 category combo with source category option combo refs");
    assertEquals(
        1, ccTargetBefore, "Expect 1 category combo with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();
    List<CategoryCombo> allCCsAfter = categoryService.getAllCategoryCombos();

    assertEquals(5, allCOCsAfter.size(), "5 COCs including 1 default");
    assertEquals(2, allCCsAfter.size(), "2 CCs including 1 default");

    long ccSourcesAfter =
        allCOCsAfter.stream()
            .filter(coc -> coc.getId() == cocDuplicate.getId())
            .filter(coc -> coc.getCategoryCombo().getId() == categoryMetadata.cc1().getId())
            .count();
    long ccTargetAfter =
        allCOCsAfter.stream()
            .filter(coc -> coc.getId() == cocTarget.getId())
            .filter(coc -> coc.getCategoryCombo().getId() == categoryMetadata.cc1().getId())
            .count();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, ccSourcesAfter, "Expect 0 entries with source category option combo refs");
    assertEquals(1, ccTargetAfter, "Expect 1 entry with target category option combo refs");

    assertTrue(
        categoryService.getCategoryOptionCombosByUid(List.of(UID.of(cocDuplicate))).isEmpty(),
        "There should be no source COCs after deletion during merge");
    assertCocCountAfterAutoGenerate(5);
  }

  // -----------------------------
  // ---- DataElementOperand -----
  // -----------------------------
  @Test
  @DisplayName("DataElementOperand refs to source CategoryOptionCombos are replaced")
  void dataElementOperandRefsReplacedSourcesDeletedTest() throws ConflictException {
    DataElementOperand deo1 = new DataElementOperand();
    deo1.setDataElement(de1);
    deo1.setCategoryOptionCombo(cocDuplicate);

    DataElementOperand deo2 = new DataElementOperand();
    deo2.setDataElement(de2);
    deo2.setCategoryOptionCombo(cocDuplicate);

    DataElementOperand deo3 = new DataElementOperand();
    deo3.setDataElement(de3);
    deo3.setCategoryOptionCombo(cocTarget);

    manager.save(List.of(de1, de2, de3));
    manager.save(List.of(deo1, deo2, deo3));

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");

    List<DataElementOperand> deoSourcesBefore =
        dataElementOperandStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
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

    // then
    assertEquals(5, allCOCsAfter.size(), "5 COCs including 1 default");
    assertFalse(report.hasErrorMessages(), "there should be no merge errors");
    List<DataElementOperand> deoSourcesAfter =
        dataElementOperandStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataElementOperand> deoTargetAfter =
        dataElementOperandStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));
    assertEquals(
        0, deoSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        3, deoTargetAfter.size(), "Expect 3 entries with target category option combo refs");
    assertCocCountAfterAutoGenerate(5);
  }

  // -----------------------------
  // ---- MinMaxDataElement -----
  // -----------------------------
  @Test
  @DisplayName("MinMaxDataElement refs to source CategoryOptionCombos are replaced")
  void minMaxDataElementRefsReplacedSourcesDeletedTest() throws ConflictException {
    OrganisationUnit ou1 = createOrganisationUnit('1');
    OrganisationUnit ou2 = createOrganisationUnit('2');
    OrganisationUnit ou3 = createOrganisationUnit('3');
    manager.save(List.of(ou1, ou2, ou3));

    MinMaxDataElement mmde1 = new MinMaxDataElement(de1, ou1, cocDuplicate, 0, 100, false);
    MinMaxDataElement mmde2 = new MinMaxDataElement(de2, ou2, cocDuplicate, 0, 100, false);
    MinMaxDataElement mmde3 = new MinMaxDataElement(de3, ou3, cocTarget, 0, 100, false);
    minMaxDataElementStore.save(mmde1);
    minMaxDataElementStore.save(mmde2);
    minMaxDataElementStore.save(mmde3);

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");

    List<MinMaxDataElement> mmdeSourcesBefore =
        minMaxDataElementStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
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

    assertEquals(5, allCOCsAfter.size(), "5 COCs including 1 default");

    // then
    List<MinMaxDataElement> mmdeSourcesAfter =
        minMaxDataElementStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<MinMaxDataElement> mmdeTargetAfter =
        minMaxDataElementStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, mmdeSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(
        3, mmdeTargetAfter.size(), "Expect 3 entries with target category option combo refs");
    assertCocCountAfterAutoGenerate(5);
  }

  // ----------------------
  // ---- Predictor -----
  // ----------------------
  @Test
  @DisplayName("Predictor refs to source CategoryOptionCombos are replaced")
  void predictorRefsReplacedSourcesDeletedTest() throws ConflictException {
    OrganisationUnitLevel ouLevel = new OrganisationUnitLevel(1, "Level 1");
    manager.save(ouLevel);

    Expression exp1 = new Expression("#{uid00001}", de1.getUid());
    Expression exp2 = new Expression("#{uid00002}", de2.getUid());
    Expression exp3 = new Expression("#{uid00003}", de3.getUid());

    Predictor p1 =
        createPredictor(
            de1,
            cocDuplicate,
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
            cocDuplicate,
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

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");

    List<Predictor> pSourcesBefore =
        predictorStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<Predictor> pTargetBefore =
        predictorStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(
        2, pSourcesBefore.size(), "Expect 2 predictors with source category option combo refs");
    assertEquals(
        1, pTargetBefore.size(), "Expect 1 predictor with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(5, allCOCsAfter.size(), "5 COCs including 1 default");

    // then
    List<Predictor> pSourcesAfter =
        predictorStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<Predictor> pTargetAfter =
        predictorStore.getByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, pSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(3, pTargetAfter.size(), "Expect 3 entries with target category option combo refs");
    assertCocCountAfterAutoGenerate(5);
  }

  // --------------------
  // ---- SMSCode -----
  // --------------------
  @Test
  @DisplayName("SMSCode refs to source CategoryOptionCombos are replaced")
  void smsCodeRefsReplacedSourcesDeletedTest() throws ConflictException {
    SMSCode smsCode1 = new SMSCode();
    smsCode1.setDataElement(de1);
    smsCode1.setOptionId(cocDuplicate);

    SMSCode smsCode2 = new SMSCode();
    smsCode2.setDataElement(de2);
    smsCode2.setOptionId(cocDuplicate);

    SMSCode smsCode3 = new SMSCode();
    smsCode3.setDataElement(de3);
    smsCode3.setOptionId(cocTarget);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3));

    smsCommandStore.save(smsCommand);

    // given state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(7, allCategoryOptionCombos.size(), "7 COCs including 1 default");

    List<SMSCode> cSourcesBefore =
        smsCommandStore.getCodesByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<SMSCode> cTargetBefore =
        smsCommandStore.getCodesByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertEquals(2, cSourcesBefore.size(), "Expect 2 code with source category option combo refs");
    assertEquals(1, cTargetBefore.size(), "Expect 1 code with target category option combo refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    List<CategoryOptionCombo> allCOCsAfter = categoryService.getAllCategoryOptionCombos();

    assertEquals(5, allCOCsAfter.size(), "5 COCs including 1 default");

    // then
    List<SMSCode> cSourcesAfter =
        smsCommandStore.getCodesByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<SMSCode> cTargetAfter =
        smsCommandStore.getCodesByCategoryOptionCombo(Set.of(UID.of(cocTarget.getUid())));

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, cSourcesAfter.size(), "Expect 0 entries with source category option combo refs");
    assertEquals(3, cTargetAfter.size(), "Expect 3 entries with target category option combo refs");
    assertCocCountAfterAutoGenerate(5);
  }

  // -------------------------------------
  // -- DataValue Category Option Combo --
  // -------------------------------------
  @Test
  @DisplayName("DataValues with references to source COCs are deleted using DISCARD strategy")
  void dataValueMergeCocDiscardTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocDuplicate, cocRandom, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocDuplicate2, cocRandom, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocRandom, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);
    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDvs = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDvs.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getCategoryOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDvs.stream()
            .filter(dv -> dv.getCategoryOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocTarget.getUid()),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are merged using LAST_UPDATED strategy, no duplicates")
  void dataValueMergeCocLastUpdatedTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocDuplicate, cocRandom, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocDuplicate, cocRandom, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocTarget, cocRandom, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);
    dbmsManager.clearSession();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getCategoryOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getCategoryOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are merged using LAST_UPDATED strategy, 1 source duplicate kept")
  void dataValueMergeCocLastUpdated1SourceKeptTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocDuplicate, cocRandom, "source value 1");
    dv1.setLastUpdated(DateUtils.parseDate("2025-01-10"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocDuplicate2, cocRandom, "source value 2");
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-11"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocTarget, cocRandom, "target value 3");
    dv3.setLastUpdated(DateUtils.parseDate("2024-01-04"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getCategoryOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getCategoryOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("source value 2"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are merged using LAST_UPDATED strategy, 1 target duplicate kept")
  void dataValueMergeCocLastUpdated1TargetTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocDuplicate, cocRandom, "source value 1");
    dv1.setLastUpdated(DateUtils.parseDate("2024-01-14"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocDuplicate2, cocRandom, "source value 2");
    dv2.setLastUpdated(DateUtils.parseDate("2024-01-24"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocTarget, cocRandom, "target value 3");
    dv3.setLastUpdated(DateUtils.parseDate("2025-01-04"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getCategoryOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getCategoryOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("target value 3"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source COCs are merged using LAST_UPDATED strategy, mix of duplicates kept")
  void dataValueMergeCocLastUpdatedMixTest() throws ConflictException {
    // given
    DataValue dv1a = createDataValue(de1, p1, ou1, cocDuplicate, cocRandom, "source value 1a");
    dv1a.setLastUpdated(DateUtils.parseDate("2024-01-14"));
    DataValue dv1b = createDataValue(de2, p1, ou1, cocDuplicate, cocRandom, "keep source value 1b");
    dv1b.setLastUpdated(DateUtils.parseDate("2025-01-14"));
    DataValue dv2a =
        createDataValue(de1, p1, ou1, cocDuplicate2, cocRandom, "keep source value 2a");
    dv2a.setLastUpdated(DateUtils.parseDate("2025-01-24"));
    DataValue dv2b = createDataValue(de2, p1, ou1, cocDuplicate2, cocRandom, "source value 2b");
    dv2b.setLastUpdated(DateUtils.parseDate("2024-01-24"));
    DataValue dv3a = createDataValue(de1, p1, ou1, cocTarget, cocRandom, "target value 3a");
    dv3a.setLastUpdated(DateUtils.parseDate("2024-01-04"));
    DataValue dv3b = createDataValue(de3, p1, ou1, cocTarget, cocRandom, "keep target value 3b");
    dv3b.setLastUpdated(DateUtils.parseDate("2025-01-04"));

    dataValueStore.addDataValue(dv1a);
    dataValueStore.addDataValue(dv1b);
    dataValueStore.addDataValue(dv2a);
    dataValueStore.addDataValue(dv2b);
    dataValueStore.addDataValue(dv3a);
    dataValueStore.addDataValue(dv3b);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getCategoryOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getCategoryOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("keep source value 1b", "keep source value 2a", "keep target value 3b"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  // -------------------------------------
  // -- DataValue Attribute Option Combo --
  // -------------------------------------
  @Test
  @DisplayName("DataValues with references to source AOCs are deleted using DISCARD strategy")
  void dataValueMergeAocDiscardTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocRandom, cocDuplicate2, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocRandom, cocTarget, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);
    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDvs = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDvs.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getAttributeOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDvs.stream()
            .filter(dv -> dv.getAttributeOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocTarget.getUid()),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are merged using LAST_UPDATED strategy, no duplicates")
  void dataValueMergeAocLastUpdatedTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate, "value1");
    DataValue dv2 = createDataValue(de2, p2, ou1, cocRandom, cocDuplicate, "value2");
    DataValue dv3 = createDataValue(de3, p3, ou1, cocRandom, cocTarget, "value3");

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // params
    MergeParams mergeParams = getMergeParams();
    dbmsManager.clearSession();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getAttributeOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getAttributeOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are merged using LAST_UPDATED strategy, 1 source duplicate kept")
  void dataValueMergeAocLastUpdated1SourceKeptTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate, "source value 1");
    dv1.setLastUpdated(DateUtils.parseDate("2025-01-10"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate2, "source value 2");
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-11"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocRandom, cocTarget, "target value 3");
    dv3.setLastUpdated(DateUtils.parseDate("2024-01-04"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getAttributeOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getAttributeOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("source value 2"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are merged using LAST_UPDATED strategy, 1 target duplicate kept")
  void dataValueMergeAocLastUpdated1TargetTest() throws ConflictException {
    // given
    DataValue dv1 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate, "source value 1");
    dv1.setLastUpdated(DateUtils.parseDate("2024-01-14"));
    DataValue dv2 = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate2, "source value 2");
    dv2.setLastUpdated(DateUtils.parseDate("2024-01-24"));
    DataValue dv3 = createDataValue(de1, p1, ou1, cocRandom, cocTarget, "target value 3");
    dv3.setLastUpdated(DateUtils.parseDate("2025-01-04"));

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getAttributeOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getAttributeOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("target value 3"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertTrue(
        Collections.disjoint(
            allCategoryOptionCombos.stream()
                .map(IdentifiableObject::getUid)
                .collect(Collectors.toSet()),
            Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataValues with references to source AOCs are merged using LAST_UPDATED strategy, mix of duplicates kept")
  void dataValueMergeAocLastUpdatedMixTest() throws ConflictException {
    // given
    DataValue dv1a = createDataValue(de1, p1, ou1, cocRandom, cocDuplicate, "source value 1a");
    dv1a.setLastUpdated(DateUtils.parseDate("2024-01-14"));
    DataValue dv1b = createDataValue(de2, p1, ou1, cocRandom, cocDuplicate, "keep source value 1b");
    dv1b.setLastUpdated(DateUtils.parseDate("2025-01-14"));
    DataValue dv2a =
        createDataValue(de1, p1, ou1, cocRandom, cocDuplicate2, "keep source value 2a");
    dv2a.setLastUpdated(DateUtils.parseDate("2025-01-24"));
    DataValue dv2b = createDataValue(de2, p1, ou1, cocRandom, cocDuplicate2, "source value 2b");
    dv2b.setLastUpdated(DateUtils.parseDate("2024-01-24"));
    DataValue dv3a = createDataValue(de1, p1, ou1, cocRandom, cocTarget, "target value 3a");
    dv3a.setLastUpdated(DateUtils.parseDate("2024-01-04"));
    DataValue dv3b = createDataValue(de3, p1, ou1, cocRandom, cocTarget, "keep target value 3b");
    dv3b.setLastUpdated(DateUtils.parseDate("2025-01-04"));

    dataValueStore.addDataValue(dv1a);
    dataValueStore.addDataValue(dv1b);
    dataValueStore.addDataValue(dv2a);
    dataValueStore.addDataValue(dv2b);
    dataValueStore.addDataValue(dv3a);
    dataValueStore.addDataValue(dv3b);

    dbmsManager.clearSession();

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    List<DataValue> sourceItems =
        allDataValues.stream()
            .filter(
                dv ->
                    Set.of(cocDuplicate.getUid(), cocDuplicate2.getUid())
                        .contains(dv.getAttributeOptionCombo().getUid()))
            .toList();
    List<DataValue> targetItems =
        allDataValues.stream()
            .filter(dv -> dv.getAttributeOptionCombo().getUid().equals(cocTarget.getUid()))
            .toList();

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("keep source value 1b", "keep source value 2a", "keep target value 3b"),
        targetItems.stream().map(DataValue::getValue).collect(Collectors.toSet()));
    assertTrue(
        allCategoryOptionCombos.stream().anyMatch(coc -> cocTarget.getUid().equals(coc.getUid())),
        "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  // ------------------------
  // -- DataValueAudit --
  // ------------------------

  @Test
  @DisplayName(
      "DataValueAudits with references to source COCs are deleted when sources are deleted")
  void dataValueAuditMergeDeleteTest() throws ConflictException {
    // given
    DataValueAudit dva1 = createDataValueAudit(cocDuplicate, "1", p1);
    DataValueAudit dva2 = createDataValueAudit(cocDuplicate, "2", p1);
    DataValueAudit dva3 = createDataValueAudit(cocDuplicate, "1", p1);
    DataValueAudit dva4 = createDataValueAudit(cocDuplicate, "2", p1);
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
    DataValueAuditQueryParams source1DvaQueryParams = getQueryParams(cocDuplicate);
    DataValueAuditQueryParams targetDvaQueryParams = getQueryParams(cocTarget);

    List<DataValueAudit> source1Audits =
        dataValueAuditStore.getDataValueAudits(source1DvaQueryParams);

    List<DataValueAudit> targetItems = dataValueAuditStore.getDataValueAudits(targetDvaQueryParams);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, source1Audits.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref");
    assertCocCountAfterAutoGenerate(5);
  }

  // ------------------------
  // -- DataApprovalAudit --
  // ------------------------
  @Test
  @DisplayName(
      "DataApprovalAudits with references to source COCs are deleted when sources are deleted")
  void dataApprovalAuditMergeDeleteTest() throws ConflictException {
    // given
    DataApprovalLevel dataApprovalLevel = new DataApprovalLevel();
    dataApprovalLevel.setLevel(1);
    dataApprovalLevel.setName("DAL");
    manager.save(dataApprovalLevel);

    DataApprovalWorkflow daw = new DataApprovalWorkflow();
    daw.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw.setName("DAW");
    daw.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw);

    DataApprovalAudit daa1 = createDataApprovalAudit(cocDuplicate, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa2 = createDataApprovalAudit(cocDuplicate, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa3 = createDataApprovalAudit(cocDuplicate, dataApprovalLevel, daw, p1);
    DataApprovalAudit daa4 = createDataApprovalAudit(cocDuplicate, dataApprovalLevel, daw, p1);
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
            .setAttributeOptionCombos(new HashSet<>(Collections.singletonList(cocDuplicate)));
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
    assertCocCountAfterAutoGenerate(5);
  }

  // -----------------------
  // ---- DataApproval ----
  // -----------------------
  @Test
  @DisplayName(
      "Non-duplicate DataApprovals with references to source COCs are replaced with target COC using LAST_UPDATED strategy")
  void dataApprovalMergeCocLastUpdatedTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    DataApprovalWorkflow daw1 = new DataApprovalWorkflow();
    daw1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw1.setName("DAW1");
    daw1.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw1);

    DataApprovalWorkflow daw2 = new DataApprovalWorkflow();
    daw2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw2.setName("DAW2");
    daw2.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw2);

    DataApproval da1 = createDataApproval(cocDuplicate, level1, daw1, p1, ou1);
    DataApproval da2 = createDataApproval(cocDuplicate, level2, daw1, p2, ou1);
    DataApproval da3 = createDataApproval(cocTarget, level2, daw2, p2, ou2);
    DataApproval da4 = createDataApproval(cocRandom, level2, daw2, p3, ou3);

    dataApprovalStore.addDataApproval(da1);
    dataApprovalStore.addDataApproval(da2);
    dataApprovalStore.addDataApproval(da3);
    dataApprovalStore.addDataApproval(da4);

    // pre-merge state
    List<DataApproval> sourcesPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));
    assertEquals(2, sourcesPreMerge.size(), "Expect 2 entries with source COC refs");
    assertEquals(1, targetPreMerge.size(), "Expect 1 entries with target COC refs");

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<DataApproval> sourceItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC refs");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Duplicate DataApprovals are replaced with target COC using LAST_UPDATED strategy, target has latest lastUpdated value")
  void duplicateDataApprovalMergeCocLastUpdatedTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    DataApprovalWorkflow daw1 = new DataApprovalWorkflow();
    daw1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw1.setName("DAW1");
    daw1.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw1);

    DataApprovalWorkflow daw2 = new DataApprovalWorkflow();
    daw2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw2.setName("DAW2");
    daw2.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw2);

    DataApproval da1a = createDataApproval(cocDuplicate, level1, daw1, p1, ou1);
    da1a.setLastUpdated(DateUtils.parseDate("2024-6-8"));
    DataApproval da1b = createDataApproval(cocDuplicate, level1, daw1, p2, ou1);
    da1b.setLastUpdated(DateUtils.parseDate("2024-10-8"));
    DataApproval da3a = createDataApproval(cocTarget, level1, daw1, p1, ou1);
    da3a.setLastUpdated(DateUtils.parseDate("2024-12-8"));
    DataApproval da3b = createDataApproval(cocTarget, level1, daw1, p2, ou1);
    da3b.setLastUpdated(DateUtils.parseDate("2024-12-9"));
    DataApproval da4a = createDataApproval(cocRandom, level1, daw1, p1, ou1);
    DataApproval da4b = createDataApproval(cocRandom, level1, daw1, p2, ou1);

    dataApprovalStore.addDataApproval(da1a);
    dataApprovalStore.addDataApproval(da1b);
    dataApprovalStore.addDataApproval(da3a);
    dataApprovalStore.addDataApproval(da3b);
    dataApprovalStore.addDataApproval(da4a);
    dataApprovalStore.addDataApproval(da4b);

    // pre-merge state
    List<DataApproval> sourcesPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));
    assertEquals(2, sourcesPreMerge.size(), "Expect 2 entries with source COC refs");
    assertEquals(2, targetPreMerge.size(), "Expect 2 entries with target COC refs");

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<DataApproval> sourceItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 4 entries with target COC refs");
    assertEquals(
        Set.of("2024-12-08", "2024-12-09"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain the original target Data Approvals lastUpdated dates");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Duplicate & non-duplicate DataApprovals are replaced with target COC using LAST_UPDATED strategy")
  void duplicateAndNonDuplicateDataApprovalMergeTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    DataApprovalWorkflow daw1 = new DataApprovalWorkflow();
    daw1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw1.setName("DAW1");
    daw1.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw1);

    DataApprovalWorkflow daw2 = new DataApprovalWorkflow();
    daw2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw2.setName("DAW2");
    daw2.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw2);

    DataApproval da1a = createDataApproval(cocDuplicate, level1, daw1, p1, ou1);
    da1a.setLastUpdated(DateUtils.parseDate("2024-12-8"));
    DataApproval da1b = createDataApproval(cocDuplicate, level1, daw1, p2, ou1);
    da1b.setLastUpdated(DateUtils.parseDate("2024-10-8"));
    DataApproval da3a = createDataApproval(cocTarget, level1, daw1, p1, ou1);
    da3a.setLastUpdated(DateUtils.parseDate("2024-12-1"));
    DataApproval da3b = createDataApproval(cocTarget, level1, daw1, p2, ou1);
    da3b.setLastUpdated(DateUtils.parseDate("2024-12-9"));
    DataApproval da4a = createDataApproval(cocRandom, level1, daw1, p1, ou1);
    DataApproval da4b = createDataApproval(cocRandom, level1, daw1, p2, ou1);

    dataApprovalStore.addDataApproval(da1a);
    dataApprovalStore.addDataApproval(da1b);
    dataApprovalStore.addDataApproval(da3a);
    dataApprovalStore.addDataApproval(da3b);
    dataApprovalStore.addDataApproval(da4a);
    dataApprovalStore.addDataApproval(da4b);

    // pre-merge state
    List<DataApproval> sourcesPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));
    assertEquals(2, sourcesPreMerge.size(), "Expect 2 entries with source COC refs");
    assertEquals(2, targetPreMerge.size(), "Expect 2 entries with target COC refs");

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);

    // then
    List<DataApproval> sourceItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entries with target COC refs");
    assertEquals(
        Set.of("2024-12-08", "2024-12-09"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain the original target Data Approvals lastUpdated dates");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Duplicate DataApprovals are replaced with target COC using LAST_UPDATED strategy, sources have latest lastUpdated value")
  void duplicateDataApprovalSourceLastUpdatedTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    DataApprovalWorkflow daw1 = new DataApprovalWorkflow();
    daw1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw1.setName("DAW1");
    daw1.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw1);

    DataApprovalWorkflow daw2 = new DataApprovalWorkflow();
    daw2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw2.setName("DAW2");
    daw2.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw2);

    DataApproval da1a = createDataApproval(cocDuplicate, level1, daw1, p1, ou1);
    da1a.setLastUpdated(DateUtils.parseDate("2024-12-03"));
    DataApproval da1b = createDataApproval(cocDuplicate, level1, daw1, p2, ou1);
    da1b.setLastUpdated(DateUtils.parseDate("2024-12-08"));
    DataApproval da3a = createDataApproval(cocTarget, level1, daw1, p1, ou1);
    da3a.setLastUpdated(DateUtils.parseDate("2024-06-08"));
    DataApproval da3b = createDataApproval(cocTarget, level1, daw1, p2, ou1);
    da3b.setLastUpdated(DateUtils.parseDate("2024-06-14"));
    DataApproval da4a = createDataApproval(cocRandom, level1, daw1, p1, ou1);
    DataApproval da4b = createDataApproval(cocRandom, level1, daw1, p2, ou1);

    dataApprovalStore.addDataApproval(da1a);
    dataApprovalStore.addDataApproval(da1b);
    dataApprovalStore.addDataApproval(da3a);
    dataApprovalStore.addDataApproval(da3b);
    dataApprovalStore.addDataApproval(da4a);
    dataApprovalStore.addDataApproval(da4b);

    // pre-merge state
    List<DataApproval> sourcesPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetPreMerge =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));
    assertEquals(2, sourcesPreMerge.size(), "Expect 2 entries with source COC refs");
    assertEquals(2, targetPreMerge.size(), "Expect 2 entries with target COC refs");

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<DataApproval> sourceItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entries with target COC refs");
    assertEquals(
        Set.of("2024-12-03", "2024-12-08"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain the original source Data Approvals lastUpdated dates");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "DataApprovals with references to source COCs are deleted when using DISCARD strategy")
  void dataApprovalMergeCocDiscardTest() throws ConflictException {
    // given
    DataApprovalLevel level1 = new DataApprovalLevel();
    level1.setLevel(1);
    level1.setName("DAL1");
    manager.save(level1);

    DataApprovalLevel level2 = new DataApprovalLevel();
    level2.setLevel(2);
    level2.setName("DAL2");
    manager.save(level2);

    DataApprovalWorkflow daw1 = new DataApprovalWorkflow();
    daw1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw1.setName("DAW1");
    daw1.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw1);

    DataApprovalWorkflow daw2 = new DataApprovalWorkflow();
    daw2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    daw2.setName("DAW2");
    daw2.setCategoryCombo(categoryMetadata.cc1());
    manager.save(daw2);

    DataApproval da1a = createDataApproval(cocDuplicate, level1, daw1, p1, ou1);
    da1a.setLastUpdated(DateUtils.parseDate("2024-12-03"));
    DataApproval da1b = createDataApproval(cocDuplicate, level1, daw1, p2, ou1);
    da1b.setLastUpdated(DateUtils.parseDate("2024-12-01"));
    DataApproval da2a = createDataApproval(cocDuplicate, level2, daw1, p1, ou1);
    da2a.setLastUpdated(DateUtils.parseDate("2024-11-01"));
    DataApproval da2b = createDataApproval(cocDuplicate, level2, daw1, p2, ou1);
    da2b.setLastUpdated(DateUtils.parseDate("2024-12-08"));
    DataApproval da3a = createDataApproval(cocTarget, level1, daw1, p1, ou1);
    da3a.setLastUpdated(DateUtils.parseDate("2024-06-08"));
    DataApproval da3b = createDataApproval(cocTarget, level1, daw1, p2, ou1);
    da3b.setLastUpdated(DateUtils.parseDate("2024-06-14"));
    DataApproval da4a = createDataApproval(cocRandom, level1, daw1, p1, ou1);
    DataApproval da4b = createDataApproval(cocRandom, level1, daw1, p2, ou1);

    dataApprovalStore.addDataApproval(da1a);
    dataApprovalStore.addDataApproval(da1b);
    dataApprovalStore.addDataApproval(da2a);
    dataApprovalStore.addDataApproval(da2b);
    dataApprovalStore.addDataApproval(da3a);
    dataApprovalStore.addDataApproval(da3b);
    dataApprovalStore.addDataApproval(da4a);
    dataApprovalStore.addDataApproval(da4b);

    // pre merge state
    List<DataApproval> sourceItemsBefore =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItemsBefore =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    assertEquals(4, sourceItemsBefore.size(), "Expect 4 entries with source COC refs");
    assertEquals(2, targetItemsBefore.size(), "Expect 2 entry with target COC ref only");

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<DataApproval> sourceItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<DataApproval> targetItems =
        dataApprovalStore.getByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  // -----------------------------
  // -- Event attributeOptionCombo --
  // -----------------------------
  @Test
  @DisplayName(
      "Event attributeOptionCombo references to source COCs are replaced with target COC when using LAST_UPDATED")
  void eventMergeTest() throws ConflictException {
    // given
    TrackedEntityType entityType = createTrackedEntityType('T');
    manager.save(entityType);
    TrackedEntity trackedEntity = createTrackedEntity(ou1, entityType);
    manager.save(trackedEntity);
    Enrollment enrollment = createEnrollment(program, trackedEntity, ou1);
    manager.save(enrollment);
    ProgramStage stage = createProgramStage('s', 2);
    manager.save(stage);

    Event e1 = createEvent(stage, enrollment, ou1);
    e1.setAttributeOptionCombo(cocDuplicate);
    Event e2 = createEvent(stage, enrollment, ou1);
    e2.setAttributeOptionCombo(cocDuplicate2);
    Event e3 = createEvent(stage, enrollment, ou1);
    e3.setAttributeOptionCombo(cocTarget);
    Event e4 = createEvent(stage, enrollment, ou1);
    e4.setAttributeOptionCombo(cocRandom);

    manager.save(List.of(e1, e2, e3, e4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);
    dbmsManager.clearSession();

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<Event> allEvents = eventStore.getAll();
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(4, allEvents.size(), "Expect 4 entries still");
    assertEquals(
        Set.of(cocTarget.getUid(), cocRandom.getUid()),
        allEvents.stream()
            .map(e -> e.getAttributeOptionCombo().getUid())
            .collect(Collectors.toSet()),
        "All events should only have references to the target coc and the random coc");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocTarget.getUid()));
    assertCocCountAfterAutoGenerate(5);
  }

  // --------------------------------
  // --CompleteDataSetRegistration--
  // --------------------------------
  @Test
  @DisplayName(
      "CompleteDataSetRegistration with references to source COCs are deleted when using DISCARD strategy")
  void cdsrMergeCocDiscardTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1');
    ds1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds1);

    CompleteDataSetRegistration cdsr1 = createCdsr(ds1, ou1, p1, cocDuplicate);
    CompleteDataSetRegistration cdsr2 = createCdsr(ds1, ou1, p2, cocDuplicate);
    CompleteDataSetRegistration cdsr3 = createCdsr(ds1, ou1, p1, cocTarget);
    CompleteDataSetRegistration cdsr4 = createCdsr(ds1, ou1, p1, cocRandom);
    completeDataSetRegistrationStore.saveCompleteDataSetRegistration(cdsr1);
    completeDataSetRegistrationStore.saveCompleteDataSetRegistration(cdsr2);
    completeDataSetRegistrationStore.saveCompleteDataSetRegistration(cdsr3);
    completeDataSetRegistrationStore.saveCompleteDataSetRegistration(cdsr4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CompleteDataSetRegistration> sourceItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<CompleteDataSetRegistration> targetItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(1, targetItems.size(), "Expect 1 entry with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "CompleteDataSetRegistration with references to source COCs are merged when using LAST_UPDATED strategy, no duplicates")
  void cdsrMergeNoDuplicatesTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1');
    ds1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds1);

    DataSet ds2 = createDataSet('2');
    ds2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds2);

    CompleteDataSetRegistration cdsr1 = createCdsr(ds1, ou1, p1, cocDuplicate);
    CompleteDataSetRegistration cdsr2 = createCdsr(ds2, ou1, p3, cocDuplicate);
    CompleteDataSetRegistration cdsr3 = createCdsr(ds1, ou3, p2, cocTarget);
    CompleteDataSetRegistration cdsr4 = createCdsr(ds2, ou2, p1, cocRandom);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr1);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr2);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr3);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CompleteDataSetRegistration> sourceItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<CompleteDataSetRegistration> targetItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(3, targetItems.size(), "Expect 3 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Merge CompleteDataSetRegistration with references to source COCs, using LAST_UPDATED strategy, with duplicates, target has latest lastUpdated")
  void cdsrMergeDuplicatesTargetLastUpdatedTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1');
    ds1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds1);

    DataSet ds2 = createDataSet('2');
    ds2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds2);

    CompleteDataSetRegistration cdsr1 = createCdsr(ds1, ou1, p1, cocDuplicate);
    cdsr1.setLastUpdated(DateUtils.parseDate("2024-11-01"));
    CompleteDataSetRegistration cdsr2 = createCdsr(ds1, ou1, p2, cocDuplicate);
    cdsr2.setLastUpdated(DateUtils.parseDate("2024-10-01"));
    CompleteDataSetRegistration cdsr3 = createCdsr(ds1, ou1, p1, cocTarget);
    cdsr3.setLastUpdated(DateUtils.parseDate("2024-12-05"));
    CompleteDataSetRegistration cdsr4 = createCdsr(ds2, ou2, p1, cocRandom);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr1);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr2);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr3);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CompleteDataSetRegistration> sourceItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<CompleteDataSetRegistration> targetItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("2024-12-05", "2024-10-01"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain target Data Approvals lastUpdated dates");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Merge CompleteDataSetRegistration with references to source COCs, using LAST_UPDATED strategy, with duplicates, sources have latest lastUpdated")
  void cdsrMergeDuplicatesSourcesLastUpdatedTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1');
    ds1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds1);

    DataSet ds2 = createDataSet('2');
    ds2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds2);

    CompleteDataSetRegistration cdsr1 = createCdsr(ds1, ou1, p1, cocDuplicate);
    cdsr1.setLastUpdated(DateUtils.parseDate("2024-10-01"));
    CompleteDataSetRegistration cdsr2 = createCdsr(ds1, ou1, p2, cocDuplicate);
    cdsr2.setLastUpdated(DateUtils.parseDate("2024-11-01"));
    CompleteDataSetRegistration cdsr3 = createCdsr(ds1, ou1, p1, cocTarget);
    cdsr3.setLastUpdated(DateUtils.parseDate("2024-05-05"));
    CompleteDataSetRegistration cdsr4 = createCdsr(ds2, ou2, p1, cocRandom);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr1);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr2);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr3);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CompleteDataSetRegistration> sourceItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<CompleteDataSetRegistration> targetItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("2024-11-01", "2024-10-01"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain source registration lastUpdated dates");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  @Test
  @DisplayName(
      "Merge CompleteDataSetRegistration with references to source COCs, using LAST_UPDATED strategy, with duplicates & non-duplicates")
  void cdsrMergeDuplicatesNonDuplicatesTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1');
    ds1.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds1);

    DataSet ds2 = createDataSet('2');
    ds2.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.save(ds2);

    CompleteDataSetRegistration cdsr1 = createCdsr(ds1, ou1, p1, cocDuplicate);
    cdsr1.setLastUpdated(DateUtils.parseDate("2024-10-01"));
    CompleteDataSetRegistration cdsr2 = createCdsr(ds2, ou2, p1, cocDuplicate);
    cdsr2.setLastUpdated(DateUtils.parseDate("2024-11-11"));
    CompleteDataSetRegistration cdsr3 = createCdsr(ds1, ou1, p1, cocTarget);
    cdsr3.setLastUpdated(DateUtils.parseDate("2024-12-05"));
    CompleteDataSetRegistration cdsr4 = createCdsr(ds2, ou2, p1, cocRandom);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr1);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr2);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr3);
    completeDataSetRegistrationStore.saveWithoutUpdatingLastUpdated(cdsr4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);

    // when
    MergeReport report = categoryOptionComboMergeService.processMerge(mergeParams);
    entityManager.flush();

    // then
    List<CompleteDataSetRegistration> sourceItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocDuplicate)));
    List<CompleteDataSetRegistration> targetItems =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(List.of(UID.of(cocTarget)));

    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceItems.size(), "Expect 0 entries with source COC refs");
    assertEquals(2, targetItems.size(), "Expect 2 entries with target COC ref only");
    assertEquals(5, allCategoryOptionCombos.size(), "Expect 5 COCs present");
    assertEquals(
        Set.of("2024-12-05", "2024-11-11"),
        targetItems.stream()
            .map(da -> DateUtils.toMediumDate(da.getLastUpdated()))
            .collect(Collectors.toSet()),
        "target items should contain target & source registration lastUpdated dates");
    assertTrue(allCategoryOptionCombos.contains(cocTarget), "Target COC should be present");
    assertFalse(
        allCategoryOptionCombos.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet())
            .contains(cocDuplicate.getUid()),
        "Source COC should not be present");
    assertCocCountAfterAutoGenerate(5);
  }

  private CompleteDataSetRegistration createCdsr(
      DataSet ds, OrganisationUnit ou, Period p, CategoryOptionCombo coc) {
    CompleteDataSetRegistration cdsr = new CompleteDataSetRegistration();
    cdsr.setSource(ou);
    cdsr.setAttributeOptionCombo(coc);
    cdsr.setPeriod(p);
    cdsr.setDataSet(ds);
    cdsr.setCompleted(true);
    return cdsr;
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(cocDuplicate.getUid(), cocDuplicate2.getUid()));
    mergeParams.setTarget(UID.of(cocTarget.getUid()));
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);
    return mergeParams;
  }

  private DataValueAudit createDataValueAudit(CategoryOptionCombo coc, String value, Period p) {
    DataValueAudit dva = new DataValueAudit();
    dva.setDataElement(de1);
    dva.setValue(value);
    dva.setAuditType(AuditOperationType.CREATE);
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

  private DataApproval createDataApproval(
      CategoryOptionCombo coc,
      DataApprovalLevel level,
      DataApprovalWorkflow workflow,
      Period p,
      OrganisationUnit org) {
    DataApproval da = new DataApproval(level, workflow, p, org, coc);
    da.setCreated(new Date());
    da.setCreator(getCurrentUser());
    return da;
  }

  private DataValueAuditQueryParams getQueryParams(CategoryOptionCombo coc) {
    return new DataValueAuditQueryParams().setCategoryOptionCombo(coc);
  }
}
