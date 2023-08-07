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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.relationship.RelationshipService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultRelationshipService implements RelationshipService {

  private final CurrentUserService currentUserService;

  private final TrackerAccessManager trackerAccessManager;

  private final RelationshipStore relationshipStore;

  private final RelationshipOperationParamsMapper mapper;

  @Override
  public Relationships getRelationships(RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException {
    RelationshipQueryParams queryParams = mapper.map(params);

    Pager pager;
    List<Relationship> relationships = getRelationships(queryParams);

    if (Boolean.TRUE.equals(queryParams.isSkipPaging())) {
      return Relationships.withoutPagination(relationships);
    }

    if (queryParams.isTotalPages()) {
      int count = countRelationships(queryParams);
      pager =
          new Pager(queryParams.getPageWithDefault(), count, queryParams.getPageSizeWithDefault());
    } else {
      pager = handleLastPageFlag(params, relationships);
    }

    return Relationships.of(relationships, pager);
  }

  public int countRelationships(RelationshipQueryParams queryParams) {

    if (queryParams.getEntity() instanceof TrackedEntity te) {
      return getRelationshipsByTrackedEntity(te, null).size();
    }

    if (queryParams.getEntity() instanceof Enrollment en) {
      return getRelationshipsByEnrollment(en, null).size();
    }

    if (queryParams.getEntity() instanceof Event ev) {
      return getRelationshipsByEvent(ev, null).size();
    }

    throw new IllegalArgumentException("Unkown type");
  }

  @Override
  public Relationship getRelationship(String uid) throws ForbiddenException, NotFoundException {
    Relationship relationship = relationshipStore.getByUid(uid);

    if (relationship == null) {
      throw new NotFoundException(Relationship.class, uid);
    }

    User user = currentUserService.getCurrentUser();
    List<String> errors = trackerAccessManager.canRead(user, relationship);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    return map(relationship);
  }

  @Override
  public Optional<Relationship> findRelationshipByUid(String uid) {
    Relationship relationship = relationshipStore.getByUid(uid);

    if (relationship == null) {
      return Optional.empty();
    }

    User user = currentUserService.getCurrentUser();
    List<String> errors = trackerAccessManager.canRead(user, relationship);

    if (!errors.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(map(relationship));
  }

  @Override
  public List<Relationship> getRelationshipsByTrackedEntity(
      TrackedEntity trackedEntity,
      PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter) {

    List<Relationship> relationships =
        relationshipStore
            .getByTrackedEntity(trackedEntity, pagingAndSortingCriteriaAdapter)
            .stream()
            .filter(
                r -> trackerAccessManager.canRead(currentUserService.getCurrentUser(), r).isEmpty())
            .toList();
    return map(relationships);
  }

  @Override
  public List<Relationship> getRelationshipsByEnrollment(
      Enrollment enrollment, PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter) {
    List<Relationship> relationships =
        relationshipStore.getByEnrollment(enrollment, pagingAndSortingCriteriaAdapter).stream()
            .filter(
                r -> trackerAccessManager.canRead(currentUserService.getCurrentUser(), r).isEmpty())
            .toList();
    return map(relationships);
  }

  @Override
  public List<Relationship> getRelationshipsByEvent(
      Event event, PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter) {
    List<Relationship> relationships =
        relationshipStore.getByEvent(event, pagingAndSortingCriteriaAdapter).stream()
            .filter(
                r -> trackerAccessManager.canRead(currentUserService.getCurrentUser(), r).isEmpty())
            .toList();
    return map(relationships);
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
    RelationshipType type = new RelationshipType();
    type.setUid(relationship.getRelationshipType().getUid());
    result.setRelationshipType(relationship.getRelationshipType());
    result.setFrom(withNestedEntity(relationship.getFrom()));
    result.setTo(withNestedEntity(relationship.getTo()));
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

  /**
   * This method will apply the logic related to the parameter 'totalPages=false'. This works in
   * conjunction with methods in : {@link RelationshipService}
   *
   * <p>This is needed because we need to query (pageSize + 1) at DB level. The resulting query will
   * allow us to evaluate if we are in the last page or not. And this is what his method does,
   * returning the respective Pager object.
   *
   * @param params the request params
   * @param relationships the reference to the list of Relationships
   * @return the populated SlimPager instance
   */
  private Pager handleLastPageFlag(
      RelationshipOperationParams params, List<Relationship> relationships) {
    Integer originalPage = defaultIfNull(params.getPage(), FIRST_PAGE);
    Integer originalPageSize = defaultIfNull(params.getPageSize(), DEFAULT_PAGE_SIZE);
    boolean isLastPage = false;

    if (isNotEmpty(relationships)) {
      isLastPage = relationships.size() <= originalPageSize;
      if (!isLastPage) {
        // Get the same number of elements of the pageSize, forcing
        // the removal of the last additional element added at querying
        // time.
        relationships.retainAll(relationships.subList(0, originalPageSize));
      }
    }

    return new SlimPager(originalPage, originalPageSize, isLastPage);
  }
}
