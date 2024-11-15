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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.audit.UserInfoTestHelper;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLog;
import org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLogQueryParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackedEntityDataValueChangeLogStoreTest extends PostgresIntegrationTestBase {
  private static final String USER_A = "userA";

  private static final UserInfoSnapshot USER_SNAP_A = UserInfoTestHelper.testUserInfo(USER_A);

  @Autowired private EventChangeLogService changeLogService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager manager;

  private CategoryOptionCombo coc;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnit ouD;

  private OrganisationUnit ouE;

  private Program pA;

  private ProgramStage psA;

  private ProgramStage psB;

  private DataElement deA;

  private DataElement deB;

  private Event eventA;

  private Event eventB;

  private Event eventC;

  private Event eventD;

  private Event eventE;

  private EventDataValue dvA;

  private EventDataValue dvB;

  private EventDataValue dvC;

  private EventDataValue dvD;

  private EventDataValue dvE;

  @BeforeAll
  void setUp() {
    coc = categoryService.getDefaultCategoryOptionCombo();
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);
    ouC = createOrganisationUnit('C', ouA);
    ouD = createOrganisationUnit('D', ouB);
    ouE = createOrganisationUnit('E', ouD);
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    organisationUnitService.addOrganisationUnit(ouD);
    organisationUnitService.addOrganisationUnit(ouE);

    pA = createProgram('A', new HashSet<>(), ouA);
    programService.addProgram(pA);

    psA = new ProgramStage("StageA", pA);
    psA.setSortOrder(1);
    programStageService.saveProgramStage(psA);
    psB = new ProgramStage("StageB", pA);
    psB.setSortOrder(2);
    programStageService.saveProgramStage(psB);
    pA.setProgramStages(Set.of(psA, psB));
    programService.updateProgram(pA);

    deA = createDataElement('A');
    deB = createDataElement('B');
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);

    TrackedEntity teA = createTrackedEntity(ouA, createDefaultTrackedEntityType());
    manager.save(teA);

    Enrollment enrollment = createEnrollment(pA, teA, ouA);
    manager.save(enrollment);
    teA.getEnrollments().add(enrollment);
    manager.update(teA);

    dvA = new EventDataValue(deA.getUid(), "A", USER_SNAP_A);
    dvB = new EventDataValue(deB.getUid(), "B", USER_SNAP_A);
    dvC = new EventDataValue(deA.getUid(), "C", USER_SNAP_A);
    dvD = new EventDataValue(deB.getUid(), "D", USER_SNAP_A);
    dvE = new EventDataValue(deB.getUid(), "E", USER_SNAP_A);

    eventA = createEvent(enrollment, psA, ouA, Set.of(dvA, dvB));
    eventB = createEvent(enrollment, psB, ouB, Set.of(dvC, dvD));
    eventC = createEvent(enrollment, psA, ouC, Set.of(dvA, dvB));
    eventD = createEvent(enrollment, psB, ouD, Set.of(dvC, dvD));
    eventE = createEvent(enrollment, psA, ouE, Set.of(dvA, dvE));
    manager.save(eventA);
    manager.save(eventB);
    manager.save(eventC);
    manager.save(eventD);
    manager.save(eventE);
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByDataElement() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventA,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventB,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setDataElements(List.of(deA, deB))
            .setEvents(List.of(eventA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(
        List.of(dvaA, dvaB), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(2, changeLogService.countTrackedEntityDataValueChangeLogs(params));

    params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setDataElements(List.of(deA))
            .setEvents(List.of(eventA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(List.of(dvaA), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(1, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByOrgUnitSelected() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventA,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventB,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setOrgUnits(List.of(ouA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(
        List.of(dvaA, dvaB), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(2, changeLogService.countTrackedEntityDataValueChangeLogs(params));

    params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setOrgUnits(List.of(ouB))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(List.of(dvaC), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(1, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByOrgUnitDescendants() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventB,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventC,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaD =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventD,
            dvD.getAuditValue(),
            USER_A,
            dvD.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaE =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventE,
            dvE.getAuditValue(),
            USER_A,
            dvE.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaD);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaE);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setOrgUnits(List.of(ouB))
            .setOuMode(OrganisationUnitSelectionMode.DESCENDANTS)
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(
        List.of(dvaB, dvaD, dvaE), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(3, changeLogService.countTrackedEntityDataValueChangeLogs(params));

    params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setOrgUnits(List.of(ouA))
            .setOuMode(OrganisationUnitSelectionMode.DESCENDANTS)
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(
        List.of(dvaA, dvaB, dvaC, dvaD, dvaE),
        changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(5, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByProgramStage() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventA,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventB,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setProgramStages(List.of(psA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(
        List.of(dvaA, dvaB), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(2, changeLogService.countTrackedEntityDataValueChangeLogs(params));

    params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setProgramStages(List.of(psB))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));
    assertContainsOnly(List.of(dvaC), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(1, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByStartEndDate() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    dvaA.setCreated(getDate(2021, 6, 1));
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventA,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    dvaB.setCreated(getDate(2021, 7, 1));
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventB,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    dvaC.setCreated(getDate(2021, 8, 1));
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setDataElements(List.of(deA, deB))
            .setStartDate(getDate(2021, 6, 15))
            .setEndDate(getDate(2021, 8, 15));
    assertContainsOnly(
        List.of(dvaB, dvaC), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(2, changeLogService.countTrackedEntityDataValueChangeLogs(params));

    params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setDataElements(List.of(deA, deB))
            .setStartDate(getDate(2021, 6, 15))
            .setEndDate(getDate(2021, 7, 15));
    assertContainsOnly(List.of(dvaB), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(1, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }

  @Test
  void testGetTrackedEntityDataValueAuditsByAuditType() {
    TrackedEntityDataValueChangeLog dvaA =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventA,
            dvA.getAuditValue(),
            USER_A,
            dvA.getProvidedElsewhere(),
            ChangeLogType.CREATE);
    TrackedEntityDataValueChangeLog dvaB =
        new TrackedEntityDataValueChangeLog(
            deB,
            eventA,
            dvB.getAuditValue(),
            USER_A,
            dvB.getProvidedElsewhere(),
            ChangeLogType.UPDATE);
    TrackedEntityDataValueChangeLog dvaC =
        new TrackedEntityDataValueChangeLog(
            deA,
            eventB,
            dvC.getAuditValue(),
            USER_A,
            dvC.getProvidedElsewhere(),
            ChangeLogType.DELETE);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaA);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaB);
    changeLogService.addTrackedEntityDataValueChangeLog(dvaC);

    TrackedEntityDataValueChangeLogQueryParams params =
        new TrackedEntityDataValueChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.UPDATE, ChangeLogType.DELETE));
    assertContainsOnly(
        List.of(dvaB, dvaC), changeLogService.getTrackedEntityDataValueChangeLogs(params));
    assertEquals(2, changeLogService.countTrackedEntityDataValueChangeLogs(params));
  }
}
