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
package org.hisp.dhis.query;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
public class JpaCriteriaQueryEngine<T extends IdentifiableObject> implements QueryEngine<T> {
  private final QueryPlanner queryPlanner;

  private final List<IdentifiableObjectStore<T>> hibernateGenericStores;

  private final EntityManager entityManager;

  private final QueryCacheManager queryCacheManager;

  private Map<Class<?>, IdentifiableObjectStore<T>> stores = new HashMap<>();

  public JpaCriteriaQueryEngine(
      QueryPlanner queryPlanner,
      List<IdentifiableObjectStore<T>> hibernateGenericStores,
      QueryCacheManager queryCacheManager,
      EntityManager entityManager) {

    checkNotNull(queryPlanner);
    checkNotNull(hibernateGenericStores);
    checkNotNull(entityManager);

    this.queryPlanner = queryPlanner;
    this.hibernateGenericStores = hibernateGenericStores;
    this.queryCacheManager = queryCacheManager;
    this.entityManager = entityManager;
  }

  @Override
  public List<T> query(Query query) {
    Schema schema = query.getSchema();

    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = (InternalHibernateGenericStore<T>) getStore(klass);

    if (store == null) {
      return new ArrayList<>();
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(CurrentUserUtil.getCurrentUserDetails());
    }

    if (!query.isPlannedQuery()) {
      QueryPlan queryPlan = queryPlanner.planQuery(query, true);
      query = queryPlan.getPersistedQuery();
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<T> criteriaQuery = builder.createQuery(klass);
    Root<T> root = criteriaQuery.from(klass);

    if (query.isEmpty()) {

      UserDetails userDetails =
          query.getCurrentUserDetails() != null
              ? query.getCurrentUserDetails()
              : CurrentUserUtil.getCurrentUserDetails();

      Predicate predicate = builder.conjunction();
      boolean shareable = schema.isShareable();
      if (shareable && !query.isSkipSharing()) {
        predicate
            .getExpressions()
            .addAll(
                store.getSharingPredicates(builder, userDetails).stream()
                    .map(t -> t.apply(root))
                    .toList());
      }
      criteriaQuery.where(predicate);

      TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);

      typedQuery.setFirstResult(query.getFirstResult());
      typedQuery.setMaxResults(query.getMaxResults());

      return typedQuery.getResultList();
    }

    Predicate predicate = buildPredicates(builder, root, query);
    boolean shareable = schema.isShareable();

    String username =
        CurrentUserUtil.getCurrentUsername() != null
            ? CurrentUserUtil.getCurrentUsername()
            : "system-process";

    if (!username.equals("system-process") && shareable && !query.isSkipSharing()) {

      UserDetails userDetails =
          query.getCurrentUserDetails() != null
              ? query.getCurrentUserDetails()
              : CurrentUserUtil.getCurrentUserDetails();

      predicate
          .getExpressions()
          .addAll(
              store.getSharingPredicates(builder, userDetails).stream()
                  .map(t -> t.apply(root))
                  .toList());
    }

    criteriaQuery.where(predicate);

    if (!query.getOrders().isEmpty()) {
      criteriaQuery.orderBy(
          query.getOrders().stream()
              .map(
                  o ->
                      o.isAscending()
                          ? builder.asc(root.get(o.getProperty().getFieldName()))
                          : builder.desc(root.get(o.getProperty().getFieldName())))
              .toList());
    }

    TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);

    typedQuery.setFirstResult(query.getFirstResult());
    typedQuery.setMaxResults(query.getMaxResults());

    if (query.isCacheable()) {
      typedQuery.setHint(QueryHints.HINT_CACHEABLE, true);
      typedQuery.setHint(
          QueryHints.HINT_CACHE_REGION,
          queryCacheManager.getQueryCacheRegionName(klass, typedQuery));
    }

    return typedQuery.getResultList();
  }

  @Override
  public long count(Query query) {
    Schema schema = query.getSchema();

    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = (InternalHibernateGenericStore<T>) getStore(klass);

    if (store == null) {
      return 0;
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(CurrentUserUtil.getCurrentUserDetails());
    }

    if (!query.isPlannedQuery()) {
      QueryPlan queryPlan = queryPlanner.planQuery(query, true);
      query = queryPlan.getPersistedQuery();
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
    Root<T> root = criteriaQuery.from(klass);

    criteriaQuery.select(builder.count(root));

    Predicate predicate = buildPredicates(builder, root, query);

    boolean shareable = schema.isShareable();
    if (shareable && !query.isSkipSharing()) {
      UserDetails currentUserDetails = query.getCurrentUserDetails();
      predicate
          .getExpressions()
          .addAll(
              store.getSharingPredicates(builder, currentUserDetails).stream()
                  .map(t -> t.apply(root))
                  .toList());
    }

    criteriaQuery.where(predicate);

    if (!query.getOrders().isEmpty()) {
      criteriaQuery.orderBy(
          query.getOrders().stream()
              .map(
                  o ->
                      o.isAscending()
                          ? builder.asc(root.get(o.getProperty().getName()))
                          : builder.desc(root.get(o.getProperty().getName())))
              .toList());
    }

    TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);

    return typedQuery.getSingleResult();
  }

  private void initStoreMap() {
    if (!stores.isEmpty()) {
      return;
    }

    for (IdentifiableObjectStore<T> store : hibernateGenericStores) {
      stores.put(store.getClazz(), store);
    }
  }

  private IdentifiableObjectStore<?> getStore(Class<? extends IdentifiableObject> klass) {
    initStoreMap();
    return stores.get(klass);
  }

  private <Y> Predicate buildPredicates(CriteriaBuilder builder, Root<Y> root, Query query) {
    Predicate junction = builder.conjunction();
    query.getAliases().forEach(alias -> root.join(alias).alias(alias));
    if (!query.getCriterions().isEmpty()) {
      junction = getJpaJunction(builder, query.getRootJunctionType());
      for (org.hisp.dhis.query.Criterion criterion : query.getCriterions()) {
        addPredicate(builder, root, junction, criterion);
      }
    }

    return junction;
  }

  private Predicate getJpaJunction(CriteriaBuilder builder, Junction.Type type) {
    switch (type) {
      case AND:
        return builder.conjunction();
      case OR:
        return builder.disjunction();
    }

    return builder.conjunction();
  }

  private <Y> Predicate getPredicate(
      CriteriaBuilder builder, Root<Y> root, Restriction restriction) {
    if (restriction == null || restriction.getOperator() == null) {
      return null;
    }
    return restriction.getOperator().getPredicate(builder, root, restriction.getQueryPath());
  }

  private <Y> void addPredicate(
      CriteriaBuilder builder,
      Root<Y> root,
      Predicate predicateJunction,
      org.hisp.dhis.query.Criterion criterion) {
    if (criterion instanceof Restriction restriction) {
      Predicate predicate = getPredicate(builder, root, restriction);

      if (predicate != null) {
        predicateJunction.getExpressions().add(predicate);
      }
    } else if (criterion instanceof Junction) {
      Predicate junction = null;

      if (criterion instanceof Disjunction) {
        junction = builder.disjunction();
      } else if (criterion instanceof Conjunction) {
        junction = builder.conjunction();
      }

      predicateJunction.getExpressions().add(junction);

      for (org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions()) {
        addJunction(builder, root, junction, c);
      }
    }
  }

  private <Y> void addJunction(
      CriteriaBuilder builder,
      Root<Y> root,
      Predicate junction,
      org.hisp.dhis.query.Criterion criterion) {
    if (criterion instanceof Restriction restriction) {
      Predicate predicate = getPredicate(builder, root, restriction);

      if (predicate != null) {
        junction.getExpressions().add(predicate);
      }
    } else if (criterion instanceof Junction) {
      Predicate j = null;

      if (criterion instanceof Disjunction) {
        j = builder.disjunction();
      } else if (criterion instanceof Conjunction) {
        j = builder.conjunction();
      }

      junction.getExpressions().add(j);

      for (org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions()) {
        addJunction(builder, root, junction, c);
      }
    }
  }
}
