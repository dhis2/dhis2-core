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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.test.TrackedEntityAttributeValueUtils.saveTrackedEntityAttributeValue;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
class TrackedEntityAttributeValueServiceTest extends PostgresIntegrationTestBase {

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private FileResourceService fileResourceService;

  private TrackedEntityAttribute attributeA;

  private TrackedEntityAttribute attributeB;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntityAttributeValue attributeValueA;

  private TrackedEntityAttributeValue attributeValueB;

  private TrackedEntityAttributeValue attributeValueC;

  @BeforeEach
  void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    trackedEntityA = createTrackedEntity(organisationUnit, trackedEntityType);
    trackedEntityB = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntityA);
    manager.save(trackedEntityB);
    attributeA = createTrackedEntityAttribute('A');
    attributeB = createTrackedEntityAttribute('B');
    attributeService.addTrackedEntityAttribute(attributeA);
    attributeService.addTrackedEntityAttribute(attributeB);
    attributeValueA = new TrackedEntityAttributeValue(attributeA, trackedEntityA, "A");
    attributeValueB = new TrackedEntityAttributeValue(attributeB, trackedEntityA, "B");
    attributeValueC = new TrackedEntityAttributeValue(attributeA, trackedEntityB, "C");
  }

  @Test
  void testSaveTrackedEntityAttributeValue() {
    saveTrackedEntityAttributeValue(entityManager, attributeValueA);
    saveTrackedEntityAttributeValue(entityManager, attributeValueB);
    assertContainsOnly(
        Set.of(attributeValueA, attributeValueB),
        attributeValueService.getTrackedEntityAttributeValues(trackedEntityA));
  }

  @Test
  void shouldDeleteAllAttributeValuesOfTrackedEntity() {
    saveTrackedEntityAttributeValue(entityManager, attributeValueA);
    saveTrackedEntityAttributeValue(entityManager, attributeValueB);
    saveTrackedEntityAttributeValue(entityManager, attributeValueC);

    attributeValueService.deleteTrackedEntityAttributeValues(trackedEntityA);

    assertIsEmpty(attributeValueService.getTrackedEntityAttributeValues(trackedEntityA));
    assertContainsOnly(
        Set.of(attributeValueC),
        attributeValueService.getTrackedEntityAttributeValues(trackedEntityB));
  }

  @Test
  void testGetTrackedEntityAttributeValue() {
    saveTrackedEntityAttributeValue(entityManager, attributeValueA);
    saveTrackedEntityAttributeValue(entityManager, attributeValueC);
    assertContainsOnly(
        Set.of(attributeValueA),
        attributeValueService.getTrackedEntityAttributeValues(trackedEntityA));
    assertContainsOnly(
        Set.of(attributeValueC),
        attributeValueService.getTrackedEntityAttributeValues(trackedEntityB));
  }

  @Test
  void testGetTrackedEntityAttributeValuesByEntityInstance() {
    saveTrackedEntityAttributeValue(entityManager, attributeValueA);
    saveTrackedEntityAttributeValue(entityManager, attributeValueB);
    saveTrackedEntityAttributeValue(entityManager, attributeValueC);
    List<TrackedEntityAttributeValue> attributeValues =
        attributeValueService.getTrackedEntityAttributeValues(trackedEntityA);
    assertEquals(2, attributeValues.size());
    assertTrue(equals(attributeValues, attributeValueA, attributeValueB));
    attributeValues = attributeValueService.getTrackedEntityAttributeValues(trackedEntityB);
    assertEquals(1, attributeValues.size());
    assertTrue(equals(attributeValues, attributeValueC));
  }

  @Test
  void shouldKeepFileResourcesAssignedWhenDeletingAttributeValues() {
    attributeA.setValueType(ValueType.IMAGE);
    attributeB.setValueType(ValueType.FILE_RESOURCE);
    attributeService.updateTrackedEntityAttribute(attributeA);
    attributeService.updateTrackedEntityAttribute(attributeB);
    FileResource fileResourceA = assignedFileResource('A', "image/jpg");
    FileResource fileResourceB = assignedFileResource('B', "application/pdf");
    attributeValueA.setValue(fileResourceA.getUid());
    attributeValueB.setValue(fileResourceB.getUid());
    saveTrackedEntityAttributeValue(entityManager, attributeValueA);
    saveTrackedEntityAttributeValue(entityManager, attributeValueB);

    attributeValueService.deleteTrackedEntityAttributeValues(trackedEntityA);

    assertIsEmpty(attributeValueService.getTrackedEntityAttributeValues(trackedEntityA));
    assertTrue(fileResourceService.getFileResource(fileResourceA.getUid()).isAssigned());
    assertTrue(fileResourceService.getFileResource(fileResourceB.getUid()).isAssigned());
  }

  private FileResource assignedFileResource(char uniqueChar, String contentType) {
    byte[] content = ("filecontent" + uniqueChar).getBytes();
    FileResource fileResource = createFileResource(uniqueChar, content);
    fileResource.setContentType(contentType);
    fileResource.setAssigned(true);
    fileResourceService.asyncSaveFileResource(fileResource, content);
    return fileResource;
  }
}
