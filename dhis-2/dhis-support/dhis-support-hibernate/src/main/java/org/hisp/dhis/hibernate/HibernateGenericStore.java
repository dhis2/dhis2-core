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
package org.hisp.dhis.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.QueryHints;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hisp.dhis.HibernateNativeStore;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.common.UID;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class HibernateGenericStore<T> extends HibernateNativeStore<T> implements GenericStore<T> {

  protected static final int OBJECT_FETCH_SIZE = 2000;

  protected final JdbcTemplate jdbcTemplate;
  protected final ApplicationEventPublisher publisher;

  protected boolean cacheable;

  public HibernateGenericStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<T> clazz,
      boolean cacheable) {
    super(entityManager, clazz);
    checkNotNull(jdbcTemplate);
    checkNotNull(publisher);

    this.jdbcTemplate = jdbcTemplate;
    this.publisher = publisher;
    this.cacheable = cacheable;
  }

  /** Could be overridden programmatically. */
  protected boolean isCacheable() {
    return cacheable;
  }

  /** Could be injected through container. */
  public void setCacheable(boolean cacheable) {
    this.cacheable = cacheable;
  }

  // -------------------------------------------------------------------------
  // Convenience methods
  // -------------------------------------------------------------------------

  /**
   * Creates a Query for given HQL query string. Return type is casted to generic type T of the
   * Store class.
   *
   * @param hql the HQL query.
   * @return a Query instance with return type is the object type T of the store class
   */
  @SuppressWarnings("unchecked")
  protected final Query<T> getQuery(@Language("hql") String hql) {
    return getSession()
        .createQuery(hql)
        .setCacheable(cacheable)
        .setHint(QueryHints.CACHEABLE, cacheable);
  }

  protected final <C> Query<C> getQuery(@Language("hql") String hql, Class<C> customClass) {
    return getSession()
        .createQuery(hql, customClass)
        .setCacheable(cacheable)
        .setHint(QueryHints.CACHEABLE, cacheable);
  }

  /**
   * Creates a Query for given HQL query string. Must specify the return type of the Query variable.
   *
   * @param hql the HQL query.
   * @return a Query instance with return type specified in the Query<Y>
   */
  @SuppressWarnings("unchecked")
  protected final <V> Query<V> getTypedQuery(@Language("hql") String hql) {
    return getSession()
        .createQuery(hql)
        .setCacheable(cacheable)
        .setHint(QueryHints.CACHEABLE, cacheable);
  }

  /** Override to add additional restrictions to criteria before it is invoked. */
  protected void preProcessDetachedCriteria(DetachedCriteria detachedCriteria) {}

  public CriteriaBuilder getCriteriaBuilder() {
    return entityManager.getCriteriaBuilder();
  }

  // ------------------------------------------------------------------------------------------
  // JPA Methods
  // ------------------------------------------------------------------------------------------

  /**
   * Get executable Typed Query from Criteria Query. Apply cache if needed.
   *
   * @return executable TypedQuery
   */
  private TypedQuery<T> getExecutableTypedQuery(CriteriaQuery<T> criteriaQuery) {
    return entityManager.createQuery(criteriaQuery).setHint(QueryHints.CACHEABLE, cacheable);
  }

  /** Method for adding additional Predicates into where clause */
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<T>, Predicate>> predicates) {}

  /**
   * Get single result from executable typedQuery
   *
   * @param typedQuery TypedQuery
   * @return single object
   */
  protected <V> V getSingleResult(TypedQuery<V> typedQuery) {
    List<V> list = typedQuery.getResultList();

    if (list != null && list.size() > 1) {
      throw new NonUniqueResultException("More than one entity found for query");
    }

    return list != null && !list.isEmpty() ? list.get(0) : null;
  }

  protected <V> V getSingleResult(Query<V> typedQuery) {
    List<V> list = typedQuery.getResultList();

    if (list != null && list.size() > 1) {
      throw new NonUniqueResultException("More than one entity found for query");
    }

    return list != null && !list.isEmpty() ? list.get(0) : null;
  }

  protected <V> V getSingleResult(NativeQuery<V> nativeQuery) {
    List<V> list = nativeQuery.getResultList();

    if (list != null && list.size() > 1) {
      throw new NonUniqueResultException("More than one entity found for query");
    }

    return list != null && !list.isEmpty() ? list.get(0) : null;
  }

  /**
   * Get List objects returned by executable TypedQuery
   *
   * @return list result
   */
  protected final List<T> getList(TypedQuery<T> typedQuery) {
    return typedQuery.getResultList();
  }

  protected final List<T> getList(Query<T> typedQuery) {
    return typedQuery.getResultList();
  }

  /**
   * Get List objects return by querying given JpaQueryParameters with Pagination
   *
   * @param parameters JpaQueryParameters
   * @return list objects
   */
  protected final List<T> getList(CriteriaBuilder builder, JpaQueryParameters<T> parameters) {
    return getTypedQuery(builder, parameters).getResultList();
  }

  protected final <V> List<T> getListFromPartitions(
      CriteriaBuilder builder,
      Collection<V> values,
      int partitionSize,
      Function<Collection<V>, JpaQueryParameters<T>> createPartitionParams) {
    if (values == null || values.isEmpty()) {
      return new ArrayList<>(0);
    }
    if (values.size() <= partitionSize) {
      // fast path: avoid aggregation collection
      return getList(builder, createPartitionParams.apply(values));
    }

    List<List<V>> partitionedValues = Lists.partition(new ArrayList<>(values), partitionSize);
    List<T> aggregate = new ArrayList<>();
    for (List<V> valuesPartition : partitionedValues) {
      aggregate.addAll(getList(builder, createPartitionParams.apply(valuesPartition)));
    }
    return aggregate;
  }

  /**
   * Get executable TypedQuery from JpaQueryParameter.
   *
   * @return executable TypedQuery
   */
  protected final TypedQuery<T> getTypedQuery(
      CriteriaBuilder builder, JpaQueryParameters<T> parameters) {
    List<Function<Root<T>, Predicate>> predicateProviders = parameters.getPredicates();
    List<Function<Root<T>, Order>> orderProviders = parameters.getOrders();
    preProcessPredicates(builder, predicateProviders);

    CriteriaQuery<T> query = builder.createQuery(getClazz());
    Root<T> root = query.from(getClazz());
    query.select(root);

    if (!predicateProviders.isEmpty()) {
      List<Predicate> predicates =
          predicateProviders.stream().map(t -> t.apply(root)).collect(Collectors.toList());
      query.where(predicates.toArray(new Predicate[0]));
    }

    if (!orderProviders.isEmpty()) {
      List<Order> orders =
          orderProviders.stream().map(o -> o.apply(root)).collect(Collectors.toList());
      query.orderBy(orders);
    }

    TypedQuery<T> typedQuery = getExecutableTypedQuery(query);

    if (parameters.hasFirstResult()) {
      typedQuery.setFirstResult(parameters.getFirstResult());
    }

    if (parameters.hasMaxResult()) {
      typedQuery.setMaxResults(parameters.getMaxResults());
    }

    return typedQuery.setHint(QueryHints.CACHEABLE, parameters.isCacheable(cacheable));
  }

  /**
   * Count number of objects based on given parameters
   *
   * @param parameters JpaQueryParameters
   * @return number of objects
   */
  protected final Long getCount(CriteriaBuilder builder, JpaQueryParameters<T> parameters) {
    CriteriaQuery<Long> query = builder.createQuery(Long.class);

    Root<T> root = query.from(getClazz());

    List<Function<Root<T>, Predicate>> predicateProviders = parameters.getPredicates();

    List<Function<Root<T>, Expression<Long>>> countExpressions = parameters.getCountExpressions();

    if (!countExpressions.isEmpty()) {
      if (countExpressions.size() > 1) {
        query.multiselect(
            countExpressions.stream().map(c -> c.apply(root)).collect(Collectors.toList()));
      } else {
        query.select(countExpressions.get(0).apply(root));
      }
    } else {
      query.select(parameters.isUseDistinct() ? builder.countDistinct(root) : builder.count(root));
    }

    if (!predicateProviders.isEmpty()) {
      List<Predicate> predicates =
          predicateProviders.stream().map(t -> t.apply(root)).collect(Collectors.toList());
      query.where(predicates.toArray(new Predicate[0]));
    }

    return getSession()
        .createQuery(query)
        .setHint(QueryHints.CACHEABLE, parameters.isCacheable(cacheable))
        .getSingleResult();
  }

  /**
   * Retrieves an object based on the given Jpa Predicates.
   *
   * @return an object of the implementation Class type.
   */
  protected T getSingleResult(CriteriaBuilder builder, JpaQueryParameters<T> parameters) {
    return getSingleResult(getTypedQuery(builder, parameters));
  }

  // ------------------------------------------------------------------------------------------
  // End JPA Methods
  // ------------------------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // GenericIdentifiableObjectStore implementation
  // -------------------------------------------------------------------------

  @Override
  public void save(@Nonnull T object) {
    AuditLogUtil.infoWrapper(log, object, AuditLogUtil.ACTION_CREATE);
    getSession().save(object);
  }

  @Override
  public void update(@Nonnull T object) {
    getSession().update(object);
  }

  @Override
  public void delete(@Nonnull T object) {
    publisher.publishEvent(new ObjectDeletionRequestedEvent(object));

    getSession().delete(object);
  }

  @CheckForNull
  @Override
  public T get(long id) {
    T object = getNoPostProcess(id);
    return postProcessObject(object);
  }

  @CheckForNull
  protected final T getNoPostProcess(long id) {
    return getSession().get(getClazz(), id);
  }

  /**
   * Override for further processing of a retrieved object.
   *
   * @param object the object.
   * @return the processed object.
   */
  protected T postProcessObject(T object) {
    return object;
  }

  @Nonnull
  @Override
  public List<T> getAll() {
    return getList(getCriteriaBuilder(), new JpaQueryParameters<>());
  }

  @Nonnull
  @Override
  public List<T> getAllByAttributes(@Nonnull Collection<UID> attributes) {
    String quotedIds = join(",", attributes.stream().map(id -> "'" + id.getValue() + "'").toList());
    // language=sql
    String sql =
        "select * from %s where jsonb_exists_any(attributevalues, array[%s])"
            .formatted(tableName, quotedIds);
    return nativeSynchronizedTypedQuery(sql).list();
  }

  @Override
  public long countAllValuesByAttributes(@Nonnull Collection<UID> attributes) {
    String quotedIds = join(",", attributes.stream().map(id -> "'" + id.getValue() + "'").toList());
    // language=sql
    String sql =
        "select count(*) from %s where jsonb_exists_any(attributevalues, array[%s])"
            .formatted(tableName, quotedIds);
    return ((Number) nativeSynchronizedQuery(sql).getSingleResult()).longValue();
  }

  @Override
  public int getCount() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
            builder, newJpaParameters().count(root -> builder.countDistinct(root.get("id"))))
        .intValue();
  }

  @Nonnull
  @Override
  public List<T> getByAttribute(@Nonnull UID attribute) {
    // language=sql
    String sql =
        "select * from %s where jsonb_exists(attributevalues, '%s')"
            .formatted(tableName, attribute.getValue());
    return nativeSynchronizedTypedQuery(sql).list();
  }

  @Nonnull
  @Override
  public List<T> getByAttributeAndValue(@Nonnull UID attribute, String value) {
    // language=sql
    String sql =
        "select * from %s where jsonb_extract_path_text(attributevalues, '%s', 'value') = :value"
            .formatted(tableName, attribute.getValue());
    return nativeSynchronizedTypedQuery(sql).setParameter("value", value).list();
  }

  @Override
  public boolean isAttributeValueUniqueTo(
      @Nonnull UID object, @Nonnull UID attribute, @Nonnull String value) {
    // language=sql
    String sql =
        "select count(*) from %s where uid != :object and jsonb_extract_path_text(attributeValues, '%s', 'value') = :value"
            .formatted(tableName, attribute.getValue());
    Number count =
        (Number)
            nativeSynchronizedQuery(sql)
                .setParameter("value", value)
                .setParameter("object", object.getValue())
                .getSingleResult();
    return count.intValue() == 0;
  }

  @Nonnull
  @Override
  public List<T> getAllByAttributeAndValues(@Nonnull UID attribute, @Nonnull List<String> values) {
    // language=sql
    String sql =
        "select * from %s where jsonb_extract_path_text(attributeValues, '%s', 'value') in :values"
            .formatted(tableName, attribute.getValue());

    return nativeSynchronizedTypedQuery(sql).setParameterList("values", values).list();
  }

  @Override
  public int updateAllAttributeValues(
      @Nonnull UID attribute, @Nonnull String newValue, boolean createMissing) {
    // language=sql
    String sql =
        """
        update %s set attributevalues = jsonb_strip_nulls(
            jsonb_set(cast(attributevalues as jsonb), '{%s}', cast(:value as jsonb), :createMissing))"""
            .formatted(tableName, attribute.getValue());
    return nativeSynchronizedQuery(sql)
        .setParameter("value", newValue)
        .setParameter("createMissing", createMissing)
        .executeUpdate();
  }

  /**
   * Create new instance of JpaQueryParameters
   *
   * @return JpaQueryParameters<T>
   */
  protected JpaQueryParameters<T> newJpaParameters() {
    return new JpaQueryParameters<>();
  }
}
