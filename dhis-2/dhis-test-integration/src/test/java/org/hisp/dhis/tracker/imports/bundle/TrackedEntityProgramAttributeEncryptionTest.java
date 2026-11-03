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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentFields;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityProgramAttributeEncryptionTest extends PostgresIntegrationTestBase {
  private static final String CONFIDENTIAL_ATTRIBUTE = "CWMLspQP2RH";

  private static final String NON_CONFIDENTIAL_ATTRIBUTE = "TsfP85GKsU5";

  private static final String CONFIDENTIAL_VALUE = "Value To Encrypt";

  private static final String DATA = "tracker/te_program_with_tea_encryption_data.json";

  private static final UID ENROLLMENT = UID.of("NfX6INLTOVq");

  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired
  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private PBEStringEncryptor encryptor;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata(
        "tracker/te_program_with_tea_encryption_metadata.json", getAdminUser());

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldEncryptConfidentialAttributeValueOnCreate() throws IOException {
    testSetup.importTrackerData(DATA);

    assertEquals(CONFIDENTIAL_VALUE, value(CONFIDENTIAL_ATTRIBUTE));
    assertEquals(CONFIDENTIAL_VALUE, encryptor.decrypt(encryptedValue(CONFIDENTIAL_ATTRIBUTE)));
    assertNull(encryptedValue(NON_CONFIDENTIAL_ATTRIBUTE));
  }

  @Test
  void shouldEncryptConfidentialAttributeValueOnUpdate() throws IOException {
    TrackerObjects trackerObjects = testSetup.importTrackerData(DATA);
    confidentialAttribute(trackerObjects).setValue("Updated value");

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.UPDATE).build(),
            trackerObjects));

    assertEquals("Updated value", value(CONFIDENTIAL_ATTRIBUTE));
    assertEquals("Updated value", encryptor.decrypt(encryptedValue(CONFIDENTIAL_ATTRIBUTE)));
  }

  @Test
  void shouldReadConfidentialAttributeValueThroughHibernate() throws IOException {
    testSetup.importTrackerData(DATA);

    assertEquals(CONFIDENTIAL_VALUE, confidentialAttributeValue().getValue());
  }

  @Test
  void shouldFallBackToPlainValueWhenConfidentialValueWasNotEncrypted() throws IOException {
    testSetup.importTrackerData(DATA);
    clearEncryptedValue();

    assertEquals(CONFIDENTIAL_VALUE, confidentialAttributeValue().getValue());
  }

  @Test
  void shouldExportPlainConfidentialAttributeValueWithEnrollment()
      throws IOException, NotFoundException {
    testSetup.importTrackerData(DATA);

    assertEquals(CONFIDENTIAL_VALUE, exportedConfidentialAttributeValue().getValue());
  }

  @Test
  void shouldExportPlainConfidentialAttributeValueWithEnrollmentWhenNotEncrypted()
      throws IOException, NotFoundException {
    testSetup.importTrackerData(DATA);
    clearEncryptedValue();

    assertEquals(CONFIDENTIAL_VALUE, exportedConfidentialAttributeValue().getValue());
  }

  private Attribute confidentialAttribute(TrackerObjects trackerObjects) {
    return trackerObjects.getTrackedEntities().get(0).getAttributes().stream()
        .filter(a -> CONFIDENTIAL_ATTRIBUTE.equals(a.getAttribute().getIdentifier()))
        .findFirst()
        .orElseThrow();
  }

  private TrackedEntityAttributeValue exportedConfidentialAttributeValue()
      throws NotFoundException {
    return enrollmentService
        .getEnrollment(ENROLLMENT, EnrollmentFields.builder().includeAttributes().build())
        .getTrackedEntity()
        .getTrackedEntityAttributeValues()
        .stream()
        .filter(v -> CONFIDENTIAL_ATTRIBUTE.equals(v.getAttribute().getUid()))
        .findFirst()
        .orElseThrow();
  }

  private TrackedEntityAttributeValue confidentialAttributeValue() {
    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    manager.clear();

    return trackedEntityAttributeValueService
        .getTrackedEntityAttributeValues(trackedEntities.get(0))
        .stream()
        .filter(v -> CONFIDENTIAL_ATTRIBUTE.equals(v.getAttribute().getUid()))
        .findFirst()
        .orElseThrow();
  }

  /** Simulates a row written by the JDBC importer before it encrypted confidential values. */
  private void clearEncryptedValue() {
    jdbcTemplate.update(
        """
        update trackedentityattributevalue set encryptedvalue = null
        where trackedentityattributeid =
          (select trackedentityattributeid from trackedentityattribute where uid = ?)""",
        CONFIDENTIAL_ATTRIBUTE);
    assertNull(encryptedValue(CONFIDENTIAL_ATTRIBUTE));
  }

  private String value(String attribute) {
    return column("value", attribute);
  }

  private String encryptedValue(String attribute) {
    return column("encryptedvalue", attribute);
  }

  private String column(String column, String attribute) {
    List<String> values =
        jdbcTemplate.queryForList(
            "select teav."
                + column
                + " from trackedentityattributevalue teav"
                + " join trackedentityattribute tea"
                + " on tea.trackedentityattributeid = teav.trackedentityattributeid"
                + " where tea.uid = ?",
            String.class,
            attribute);
    assertEquals(1, values.size(), () -> "expected one value for attribute " + attribute);
    return values.get(0);
  }
}
