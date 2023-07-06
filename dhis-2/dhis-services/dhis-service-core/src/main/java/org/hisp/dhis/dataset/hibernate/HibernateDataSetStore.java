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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Kristian Nordal
 */
@Repository("org.hisp.dhis.dataset.DataSetStore")
public class HibernateDataSetStore extends HibernateIdentifiableObjectStore<DataSet>
    implements DataSetStore {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final PeriodService periodService;

  public HibernateDataSetStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService,
      PeriodService periodService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        DataSet.class,
        currentUserService,
        aclService,
        true);

    checkNotNull(periodService);

    this.periodService = periodService;
  }

  // -------------------------------------------------------------------------
  // DataSet
  // -------------------------------------------------------------------------

  @Override
  public void save(@Nonnull DataSet dataSet) {
    PeriodType periodType = periodService.reloadPeriodType(dataSet.getPeriodType());

    dataSet.setPeriodType(periodType);

    super.save(dataSet);
  }

  @Override
  public void update(@Nonnull DataSet dataSet) {
    PeriodType periodType = periodService.reloadPeriodType(dataSet.getPeriodType());

    dataSet.setPeriodType(periodType);

    super.update(dataSet);
  }

  @Override
  public List<DataSet> getDataSetsByPeriodType(PeriodType periodType) {
    PeriodType refreshedPeriodType = periodService.reloadPeriodType(periodType);

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<DataSet> parameters =
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("periodType"), refreshedPeriodType));

    return getList(builder, parameters);
  }

  @Override
  public List<DataSet> getDataSetsByDataEntryForm(DataEntryForm dataEntryForm) {
    if (dataEntryForm == null) {
      return Lists.newArrayList();
    }

    final String hql = "from DataSet d where d.dataEntryForm = :dataEntryForm";

    Query<DataSet> query = getQuery(hql);

    return query.setParameter("dataEntryForm", dataEntryForm).list();
  }

  @Override
  public List<DataSet> getDataSetsNotAssignedToOrganisationUnits() {
    return getQuery("from DataSet ds where size(ds.sources) = 0").list();
  }
}
