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
package org.hisp.dhis.period.hibernate;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Persistence for {@link Period} and {@link PeriodType}.
 *
 * <p>These two are fundamentally different to most other {@link
 * org.hisp.dhis.common.IdentifiableObject}s not just because they do not have a UID, they also are
 * "implicitly" created sometimes. Their table is mainly to remember the mapping between their
 * external name (iso/name) and the internal DB PK that is used as FK in other tables. When such an
 * entry is needed a transient object is passed to this store which resolves the PK ID. This might
 * require to insert a new mapping into the table. Since this is implicit such inserts happen in a
 * {@link StatelessSession} so the caller that wants to reference a period does not need to be in a
 * write transaction.
 *
 * <p>This means conceptually all instances of {@link Period} and {@link PeriodType} can and should
 * be considered transient (detached) objects, but they can be linked using {@link
 * #reloadForceAddPeriod(Period)}.
 *
 * @author Jan Bernitt
 */
@Repository
@Slf4j
public class HibernatePeriodStore extends HibernateIdentifiableObjectStore<Period>
    implements PeriodStore {

  private final Map<String, Long> periodIdByIsoPeriod = new ConcurrentHashMap<>();

  private final DataSource dataSource;

  public HibernatePeriodStore(
      EntityManager entityManager,
      DataSource dataSource,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Period.class, aclService, false);
    this.dataSource = dataSource;

    transientIdentifiableProperties = true;
  }

  @Override
  public void invalidateCache() {
    periodIdByIsoPeriod.clear();
  }

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------

  /**
   * This method is overridden to use native SQL to insert a period with its ISO value. This also
   * handles to never insert duplicates but to treat such an (accidental) attempt as a NOOP update
   * instead. In both cases (INSERT and UPDATE) the ID will be set in {@link Period#setId(long)}. In
   * that way this is effectively a "load or create" operation. To allow working with transient
   * period types the type parameter is provided as name, not as ID. Also, this uses a stateless
   * session so the creation (write) can happen without the need to already be in a write
   * transaction context.
   */
  @Override
  public void save(@Nonnull Period period, @Nonnull UserDetails userDetails, boolean clearSharing) {
    String sql1 = "SELECT periodid FROM period WHERE iso = :iso";
    String sql2 =
        """
        INSERT INTO period (periodid, periodtypeid, startdate, enddate, iso)
        VALUES (nextval('hibernate_sequence'),
          (SELECT periodtypeid FROM periodtype WHERE name = :type), :start, :end, :iso)""";
    String isoDate = period.getIsoDate();
    Object id =
        runAutoJoinTransaction(
            session -> {
              Object pk =
                  getSingleResult(session.createNativeQuery(sql1).setParameter("iso", isoDate));
              if (pk != null) {
                return pk;
              }
              session
                  .createNativeQuery(sql2)
                  .setParameter("type", period.getPeriodType().getName())
                  .setParameter("start", period.getStartDate())
                  .setParameter("end", period.getEndDate())
                  .setParameter("iso", isoDate)
                  .executeUpdate();
              return session.createNativeQuery("SELECT lastval()").uniqueResult();
            });
    if (id instanceof Number n) {
      long periodId = n.longValue();
      periodIdByIsoPeriod.putIfAbsent(isoDate, periodId);
      period.setId(periodId);
    }
  }

  @Override
  public void delete(@Nonnull Period period) {
    String isoDate = period.getIsoDate();
    int deleted =
        getSession()
            .createNativeQuery("DELETE FROM period where iso = :iso")
            .setParameter("iso", isoDate)
            .executeUpdate();
    if (deleted > 0) {
      periodIdByIsoPeriod.remove(isoDate);
    }
  }

  @Override
  public void update(@Nonnull Period object, @Nonnull UserDetails userDetails) {
    throw new UnsupportedOperationException("Periods are never updated.");
  }

  @Override
  public void addPeriod(Period period) {
    save(period);
  }

  @Override
  public List<Period> getPeriodsBetweenDates(Date startDate, Date endDate) {
    String sql = "SELECT * FROM period p WHERE p.startdate >=:start AND p.enddate <=:end";
    return getSession()
        .createNativeQuery(sql, Period.class)
        .setParameter("start", startDate)
        .setParameter("end", endDate)
        .list();
  }

  @Override
  public List<Period> getPeriodsBetweenDates(PeriodType periodType, Date startDate, Date endDate) {
    String sql =
        """
      SELECT * FROM period p
      WHERE p.startdate >= :start
        AND p.enddate <= :end
        AND p.periodtypeid = (SELECT periodtypeid FROM periodtype WHERE name = :type)""";

    return getSession()
        .createNativeQuery(sql, Period.class)
        .setParameter("start", startDate)
        .setParameter("end", endDate)
        .setParameter("type", periodType.getName())
        .list();
  }

  @Override
  public List<Period> getIntersectingPeriods(Date startDate, Date endDate) {
    String sql =
        """
      SELECT * FROM period p
      WHERE p.startdate <= :end
        AND p.enddate >= :start""";

    return getSession()
        .createNativeQuery(sql, Period.class)
        .setParameter("start", startDate)
        .setParameter("end", endDate)
        .list();
  }

  @Override
  public Period getPeriodFromDates(Date startDate, PeriodType periodType) {
    return reloadPeriod(periodType.createPeriod(startDate));
  }

  @Override
  @CheckForNull
  public Period reloadPeriod(Period period) {
    Long id = getPeriodId(period);
    if (id == null) {
      return null;
    }
    period.setId(id); // link the transient instance by supplying the persisted ID
    return period;
  }

  @CheckForNull
  private Long getPeriodId(Period period) {
    Long cachedId = periodIdByIsoPeriod.get(period.getIsoDate());
    if (cachedId != null) {
      return cachedId;
    }
    String isoDate = period.getIsoDate();
    String sql = "select periodid from period p where p.iso = :iso";
    NativeQuery<?> q = getSession().createNativeQuery(sql).setParameter("iso", isoDate);
    Object id = getSingleResult(q);
    if (id instanceof Number n) {
      long periodId = n.longValue();
      periodIdByIsoPeriod.putIfAbsent(isoDate, periodId);
      return periodId;
    }
    return null;
  }

  @Override
  public Period reloadForceAddPeriod(Period period) {
    addPeriod(period);
    return period;
  }

  // -------------------------------------------------------------------------
  // PeriodType (do not use generic store which is linked to Period)
  // -------------------------------------------------------------------------

  @Override
  public void addPeriodType(PeriodType periodType) {
    String name = periodType.getName();
    String sql1 = "SELECT periodtypeid from periodtype where name = :name";
    String sql2 =
        """
        INSERT INTO periodtype (periodtypeid, name)
        VALUES (nextval('hibernate_sequence'), :name)""";
    Object id =
        runAutoJoinTransaction(
            session -> {
              Object pk =
                  getSingleResult(session.createNativeQuery(sql1).setParameter("name", name));
              if (pk != null) {
                return pk;
              }
              session.createNativeQuery(sql2).setParameter("name", name).executeUpdate();
              return session.createNativeQuery("SELECT lastval()").uniqueResult();
            });
    if (id instanceof Number n) {
      int periodTypeId = n.intValue();
      periodType.setId(periodTypeId);
      return;
    }
    throw new IllegalStateException("Failed to upsert period type: " + name);
  }

  @Override
  public List<PeriodType> getAllPeriodTypes() {
    return getSession().createNativeQuery("select * from periodtype", PeriodType.class).list();
  }

  private <R> R runAutoJoinTransaction(Function<StatelessSession, R> query) {
    boolean active = TransactionSynchronizationManager.isActualTransactionActive();
    boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

    if (active && !readOnly) {
      // run in existing TX (for visibility)
      Connection borrowedConnection = DataSourceUtils.getConnection(dataSource);
      StatelessSession session =
          getSession().getSessionFactory().openStatelessSession(borrowedConnection);
      return query.apply(session);
    }

    // run in new TX
    StatelessSession session = getSession().getSessionFactory().openStatelessSession();

    Transaction transaction = null;
    try {
      transaction = session.beginTransaction();
      R result = query.apply(session);
      transaction.commit();
      return result;
    } catch (RuntimeException e) {
      // Handle rollback for self-managed transactions
      if (transaction != null && transaction.isActive()) {
        transaction.rollback();
      }
      throw e;
    } finally {
      try {
        session.close();
      } catch (Exception e) {
        log.error("Session close error", e);
      }
    }
  }

  // -------------------------------------------------------------------------
  // RelativePeriods (do not use generic store which is linked to Period)
  // -------------------------------------------------------------------------

  @Override
  public void deleteRelativePeriods(RelativePeriods relativePeriods) {
    getSession().delete(relativePeriods);
  }
}
