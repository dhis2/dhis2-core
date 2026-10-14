/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createEnrollment;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createEvent;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataExportService;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueKey;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the ability of predictions to access event data through the real analytics service, using
 * analytics tables generated from aggregate and event data.
 *
 * <p>Each predictor predicts April and May from the two preceding months. The data are:
 *
 * <ul>
 *   <li>Enrollment: March (one tracked entity with an attribute value)
 *   <li>Event program data element X: March 10, April 20
 *   <li>Program indicator A (X * 10): March 100, April 200
 *   <li>Program indicator B (enrollment count, by enrollment date): March 1
 *   <li>Aggregate data element E: March 1000, April 2000
 * </ul>
 *
 * <p>Program B has events with sex (F or M), age (Child or Adult) and funder (1 or 2), which are
 * mapped to the categories Sex, Age and Funder (an attribute category.) Its events are:
 *
 * <ul>
 *   <li>March: F Child Funder 1 with 1 case, F Adult Funder 2 with 2 cases, M Adult Funder 1 with 4
 *       cases
 *   <li>April: F Child Funder 2 with 10 cases, M Child Funder 1 with 20 cases, M Adult Funder 2
 *       with 40 cases
 * </ul>
 *
 * <p>Its disaggregated program indicators, and aggregate data elements R and T, are:
 *
 * <ul>
 *   <li>Cases by Sex: March F 3, M 4; April F 10, M 60
 *   <li>Visits by Sex (event count): March F 2, M 1; April F 1, M 2
 *   <li>Cases by Sex and Age: March F Child 1, F Adult 2, M Adult 4; April F Child 10, M Child 20,
 *       M Adult 40
 *   <li>Cases by Sex with attribute Funder: March F Funder 1 1, F Funder 2 2, M Funder 1 4; April F
 *       Funder 2 10, M Funder 1 20, M Funder 2 40
 *   <li>Aggregate data element R by Sex: March F 1, M 3; April F 2, M 4
 *   <li>Aggregate data element T by Sex with attribute Funder: March F Funder 1 100, F Funder 2
 *       200, M Funder 1 300
 * </ul>
 *
 * <p>So the April prediction uses the March values, and the May prediction sums the March and April
 * values.
 *
 * @author Jim Grace
 */
@TestInstance(Lifecycle.PER_CLASS)
class AnalyticsPredictionServiceTest extends PostgresIntegrationTestBase {

  private static final String DATA_ELEMENT_S_UID = "DataElemenS";

  private static final String DATA_ELEMENT_Y_UID = "DataElemenY";

  private static final String DATA_ELEMENT_G_UID = "DataElemenG";

  private static final String DATA_ELEMENT_U_UID = "DataElemenU";

  private static final String PROGRAM_STAGE_B_UID = "ProgramStgB";

  @Autowired private PredictionService predictionService;

  @Autowired private TrackedEntityAttributeService entityAttributeService;

  @Autowired private TrackedEntityAttributeValueService entityAttributeValueService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private ProgramIndicatorService programIndicatorService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private PeriodService periodService;

  @Autowired private PeriodStore periodStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private DataExportService dataExportService;

  @Autowired private DataDumpService dataDumpService;

  @Autowired private AnalyticsTableGenerator analyticsTableGenerator;

  @Autowired private List<AnalyticsTableService> analyticsTableServices;

  @Autowired private IdentifiableObjectManager manager;

  private CategoryOptionCombo defaultCombo;

  private OrganisationUnit orgUnitA;

  private int testYear;

  private Period periodApr;

  private Period periodMay;

  private PeriodType periodTypeMonthly;

  private OrganisationUnitLevel orgUnitLevel1;

  private Predictor predictorA;

  private Predictor predictorD;

  private Predictor predictorI;

  private Predictor predictorAE;

  private Predictor predictorDE;

  private Predictor predictorIE;

  private Predictor predictorADIE;

  private CategoryOptionCombo cocFemale;

  private CategoryOptionCombo cocMale;

  private Predictor predictorCases;

  private Predictor predictorCombination;

  private Predictor predictorNonDisaggregated;

  private CategoryOptionCombo cocFemaleChild;

  private CategoryOptionCombo cocFemaleAdult;

  private CategoryOptionCombo cocMaleChild;

  private CategoryOptionCombo cocMaleAdult;

  private CategoryOptionCombo aocFunder1;

  private CategoryOptionCombo aocFunder2;

  private Predictor predictorSexAge;

  private Predictor predictorSexFromSexAge;

  private Predictor predictorFunder;

  @BeforeAll
  void setUp() {
    createPeriodTypes();
    periodStore.invalidateCache();
    PeriodType.invalidatePeriodCache();

    final String DATA_ELEMENT_E_UID = "DataElemenE";
    final String DATA_ELEMENT_X_UID = "DataElemenX";
    final String TRACKED_ENTITY_ATTRIBUTE_UID = "TEAttribute";
    final String PROGRAM_UID = "ProgramUidA";
    final String PROGRAM_STAGE_UID = "ProgramStgA";
    final String PROGRAM_INDICATOR_A_UID = "ProgramIndA";
    final String PROGRAM_INDICATOR_B_UID = "ProgramIndB";
    final String PROGRAM_B_UID = "ProgramUidB";
    final String DATA_ELEMENT_R_UID = "DataElemenR";
    final String PROGRAM_INDICATOR_CASES_UID = "ProgramIndC";
    final String PROGRAM_INDICATOR_VISITS_UID = "ProgramIndV";
    final String PROGRAM_INDICATOR_SEX_AGE_UID = "ProgramIndG";
    final String PROGRAM_INDICATOR_FUNDER_UID = "ProgramIndF";
    final String DATA_ELEMENT_T_UID = "DataElemenT";
    final String SEX_MAPPING_UID = "ProgCatMapS";
    final String AGE_MAPPING_UID = "ProgCatMapA";
    final String FUNDER_MAPPING_UID = "ProgCatMapF";

    // A - ProgramTrackedEntityAttribute
    final String EXPRESSION_A =
        "sum( A{" + PROGRAM_UID + SEPARATOR + TRACKED_ENTITY_ATTRIBUTE_UID + "} )";
    // D - ProgramDataElement
    final String EXPRESSION_D = "sum( D{" + PROGRAM_UID + SEPARATOR + DATA_ELEMENT_X_UID + "} )";
    // I - ProgramIndicators
    final String EXPRESSION_I =
        "sum( I{" + PROGRAM_INDICATOR_A_UID + "} + I{" + PROGRAM_INDICATOR_B_UID + "} )";
    // E - Aggregate data element
    final String EXPRESSION_E = "sum( #{" + DATA_ELEMENT_E_UID + "} )";

    // Program Indicator A expression
    final String EX_INDICATOR_A =
        "#{" + PROGRAM_STAGE_UID + SEPARATOR + DATA_ELEMENT_X_UID + "} * 10";
    // Program Indicator B expression
    final String EX_INDICATOR_B = "V{enrollment_count}";

    defaultCombo = categoryService.getDefaultCategoryOptionCombo();
    orgUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(orgUnitA);
    orgUnitLevel1 = new OrganisationUnitLevel(1, "Level1");
    organisationUnitService.addOrganisationUnitLevel(orgUnitLevel1);
    Set<OrganisationUnit> orgUnitASet = Sets.newHashSet(orgUnitA);

    periodTypeMonthly = new MonthlyPeriodType();
    testYear = Calendar.getInstance().get(Calendar.YEAR) - 1;
    Period periodMar =
        createPeriod(periodTypeMonthly, getDate(testYear, 3, 1), getDate(testYear, 3, 31));
    periodApr = createPeriod(periodTypeMonthly, getDate(testYear, 4, 1), getDate(testYear, 4, 30));
    periodMay = createPeriod(periodTypeMonthly, getDate(testYear, 5, 1), getDate(testYear, 5, 31));
    periodService.addPeriod(periodMar);
    periodService.addPeriod(periodApr);
    periodService.addPeriod(periodMay);
    Date dateMar15 = getDate(testYear, 3, 15);
    Date dateApr15 = getDate(testYear, 4, 15);

    // Data elements
    DataElement dataElementE = createDataElement('E');
    dataElementE.setUid(DATA_ELEMENT_E_UID);
    dataElementService.addDataElement(dataElementE);
    DataElement dataElementX =
        createDataElement('X', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER);
    dataElementX.setUid(DATA_ELEMENT_X_UID);
    dataElementService.addDataElement(dataElementX);

    // Tracked entity with an attribute value
    TrackedEntityAttribute entityAttribute = createTrackedEntityAttribute('A');
    entityAttribute.setAggregationType(AggregationType.COUNT);
    entityAttribute.setUid(TRACKED_ENTITY_ATTRIBUTE_UID);
    entityAttributeService.addTrackedEntityAttribute(entityAttribute);
    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntity =
        createTrackedEntity('A', orgUnitA, entityAttribute, trackedEntityType);
    manager.save(trackedEntity);
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        new TrackedEntityAttributeValue(entityAttribute, trackedEntity);
    trackedEntityAttributeValue.setValue("123");
    entityAttributeValueService.addTrackedEntityAttributeValue(trackedEntityAttributeValue);
    trackedEntity.setTrackedEntityAttributeValues(Sets.newHashSet(trackedEntityAttributeValue));
    manager.update(trackedEntity);

    // Program, program stage and program indicators
    Program program = createProgram('A', null, Sets.newHashSet(entityAttribute), orgUnitASet);
    program.setUid(PROGRAM_UID);
    programService.addProgram(program);
    ProgramStage stageA = createProgramStage('A', 0);
    stageA.setUid(PROGRAM_STAGE_UID);
    stageA.setProgram(program);
    stageA.addDataElement(dataElementX, 1);
    programStageService.saveProgramStage(stageA);
    ProgramIndicator programIndicatorA =
        createProgramIndicator('A', AnalyticsType.EVENT, program, EX_INDICATOR_A, null);
    programIndicatorA.setAggregationType(AggregationType.SUM);
    programIndicatorA.setUid(PROGRAM_INDICATOR_A_UID);
    programIndicatorService.addProgramIndicator(programIndicatorA);
    ProgramIndicator programIndicatorB =
        createProgramIndicator('B', AnalyticsType.ENROLLMENT, program, EX_INDICATOR_B, null);
    programIndicatorB.setAggregationType(AggregationType.COUNT);
    programIndicatorB.setUid(PROGRAM_INDICATOR_B_UID);
    programIndicatorService.addProgramIndicator(programIndicatorB);
    program.setProgramStages(Sets.newHashSet(stageA));
    program.getProgramIndicators().add(programIndicatorA);
    program.getProgramIndicators().add(programIndicatorB);
    programService.updateProgram(program);

    // Enrollment in March
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnitA);
    enrollment.setEnrollmentDate(dateMar15);
    enrollment.setOccurredDate(dateMar15);
    manager.save(enrollment);
    trackedEntity.getEnrollments().add(enrollment);
    manager.update(trackedEntity);

    // Events: X = 10 in March, X = 20 in April
    TrackerEvent eventMar = createEvent(stageA, enrollment, orgUnitA);
    eventMar.setOccurredDate(dateMar15);
    eventMar.setAttributeOptionCombo(defaultCombo);
    eventMar.setEventDataValues(Set.of(new EventDataValue(DATA_ELEMENT_X_UID, "10")));
    manager.save(eventMar);
    TrackerEvent eventApr = createEvent(stageA, enrollment, orgUnitA);
    eventApr.setOccurredDate(dateApr15);
    eventApr.setAttributeOptionCombo(defaultCombo);
    eventApr.setEventDataValues(Set.of(new EventDataValue(DATA_ELEMENT_X_UID, "20")));
    manager.save(eventApr);

    // Categories Sex and Age for disaggregation, and Funder for attributes
    CategoryOption coFemale = newCategoryOption("Female", "CatOptFemal");
    CategoryOption coMale = newCategoryOption("Male", "CatOptMale1");
    CategoryOption coChild = newCategoryOption("Child", "CatOptChild");
    CategoryOption coAdult = newCategoryOption("Adult", "CatOptAdult");
    CategoryOption coFunder1 = newCategoryOption("Funder 1", "CatOptFund1");
    CategoryOption coFunder2 = newCategoryOption("Funder 2", "CatOptFund2");
    Category catSex = newCategory("Sex", "CategorySex", DISAGGREGATION, coFemale, coMale);
    Category catAge = newCategory("Age", "CategoryAge", DISAGGREGATION, coChild, coAdult);
    Category catFunder = newCategory("Funder", "CategoryFnd", ATTRIBUTE, coFunder1, coFunder2);
    CategoryCombo ccSex = newCategoryCombo("Sex", "CatComboSex", DISAGGREGATION, catSex);
    CategoryCombo ccSexAge =
        newCategoryCombo("Sex and Age", "CatComboSxA", DISAGGREGATION, catSex, catAge);
    CategoryCombo ccFunder = newCategoryCombo("Funder", "CatComboFnd", ATTRIBUTE, catFunder);
    cocFemale = newCategoryOptionCombo("Female", "CatOptCombF", ccSex, coFemale);
    cocMale = newCategoryOptionCombo("Male", "CatOptCombM", ccSex, coMale);
    cocFemaleChild =
        newCategoryOptionCombo("Female, Child", "CocFemChild", ccSexAge, coFemale, coChild);
    cocFemaleAdult =
        newCategoryOptionCombo("Female, Adult", "CocFemAdult", ccSexAge, coFemale, coAdult);
    cocMaleChild = newCategoryOptionCombo("Male, Child", "CocMalChild", ccSexAge, coMale, coChild);
    cocMaleAdult = newCategoryOptionCombo("Male, Adult", "CocMalAdult", ccSexAge, coMale, coAdult);
    aocFunder1 = newCategoryOptionCombo("Funder 1", "AocFunder01", ccFunder, coFunder1);
    aocFunder2 = newCategoryOptionCombo("Funder 2", "AocFunder02", ccFunder, coFunder2);

    // Program B data elements: cases (Y), sex (S), age (G) and funder (U)
    DataElement dataElementY =
        createDataElement('Y', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER);
    dataElementY.setUid(DATA_ELEMENT_Y_UID);
    dataElementService.addDataElement(dataElementY);
    DataElement dataElementS = newTextTrackerDataElement('S', DATA_ELEMENT_S_UID);
    DataElement dataElementG = newTextTrackerDataElement('G', DATA_ELEMENT_G_UID);
    DataElement dataElementU = newTextTrackerDataElement('U', DATA_ELEMENT_U_UID);

    // Program B, mapping the sex, age and funder data elements to their categories
    Program programB = createProgram('B', null, Sets.newHashSet(), orgUnitASet);
    programB.setUid(PROGRAM_B_UID);
    programB.setCategoryMappings(
        Set.of(
            newCategoryMapping(
                SEX_MAPPING_UID, catSex, DATA_ELEMENT_S_UID, Map.of(coFemale, "F", coMale, "M")),
            newCategoryMapping(
                AGE_MAPPING_UID,
                catAge,
                DATA_ELEMENT_G_UID,
                Map.of(coChild, "Child", coAdult, "Adult")),
            newCategoryMapping(
                FUNDER_MAPPING_UID,
                catFunder,
                DATA_ELEMENT_U_UID,
                Map.of(coFunder1, "1", coFunder2, "2"))));
    programService.addProgram(programB);
    ProgramStage stageB = createProgramStage('B', 0);
    stageB.setUid(PROGRAM_STAGE_B_UID);
    stageB.setProgram(programB);
    stageB.addDataElement(dataElementY, 1);
    stageB.addDataElement(dataElementS, 2);
    stageB.addDataElement(dataElementG, 3);
    stageB.addDataElement(dataElementU, 4);
    programStageService.saveProgramStage(stageB);
    final String EX_CASES = "#{" + PROGRAM_STAGE_B_UID + SEPARATOR + DATA_ELEMENT_Y_UID + "}";
    programB.setProgramStages(Sets.newHashSet(stageB));
    programB
        .getProgramIndicators()
        .addAll(
            List.of(
                newDisaggregatedProgramIndicator(
                    'C',
                    PROGRAM_INDICATOR_CASES_UID,
                    programB,
                    EX_CASES,
                    ccSex,
                    defaultCombo.getCategoryCombo(),
                    SEX_MAPPING_UID),
                newDisaggregatedProgramIndicator(
                    'V',
                    PROGRAM_INDICATOR_VISITS_UID,
                    programB,
                    "V{event_count}",
                    ccSex,
                    defaultCombo.getCategoryCombo(),
                    SEX_MAPPING_UID),
                newDisaggregatedProgramIndicator(
                    'G',
                    PROGRAM_INDICATOR_SEX_AGE_UID,
                    programB,
                    EX_CASES,
                    ccSexAge,
                    defaultCombo.getCategoryCombo(),
                    SEX_MAPPING_UID,
                    AGE_MAPPING_UID),
                newDisaggregatedProgramIndicator(
                    'F',
                    PROGRAM_INDICATOR_FUNDER_UID,
                    programB,
                    EX_CASES,
                    ccSex,
                    ccFunder,
                    SEX_MAPPING_UID,
                    FUNDER_MAPPING_UID)));
    programService.updateProgram(programB);

    // Enrollment in program B in March
    Enrollment enrollmentB = createEnrollment(programB, trackedEntity, orgUnitA);
    enrollmentB.setEnrollmentDate(getDate(testYear, 3, 1));
    enrollmentB.setOccurredDate(getDate(testYear, 3, 1));
    manager.save(enrollmentB);
    trackedEntity.getEnrollments().add(enrollmentB);
    manager.update(trackedEntity);

    // Program B events (sex, age, funder, cases)
    addEventB(stageB, enrollmentB, getDate(testYear, 3, 10), "F", "Child", "1", "1");
    addEventB(stageB, enrollmentB, getDate(testYear, 3, 11), "F", "Adult", "2", "2");
    addEventB(stageB, enrollmentB, getDate(testYear, 3, 12), "M", "Adult", "1", "4");
    addEventB(stageB, enrollmentB, getDate(testYear, 4, 10), "F", "Child", "2", "10");
    addEventB(stageB, enrollmentB, getDate(testYear, 4, 11), "M", "Child", "1", "20");
    addEventB(stageB, enrollmentB, getDate(testYear, 4, 12), "M", "Adult", "2", "40");

    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();

    // Aggregate data element R by Sex, and T by Sex with attribute Funder
    DataElement dataElementR = createDataElement('R', ccSex);
    dataElementR.setUid(DATA_ELEMENT_R_UID);
    dataElementService.addDataElement(dataElementR);
    DataElement dataElementT = createDataElement('T', ccSex);
    dataElementT.setUid(DATA_ELEMENT_T_UID);
    dataElementService.addDataElement(dataElementT);

    // Aggregate data: E = 1000 in March, 2000 in April
    addDataValues(
        createDataValue(dataElementE, periodMar, orgUnitA, defaultCombo, defaultCombo, "1000"),
        createDataValue(dataElementE, periodApr, orgUnitA, defaultCombo, defaultCombo, "2000"));

    // Aggregate data: R = F 1, M 3 in March; F 2, M 4 in April
    addDataValues(
        createDataValue(dataElementR, periodMar, orgUnitA, cocFemale, defaultCombo, "1"),
        createDataValue(dataElementR, periodMar, orgUnitA, cocMale, defaultCombo, "3"),
        createDataValue(dataElementR, periodApr, orgUnitA, cocFemale, defaultCombo, "2"),
        createDataValue(dataElementR, periodApr, orgUnitA, cocMale, defaultCombo, "4"));

    // Aggregate data: T = F Funder 1 100, F Funder 2 200, M Funder 1 300 in March
    addDataValues(
        createDataValue(dataElementT, periodMar, orgUnitA, cocFemale, aocFunder1, "100"),
        createDataValue(dataElementT, periodMar, orgUnitA, cocFemale, aocFunder2, "200"),
        createDataValue(dataElementT, periodMar, orgUnitA, cocMale, aocFunder1, "300"));

    // Predictors
    predictorA = newPredictor('A', EXPRESSION_A);
    predictorD = newPredictor('D', EXPRESSION_D);
    predictorI = newPredictor('I', EXPRESSION_I);
    predictorAE = newPredictor('1', EXPRESSION_A + " + " + EXPRESSION_E);
    predictorDE = newPredictor('2', EXPRESSION_D + " + " + EXPRESSION_E);
    predictorIE = newPredictor('3', EXPRESSION_I + " + " + EXPRESSION_E);
    predictorADIE =
        newPredictor(
            '4', EXPRESSION_A + " + " + EXPRESSION_D + " + " + EXPRESSION_I + " + " + EXPRESSION_E);

    // Disaggregated predictors (output data elements disaggregated by sex)
    final String EXPRESSION_CASES = "sum( I{" + PROGRAM_INDICATOR_CASES_UID + "} )";
    final String EXPRESSION_VISITS = "sum( I{" + PROGRAM_INDICATOR_VISITS_UID + "} )";
    final String EXPRESSION_R = "sum( #{" + DATA_ELEMENT_R_UID + "} )";
    predictorCases = newDisaggregatedPredictor('5', ccSex, EXPRESSION_CASES);
    predictorCombination =
        newDisaggregatedPredictor(
            '6',
            ccSex,
            "( "
                + EXPRESSION_CASES
                + " + "
                + EXPRESSION_R
                + " ) * "
                + EXPRESSION_VISITS
                + " + "
                + EXPRESSION_E
                + " / 100");
    predictorNonDisaggregated =
        newDisaggregatedPredictor('7', ccSex, "sum( I{" + PROGRAM_INDICATOR_A_UID + "} )");
    final String EXPRESSION_SEX_AGE = "sum( I{" + PROGRAM_INDICATOR_SEX_AGE_UID + "} )";
    predictorSexAge = newDisaggregatedPredictor('8', ccSexAge, EXPRESSION_SEX_AGE);
    predictorSexFromSexAge = newDisaggregatedPredictor('9', ccSex, EXPRESSION_SEX_AGE);
    predictorFunder =
        newDisaggregatedPredictor(
            'F',
            ccSex,
            "sum( I{"
                + PROGRAM_INDICATOR_FUNDER_UID
                + "} ) + sum( #{"
                + DATA_ELEMENT_T_UID
                + "} )");

    User user = createAndAddUser(true, "analyticsUser", orgUnitASet, orgUnitASet);
    injectSecurityContextUser(user);

    // Start the analytics tables generation in the future, so that the data just created (whose
    // last updated times are stored to the millisecond) is seen as in the past.
    Date oneSecondFromNow =
        Date.from(LocalDateTime.now().plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());
    analyticsTableGenerator.generateAnalyticsTables(
        AnalyticsTableUpdateParams.newBuilder().startTime(oneSecondFromNow).build(),
        JobProgress.noop());
  }

  @AfterAll
  void tearDown() {
    cleanPeriodTypes();
    for (AnalyticsTableService service : analyticsTableServices) {
      service.dropTables();
    }
  }

  /**
   * Makes a predictor with its own output data element, predicting from the two previous months.
   *
   * @param uniqueCharacter unique character for the predictor and its output data element
   * @param expression the predictor generator expression
   * @return the predictor
   */
  private Predictor newPredictor(char uniqueCharacter, String expression) {
    DataElement output = createDataElement(uniqueCharacter);
    dataElementService.addDataElement(output);
    return createPredictor(
        output,
        defaultCombo,
        String.valueOf(uniqueCharacter),
        new Expression(expression, "Expression " + uniqueCharacter),
        null,
        periodTypeMonthly,
        orgUnitLevel1,
        2,
        0,
        0);
  }

  /**
   * Makes a predictor with its own disaggregated output data element, and no output category option
   * combo, so it makes a prediction for each category option combo of the output data element.
   */
  private Predictor newDisaggregatedPredictor(
      char uniqueCharacter, CategoryCombo categoryCombo, String expression) {
    DataElement output = createDataElement(uniqueCharacter, categoryCombo);
    dataElementService.addDataElement(output);
    return createPredictor(
        output,
        null,
        String.valueOf(uniqueCharacter),
        new Expression(expression, "Expression " + uniqueCharacter),
        null,
        periodTypeMonthly,
        orgUnitLevel1,
        2,
        0,
        0);
  }

  /** Makes and saves an event program indicator with a category combo and attribute combo. */
  private ProgramIndicator newDisaggregatedProgramIndicator(
      char uniqueCharacter,
      String uid,
      Program program,
      String expression,
      CategoryCombo categoryCombo,
      CategoryCombo attributeCombo,
      String... categoryMappingUids) {
    ProgramIndicator pi =
        createProgramIndicator(uniqueCharacter, AnalyticsType.EVENT, program, expression, null);
    pi.setAggregationType(AggregationType.SUM);
    pi.setUid(uid);
    pi.setCategoryCombo(categoryCombo);
    pi.setAttributeCombo(attributeCombo);
    pi.setCategoryMappingIds(Set.of(categoryMappingUids));
    programIndicatorService.addProgramIndicator(pi);
    return pi;
  }

  /** Makes and saves a text tracker data element. */
  private DataElement newTextTrackerDataElement(char uniqueCharacter, String uid) {
    DataElement de =
        createDataElement(
            uniqueCharacter, ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER);
    de.setUid(uid);
    dataElementService.addDataElement(de);
    return de;
  }

  /** Makes and saves a category option. */
  private CategoryOption newCategoryOption(String name, String uid) {
    CategoryOption co = createCategoryOption(name, uid);
    categoryService.addCategoryOption(co);
    return co;
  }

  /** Makes and saves a category. */
  private Category newCategory(
      String name, String uid, DataDimensionType type, CategoryOption... options) {
    Category category = createCategory(name, uid, options);
    category.setDataDimensionType(type);
    categoryService.addCategory(category);
    return category;
  }

  /** Makes and saves a category combo. */
  private CategoryCombo newCategoryCombo(
      String name, String uid, DataDimensionType type, Category... categories) {
    CategoryCombo cc = createCategoryCombo(name, uid, categories);
    cc.setDataDimensionType(type);
    categoryService.addCategoryCombo(cc);
    return cc;
  }

  /** Makes and saves a category option combo, adding it to its category combo and options. */
  private CategoryOptionCombo newCategoryOptionCombo(
      String name, String uid, CategoryCombo cc, CategoryOption... options) {
    CategoryOptionCombo coc = createCategoryOptionCombo(name, uid, cc, options);
    categoryService.addCategoryOptionCombo(coc);
    cc.getOptionCombos().add(coc);
    categoryService.updateCategoryCombo(cc);
    for (CategoryOption co : options) {
      co.getCategoryOptionCombos().add(coc);
      categoryService.updateCategoryOption(co);
    }
    return coc;
  }

  /**
   * Makes a program category mapping, mapping each category option from a program stage B data
   * element value.
   */
  private ProgramCategoryMapping newCategoryMapping(
      String id, Category category, String dataElementUid, Map<CategoryOption, String> values) {
    return ProgramCategoryMapping.builder()
        .id(id)
        .categoryId(category.getUid())
        .mappingName(category.getName() + " mapping")
        .optionMappings(
            values.entrySet().stream()
                .map(
                    e ->
                        ProgramCategoryOptionMapping.builder()
                            .optionId(e.getKey().getUid())
                            .filter(
                                "#{"
                                    + PROGRAM_STAGE_B_UID
                                    + SEPARATOR
                                    + dataElementUid
                                    + "} == '"
                                    + e.getValue()
                                    + "'")
                            .build())
                .toList())
        .build();
  }

  /** Adds a program B event with a sex, age, funder and number of cases. */
  private void addEventB(
      ProgramStage stageB,
      Enrollment enrollmentB,
      Date date,
      String sex,
      String age,
      String funder,
      String cases) {
    TrackerEvent event = createEvent(stageB, enrollmentB, orgUnitA);
    event.setOccurredDate(date);
    event.setAttributeOptionCombo(defaultCombo);
    event.setEventDataValues(
        Set.of(
            new EventDataValue(DATA_ELEMENT_S_UID, sex),
            new EventDataValue(DATA_ELEMENT_G_UID, age),
            new EventDataValue(DATA_ELEMENT_U_UID, funder),
            new EventDataValue(DATA_ELEMENT_Y_UID, cases)));
    manager.save(event);
  }

  /**
   * Gets a data value from the database.
   *
   * @param dataElement element of value to get
   * @param period period of value to get
   * @return the value
   */
  private String getDataValue(DataElement dataElement, Period period) {
    return getDataValue(dataElement, period, defaultCombo, defaultCombo);
  }

  /**
   * Gets a data value from the database.
   *
   * @param dataElement element of value to get
   * @param period period of value to get
   * @param coc category option combo of value to get
   * @param aoc attribute option combo of value to get
   * @return the value
   */
  private String getDataValue(
      DataElement dataElement, Period period, CategoryOptionCombo coc, CategoryOptionCombo aoc) {
    DataValueKey key = new DataValueKey(dataElement, period, orgUnitA, coc, aoc);
    try {
      DataExportValue dv = dataExportService.exportValue(key);
      if (dv != null) return dv.value();
    } catch (ConflictException e) {
      fail("Failed to read data value: " + key, e);
    }
    return null;
  }

  /** Runs a predictor for April and May. */
  private void predict(Predictor predictor) {
    predictionService.predict(
        predictor, getDate(testYear, 4, 1), getDate(testYear, 5, 31), new PredictionSummary());
  }

  /** Runs a predictor for April and May, and checks the predicted values. */
  private void assertPredictions(Predictor predictor, String expectedApr, String expectedMay) {
    predictionService.predict(
        predictor, getDate(testYear, 4, 1), getDate(testYear, 5, 31), new PredictionSummary());
    assertEquals(expectedApr, getDataValue(predictor.getOutput(), periodApr));
    assertEquals(expectedMay, getDataValue(predictor.getOutput(), periodMay));
  }

  /** Runs a disaggregated predictor for April and May, and checks the predicted values by sex. */
  private void assertSexPredictions(
      Predictor predictor, String aprFemale, String aprMale, String mayFemale, String mayMale) {
    predictionService.predict(
        predictor, getDate(testYear, 4, 1), getDate(testYear, 5, 31), new PredictionSummary());
    assertEquals(
        aprFemale,
        getDataValue(predictor.getOutput(), periodApr, cocFemale, defaultCombo),
        "Apr F");
    assertEquals(
        aprMale, getDataValue(predictor.getOutput(), periodApr, cocMale, defaultCombo), "Apr M");
    assertEquals(
        mayFemale,
        getDataValue(predictor.getOutput(), periodMay, cocFemale, defaultCombo),
        "May F");
    assertEquals(
        mayMale, getDataValue(predictor.getOutput(), periodMay, cocMale, defaultCombo), "May M");
  }

  /** Tests predicting from a program tracked entity attribute (event count). */
  @Test
  void testPredictProgramTrackedEntityAttribute() {
    assertPredictions(predictorA, "1", "2");
  }

  /** Tests predicting from a program data element. */
  @Test
  void testPredictProgramDataElement() {
    assertPredictions(predictorD, "10", "30");
  }

  /** Tests predicting from program indicators. */
  @Test
  void testPredictProgramIndicators() {
    assertPredictions(predictorI, "101", "301");
  }

  /**
   * Tests predicting from both a program tracked entity attribute and an aggregate data element.
   */
  @Test
  void testPredictEventAndDataElement() {
    assertPredictions(predictorAE, "1001", "3002");
  }

  /** Tests predicting from both a program data element and an aggregate data element. */
  @Test
  void testPredictProgramDataElementAndDataElement() {
    assertPredictions(predictorDE, "1010", "3030");
  }

  /** Tests predicting from both program indicators and an aggregate data element. */
  @Test
  void testPredictProgramIndicatorsAndDataElement() {
    assertPredictions(predictorIE, "1101", "3301");
  }

  /**
   * Tests predicting from a program tracked entity attribute, a program data element, program
   * indicators and an aggregate data element.
   */
  @Test
  void testPredictAllEventItemsAndDataElement() {
    assertPredictions(predictorADIE, "1112", "3333");
  }

  /** Tests predicting by sex from a program indicator disaggregated by sex. */
  @Test
  void testPredictDisaggregatedProgramIndicator() {
    assertSexPredictions(predictorCases, "3", "4", "13", "64");
  }

  /**
   * Tests predicting by sex from two program indicators disaggregated by sex, an aggregate data
   * element disaggregated by sex, and an aggregate data element that is not disaggregated.
   *
   * <p>( cases + R ) * visits + E / 100:
   *
   * <ul>
   *   <li>April F: ( 3 + 1 ) * 2 + 1000 / 100 = 18
   *   <li>April M: ( 4 + 3 ) * 1 + 1000 / 100 = 17
   *   <li>May F: ( 13 + 3 ) * 3 + 3000 / 100 = 78
   *   <li>May M: ( 64 + 7 ) * 3 + 3000 / 100 = 243
   * </ul>
   */
  @Test
  void testPredictDisaggregatedProgramIndicatorsAndDataElements() {
    assertSexPredictions(predictorCombination, "18", "17", "78", "243");
  }

  /**
   * Tests predicting by sex from a program indicator that is not disaggregated, so the whole value
   * is predicted for each sex.
   */
  @Test
  void testPredictDisaggregatedFromNonDisaggregatedProgramIndicator() {
    assertSexPredictions(predictorNonDisaggregated, "100", "100", "300", "300");
  }

  /**
   * Tests predicting by sex and age from a program indicator disaggregated by sex and age. There is
   * no data for M Child in March, so there is no prediction for M Child in April.
   */
  @Test
  void testPredictTwoCategoryProgramIndicator() {
    predict(predictorSexAge);
    DataElement output = predictorSexAge.getOutput();
    assertEquals("1", getDataValue(output, periodApr, cocFemaleChild, defaultCombo), "Apr F C");
    assertEquals("2", getDataValue(output, periodApr, cocFemaleAdult, defaultCombo), "Apr F A");
    assertNull(getDataValue(output, periodApr, cocMaleChild, defaultCombo), "Apr M C");
    assertEquals("4", getDataValue(output, periodApr, cocMaleAdult, defaultCombo), "Apr M A");
    assertEquals("11", getDataValue(output, periodMay, cocFemaleChild, defaultCombo), "May F C");
    assertEquals("2", getDataValue(output, periodMay, cocFemaleAdult, defaultCombo), "May F A");
    assertEquals("20", getDataValue(output, periodMay, cocMaleChild, defaultCombo), "May M C");
    assertEquals("44", getDataValue(output, periodMay, cocMaleAdult, defaultCombo), "May M A");
  }

  /**
   * Tests predicting by sex from a program indicator disaggregated by sex and age, so the ages are
   * summed for each sex.
   */
  @Test
  void testPredictOneCategoryFromTwoCategoryProgramIndicator() {
    assertSexPredictions(predictorSexFromSexAge, "3", "4", "13", "64");
  }

  /**
   * Tests predicting by sex and funder (attribute) from a program indicator disaggregated by sex
   * with attribute funder, plus an aggregate data element disaggregated by sex with attribute
   * funder. There is no data for M Funder 2 in March, so there is no prediction for M Funder 2 in
   * April.
   */
  @Test
  void testPredictCategoryAndAttributeProgramIndicatorAndDataElement() {
    predict(predictorFunder);
    DataElement output = predictorFunder.getOutput();
    assertEquals("101", getDataValue(output, periodApr, cocFemale, aocFunder1), "Apr F 1");
    assertEquals("304", getDataValue(output, periodApr, cocMale, aocFunder1), "Apr M 1");
    assertEquals("202", getDataValue(output, periodApr, cocFemale, aocFunder2), "Apr F 2");
    assertNull(getDataValue(output, periodApr, cocMale, aocFunder2), "Apr M 2");
    assertEquals("101", getDataValue(output, periodMay, cocFemale, aocFunder1), "May F 1");
    assertEquals("324", getDataValue(output, periodMay, cocMale, aocFunder1), "May M 1");
    assertEquals("212", getDataValue(output, periodMay, cocFemale, aocFunder2), "May F 2");
    assertEquals("40", getDataValue(output, periodMay, cocMale, aocFunder2), "May M 2");
    assertNull(getDataValue(output, periodApr, cocFemale, defaultCombo), "Apr F default");
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValuesForJdbcTest(values) < values.length)
      fail("Failed to upsert test data");
  }
}
