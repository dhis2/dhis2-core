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
import static org.hisp.dhis.analytics.AggregationType.FIRST;
import static org.hisp.dhis.analytics.AggregationType.FIRST_AVERAGE_ORG_UNIT;
import static org.hisp.dhis.analytics.AggregationType.LAST;
import static org.hisp.dhis.analytics.AggregationType.LAST_AVERAGE_ORG_UNIT;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeByName;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.AnalyticsType.EVENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
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
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventStore;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramOwnershipHistory;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests event and enrollment analytics services.
 *
 * @author Henning Haakonsen
 * @author Jim Grace (nearly complete rewrite)
 */
class EventAnalyticsServiceTest extends SingleSetupIntegrationTestBase {
  @Autowired private EventAnalyticsService eventTarget;

  @Autowired private EnrollmentAnalyticsService enrollmentTarget;

  @Autowired private List<AnalyticsTableService> analyticsTableServices;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private AnalyticsTableGenerator analyticsTableGenerator;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private EventStore eventStore;

  @Autowired private ProgramOwnershipHistoryService programOwnershipHistoryService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired private UserService _userService;

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
  private Period peJan = createPeriod("2017-01");

  private Period peFeb = createPeriod("2017-02");

  private Period peMar = createPeriod("2017-03");

  private CategoryOption coA;

  private CategoryOption coB;

  private Category caA;

  private Category caB;

  private CategoryCombo ccA;

  private CategoryOptionCombo cocA;

  private CategoryOptionCombo cocB;

  private DataElement deA;

  private DataElement deU;

  private TrackedEntityAttribute atU;

  private Program programA;

  private Program programB;

  private ProgramStage psA;

  private User userA;

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

  @Override
  public void setUpTest() throws IOException, InterruptedException {
    userService = _userService;

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

    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
    idObjectManager.save(ouD);
    idObjectManager.save(ouE);
    idObjectManager.save(ouF);
    idObjectManager.save(ouG);
    idObjectManager.save(ouH);
    idObjectManager.save(ouI);
    idObjectManager.save(ouJ);
    idObjectManager.save(ouK);
    idObjectManager.save(ouL);
    idObjectManager.save(ouM);
    idObjectManager.save(ouN);

    level3Ous = organisationUnitService.getOrganisationUnitsAtLevel(3);

    // Organisation Unit Levels
    OrganisationUnitLevel ou1 = new OrganisationUnitLevel(1, "Ou Level 1");
    OrganisationUnitLevel ou2 = new OrganisationUnitLevel(2, "Ou Level 2");
    OrganisationUnitLevel ou3 = new OrganisationUnitLevel(3, "Ou Level 3");
    idObjectManager.save(ou1);
    idObjectManager.save(ou2);
    idObjectManager.save(ou3);

    // Category Options
    coA = createCategoryOption('A');
    coB = createCategoryOption('B');
    coA.setUid("cataOptionA");
    coB.setUid("cataOptionB");
    categoryService.addCategoryOption(coA);
    categoryService.addCategoryOption(coB);

    // Categories
    caA = createCategory('A', coA);
    caB = createCategory('B', coB);
    caA.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    caB.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    caA.setUid("categoryIdA");
    caB.setUid("categoryIdB");
    categoryService.addCategory(caA);
    categoryService.addCategory(caB);

    // Category Combos
    ccA = createCategoryCombo("CCa", "categComboA", caA, caB);
    categoryService.addCategoryCombo(ccA);

    // Category Option Combos
    cocA = createCategoryOptionCombo("COCa", "catOptCombA", ccA, coA);
    cocB = createCategoryOptionCombo("COCb", "catOptCombB", ccA, coB);
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    ccA.getOptionCombos().add(cocA);
    ccA.getOptionCombos().add(cocB);
    categoryService.updateCategoryCombo(ccA);
    coA.getCategoryOptionCombos().add(cocA);
    coB.getCategoryOptionCombos().add(cocB);
    categoryService.updateCategoryOption(coA);
    categoryService.updateCategoryOption(coB);

    // Default Category Option Combo
    CategoryOptionCombo cocDefault = categoryService.getDefaultCategoryOptionCombo();

    Date jan1 = new GregorianCalendar(2017, JANUARY, 1).getTime();
    Date jan15 = new GregorianCalendar(2017, JANUARY, 15).getTime();
    Date jan20 = new GregorianCalendar(2017, JANUARY, 20).getTime();
    Date feb15 = new GregorianCalendar(2017, FEBRUARY, 15).getTime();
    Date feb15Noon = new GregorianCalendar(2017, FEBRUARY, 15, 12, 0).getTime();
    Date mar15 = new GregorianCalendar(2017, MARCH, 15).getTime();

    // Data Elements
    deA = createDataElement('A', INTEGER, SUM);
    deA.setUid("deInteger0A");
    dataElementService.addDataElement(deA);

    deU = createDataElement('U', ORGANISATION_UNIT, NONE);
    deU.setUid("deOrgUnitU");
    dataElementService.addDataElement(deU);

    // Program Stages
    psA = createProgramStage('A', 0);
    psA.setUid("progrStageA");
    psA.addDataElement(deA, 1);
    psA.addDataElement(deU, 2);
    idObjectManager.save(psA);

    ProgramStage psB = createProgramStage('B', 0);
    psB.setUid("progrStageB");
    psB.addDataElement(deA, 1);
    idObjectManager.save(psB);

    // Programs
    programA = createProgram('A');
    programA.getProgramStages().add(psA);
    programA.getOrganisationUnits().addAll(level3Ous);
    programA.setUid("programA123");
    idObjectManager.save(programA);

    programB = createProgram('B');
    programB.getProgramStages().add(psB);
    programB.getOrganisationUnits().addAll(level3Ous);
    programB.setUid("programB123");
    programB.setCategoryCombo(ccA);
    idObjectManager.save(programB);

    // Tracked Entity Attributes
    atU = createTrackedEntityAttribute('U', ORGANISATION_UNIT);
    atU.setUid("teaAttribuU");
    idObjectManager.save(atU);

    ProgramTrackedEntityAttribute pTea = createProgramTrackedEntityAttribute(programA, atU);
    programA.getProgramAttributes().add(pTea);
    idObjectManager.update(programA);

    // Tracked Entity Types
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    idObjectManager.save(trackedEntityType);

    // Tracked Entity Instances (Registrations)
    TrackedEntity teiA = createTrackedEntity(ouD);
    teiA.setUid("trackEntInA");
    teiA.setTrackedEntityType(trackedEntityType);
    idObjectManager.save(teiA);

    // Tracked Entity Attribute Values
    TrackedEntityAttributeValue atv = createTrackedEntityAttributeValue('A', teiA, atU);
    atv.setValue(ouF.getUid());
    attributeValueService.addTrackedEntityAttributeValue(atv);

    // Enrollments (Enrollments)
    Enrollment piA = enrollmentService.enrollTrackedEntity(teiA, programA, jan1, jan1, ouE);
    piA.setEnrollmentDate(jan1);
    piA.setIncidentDate(jan1);
    enrollmentService.addEnrollment(piA);

    Enrollment piB = enrollmentService.enrollTrackedEntity(teiA, programB, jan1, jan1, ouE);
    piB.setEnrollmentDate(jan1);
    piB.setIncidentDate(jan1);
    enrollmentService.addEnrollment(piB);

    // Change programA / teiA ownership through time:
    // Jan 1 (enrollment) - Jan 15: ouE
    // Jan 15 - Feb 15: ouF
    // Feb 15 - Feb 15 Noon: ouE
    // Feb 15 Noon - Mar 15: ouG
    // Mar 15 - present: ouH
    addProgramOwnershipHistory(programA, teiA, ouE, piA.getEnrollmentDate(), jan15);
    addProgramOwnershipHistory(programA, teiA, ouF, jan15, feb15);
    addProgramOwnershipHistory(programA, teiA, ouE, feb15, feb15Noon);
    addProgramOwnershipHistory(programA, teiA, ouG, feb15Noon, mar15);
    trackedEntityProgramOwnerService.createOrUpdateTrackedEntityProgramOwner(teiA, programA, ouH);

    // Program Stage Instances (Events)
    Event eventA1 = createEvent(psA, piA, ouI);
    eventA1.setDueDate(jan15);
    eventA1.setExecutionDate(jan15);
    eventA1.setUid("event0000A1");
    eventA1.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "1"), new EventDataValue(deU.getUid(), ouL.getUid())));
    eventA1.setAttributeOptionCombo(cocDefault);

    Event eventA2 = createEvent(psA, piA, ouJ);
    eventA2.setDueDate(feb15);
    eventA2.setExecutionDate(feb15);
    eventA2.setUid("event0000A2");
    eventA2.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "2"), new EventDataValue(deU.getUid(), ouM.getUid())));
    eventA2.setAttributeOptionCombo(cocDefault);

    Event eventA3 = createEvent(psA, piA, ouK);
    eventA3.setDueDate(mar15);
    eventA3.setExecutionDate(mar15);
    eventA3.setUid("event0000A3");
    eventA3.setEventDataValues(
        Set.of(
            new EventDataValue(deA.getUid(), "4"), new EventDataValue(deU.getUid(), ouN.getUid())));
    eventA3.setAttributeOptionCombo(cocDefault);

    Event eventB1 = createEvent(psB, piB, ouI);
    eventB1.setDueDate(jan15);
    eventB1.setExecutionDate(jan15);
    eventB1.setUid("event0000B1");
    eventB1.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "10")));
    eventB1.setAttributeOptionCombo(cocDefault);

    Event eventB2 = createEvent(psB, piB, ouI);
    eventB2.setDueDate(jan20);
    eventB2.setExecutionDate(jan20);
    eventB2.setUid("event0000B2");
    eventB2.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "20")));
    eventB2.setAttributeOptionCombo(cocDefault);

    Event eventB3 = createEvent(psB, piB, ouJ);
    eventB3.setDueDate(jan15);
    eventB3.setExecutionDate(jan15);
    eventB3.setUid("event0000B3");
    eventB3.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "30")));
    eventB3.setAttributeOptionCombo(cocDefault);

    Event eventB4 = createEvent(psB, piB, ouJ);
    eventB4.setDueDate(jan20);
    eventB4.setExecutionDate(jan20);
    eventB4.setUid("event0000B4");
    eventB4.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "40")));
    eventB4.setAttributeOptionCombo(cocDefault);

    Event eventB5 = createEvent(psB, piB, ouI);
    eventB5.setDueDate(feb15);
    eventB5.setExecutionDate(feb15);
    eventB5.setUid("event0000B5");
    eventB5.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "50")));
    eventB5.setAttributeOptionCombo(cocDefault);

    Event eventB6 = createEvent(psB, piB, ouI);
    eventB6.setDueDate(feb15Noon);
    eventB6.setExecutionDate(feb15Noon);
    eventB6.setUid("event0000B6");
    eventB6.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "60")));
    eventB6.setAttributeOptionCombo(cocDefault);

    Event eventB7 = createEvent(psB, piB, ouJ);
    eventB7.setDueDate(feb15);
    eventB7.setExecutionDate(feb15);
    eventB7.setUid("event0000B7");
    eventB7.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "70")));
    eventB7.setAttributeOptionCombo(cocDefault);

    Event eventB8 = createEvent(psB, piB, ouJ);
    eventB8.setDueDate(feb15Noon);
    eventB8.setExecutionDate(feb15Noon);
    eventB8.setUid("event0000B8");
    eventB8.setEventDataValues(Set.of(new EventDataValue(deA.getUid(), "80")));
    eventB8.setAttributeOptionCombo(cocDefault);

    saveEvents(
        List.of(
            eventA1, eventA2, eventA3, eventB1, eventB2, eventB3, eventB4, eventB5, eventB6,
            eventB7, eventB8));

    // Users
    userA = createUserWithAuth("A", "F_VIEW_EVENT_ANALYTICS");
    userA.setCatDimensionConstraints(Sets.newHashSet(caA, caB));
    userService.addUser(userA);
    enableDataSharing(userA, programA, AccessStringHelper.DATA_READ_WRITE);
    enableDataSharing(userA, programB, AccessStringHelper.DATA_READ_WRITE);
    idObjectManager.update(userA);

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
    analyticsTableGenerator.generateTables(
        AnalyticsTableUpdateParams.newBuilder().withStartTime(oneSecondFromNow).build(),
        NoopJobProgress.INSTANCE);
  }

  /**
   * Saves events. Since the store is called directly, wraps the call in a transaction. Since
   * transactional methods must be overridable, the method is protected.
   */
  @Transactional
  protected void saveEvents(List<Event> events) {
    eventStore.saveEvents(events);
  }

  /** Adds a program ownership history entry. */
  private void addProgramOwnershipHistory(
      Program program, TrackedEntity tei, OrganisationUnit ou, Date startDate, Date endDate) {
    ProgramOwnershipHistory poh =
        new ProgramOwnershipHistory(program, tei, ou, startDate, endDate, "admin");

    programOwnershipHistoryService.addProgramOwnershipHistory(poh);
  }

  @Override
  public void tearDownTest() {
    for (AnalyticsTableService service : analyticsTableServices) {
      service.dropTables();
    }
  }

  @BeforeEach
  public void beforeEach() {
    // Reset the security context for each test.
    clearSecurityContext();
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
    injectSecurityContext(userA);

    // All events in 2017
    EventQueryParams events_2017_params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(Lists.newArrayList(ouD))
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withProgram(programB)
            .build();

    // Then
    assertDoesNotThrow(() -> eventTarget.getAggregatedEventData(events_2017_params));
  }

  @Test
  void testDimensionRestrictionWhenUserCannotReadCategoryOptions() {
    // Given
    // The category options are not readable by the user
    coA.getSharing().setOwner("cannotRead");
    coB.getSharing().setOwner("cannotRead");
    removeUserAccess(coA);
    removeUserAccess(coB);
    categoryService.updateCategoryOption(coA);
    categoryService.updateCategoryOption(coB);

    // The category is not readable by the user
    caA.getSharing().setOwner("cannotRead");
    caB.getSharing().setOwner("cannotRead");
    categoryService.updateCategory(caA);
    categoryService.updateCategory(caB);

    // The user is active
    injectSecurityContext(userA);

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
            () -> eventTarget.getAggregatedEventData(events_2017_params));
    assertThat(
        exception.getMessage(),
        containsString(
            "Current user is constrained by a dimension but has access to no dimension items"));
  }

  // -------------------------------------------------------------------------
  // Test getAggregatedEventData with OrgUnitFields
  // -------------------------------------------------------------------------

  @Test
  void testGetAggregatedEventDataWithRegistrationOrgUnit() {
    EventQueryParams params =
        getAggregatedQueryBuilderA().withOrgUnitField(new OrgUnitField("REGISTRATION")).build();

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = eventTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = eventTarget.getEvents(params);

    // Note that owner at start does not change with each event because
    // there is no monthly aggregation.
    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = eventTarget.getEvents(params);

    // Note that owner at end does not change with each event because
    // there is no monthly aggregation.
    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = eventTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = eventTarget.getEvents(params);

    assertGridContains(
        // Headers
        List.of("eventdate", "ou", "deOrgUnitU"),
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

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(List.of("2017-01-01 00:00:00.0", "ouabcdefghD", "trackEntInA", "ouabcdefghF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithEnrollmentOrgUnit() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("ENROLLMENT")).build();

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "ouname", "teaAttribuU"),
        // Grid
        List.of(
            List.of("2017-01-01 00:00:00.0", "ouabcdefghE", "OrganisationUnitE", "ouabcdefghF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithOwnerAtStart() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_START")).build();

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "oucode", "teaAttribuU"),
        // Grid
        List.of(
            List.of(
                "2017-01-01 00:00:00.0", "ouabcdefghE", "OrganisationUnitCodeE", "ouabcdefghF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithOwnerAtEnd() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField("OWNER_AT_END")).build();

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(List.of("2017-01-01 00:00:00.0", "ouabcdefghH", "trackEntInA", "ouabcdefghF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithDefaultEnrollmentOrgUnit() {
    EventQueryParams params = getEnrollmentQueryBuilderA().build();

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(List.of("2017-01-01 00:00:00.0", "ouabcdefghE", "trackEntInA", "ouabcdefghF")),
        grid);
  }

  @Test
  void testGetEnrollmentsWithAttributeOrgUnit() {
    EventQueryParams params =
        getEnrollmentQueryBuilderA().withOrgUnitField(new OrgUnitField(atU.getUid())).build();

    Grid grid = enrollmentTarget.getEnrollments(params);

    assertGridContains(
        // Headers
        List.of("enrollmentdate", "ou", "tei", "teaAttribuU"),
        // Grid
        List.of(List.of("2017-01-01 00:00:00.0", "ouabcdefghF", "trackEntInA", "ouabcdefghF")),
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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

    Grid grid = eventTarget.getAggregatedEventData(params);

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

  // -------------------------------------------------------------------------
  // Supportive test methods
  // -------------------------------------------------------------------------

  private EventQueryParams.Builder getBaseEventQueryParamsBuilder() {
    return new EventQueryParams.Builder()
        .withOutputType(EventOutputType.EVENT)
        .withDisplayProperty(DisplayProperty.SHORTNAME)
        .withEndpointItem(RequestTypeAware.EndpointItem.EVENT)
        .withCoordinateFields(List.of("psigeometry"));
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
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(
            PERIOD_DIM_ID, DimensionType.PERIOD, getList(peJan, peFeb, peMar));

    BaseDimensionalObject orgUnitDimension =
        new BaseDimensionalObject(ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList(ouA));

    return getBaseEventQueryParamsBuilder()
        .withProgram(programA)
        .addItem(new QueryItem(atU, null, atU.getValueType(), atU.getAggregationType(), null))
        .addDimension(orgUnitDimension)
        .addDimension(periodDimension);
  }

  /** Gets a grid to test aggregation types */
  private Grid getTestAggregatedGrid(AggregationType aggregationType) {
    ProgramIndicator pi =
        createProgramIndicatorB(EVENT, "#{progrStageB.deInteger0A}", null, aggregationType);

    EventQueryParams params =
        getBaseEventQueryParamsBuilder()
            .withAggregateData(true)
            .addItemProgramIndicator(pi)
            .withPeriods(List.of(peJan, peFeb), "Monthly")
            .withOrganisationUnits(List.of(ouA, ouI, ouJ))
            .build();

    return eventTarget.getAggregatedEventData(params);
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

  /**
   * Asserts that a grid:
   *
   * <ol>
   *   <li>contains a given number of rows
   *   <li>includes the given column headers
   *   <li>has rows with expected values in those columns
   * </ol>
   *
   * Note that the headers and the expected values do not have to include every column in the grid.
   * They need only include those columns needed for the test.
   *
   * <p>The grid rows may be found in any order. The expected and actual rows are converted to text
   * strings and sorted.
   */
  private void assertGridContains(List<String> headers, List<List<Object>> rows, Grid grid) {
    // Assert grid contains the expected number of rows
    assertEquals(
        rows.size(), grid.getHeight(), "Expected " + rows.size() + " rows in grid " + grid);

    // Make a map from header name to grid column index
    Map<String, Integer> headerMap =
        range(0, grid.getHeaders().size())
            .boxed()
            .collect(Collectors.toMap(i -> grid.getHeaders().get(i).getName(), identity()));

    // Assert grid contains all the expected headers (column names)
    assertTrue(
        headerMap.keySet().containsAll(headers),
        "Expected headers " + headers + " in grid " + grid);

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
    assertEquals(expected, actual);
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
