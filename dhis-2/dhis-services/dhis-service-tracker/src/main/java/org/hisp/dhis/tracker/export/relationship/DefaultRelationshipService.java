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
package org.hisp.dhis.tracker.export.relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.relationship.RelationshipService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultRelationshipService implements RelationshipService {
  private static final RelationshipItemMapper RELATIONSHIP_ITEM_MAPPER =
      Mappers.getMapper(RelationshipItemMapper.class);
  private final TrackerAccessManager trackerAccessManager;
  private final HibernateRelationshipStore relationshipStore;
  private final RelationshipOperationParamsMapper mapper;

  // TODO(DHIS2-19137) Pass fields params as a parameter
  @Nonnull
  @Override
  public Set<RelationshipItem> findRelationshipItems(
      TrackerType trackerType, UID uid, boolean includeDeleted) {
    List<RelationshipItem> relationshipItems =
        switch (trackerType) {
          case TRACKED_ENTITY ->
              relationshipStore.getRelationshipItemsByTrackedEntity(uid, includeDeleted);
          case ENROLLMENT ->
              relationshipStore.getRelationshipItemsByEnrollment(uid, includeDeleted);
          case EVENT -> relationshipStore.getRelationshipItemsByEvent(uid, includeDeleted);
          case RELATIONSHIP -> throw new IllegalArgumentException("Unsupported type");
        };
    return relationshipItems.stream()
        .filter(
            ri ->
                ri.getRelationship().getFrom().equals(ri)
                    || ri.getRelationship().getRelationshipType().isBidirectional())
        .filter(
            ri ->
                trackerAccessManager
                    .canRead(CurrentUserUtil.getCurrentUserDetails(), ri.getRelationship())
                    .isEmpty())
        .map(RELATIONSHIP_ITEM_MAPPER::map)
        .collect(Collectors.toSet());
  }

  @Nonnull
  @Override
  public List<Relationship> findRelationships(@Nonnull RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipQueryParams queryParams = mapper.map(params);

    return map(relationshipStore.getRelationships(queryParams));
  }

  @Nonnull
  @Override
  public Page<Relationship> findRelationships(
      @Nonnull RelationshipOperationParams params, @Nonnull PageParams pageParams)
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipQueryParams queryParams = mapper.map(params);
    Page<Relationship> relationships = relationshipStore.getRelationships(queryParams, pageParams);
    return relationships.withFilteredItems(map(relationships.getItems()));
  }

  @Nonnull
  @Override
  public Optional<Relationship> findRelationship(@Nonnull UID uid) {
    try {
      return Optional.of(getRelationship(uid));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  public Relationship getRelationship(@Nonnull UID uid) throws NotFoundException {
    Page<Relationship> relationships;
    try {
      relationships =
          findRelationships(
              RelationshipOperationParams.builder(Set.of(uid)).build(), PageParams.single());
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the RelationshipOperationParams are built");
    }

    if (relationships.getItems().isEmpty()) {
      throw new NotFoundException(Relationship.class, uid);
    }

    return relationships.getItems().get(0);
  }

  @Nonnull
  @Override
  public List<Relationship> findRelationships(@Nonnull Set<UID> uids)
      throws ForbiddenException, NotFoundException {
    if (uids.isEmpty()) {
      return List.of();
    }

    try {
      return findRelationships(RelationshipOperationParams.builder(uids).build());
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the RelationshipOperationParams are built");
    }
  }

  @Nonnull
  @Override
  public List<Relationship> getRelationshipsByRelationshipKeys(
      List<RelationshipKey> relationshipKeys) {
    return relationshipStore.getRelationshipsByRelationshipKeys(relationshipKeys);
  }

  /** Map to a non-proxied Relationship to prevent hibernate exceptions. */
  private List<Relationship> map(List<Relationship> relationships) {
    List<Relationship> result = new ArrayList<>(relationships.size());
    for (Relationship relationship : relationships) {
      if (trackerAccessManager
          .canRead(CurrentUserUtil.getCurrentUserDetails(), relationship)
          .isEmpty()) {
        result.add(map(relationship));
      }
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
