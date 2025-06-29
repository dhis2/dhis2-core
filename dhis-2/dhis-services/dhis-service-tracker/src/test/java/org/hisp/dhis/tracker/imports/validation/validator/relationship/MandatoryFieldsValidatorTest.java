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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4001;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.validator.AssertValidations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MandatoryFieldsValidatorTest {

  private MandatoryFieldsValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new MandatoryFieldsValidator();

    when(bundle.getImportStrategy()).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(bundle.getValidationMode()).thenReturn(ValidationMode.FULL);
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void verifyRelationshipValidationSuccess() {
    String relTypeUid = CodeGenerator.generateUid();
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid(relTypeUid);

    Relationship relationship =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relTypeUid))
            .from(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .to(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    when(preheat.getAll(RelationshipType.class))
        .thenReturn(Collections.singletonList(relationshipType));
    when(bundle.getPreheat().getRelationshipType(relationship.getRelationshipType()))
        .thenReturn(relationshipType);

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenRelationshipMissingFrom() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .to(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `from` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void shouldFailWhenFromItemHasNoEntities() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .from(RelationshipItem.builder().build())
            .to(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `from` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void shouldFailWhenFromItemHasMultipleEntities() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .from(
                RelationshipItem.builder()
                    .trackedEntity(UID.generate())
                    .enrollment(UID.generate())
                    .build())
            .to(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `from` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void shouldFailWhenRelationshipMissingTo() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .from(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `to` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void shouldFailWhenToItemHasNoEntities() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .from(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .to(RelationshipItem.builder().build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `to` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void shouldFailWhenToItemHasMultipleEntities() {
    UID relationshipUid = UID.generate();
    Relationship relationship =
        Relationship.builder()
            .relationship(relationshipUid)
            .relationshipType(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .from(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .to(
                RelationshipItem.builder()
                    .trackedEntity(UID.generate())
                    .enrollment(UID.generate())
                    .build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4001,
        "RelationshipItem `to` for Relationship `" + relationshipUid + "`");
  }

  @Test
  void verifyRelationshipValidationFailsOnMissingRelationshipType() {
    Relationship relationship =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.EMPTY_UID)
            .from(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .to(RelationshipItem.builder().trackedEntity(trackedEntity()).build())
            .build();

    validator.validate(reporter, bundle, relationship);

    assertMissingProperty(reporter, relationship, "relationshipType");
  }

  private void assertMissingProperty(Reporter reporter, TrackerDto dto, String property) {
    AssertValidations.assertMissingProperty(reporter, dto, ValidationCode.E1124, property);
  }

  private UID trackedEntity() {
    return UID.generate();
  }
}
