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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityAttributeValueChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityAttributeValueChangeLogQueryParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityAttributeValueChangeLogStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackedEntityAttributeValueStoreTest extends PostgresIntegrationTestBase {

  @Autowired private HibernateTrackedEntityAttributeValueStore attributeValueStore;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueChangeLogStore attributeValueChangeLogStore;

  @Autowired private RenderService renderService;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private TrackedEntityAttribute atC;

  private TrackedEntityAttribute atD;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityC;

  private TrackedEntity trackedEntityD;

  private TrackedEntityAttributeValue avA;

  private TrackedEntityAttributeValue avB;

  private TrackedEntityAttributeValue avC;

  private TrackedEntityAttributeValue avD;

  @BeforeAll
  void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    trackedEntityA = createTrackedEntity(organisationUnit, trackedEntityType);
    trackedEntityB = createTrackedEntity(organisationUnit, trackedEntityType);
    trackedEntityC = createTrackedEntity(organisationUnit, trackedEntityType);
    trackedEntityD = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntityA);
    manager.save(trackedEntityB);
    manager.save(trackedEntityC);
    manager.save(trackedEntityD);
    atA = createTrackedEntityAttribute('A');
    atB = createTrackedEntityAttribute('B');
    atC = createTrackedEntityAttribute('C');
    atD = createTrackedEntityAttribute('D');
    attributeService.addTrackedEntityAttribute(atA);
    attributeService.addTrackedEntityAttribute(atB);
    attributeService.addTrackedEntityAttribute(atC);
    attributeService.addTrackedEntityAttribute(atD);
    avA = new TrackedEntityAttributeValue(atA, trackedEntityA, "A");
    avB = new TrackedEntityAttributeValue(atB, trackedEntityA, "B");
    avC = new TrackedEntityAttributeValue(atC, trackedEntityB, "C");
    avD = new TrackedEntityAttributeValue(atD, trackedEntityB, "D");
    avA.setAutoFields();
    avB.setAutoFields();
    avC.setAutoFields();
    avD.setAutoFields();
  }

  @Test
  void testSaveTrackedEntityAttributeValue() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    assertNotNull(attributeValueStore.get(trackedEntityA, atA));
    assertNotNull(attributeValueStore.get(trackedEntityA, atA));
  }

  @Test
  void testDeleteTrackedEntityAttributeValueByEntityInstance() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    assertNotNull(attributeValueStore.get(trackedEntityA, atA));
    assertNotNull(attributeValueStore.get(trackedEntityA, atB));
    assertNotNull(attributeValueStore.get(trackedEntityB, atC));
    attributeValueStore.deleteByTrackedEntity(trackedEntityA);
    assertNull(attributeValueStore.get(trackedEntityA, atA));
    assertNull(attributeValueStore.get(trackedEntityA, atB));
    assertNotNull(attributeValueStore.get(trackedEntityB, atC));
    attributeValueStore.deleteByTrackedEntity(trackedEntityB);
    assertNull(attributeValueStore.get(trackedEntityA, atA));
    assertNull(attributeValueStore.get(trackedEntityA, atB));
    assertNull(attributeValueStore.get(trackedEntityB, atC));
  }

  @Test
  void testGetTrackedEntityAttributeValue() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avC);
    assertEquals(avA, attributeValueStore.get(trackedEntityA, atA));
    assertEquals(avC, attributeValueStore.get(trackedEntityB, atC));
  }

  @Test
  void testGetByEntityInstance() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    List<TrackedEntityAttributeValue> attributeValues = attributeValueStore.get(trackedEntityA);
    assertContainsOnly(List.of(avA, avB), attributeValues);
    attributeValues = attributeValueStore.get(trackedEntityB);
    assertContainsOnly(List.of(avC), attributeValues);
  }

  @Test
  void testGetTrackedEntityAttributeValueAudits() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    attributeValueStore.saveVoid(avD);
    TrackedEntityAttributeValueChangeLog auditA =
        new TrackedEntityAttributeValueChangeLog(
            avA, renderService.toJsonAsString(avA), "userA", ChangeLogType.UPDATE);
    TrackedEntityAttributeValueChangeLog auditB =
        new TrackedEntityAttributeValueChangeLog(
            avB, renderService.toJsonAsString(avB), "userA", ChangeLogType.UPDATE);
    TrackedEntityAttributeValueChangeLog auditC =
        new TrackedEntityAttributeValueChangeLog(
            avC, renderService.toJsonAsString(avC), "userA", ChangeLogType.CREATE);
    TrackedEntityAttributeValueChangeLog auditD =
        new TrackedEntityAttributeValueChangeLog(
            avD, renderService.toJsonAsString(avD), "userA", ChangeLogType.DELETE);
    attributeValueChangeLogStore.addTrackedEntityAttributeValueChangeLog(auditA);
    attributeValueChangeLogStore.addTrackedEntityAttributeValueChangeLog(auditB);
    attributeValueChangeLogStore.addTrackedEntityAttributeValueChangeLog(auditC);
    attributeValueChangeLogStore.addTrackedEntityAttributeValueChangeLog(auditD);

    TrackedEntityAttributeValueChangeLogQueryParams params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setTrackedEntityAttributes(List.of(atA))
            .setTrackedEntities(List.of(trackedEntityA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));

    assertContainsOnly(
        List.of(auditA),
        attributeValueChangeLogStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setTrackedEntities(List.of(trackedEntityA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));

    assertContainsOnly(
        List.of(auditA, auditB),
        attributeValueChangeLogStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.CREATE));

    assertContainsOnly(
        List.of(auditC),
        attributeValueChangeLogStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.CREATE, ChangeLogType.DELETE));

    assertContainsOnly(
        List.of(auditC, auditD),
        attributeValueChangeLogStore.getTrackedEntityAttributeValueChangeLogs(params));
  }
}
