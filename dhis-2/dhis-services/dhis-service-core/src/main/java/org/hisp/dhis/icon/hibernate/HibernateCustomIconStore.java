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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.CustomIconOperationParams;
import org.hisp.dhis.icon.CustomIconStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair Asghar
 */
@Repository("org.hisp.dhis.icon.CustomIconStore")
public class HibernateCustomIconStore extends HibernateIdentifiableObjectStore<CustomIcon>
    implements CustomIconStore {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public HibernateCustomIconStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, CustomIcon.class, aclService, true);
  }

  @Override
  public long count(CustomIconOperationParams iconOperationParams) {

    String sql = """
            select count(*) from customicon
            """;
    Map<String, Object> parameterSource = new HashMap<>();
    sql = buildIconQuery(iconOperationParams, sql, parameterSource);

    NativeQuery<Long> query = getSession().createNativeQuery(sql, Long.class);

    setParameters(query, parameterSource);

    return query.getSingleResult();
  }

  @Override
  public CustomIcon getCustomIconByKey(String key) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<CustomIcon> parameters =
        newJpaParameters().addPredicate(root -> builder.equal(root.get("key"), key));

    return getSingleResult(builder, parameters);
  }

  @Override
  public Set<String> getKeywords() {

    Set<String> keys = new HashSet<>();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HashSet> criteriaQuery = builder.createQuery(HashSet.class);

    Root<CustomIcon> root = criteriaQuery.from(CustomIcon.class);
    criteriaQuery.where(builder.isNotNull(root.get("keywords")));
    criteriaQuery.select(root.get("keywords"));

    entityManager.createQuery(criteriaQuery).getResultList().forEach(keys::addAll);

    return keys;
  }

  @Override
  public Set<CustomIcon> getCustomIconsByKeywords(Set<String> keywords) {

    String keywordsJsonArray;
    try {
      keywordsJsonArray = objectMapper.writeValueAsString(keywords);

    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return Set.of();
    }

    String sql =
        """
         SELECT * FROM customicon WHERE keywords @> cast(:param as jsonb);
        """;
    return getSession()
        .createNativeQuery(sql, CustomIcon.class)
        .setParameter("param", keywordsJsonArray)
        .getResultStream()
        .collect(Collectors.toSet());
  }

  @Override
  public Set<CustomIcon> getCustomIcons(CustomIconOperationParams iconOperationParams) {

    String sql = """
            select * from customicon c
            """;
    Map<String, Object> parameterSource = new HashMap<>();
    sql = buildIconQuery(iconOperationParams, sql, parameterSource);

    NativeQuery<CustomIcon> query = getSession().createNativeQuery(sql, CustomIcon.class);

    setParameters(query, parameterSource);

    return query.list().stream().collect(Collectors.toUnmodifiableSet());
  }

  private void setParameters(NativeQuery<?> query, Map<String, Object> parameterSource) {
    parameterSource.entrySet().stream().forEach(p -> query.setParameter(p.getKey(), p.getValue()));
  }

  private String buildIconQuery(
      CustomIconOperationParams iconOperationParams,
      String sql,
      Map<String, Object> parameterSource) {
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
