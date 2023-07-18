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
package org.hisp.dhis.tracker.validation.validator.relationship;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4015;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4016;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4017;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasWarning;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class ExistenceValidatorTest {
  private static final String NOT_PRESENT_RELATIONSHIP_UID = "NotPresentRelationshipId";

  private static final String RELATIONSHIP_UID = "RelationshipId";

  private static final String SOFT_DELETED_RELATIONSHIP_UID = "SoftDeletedRelationshipId";

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private ExistenceValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new ExistenceValidator();
  }

  @Test
  void verifyRelationshipValidationSuccessWhenIsCreate() {
    Relationship rel = Relationship.builder().relationship(NOT_PRESENT_RELATIONSHIP_UID).build();
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(rel)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, rel);

    assertIsEmpty(reporter.getErrors());
    assertThat(reporter.getWarnings(), empty());
  }

  @Test
  void verifyRelationshipValidationSuccessWithWarningWhenUpdate() {
    Relationship rel = getPayloadRelationship();
    when(bundle.getStrategy(rel)).thenReturn(TrackerImportStrategy.UPDATE);
    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getRelationship(RELATIONSHIP_UID)).thenReturn(getRelationship());

    validator.validate(reporter, bundle, rel);

    assertIsEmpty(reporter.getErrors());
    assertHasWarning(reporter, rel, E4015);
  }

  @Test
  void verifyRelationshipValidationFailsWhenIsCreateAndRelationshipIsAlreadyPresent() {
    Relationship rel = getPayloadRelationship();
    when(bundle.getStrategy(rel)).thenReturn(TrackerImportStrategy.CREATE);
    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getRelationship(RELATIONSHIP_UID)).thenReturn(getRelationship());

    validator.validate(reporter, bundle, rel);

    assertHasError(reporter, rel, E4015);
  }

  @Test
  void verifyRelationshipValidationFailsWhenIsDeleteAndRelationshipIsNotPresent() {
    Relationship rel = Relationship.builder().relationship(NOT_PRESENT_RELATIONSHIP_UID).build();
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(rel)).thenReturn(TrackerImportStrategy.DELETE);

    validator.validate(reporter, bundle, rel);

    assertHasError(reporter, rel, E4016);
  }

  @Test
  void verifyRelationshipValidationFailsWhenIsSoftDeleted() {
    Relationship rel = Relationship.builder().relationship(SOFT_DELETED_RELATIONSHIP_UID).build();
    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getRelationship(SOFT_DELETED_RELATIONSHIP_UID))
        .thenReturn(softDeletedRelationship());

    validator.validate(reporter, bundle, rel);

    assertHasError(reporter, rel, E4017);
  }

  private Relationship getPayloadRelationship() {
    return Relationship.builder().relationship(RELATIONSHIP_UID).build();
  }

  private org.hisp.dhis.relationship.Relationship softDeletedRelationship() {
    org.hisp.dhis.relationship.Relationship relationship =
        new org.hisp.dhis.relationship.Relationship();
    relationship.setUid(SOFT_DELETED_RELATIONSHIP_UID);
    relationship.setDeleted(true);
    return relationship;
  }

  private org.hisp.dhis.relationship.Relationship getRelationship() {
    org.hisp.dhis.relationship.Relationship relationship =
        new org.hisp.dhis.relationship.Relationship();
    relationship.setUid(RELATIONSHIP_UID);
    return relationship;
  }
}
