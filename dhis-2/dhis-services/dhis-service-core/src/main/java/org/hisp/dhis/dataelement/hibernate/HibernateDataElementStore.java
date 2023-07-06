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
package org.hisp.dhis.dataelement.hibernate;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
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
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        DataElement.class,
        currentUserService,
        aclService,
        false);
  }

  // -------------------------------------------------------------------------
  // DataElement
  // -------------------------------------------------------------------------

  @Override
  public List<DataElement> getDataElementsByDomainType(DataElementDomain domainType) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("domainType"), domainType)));
  }

  @Override
  public List<DataElement> getDataElementsByValueType(ValueType valueType) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("valueType"), valueType)));
  }

  @Override
  public List<DataElement> getDataElementByCategoryCombo(CategoryCombo categoryCombo) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("categoryCombo"), categoryCombo)));
  }

  @Override
  public List<DataElement> getDataElementsByZeroIsSignificant(boolean zeroIsSignificant) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("zeroIsSignificant"), zeroIsSignificant))
            .addPredicate(root -> root.get("valueType").in(ValueType.NUMERIC_TYPES)));
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

    JpaQueryParameters<DataElement> param =
        new JpaQueryParameters<DataElement>()
            .addPredicates(getSharingPredicates(builder, user, AclService.LIKE_READ_METADATA))
            .addPredicate(root -> builder.equal(root.get("uid"), uid));

    return getSingleResult(builder, param);
  }
}
