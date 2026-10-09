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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeTaEncryptionValidationTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;
  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/validations/te-program_with_tea_encryption_metadata.json");
    injectAdminUser();
  }

  @Test
  void testUniqueFailInOrgUnit() throws IOException {
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    trackerObjects = fromJson("tracker/validations/te-program_with_tea_unique_data_in_region.json");
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailWhenOrgUnitScopedUniqueValueExistsForAnotherTrackedEntityInTheSameOrgUnit()
      throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    // another tracked entity with the same value, moved to the org unit of the first one
    trackerObjects = fromJson("tracker/validations/te-program_with_tea_unique_data_in_region.json");
    trackerObjects.getTrackedEntities().get(0).setOrgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1064);
  }

  @Test
  void shouldImportNewTrackedEntitiesWithTheSameOrgUnitScopedValueInDifferentOrgUnits()
      throws IOException {
    TrackerObjects trackerObjects = countryAndRegionTrackedEntitiesWithTheSameValue();

    ImportReport importReport =
        trackerImportService.importTracker(TrackerImportParams.builder().build(), trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void shouldRejectNewTrackedEntitiesWithTheSameOrgUnitScopedValueInTheSameOrgUnit()
      throws IOException {
    TrackerObjects trackerObjects = countryAndRegionTrackedEntitiesWithTheSameValue();
    trackerObjects.getTrackedEntities().get(1).setOrgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"));

    ImportReport importReport =
        trackerImportService.importTracker(TrackerImportParams.builder().build(), trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1064);
  }

  /** Two new tracked entities, in the country and in the region, with the same value 321. */
  private TrackerObjects countryAndRegionTrackedEntitiesWithTheSameValue() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    trackerObjects
        .getTrackedEntities()
        .addAll(
            fromJson("tracker/validations/te-program_with_tea_unique_data_in_region.json")
                .getTrackedEntities());
    return trackerObjects;
  }

  @Test
  void shouldOnlyRejectEnrollmentWhenItsUnknownTrackedEntitySendsTheSameOrgUnitScopedValue()
      throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    String trackedEntity = trackerObjects.getTrackedEntities().get(0).getUid();
    Attribute scopedValue = trackerObjects.getTrackedEntities().get(0).getAttributes().get(0);
    // the tracked entity of this enrollment exists neither in the payload nor in the DB, so the org
    // unit of its value is unknown
    String enrollment = CodeGenerator.generateUid();
    trackerObjects
        .getEnrollments()
        .add(
            Enrollment.builder()
                .enrollment(enrollment)
                .trackedEntity(CodeGenerator.generateUid())
                .program(MetadataIdentifier.ofUid("hJUBNVQWl4e"))
                .orgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"))
                .enrolledAt(Instant.now())
                .occurredAt(Instant.now())
                .attributes(List.of(scopedValue))
                .build());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    List<Error> errors = importReport.getValidationReport().getErrors();
    assertFalse(errors.isEmpty(), "the enrollment of an unknown tracked entity must be rejected");
    assertTrue(
        errors.stream().allMatch(e -> enrollment.equals(e.getUid())),
        () -> "only the enrollment must be rejected, got: " + errors);
    assertTrue(
        errors.stream().noneMatch(e -> ValidationCode.E1064.name().equals(e.getErrorCode())),
        () -> "the value of an unknown org unit must not collide, got: " + errors);
    assertFalse(errors.stream().anyMatch(e -> trackedEntity.equals(e.getUid())));
  }

  @Test
  void shouldFailWhenEnrollmentOfTrackedEntityInDbSendsOrgUnitScopedValueStoredInItsOrgUnit()
      throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data_in_country.json");
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    // another tracked entity in the country, with another value
    trackerObjects = fromJson("tracker/validations/te-program_with_tea_unique_data_in_region.json");
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        trackerObjects.getTrackedEntities().get(0);
    trackedEntity.setOrgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"));
    trackedEntity.getAttributes().get(0).setValue("322");
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    // only the enrollment is sent, in the region, so the country of its tracked entity is not in
    // the payload
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .trackedEntity(trackedEntity.getUid())
            .program(MetadataIdentifier.ofUid("hJUBNVQWl4e"))
            .orgUnit(MetadataIdentifier.ofUid("cNEZTkdAvfe"))
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .attributes(
                List.of(
                    Attribute.builder()
                        .attribute(MetadataIdentifier.ofUid("p5TPww5Uhrd"))
                        .value("9001")
                        .build(),
                    Attribute.builder()
                        .attribute(MetadataIdentifier.ofUid("p5TPxx5Uhrf"))
                        .value("321")
                        .build()))
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            params, TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    assertHasOnlyErrors(importReport, ValidationCode.E1064);
  }

  @Test
  void testUniqueFail() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-program_with_tea_unique_data.json");
    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);
    assertNoErrors(importReport);

    trackerObjects = fromJson("tracker/validations/te-program_with_tea_unique_data2.json");

    importReport = trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1064);
  }
}
