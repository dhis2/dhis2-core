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
package org.hisp.dhis.relationship;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.system.deletion.DefaultDeletionManager;
import org.hisp.dhis.system.deletion.DeletionManager;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationshipDeletionHandlerTest {

  @Mock private RelationshipService relationshipService;

  private final DeletionManager deletionManager = new DefaultDeletionManager();

  @BeforeEach
  public void setUp() {
    RelationshipDeletionHandler handler = new RelationshipDeletionHandler(relationshipService);
    handler.setManager(deletionManager);
    handler.init();
  }

  @Test
  void allowDeleteRelationshipTypeWithData() {
    when(relationshipService.getRelationshipsByRelationshipType(any()))
        .thenReturn(singletonList(new Relationship()));

    ObjectDeletionRequestedEvent event = new ObjectDeletionRequestedEvent(new RelationshipType());
    Exception ex =
        assertThrows(DeleteNotAllowedException.class, () -> deletionManager.onDeletion(event));
    assertEquals(
        "Object could not be deleted because it is associated with another object: Relationship",
        ex.getMessage());
  }

  @Test
  void allowDeleteRelationshipTypeWithoutData() {
    when(relationshipService.getRelationshipsByRelationshipType(any())).thenReturn(emptyList());

    ObjectDeletionRequestedEvent event = new ObjectDeletionRequestedEvent(new RelationshipType());
    deletionManager.onDeletion(event);

    verify(relationshipService, atLeastOnce()).getRelationshipsByRelationshipType(any());
  }

  @Test
  void deleteTrackedEntityInstance() {
    when(relationshipService.getRelationshipsByTrackedEntityInstance(any(), anyBoolean()))
        .thenReturn(singletonList(new Relationship()));

    ObjectDeletionRequestedEvent event =
        new ObjectDeletionRequestedEvent(new TrackedEntityInstance());
    deletionManager.onDeletion(event);

    verify(relationshipService, atLeastOnce())
        .getRelationshipsByTrackedEntityInstance(any(), anyBoolean());
    verify(relationshipService, atLeastOnce()).deleteRelationship(any());
  }
}
