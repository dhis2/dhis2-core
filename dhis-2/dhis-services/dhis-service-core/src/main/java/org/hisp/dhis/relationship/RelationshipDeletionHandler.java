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

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component
@RequiredArgsConstructor
public class RelationshipDeletionHandler extends DeletionHandler {
  private static final DeletionVeto VETO = new DeletionVeto(Relationship.class);

  private final RelationshipService relationshipService;

  @Override
  protected void register() {
    whenDeleting(TrackedEntity.class, this::deleteTrackedEntity);
    whenDeleting(Event.class, this::deleteEvent);
    whenDeleting(Enrollment.class, this::deleteEnrollment);
    whenVetoing(RelationshipType.class, this::allowDeleteRelationshipType);
  }

  private void deleteTrackedEntity(TrackedEntity trackedEntity) {
    Collection<Relationship> relationships =
        relationshipService.getRelationshipsByTrackedEntity(trackedEntity, false);

    if (relationships != null) {
      for (Relationship relationship : relationships) {
        relationshipService.deleteRelationship(relationship);
      }
    }
  }

  private void deleteEvent(Event event) {
    Collection<Relationship> relationships =
        relationshipService.getRelationshipsByEvent(event, false);

    if (relationships != null) {
      for (Relationship relationship : relationships) {
        relationshipService.deleteRelationship(relationship);
      }
    }
  }

  private void deleteEnrollment(Enrollment enrollment) {
    Collection<Relationship> relationships =
        relationshipService.getRelationshipsByEnrollment(enrollment, false);

    if (relationships != null) {
      for (Relationship relationship : relationships) {
        relationshipService.deleteRelationship(relationship);
      }
    }
  }

  private DeletionVeto allowDeleteRelationshipType(RelationshipType relationshipType) {
    Collection<Relationship> relationships =
        relationshipService.getRelationshipsByRelationshipType(relationshipType);

    return relationships.isEmpty() ? ACCEPT : VETO;
  }
}
