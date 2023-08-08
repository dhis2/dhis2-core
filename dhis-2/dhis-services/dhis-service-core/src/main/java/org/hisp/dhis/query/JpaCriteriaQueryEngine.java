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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
public class JpaCriteriaQueryEngine<T extends IdentifiableObject> implements QueryEngine<T> {
  private final CurrentUserService currentUserService;

  private final QueryPlanner queryPlanner;

  private final List<InternalHibernateGenericStore<T>> hibernateGenericStores;

  private final SessionFactory sessionFactory;

  private final QueryCacheManager queryCacheManager;

  private Map<Class<?>, InternalHibernateGenericStore<T>> stores = new HashMap<>();

  @Autowired
  public JpaCriteriaQueryEngine(
      CurrentUserService currentUserService,
      QueryPlanner queryPlanner,
      List<InternalHibernateGenericStore<T>> hibernateGenericStores,
      SessionFactory sessionFactory,
      QueryCacheManager queryCacheManager) {
    checkNotNull(currentUserService);
    checkNotNull(queryPlanner);
    checkNotNull(hibernateGenericStores);
    checkNotNull(sessionFactory);

    this.currentUserService = currentUserService;
    this.queryPlanner = queryPlanner;
    this.hibernateGenericStores = hibernateGenericStores;
    this.sessionFactory = sessionFactory;
    this.queryCacheManager = queryCacheManager;
  }

  @Override
  public List<T> query(Query query) {
    Schema schema = query.getSchema();

    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = (InternalHibernateGenericStore<T>) getStore(klass);

    if (store == null) {
      return new ArrayList<>();
    }

    if (query.getUser() == null) {
      query.setUser(currentUserService.getCurrentUser());
    }

    if (!query.isPlannedQuery()) {
      QueryPlan queryPlan = queryPlanner.planQuery(query, true);
      query = queryPlan.getPersistedQuery();
    }

    CriteriaBuilder builder = sessionFactory.getCriteriaBuilder();

    CriteriaQuery<T> criteriaQuery = builder.createQuery(klass);
    Root<T> root = criteriaQuery.from(klass);

    if (query.isEmpty()) {
      Predicate predicate = builder.conjunction();
      if (!query.isSkipSharing()) {
        predicate
            .getExpressions()
            .addAll(
                store.getSharingPredicates(builder, query.getUser()).stream()
                    .map(t -> t.apply(root))
                    .collect(Collectors.toList()));
      }
      criteriaQuery.where(predicate);

      TypedQuery<T> typedQuery = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

      typedQuery.setFirstResult(query.getFirstResult());
      typedQuery.setMaxResults(query.getMaxResults());

      return typedQuery.getResultList();
    }

    Predicate predicate = buildPredicates(builder, root, query);
    if (!query.isSkipSharing()) {
      predicate
          .getExpressions()
          .addAll(
              store.getSharingPredicates(builder, query.getUser()).stream()
                  .map(t -> t.apply(root))
                  .collect(Collectors.toList()));
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
              .collect(Collectors.toList()));
    }

    TypedQuery<T> typedQuery = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

    typedQuery.setFirstResult(query.getFirstResult());
    typedQuery.setMaxResults(query.getMaxResults());

    if (query.isCacheable()) {
      typedQuery.setHint("org.hibernate.cacheable", true);
      typedQuery.setHint(
          "org.hibernate.cacheRegion",
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

    if (query.getUser() == null) {
      query.setUser(currentUserService.getCurrentUser());
    }

    if (!query.isPlannedQuery()) {
      QueryPlan queryPlan = queryPlanner.planQuery(query, true);
      query = queryPlan.getPersistedQuery();
    }

    CriteriaBuilder builder = sessionFactory.getCriteriaBuilder();

    CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
    Root<T> root = criteriaQuery.from(klass);

    criteriaQuery.select(builder.count(root));

    Predicate predicate = buildPredicates(builder, root, query);

    predicate
        .getExpressions()
        .addAll(
            store.getSharingPredicates(builder, query.getUser()).stream()
                .map(t -> t.apply(root))
                .collect(Collectors.toList()));

    criteriaQuery.where(predicate);

    if (!query.getOrders().isEmpty()) {
      criteriaQuery.orderBy(
          query.getOrders().stream()
              .map(
                  o ->
                      o.isAscending()
                          ? builder.asc(root.get(o.getProperty().getName()))
                          : builder.desc(root.get(o.getProperty().getName())))
              .collect(Collectors.toList()));
    }

    TypedQuery<Long> typedQuery = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

    return typedQuery.getSingleResult();
  }

  private void initStoreMap() {
    if (!stores.isEmpty()) {
      return;
    }

    for (InternalHibernateGenericStore<T> store : hibernateGenericStores) {
      stores.put(store.getClazz(), store);
    }
  }

  private InternalHibernateGenericStore<?> getStore(Class<? extends IdentifiableObject> klass) {
    initStoreMap();
    return stores.get(klass);
  }

  private <Y> Predicate buildPredicates(CriteriaBuilder builder, Root<Y> root, Query query) {
    Predicate junction = getJpaJunction(builder, query.getRootJunctionType());

    for (org.hisp.dhis.query.Criterion criterion : query.getCriterions()) {
      addPredicate(builder, root, junction, criterion);
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
