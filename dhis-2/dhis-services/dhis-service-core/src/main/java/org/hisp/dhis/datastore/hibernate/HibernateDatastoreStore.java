/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.datastore.hibernate;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.query.JpaQueryUtils.generateHqlQueryForSharingCheck;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Stian Sandvold
 */
@Repository
public class HibernateDatastoreStore extends HibernateIdentifiableObjectStore<DatastoreEntry>
    implements DatastoreStore {
  public HibernateDatastoreStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        entityManager,
        jdbcTemplate,
        publisher,
        DatastoreEntry.class,
        currentUserService,
        aclService,
        true);
  }

  @Override
  public List<String> getNamespaces() {
    Query<String> query = getTypedQuery("select distinct namespace from DatastoreEntry");
    return query.list();
  }

  @Override
  public List<String> getKeysInNamespace(String namespace) {
    String hql = "select key from DatastoreEntry where namespace = :namespace";
    Query<String> query = getTypedQuery(hql);
    return query.setParameter("namespace", namespace).list();
  }

  @Override
  public List<String> getKeysInNamespace(String namespace, Date lastUpdated) {
    User currentUser = currentUserService.getCurrentUser();
    String accessFilter =
        generateHqlQueryForSharingCheck("ds", currentUser, AclService.LIKE_READ_METADATA);

    String hql =
        "select key from DatastoreEntry ds where namespace = :namespace and " + accessFilter;

    if (lastUpdated != null) {
      hql += " and lastupdated >= :lastUpdated ";
    }

    Query<String> query = getTypedQuery(hql);
    query.setParameter("namespace", namespace);

    if (lastUpdated != null) {
      query.setParameter("lastUpdated", lastUpdated);
    }

    return query.list();
  }

  @Override
  public List<DatastoreEntry> getEntriesInNamespace(String namespace) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("namespace"), namespace)));
  }

  @Override
  public <T> T getEntries(DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform) {
    User currentUser = currentUserService.getCurrentUser();
    String accessFilter =
        generateHqlQueryForSharingCheck("ds", currentUser, AclService.LIKE_READ_METADATA);
    DatastoreQueryBuilder builder =
        new DatastoreQueryBuilder(
            "from DatastoreEntry ds where namespace = :namespace and " + accessFilter, query);

    String hql = builder.createFetchHQL();

    Query<?> hQuery =
        getSession()
            .createQuery(hql, Object[].class)
            .setParameter("namespace", query.getNamespace())
            .setCacheable(false);

    builder.applyParameterValues(hQuery::setParameter);

    if (query.isPaging()) {
      int size = Math.min(1000, Math.max(1, query.getPageSize()));
      int offset = Math.max(0, (query.getPage() - 1) * size);
      hQuery.setMaxResults(size);
      hQuery.setFirstResult(offset);
    }

    if (query.getFields().isEmpty()) {
      return transform.apply(
          hQuery.stream().map(row -> new DatastoreFields((String) row, emptyList())));
    }

    @SuppressWarnings("unchecked")
    Query<Object[]> multiFieldQuery = (Query<Object[]>) hQuery;
    return transform.apply(
        multiFieldQuery.stream()
            .map(
                row ->
                    new DatastoreFields(
                        (String) row[0], asList(copyOfRange(row, 1, row.length, String[].class)))));
  }

  @Override
  public DatastoreEntry getEntry(String namespace, String key) {

    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("namespace"), namespace))
            .addPredicate(root -> builder.equal(root.get("key"), key)));
  }

  @Override
  public void deleteNamespace(String ns) {
    // language=SQL
    String sql = "delete from keyjsonvalue ds where ds.namespace = :ns";
    getSession().createNativeQuery(sql).setParameter("ns", ns).executeUpdate();
  }

  @Override
  public int countKeysInNamespace(String ns) {
    // language=SQL
    String sql = "select count(*) from keyjsonvalue v where v.namespace = :ns";
    Object count = getSession().createNativeQuery(sql).setParameter("ns", ns).uniqueResult();
    if (count == null) return 0;
    if (count instanceof Number) return ((Number) count).intValue();
    throw new IllegalStateException("Count did not return a number but: " + count);
  }

  @Override
  public boolean updateEntry(
      @Nonnull String ns,
      @Nonnull String key,
      @CheckForNull String value,
      @CheckForNull String path,
      @CheckForNull Integer roll) {
    boolean rootIsTarget = path == null || path.isEmpty();
    if (value == null && rootIsTarget) return updateEntryRootDelete(ns, key);
    if (value == null) return updateEntryPathSetToNull(ns, key, path);
    if (roll == null && rootIsTarget) return updateEntryRootSetToValue(ns, key, value);
    if (roll == null) return updateEntryPathSetToValue(ns, key, value, path);
    if (rootIsTarget) return updateEntryRootRollValue(ns, key, value, roll);
    return updateEntryPathRollValue(ns, key, value, path, roll);
  }

  private boolean updateEntryPathRollValue(
      @Nonnull String ns,
      @Nonnull String key,
      @Nonnull String value,
      @Nonnull String path,
      @Nonnull Integer roll) {
    String sql =
        "update keyjsonvalue\n"
            + "set jbvalue = case jsonb_typeof(jsonb_extract_path(jbvalue, VARIADIC cast(:path as text[])))\n"
            + "  when 'array' then case\n"
            + "    when :size < 0 or jsonb_array_length(jsonb_extract_path(jbvalue, VARIADIC cast(:path as text[]))) >= :size\n"
            + "      then jsonb_set(jbvalue, cast(:path as text[]), (jsonb_extract_path(jbvalue, VARIADIC cast(:path as text[])) - 0) || to_jsonb(ARRAY[cast(:value as jsonb)]), false)\n"
            + "    else jsonb_set(jbvalue, cast(:path as text[]), jsonb_extract_path(jbvalue, VARIADIC cast(:path as text[])) || to_jsonb(ARRAY[cast(:value as jsonb)]), false)\n"
            + "    end\n"
            + "  when 'string'  then jsonb_set(jbvalue, cast(:path as text[]), cast(:value as jsonb))\n"
            + "  when 'number'  then jsonb_set(jbvalue, cast(:path as text[]), cast(:value as jsonb))\n"
            + "  when 'object'  then jsonb_set(jbvalue, cast(:path as text[]), cast(:value as jsonb))\n"
            + "  when 'boolean' then jsonb_set(jbvalue, cast(:path as text[]), cast(:value as jsonb))\n"
            + "  when 'null'    then jsonb_set(jbvalue, cast(:path as text[]), to_jsonb(ARRAY[cast(:value as jsonb)]))\n"
            + "  -- undefined => same as null, start an array\n"
            + "  else jsonb_set(jbvalue, cast(:path as text[]), to_jsonb(ARRAY[cast(:value as jsonb)]))\n"
            + "  end\n"
            + "where namespace = :ns and namespacekey = :key";
    return getSession()
            .createNativeQuery(sql)
            .setParameter("ns", ns)
            .setParameter("key", key)
            .setParameter("value", value)
            .setParameter("size", roll)
            .setParameter("path", toJsonbPath(path))
            .executeUpdate()
        > 0;
  }

  private boolean updateEntryRootRollValue(
      @Nonnull String ns, @Nonnull String key, @Nonnull String value, @Nonnull Integer roll) {
    String sql =
        "update keyjsonvalue\n"
            + "set jbvalue = case jsonb_typeof(jbvalue)\n"
            + "  when 'null' then to_jsonb(ARRAY[cast(:value as jsonb)])\n"
            + "  when 'array' then case\n"
            + "    when :size < 0 or jsonb_array_length(jbvalue) >= :size\n"
            + "      then (jbvalue - 0) || to_jsonb(ARRAY[cast(:value as jsonb)])\n"
            + "    else jbvalue || to_jsonb(ARRAY[cast(:value as jsonb)])\n"
            + "    end\n"
            + "  else cast(:value as jsonb)\n"
            + "  end\n"
            + "where namespace = :ns and namespacekey = :key";
    return getSession()
            .createNativeQuery(sql)
            .setParameter("ns", ns)
            .setParameter("key", key)
            .setParameter("value", value)
            .setParameter("size", roll)
            .executeUpdate()
        > 0;
  }

  private boolean updateEntryPathSetToValue(
      @Nonnull String ns, @Nonnull String key, @Nonnull String value, @Nonnull String path) {
    return getSession()
            .createNativeQuery(
                "update keyjsonvalue set jbvalue = jsonb_set(jbvalue, cast(:path as text[]), cast(:value as jsonb), false) where namespace = :ns and namespacekey = :key")
            .setParameter("ns", ns)
            .setParameter("key", key)
            .setParameter("value", value)
            .setParameter("path", toJsonbPath(path))
            .executeUpdate()
        > 0;
  }

  private boolean updateEntryRootSetToValue(
      @Nonnull String ns, @Nonnull String key, @Nonnull String value) {
    return getSession()
            .createNativeQuery(
                "update keyjsonvalue set jbvalue = cast(:value as jsonb) where namespace = :ns and namespacekey = :key")
            .setParameter("ns", ns)
            .setParameter("key", key)
            .setParameter("value", value)
            .executeUpdate()
        > 0;
  }

  private boolean updateEntryPathSetToNull(
      @Nonnull String ns, @Nonnull String key, @Nonnull String path) {
    return getSession()
            .createNativeQuery(
                "update keyjsonvalue set jbvalue = jsonb_set(jbvalue, cast(:path as text[]), 'null', false) where namespace = :ns and namespacekey = :key")
            .setParameter("ns", ns)
            .setParameter("key", key)
            .setParameter("path", toJsonbPath(path))
            .executeUpdate()
        > 0;
  }

  private boolean updateEntryRootDelete(@Nonnull String ns, @Nonnull String key) {
    // delete
    return getSession()
            .createNativeQuery(
                "delete from keyjsonvalue where namespace = :ns and namespacekey = :key")
            .setParameter("ns", ns)
            .setParameter("key", key)
            .executeUpdate()
        > 0;
  }

  /**
   * Transforms Java/JSON property paths with paths as expected by jsonb functions, for example
   *
   * <p>{@code foo.bar.[0]} becomes {@code foo,bar,0}
   *
   * @param path a property path
   * @return a jsonb path
   */
  private static String toJsonbPath(String path) {
    if (path == null || path.isEmpty()) return "{}";
    String jsonbPath =
        path.replaceAll("\\[(\\d+)]", ",$1") // replace [#] with ,#
            .replace('.', ',') // replace . with ,
            .replace(",,", ","); // undo ,, that might originate from 1. replace with just ,
    return String.format("{%s}", jsonbPath);
  }
}
