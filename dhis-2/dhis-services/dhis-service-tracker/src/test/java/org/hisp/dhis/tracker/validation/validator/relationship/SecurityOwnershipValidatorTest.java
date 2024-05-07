/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4020;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.RelationshipTrackerConverterService;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityOwnershipValidatorTest {

  private SecurityOwnershipValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private AclService aclService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private RelationshipTrackerConverterService converterService;

  private org.hisp.dhis.relationship.Relationship convertedRelationship;

  private Reporter reporter;

  private Relationship relationship;

  @BeforeEach
  public void setUp() {
    validator = new SecurityOwnershipValidator(trackerAccessManager, converterService);

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
    relationship = new Relationship();
    relationship.setRelationship("relationshipUid");

    convertedRelationship = new org.hisp.dhis.relationship.Relationship();
  }

  @Test
  void shouldCreateWhenUserHasWriteAccessToRelationship() {
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(converterService.from(preheat, relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canWrite(any(), eq(convertedRelationship))).thenReturn(List.of());

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailToCreateWhenUserHasNoWriteAccessToRelationship() {
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(converterService.from(preheat, relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canWrite(any(), eq(convertedRelationship)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, relationship);

    assertHasError(reporter, relationship, E4020);
  }

  @Test
  void shouldDeleteWhenUserHasWriteAccessToRelationship() {
    when(bundle.getStrategy(relationship)).thenReturn(DELETE);
    when(preheat.getRelationship(relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canDelete(any(), eq(convertedRelationship))).thenReturn(List.of());

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailToDeleteWhenUserHasNoWriteAccessToRelationship() {
    when(bundle.getStrategy(relationship)).thenReturn(DELETE);
    when(preheat.getRelationship(relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canDelete(any(), eq(convertedRelationship)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, relationship);

    assertHasError(reporter, relationship, E4020);
  }
}
