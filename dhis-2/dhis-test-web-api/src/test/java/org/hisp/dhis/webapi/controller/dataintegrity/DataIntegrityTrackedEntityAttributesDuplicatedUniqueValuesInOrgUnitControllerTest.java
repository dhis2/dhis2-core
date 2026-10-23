/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails.JsonDataIntegrityIssue;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for tracked entity attributes unique within an org unit having the same value for more than
 * one tracked entity in the same org unit {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/tracked_entity_attributes/tracked_entity_attributes_duplicated_unique_values_in_org_unit.yaml}
 */
class DataIntegrityTrackedEntityAttributesDuplicatedUniqueValuesInOrgUnitControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK =
      "tracked_entity_attributes_duplicated_unique_values_in_org_unit";

  private static final String DETAILS_ID_TYPE = "trackedEntityAttributes";

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private TrackedEntityType trackedEntityType;

  private TrackedEntityAttribute scopedA;

  private TrackedEntityAttribute scopedB;

  @BeforeEach
  void setUp() {
    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);
    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    scopedA = uniqueAttribute('A', true);
    scopedB = uniqueAttribute('B', true);
  }

  @Test
  void shouldReportNoIssuesWhenThereIsNoTrackerData() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportNoIssuesWhenTheSameValueIsInDifferentOrgUnits() {
    trackedEntityWithValue(orgUnitA, scopedA, "321");
    trackedEntityWithValue(orgUnitB, scopedA, "321");

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportAttributeWhenTheSameValueIgnoringCaseIsInTheSameOrgUnit() {
    TrackedEntity first = trackedEntityWithValue(orgUnitA, scopedA, "abc");
    TrackedEntity second = trackedEntityWithValue(orgUnitA, scopedA, "ABC");
    trackedEntityWithValue(orgUnitA, scopedB, "abc");

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        50,
        Set.of(scopedA.getUid()),
        Set.of(scopedA.getName()),
        Set.of("1 duplicated value(s)"),
        true);
    assertEquals(
        List.of(first.getUid(), second.getUid()).stream().sorted().toList(),
        getDetails(CHECK).getIssues().get(0).getRefs().toList(JsonString::string));
  }

  @Test
  void shouldCountEveryDuplicatedValueWhenAttributeHasDuplicatesInSeveralOrgUnits() {
    trackedEntityWithValue(orgUnitA, scopedA, "321");
    trackedEntityWithValue(orgUnitA, scopedA, "321");
    trackedEntityWithValue(orgUnitB, scopedA, "322");
    trackedEntityWithValue(orgUnitB, scopedA, "322");
    trackedEntityWithValue(orgUnitB, scopedA, "322");

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        50,
        Set.of(scopedA.getUid()),
        Set.of(scopedA.getName()),
        Set.of("2 duplicated value(s)"),
        true);
    JsonDataIntegrityIssue issue = getDetails(CHECK).getIssues().get(0);
    assertEquals(5, issue.getRefs().size());
  }

  @Test
  void shouldReportNoIssuesWhenOnlyAttributesUniqueInTheWholeSystemOrNotUniqueHaveDuplicates() {
    TrackedEntityAttribute systemWide = uniqueAttribute('C', false);
    TrackedEntityAttribute notUnique = createTrackedEntityAttribute('D');
    manager.save(notUnique, false);
    trackedEntityWithValue(orgUnitA, systemWide, "321");
    trackedEntityWithValue(orgUnitA, systemWide, "321");
    trackedEntityWithValue(orgUnitA, notUnique, "321");
    trackedEntityWithValue(orgUnitA, notUnique, "321");

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportNoIssuesWhenTheDuplicateBelongsToADeletedTrackedEntity() {
    trackedEntityWithValue(orgUnitA, scopedA, "321");
    TrackedEntity deleted = createTrackedEntity(orgUnitA, trackedEntityType);
    deleted.setDeleted(true);
    addValue(deleted, scopedA, "321");

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  private TrackedEntityAttribute uniqueAttribute(char uniqueChar, boolean orgUnitScope) {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute(uniqueChar);
    attribute.setUnique(true);
    attribute.setOrgunitScope(orgUnitScope);
    manager.save(attribute, false);
    return attribute;
  }

  private TrackedEntity trackedEntityWithValue(
      OrganisationUnit orgUnit, TrackedEntityAttribute attribute, String value) {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    addValue(trackedEntity, attribute, value);
    return trackedEntity;
  }

  private void addValue(
      TrackedEntity trackedEntity, TrackedEntityAttribute attribute, String value) {
    TrackedEntityAttributeValue attributeValue =
        new TrackedEntityAttributeValue(attribute, trackedEntity, value);
    trackedEntity.addAttributeValue(attributeValue);
    manager.save(trackedEntity, false);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(attributeValue);
    manager.flush();
  }
}
