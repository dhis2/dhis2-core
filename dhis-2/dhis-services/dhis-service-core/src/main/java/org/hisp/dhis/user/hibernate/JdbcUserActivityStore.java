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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.user.DailyActiveUsers;
import org.hisp.dhis.user.UserActivityStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain JDBC store for the {@code useractivity} table. There is no Hibernate entity: rows are
 * write-once-per-day upserts and aggregate reads only.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Repository
@RequiredArgsConstructor
public class JdbcUserActivityStore implements UserActivityStore {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void upsertActivity(String username, LocalDate date, Date lastActive) {
    jdbcTemplate.update(
        """
        insert into useractivity (username, activitydate, lastactive)
        values (?, ?, ?)
        on conflict (username, activitydate) do update set lastactive = excluded.lastactive
        """,
        username,
        java.sql.Date.valueOf(date),
        new Timestamp(lastActive.getTime()));
  }

  @Override
  public List<Integer> getActiveUserCounts(List<Date> sinceDates) {
    if (sinceDates.isEmpty()) {
      return List.of();
    }
    StringBuilder sql = new StringBuilder("select ");
    for (int i = 0; i < sinceDates.size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append("count(distinct username) filter (where lastactive >= ?) as c").append(i);
    }
    sql.append(" from useractivity where lastactive >= ?");
    Date earliest = sinceDates.stream().min(Date::compareTo).orElseThrow();
    return jdbcTemplate.query(
        sql.toString(),
        ps -> {
          int i = 1;
          for (Date since : sinceDates) {
            ps.setTimestamp(i++, new Timestamp(since.getTime()));
          }
          ps.setTimestamp(i, new Timestamp(earliest.getTime()));
        },
        rs -> {
          rs.next();
          List<Integer> counts = new ArrayList<>(sinceDates.size());
          for (int i = 0; i < sinceDates.size(); i++) {
            counts.add(rs.getInt(i + 1));
          }
          return counts;
        });
  }

  @Override
  public List<DailyActiveUsers> getDailyStatistics(Date startDate, Date endDate) {
    final String sql =
        """
        select day, sum(activeusers)::int as activeusers
        from (
          select activitydate as day, activeusers
          from useractivitydaily
          where activitydate >= ? and activitydate < ?
          union all
          select activitydate as day, count(*)::int as activeusers
          from useractivity
          where activitydate >= ? and activitydate < ?
          group by activitydate
        ) s
        group by day
        order by day
        """;
    java.sql.Date start = new java.sql.Date(startDate.getTime());
    java.sql.Date end = new java.sql.Date(endDate.getTime());
    return jdbcTemplate.query(
        sql,
        ps -> {
          ps.setDate(1, start);
          ps.setDate(2, end);
          ps.setDate(3, start);
          ps.setDate(4, end);
        },
        rs -> {
          List<DailyActiveUsers> result = new ArrayList<>();
          while (rs.next()) {
            result.add(
                new DailyActiveUsers(rs.getDate("day").toLocalDate(), rs.getInt("activeusers")));
          }
          return result;
        });
  }

  @Override
  public int rollupOlderThan(Date olderThan) {
    final String sql =
        """
        insert into useractivitydaily (activitydate, activeusers)
        select activitydate, count(*)::int
        from useractivity
        where activitydate < ?
        group by activitydate
        on conflict (activitydate) do update set activeusers = excluded.activeusers
        """;
    return jdbcTemplate.update(sql, new java.sql.Date(olderThan.getTime()));
  }

  @Override
  public int deleteOlderThan(Date olderThan) {
    return jdbcTemplate.update(
        "delete from useractivity where activitydate < ?", new java.sql.Date(olderThan.getTime()));
  }
}
