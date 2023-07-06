/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.expression.Operator.compulsory_pair;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.exclusive_pair;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.hisp.dhis.expression.Operator.less_than;
import static org.hisp.dhis.expression.Operator.less_than_or_equal_to;
import static org.hisp.dhis.expression.Operator.not_equal_to;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class ValidationServiceTest extends IntegrationTestBase {

  @Autowired private ValidationService validationService;

  @Autowired private ValidationRuleService validationRuleService;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private ExpressionService expressionService;

  @Autowired private DataSetService dataSetService;

  @Autowired private ProgramService programService;

  @Autowired private DataValueService dataValueService;

  @Autowired private DataValueStore dataValueStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  @Autowired private PeriodService periodService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Autowired private UserService injectUserService;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private DataElement dataElementD;

  private DataElement dataElementE;

  private Set<CategoryOptionCombo> optionCombos;

  private CategoryCombo categoryComboX;

  private CategoryOptionCombo optionComboX;

  private CategoryOptionCombo optionCombo;

  private DataSet dataSetWeekly;

  private DataSet dataSetMonthly;

  private DataSet dataSetYearly;

  private Period periodA;

  private Period periodB;

  private Period periodC;

  private Period periodY;

  private int dayInPeriodA;

  private int dayInPeriodB;

  private int dayInPeriodC;

  private int dayInPeriodY;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private OrganisationUnit sourceC;

  private OrganisationUnit sourceD;

  private OrganisationUnit sourceE;

  private OrganisationUnit sourceF;

  private OrganisationUnit sourceG;

  private OrganisationUnitGroup orgUnitGroupA;

  private OrganisationUnitGroup orgUnitGroupB;

  private OrganisationUnitGroup orgUnitGroupC;

  private Set<OrganisationUnit> allSources = new HashSet<>();

  private ValidationRule ruleA;

  private ValidationRule ruleB;

  private ValidationRule ruleC;

  private ValidationRule ruleD;

  private ValidationRule ruleE;

  private ValidationRule ruleF;

  private ValidationRule ruleG;

  private ValidationRule ruleP;

  private ValidationRule ruleQ;

  private ValidationRule ruleR;

  private ValidationRule ruleS;

  private ValidationRule ruleT;

  private ValidationRule ruleU;

  private ValidationRule ruleX;

  private ValidationRuleGroup group;

  private PeriodType ptWeekly;

  private PeriodType ptMonthly;

  private PeriodType ptYearly;

  private CategoryOptionCombo defaultCombo;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------
  @Override
  public void setUpTest() throws Exception {
    this.userService = injectUserService;
    User user = createAndAddUser(true, "SUPERUSER", allSources, null);
    injectSecurityContext(user);

    ptWeekly = new WeeklyPeriodType();
    ptMonthly = new MonthlyPeriodType();
    ptYearly = new YearlyPeriodType();
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementC = createDataElement('C');
    dataElementD = createDataElement('D');
    dataElementE = createDataElement('E');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataElementService.addDataElement(dataElementD);
    dataElementService.addDataElement(dataElementE);
    CategoryOption optionX = createCategoryOption('X');
    Category categoryX = createCategory('X', optionX);
    categoryComboX = createCategoryCombo('X', categoryX);
    optionComboX = createCategoryOptionCombo(categoryComboX, optionX);
    categoryComboX.getOptionCombos().add(optionComboX);
    categoryService.addCategoryOption(optionX);
    categoryService.addCategory(categoryX);
    categoryService.addCategoryCombo(categoryComboX);
    categoryService.addCategoryOptionCombo(optionComboX);
    optionCombo = categoryService.getDefaultCategoryOptionCombo();
    String suffixX = SEPARATOR + optionComboX.getUid();
    String suffix = SEPARATOR + optionCombo.getUid();
    optionCombos = new HashSet<>();
    optionCombos.add(optionCombo);
    Expression expressionA =
        new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}",
            "expressionA");
    Expression expressionA2 =
        new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}",
            "expressionA2");
    Expression expressionA3 =
        new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}",
            "expressionA3");
    Expression expressionA4 =
        new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}",
            "expressionA4");
    Expression expressionA5 =
        new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}",
            "expressionA5");
    Expression expressionB =
        new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}",
            "expressionB");
    Expression expressionB2 =
        new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}",
            "expressionB2");
    Expression expressionB3 =
        new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}",
            "expressionB3");
    Expression expressionC =
        new Expression("#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC");
    Expression expressionC2 =
        new Expression("#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC2");
    Expression expressionC3 =
        new Expression("#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC3");
    Expression expressionI =
        new Expression("#{" + dataElementA.getUid() + suffix + "}", "expressionI");
    Expression expressionI2 =
        new Expression("#{" + dataElementA.getUid() + suffix + "}", "expressionI2");
    Expression expressionI3 =
        new Expression("#{" + dataElementA.getUid() + suffix + "}", "expressionI3");
    Expression expressionJ =
        new Expression("#{" + dataElementB.getUid() + suffix + "}", "expressionJ");
    Expression expressionJ2 =
        new Expression("#{" + dataElementB.getUid() + suffix + "}", "expressionJ2");
    Expression expressionK =
        new Expression("#{" + dataElementC.getUid() + "}", "expressionK", NEVER_SKIP);
    Expression expressionL =
        new Expression("#{" + dataElementD.getUid() + "}", "expressionL", NEVER_SKIP);
    Expression expressionP = new Expression(SYMBOL_DAYS, "expressionP", NEVER_SKIP);
    Expression expressionP2 = new Expression(SYMBOL_DAYS, "expressionP2", NEVER_SKIP);
    Expression expressionQ =
        new Expression("#{" + dataElementE.getUid() + "}", "expressionQ", NEVER_SKIP);
    Expression expressionR =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionR");
    Expression expressionS =
        new Expression(
            "#{"
                + dataElementA.getUid()
                + suffixX
                + "} + #{"
                + dataElementB.getUid()
                + suffixX
                + "}",
            "expressionS");
    Expression expressionT =
        new Expression(
            "#{"
                + dataElementA.getUid()
                + suffix
                + "} + #{"
                + dataElementB.getUid()
                + suffixX
                + "}",
            "expressionT");
    Expression expressionU = new Expression("1000", "expressionU");
    Expression expressionU2 = new Expression("1000", "expressionU2");
    Expression expressionU3 = new Expression("1000", "expressionU3");
    Expression expressionU4 = new Expression("1000", "expressionU4");
    periodA = createPeriod(ptMonthly, getDate(2000, 3, 1), getDate(2000, 3, 31));
    periodB = createPeriod(ptMonthly, getDate(2000, 4, 1), getDate(2000, 4, 30));
    periodC = createPeriod(ptMonthly, getDate(2000, 5, 1), getDate(2000, 5, 31));
    periodY = createPeriod(ptYearly, getDate(2000, 1, 1), getDate(2000, 12, 31));
    dayInPeriodA = periodService.getDayInPeriod(periodA, new Date());
    dayInPeriodB = periodService.getDayInPeriod(periodB, new Date());
    dayInPeriodC = periodService.getDayInPeriod(periodC, new Date());
    dayInPeriodY = periodService.getDayInPeriod(periodY, new Date());
    dataSetWeekly = createDataSet('W', ptWeekly);
    dataSetMonthly = createDataSet('M', ptMonthly);
    dataSetYearly = createDataSet('Y', ptYearly);
    // OrgUnit hierarchy levels:
    // 1 - 2 - 3
    // A
    // B - C
    // B - D
    // B - D - E
    // B - D - F
    // G
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B');
    sourceC = createOrganisationUnit('C', sourceB);
    sourceD = createOrganisationUnit('D', sourceB);
    sourceE = createOrganisationUnit('E', sourceD);
    sourceF = createOrganisationUnit('F', sourceD);
    sourceG = createOrganisationUnit('G');
    organisationUnitService.addOrganisationUnit(sourceA);
    organisationUnitService.addOrganisationUnit(sourceB);
    organisationUnitService.addOrganisationUnit(sourceC);
    organisationUnitService.addOrganisationUnit(sourceD);
    organisationUnitService.addOrganisationUnit(sourceE);
    organisationUnitService.addOrganisationUnit(sourceF);
    organisationUnitService.addOrganisationUnit(sourceG);

    allSources.add(sourceA);
    allSources.add(sourceB);
    allSources.add(sourceC);
    allSources.add(sourceD);
    allSources.add(sourceE);
    allSources.add(sourceF);
    allSources.add(sourceG);
    dataSetMonthly.addOrganisationUnit(sourceA);
    dataSetMonthly.addOrganisationUnit(sourceB);
    dataSetMonthly.addOrganisationUnit(sourceC);
    dataSetMonthly.addOrganisationUnit(sourceD);
    dataSetMonthly.addOrganisationUnit(sourceE);
    dataSetMonthly.addOrganisationUnit(sourceF);
    dataSetWeekly.addOrganisationUnit(sourceB);
    dataSetWeekly.addOrganisationUnit(sourceC);
    dataSetWeekly.addOrganisationUnit(sourceD);
    dataSetWeekly.addOrganisationUnit(sourceE);
    dataSetWeekly.addOrganisationUnit(sourceF);
    dataSetWeekly.addOrganisationUnit(sourceG);
    dataSetYearly.addOrganisationUnit(sourceB);
    dataSetYearly.addOrganisationUnit(sourceC);
    dataSetYearly.addOrganisationUnit(sourceD);
    dataSetYearly.addOrganisationUnit(sourceE);
    dataSetYearly.addOrganisationUnit(sourceF);

    orgUnitGroupA = createOrganisationUnitGroup('A');
    orgUnitGroupB = createOrganisationUnitGroup('B');
    orgUnitGroupC = createOrganisationUnitGroup('C');
    orgUnitGroupA.addOrganisationUnit(sourceB);
    orgUnitGroupA.addOrganisationUnit(sourceC);
    orgUnitGroupB.addOrganisationUnit(sourceD);
    orgUnitGroupB.addOrganisationUnit(sourceE);
    orgUnitGroupC.addOrganisationUnit(sourceE);
    orgUnitGroupC.addOrganisationUnit(sourceF);
    organisationUnitGroupService.addOrganisationUnitGroup(orgUnitGroupA);
    organisationUnitGroupService.addOrganisationUnitGroup(orgUnitGroupB);
    organisationUnitGroupService.addOrganisationUnitGroup(orgUnitGroupC);
    dataSetMonthly.addDataSetElement(dataElementA);
    dataSetMonthly.addDataSetElement(dataElementB);
    dataSetMonthly.addDataSetElement(dataElementC);
    dataSetMonthly.addDataSetElement(dataElementD);
    dataSetWeekly.addDataSetElement(dataElementE);
    dataSetYearly.addDataSetElement(dataElementE);
    dataSetService.addDataSet(dataSetWeekly);
    dataSetService.addDataSet(dataSetMonthly);
    dataSetService.addDataSet(dataSetYearly);
    dataElementService.updateDataElement(dataElementA);
    dataElementService.updateDataElement(dataElementB);
    dataElementService.updateDataElement(dataElementC);
    dataElementService.updateDataElement(dataElementD);
    dataElementService.updateDataElement(dataElementE);
    // deA + deB = deC - deD
    ruleA = createValidationRule("A", equal_to, expressionA, expressionB, ptMonthly);
    // deC - deD > deB * 2
    ruleB = createValidationRule("B", greater_than, expressionB2, expressionC, ptMonthly);
    // deC - deD <= deA + deB
    ruleC = createValidationRule("C", less_than_or_equal_to, expressionB3, expressionA2, ptMonthly);
    // deA + deB < deB * 2
    ruleD = createValidationRule("D", less_than, expressionA3, expressionC2, ptMonthly);
    // deA [Compulsory pair] deB
    ruleE = createValidationRule("E", compulsory_pair, expressionI, expressionJ, ptMonthly);
    // deA [Exclusive pair] deB
    ruleF = createValidationRule("F", exclusive_pair, expressionI2, expressionJ2, ptMonthly);
    // deC = DeD
    ruleG = createValidationRule("G", equal_to, expressionK, expressionL, ptMonthly);
    // deA = [days]
    ruleP = createValidationRule("P", equal_to, expressionI3, expressionP, ptMonthly);
    // deE = [days]
    ruleQ = createValidationRule("Q", equal_to, expressionQ, expressionP2, ptYearly);
    // deA(sum) + deB(sum) = 1000
    ruleR = createValidationRule("R", equal_to, expressionR, expressionU, ptMonthly);
    // deA.optionComboX + deB.optionComboX = 1000
    ruleS = createValidationRule("S", equal_to, expressionS, expressionU2, ptMonthly);
    // deA.default + deB.optionComboX = 1000
    ruleT = createValidationRule("T", equal_to, expressionT, expressionU3, ptMonthly);
    // deA.default + deB.default = 1000
    ruleU = createValidationRule("U", equal_to, expressionA4, expressionU4, ptMonthly);
    // deA + deB = deB * 2
    ruleX = createValidationRule("X", equal_to, expressionA5, expressionC3, ptMonthly);
    group = createValidationRuleGroup('A');
    defaultCombo = categoryService.getDefaultCategoryOptionCombo();
  }

  // -------------------------------------------------------------------------
  // Local convenience methods
  // -------------------------------------------------------------------------
  private ValidationResult createValidationResult(
      ValidationRule validationRule,
      Period period,
      OrganisationUnit orgUnit,
      CategoryOptionCombo catCombo,
      double ls,
      double rs,
      int dayInPeriod) {
    ValidationResult vr =
        new ValidationResult(validationRule, period, orgUnit, catCombo, ls, rs, dayInPeriod);
    return vr;
  }

  /**
   * Returns a naturally ordered list of ValidationResults.
   *
   * <p>When comparing two collections, this assures that all the items are in the same order for
   * comparison. It also means that when there are different values for the same period/rule/source,
   * etc., the results are more likely to be in the same order to make it easier to see the
   * difference.
   *
   * <p>By making this a List instead of, say a TreeSet, duplicate values (if any should exist by
   * mistake!) are preserved.
   *
   * @param results collection of ValidationResult to order.
   * @return ValidationResults in their natural order.
   */
  private List<ValidationResult> orderedList(Collection<ValidationResult> results) {
    List<ValidationResult> resultList = new ArrayList<>(results);
    Collections.sort(resultList);
    return resultList;
  }

  /**
   * Asserts that a collection of ValidationResult is empty.
   *
   * @param results collection of ValidationResult to test.
   */
  private void assertResultsEmpty(Collection<ValidationResult> results) {
    assertResultsEquals(new HashSet<>(), results);
  }

  /**
   * Asserts that a collection of ValidationResult matches a reference collection. If it doesn't,
   * log some extra diagnostic information.
   *
   * <p>This method was written in response to intermittent test failures. The extra diagnostic
   * information is an attempt to further investigate the nature of the failures.
   *
   * <p>A partial stack trace is logged (just within this file), so when the test is working, the
   * check inequality can be commented out and the tester can generate a reference of expected vales
   * for each call.
   *
   * <p>Also tests to be sure that each result expression was evaluated correctly.
   *
   * @param reference the reference collection of ValidationResult.
   * @param results collection of ValidationResult to test.
   */
  private void assertResultsEquals(
      Collection<ValidationResult> reference, Collection<ValidationResult> results) {
    List<ValidationResult> referenceList = orderedList(reference);
    List<ValidationResult> resultsList = orderedList(results);
    StringBuilder sb = new StringBuilder();
    if (!referenceList.equals(resultsList)) {
      sb.append("\n");
      StackTraceElement[] e = Thread.currentThread().getStackTrace();
      for (int i = 1; i < e.length && e[i].getFileName().equals(e[1].getFileName()); i++) {
        sb.append("  at ")
            .append(e[i].getMethodName())
            .append("(")
            .append(e[i].getFileName())
            .append(":")
            .append(e[i].getLineNumber())
            .append(")\n");
      }
      sb.append(formatResultsList("Expected", referenceList))
          .append(formatResultsList("But was", resultsList))
          .append(getAllDataValues())
          .append(getAllValidationRules());
    }
    assertEquals("", sb.toString());
    for (ValidationResult result : results) {
      String operator = result.getValidationRule().getOperator().getMathematicalOperator();
      if (!operator.startsWith("[")) {
        String test =
            result.getLeftsideValue()
                + result.getValidationRule().getOperator().getMathematicalOperator()
                + result.getRightsideValue();
        assertFalse(
            (Boolean)
                expressionService.getExpressionValue(
                    ExpressionParams.builder().expression(test).parseType(SIMPLE_TEST).build()));
      }
    }
  }

  private String formatResultsList(String label, List<ValidationResult> results) {
    StringBuilder sb = new StringBuilder(label + " (" + results.size() + "):\n");
    results.forEach(r -> sb.append("  ").append(r.toString()).append("\n"));
    return sb.toString();
  }

  private String getAllDataValues() {
    List<DataValue> allDataValues = dataValueStore.getAllDataValues();
    StringBuilder sb = new StringBuilder("All data values (" + allDataValues.size() + "):\n");
    allDataValues.forEach(d -> sb.append("  ").append(d.toString()).append("\n"));
    return sb.toString();
  }

  private String getAllValidationRules() {
    List<ValidationRule> allValidationRules = validationRuleService.getAllValidationRules();
    StringBuilder sb =
        new StringBuilder("All validation rules (" + allValidationRules.size() + "):\n");
    allValidationRules.forEach(
        v ->
            sb.append("  ")
                .append(v.getName())
                .append(": ")
                .append(v.getLeftSide().getExpression())
                .append(" [")
                .append(v.getOperator())
                .append("] ")
                .append(v.getRightSide().getExpression())
                .append("\n"));
    return sb.toString();
  }

  private void useDataValue(DataElement e, Period p, OrganisationUnit s, String value) {
    dataValueService.addDataValue(createDataValue(e, p, s, optionCombo, optionCombo, value));
  }

  private void useDataValue(
      DataElement e,
      Period p,
      OrganisationUnit s,
      String value,
      CategoryOptionCombo oc1,
      CategoryOptionCombo oc2) {
    dataValueService.addDataValue(createDataValue(e, p, s, oc1, oc2, value));
  }

  // -------------------------------------------------------------------------
  // Business logic tests
  // -------------------------------------------------------------------------
  @Test
  void testValidateDateDateSources() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementC, periodA, sourceB, "3");
    useDataValue(dataElementD, periodA, sourceB, "4");
    useDataValue(dataElementA, periodB, sourceB, "1");
    useDataValue(dataElementB, periodB, sourceB, "2");
    useDataValue(dataElementC, periodB, sourceB, "3");
    useDataValue(dataElementD, periodB, sourceB, "4");
    useDataValue(dataElementA, periodA, sourceC, "1");
    useDataValue(dataElementB, periodA, sourceC, "2");
    useDataValue(dataElementC, periodA, sourceC, "3");
    useDataValue(dataElementD, periodA, sourceC, "4");
    useDataValue(dataElementA, periodB, sourceC, "1");
    useDataValue(dataElementB, periodB, sourceC, "2");
    useDataValue(dataElementC, periodB, sourceC, "3");
    useDataValue(dataElementD, periodB, sourceC, "4");
    // Invalid
    validationRuleService.saveValidationRule(ruleA);
    // Invalid
    validationRuleService.saveValidationRule(ruleB);
    // Valid
    validationRuleService.saveValidationRule(ruleC);
    // Valid
    validationRuleService.saveValidationRule(ruleD);
    // Note: in this and subsequent tests we insert the validation results
    // collection into a new HashSet. This insures that if they are the same
    // as the reference results, they will appear in the same order.
    ValidationAnalysisParams parameters =
        validationService
            .newParamsBuilder(null, sourceB, getDate(2000, 2, 1), getDate(2000, 6, 1))
            .withIncludeOrgUnitDescendants(true)
            .build();
    Collection<ValidationResult> results = runValidationAnalysis(parameters);
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        createValidationResult(ruleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        createValidationResult(ruleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB));
    reference.add(
        createValidationResult(ruleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        createValidationResult(ruleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB));
    reference.add(
        createValidationResult(ruleB, periodA, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodA));
    reference.add(
        createValidationResult(ruleB, periodB, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodB));
    reference.add(
        createValidationResult(ruleB, periodA, sourceC, defaultCombo, -1.0, 4.0, dayInPeriodA));
    reference.add(
        createValidationResult(ruleB, periodB, sourceC, defaultCombo, -1.0, 4.0, dayInPeriodB));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateDateDateSourcesGroup() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementC, periodA, sourceB, "3");
    useDataValue(dataElementD, periodA, sourceB, "4");
    useDataValue(dataElementA, periodB, sourceB, "1");
    useDataValue(dataElementB, periodB, sourceB, "2");
    useDataValue(dataElementC, periodB, sourceB, "3");
    useDataValue(dataElementD, periodB, sourceB, "4");
    useDataValue(dataElementA, periodA, sourceC, "1");
    useDataValue(dataElementB, periodA, sourceC, "2");
    useDataValue(dataElementC, periodA, sourceC, "3");
    useDataValue(dataElementD, periodA, sourceC, "4");
    useDataValue(dataElementA, periodB, sourceC, "1");
    useDataValue(dataElementB, periodB, sourceC, "2");
    useDataValue(dataElementC, periodB, sourceC, "3");
    useDataValue(dataElementD, periodB, sourceC, "4");
    // Invalid
    validationRuleService.saveValidationRule(ruleA);
    // Invalid
    validationRuleService.saveValidationRule(ruleB);
    // Valid
    validationRuleService.saveValidationRule(ruleC);
    // Valid
    validationRuleService.saveValidationRule(ruleD);
    group.getMembers().add(ruleA);
    group.getMembers().add(ruleC);
    validationRuleService.addValidationRuleGroup(group);
    ValidationAnalysisParams params =
        validationService
            .newParamsBuilder(group, sourceB, getDate(2000, 2, 1), getDate(2000, 6, 1))
            .withIncludeOrgUnitDescendants(true)
            .build();
    Collection<ValidationResult> results = runValidationAnalysis(params);
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB));
    reference.add(
        new ValidationResult(ruleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidatePeriodsRulesSources() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "2");
    useDataValue(dataElementC, periodA, sourceA, "3");
    useDataValue(dataElementD, periodA, sourceA, "4");
    useDataValue(dataElementA, periodB, sourceA, "1");
    useDataValue(dataElementB, periodB, sourceA, "2");
    useDataValue(dataElementC, periodB, sourceA, "3");
    useDataValue(dataElementD, periodB, sourceA, "4");
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementC, periodA, sourceB, "3");
    useDataValue(dataElementD, periodA, sourceB, "4");
    useDataValue(dataElementA, periodB, sourceB, "1");
    useDataValue(dataElementB, periodB, sourceB, "2");
    useDataValue(dataElementC, periodB, sourceB, "3");
    useDataValue(dataElementD, periodB, sourceB, "4");
    useDataValue(dataElementA, periodA, sourceC, "1");
    useDataValue(dataElementB, periodA, sourceC, "2");
    useDataValue(dataElementC, periodA, sourceC, "3");
    useDataValue(dataElementD, periodA, sourceC, "4");
    useDataValue(dataElementA, periodB, sourceC, "1");
    useDataValue(dataElementB, periodB, sourceC, "2");
    useDataValue(dataElementC, periodB, sourceC, "3");
    useDataValue(dataElementD, periodB, sourceC, "4");
    // Invalid
    validationRuleService.saveValidationRule(ruleA);
    // Invalid
    validationRuleService.saveValidationRule(ruleB);
    // Valid
    validationRuleService.saveValidationRule(ruleC);
    // Valid
    validationRuleService.saveValidationRule(ruleD);
    List<ValidationRule> validationRules = Lists.newArrayList(ruleA, ruleC);
    List<Period> periods =
        periodService.getPeriodsBetweenDates(getDate(2000, 2, 1), getDate(2000, 6, 1));
    ValidationAnalysisParams params =
        validationService
            .newParamsBuilder(validationRules, null, periods)
            .withIncludeOrgUnitDescendants(true)
            .build();
    Collection<ValidationResult> results = runValidationAnalysis(params);
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleA, periodB, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodB));
    reference.add(
        new ValidationResult(ruleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB));
    reference.add(
        new ValidationResult(ruleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateDataSetPeriodSource() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "2");
    useDataValue(dataElementC, periodA, sourceA, "3");
    useDataValue(dataElementD, periodA, sourceA, "4");
    validationRuleService.saveValidationRule(ruleA);
    validationRuleService.saveValidationRule(ruleB);
    validationRuleService.saveValidationRule(ruleC);
    validationRuleService.saveValidationRule(ruleD);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleB, periodA, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateForm() {
    ruleA.setSkipFormValidation(true);
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "2");
    useDataValue(dataElementC, periodA, sourceA, "3");
    useDataValue(dataElementD, periodA, sourceA, "4");
    validationRuleService.saveValidationRule(ruleA);
    validationRuleService.saveValidationRule(ruleB);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleB, periodA, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateDays() {
    useDataValue(dataElementA, periodA, sourceA, "1111");
    useDataValue(dataElementE, periodY, sourceB, "2222");
    validationRuleService.saveValidationRule(ruleP);
    validationRuleService.saveValidationRule(ruleQ);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleP, periodA, sourceA, defaultCombo, 1111.0, 31.0, dayInPeriodA));
    assertResultsEquals(reference, results);
    results =
        runValidationAnalysis(
            validationService.newParamsBuilder(dataSetYearly, sourceB, periodY).build());
    reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleQ, periodY, sourceB, defaultCombo, 2222.0, 366.0, dayInPeriodY));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateMissingValues00() {
    validationRuleService.saveValidationRule(ruleG);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateMissingValues01() {
    useDataValue(dataElementD, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleG);
    Collection<ValidationResult> reference = new HashSet<>();
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateMissingValues10() {
    useDataValue(dataElementC, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleG);
    Collection<ValidationResult> reference = new HashSet<>();
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 1.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateMissingValues11() {
    useDataValue(dataElementC, periodA, sourceA, "1");
    useDataValue(dataElementD, periodA, sourceA, "1");
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateCompulsoryPair00() {
    validationRuleService.saveValidationRule(ruleE);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateCompulsoryPair01() {
    useDataValue(dataElementB, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleE);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleE, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateCompulsoryPair10() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleE);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleE, periodA, sourceA, defaultCombo, 1.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateCompulsoryPair11() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleE);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateExclusivePairWithOtherData00() {
    useDataValue(dataElementC, periodA, sourceA, "96");
    validationRuleService.saveValidationRule(ruleG);
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 96.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateExclusivePairWithOtherData01() {
    useDataValue(dataElementC, periodA, sourceA, "97");
    validationRuleService.saveValidationRule(ruleG);
    useDataValue(dataElementB, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 97.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateExclusivePairWithOtherData10() {
    useDataValue(dataElementC, periodA, sourceA, "98");
    validationRuleService.saveValidationRule(ruleG);
    useDataValue(dataElementA, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 98.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateExclusivePairWithOtherData11() {
    useDataValue(dataElementC, periodA, sourceA, "99");
    validationRuleService.saveValidationRule(ruleG);
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "2");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleG, periodA, sourceA, defaultCombo, 99.0, 0.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateExclusivePair00() {
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateExclusivePair01() {
    useDataValue(dataElementB, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateExclusivePair10() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateExclusivePair11() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    useDataValue(dataElementB, periodA, sourceA, "2");
    validationRuleService.saveValidationRule(ruleF);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithCategoryOptions() {
    CategoryOption optionA = new CategoryOption("CategoryOptionA");
    CategoryOption optionB = new CategoryOption("CategoryOptionB");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);
    Category categoryA = createCategory('A', optionA, optionB);
    categoryService.addCategory(categoryA);
    CategoryCombo categoryComboA = createCategoryCombo('A', categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    CategoryOptionCombo optionComboA = createCategoryOptionCombo(categoryComboA, optionA);
    CategoryOptionCombo optionComboB = createCategoryOptionCombo(categoryComboA, optionB);
    categoryService.addCategoryOptionCombo(optionComboA);
    categoryService.addCategoryOptionCombo(optionComboB);
    useDataValue(dataElementD, periodA, sourceA, "3", optionComboA, optionCombo);
    useDataValue(dataElementD, periodA, sourceA, "4", optionComboB, optionCombo);
    Expression expressionZ =
        new Expression(
            "#{"
                + dataElementD.getUid()
                + "."
                + optionComboA.getUid()
                + "} * 2"
                + " + #{"
                + dataElementD.getUid()
                + "."
                + optionComboB.getUid()
                + "}",
            "expressionZ",
            NEVER_SKIP);
    Expression expressionV =
        new Expression("#{" + dataElementD.getUid() + "}", "expressionV", NEVER_SKIP);
    ValidationRule validationRuleZ =
        createValidationRule("Z", equal_to, expressionV, expressionZ, ptMonthly);
    // deD[all] = deD.optionComboA * 2 + deD.optionComboB
    validationRuleService.saveValidationRule(validationRuleZ);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(
            validationRuleZ, periodA, sourceA, defaultCombo, 7.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithDataSetElements() {
    DataSet dataSetA = createDataSet('A', ptMonthly);
    DataSet dataSetB = createDataSet('B', ptMonthly);
    DataSet dataSetC = createDataSet('C', ptMonthly);
    DataSet dataSetD = createDataSet('D', ptMonthly);
    dataSetA.addDataSetElement(dataElementA);
    dataSetA.addDataSetElement(dataElementB);
    dataSetB.addDataSetElement(dataElementA, categoryComboX);
    dataSetB.addDataSetElement(dataElementB);
    dataSetC.addDataSetElement(dataElementA);
    dataSetC.addDataSetElement(dataElementB, categoryComboX);
    dataSetD.addDataSetElement(dataElementA, categoryComboX);
    dataSetD.addDataSetElement(dataElementB, categoryComboX);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);
    dataSetService.addDataSet(dataSetC);
    dataSetService.addDataSet(dataSetD);
    validationRuleService.saveValidationRule(ruleR);
    validationRuleService.saveValidationRule(ruleS);
    validationRuleService.saveValidationRule(ruleT);
    validationRuleService.saveValidationRule(ruleU);
    useDataValue(dataElementA, periodA, sourceA, "1", optionCombo, optionCombo);
    useDataValue(dataElementB, periodA, sourceA, "2", optionCombo, optionCombo);
    useDataValue(dataElementA, periodA, sourceA, "4", optionComboX, optionCombo);
    useDataValue(dataElementB, periodA, sourceA, "8", optionComboX, optionCombo);
    ValidationResult r =
        new ValidationResult(ruleR, periodA, sourceA, defaultCombo, 15.0, 1000.0, dayInPeriodA);
    ValidationResult s =
        new ValidationResult(ruleS, periodA, sourceA, defaultCombo, 12.0, 1000.0, dayInPeriodA);
    ValidationResult t =
        new ValidationResult(ruleT, periodA, sourceA, defaultCombo, 9.0, 1000.0, dayInPeriodA);
    ValidationResult u =
        new ValidationResult(ruleU, periodA, sourceA, defaultCombo, 3.0, 1000.0, dayInPeriodA);
    assertResultsEquals(
        Lists.newArrayList(r, t, u),
        runValidationAnalysis(
            validationService.newParamsBuilder(dataSetA, sourceA, periodA).build()));
    assertResultsEquals(
        Lists.newArrayList(r, s, u),
        runValidationAnalysis(
            validationService.newParamsBuilder(dataSetB, sourceA, periodA).build()));
    assertResultsEquals(
        Lists.newArrayList(r, s, t, u),
        runValidationAnalysis(
            validationService.newParamsBuilder(dataSetC, sourceA, periodA).build()));
    assertResultsEquals(
        Lists.newArrayList(r, s, t),
        runValidationAnalysis(
            validationService.newParamsBuilder(dataSetD, sourceA, periodA).build()));
  }

  @Test
  void testValidateWithAttributeOptions() {
    CategoryOption optionA = new CategoryOption("CategoryOptionA");
    CategoryOption optionB = new CategoryOption("CategoryOptionB");
    CategoryOption optionC = new CategoryOption("CategoryOptionC");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);
    categoryService.addCategoryOption(optionC);
    Category categoryA = createCategory('A', optionA, optionB);
    Category categoryB = createCategory('B', optionC);
    categoryA.setDataDimension(true);
    categoryB.setDataDimension(true);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);
    CategoryCombo categoryComboAB = createCategoryCombo('A', categoryA, categoryB);
    categoryService.addCategoryCombo(categoryComboAB);
    CategoryOptionCombo optionComboAC =
        createCategoryOptionCombo(categoryComboAB, optionA, optionC);
    CategoryOptionCombo optionComboBC =
        createCategoryOptionCombo(categoryComboAB, optionB, optionC);
    categoryService.addCategoryOptionCombo(optionComboAC);
    categoryService.addCategoryOptionCombo(optionComboBC);
    useDataValue(dataElementA, periodA, sourceA, "4", optionCombo, optionComboAC);
    useDataValue(dataElementB, periodA, sourceA, "3", optionCombo, optionComboAC);
    useDataValue(dataElementA, periodA, sourceA, "2", optionCombo, optionComboBC);
    useDataValue(dataElementB, periodA, sourceA, "1", optionCombo, optionComboBC);
    // deA + deB < deB * 2
    validationRuleService.saveValidationRule(ruleD);
    // deA + deB = deB * 2
    validationRuleService.saveValidationRule(ruleX);
    //
    // optionComboAC
    //
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceA, periodA)
                .withAttributeOptionCombo(optionComboAC)
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA));
    assertResultsEquals(reference, results);
    //
    // All optionCombos
    //
    results = runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    reference = new HashSet<>();
    reference.add(
        new ValidationResult(ruleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleD, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA));
    reference.add(
        new ValidationResult(ruleX, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA));
    assertResultsEquals(reference, results);
    //
    // Default optionCombo
    //
    results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceA, periodA)
                .withAttributeOptionCombo(optionCombo)
                .build());
    assertResultsEmpty(results);
  }

  @Test
  void testValidateNeverSkip() {
    useDataValue(dataElementA, periodB, sourceA, "1");
    useDataValue(dataElementA, periodC, sourceA, "2");
    useDataValue(dataElementB, periodC, sourceA, "3");
    Expression expressionLeft =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "exprLeft",
            NEVER_SKIP);
    Expression expressionRight =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "exprRight",
            NEVER_SKIP);
    ValidationRule rule =
        createValidationRule("R", not_equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(
                    Sets.newHashSet(rule), sourceA, Sets.newHashSet(periodA, periodB, periodC))
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodB, sourceA, defaultCombo, 1.0, 1.0, dayInPeriodB));
    reference.add(
        new ValidationResult(rule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateSkipIfAllValuesAreMissing() {
    useDataValue(dataElementA, periodB, sourceA, "1");
    useDataValue(dataElementA, periodC, sourceA, "2");
    useDataValue(dataElementB, periodC, sourceA, "3");
    Expression expressionLeft =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "exprLeft",
            SKIP_IF_ALL_VALUES_MISSING);
    Expression expressionRight =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "exprRight",
            SKIP_IF_ALL_VALUES_MISSING);
    ValidationRule rule =
        createValidationRule("R", not_equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(
                    Sets.newHashSet(rule), sourceA, Sets.newHashSet(periodA, periodB, periodC))
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodB, sourceA, defaultCombo, 1.0, 1.0, dayInPeriodB));
    reference.add(
        new ValidationResult(rule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateSkipIfAnyValueIsMissing() {
    useDataValue(dataElementA, periodB, sourceA, "1");
    useDataValue(dataElementA, periodC, sourceA, "2");
    useDataValue(dataElementB, periodC, sourceA, "3");
    Expression expressionLeft =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "exprLeft",
            SKIP_IF_ANY_VALUE_MISSING);
    Expression expressionRight =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}",
            "expressionRight",
            SKIP_IF_ANY_VALUE_MISSING);
    ValidationRule rule =
        createValidationRule("R", not_equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(
                    Sets.newHashSet(rule), sourceA, Sets.newHashSet(periodA, periodB, periodC))
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithIf() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    Expression expressionLeft =
        new Expression("if(#{" + dataElementA.getUid() + "}==1,5,6)", "exprLeft");
    Expression expressionRight =
        new Expression("if(#{" + dataElementA.getUid() + "}==2,7,8)", "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 5.0, 8.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithIsNull() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    Expression expressionLeft =
        new Expression("if(isNull(#{" + dataElementA.getUid() + "}),5,6)", "exprLeft");
    Expression expressionRight =
        new Expression("if(isNull(#{" + dataElementB.getUid() + "}),7,8)", "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 6.0, 7.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithIsNotNull() {
    useDataValue(dataElementA, periodA, sourceA, "1");
    Expression expressionLeft =
        new Expression("if(isNotNull(#{" + dataElementA.getUid() + "}),5,6)", "exprLeft");
    Expression expressionRight =
        new Expression("if(isNotNull(#{" + dataElementB.getUid() + "}),7,8)", "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 5.0, 8.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithFirstNonNull() {
    useDataValue(dataElementA, periodA, sourceA, "3");
    Expression expressionLeft =
        new Expression(
            "firstNonNull( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )",
            "exprLeft");
    Expression expressionRight =
        new Expression(
            "firstNonNull( #{" + dataElementB.getUid() + "}, #{" + dataElementA.getUid() + "} )",
            "exprRight");
    ValidationRule rule =
        createValidationRule("R", not_equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 3.0, 3.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateWithGreatestAndLeast() {
    useDataValue(dataElementA, periodA, sourceA, "10");
    useDataValue(dataElementB, periodA, sourceA, "20");
    Expression expressionLeft =
        new Expression(
            "greatest( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )",
            "exprLeft");
    Expression expressionRight =
        new Expression(
            "least( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )",
            "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 20.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateBooleanIsNull() {
    DataElement deP = createDataElement('P', ValueType.TRUE_ONLY, AggregationType.NONE);
    DataElement deQ = createDataElement('Q', ValueType.TRUE_ONLY, AggregationType.NONE);
    DataElement deR = createDataElement('R', ValueType.TRUE_ONLY, AggregationType.NONE);
    dataElementService.addDataElement(deP);
    dataElementService.addDataElement(deQ);
    dataElementService.addDataElement(deR);
    dataSetMonthly.addDataSetElement(deP);
    dataSetMonthly.addDataSetElement(deQ);
    dataSetMonthly.addDataSetElement(deR);
    dataSetService.updateDataSet(dataSetMonthly);
    useDataValue(deP, periodA, sourceA, "true");
    useDataValue(deQ, periodA, sourceA, "true");
    Expression expressionLeft =
        new Expression(
            "if(isNull(#{"
                + deP.getUid()
                + "}),0,1) + "
                + "if(isNull(#{"
                + deQ.getUid()
                + "}),0,1) + "
                + "if(isNull(#{"
                + deR.getUid()
                + "}),0,1)",
            "exprLeft");
    Expression expressionRight = new Expression("1", "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(createParamsMonthlySourceAPeriodA());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceA, defaultCombo, 2.0, 1.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateOrgUnitAncestor() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementA, periodA, sourceC, "3");
    useDataValue(dataElementA, periodA, sourceD, "5");
    useDataValue(dataElementA, periodA, sourceE, "7");
    useDataValue(dataElementA, periodA, sourceF, "9");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementB, periodA, sourceC, "4");
    useDataValue(dataElementB, periodA, sourceD, "6");
    useDataValue(dataElementB, periodA, sourceE, "8");
    useDataValue(dataElementB, periodA, sourceF, "10");
    Expression expressionLeft =
        new Expression(
            "if(orgUnit.ancestor(" + sourceB.getUid() + "), #{" + dataElementA.getUid() + "}, 20)",
            "left");
    Expression expressionRight =
        new Expression(
            "if(orgUnit.ancestor("
                + sourceC.getUid()
                + ","
                + sourceD.getUid()
                + "), #{"
                + dataElementB.getUid()
                + "}, 30)",
            "right");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceB, periodA)
                .withIncludeOrgUnitDescendants(true)
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceB, defaultCombo, 20.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceC, defaultCombo, 3.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceD, defaultCombo, 5.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceE, defaultCombo, 7.0, 8.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceF, defaultCombo, 9.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateOrgUnitGroup() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementA, periodA, sourceC, "3");
    useDataValue(dataElementA, periodA, sourceD, "5");
    useDataValue(dataElementA, periodA, sourceE, "7");
    useDataValue(dataElementA, periodA, sourceF, "9");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementB, periodA, sourceC, "4");
    useDataValue(dataElementB, periodA, sourceD, "6");
    useDataValue(dataElementB, periodA, sourceE, "8");
    useDataValue(dataElementB, periodA, sourceF, "10");
    Expression expressionLeft =
        new Expression(
            "if(orgUnit.group( "
                + orgUnitGroupA.getUid()
                + " ), #{"
                + dataElementA.getUid()
                + "}, 20)",
            "left");
    Expression expressionRight =
        new Expression(
            "if(orgUnit.group( "
                + orgUnitGroupB.getUid()
                + " , "
                + orgUnitGroupC.getUid()
                + " ), #{"
                + dataElementB.getUid()
                + "}, 30)",
            "right");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceB, periodA)
                .withIncludeOrgUnitDescendants(true)
                .build());
    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceB, defaultCombo, 1.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceC, defaultCombo, 3.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceD, defaultCombo, 20.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceE, defaultCombo, 20.0, 8.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceF, defaultCombo, 20.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateOrgUnitDataSet() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementA, periodA, sourceC, "3");
    useDataValue(dataElementA, periodA, sourceD, "5");
    useDataValue(dataElementA, periodA, sourceE, "7");
    useDataValue(dataElementA, periodA, sourceF, "9");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementB, periodA, sourceC, "4");
    useDataValue(dataElementB, periodA, sourceD, "6");
    useDataValue(dataElementB, periodA, sourceE, "8");
    useDataValue(dataElementB, periodA, sourceF, "10");

    DataSet dataSetA = createDataSet('A', ptMonthly);
    DataSet dataSetB = createDataSet('B', ptMonthly);
    DataSet dataSetC = createDataSet('C', ptMonthly);
    dataSetA.addOrganisationUnit(sourceB);
    dataSetA.addOrganisationUnit(sourceC);
    dataSetB.addOrganisationUnit(sourceD);
    dataSetB.addOrganisationUnit(sourceE);
    dataSetC.addOrganisationUnit(sourceE);
    dataSetC.addOrganisationUnit(sourceF);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);
    dataSetService.addDataSet(dataSetC);

    Expression expressionLeft =
        new Expression(
            "if(orgUnit.dataSet( "
                + dataSetA.getUid()
                + " ), #{"
                + dataElementA.getUid()
                + "}, 20)",
            "left");
    Expression expressionRight =
        new Expression(
            "if(orgUnit.dataSet( "
                + dataSetB.getUid()
                + " , "
                + dataSetC.getUid()
                + " ), #{"
                + dataElementB.getUid()
                + "}, 30)",
            "right");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);

    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceB, periodA)
                .withIncludeOrgUnitDescendants(true)
                .build());

    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceB, defaultCombo, 1.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceC, defaultCombo, 3.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceD, defaultCombo, 20.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceE, defaultCombo, 20.0, 8.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceF, defaultCombo, 20.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testValidateOrgUnitProgram() {
    useDataValue(dataElementA, periodA, sourceB, "1");
    useDataValue(dataElementA, periodA, sourceC, "3");
    useDataValue(dataElementA, periodA, sourceD, "5");
    useDataValue(dataElementA, periodA, sourceE, "7");
    useDataValue(dataElementA, periodA, sourceF, "9");
    useDataValue(dataElementB, periodA, sourceB, "2");
    useDataValue(dataElementB, periodA, sourceC, "4");
    useDataValue(dataElementB, periodA, sourceD, "6");
    useDataValue(dataElementB, periodA, sourceE, "8");
    useDataValue(dataElementB, periodA, sourceF, "10");

    Program programA = createProgram('A');
    Program programB = createProgram('B');
    Program programC = createProgram('C');
    programA.addOrganisationUnit(sourceB);
    programA.addOrganisationUnit(sourceC);
    programB.addOrganisationUnit(sourceD);
    programB.addOrganisationUnit(sourceE);
    programC.addOrganisationUnit(sourceE);
    programC.addOrganisationUnit(sourceF);
    programService.addProgram(programA);
    programService.addProgram(programB);
    programService.addProgram(programC);

    Expression expressionLeft =
        new Expression(
            "if(orgUnit.program( "
                + programA.getUid()
                + " ), #{"
                + dataElementA.getUid()
                + "}, 20)",
            "left");
    Expression expressionRight =
        new Expression(
            "if(orgUnit.program( "
                + programB.getUid()
                + " , "
                + programC.getUid()
                + " ), #{"
                + dataElementB.getUid()
                + "}, 30)",
            "right");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    validationRuleService.saveValidationRule(rule);

    Collection<ValidationResult> results =
        runValidationAnalysis(
            validationService
                .newParamsBuilder(dataSetMonthly, sourceB, periodA)
                .withIncludeOrgUnitDescendants(true)
                .build());

    Collection<ValidationResult> reference = new HashSet<>();
    reference.add(
        new ValidationResult(rule, periodA, sourceB, defaultCombo, 1.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceC, defaultCombo, 3.0, 30.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceD, defaultCombo, 20.0, 6.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceE, defaultCombo, 20.0, 8.0, dayInPeriodA));
    reference.add(
        new ValidationResult(rule, periodA, sourceF, defaultCombo, 20.0, 10.0, dayInPeriodA));
    assertResultsEquals(reference, results);
  }

  @Test
  void testInstructionTranslation() {
    User user = createUserAndInjectSecurityContext(true);
    Locale locale = Locale.FRENCH;
    CurrentUserUtil.setUserSetting(UserSettingKey.DB_LOCALE, locale);

    useDataValue(dataElementA, periodA, sourceA, "10");
    useDataValue(dataElementB, periodA, sourceA, "20");
    Expression expressionLeft =
        new Expression(
            "greatest( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )",
            "exprLeft");
    Expression expressionRight =
        new Expression(
            "least( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )",
            "exprRight");
    ValidationRule rule =
        createValidationRule("R", equal_to, expressionLeft, expressionRight, ptMonthly);
    rule.setInstruction("Validation rule instruction");
    validationRuleService.saveValidationRule(rule);
    String instructionTranslated = "Validation rule instruction translated";
    Set<Translation> listObjectTranslation = new HashSet<>(rule.getTranslations());
    listObjectTranslation.add(
        new Translation(locale.getLanguage(), "INSTRUCTION", instructionTranslated));
    identifiableObjectManager.updateTranslations(rule, listObjectTranslation);
    assertEquals(instructionTranslated, rule.getDisplayInstruction());
  }

  @Test
  void testGetValidationRuleExpressionDetails() {
    useDataValue(dataElementA, periodA, sourceA, "10");
    useDataValue(dataElementB, periodA, sourceA, "20", optionComboX, optionCombo);
    Expression expressionX =
        new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "exprX");
    Expression expressionY =
        new Expression(
            "#{" + dataElementB.getUid() + SEPARATOR + optionComboX.getUid() + "}", "exprY");
    ValidationRule rule = createValidationRule("A", equal_to, expressionX, expressionY, ptMonthly);
    validationRuleService.saveValidationRule(rule);
    List<Map<String, String>> leftSideExpected =
        Lists.newArrayList(
            ImmutableMap.of("name", "DataElementA", "value", "10.0"),
            ImmutableMap.of("name", "DataElementB", "value", "20.0"));
    List<Map<String, String>> rightSideExpected =
        Lists.newArrayList(
            ImmutableMap.of("name", "DataElementB CategoryOptionX", "value", "20.0"));
    ValidationRuleExpressionDetails details =
        validationService.getValidationRuleExpressionDetails(
            validationService
                .newParamsBuilder(Lists.newArrayList(rule), sourceA, Lists.newArrayList(periodA))
                .withAttributeOptionCombo(optionCombo)
                .build());
    assertEquals(leftSideExpected, details.getLeftSide());
    assertEquals(rightSideExpected, details.getRightSide());
  }

  private List<ValidationResult> runValidationAnalysis(ValidationAnalysisParams params) {
    return validationService.validationAnalysis(params, NoopJobProgress.INSTANCE);
  }

  private ValidationAnalysisParams createParamsMonthlySourceAPeriodA() {
    return validationService.newParamsBuilder(dataSetMonthly, sourceA, periodA).build();
  }
}
