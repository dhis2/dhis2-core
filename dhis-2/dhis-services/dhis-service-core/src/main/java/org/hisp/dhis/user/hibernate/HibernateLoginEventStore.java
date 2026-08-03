/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.user.hibernate;

import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.user.DailyLoginStatistics;
import org.hisp.dhis.user.LoginEvent;
import org.hisp.dhis.user.LoginEventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Repository("org.hisp.dhis.user.LoginEventStore")
public class HibernateLoginEventStore extends HibernateGenericStore<LoginEvent>
    implements LoginEventStore {

  public HibernateLoginEventStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, LoginEvent.class, false);
  }

  @Override
  public void save(@Nonnull LoginEvent object) {
    // Flush so subsequent JDBC aggregate queries in the same transaction can see the row.
    super.save(object);
    getSession().flush();
  }

  @Override
  public List<DailyLoginStatistics> getDailyStatistics(
      java.util.Date startDate, java.util.Date endDate) {
    // Prefer the rollup table for dates already cleaned up, and live-aggregate remaining raw
    // rows. UNION ALL + outer group-by merges the two sources if a day appears in both
    // (should not happen after a successful cleanup, but keeps the query safe).
    final String sql =
        """
        select day, sum(logins)::int as logins, sum(uniqueusers)::int as uniqueusers
        from (
          select logindate as day, logins, uniqueusers
          from logineventdaily
          where logindate >= ? and logindate < ?
          union all
          select cast(timestamp as date) as day,
                 count(*)::int as logins,
                 count(distinct username)::int as uniqueusers
          from loginevent
          where timestamp >= ? and timestamp < ?
          group by cast(timestamp as date)
        ) s
        group by day
        order by day
        """;

    Timestamp start = new Timestamp(startDate.getTime());
    Timestamp end = new Timestamp(endDate.getTime());

    return jdbcTemplate.query(
        sql,
        ps -> {
          ps.setDate(1, new Date(startDate.getTime()));
          ps.setDate(2, new Date(endDate.getTime()));
          ps.setTimestamp(3, start);
          ps.setTimestamp(4, end);
        },
        rs -> {
          List<DailyLoginStatistics> result = new ArrayList<>();
          while (rs.next()) {
            LocalDate day = rs.getDate("day").toLocalDate();
            result.add(
                new DailyLoginStatistics(day, rs.getInt("logins"), rs.getInt("uniqueusers")));
          }
          return result;
        });
  }

  @Override
  public int rollupOlderThan(java.util.Date olderThan) {
    final String sql =
        """
        insert into logineventdaily (logindate, logins, uniqueusers)
        select cast(timestamp as date) as day,
               count(*)::int,
               count(distinct username)::int
        from loginevent
        where timestamp < ?
        group by cast(timestamp as date)
        on conflict (logindate) do update
          set logins = excluded.logins,
              uniqueusers = excluded.uniqueusers
        """;
    return jdbcTemplate.update(sql, new Timestamp(olderThan.getTime()));
  }

  @Override
  public int deleteOlderThan(java.util.Date olderThan) {
    return jdbcTemplate.update(
        "delete from loginevent where timestamp < ?", new Timestamp(olderThan.getTime()));
  }

  @Override
  public LocalDate getEarliestRollupDate() {
    return jdbcTemplate.query(
        "select min(logindate) as d from logineventdaily",
        rs -> {
          if (rs.next()) {
            Date d = rs.getDate("d");
            return d == null ? null : d.toLocalDate();
          }
          return null;
        });
  }
}
