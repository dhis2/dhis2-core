/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.icon.hibernate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconOperationParams;
import org.hisp.dhis.icon.IconStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair Asghar
 */
@Repository("org.hisp.dhis.icon.IconStore")
public class HibernateIconStore extends HibernateIdentifiableObjectStore<Icon>
    implements IconStore {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public HibernateIconStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Icon.class, aclService, true);
  }

  @Override
  public long count(IconOperationParams params) {

    String sql = """
            select count(*) as count from customicon c
            """;
    Map<String, Object> parameterSource = new HashMap<>();
    sql = buildIconQuery(params, sql, parameterSource);

    return Optional.ofNullable(
            namedParameterJdbcTemplate.queryForObject(sql, parameterSource, Long.class))
        .orElse(0L);
  }

  @Override
  public Icon getIconByKey(String key) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<Icon> parameters =
        newJpaParameters().addPredicate(root -> builder.equal(root.get("key"), key));

    return getSingleResult(builder, parameters);
  }

  @Override
  public Set<String> getKeywords() {

    Set<String> keys = new HashSet<>();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HashSet> criteriaQuery = builder.createQuery(HashSet.class);

    Root<Icon> root = criteriaQuery.from(Icon.class);
    criteriaQuery.where(builder.isNotNull(root.get("keywords")));
    criteriaQuery.select(root.get("keywords"));

    entityManager.createQuery(criteriaQuery).getResultList().forEach(keys::addAll);

    return Collections.unmodifiableSet(keys);
  }

  @Override
  public Set<Icon> getIcons(IconOperationParams params) {

    String sql = """
            select * from customicon c
            """;
    Map<String, Object> parameterSource = new HashMap<>();
    sql = buildIconQuery(params, sql, parameterSource);

    NativeQuery<Icon> query = getSession().createNativeQuery(sql, Icon.class);

    setParameters(query, parameterSource);

    if (params.isPaging()) {
      query.setFirstResult(params.getPager().getPage());
      query.setMaxResults(params.getPager().getPageSize());
    }

    return query.list().stream().collect(Collectors.toUnmodifiableSet());
  }

  private void setParameters(Query query, Map<String, Object> parameterSource) {
    parameterSource.forEach(query::setParameter);
  }

  private String buildIconQuery(
      IconOperationParams iconOperationParams, String sql, Map<String, Object> parameterSource) {
    SqlHelper hlp = new SqlHelper(true);

    if (iconOperationParams.hasLastUpdatedStartDate()) {
      sql += hlp.whereAnd() + " c.lastupdated >= :lastUpdatedStartDate ";

      parameterSource.put(":lastUpdatedStartDate", iconOperationParams.getLastUpdatedStartDate());
    }

    if (iconOperationParams.hasLastUpdatedEndDate()) {
      sql += hlp.whereAnd() + " c.lastupdated <= :lastUpdatedEndDate ";

      parameterSource.put("lastUpdatedEndDate", iconOperationParams.getLastUpdatedEndDate());
    }

    if (iconOperationParams.hasCreatedStartDate()) {
      sql += hlp.whereAnd() + " c.created >= :createdStartDate";

      parameterSource.put("createdStartDate", iconOperationParams.getCreatedStartDate());
    }

    if (iconOperationParams.hasCreatedEndDate()) {
      sql += hlp.whereAnd() + " c.created <= :createdEndDate ";

      parameterSource.put("createdEndDate", iconOperationParams.getCreatedEndDate());
    }

    if (iconOperationParams.hasCustom()) {
      sql += hlp.whereAnd() + " c.custom = :custom ";

      parameterSource.put("custom", iconOperationParams.getCustom());
    }

    if (iconOperationParams.hasKeywords()) {

      sql += hlp.whereAnd() + " keywords @> cast(:param as jsonb)";

      String keywordsJsonArray = null;
      try {
        keywordsJsonArray = objectMapper.writeValueAsString(iconOperationParams.getKeywords());

      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      parameterSource.put("param", keywordsJsonArray);
    }

    if (iconOperationParams.hasKeys()) {
      sql += hlp.whereAnd() + " c.iconkey IN (:keys )";

      parameterSource.put("keys", iconOperationParams.getKeys());
    }

    return sql;
  }
}
