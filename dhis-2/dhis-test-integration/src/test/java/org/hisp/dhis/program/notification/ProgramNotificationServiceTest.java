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
package org.hisp.dhis.program.notification;

import static org.hisp.dhis.program.notification.NotificationTrigger.SCHEDULED_DAYS_DUE_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementStore;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Chau Thu Tran
 */
class ProgramNotificationServiceTest extends TransactionalIntegrationTest {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ProgramStageDataElementStore programStageDataElementStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private CategoryService categoryService;

  @Autowired
  @Qualifier("org.hisp.dhis.program.notification.ProgramNotificationStore")
  private IdentifiableObjectStore<ProgramNotificationTemplate> programNotificationStore;

  @Autowired private ProgramNotificationService programNotificationService;

  private CategoryOptionCombo coA;

  private ProgramStage stageA;

  private ProgramStage stageB;

  private ProgramStage stageC;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  @Override
  public void setUpTest() {
    coA = categoryService.getDefaultCategoryOptionCombo();
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    manager.save(organisationUnitA);
    manager.save(organisationUnitB);
    TrackedEntity trackedEntityA = createTrackedEntity(organisationUnitA);
    trackedEntityService.addTrackedEntity(trackedEntityA);
    TrackedEntity trackedEntityB = createTrackedEntity(organisationUnitB);
    trackedEntityService.addTrackedEntity(trackedEntityB);
    Program programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programService.addProgram(programA);
    stageA = new ProgramStage("A", programA);
    programStageService.saveProgramStage(stageA);
    stageB = new ProgramStage("B", programA);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    programA.getProgramStages().addAll(programStages);
    programService.updateProgram(programA);
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    ProgramStageDataElement stageDataElementA =
        createProgramStageDataElement(stageA, dataElementA, 1);
    ProgramStageDataElement stageDataElementB =
        createProgramStageDataElement(stageA, dataElementB, 2);
    ProgramStageDataElement stageDataElementC =
        createProgramStageDataElement(stageB, dataElementA, 1);
    ProgramStageDataElement stageDataElementD =
        createProgramStageDataElement(stageB, dataElementB, 2);
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);
    programStageDataElementStore.save(stageDataElementC);
    programStageDataElementStore.save(stageDataElementD);
    Program programB = createProgram('B', new HashSet<>(), organisationUnitB);
    programService.addProgram(programB);
    stageC = createProgramStage('C', 0);
    stageC.setProgram(programB);
    programStageService.saveProgramStage(stageC);
    ProgramStage stageD = createProgramStage('D', 0);
    stageD.setProgram(programB);
    stageC.setRepeatable(true);
    programStageService.saveProgramStage(stageD);
    programStages = new HashSet<>();
    programStages.add(stageC);
    programStages.add(stageD);
    programB.getProgramStages().addAll(programStages);
    programService.updateProgram(programB);
    DateTime testDate1 = DateTime.now();
    testDate1.withTimeAtStartOfDay();
    testDate1 = testDate1.minusDays(70);
    Date incidenDate = testDate1.toDate();
    DateTime testDate2 = DateTime.now();
    testDate2.withTimeAtStartOfDay();
    Date enrollmentDate = testDate2.toDate();
    enrollmentA = new Enrollment(enrollmentDate, incidenDate, trackedEntityA, programA);
    enrollmentA.setUid("UID-PIA");
    enrollmentService.addEnrollment(enrollmentA);
    enrollmentB = new Enrollment(enrollmentDate, incidenDate, trackedEntityB, programB);
    enrollmentService.addEnrollment(enrollmentB);
    Event eventA = new Event(enrollmentA, stageA);
    eventA.setScheduledDate(enrollmentDate);
    eventA.setUid("UID-A");
    eventA.setAttributeOptionCombo(coA);
    Event eventB = new Event(enrollmentA, stageB);
    eventB.setScheduledDate(enrollmentDate);
    eventB.setUid("UID-B");
    eventB.setAttributeOptionCombo(coA);
    Event eventC = new Event(enrollmentB, stageC);
    eventC.setScheduledDate(enrollmentDate);
    eventC.setUid("UID-C");
    eventC.setAttributeOptionCombo(coA);
    Event eventD1 = new Event(enrollmentB, stageD);
    eventD1.setScheduledDate(enrollmentDate);
    eventD1.setUid("UID-D1");
    eventD1.setAttributeOptionCombo(coA);
    Event eventD2 = new Event(enrollmentB, stageD);
    eventD2.setScheduledDate(enrollmentDate);
    eventD2.setUid("UID-D2");
    eventD2.setAttributeOptionCombo(coA);
  }

  @Test
  void testGetWithScheduledNotifications() {
    ProgramNotificationTemplate
        a1 =
            createProgramNotificationTemplate(
                "a1",
                -1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        a2 =
            createProgramNotificationTemplate(
                "a2",
                -2,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        a3 =
            createProgramNotificationTemplate(
                "a3",
                1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        b1 =
            createProgramNotificationTemplate(
                "b1",
                -1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        b2 =
            createProgramNotificationTemplate(
                "b2",
                -2,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        b3 =
            createProgramNotificationTemplate(
                "b3",
                1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        c1 =
            createProgramNotificationTemplate(
                "c1",
                -1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        c2 =
            createProgramNotificationTemplate(
                "c2",
                -2,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE),
        c3 =
            createProgramNotificationTemplate(
                "c3",
                1,
                SCHEDULED_DAYS_DUE_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE);
    programNotificationStore.save(a1);
    programNotificationStore.save(a2);
    programNotificationStore.save(a3);
    programNotificationStore.save(b1);
    programNotificationStore.save(b2);
    programNotificationStore.save(b3);
    programNotificationStore.save(c1);
    programNotificationStore.save(c2);
    programNotificationStore.save(c3);
    // Stage
    stageA.setNotificationTemplates(Sets.newHashSet(a1, a2, a3));
    programStageService.updateProgramStage(stageA);
    stageB.setNotificationTemplates(Sets.newHashSet(b1, b2, b3));
    programStageService.updateProgramStage(stageB);
    stageC.setNotificationTemplates(Sets.newHashSet(c1, c2, c3));
    programStageService.updateProgramStage(stageC);
    // Dates
    Calendar cal = Calendar.getInstance();
    PeriodType.clearTimeOfDay(cal);
    // 2016-01-10 -> "today"
    Date today = cal.getTime();
    // 2016-01-11
    cal.add(Calendar.DATE, 1);
    Date tomorrow = cal.getTime();
    // 2016-01-09
    cal.add(Calendar.DATE, -2);
    Date yesterday = cal.getTime();
    // Events
    Event eventA = new Event(enrollmentA, stageA);
    eventA.setScheduledDate(tomorrow);
    eventA.setAttributeOptionCombo(coA);
    manager.save(eventA);
    Event eventB = new Event(enrollmentB, stageB);
    eventB.setScheduledDate(today);
    eventB.setAttributeOptionCombo(coA);
    manager.save(eventB);
    Event eventC = new Event(enrollmentB, stageC);
    eventC.setScheduledDate(yesterday);
    eventC.setAttributeOptionCombo(coA);
    manager.save(eventC);
    // Queries
    List<Event> results;
    // A
    results = programNotificationService.getWithScheduledNotifications(a1, today);
    assertEquals(1, results.size());
    assertEquals(eventA, results.get(0));
    results = programNotificationService.getWithScheduledNotifications(a2, today);
    assertEquals(0, results.size());
    results = programNotificationService.getWithScheduledNotifications(a3, today);
    assertEquals(0, results.size());
    // B
    results = programNotificationService.getWithScheduledNotifications(b1, today);
    assertEquals(0, results.size());
    results = programNotificationService.getWithScheduledNotifications(b2, today);
    assertEquals(0, results.size());
    results = programNotificationService.getWithScheduledNotifications(b3, today);
    assertEquals(0, results.size());
    // C
    results = programNotificationService.getWithScheduledNotifications(c1, today);
    assertEquals(0, results.size());
    results = programNotificationService.getWithScheduledNotifications(c2, today);
    assertEquals(0, results.size());
    results = programNotificationService.getWithScheduledNotifications(c3, today);
    assertEquals(1, results.size());
    assertEquals(eventC, results.get(0));
  }
}
