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
package org.hisp.dhis.userdatastore.hibernate;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.query.Query;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.hibernate.DatastoreQueryBuilder;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.userdatastore.UserDatastoreStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Stian Sandvold
 */
@Repository
public class HibernateUserDatastoreStore
    extends HibernateIdentifiableObjectStore<UserDatastoreEntry> implements UserDatastoreStore {
  public HibernateUserDatastoreStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        entityManager,
        jdbcTemplate,
        publisher,
        UserDatastoreEntry.class,
        currentUserService,
        aclService,
        true);
  }

  @Override
  public int countKeysInNamespace(User user, String namespace) {
    String hql =
        "select count(*) from UserDatastoreEntry where createdBy = :user and namespace = :namespace";
    Query<Long> count = getTypedQuery(hql);
    return count
        .setParameter("namespace", namespace)
        .setParameter("user", user)
        .getSingleResult()
        .intValue();
  }

  @Override
  public UserDatastoreEntry getEntry(User user, String namespace, String key) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get(BaseIdentifiableObject_.CREATED_BY), user))
            .addPredicate(root -> builder.equal(root.get("namespace"), namespace))
            .addPredicate(root -> builder.equal(root.get("key"), key)));
  }

  @Override
  public List<String> getNamespaces(User user) {
    Query<String> query =
        getTypedQuery("select distinct namespace from UserDatastoreEntry where createdBy = :user");
    return query.setParameter("user", user).list();
  }

  @Override
  public List<String> getKeysInNamespace(User user, String namespace) {
    return (getEntriesInNamespace(user, namespace))
        .stream().map(UserDatastoreEntry::getKey).collect(Collectors.toList());
  }

  @Override
  public List<UserDatastoreEntry> getEntriesInNamespace(User user, String namespace) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get(BaseIdentifiableObject_.CREATED_BY), user))
            .addPredicate(root -> builder.equal(root.get("namespace"), namespace)));
  }

  @Override
  public <T> T getEntries(
      User user, DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform) {
    DatastoreQueryBuilder builder =
        new DatastoreQueryBuilder(
            "from UserDatastoreEntry where createdBy = :user and namespace = :namespace", query);
    String hql = builder.createFetchHQL();

    Query<?> hQuery =
        getSession()
            .createQuery(hql, Object[].class)
            .setParameter("namespace", query.getNamespace())
            .setParameter("user", user)
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
}
