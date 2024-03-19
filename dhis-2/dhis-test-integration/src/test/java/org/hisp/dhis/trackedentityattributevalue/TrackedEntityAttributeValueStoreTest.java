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
package org.hisp.dhis.trackedentityattributevalue;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityAttributeValueStoreTest extends SingleSetupIntegrationTestBase {

  @Autowired private TrackedEntityAttributeValueStore attributeValueStore;

  @Autowired private TrackedEntityService entityInstanceService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueChangeLogStore attributeValueAuditStore;

  @Autowired private RenderService renderService;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private TrackedEntityAttribute atC;

  private TrackedEntityAttribute atD;

  private TrackedEntity teiA;

  private TrackedEntity teiB;

  private TrackedEntity teiC;

  private TrackedEntity teiD;

  private TrackedEntityAttributeValue avA;

  private TrackedEntityAttributeValue avB;

  private TrackedEntityAttributeValue avC;

  private TrackedEntityAttributeValue avD;

  @Override
  public void setUpTest() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    teiA = createTrackedEntity(organisationUnit);
    teiB = createTrackedEntity(organisationUnit);
    teiC = createTrackedEntity(organisationUnit);
    teiD = createTrackedEntity(organisationUnit);
    entityInstanceService.addTrackedEntity(teiA);
    entityInstanceService.addTrackedEntity(teiB);
    entityInstanceService.addTrackedEntity(teiC);
    entityInstanceService.addTrackedEntity(teiD);
    atA = createTrackedEntityAttribute('A');
    atB = createTrackedEntityAttribute('B');
    atC = createTrackedEntityAttribute('C');
    atD = createTrackedEntityAttribute('D');
    attributeService.addTrackedEntityAttribute(atA);
    attributeService.addTrackedEntityAttribute(atB);
    attributeService.addTrackedEntityAttribute(atC);
    attributeService.addTrackedEntityAttribute(atD);
    avA = new TrackedEntityAttributeValue(atA, teiA, "A");
    avB = new TrackedEntityAttributeValue(atB, teiA, "B");
    avC = new TrackedEntityAttributeValue(atC, teiB, "C");
    avD = new TrackedEntityAttributeValue(atD, teiB, "D");
    avA.setAutoFields();
    avB.setAutoFields();
    avC.setAutoFields();
    avD.setAutoFields();
  }

  @Test
  void testSaveTrackedEntityAttributeValue() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    assertNotNull(attributeValueStore.get(teiA, atA));
    assertNotNull(attributeValueStore.get(teiA, atA));
  }

  @Test
  void testDeleteTrackedEntityAttributeValueByEntityInstance() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    assertNotNull(attributeValueStore.get(teiA, atA));
    assertNotNull(attributeValueStore.get(teiA, atB));
    assertNotNull(attributeValueStore.get(teiB, atC));
    attributeValueStore.deleteByTrackedEntity(teiA);
    assertNull(attributeValueStore.get(teiA, atA));
    assertNull(attributeValueStore.get(teiA, atB));
    assertNotNull(attributeValueStore.get(teiB, atC));
    attributeValueStore.deleteByTrackedEntity(teiB);
    assertNull(attributeValueStore.get(teiA, atA));
    assertNull(attributeValueStore.get(teiA, atB));
    assertNull(attributeValueStore.get(teiB, atC));
  }

  @Test
  void testGetTrackedEntityAttributeValue() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avC);
    assertEquals(avA, attributeValueStore.get(teiA, atA));
    assertEquals(avC, attributeValueStore.get(teiB, atC));
  }

  @Test
  void testGetByEntityInstance() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    List<TrackedEntityAttributeValue> attributeValues = attributeValueStore.get(teiA);
    assertContainsOnly(List.of(avA, avB), attributeValues);
    attributeValues = attributeValueStore.get(teiB);
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
    attributeValueAuditStore.addTrackedEntityAttributeValueChangeLog(auditA);
    attributeValueAuditStore.addTrackedEntityAttributeValueChangeLog(auditB);
    attributeValueAuditStore.addTrackedEntityAttributeValueChangeLog(auditC);
    attributeValueAuditStore.addTrackedEntityAttributeValueChangeLog(auditD);

    TrackedEntityAttributeValueChangeLogQueryParams params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setTrackedEntityAttributes(List.of(atA))
            .setTrackedEntities(List.of(teiA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));

    assertContainsOnly(
        List.of(auditA), attributeValueAuditStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setTrackedEntities(List.of(teiA))
            .setAuditTypes(List.of(ChangeLogType.UPDATE));

    assertContainsOnly(
        List.of(auditA, auditB),
        attributeValueAuditStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.CREATE));

    assertContainsOnly(
        List.of(auditC), attributeValueAuditStore.getTrackedEntityAttributeValueChangeLogs(params));

    params =
        new TrackedEntityAttributeValueChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.CREATE, ChangeLogType.DELETE));

    assertContainsOnly(
        List.of(auditC, auditD),
        attributeValueAuditStore.getTrackedEntityAttributeValueChangeLogs(params));
  }
}
