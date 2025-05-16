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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.export.relationship.RelationshipStore")
class HibernateRelationshipStore extends SoftDeleteHibernateObjectStore<Relationship> {

  private static final org.hisp.dhis.tracker.export.Order DEFAULT_ORDER =
      new org.hisp.dhis.tracker.export.Order("id", SortDirection.DESC);

  /**
   * Relationships can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.relationship.Relationship}.
   */
  private static final Set<String> ORDERABLE_FIELDS = Set.of("created", "createdAtClient");

  private static final String TRACKED_ENTITY = "trackedEntity";

  private static final String ENROLLMENT = "enrollment";

  private static final String EVENT = "event";

  public HibernateRelationshipStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Relationship.class, aclService, true);
  }

  public Optional<TrackedEntity> findTrackedEntity(UID trackedEntity, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        from TrackedEntity te \
        where te.uid = :trackedEntity \
        """;
    if (!includeDeleted) {
      hql += "and te.deleted = false";
    }
    List<TrackedEntity> trackedEntities =
        getQuery(hql, TrackedEntity.class)
            .setParameter("trackedEntity", trackedEntity.getValue())
            .getResultList();
    return trackedEntities.stream().findFirst();
  }

  public Optional<Enrollment> findEnrollment(UID enrollment, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        from Enrollment e \
        where e.uid = :enrollment \
        """;
    if (!includeDeleted) {
      hql += "and e.deleted = false";
    }
    List<Enrollment> enrollments =
        getQuery(hql, Enrollment.class)
            .setParameter("enrollment", enrollment.getValue())
            .getResultList();
    return enrollments.stream().findFirst();
  }

  public Optional<Event> findEvent(UID event, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        from Event e \
        where e.uid = :event \
        """;
    if (!includeDeleted) {
      hql += "and e.deleted = false";
    }
    List<Event> events =
        getQuery(hql, Event.class).setParameter("event", event.getValue()).getResultList();
    return events.stream().findFirst();
  }

  public List<Relationship> getRelationships(@Nonnull RelationshipQueryParams queryParams) {
    return relationshipsList(queryParams, null);
  }

  public Page<Relationship> getRelationships(
      @Nonnull final RelationshipQueryParams queryParams, @Nonnull PageParams pageParams) {
    List<Relationship> relationships = relationshipsList(queryParams, pageParams);
    return new Page<>(relationships, pageParams, () -> countRelationships(queryParams));
  }

  public List<Relationship> getRelationshipsByRelationshipKeys(
      List<RelationshipKey> relationshipKeys) {
    if (CollectionUtils.isEmpty(relationshipKeys)) {
      return Collections.emptyList();
    }

    @Language("hql")
    String hql =
        """
        from Relationship r \
        where r.deleted = false and (r.key in (:keys) \
        or (r.invertedKey in (:keys) and r.relationshipType.bidirectional = true))""";
    List<String> relationshipKeysAsString =
        relationshipKeys.stream().map(RelationshipKey::asString).toList();
    return getQuery(hql, Relationship.class).setParameter("keys", relationshipKeysAsString).list();
  }

  public List<RelationshipItem> getRelationshipItemsByTrackedEntity(
      UID trackedEntity, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        select ri \
        from RelationshipItem ri \
        join ri.relationship r \
        join r.relationshipType rt \
        where (r.from = ri or rt.bidirectional = true) \
        and ri.trackedEntity.uid = :trackedEntity \
        """;
    if (!includeDeleted) {
      hql += "and r.deleted = false";
    }
    return getQuery(hql, RelationshipItem.class)
        .setParameter("trackedEntity", trackedEntity.getValue())
        .list();
  }

  public List<RelationshipItem> getRelationshipItemsByEnrollment(
      UID enrollment, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        select ri \
        from RelationshipItem ri \
        join ri.relationship r \
        join r.relationshipType rt \
        where (r.from = ri or rt.bidirectional = true) \
        and ri.enrollment.uid = :enrollment \
        """;
    if (!includeDeleted) {
      hql += "and r.deleted = false";
    }
    return getQuery(hql, RelationshipItem.class)
        .setParameter("enrollment", enrollment.getValue())
        .list();
  }

  public List<RelationshipItem> getRelationshipItemsByEvent(UID event, boolean includeDeleted) {
    @Language("hql")
    String hql =
        """
        select ri \
        from RelationshipItem ri \
        join ri.relationship r \
        join r.relationshipType rt \
        where (r.from = ri or rt.bidirectional = true) \
        and ri.event.uid = :event \
        """;
    if (!includeDeleted) {
      hql += "and r.deleted = false";
    }
    return getQuery(hql, RelationshipItem.class).setParameter("event", event.getValue()).list();
  }

  private List<Relationship> relationshipsList(
      RelationshipQueryParams queryParams, PageParams pageParams) {
    CriteriaQuery<Relationship> criteriaQuery = criteriaQuery(queryParams);

    TypedQuery<Relationship> query = entityManager.createQuery(criteriaQuery);

    if (pageParams != null) {
      query.setFirstResult(pageParams.getOffset());
      query.setMaxResults(
          pageParams.getPageSize()
              + 1); // get extra relationship to determine if there is a nextPage
    }

    return query.getResultList();
  }

  private long countRelationships(RelationshipQueryParams queryParams) {

    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);

    Root<Relationship> root = criteriaQuery.from(Relationship.class);

    criteriaQuery.select(builder.count(root));

    if (queryParams.getEntity() != null) {
      criteriaQuery.where(
          whereConditionPredicates(
              queryParams.getEntity(),
              builder,
              criteriaQuery,
              root,
              queryParams.isIncludeDeleted()));
    } else {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(root.get("uid").in((UID.toValueList(queryParams.getRelationships()))));

      if (!queryParams.isIncludeDeleted()) {
        predicates.add(builder.equal(root.get("deleted"), false));
      }

      criteriaQuery.where(predicates.toArray(Predicate[]::new));
    }
    return entityManager.createQuery(criteriaQuery).getSingleResult().longValue();
  }

  private CriteriaQuery<Relationship> criteriaQuery(RelationshipQueryParams queryParams) {
    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaQuery<Relationship> criteriaQuery = builder.createQuery(Relationship.class);

    Root<Relationship> root = criteriaQuery.from(Relationship.class);

    criteriaQuery.select(root);

    if (queryParams.getEntity() != null) {
      criteriaQuery.where(
          whereConditionPredicates(
              queryParams.getEntity(),
              builder,
              criteriaQuery,
              root,
              queryParams.isIncludeDeleted()));
    } else {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(root.get("uid").in((UID.toValueList(queryParams.getRelationships()))));

      if (!queryParams.isIncludeDeleted()) {
        predicates.add(builder.equal(root.get("deleted"), false));
      }

      criteriaQuery.where(predicates.toArray(Predicate[]::new));
    }

    criteriaQuery.orderBy(orderBy(queryParams, builder, root));

    return criteriaQuery;
  }

  private <T extends SoftDeletableObject> Predicate[] whereConditionPredicates(
      T entity,
      CriteriaBuilder builder,
      CriteriaQuery<?> relationshipItemCriteriaQuery,
      Root<Relationship> root,
      boolean includeDeleted) {
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

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.or(builder.exists(fromSubQuery), builder.exists(toSubQuery)));

    if (!includeDeleted) {
      predicates.add(builder.equal(root.get("deleted"), false));
    }

    return predicates.toArray(Predicate[]::new);
  }

  private <T extends IdentifiableObject> String getRelationshipEntityType(T entity) {
    if (entity instanceof TrackedEntity) return TRACKED_ENTITY;
    else if (entity instanceof Enrollment) return ENROLLMENT;
    else if (entity instanceof Event) return EVENT;
    else
      throw new IllegalArgumentException(
          entity.getClass().getSimpleName() + " not supported in relationship");
  }

  private List<Order> orderBy(
      RelationshipQueryParams queryParams, CriteriaBuilder builder, Root<Relationship> root) {
    List<Order> defaultOrder = orderBy(List.of(DEFAULT_ORDER), builder, root);
    if (!queryParams.getOrder().isEmpty()) {
      return Stream.concat(
              orderBy(queryParams.getOrder(), builder, root).stream(), defaultOrder.stream())
          .toList();
    } else {
      return defaultOrder;
    }
  }

  List<Order> orderBy(
      List<org.hisp.dhis.tracker.export.Order> orderList,
      CriteriaBuilder builder,
      Root<Relationship> root) {

    return orderList.stream()
        .map(
            order ->
                order.getDirection().isAscending()
                    ? builder.asc(root.get((String) order.getField()))
                    : builder.desc(root.get((String) order.getField())))
        .toList();
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }
}
