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
package org.hisp.dhis.relationship.hibernate;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew
 */
@Repository("org.hisp.dhis.relationship.RelationshipStore")
public class HibernateRelationshipStore extends SoftDeleteHibernateObjectStore<Relationship>
    implements RelationshipStore {

  public HibernateRelationshipStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Relationship.class, aclService, true);
  }

  @Override
  public List<Relationship> getByRelationshipType(RelationshipType relationshipType) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.join("relationshipType"), relationshipType)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getUidsByRelationshipKeys(List<String> relationshipKeyList) {
    if (CollectionUtils.isEmpty(relationshipKeyList)) {
      return Collections.emptyList();
    }

    String sql =
        """
        SELECT R.uid
        FROM relationship R
        INNER JOIN relationshiptype RT ON RT.relationshiptypeid = R.relationshiptypeid
        WHERE R.deleted = false AND (R.key IN (:keys)
        OR (R.inverted_key IN (:keys) AND RT.bidirectional = TRUE))
        """;
    List<Object> c =
        nativeSynchronizedQuery(sql)
            .addSynchronizedEntityClass(RelationshipType.class)
            .setParameter("keys", relationshipKeyList)
            .getResultList();

    return c.stream().map(String::valueOf).collect(Collectors.toList());
  }

  @Override
  public List<Relationship> getByUidsIncludeDeleted(List<String> uids) {
    CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

    CriteriaQuery<Relationship> query = criteriaBuilder.createQuery(Relationship.class);

    Root<Relationship> root = query.from(Relationship.class);

    query.where(criteriaBuilder.in(root.get("uid")).value(uids));

    try {
      return getSession().createQuery(query).getResultList();
    } catch (NoResultException nre) {
      return null;
    }
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<Relationship>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected Relationship postProcessObject(Relationship relationship) {
    return (relationship == null || relationship.isDeleted()) ? null : relationship;
  }
}
