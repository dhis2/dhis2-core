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
package org.hisp.dhis.tracker.export.relationship;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;

public interface RelationshipStore extends IdentifiableObjectStore<Relationship> {
  String ID = RelationshipStore.class.getName();

  List<RelationshipItem> getRelationshipItemsByTrackedEntity(UID trackedEntity);

  List<RelationshipItem> getRelationshipItemsByEnrollment(UID enrollment);

  List<RelationshipItem> getRelationshipItemsByEvent(UID event);

  Optional<TrackedEntity> findTrackedEntity(UID trackedEntity);

  Optional<Enrollment> findEnrollment(UID enrollment);

  Optional<Event> findEvent(UID event);

  List<Relationship> getByTrackedEntity(
      TrackedEntity trackedEntity, RelationshipQueryParams queryParams);

  List<Relationship> getByEnrollment(Enrollment enrollment, RelationshipQueryParams queryParams);

  List<Relationship> getByEvent(Event event, RelationshipQueryParams queryParams);

  Page<Relationship> getByTrackedEntity(
      TrackedEntity trackedEntity, RelationshipQueryParams queryParams, PageParams pageParams);

  Page<Relationship> getByEnrollment(
      Enrollment enrollment, RelationshipQueryParams queryParams, PageParams pageParams);

  Page<Relationship> getByEvent(
      Event event, RelationshipQueryParams queryParams, PageParams pageParams);

  List<Relationship> getUidsByRelationshipKeys(List<String> relationshipKeyList);

  /**
   * Fields the store can order relationships by. Ordering by fields other than these is considered
   * a programmer error. Validation of user provided field names should occur before calling any
   * store method.
   */
  Set<String> getOrderableFields();
}
