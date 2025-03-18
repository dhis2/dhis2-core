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
package org.hisp.dhis.indicator.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorStore;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Repository("org.hisp.dhis.indicator.IndicatorStore")
public class HibernateIndicatorStore extends HibernateIdentifiableObjectStore<Indicator>
    implements IndicatorStore {
  public HibernateIndicatorStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Indicator.class, aclService, true);
  }

  // -------------------------------------------------------------------------
  // Indicator
  // -------------------------------------------------------------------------

  @Override
  public List<Indicator> getIndicatorsWithGroupSets() {
    final String hql = "from Indicator d where size(d.groupSets) > 0";

    return getQuery(hql).setCacheable(true).list();
  }

  @Override
  public List<Indicator> getIndicatorsWithoutGroups() {
    final String hql = "from Indicator d where size(d.groups) = 0";

    return getQuery(hql).setCacheable(true).list();
  }

  @Override
  public List<Indicator> getIndicatorsWithDataSets() {
    final String hql = "from Indicator d where size(d.dataSets) > 0";

    return getQuery(hql).setCacheable(true).list();
  }

  @Override
  public List<Indicator> getAssociatedIndicators(List<IndicatorType> indicatorTypes) {
    // language=sql
    TypedQuery<Indicator> query =
        entityManager.createQuery(
            "FROM Indicator i where i.indicatorType in :indicatorTypes", Indicator.class);
    return query.setParameter("indicatorTypes", indicatorTypes).getResultList();
  }

  @Override
  public List<Indicator> getIndicatorsWithNumeratorContaining(String search) {
    return getQuery("FROM Indicator i where i.numerator like :search", Indicator.class)
        .setParameter("search", "%" + search + "%")
        .getResultList();
  }

  @Override
  public List<Indicator> getIndicatorsWithDenominatorContaining(String search) {
    return getQuery("FROM Indicator i where i.denominator like :search", Indicator.class)
        .setParameter("search", "%" + search + "%")
        .getResultList();
  }

  @Override
  public int updateNumeratorDenominatorContaining(String find, String replace) {
    String sql =
        """
        update indicator
        set numerator = replace(numerator, '%s', '%s'),
            denominator = replace(denominator, '%s', '%s')
        where numerator like '%s'
          or denominator like '%s';
        """
            .formatted(find, replace, find, replace, "%" + find + "%", "%" + find + "%");
    return jdbcTemplate.update(sql);
  }
}
