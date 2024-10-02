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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.relationship.RelationshipService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultRelationshipService implements RelationshipService {

  private final TrackerAccessManager trackerAccessManager;
  private final RelationshipStore relationshipStore;
  private final RelationshipOperationParamsMapper mapper;

  @Override
  public List<Relationship> getRelationships(@Nonnull RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipQueryParams queryParams = mapper.map(params);

    return getRelationships(queryParams);
  }

  @Override
  public Page<Relationship> getRelationships(
      @Nonnull RelationshipOperationParams params, @Nonnull PageParams pageParams)
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipQueryParams queryParams = mapper.map(params);

    return getRelationships(queryParams, pageParams);
  }

  @Override
  public Relationship getRelationship(@Nonnull String uid)
      throws ForbiddenException, NotFoundException {
    Relationship relationship = relationshipStore.getByUid(uid);

    if (relationship == null) {
      throw new NotFoundException(Relationship.class, uid);
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    List<String> errors = accessErrors(currentUser, relationship);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    return map(relationship);
  }

  @Override
  public List<Relationship> getRelationships(@Nonnull List<String> uids)
      throws ForbiddenException, NotFoundException {
    List<Relationship> relationships = new ArrayList<>();
    for (String uid : uids) {
      relationships.add(getRelationship(uid));
    }
    return relationships;
  }

  private List<Relationship> getRelationshipsByTrackedEntity(
      TrackedEntity trackedEntity, RelationshipQueryParams queryParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    List<Relationship> relationships =
        relationshipStore.getByTrackedEntity(trackedEntity, queryParams).stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return map(relationships);
  }

  private Page<Relationship> getRelationshipsByTrackedEntity(
      TrackedEntity trackedEntity, RelationshipQueryParams queryParams, PageParams pageParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Page<Relationship> relationshipPage =
        relationshipStore.getByTrackedEntity(trackedEntity, queryParams, pageParams);
    List<Relationship> relationships =
        relationshipPage.getItems().stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return relationshipPage.withItems(map(relationships));
  }

  private List<Relationship> getRelationshipsByEnrollment(
      Enrollment enrollment, RelationshipQueryParams queryParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    List<Relationship> relationships =
        relationshipStore.getByEnrollment(enrollment, queryParams).stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return map(relationships);
  }

  private Page<Relationship> getRelationshipsByEnrollment(
      Enrollment enrollment, RelationshipQueryParams queryParams, PageParams pageParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Page<Relationship> relationshipPage =
        relationshipStore.getByEnrollment(enrollment, queryParams, pageParams);
    List<Relationship> relationships =
        relationshipPage.getItems().stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return relationshipPage.withItems(map(relationships));
  }

  private List<Relationship> getRelationshipsByEvent(
      Event event, RelationshipQueryParams queryParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    List<Relationship> relationships =
        relationshipStore.getByEvent(event, queryParams).stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return map(relationships);
  }

  public Page<Relationship> getRelationshipsByEvent(
      Event event, RelationshipQueryParams queryParams, PageParams pageParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Page<Relationship> relationshipPage =
        relationshipStore.getByEvent(event, queryParams, pageParams);
    List<Relationship> relationships =
        relationshipPage.getItems().stream()
            .filter(r -> accessErrors(currentUser, r).isEmpty())
            .toList();
    return relationshipPage.withItems(map(relationships));
  }

  private List<Relationship> getRelationships(RelationshipQueryParams queryParams) {

    if (queryParams.getEntity() instanceof TrackedEntity te) {
      return getRelationshipsByTrackedEntity(te, queryParams);
    }

    if (queryParams.getEntity() instanceof Enrollment en) {
      return getRelationshipsByEnrollment(en, queryParams);
    }

    if (queryParams.getEntity() instanceof Event ev) {
      return getRelationshipsByEvent(ev, queryParams);
    }

    throw new IllegalArgumentException("Unkown type");
  }

  private Page<Relationship> getRelationships(
      RelationshipQueryParams queryParams, PageParams pageParams) {

    if (queryParams.getEntity() instanceof TrackedEntity te) {
      return getRelationshipsByTrackedEntity(te, queryParams, pageParams);
    }

    if (queryParams.getEntity() instanceof Enrollment en) {
      return getRelationshipsByEnrollment(en, queryParams, pageParams);
    }

    if (queryParams.getEntity() instanceof Event ev) {
      return getRelationshipsByEvent(ev, queryParams, pageParams);
    }

    throw new IllegalArgumentException("Unkown type");
  }

  private List<String> accessErrors(UserDetails user, Relationship relationship) {
    return trackerAccessManager.canRead(user, relationship);
  }

  /** Map to a non-proxied Relationship to prevent hibernate exceptions. */
  private List<Relationship> map(List<Relationship> relationships) {
    List<Relationship> result = new ArrayList<>(relationships.size());
    for (Relationship relationship : relationships) {
      result.add(map(relationship));
    }
    return result;
  }

  private Relationship map(Relationship relationship) {
    Relationship result = new Relationship();
    result.setUid(relationship.getUid());
    result.setCreated(relationship.getCreated());
    result.setCreatedBy(relationship.getCreatedBy());
    result.setLastUpdated(relationship.getLastUpdated());
    result.setLastUpdatedBy(relationship.getLastUpdatedBy());
    result.setDeleted(relationship.isDeleted());
    RelationshipType type = new RelationshipType();
    type.setUid(relationship.getRelationshipType().getUid());
    result.setRelationshipType(relationship.getRelationshipType());
    result.setFrom(withNestedEntity(relationship.getFrom()));
    result.setTo(withNestedEntity(relationship.getTo()));
    result.setCreatedAtClient(relationship.getCreatedAtClient());
    return result;
  }

  private RelationshipItem withNestedEntity(RelationshipItem item) {
    // relationships of relationship items are not mapped to JSON so there is no need to fetch them
    RelationshipItem result = new RelationshipItem();

    // the call to the individual services is to detach and apply some logic like filtering out
    // attribute values
    // for tracked entity type attributes from enrollment.trackedEntity. Enrollment attributes are
    // actually
    // owned by the TE and cannot be set on the Enrollment. When returning enrollments in our API
    // an enrollment
    // should only have the program tracked entity attributes.
    if (item.getTrackedEntity() != null) {
      result.setTrackedEntity(item.getTrackedEntity());
    } else if (item.getEnrollment() != null) {
      result.setEnrollment(item.getEnrollment());
    } else if (item.getEvent() != null) {
      result.setEvent(item.getEvent());
    }

    return result;
  }

  @Override
  public Set<String> getOrderableFields() {
    return relationshipStore.getOrderableFields();
  }
}
