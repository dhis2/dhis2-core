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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackedEntityFieldsTest {
  @Test
  void shouldIncludeAllFields() {
    TrackedEntityFields fields = TrackedEntityFields.all();

    assertTrue(fields.isIncludesAttributes());
    assertTrue(fields.isIncludesProgramOwners());

    assertTrue(fields.isIncludesRelationships());
    assertRelationshipFields(fields.getRelationshipFields(), Assertions::assertTrue);

    assertTrue(fields.isIncludesEnrollments());
    assertTrue(fields.getEnrollmentFields().isIncludesAttributes());
    assertTrue(fields.getEnrollmentFields().isIncludesRelationships());
    assertRelationshipFields(
        fields.getEnrollmentFields().getRelationshipFields(), Assertions::assertTrue);

    assertTrue(fields.getEnrollmentFields().isIncludesEvents());
    assertTrue(fields.getEnrollmentFields().getEventFields().isIncludesRelationships());
    assertRelationshipFields(
        fields.getEnrollmentFields().getEventFields().getRelationshipFields(),
        Assertions::assertTrue);
  }

  @Test
  void shouldIncludeNoFields() {
    TrackedEntityFields fields = TrackedEntityFields.none();

    assertFalse(fields.isIncludesAttributes());
    assertFalse(fields.isIncludesProgramOwners());

    assertFalse(fields.isIncludesRelationships());
    assertRelationshipFields(fields.getRelationshipFields(), Assertions::assertFalse);

    assertFalse(fields.isIncludesEnrollments());
    assertFalse(fields.getEnrollmentFields().isIncludesAttributes());
    assertFalse(fields.getEnrollmentFields().isIncludesRelationships());
    assertRelationshipFields(
        fields.getEnrollmentFields().getRelationshipFields(), Assertions::assertFalse);

    assertFalse(fields.getEnrollmentFields().isIncludesEvents());
    assertFalse(fields.getEnrollmentFields().getEventFields().isIncludesRelationships());
    assertRelationshipFields(
        fields.getEnrollmentFields().getEventFields().getRelationshipFields(),
        Assertions::assertFalse);
  }

  private static void assertRelationshipFields(
      RelationshipFields relationshipFields, java.util.function.Consumer<Boolean> assertionMethod) {
    assertionMethod.accept(relationshipFields.isIncludesFrom());
    assertionMethod.accept(relationshipFields.getFromFields().isIncludesTrackedEntity());
    assertionMethod.accept(
        relationshipFields.getFromFields().getTrackedEntityFields().isIncludesAttributes());
    assertionMethod.accept(
        relationshipFields.getFromFields().getTrackedEntityFields().isIncludesProgramOwners());
    assertionMethod.accept(
        relationshipFields.getFromFields().getTrackedEntityFields().isIncludesEnrollments());
    assertionMethod.accept(
        relationshipFields
            .getFromFields()
            .getTrackedEntityFields()
            .getEnrollmentFields()
            .isIncludesAttributes());
    assertionMethod.accept(
        relationshipFields
            .getFromFields()
            .getTrackedEntityFields()
            .getEnrollmentFields()
            .isIncludesEvents());
    assertionMethod.accept(relationshipFields.getFromFields().isIncludesEnrollment());
    assertionMethod.accept(
        relationshipFields.getFromFields().getEnrollmentFields().isIncludesAttributes());
    assertionMethod.accept(
        relationshipFields.getFromFields().getEnrollmentFields().isIncludesEvents());
    assertionMethod.accept(relationshipFields.getFromFields().isIncludesEvent());
    assertionMethod.accept(relationshipFields.isIncludesTo());
    assertionMethod.accept(relationshipFields.getToFields().isIncludesTrackedEntity());
    assertionMethod.accept(
        relationshipFields
            .getToFields()
            .getTrackedEntityFields()
            .getEnrollmentFields()
            .isIncludesAttributes());
    assertionMethod.accept(
        relationshipFields
            .getToFields()
            .getTrackedEntityFields()
            .getEnrollmentFields()
            .isIncludesEvents());
    assertionMethod.accept(relationshipFields.getToFields().isIncludesEnrollment());
    assertionMethod.accept(
        relationshipFields.getToFields().getEnrollmentFields().isIncludesAttributes());
    assertionMethod.accept(
        relationshipFields.getToFields().getEnrollmentFields().isIncludesEvents());
    assertionMethod.accept(relationshipFields.getToFields().isIncludesEvent());
  }
}
