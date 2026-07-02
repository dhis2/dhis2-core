/*
 * Copyright (c) 2004-2025, University of Oslo
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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests the {@code tracked_entity_attribute_null_value} data integrity check, which flags tracked
 * entity attribute values that have no plain text value stored in the {@code value} column. This
 * includes values that only have an {@code encryptedvalue} (previously confidential attributes) and
 * values that are completely empty. Anomalous rows are inserted with plain SQL because the service
 * layer refuses to persist a null value.
 */
class DataIntegrityTrackedEntityAttributeNullValueControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK = "tracked_entity_attribute_null_value";

  private static final String DETAILS_ID_TYPE = "tracker";

  @Autowired private JdbcTemplate jdbcTemplate;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute attribute;

  @BeforeEach
  void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    manager.save(organisationUnit);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    trackedEntity = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntity);

    attribute = createTrackedEntityAttribute('A');
    manager.save(attribute);

    // flush so the parent rows exist in the DB before the raw JDBC inserts below
    manager.flush();
  }

  @Test
  void shouldFindNoIssuesWhenThereIsNoData() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, false);
  }

  @Test
  void shouldFindNoIssuesWhenAttributeValueHasPlainTextValue() {
    insertAttributeValue("some value", null);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldFindIssueWhenAttributeValueHasNeitherPlainTextNorEncryptedValue() {
    insertAttributeValue(null, null);

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        100,
        trackedEntity.getUid(),
        trackedEntity.getUid(),
        "has no value",
        true);
  }

  @Test
  void shouldFindIssueWhenAttributeValueHasEncryptedValueButNoPlainTextValue() {
    insertAttributeValue(null, "encrypted-cipher-text");

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        100,
        trackedEntity.getUid(),
        trackedEntity.getUid(),
        "has an encrypted value but no plain text value",
        true);
  }

  private void insertAttributeValue(String value, String encryptedValue) {
    jdbcTemplate.update(
        "INSERT INTO trackedentityattributevalue "
            + "(trackedentityid, trackedentityattributeid, created, lastupdated, value, encryptedvalue) "
            + "VALUES (?, ?, now(), now(), ?, ?)",
        trackedEntity.getId(),
        attribute.getId(),
        value,
        encryptedValue);
  }
}
