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

import static org.hisp.dhis.query.JpaQueryUtils.stringPredicateIgnoreCase;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class JpaCriteriaQueryEngine implements QueryEngine {

  private final SchemaService schemaService;
  private final List<IdentifiableObjectStore<?>> hibernateGenericStores;
  private final QueryCacheManager queryCacheManager;
  private final EntityManager entityManager;
  private final Map<Class<?>, IdentifiableObjectStore<?>> stores = new HashMap<>();

  @Override
  public <T extends IdentifiableObject> List<T> query(Query query) {
    Schema schema = query.getSchema();

    @SuppressWarnings("unchecked")
    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = getStore(klass);

    if (store == null) {
      return new ArrayList<>();
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(getCurrentUserDetails());
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<T> criteriaQuery = builder.createQuery(klass);
    Root<T> root = criteriaQuery.from(klass);

    Predicate predicate = queryFilters(builder, root, query);
    addSharingFilters(query, schema, predicate, store, builder, root);
    criteriaQuery.where(predicate);

    if (!query.getOrders().isEmpty()) criteriaQuery.orderBy(getOrders(query, builder, root));

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

  private static <T extends IdentifiableObject> void addSharingFilters(
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
  public <T extends IdentifiableObject> long count(Query query) {
    Schema schema = query.getSchema();

    @SuppressWarnings("unchecked")
    Class<T> klass = (Class<T>) schema.getKlass();

    InternalHibernateGenericStore<T> store = getStore(klass);

    if (store == null) {
      return 0;
    }

    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(getCurrentUserDetails());
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
    Root<T> root = criteriaQuery.from(klass);

    criteriaQuery.select(builder.count(root));

    Predicate predicate = queryFilters(builder, root, query);
    addSharingFilters(query, schema, predicate, store, builder, root);
    criteriaQuery.where(predicate);

    TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);

    return typedQuery.getSingleResult();
  }

  @Nonnull
  private static <T extends IdentifiableObject> List<jakarta.persistence.criteria.Order> getOrders(
      Query query, CriteriaBuilder builder, Root<T> root) {
    return query.getOrders().stream().map(o -> getOrderPredicate(builder, root, o)).toList();
  }

  private static <T extends IdentifiableObject>
      jakarta.persistence.criteria.Order getOrderPredicate(
          CriteriaBuilder builder, Root<T> root, @Nonnull Order order) {

    if (order.isIgnoreCase()) {
      return order.isAscending()
          ? builder.asc(builder.lower(root.get(order.getProperty().getFieldName())))
          : builder.desc(builder.lower(root.get(order.getProperty().getFieldName())));
    }

    return order.isAscending()
        ? builder.asc(root.get(order.getProperty().getFieldName()))
        : builder.desc(root.get(order.getProperty().getFieldName()));
  }

  private void initStoreMap() {
    if (!stores.isEmpty()) {
      return;
    }

    for (IdentifiableObjectStore<?> store : hibernateGenericStores) {
      stores.put(store.getClazz(), store);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends IdentifiableObject> InternalHibernateGenericStore<E> getStore(Class<E> klass) {
    initStoreMap();
    return (InternalHibernateGenericStore<E>) stores.get(klass);
  }

  private <Y> Predicate queryFilters(CriteriaBuilder builder, Root<Y> root, Query query) {
    if (query.getCriterions().isEmpty()) return builder.conjunction();

    Predicate rootJunction =
        switch (query.getRootJunctionType()) {
          case AND -> builder.conjunction();
          case OR -> builder.disjunction();
        };

    for (Restriction restriction : query.getCriterions()) {
      Predicate filter = queryFilter(builder, root, restriction, query);
      if (filter != null) rootJunction.getExpressions().add(filter);
    }

    Set<String> aliases = new HashSet<>();
    query.getCriterions().stream().flatMap(Restriction::aliases).forEach(aliases::add);
    aliases.forEach(alias -> root.get(alias).alias(alias));
    return rootJunction;
  }

  private <Y> Predicate queryFilter(
      CriteriaBuilder builder, Root<Y> root, Restriction restriction, Query query) {
    if (restriction == null || restriction.getOperator() == null) return null;
    if (!restriction.isVirtual())
      return restriction.getOperator().getPredicate(builder, root, restriction.getQueryPath());
    // handle special cases:
    if (restriction.isIdentifiable())
      return queryIdentifiableFilter(builder, root, restriction, query);
    if (restriction.isQuery()) return queryQueryFilter(builder, root, restriction);
    throw new UnsupportedOperationException(
        "Special filter is not implemented yet :/ " + restriction);
  }

  private <Y> Predicate queryIdentifiableFilter(
      CriteriaBuilder builder, Root<Y> root, Restriction restriction, Query query) {
    Predicate or = builder.disjunction();
    Operator<?> op = restriction.getOperator();
    Function<String, Predicate> getPredicate =
        path -> op.getPredicate(builder, root, schemaService.getQueryPath(query.getSchema(), path));
    or.getExpressions().add(getPredicate.apply("id"));
    or.getExpressions().add(getPredicate.apply("code"));
    or.getExpressions().add(getPredicate.apply("name"));
    if (query.getSchema().hasPersistedProperty("shortName"))
      or.getExpressions().add(getPredicate.apply("shortName"));
    return or;
  }

  private <Y> Predicate queryQueryFilter(
      CriteriaBuilder builder, Root<Y> root, Restriction restriction) {
    String value = (String) restriction.getOperator().getArgs().get(0);
    Predicate or = builder.disjunction();
    or.getExpressions().add(builder.equal(root.get("id"), value));
    or.getExpressions().add(builder.equal(root.get("code"), value));
    or.getExpressions().add(
        stringPredicateIgnoreCase(builder, root.get("name"), value, JpaQueryUtils.StringSearchMode.ANYWHERE));
    return or;
  }
}
