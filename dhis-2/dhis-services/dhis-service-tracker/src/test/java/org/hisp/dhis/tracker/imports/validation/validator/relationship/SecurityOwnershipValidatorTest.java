/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4020;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityOwnershipValidatorTest extends TestBase {

  private SecurityOwnershipValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackerAccessManager trackerAccessManager;

  private org.hisp.dhis.relationship.Relationship convertedRelationship;

  private Reporter reporter;

  private Relationship relationship;

  @BeforeEach
  public void setUp() {
    validator = new SecurityOwnershipValidator(trackerAccessManager);
    MetadataIdentifier relationshipTypeUid = MetadataIdentifier.ofUid("relationshipType");

    when(bundle.getPreheat()).thenReturn(preheat);
    lenient()
        .when(preheat.getRelationshipType(relationshipTypeUid))
        .thenReturn(
            createPersonToPersonRelationshipType(
                'A', createProgram('A'), createTrackedEntityType('A'), false));

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
    relationship =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(relationshipTypeUid)
            .from(RelationshipItem.builder().trackedEntity(UID.generate()).build())
            .to(RelationshipItem.builder().trackedEntity(UID.generate()).build())
            .build();

    convertedRelationship = TrackerObjectsMapper.map(preheat, relationship, new SystemUser());
  }

  @Test
  void shouldCreateWhenUserHasWriteAccessToRelationship() {
    SystemUser user = new SystemUser();
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(bundle.getUser()).thenReturn(user);
    when(trackerAccessManager.canCreate(any(), eq(convertedRelationship))).thenReturn(List.of());

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailToCreateWhenUserHasNoWriteAccessToRelationship() {
    SystemUser user = new SystemUser();
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(bundle.getUser()).thenReturn(user);
    when(trackerAccessManager.canCreate(any(), eq(convertedRelationship)))
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
