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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
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

  private Reporter reporter;

  private Relationship relationship;

  private RelationshipType relationshipType;

  @BeforeEach
  public void setUp() {
    validator = new SecurityOwnershipValidator(aclService);

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
    relationship = new Relationship();
    relationshipType = new RelationshipType();

    when(bundle.getPreheat().getRelationshipType(any())).thenReturn(relationshipType);
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
  }

  @Test
  void shouldFailWhenUserHasNoAccessToRelationshipType() {
    when(aclService.canDataWrite(bundle.getUser(), relationshipType)).thenReturn(false);

    validator.validate(reporter, bundle, relationship);

    assertNotEmpty(reporter.getErrors());
    assertContains(
        "has no data write access to relationship type", reporter.getErrors().get(0).getMessage());
  }

  @Test
  void shouldWorkWhenUserHasAccessToRelationshipType() {
    when(aclService.canDataWrite(bundle.getUser(), relationshipType)).thenReturn(true);

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }
}
