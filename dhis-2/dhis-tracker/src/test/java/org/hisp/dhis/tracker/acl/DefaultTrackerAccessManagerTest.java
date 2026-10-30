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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.test.TestBase.createPersonToPersonRelationshipType;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createTrackedEntityType;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4019;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.RelationshipItem;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTrackerAccessManagerTest {

  @Mock private AclService aclService;
  @Mock private TrackerOwnershipManager ownershipAccessManager;
  @Mock private TrackerProgramService trackerProgramService;

  @InjectMocks private DefaultTrackerAccessManager trackerAccessManager;

  private RelationshipType relationshipType;
  private Relationship relationship;

  @BeforeEach
  void setUp() {
    relationshipType =
        createPersonToPersonRelationshipType(
            'A', createProgram('A'), createTrackedEntityType('A'), false);

    relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(new RelationshipItem());
    relationship.setTo(new RelationshipItem());
  }

  @Test
  void shouldReturnNoErrorsWhenSuperUserReadsRelationship() {
    assertIsEmpty(trackerAccessManager.canRead(new SystemUser(), relationship));
  }

  @Test
  void shouldReturnNoErrorsWhenUserHasDataReadAccessToRelationshipType() {
    UserDetails user = userDetails("userA");
    when(aclService.canDataRead(user, relationshipType)).thenReturn(true);

    assertIsEmpty(trackerAccessManager.canRead(user, relationship));
  }

  @Test
  void shouldReturnE4019WhenUserHasNoDataReadAccessToRelationshipType() {
    UserDetails user = userDetails("userA");
    when(aclService.canDataRead(user, relationshipType)).thenReturn(false);

    List<ErrorMessage> errors = trackerAccessManager.canRead(user, relationship);

    assertEquals(1, errors.size());
    assertEquals(E4019, errors.get(0).validationCode());
  }

  private static UserDetails userDetails(String uid) {
    User user = new User();
    user.setUid(uid);
    return UserDetails.fromUser(user);
  }
}
