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
package org.hisp.dhis.analytics.event.data;

import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MARCH;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.AggregationType.AVERAGE;
import static org.hisp.dhis.analytics.AggregationType.CUSTOM;
import static org.hisp.dhis.analytics.AggregationType.FIRST;
import static org.hisp.dhis.analytics.AggregationType.FIRST_AVERAGE_ORG_UNIT;
import static org.hisp.dhis.analytics.AggregationType.LAST;
import static org.hisp.dhis.analytics.AggregationType.LAST_AVERAGE_ORG_UNIT;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.MULTI_TEXT;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeByName;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.AnalyticsType.EVENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.option.OptionStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramOwnershipHistory;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests event and enrollment analytics services.
 *
 * @author Henning Haakonsen
 * @author Jim Grace (nearly complete rewrite)
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@Order(1) // must run before other tests in analytics package (for some unknown reason)
class EventAnalyticsServiceTest extends PostgresIntegrationTestBase {
  @Autowired private EventQueryService eventQueryTarget;

  @Autowired private EventAggregateService eventAggregateService;

  @Autowired private EnrollmentQueryService enrollmentQueryTarget;

  @Autowired private List<AnalyticsTableService> analyticsTableServices;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OptionStore optionStore;

  @Autowired private OptionService optionService;

  @Autowired private AnalyticsTableGenerator analyticsTableGenerator;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ProgramOwnershipHistoryService programOwnershipHistoryService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired private CategoryService categoryService;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnit ouD;

  private OrganisationUnit ouE;

  private OrganisationUnit ouF;

  private OrganisationUnit ouG;

  private OrganisationUnit ouH;

  private OrganisationUnit ouI;

  private OrganisationUnit ouJ;

  private OrganisationUnit ouK;

  private OrganisationUnit ouL;

  private OrganisationUnit ouM;

  private OrganisationUnit ouN;

  private List<OrganisationUnit> level3Ous;

  // Note: The periods are not persisted. They don't need to be for event
  // analytics, so the tests should work without them being persisted.
  private PeriodDimension peJan = new PeriodDimension(createPeriod("2017-01"));

  private PeriodDimension peFeb = new PeriodDimension(createPeriod("2017-02"));

  private PeriodDimension peMar = new PeriodDimension(createPeriod("2017-03"));

  private CategoryOption coA;

  private CategoryOption coB;

  private CategoryOption coC;

  private CategoryOption coD;

  private Category caA;

  private Category caB;

  private CategoryCombo ccA;

  private CategoryOptionCombo cocAC;

  private CategoryOptionCombo cocAD;

  private CategoryOptionCombo cocBC;

  private CategoryOptionCombo cocBD;

  private DataElement deA;

  private DataElement deB;

  private DataElement deM;

  private DataElement deU;

  private ProgramCategoryMapping cmA;

  private ProgramCategoryMapping cmB;

  private TrackedEntityAttribute atU;

  private Program programA;

  private Program programB;

  private ProgramStage psA;

  private User userA;

  @BeforeAll
  void setUp() throws ConflictException {
    // Organisation Units
    //
    // A -> B -> D,E,F,G
    // A -> C -> H,I,J,K,L,M,N
    //
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);
    ouC = createOrganisationUnit('C', ouA);
    ouC.setOpeningDate(getDate(2016, 4, 10));
    ouD = createOrganisationUnit('D', ouB);
    ouD.setOpeningDate(getDate(2016, 12, 10));
    ouE = createOrganisationUnit('E', ouB);
    ouF = createOrganisationUnit('F', ouB);
    ouG = createOrganisationUnit('G', ouB);
    ouH = createOrganisationUnit('H', ouC);
    ouI = createOrganisationUnit('I', ouC);
    ouJ = createOrganisationUnit('J', ouC);
    ouK = createOrganisationUnit('K', ouC);
    ouL = createOrganisationUnit('L', ouC);
    ouM = createOrganisationUnit('M', ouC);
    ouN = createOrganisationUnit('N', ouC);

    manager.save(ouA);
    manager.save(ouB);
    manager.save(ouC);
    manager.save(ouD);
    manager.save(ouE);
    manager.save(ouF);
    manager.save(ouG);
    manager.save(ouH);
    manager.save(ouI);
    manager.save(ouJ);
    manager.save(ouK);
    manager.save(ouL);
    manager.save(ouM);
    manager.save(ouN);

    level3Ous = organisationUnitService.getOrganisationUnitsAtLevel(3);

    // Organisation Unit Levels
    OrganisationUnitLevel ou1 = new OrganisationUnitLevel(1, "Ou Level 1");
    ou1.getSharing().setPublicAccess("--------");
    OrganisationUnitLevel ou2 = new OrganisationUnitLevel(2, "Ou Level 2");
    ou2.getSharing().setPublicAccess("--------");
    OrganisationUnitLevel ou3 = new OrganisationUnitLevel(3, "Ou Level 3");
    ou3.getSharing().setPublicAccess("--------");
    manager.save(ou1);
    manager.save(ou2);
    manager.save(ou3);

    // Category Options
    coA = createCategoryOption('A');
    coB = createCategoryOption('B');
    coC = createCategoryOption('C');
    coD = createCategoryOption('D');
    coA.setUid("cataOptionA");
    coB.setUid("cataOptionB");
    coC.setUid("cataOptionC");
    coD.setUid("cataOptionD");
    manager.save(coA);
    manager.save(coB);
    manager.save(coC);
    manager.save(coD);

    // Categories
    caA = createCategory('A', coA, coB);
    caB = createCategory('B', coC, coD);
    caA.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    caB.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    caA.setUid("categoryIdA");
    caB.setUid("categoryIdB");
    manager.save(caA);
    manager.save(caB);

    // Category Combos
    ccA = createCategoryCombo("CCa", "categComboA", caA, caB);
    manager.save(ccA);

    // Category Option Combos
    cocAC = createCategoryOptionCombo("COCac", "catOptComAC", ccA, coA, coC);
    cocAD = createCategoryOptionCombo("COCad", "catOptComAD", ccA, coA, coD);
    cocBC = createCategoryOptionCombo("COCbc", "catOptComBC", ccA, coB, coC);
    cocBD = createCategoryOptionCombo("COCbd", "catOptComBD", ccA, coB, coD);
    categoryService.addCategoryOptionCombo(cocAC);
    categoryService.addCategoryOptionCombo(cocAD);
    categoryService.addCategoryOptionCombo(cocBC);
    categoryService.addCategoryOptionCombo(cocBD);
    ccA.getOptionCombos().add(cocAC);
    ccA.getOptionCombos().add(cocAD);
    ccA.getOptionCombos().add(cocBC);
    ccA.getOptionCombos().add(cocBD);
    manager.save(ccA);
    coA.getCategoryOptionCombos().add(cocAC);
    coA.getCategoryOptionCombos().add(cocAD);
    coB.getCategoryOptionCombos().add(cocBC);
    coB.getCategoryOptionCombos().add(cocBD);
    coC.getCategoryOptionCombos().add(cocAC);
    coC.getCategoryOptionCombos().add(cocBC);
    coD.getCategoryOptionCombos().add(cocAD);
    coD.getCategoryOptionCombos().add(cocBD);
    manager.save(coA);
    manager.save(coB);
    manager.save(coC);
    manager.save(coD);

    // Default Category Option Combo
    CategoryOptionCombo cocDefault = categoryService.getDefaultCategoryOptionCombo();

    Date jan1 = new GregorianCalendar(2017, JANUARY, 1).getTime();
    Date jan15 = new GregorianCalendar(2017, JANUARY, 15).getTime();
    Date jan20 = new GregorianCalendar(2017, JANUARY, 20).getTime();
    Date feb15 = new GregorianCalendar(2017, FEBRUARY, 15).getTime();
    Date feb15Noon = new GregorianCalendar(2017, FEBRUARY, 15, 12, 0).getTime();
    Date mar15 = new GregorianCalendar(2017, MARCH, 15).getTime();

    // Options and option Sets
    Option oAbc = createOption("abc");
    Option oDef = createOption("def");
    Option oGhi = createOption("ghi");
    Option oJkl = createOption("jkl");

    OptionSet osA = createOptionSet('A', oAbc, oDef, oGhi, oJkl);
    osA.setValueType(ValueType.MULTI_TEXT);

    osA.getOptions().forEach(optionStore::save);

    optionService.saveOptionSet(osA);

    // Data Elements
    deA = createDataElement('A', INTEGER, SUM);
    deA.setUid("deInteger0A");
    dataElementService.addDataElement(deA);

    deB = createDataElement('B', TEXT, NONE);
    deB.setUid("deText0000B");
    dataElementService.addDataElement(deB);

    deM = createDataElement('M', MULTI_TEXT, NONE);
    deM.setUid("deMultTextM");
    deM.setOptionSet(osA);
    dataElementService.addDataElement(deM);

    deU = createDataElement('U', ORGANISATION_UNIT, NONE);
    deU.setUid("deOrgUnit0U");
    dataElementService.addDataElement(deU);

    // Program Stages
    psA = createProgramStage('A', 0);
    psA.setUid("progrStageA");
    psA.addDataElement(deA, 1);
    psA.addDataElement(deU, 2);
    manager.save(psA);

    ProgramStage psB = createProgramStage('B', 0);
    psB.setUid("progrStageB");
    psB.addDataElement(deA, 1);
    psB.addDataElement(deB, 2);
    psB.addDataElement(deM, 3);
    manager.save(psB);

    // Program Category Option Mappings
    ProgramCategoryOptionMapping omA =
        ProgramCategoryOptionMapping.builder()
            .optionId(coA.getUid())
            .filter("#{" + psB.getUid() + "." + deA.getUid() + "} < 15")
            .build();
    ProgramCategoryOptionMapping omB =
        ProgramCategoryOptionMapping.builder()
            .optionId(coB.getUid())
            .filter("#{" + psB.getUid() + "." + deA.getUid() + "} >= 15")
            .build();
    ProgramCategoryOptionMapping omC =
        ProgramCategoryOptionMapping.builder()
            .optionId(coC.getUid())
            .filter("is(#{" + psB.getUid() + "." + deB.getUid() + "} in 'A','B','C')")
            .build();
    ProgramCategoryOptionMapping omD =
        ProgramCategoryOptionMapping.builder()
            .optionId(coD.getUid())
            .filter("not is(#{" + psB.getUid() + "." + deB.getUid() + "} in 'A','B','C')")
            .build();

    // Program Category Mappings
    cmA =
        ProgramCategoryMapping.builder()
            .id("ProgCatMapA")
            .categoryId(caA.getUid())
            .mappingName("Category A mapping")
            .optionMappings(List.of(omA, omB))
            .build();
    cmB =
        ProgramCategoryMapping.builder()
            .id("ProgCatMapB")
            .categoryId(caB.getUid())
            .mappingName("Category B mapping")
            .optionMappings(List.of(omC, omD))
            .build();

    // Programs
    programA = createProgram('A');
    programA.getProgramStages().add(psA);
    programA.getOrganisationUnits().addAll(level3Ous);
    programA.setUid("programA123");
    manager.save(programA);

    programB = createProgram('B');
    programB.getProgramStages().add(psB);
    programB.getOrganisationUnits().addAll(level3Ous);
    programB.setUid("programB123");
    programB.setCategoryCombo(ccA);
    programB.setCategoryMappings(Set.of(cmA, cmB));
    manager.save(programB);

    // Tracked Entity Attributes
    atU = createTrackedEntityAttribute('U', ORGANISATION_UNIT);
    atU.setUid("teaAttribuU");
    manager.save(atU);

    ProgramTrackedEntityAttribute pTea = createProgramTrackedEntityAttribute(programA, atU);
    programA.getProgramAttributes().add(pTea);
    manager.update(programA);

    // Tracked Entity Types
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    // Tracked Entity Instances (Registrations)
    TrackedEntity teiA = createTrackedEntity(ouD, trackedEntityType);
    teiA.setUid("trackEntInA");
    manager.save(teiA);

    // Tracked Entity Attribute Values
    TrackedEntityAttributeValue atv = createTrackedEntityAttributeValue('A', teiA, atU);
    atv.setValue(ouF.getUid());
    attributeValueService.addTrackedEntityAttributeValue(atv);

    // Enrollments (Enrollments)
    Enrollment enrollmentA = createEnrollment(programA, teiA, ouE);
    enrollmentA.setEnrollmentDate(jan1);
    enrollmentA.setOccurredDate(jan1);
    manager.save(enrollmentA);
    teiA.getEnrollments().add(enrollmentA);
    manager.update(teiA);

    Enrollment enrollmentB = createEnrollment(programB, teiA, ouE);
    enrollmentB.setEnrollmentDate(jan1);
    enrollmentB.setOccurredDate(jan1);
    manager.save(enrollmentB);
    teiA.getEnrollments().add(enrollmentB);
    manager.update(teiA);

    // Change programA / teiA ownership through time:
    // Jan 1 (enrollment) - Jan 15: ouE
    // Jan 15 - Feb 15: ouF
    // Feb 15 - Feb 15 Noon: ouE
    // Feb 15 Noon - Mar 15: ouG
    // Mar 15 - present: ouH
    addProgramOwnershipHistory(programA, teiA, ouE, enrollmentA.getEnrollmentDate(), jan15);
    addProgramOwnershipHistory(programA, teiA, ouF, jan15, feb15);
    addProgramOwnershipHistory(programA, teiA, ouE, feb15, feb15Noon);
    addProgramOwnershipHistory(programA, teiA, ouG, feb15Noon, mar15);
    trackedEntityProgramOwnerService.createOrUpdateTrackedEntityProgramOwner(teiA, programA, ouH);

    TrackerEvent eventA1 = createEvent(psA, enrollmentA, ouI);
    eventA1.setScheduledDate(jan15);
    eventA1.setOccurredDate(jan15);
    eventA1.setUid("event0000A1");
    eventA1.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "1"), new EventDataValue(deU.getUid(), ouL.getUid())));
    eventA1.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventA2 = createEvent(psA, enrollmentA, ouJ);
    eventA2.setScheduledDate(feb15);
    eventA2.setOccurredDate(feb15);
    eventA2.setUid("event0000A2");
    eventA2.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "2"), new EventDataValue(deU.getUid(), ouM.getUid())));
    eventA2.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventA3 = createEvent(psA, enrollmentA, ouK);
    eventA3.setScheduledDate(mar15);
    eventA3.setOccurredDate(mar15);
    eventA3.setUid("event0000A3");
    eventA3.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "4"), new EventDataValue(deU.getUid(), ouN.getUid())));
    eventA3.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB1 = createEvent(psB, enrollmentB, ouI);
    eventB1.setScheduledDate(jan1);
    eventB1.setOccurredDate(jan1);
    eventB1.setUid("event0000B1");
    eventB1.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "10"), new EventDataValue(deB.getUid(), "A")));
    eventB1.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB2 = createEvent(psB, enrollmentB, ouI);
    eventB2.setScheduledDate(jan20);
    eventB2.setOccurredDate(jan20);
    eventB2.setUid("event0000B2");
    eventB2.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "20"), new EventDataValue(deB.getUid(), "B")));
    eventB2.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB3 = createEvent(psB, enrollmentB, ouJ);
    eventB3.setScheduledDate(jan1);
    eventB3.setOccurredDate(jan1);
    eventB3.setUid("event0000B3");
    eventB3.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "30"), new EventDataValue(deB.getUid(), "C")));
    eventB3.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB4 = createEvent(psB, enrollmentB, ouJ);
    eventB4.setScheduledDate(jan20);
    eventB4.setOccurredDate(jan20);
    eventB4.setUid("event0000B4");
    eventB4.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "40"), new EventDataValue(deB.getUid(), "D")));
    eventB4.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB5 = createEvent(psB, enrollmentB, ouI);
    eventB5.setScheduledDate(feb15);
    eventB5.setOccurredDate(feb15);
    eventB5.setUid("event0000B5");
    eventB5.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "50"), new EventDataValue(deB.getUid(), "E")));
    eventB5.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB6 = createEvent(psB, enrollmentB, ouI);
    eventB6.setScheduledDate(feb15Noon);
    eventB6.setOccurredDate(feb15Noon);
    eventB6.setUid("event0000B6");
    eventB6.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "60"), new EventDataValue(deB.getUid(), "F")));
    eventB6.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB7 = createEvent(psB, enrollmentB, ouJ);
    eventB7.setScheduledDate(feb15);
    eventB7.setOccurredDate(feb15);
    eventB7.setUid("event0000B7");
    eventB7.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "70"), new EventDataValue(deB.getUid(), "G")));
    eventB7.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventB8 = createEvent(psB, enrollmentB, ouJ);
    eventB8.setScheduledDate(feb15Noon);
    eventB8.setOccurredDate(feb15Noon);
    eventB8.setUid("event0000B8");
    eventB8.setEventDataValues(
        Set.of(new EventDataValue(deA.getUid(), "80"), new EventDataValue(deB.getUid(), "H")));
    eventB8.setAttributeOptionCombo(cocDefault);

    TrackerEvent eventM1 = createEvent(psB, enrollmentB, ouI);
    eventM1.setScheduledDate(jan15);
    eventM1.setOccurredDate(jan15);
    eventM1.setUid("event0000M1");
    eventM1.setEventDataValues(Set.of(new EventDataValue(deM.getUid(), "abc,def,ghi,jkl")));
    eventM1.setAttributeOptionCombo(cocDefault);
    manager.save(eventA2);

    manager.save(
        List.of(
            eventA1, eventA2, eventA3, eventB1, eventB2, eventB3, eventB4, eventB5, eventB6,
            eventB7, eventB8, eventM1));

    // Users
    userA = createUserWithAuth("A", "F_VIEW_EVENT_ANALYTICS");
    userA.setCatDimensionConstraints(Sets.newHashSet(caA, caB));
    userService.addUser(userA);
    enableDataSharing(userA, programA, AccessStringHelper.DATA_READ_WRITE);
    enableDataSharing(userA, programB, AccessStringHelper.DATA_READ_WRITE);
    manager.update(userA);

    // Wait for one second. This is needed because last updated time for
    // the data we just created is stored to milliseconds, hh:mm:ss.SSS.
    // The queries to build analytics tables tests data last updated times
    // to be in the past but compares only to the second. So for example a
    // recorded last updated time of 11:23:50.123 could appear to be in the
    // future compared with 11:23:50. To compensate for this, we wait a
    // second until the time is 11:23:51. Then 11:23:50.123 will appear to
    // be in the past.
    Date oneSecondFromNow =
        Date.from(LocalDateTime.now().plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());

    // Generate resource tables and analytics tables
    analyticsTableGenerator.generateAnalyticsTables(
        AnalyticsTableUpdateParams.newBuilder().startTime(oneSecondFromNow).build(),
        JobProgress.noop());
  }

  @BeforeEach
  public void beforeEach() {
    injectAdminIntoSecurityContext();
  }

  /** Adds a program ownership history entry. */
  private void addProgramOwnershipHistory(
      Program program, TrackedEntity te, OrganisationUnit ou, Date startDate, Date endDate) {
    ProgramOwnershipHistory poh =
        new ProgramOwnershipHistory(program, te, ou, startDate, endDate, "admin");

    programOwnershipHistoryService.addProgramOwnershipHistory(poh);
  }

  @AfterAll
  public void tearDown() {
    for (AnalyticsTableService service : analyticsTableServices) {
      service.dropTables();
    }
  }

  // -------------------------------------------------------------------------
  // Test dimension restrictions
  // -------------------------------------------------------------------------

  @Test
  void testDimensionRestrictionSuccessfully() {
    // Given
    // The category options are readable by the user
    coA.getSharing().setOwner(userA);
    coB.getSharing().setOwner(userA);
    enableDataSharing(userA, coA, "rwrw----");
    enableDataSharing(userA, coB, "rwrw----");
    categoryService.updateCategoryOption(coA);
    categoryService.updateCategoryOption(coB);

    // The categories are readable by the user
    caA.getSharing().setOwner(userA);
    caB.getSharing().setOwner(userA);
    categoryService.updateCategory(caA);
    categoryService.updateCategory(caB);

    // The user is active
    injectSecurityContextUser(userA);

    // All events in 2017
    EventQueryParams events_2017_params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(Lists.newArrayList(ouD))
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withProgram(programB)
            .build();

    // Then
    assertDoesNotThrow(() -> eventAggregateService.getAggregatedData(events_2017_params));
  }

  @Test
  void testDimensionRestrictionWhenUserCannotReadCategoryOptions() {
    injectAdminIntoSecurityContext();

    // Given
    // The category options are not readable by the user
    coA.getSharing().setOwner("cannotRead");
    coA.getSharing().setPublicAccess("--------");
    coB.getSharing().setOwner("cannotRead");
    coB.getSharing().setPublicAccess("--------");
    removeUserAccess(coA);
    removeUserAccess(coB);
    categoryService.updateCategoryOption(coA);
    categoryService.updateCategoryOption(coB);

    // The category is not readable by the user
    caA.getSharing().setOwner("cannotRead");
    coA.getSharing().setPublicAccess("--------");
    caB.getSharing().setOwner("cannotRead");
    caB.getSharing().setPublicAccess("--------");
    categoryService.updateCategory(caA);
    categoryService.updateCategory(caB);

    // The user is active
    injectSecurityContextUser(userA);

    // All events in 2017
    EventQueryParams events_2017_params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(Lists.newArrayList(ouD))
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withProgram(programB)
            .build();

    // Then
    Throwable exception =
        assertThrows(
            IllegalQueryException.class,
            () -> eventAggregateService.getAggregatedData(events_2017_params));

    assertThat(
        exception.getMessage(),
        containsString(
            "Current user is constrained by a dimension but has access to no dimension items"));
  }

  @Test
  void testEnrollmentWithCategoryDimensionRestriction() {
    injectSecurityContextUser(userA);
    EventQueryParams params = getEnrollmentQueryBuilderA().build();
    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghE", "trackEntInA", "OrganisationUnitF")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Test getAggregatedEventData with OrgUnitFields
  // -------------------------------------------------------------------------

  @Test
  void testGetAggregatedEventDataWithRegistrationOrgUnit() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField("REGISTRATION")).build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghD"),
            List.of("201702", "ouabcdefghD"),
            List.of("201703", "ouabcdefghD")),
        grid);
  }

  @Test
  void testGetAggregatedEventDataWithEnrollmentOrgUnit() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField("ENROLLMENT")).build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghE"),
            List.of("201702", "ouabcdefghE"),
            List.of("201703", "ouabcdefghE")),
        grid);
  }

  @Test
  void testGetAggregatedEventDataWithOwnerAtStart() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_START")).build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghE"),
            List.of("201702", "ouabcdefghF"),
            List.of("201703", "ouabcdefghG")),
        grid);
  }

  @Test
  void testGetAggregatedEventDataWithOwnerAtEnd() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_END")).build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghF"),
            List.of("201702", "ouabcdefghG"),
            List.of("201703", "ouabcdefghH")),
        grid);
  }

  @Test
  void testGetAggregatedEventDataWithDefaultEventOrgUnit() {
    EventQueryParams params = getAggregatedQueryBuilderA().build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI"),
            List.of("201702", "ouabcdefghJ"),
            List.of("201703", "ouabcdefghK")),
        grid);
  }

  @Test
  void testGetAggregatedEventDataWithDataElementOrgUnit() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField(deU.getUid())).build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("pe", "ou"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghL"),
            List.of("201702", "ouabcdefghM"),
            List.of("201703", "ouabcdefghN")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Test getEvents with OrgUnitFields
  // -------------------------------------------------------------------------

  @Test
  void testGetEventsWithRegistrationOrgUnit() {
    EventQueryParams params =
        getEventQueryBuilderA().withOrgUnitField(new OrgUnitField("REGISTRATION")).build();

    Grid grid = eventQueryTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitN")),
        grid);
  }

  @Test
  void testGetEventsWithEnrollmentOrgUnit() {
    EventQueryParams params =
        getEventQueryBuilderA().withOrgUnitField(new OrgUnitField("ENROLLMENT")).build();

    Grid grid = eventQueryTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitN")),
        grid);
  }

  @Test
  void testGetEventsWithOwnerAtStart() {
    EventQueryParams params =
        getEventQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_START")).build();

    Grid grid = eventQueryTarget.getEvents(params);

    // Note that owner at start does not change with each event because
    // there is no monthly aggregation.
    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitN")),
        grid);
  }

  @Test
  void testGetEventsWithOwnerAtEnd() {
    EventQueryParams params =
        getEventQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_END")).build();

    Grid grid = eventQueryTarget.getEvents(params);

    // Note that owner at end does not change with each event because
    // there is no monthly aggregation.
    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitN")),
        grid);
  }

  @Test
  void testGetEventsWithDefaultEventOrgUnit() {
    EventQueryParams params = getEventQueryBuilderA().build();

    Grid grid = eventQueryTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghI", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghJ", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghK", "OrganisationUnitN")),
        grid);
  }

  @Test
  void testGetEventsWithDataElementOrgUnit() {
    EventQueryParams params =
        getEventQueryBuilderA().withOrgUnitField(new OrgUnitField(deU.getUid())).build();

    Grid grid = eventQueryTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnit0U"),
        // Grid
        List.of(
            List.of("2017-01-15 00:00:00.0", "ouabcdefghL", "OrganisationUnitL"),
            List.of("2017-02-15 00:00:00.0", "ouabcdefghM", "OrganisationUnitM"),
            List.of("2017-03-15 00:00:00.0", "ouabcdefghN", "OrganisationUnitN")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Test getEnrollments with OrgUnitFields
  // -------------------------------------------------------------------------

  @Test
  void testGetEnrollmentsWithRegistrationOrgUnit() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("REGISTRATION")).build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghD", "trackEntInA", "OrganisationUnitF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithEnrollmentOrgUnit() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("ENROLLMENT")).build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "ouname", "teaAttribuU"),
        // Grid
        List.of(
            List.of(
                "2017-01-01 00:00:00.0", "ouabcdefghE", "OrganisationUnitE", "OrganisationUnitF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithOwnerAtStart() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_START")).build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "oucode", "teaAttribuU"),
        // Grid
        List.of(
            List.of(
                "2017-01-01 00:00:00.0",
                "ouabcdefghE",
                "OrganisationUnitCodeE",
                "OrganisationUnitF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithOwnerAtEnd() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_END")).build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghH", "trackEntInA", "OrganisationUnitF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithDefaultEnrollmentOrgUnit() {
    EventQueryParams params = getEnrollmentQueryBuilderA().build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghE", "trackEntInA", "OrganisationUnitF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithAttributeOrgUnit() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField(atU.getUid())).build();

    Grid grid = enrollmentQueryTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghF", "trackEntInA", "OrganisationUnitF")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Test program indicators with orgUnitField
  // -------------------------------------------------------------------------

  @Test
  void testEventProgramIndicatorWithNoOrgUnitField() {
    ProgramIndicator pi =
        createProgramIndicatorA(EVENT, "#{progrStageA.deInteger0A}", null, null, 0);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndA", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndA", "201702", "ouabcdefghJ", "2.0"),
            List.of("programIndA", "201703", "ouabcdefghK", "4.0")),
        grid);
  }

  @Test
  void testEventProgramIndicatorWithOrgUnitFieldAtStart() {
    ProgramIndicator pi =
        createProgramIndicatorA(EVENT, "#{progrStageA.deInteger0A}", "OWNER_AT_START", null, 0);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndA", "201701", "ouabcdefghE", "1.0"),
            List.of("programIndA", "201702", "ouabcdefghF", "2.0"),
            List.of("programIndA", "201703", "ouabcdefghG", "4.0")),
        grid);
  }

  @Test
  void testEventProgramIndicatorWithOrgUnitFieldAtEnd() {
    ProgramIndicator pi =
        createProgramIndicatorA(EVENT, "#{progrStageA.deInteger0A}", "OWNER_AT_END", null, 0);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndA", "201701", "ouabcdefghF", "1.0"),
            List.of("programIndA", "201702", "ouabcdefghG", "2.0"),
            List.of("programIndA", "201703", "ouabcdefghH", "4.0")),
        grid);
  }

  @Test
  void testEnrollmentProgramIndicatorWithOrgUnitFieldAtStart() {
    ProgramIndicator pi =
        createProgramIndicatorA(
            ENROLLMENT,
            "#{progrStageA.deInteger0A}",
            "OWNER_AT_START",
            getPeriodTypeByName("Yearly"),
            -10);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndA", "201701", "ouabcdefghE", "4.0"),
            List.of("programIndA", "201702", "ouabcdefghF", "4.0"),
            List.of("programIndA", "201703", "ouabcdefghG", "4.0")),
        grid);
  }

  @Test
  void testEnrollmentProgramIndicatorWithOrgUnitFieldAtEnd() {
    ProgramIndicator pi =
        createProgramIndicatorA(
            ENROLLMENT,
            "#{progrStageA.deInteger0A}",
            "OWNER_AT_END",
            getPeriodTypeByName("Yearly"),
            -10);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndA", "201701", "ouabcdefghF", "4.0"),
            List.of("programIndA", "201702", "ouabcdefghG", "4.0"),
            List.of("programIndA", "201703", "ouabcdefghH", "4.0")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Test program indicators with aggregation types
  // -------------------------------------------------------------------------

  @Test
  void testEventProgramIndicatorFirstSumOrgUnit() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "10.0"), // First of 10, 20
            List.of("201701", "ouabcdefghJ", "30.0"), // First of 30, 40
            List.of("201701", "ouabcdefghA", "40.0"), // Sum
            List.of("201702", "ouabcdefghI", "50.0"), // First of 50, 60
            List.of("201702", "ouabcdefghJ", "70.0"), // First of 70, 80
            List.of("201702", "ouabcdefghA", "120.0")), // Sum
        getTestAggregatedGrid(FIRST));
  }

  @Test
  void testEventProgramIndicatorLastSumOrgUnit() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "20.0"), // Last of 10, 20
            List.of("201701", "ouabcdefghJ", "40.0"), // Last of 30, 40
            List.of("201701", "ouabcdefghA", "60.0"), // Sum
            List.of("201702", "ouabcdefghI", "60.0"), // Last of 50, 60
            List.of("201702", "ouabcdefghJ", "80.0"), // Last of 70, 80
            List.of("201702", "ouabcdefghA", "140.0")), // Sum
        getTestAggregatedGrid(LAST));
  }

  @Test
  void testEventProgramIndicatorFirstAverageOrgUnit() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "10.0"), // First of 10, 20
            List.of("201701", "ouabcdefghJ", "30.0"), // First of 30, 40
            List.of("201701", "ouabcdefghA", "20.0"), // Average
            List.of("201702", "ouabcdefghI", "50.0"), // First of 50, 60
            List.of("201702", "ouabcdefghJ", "70.0"), // First of 70, 80
            List.of("201702", "ouabcdefghA", "60.0")), // Average
        getTestAggregatedGrid(FIRST_AVERAGE_ORG_UNIT));
  }

  @Test
  void testEventProgramIndicatorLastAverageOrgUnit() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "20.0"), // Last of 10, 20
            List.of("201701", "ouabcdefghJ", "40.0"), // Last of 30, 40
            List.of("201701", "ouabcdefghA", "30.0"), // Average
            List.of("201702", "ouabcdefghI", "60.0"), // Last of 50, 60
            List.of("201702", "ouabcdefghJ", "80.0"), // Last of 70, 80
            List.of("201702", "ouabcdefghA", "70.0")), // Average
        getTestAggregatedGrid(LAST_AVERAGE_ORG_UNIT));
  }

  @Test
  void testEventProgramIndicatorSum() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "30.0"), // 10 + 20
            List.of("201701", "ouabcdefghJ", "70.0"), // 30 + 40
            List.of("201701", "ouabcdefghA", "100.0"), // Sum
            List.of("201702", "ouabcdefghI", "110.0"), // 50 + 60
            List.of("201702", "ouabcdefghJ", "150.0"), // 70 + 80
            List.of("201702", "ouabcdefghA", "260.0")), // Sum
        getTestAggregatedGrid(SUM));
  }

  @Test
  void testEventProgramIndicatorAverage() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "15.0"), // avg(10,20)
            List.of("201701", "ouabcdefghJ", "35.0"), // avg(30,40)
            List.of("201701", "ouabcdefghA", "25.0"), // avg(10,20,30,40)
            List.of("201702", "ouabcdefghI", "55.0"), // avg(50,60)
            List.of("201702", "ouabcdefghJ", "75.0"), // avg(70,80)
            List.of("201702", "ouabcdefghA", "65.0") // avg(50,60,70,80)
            ),
        getTestAggregatedGrid(AVERAGE));
  }

  @Test
  void testEventProgramIndicatorCustomCountIntegers() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "2.0"), // count(10,20)
            List.of("201701", "ouabcdefghJ", "2.0"), // count(30,40)
            List.of("201701", "ouabcdefghA", "4.0"), // count(10,20,30,40)
            List.of("201702", "ouabcdefghI", "2.0"), // count(50,60)
            List.of("201702", "ouabcdefghJ", "2.0"), // count(70,80)
            List.of("201702", "ouabcdefghA", "4.0") // count(50,60,70,80)
            ),
        getTestAggregatedGrid("count(#{progrStageB.deInteger0A})", CUSTOM));
  }

  @Test
  void testEventProgramIndicatorCustomCountOrgUnits() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(
            List.of("201701", "ouabcdefghI", "2.0"), // count("A","B")
            List.of("201701", "ouabcdefghJ", "2.0"), // count("C","D")
            List.of("201701", "ouabcdefghA", "4.0"), // count("A","B","C","D")
            List.of("201702", "ouabcdefghI", "2.0"), // count("E","F")
            List.of("201702", "ouabcdefghJ", "2.0"), // count("G","H")
            List.of("201702", "ouabcdefghA", "4.0") // count("E","F","G","H")
            ),
        getTestAggregatedGrid("count(#{progrStageB.deText0000B})", CUSTOM));
  }

  // -------------------------------------------------------------------------
  // Test program indicators with contains()
  // -------------------------------------------------------------------------

  @Test
  void testProgramIndicatorContains() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "1.0"), List.of("201701", "ouabcdefghI", "1.0")),
        getTestAggregatedGrid("if(contains(#{progrStageB.deMultTextM},'abc','ghi'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "2.0"), List.of("201701", "ouabcdefghI", "2.0")),
        getTestAggregatedGrid("if(contains(#{progrStageB.deMultTextM},'xyz'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "1.0"), List.of("201701", "ouabcdefghI", "1.0")),
        getTestAggregatedGrid("if(contains(#{progrStageB.deMultTextM},'ab'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "1.0"), List.of("201701", "ouabcdefghI", "1.0")),
        getTestAggregatedGrid("if(contains(#{progrStageB.deMultTextM},'c,d'),1,2)"));
  }

  // -------------------------------------------------------------------------
  // Test program indicators with containsItems()
  // -------------------------------------------------------------------------

  @Test
  void testProgramIndicatorContainsItems() {
    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "1.0"), List.of("201701", "ouabcdefghI", "1.0")),
        getTestAggregatedGrid("if(containsItems(#{progrStageB.deMultTextM},'abc','ghi'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "2.0"), List.of("201701", "ouabcdefghI", "2.0")),
        getTestAggregatedGrid("if(containsItems(#{progrStageB.deMultTextM},'xyz'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "2.0"), List.of("201701", "ouabcdefghI", "2.0")),
        getTestAggregatedGrid("if(containsItems(#{progrStageB.deMultTextM},'ab'),1,2)"));

    assertGridContains(
        // Headers
        List.of("pe", "ou", "value"),
        // Grid
        List.of(List.of("201701", "ouabcdefghA", "2.0"), List.of("201701", "ouabcdefghI", "2.0")),
        getTestAggregatedGrid("if(containsItems(#{progrStageB.deMultTextM},'c,d'),1,2)"));
  }

  // -------------------------------------------------------------------------
  // Test program indicator with category mappings
  // -------------------------------------------------------------------------

  @Test
  void testEventProgramIndicatorCategoryMappingsWithNoExtraDimension() {
    ProgramIndicator pi = createProgramIndicatorBWithCategoryMappings();

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndB", "201701", "ouabcdefghI", "2.0"),
            List.of("programIndB", "201701", "ouabcdefghJ", "2.0"),
            List.of("programIndB", "201702", "ouabcdefghI", "2.0"),
            List.of("programIndB", "201702", "ouabcdefghJ", "2.0")),
        grid);
  }

  @Test
  void testEventProgramIndicatorCategoryMappingsWithOneExtraDimension() {
    ProgramIndicator pi = createProgramIndicatorBWithCategoryMappings();

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .addDimension(caA)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "categoryIdA", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndB", "cataOptionA", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB", "cataOptionB", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB", "cataOptionB", "201701", "ouabcdefghJ", "2.0"),
            List.of("programIndB", "cataOptionB", "201702", "ouabcdefghI", "2.0"),
            List.of("programIndB", "cataOptionB", "201702", "ouabcdefghJ", "2.0")),
        grid);
  }

  @Test
  void testEventProgramIndicatorCategoryMappingsWithTwoExtraDimensions() {
    ProgramIndicator pi = createProgramIndicatorBWithCategoryMappings();

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .addDimension(caA)
            .addDimension(caB)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "categoryIdA", "categoryIdB", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndB", "cataOptionA", "cataOptionC", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB", "cataOptionB", "cataOptionC", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB", "cataOptionB", "cataOptionC", "201701", "ouabcdefghJ", "1.0"),
            List.of("programIndB", "cataOptionB", "cataOptionD", "201701", "ouabcdefghJ", "1.0"),
            List.of("programIndB", "cataOptionB", "cataOptionD", "201702", "ouabcdefghI", "2.0"),
            List.of("programIndB", "cataOptionB", "cataOptionD", "201702", "ouabcdefghJ", "2.0")),
        grid);
  }

  @Test
  void testEventProgramIndicatorCategoryMappingsAsDataValueSet() {
    ProgramIndicator pi = createProgramIndicatorBWithCategoryMappings();

    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withOutputFormat(DATA_VALUE_SET).build();

    EventQueryParams params =
        getBaseEventQueryParamsBuilder(dataQueryParams)
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
            .withOrganisationUnits(level3Ous)
            .build();

    Grid grid = eventAggregateService.getAggregatedData(params);

    assertGridContains(
        // Headers
        List.of("dy", "pe", "ou", "value"),
        // Grid
        List.of(
            List.of("programIndB.*.catOptComAC", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB.*.catOptComBC", "201701", "ouabcdefghI", "1.0"),
            List.of("programIndB.*.catOptComBC", "201701", "ouabcdefghJ", "1.0"),
            List.of("programIndB.*.catOptComBD", "201701", "ouabcdefghJ", "1.0"),
            List.of("programIndB.*.catOptComBD", "201702", "ouabcdefghI", "2.0"),
            List.of("programIndB.*.catOptComBD", "201702", "ouabcdefghJ", "2.0")),
        grid);
  }

  // -------------------------------------------------------------------------
  // Supportive test methods
  // -------------------------------------------------------------------------

  /** EventQueryParams.Builder with frequently-used fields */
  private EventQueryParams.Builder getBaseEventQueryParamsBuilder() {
    return addToBuilder(new EventQueryParams.Builder());
  }

  /** EventQueryParams.Builder with frequently-used fields, based on DataQueryParams */
  private EventQueryParams.Builder getBaseEventQueryParamsBuilder(DataQueryParams dataQueryParams) {
    return addToBuilder(new EventQueryParams.Builder(dataQueryParams));
  }

  /** Adds the frequently-used fields to an EventQueryParams.Builder */
  private EventQueryParams.Builder addToBuilder(EventQueryParams.Builder builder) {
    return builder
        .withOutputType(EventOutputType.EVENT)
        .withDisplayProperty(DisplayProperty.SHORTNAME)
        .withEndpointItem(RequestTypeAware.EndpointItem.EVENT)
        .withCoordinateFields(List.of("eventgeometry"));
  }

  /** Params builder A for getting aggregated grids. */
  private EventQueryParams.Builder getAggregatedQueryBuilderA() {
    return getBaseEventQueryParamsBuilder()
        .withProgram(programA)
        .withProgramStage(psA)
        .withPeriods(List.of(peJan, peFeb, peMar), "Monthly")
        .withOrganisationUnits(level3Ous);
  }

  /** Params builder A for getting events. */
  private EventQueryParams.Builder getEventQueryBuilderA() {
    return getEnrollmentQueryBuilderA()
        .withProgramStage(psA)
        .addItem(new QueryItem(deU, null, deU.getValueType(), deU.getAggregationType(), null));
  }

  /** Params builder A for getting enrollments. */
  private EventQueryParams.Builder getEnrollmentQueryBuilderA() {
    DimensionalObject periodDimension =
        new BaseDimensionalObject(
            PERIOD_DIM_ID, DimensionType.PERIOD, getList(peJan, peFeb, peMar));

    DimensionalObject orgUnitDimension =
        new BaseDimensionalObject(ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList(ouA));

    return getBaseEventQueryParamsBuilder()
        .withProgram(programA)
        .addItem(new QueryItem(atU, null, atU.getValueType(), atU.getAggregationType(), null))
        .addDimension(orgUnitDimension)
        .addDimension(periodDimension);
  }

  /** Gets a grid for a PI specifying expression */
  private Grid getTestAggregatedGrid(String expression) {
    return getTestAggregatedGrid(expression, SUM);
  }

  /** Gets a grid for a PI specifying aggregation type */
  private Grid getTestAggregatedGrid(AggregationType aggregationType) {
    return getTestAggregatedGrid("#{progrStageB.deInteger0A}", aggregationType);
  }

  /** Gets a grid for a PI specifying expression and aggregation type */
  private Grid getTestAggregatedGrid(String expression, AggregationType aggregationType) {
    ProgramIndicator pi = createProgramIndicatorB(EVENT, expression, null, aggregationType);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb), "Monthly")
            .withOrganisationUnits(List.of(ouA, ouI, ouJ))
            .build();

    return eventAggregateService.getAggregatedData(params);
  }

  /** Creates program indicator for program A with orgUnitField. */
  private ProgramIndicator createProgramIndicatorA(
      AnalyticsType analyticsType,
      String expression,
      String orgUnitField,
      PeriodType afterStartPeriodType,
      int afterStartPeriods) {
    ProgramIndicator pi =
        createProgramIndicator(
            'A',
            analyticsType,
            programA,
            expression,
            null,
            afterStartPeriodType,
            afterStartPeriods);
    pi.setUid("programIndA");
    pi.setOrgUnitField(orgUnitField);
    return pi;
  }

  /** Creates program indicator for program B with aggregationType. */
  private ProgramIndicator createProgramIndicatorB(
      AnalyticsType analyticsType,
      String expression,
      String filter,
      AggregationType aggregationType) {
    ProgramIndicator pi = createProgramIndicator('B', analyticsType, programB, expression, filter);
    pi.setUid("programIndB");
    pi.setAggregationType(aggregationType);
    return pi;
  }

  /** Creates program indicator for program B with category mappings. */
  private ProgramIndicator createProgramIndicatorBWithCategoryMappings() {
    ProgramIndicator pi = createProgramIndicatorB(EVENT, "V{event_count}", null, SUM);
    pi.setCategoryMappingIds(Set.of(cmA.getId(), cmB.getId()));
    pi.setAttributeCombo(ccA);
    return pi;
  }

  /**
   * Asserts that a grid:
   *
   * <ol>
   *   <li>contains a given number of rows
   *   <li>includes the given column headers
   *   <li>has rows with expected values in those columns
   * </ol>
   *
   * <p>Note that the headers and the expected values do not have to include every column in the
   * grid. They need only include those columns needed for the test.
   *
   * <p>The grid rows may be found in any order. The expected and actual rows are converted to text
   * strings and sorted.
   */
  private void assertGridContains(List<String> headers, List<List<Object>> rows, Grid grid) {
    // Assert grid contains the expected number of rows
    if (rows.size() != grid.getHeight()) {
      System.out.println("❌ ROW COUNT MISMATCH:");
      System.out.println("   Expected: " + rows.size() + " rows");
      System.out.println("   Actual:   " + grid.getHeight() + " rows");
      printGridComparison(headers, rows, grid);
      assertEquals(rows.size(), grid.getHeight(), "Expected " + rows.size() + " rows in grid");
    }

    // Make a map from header name to grid column index
    Map<String, Integer> headerMap =
        range(0, grid.getHeaders().size())
            .boxed()
            .collect(Collectors.toMap(i -> grid.getHeaders().get(i).getName(), identity()));

    // Assert grid contains all the expected headers (column names)
    if (!headerMap.keySet().containsAll(headers)) {
      System.out.println("❌ HEADER MISMATCH:");
      System.out.println("   Expected headers: " + headers);
      System.out.println("   Available headers: " + headerMap.keySet());
      assertTrue(
          headerMap.keySet().containsAll(headers), "Expected headers " + headers + " in grid");
    }

    // Make colA:row1value/colB:row1value, colA:row2value/colB:row2value...
    List<String> expected =
        rows.stream().map(r -> flattenExpectedRow(headers, r)).sorted().collect(toList());

    // Make colA:row1value/colB:row1value, colA:row2value/colB:row2value...
    List<String> actual =
        grid.getRows().stream()
            .map(r -> flattenGridRow(headers, headerMap, r))
            .sorted()
            .collect(toList());

    // Assert the expected rows are present with the expected values
    if (!expected.equals(actual)) {
      System.out.println("❌ GRID CONTENT MISMATCH:");
      printGridComparison(headers, rows, grid);
      assertEquals(expected, actual, "Grid content does not match expected values");
    }
  }

  private void printGridComparison(
      List<String> headers, List<List<Object>> expectedRows, Grid actualGrid) {
    System.out.println("\n📊 GRID COMPARISON:");
    System.out.println("=".repeat(80));

    // Print headers
    System.out.println("HEADERS: " + headers);
    System.out.println();

    // Print expected data
    System.out.println("✅ EXPECTED (" + expectedRows.size() + " rows):");
    System.out.println("-".repeat(40));
    printFormattedRows(headers, expectedRows);

    System.out.println();

    // Print actual data
    System.out.println("❌ ACTUAL (" + actualGrid.getHeight() + " rows):");
    System.out.println("-".repeat(40));

    if (actualGrid.getHeight() == 0) {
      System.out.println("   (no rows)");
    } else {
      // Extract actual rows for the specified headers
      Map<String, Integer> headerMap =
          range(0, actualGrid.getHeaders().size())
              .boxed()
              .collect(Collectors.toMap(i -> actualGrid.getHeaders().get(i).getName(), identity()));

      List<List<Object>> actualRows =
          actualGrid.getRows().stream()
              .map(
                  row ->
                      headers.stream()
                          .map(
                              header ->
                                  headerMap.containsKey(header)
                                      ? row.get(headerMap.get(header))
                                      : "N/A")
                          .collect(toList()))
              .collect(toList());

      printFormattedRows(headers, actualRows);
    }

    System.out.println();
    System.out.println("=".repeat(80));
  }

  private void printFormattedRows(List<String> headers, List<List<Object>> rows) {
    if (rows.isEmpty()) {
      System.out.println("   (no rows)");
      return;
    }

    // Calculate column widths
    int[] widths = new int[headers.size()];
    for (int i = 0; i < headers.size(); i++) {
      widths[i] = Math.max(headers.get(i).length(), 10);
      for (List<Object> row : rows) {
        if (i < row.size()) {
          widths[i] = Math.max(widths[i], String.valueOf(row.get(i)).length());
        }
      }
    }

    // Print header row
    System.out.print("   ");
    for (int i = 0; i < headers.size(); i++) {
      System.out.printf("%-" + widths[i] + "s", headers.get(i));
      if (i < headers.size() - 1) System.out.print(" | ");
    }
    System.out.println();

    // Print separator
    System.out.print("   ");
    for (int i = 0; i < headers.size(); i++) {
      System.out.print("-".repeat(widths[i]));
      if (i < headers.size() - 1) System.out.print("-+-");
    }
    System.out.println();

    // Print data rows
    for (List<Object> row : rows) {
      System.out.print("   ");
      for (int i = 0; i < headers.size(); i++) {
        String value = i < row.size() ? String.valueOf(row.get(i)) : "N/A";
        System.out.printf("%-" + widths[i] + "s", value);
        if (i < headers.size() - 1) System.out.print(" | ");
      }
      System.out.println();
    }
  }

  /**
   * Concatenates the headers and values that are expected to be contained in a grid row. Returns
   * colA:value/colB:value/...
   */
  private String flattenExpectedRow(List<String> headers, List<Object> values) {
    return range(0, headers.size())
        .boxed()
        .map(i -> headers.get(i) + ":" + values.get(i))
        .collect(joining("/"));
  }

  /**
   * Concatenates the headers and values from a returned grid row. Returns colA:value/colB:value/...
   */
  private String flattenGridRow(
      List<String> headers, Map<String, Integer> headerMap, List<Object> values) {
    return range(0, headers.size())
        .boxed()
        .map(i -> headers.get(i) + ":" + values.get(headerMap.get(headers.get(i))))
        .collect(joining("/"));
  }
}
