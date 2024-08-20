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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityServiceTest extends PostgresIntegrationTestBase {
  private static final String TE_A_UID = uidWithPrefix('A');
  private static final String TE_B_UID = uidWithPrefix('B');
  private static final String TE_C_UID = uidWithPrefix('C');
  private static final String TE_D_UID = uidWithPrefix('D');
  private static final String ENROLLMENT_A_UID = UID.of(CodeGenerator.generateUid()).getValue();
  private static final String EVENT_A_UID = UID.of(CodeGenerator.generateUid()).getValue();

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  private Event event;

  private Enrollment enrollment;

  private TrackedEntity trackedEntityA1;

  private TrackedEntity trackedEntityB1;

  @BeforeEach
  void setUp() {
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    TrackedEntityAttribute attrD = createTrackedEntityAttribute('D');
    TrackedEntityAttribute attrE = createTrackedEntityAttribute('E');
    TrackedEntityAttribute filtF = createTrackedEntityAttribute('F');
    TrackedEntityAttribute filtG = createTrackedEntityAttribute('G');
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('H');

    trackedEntityAttributeService.addTrackedEntityAttribute(attrD);
    trackedEntityAttributeService.addTrackedEntityAttribute(attrE);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtF);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtG);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    attributeService.addTrackedEntityAttribute(createTrackedEntityAttribute('A'));
    trackedEntityA1 = createTrackedEntity(organisationUnit);
    trackedEntityB1 = createTrackedEntity(organisationUnit);
    TrackedEntity trackedEntityC1 = createTrackedEntity(organisationUnit);
    TrackedEntity trackedEntityD1 = createTrackedEntity(organisationUnit);
    trackedEntityA1.setUid(TE_A_UID);
    trackedEntityB1.setUid(TE_B_UID);
    trackedEntityC1.setUid(TE_C_UID);
    trackedEntityD1.setUid(TE_D_UID);
    Program program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
    ProgramStage stageA = createProgramStage('A', program);
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    program.setProgramStages(programStages);
    programService.updateProgram(program);
    DateTime enrollmentDate = DateTime.now();
    enrollmentDate.withTimeAtStartOfDay();
    enrollmentDate = enrollmentDate.minusDays(70);
    DateTime incidentDate = DateTime.now();
    incidentDate.withTimeAtStartOfDay();
    enrollment =
        new Enrollment(enrollmentDate.toDate(), incidentDate.toDate(), trackedEntityA1, program);
    enrollment.setUid(ENROLLMENT_A_UID);
    enrollment.setOrganisationUnit(organisationUnit);
    event = createEvent(stageA, enrollment, organisationUnit);
    event.setUid(EVENT_A_UID);

    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    attributeService.addTrackedEntityAttribute(attrD);
    attributeService.addTrackedEntityAttribute(attrE);
    attributeService.addTrackedEntityAttribute(filtF);
    attributeService.addTrackedEntityAttribute(filtG);

    User user = createUserWithAuth("testUser");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContextUser(user);
  }

  @Test
  void testSaveTrackedEntity() {
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    assertNotNull(trackedEntityService.getTrackedEntity(trackedEntityA1.getUid()));
    assertNotNull(trackedEntityService.getTrackedEntity(trackedEntityB1.getUid()));
  }

  @Test
  void testDeleteTrackedEntity() {
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    TrackedEntity trackedEntityA = trackedEntityService.getTrackedEntity(trackedEntityA1.getUid());
    TrackedEntity trackedEntityB = trackedEntityService.getTrackedEntity(trackedEntityB1.getUid());
    assertNotNull(trackedEntityA);
    assertNotNull(trackedEntityB);
    manager.delete(trackedEntityA1);
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNotNull(trackedEntityService.getTrackedEntity(trackedEntityB.getUid()));
    manager.delete(trackedEntityB1);
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityB.getUid()));
  }

  @Test
  void testDeleteTrackedEntityAndLinkedEnrollmentsAndEvents() throws NotFoundException {
    manager.save(trackedEntityA1);
    manager.save(enrollment);
    manager.save(event);
    long eventIdA = event.getId();
    enrollment.setEvents(Set.of(event));
    trackedEntityA1.setEnrollments(Set.of(enrollment));
    manager.update(enrollment);
    manager.update(trackedEntityA1);
    TrackedEntity trackedEntityA = trackedEntityService.getTrackedEntity(trackedEntityA1.getUid());
    Enrollment psA = manager.get(Enrollment.class, enrollment.getUid());
    Event eventA = manager.get(Event.class, eventIdA);
    assertNotNull(trackedEntityA);
    assertNotNull(psA);
    assertNotNull(eventA);
    trackerObjectDeletionService.deleteTrackedEntities(List.of(trackedEntityA.getUid()));
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNull(manager.get(Enrollment.class, enrollment.getUid()));
    assertNull(manager.get(Event.class, eventIdA));
  }

  @Test
  void testUpdateTrackedEntity() {
    manager.save(trackedEntityA1);
    assertNotNull(trackedEntityService.getTrackedEntity(trackedEntityA1.getUid()));
    trackedEntityA1.setName("B");
    manager.update(trackedEntityA1);
    assertEquals("B", trackedEntityService.getTrackedEntity(trackedEntityA1.getUid()).getName());
  }

  @Test
  void testGetTrackedEntityById() {
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    assertEquals(trackedEntityA1, trackedEntityService.getTrackedEntity(trackedEntityA1.getUid()));
    assertEquals(trackedEntityB1, trackedEntityService.getTrackedEntity(trackedEntityB1.getUid()));
  }

  @Test
  void testGetTrackedEntityByUid() {
    trackedEntityA1.setUid("A1");
    trackedEntityB1.setUid("B1");
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    assertEquals(trackedEntityA1, trackedEntityService.getTrackedEntity("A1"));
    assertEquals(trackedEntityB1, trackedEntityService.getTrackedEntity("B1"));
  }

  @Test
  void testStoredByColumnForTrackedEntity() {
    trackedEntityA1.setStoredBy("test");
    manager.save(trackedEntityA1);
    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityA1.getUid());
    assertEquals("test", trackedEntity.getStoredBy());
  }

  private static String uidWithPrefix(char prefix) {
    String value = prefix + CodeGenerator.generateUid().substring(0, 10);
    return UID.of(value).getValue();
  }
}
