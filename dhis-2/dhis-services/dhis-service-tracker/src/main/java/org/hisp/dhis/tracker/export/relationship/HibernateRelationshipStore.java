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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.relationship.RelationshipStore")
class HibernateRelationshipStore extends SoftDeleteHibernateObjectStore<Relationship>
    implements RelationshipStore {

  private static final Order DEFAULT_ORDER = new Order("id", SortDirection.DESC);

  /**
   * Relationships can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.relationship.Relationship}.
   */
  private static final Set<String> ORDERABLE_FIELDS = Set.of("created", "createdAtClient");

  private static final String TRACKED_ENTITY = "trackedEntity";

  private static final String PROGRAM_INSTANCE = "enrollment";

  private static final String EVENT = "event";

  public HibernateRelationshipStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        entityManager,
        jdbcTemplate,
        publisher,
        Relationship.class,
        currentUserService,
        aclService,
        true);
  }

  @Override
  public List<Relationship> getByTrackedEntity(
      TrackedEntity trackedEntity, RelationshipQueryParams queryParams) {
    TypedQuery<Relationship> relationshipTypedQuery =
        getRelationshipTypedQuery(trackedEntity, queryParams);

    return getList(relationshipTypedQuery);
  }

  @Override
  public List<Relationship> getByEnrollment(
      Enrollment enrollment, RelationshipQueryParams queryParams) {
    TypedQuery<Relationship> relationshipTypedQuery =
        getRelationshipTypedQuery(enrollment, queryParams);

    return getList(relationshipTypedQuery);
  }

  @Override
  public List<Relationship> getByEvent(Event event, RelationshipQueryParams queryParams) {
    TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery(event, queryParams);

    return getList(relationshipTypedQuery);
  }

  private <T extends IdentifiableObject> TypedQuery<Relationship> getRelationshipTypedQuery(
      T entity, RelationshipQueryParams queryParams) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Relationship> relationshipItemCriteriaQuery =
        builder.createQuery(Relationship.class);
    Root<Relationship> root = relationshipItemCriteriaQuery.from(Relationship.class);

    setRelationshipItemCriteriaQueryExistsCondition(
        entity, builder, relationshipItemCriteriaQuery, root);

    return getRelationshipTypedQuery(queryParams, builder, relationshipItemCriteriaQuery, root);
  }

  private <T extends IdentifiableObject> void setRelationshipItemCriteriaQueryExistsCondition(
      T entity,
      CriteriaBuilder builder,
      CriteriaQuery<Relationship> relationshipItemCriteriaQuery,
      Root<Relationship> root) {
    Subquery<RelationshipItem> fromSubQuery =
        relationshipItemCriteriaQuery.subquery(RelationshipItem.class);
    Root<RelationshipItem> fromRoot = fromSubQuery.from(RelationshipItem.class);

    String relationshipEntityType = getRelationshipEntityType(entity);

    fromSubQuery.where(
        builder.equal(root.get("from"), fromRoot.get("id")),
        builder.equal(fromRoot.get(relationshipEntityType), entity.getId()));

    fromSubQuery.select(fromRoot.get("id"));

    Subquery<RelationshipItem> toSubQuery =
        relationshipItemCriteriaQuery.subquery(RelationshipItem.class);
    Root<RelationshipItem> toRoot = toSubQuery.from(RelationshipItem.class);

    toSubQuery.where(
        builder.equal(root.get("to"), toRoot.get("id")),
        builder.equal(toRoot.get(relationshipEntityType), entity.getId()));

    toSubQuery.select(toRoot.get("id"));

    relationshipItemCriteriaQuery.where(
        builder.or(builder.exists(fromSubQuery), builder.exists(toSubQuery)));

    relationshipItemCriteriaQuery.select(root);
  }

  private <T extends IdentifiableObject> String getRelationshipEntityType(T entity) {
    if (entity instanceof TrackedEntity) return TRACKED_ENTITY;
    else if (entity instanceof Enrollment) return PROGRAM_INSTANCE;
    else if (entity instanceof Event) return EVENT;
    else
      throw new IllegalArgumentException(
          entity.getClass().getSimpleName() + " not supported in relationship");
  }

  private TypedQuery<Relationship> getRelationshipTypedQuery(
      RelationshipQueryParams queryParams,
      CriteriaBuilder builder,
      CriteriaQuery<Relationship> relationshipItemCriteriaQuery,
      Root<Relationship> root) {
    JpaQueryParameters<Relationship> jpaQueryParameters = newJpaParameters(queryParams, builder);

    relationshipItemCriteriaQuery.orderBy(
        jpaQueryParameters.getOrders().stream()
            .map(o -> o.apply(root))
            .collect(Collectors.toList()));

    TypedQuery<Relationship> relationshipTypedQuery =
        getSession().createQuery(relationshipItemCriteriaQuery);

    if (jpaQueryParameters.hasFirstResult()) {
      relationshipTypedQuery.setFirstResult(jpaQueryParameters.getFirstResult());
    }

    if (jpaQueryParameters.hasMaxResult()) {
      relationshipTypedQuery.setMaxResults(jpaQueryParameters.getMaxResults());
    }

    return relationshipTypedQuery;
  }

  private JpaQueryParameters<Relationship> newJpaParameters(
      RelationshipQueryParams queryParams, CriteriaBuilder criteriaBuilder) {

    JpaQueryParameters<Relationship> jpaQueryParameters = newJpaParameters();

    if (Objects.nonNull(queryParams)) {
      if (!queryParams.getOrder().isEmpty()) {
        queryParams
            .getOrder()
            .forEach(order -> addOrder(jpaQueryParameters, order, criteriaBuilder));
      } else {
        addOrder(jpaQueryParameters, DEFAULT_ORDER, criteriaBuilder);
      }

      if (!queryParams.isSkipPaging()) {
        jpaQueryParameters.setFirstResult(queryParams.getOffset());
        jpaQueryParameters.setMaxResults(queryParams.getPageSizeWithDefault());
      }

      // When the clients choose to not show the total of pages.
      if (!queryParams.isTotalPages() && !queryParams.isSkipPaging()) {
        // Get pageSize + 1, so we are able to know if there is another
        // page available. It adds one additional element into the list,
        // as consequence. The caller needs to remove the last element.
        jpaQueryParameters.setMaxResults(queryParams.getPageSizeWithDefault() + 1);
      }
    }

    return jpaQueryParameters;
  }

  private void addOrder(
      JpaQueryParameters<Relationship> jpaQueryParameters, Order order, CriteriaBuilder builder) {
    jpaQueryParameters.addOrder(
        relationshipRoot ->
            order.getDirection().isAscending()
                ? builder.asc(relationshipRoot.get((String) order.getField()))
                : builder.desc(relationshipRoot.get((String) order.getField())));
  }

  @Override
  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<Relationship>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected Relationship postProcessObject(Relationship relationship) {
    return (relationship == null || relationship.isDeleted()) ? null : relationship;
  }
}
