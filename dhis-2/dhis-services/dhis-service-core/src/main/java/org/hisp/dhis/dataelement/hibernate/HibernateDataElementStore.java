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
package org.hisp.dhis.dataelement.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Torgeir Lorange Ostby
 */
@Repository("org.hisp.dhis.dataelement.DataElementStore")
public class HibernateDataElementStore extends HibernateIdentifiableObjectStore<DataElement>
    implements DataElementStore {
  public HibernateDataElementStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, DataElement.class, aclService, false);
  }

  // -------------------------------------------------------------------------
  // DataElement
  // -------------------------------------------------------------------------

  @Override
  public List<DataElement> getDataElementByCategoryCombo(CategoryCombo categoryCombo) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("categoryCombo"), categoryCombo)));
  }

  @Override
  public List<DataElement> getDataElementsWithoutGroups() {
    String hql = "from DataElement d where size(d.groups) = 0";

    return getQuery(hql).setCacheable(true).list();
  }

  @Override
  public List<DataElement> getDataElementsWithoutDataSets() {
    String hql =
        "from DataElement d where size(d.dataSetElements) = 0 and d.domainType =:domainType";

    return getQuery(hql)
        .setParameter("domainType", DataElementDomain.AGGREGATE)
        .setCacheable(true)
        .list();
  }

  @Override
  public List<DataElement> getDataElementsWithDataSets() {
    String hql = "from DataElement d where size(d.dataSetElements) > 0";

    return getQuery(hql).setCacheable(true).list();
  }

  @Override
  public List<DataElement> getDataElementsByAggregationLevel(int aggregationLevel) {
    String hql = "from DataElement de join de.aggregationLevels al where al = :aggregationLevel";

    return getQuery(hql).setParameter("aggregationLevel", aggregationLevel).list();
  }

  @Override
  public DataElement getDataElement(String uid, User user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    // TODO MAS: Remove this in separate PR when we invalidate user sessions on user group changes.
    // Need to refetch here since the user might have been updated, and tests transactional
    // semantics might not have committed yet.
    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(user.getUid());

    List<Function<Root<DataElement>, Predicate>> sharingPredicates =
        getSharingPredicates(
            builder,
            user.getUid(),
            currentUserGroupInfo.getUserGroupUIDs(),
            AclService.LIKE_READ_METADATA);

    JpaQueryParameters<DataElement> param =
        new JpaQueryParameters<DataElement>()
            .addPredicates(sharingPredicates)
            .addPredicate(root -> builder.equal(root.get("uid"), uid));

    return getSingleResult(builder, param);
  }
}
