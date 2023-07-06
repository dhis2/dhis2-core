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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository("org.hisp.dhis.dataset.LockExceptionStore")
public class HibernateLockExceptionStore extends HibernateGenericStore<LockException>
    implements LockExceptionStore {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private DataSetStore dataSetStore;

  private PeriodService periodService;

  public HibernateLockExceptionStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      DataSetStore dataSetStore,
      PeriodService periodService) {
    super(sessionFactory, jdbcTemplate, publisher, LockException.class, false);

    checkNotNull(dataSetStore);
    checkNotNull(periodService);

    this.dataSetStore = dataSetStore;
    this.periodService = periodService;
  }

  // -------------------------------------------------------------------------
  // LockExceptionStore Implementation
  // -------------------------------------------------------------------------

  @Override
  public void save(LockException lockException) {
    lockException.setPeriod(periodService.reloadPeriod(lockException.getPeriod()));

    super.save(lockException);
  }

  @Override
  public void update(LockException lockException) {
    lockException.setPeriod(periodService.reloadPeriod(lockException.getPeriod()));

    super.update(lockException);
  }

  @Override
  public List<LockException> getCombinations() {
    final String sql = "select distinct datasetid, periodid from lockexception";

    final List<LockException> lockExceptions = new ArrayList<>();

    jdbcTemplate.query(
        sql,
        new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet rs) throws SQLException {
            int dataSetId = rs.getInt(1);
            int periodId = rs.getInt(2);

            LockException lockException = new LockException();
            Period period = periodService.getPeriod(periodId);
            DataSet dataSet = dataSetStore.get(dataSetId);

            lockException.setDataSet(dataSet);
            lockException.setPeriod(period);

            lockExceptions.add(lockException);
          }
        });

    return lockExceptions;
  }

  @Override
  public void deleteCombination(DataSet dataSet, Period period) {
    final String hql = "delete from LockException where dataSet=:dataSet and period=:period";

    getQuery(hql).setParameter("dataSet", dataSet).setParameter("period", period).executeUpdate();
  }

  @Override
  public void deleteCombination(DataSet dataSet, Period period, OrganisationUnit organisationUnit) {
    final String hql =
        "delete from LockException where dataSet=:dataSet and period=:period and organisationUnit=:organisationUnit";

    getQuery(hql)
        .setParameter("dataSet", dataSet)
        .setParameter("period", period)
        .setParameter("organisationUnit", organisationUnit)
        .executeUpdate();
  }

  @Override
  public void delete(OrganisationUnit organisationUnit) {
    final String hql = "delete from LockException where organisationUnit=:organisationUnit";

    getQuery(hql).setParameter("organisationUnit", organisationUnit).executeUpdate();
  }

  @Override
  public List<LockException> getAllOrderedName(int first, int max) {
    return getList(
        getCriteriaBuilder(), newJpaParameters().setFirstResult(first).setMaxResults(max));
  }

  @Override
  public long getCount(DataElement dataElement, Period period, OrganisationUnit organisationUnit) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
        builder,
        newJpaParameters()
            .addPredicate(
                root -> builder.equal(root.get("period"), periodService.reloadPeriod(period)))
            .addPredicate(root -> builder.equal(root.get("organisationUnit"), organisationUnit))
            .addPredicate(root -> root.get("dataSet").in(dataElement.getDataSets())));
  }

  @Override
  public long getCount(DataSet dataSet, Period period, OrganisationUnit organisationUnit) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
        builder,
        newJpaParameters()
            .addPredicate(
                root -> builder.equal(root.get("period"), periodService.reloadPeriod(period)))
            .addPredicate(root -> builder.equal(root.get("organisationUnit"), organisationUnit))
            .addPredicate(root -> builder.equal(root.get("dataSet"), dataSet)));
  }

  @Override
  public boolean anyExists() {
    String hql = "from LockException";

    return getQuery(hql).setMaxResults(1).list().size() > 0;
  }
}
