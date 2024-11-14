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
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

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
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
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

  private final Map<Class<?>, IdentifiableObjectStore<T>> stores = new HashMap<>();

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

    InternalHibernateGenericStore<T> store = getStore(klass);

    if (store == null) {
      return new ArrayList<>();
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(getCurrentUserDetails());
    }

    if (!query.isPlannedQuery()) {
      QueryPlan queryPlan = queryPlanner.planQuery(query, true);
      query = queryPlan.getPersistedQuery();
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<T> criteriaQuery = builder.createQuery(klass);
    Root<T> root = criteriaQuery.from(klass);

    if (query.isEmpty()) {
      Predicate predicate = builder.conjunction();

      addSharingPredicates(query, schema, predicate, store, builder, root);
      criteriaQuery.where(predicate);

      // TODO why isn't ordering added here just because there is no filter????
      TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);

      typedQuery.setFirstResult(query.getFirstResult());
      typedQuery.setMaxResults(query.getMaxResults());

      return typedQuery.getResultList();
    }

    Predicate predicate = buildPredicates(builder, root, query);

    addSharingPredicates(query, schema, predicate, store, builder, root);

    criteriaQuery.where(predicate);

    if (!query.getOrders().isEmpty()) {
      criteriaQuery.orderBy(getOrders(query, builder, root));
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

  private static <T extends IdentifiableObject> void addSharingPredicates(
      Query query,
      Schema schema,
      Predicate predicate,
      InternalHibernateGenericStore<T> store,
      CriteriaBuilder builder,
      Root<T> root) {
    boolean shareable = schema.isShareable();
    if (!shareable) return;
    UserDetails user = query.getCurrentUserDetails();
    if (user == null) user = getCurrentUserDetails();
    List<Function<Root<T>, Predicate>> predicates = List.of();
    if (query.isDataSharing()) {
      predicates = store.getDataSharingPredicates(builder, user);
    } else if (!query.isSkipSharing()) {
      predicates = store.getSharingPredicates(builder, user);
    }
    if (!predicates.isEmpty())
      predicate.getExpressions().addAll(predicates.stream().map(t -> t.apply(root)).toList());
  }

  @Override
  public long count(Query query) {
    Schema schema = query.getSchema();

    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = getStore(klass);

    if (store == null) {
      return 0;
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(getCurrentUserDetails());
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

    addSharingPredicates(query, schema, predicate, store, builder, root);

    criteriaQuery.where(predicate);

    TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);

    return typedQuery.getSingleResult();
  }

  @Nonnull
  private static <T extends IdentifiableObject> List<jakarta.persistence.criteria.Order> getOrders(
      Query query, CriteriaBuilder builder, Root<T> root) {
    return query.getOrders().stream()
        .map(
            o ->
                o.isAscending()
                    ? builder.asc(root.get(o.getProperty().getFieldName()))
                    : builder.desc(root.get(o.getProperty().getFieldName())))
        .toList();
  }

  private void initStoreMap() {
    if (!stores.isEmpty()) {
      return;
    }

    for (IdentifiableObjectStore<T> store : hibernateGenericStores) {
      stores.put(store.getClazz(), store);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends IdentifiableObject> InternalHibernateGenericStore<E> getStore(Class<E> klass) {
    initStoreMap();
    return (InternalHibernateGenericStore<E>) stores.get(klass);
  }

  private <Y> Predicate buildPredicates(CriteriaBuilder builder, Root<Y> root, Query query) {
    Predicate junction = builder.conjunction();
    if (!query.getCriterions().isEmpty()) {
      junction = getJpaJunction(builder, query.getRootJunctionType());
      for (org.hisp.dhis.query.Criterion criterion : query.getCriterions()) {
        addPredicate(builder, root, junction, criterion);
      }
    }
    query.getAliases().forEach(alias -> root.get(alias).alias(alias));
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
    if (criterion instanceof Restriction) {
      Restriction restriction = (Restriction) criterion;
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
    if (criterion instanceof Restriction) {
      Restriction restriction = (Restriction) criterion;
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
