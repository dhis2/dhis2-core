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
package org.hisp.dhis.dataset.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Tri
 */
@Repository
public class HibernateSectionStore extends HibernateIdentifiableObjectStore<Section>
    implements SectionStore {
  public HibernateSectionStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Section.class, aclService, true);
  }

  @Override
  public Section getSectionByName(String name, DataSet dataSet) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("name"), name))
            .addPredicate(root -> builder.equal(root.get("dataSet"), dataSet)));
  }

  @Override
  public List<Section> getSectionsByDataElement(String uid) {
    final String sql =
        """
        select * from section s \
        left join sectiondataelements sde on s.sectionid = sde.sectionid \
        left join sectiongreyedfields sgf on s.sectionid = sgf.sectionid \
        left join dataelementoperand deo on sgf.dataelementoperandid = deo.dataelementoperandid \
        , dataelement de \
        where de.uid = :dataElementId and (sde.dataelementid = de.dataelementid or deo.dataelementid = de.dataelementid);""";

    return nativeSynchronizedTypedQuery(sql).setParameter("dataElementId", uid).list();
  }

  @Override
  public List<Section> getSectionsByIndicators(Collection<Indicator> indicators) {
    final String sql =
        """
        select s.* from section s \
        join sectionindicators si on s.sectionid = si.sectionid \
        where si.indicatorid in :indicators \
        group by s.sectionid""";

    return nativeSynchronizedTypedQuery(sql).setParameter("indicators", indicators).list();
  }

  @Override
  public List<Section> getSectionsByDataElement(Collection<DataElement> dataElements) {
    final String sql =
        """
        select s from Section s \
        join s.dataElements de \
        where de in :dataElements \
        group by s.id""";

    return getQuery(sql, Section.class).setParameter("dataElements", dataElements).getResultList();
  }
}
