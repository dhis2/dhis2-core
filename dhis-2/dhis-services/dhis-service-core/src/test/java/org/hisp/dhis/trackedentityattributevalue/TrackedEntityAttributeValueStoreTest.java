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
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityAttributeValueStoreTest extends DhisSpringTest {

  @Autowired private TrackedEntityAttributeValueStore attributeValueStore;

  @Autowired private TrackedEntityInstanceService entityInstanceService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueAuditStore attributeValueAuditStore;

  @Autowired private RenderService renderService;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private TrackedEntityAttribute atC;

  private TrackedEntityAttribute atD;

  private TrackedEntityInstance teiA;

  private TrackedEntityInstance teiB;

  private TrackedEntityInstance teiC;

  private TrackedEntityInstance teiD;

  private TrackedEntityAttributeValue avA;

  private TrackedEntityAttributeValue avB;

  private TrackedEntityAttributeValue avC;

  private TrackedEntityAttributeValue avD;

  @Override
  public void setUpTest() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    teiA = createTrackedEntityInstance(organisationUnit);
    teiB = createTrackedEntityInstance(organisationUnit);
    teiC = createTrackedEntityInstance(organisationUnit);
    teiD = createTrackedEntityInstance(organisationUnit);
    entityInstanceService.addTrackedEntityInstance(teiA);
    entityInstanceService.addTrackedEntityInstance(teiB);
    entityInstanceService.addTrackedEntityInstance(teiC);
    entityInstanceService.addTrackedEntityInstance(teiD);
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
    attributeValueStore.deleteByTrackedEntityInstance(teiA);
    assertNull(attributeValueStore.get(teiA, atA));
    assertNull(attributeValueStore.get(teiA, atB));
    assertNotNull(attributeValueStore.get(teiB, atC));
    attributeValueStore.deleteByTrackedEntityInstance(teiB);
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
    assertContainsOnly(attributeValues, avA, avB);
    attributeValues = attributeValueStore.get(teiB);
    assertContainsOnly(attributeValues, avC);
  }

  @Test
  void testGetTrackedEntityAttributeValueAudits() {
    attributeValueStore.saveVoid(avA);
    attributeValueStore.saveVoid(avB);
    attributeValueStore.saveVoid(avC);
    attributeValueStore.saveVoid(avD);
    TrackedEntityAttributeValueAudit auditA =
        new TrackedEntityAttributeValueAudit(
            avA, renderService.toJsonAsString(avA), "userA", AuditType.UPDATE);
    TrackedEntityAttributeValueAudit auditB =
        new TrackedEntityAttributeValueAudit(
            avB, renderService.toJsonAsString(avB), "userA", AuditType.UPDATE);
    TrackedEntityAttributeValueAudit auditC =
        new TrackedEntityAttributeValueAudit(
            avC, renderService.toJsonAsString(avC), "userA", AuditType.CREATE);
    TrackedEntityAttributeValueAudit auditD =
        new TrackedEntityAttributeValueAudit(
            avD, renderService.toJsonAsString(avD), "userA", AuditType.DELETE);
    attributeValueAuditStore.addTrackedEntityAttributeValueAudit(auditA);
    attributeValueAuditStore.addTrackedEntityAttributeValueAudit(auditB);
    attributeValueAuditStore.addTrackedEntityAttributeValueAudit(auditC);
    attributeValueAuditStore.addTrackedEntityAttributeValueAudit(auditD);

    TrackedEntityAttributeValueAuditQueryParams params =
        new TrackedEntityAttributeValueAuditQueryParams()
            .setTrackedEntityAttributes(List.of(atA))
            .setTrackedEntityInstances(List.of(teiA))
            .setAuditTypes(List.of(AuditType.UPDATE));

    assertContainsOnly(
        attributeValueAuditStore.getTrackedEntityAttributeValueAudits(params), auditA);

    params =
        new TrackedEntityAttributeValueAuditQueryParams()
            .setTrackedEntityInstances(List.of(teiA))
            .setAuditTypes(List.of(AuditType.UPDATE));

    assertContainsOnly(
        attributeValueAuditStore.getTrackedEntityAttributeValueAudits(params), auditA, auditB);

    params =
        new TrackedEntityAttributeValueAuditQueryParams().setAuditTypes(List.of(AuditType.CREATE));

    assertContainsOnly(
        attributeValueAuditStore.getTrackedEntityAttributeValueAudits(params), auditC);

    params =
        new TrackedEntityAttributeValueAuditQueryParams()
            .setAuditTypes(List.of(AuditType.CREATE, AuditType.DELETE));

    assertContainsOnly(
        attributeValueAuditStore.getTrackedEntityAttributeValueAudits(params), auditC, auditD);
  }
}
