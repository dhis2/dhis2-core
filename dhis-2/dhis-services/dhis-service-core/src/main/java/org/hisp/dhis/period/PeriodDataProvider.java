/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.period;

import static java.time.LocalDate.now;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Component responsible for fetching, extracting and providing specific data related to existing
 * periods in the database.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class PeriodDataProvider {
  static final int BEFORE_AND_AFTER_DATA_YEARS_SUPPORTED = 5;

  static final int DEFAULT_FIRST_YEAR_SUPPORTED = 1975;

  static final int DEFAULT_LATEST_YEAR_SUPPORTED = now().plusYears(25).getYear();

  private final JdbcTemplate jdbcTemplate;

  public enum DataSource {
    SYSTEM_DEFINED,
    DATABASE
  }

  /**
   * Returns a distinct union of all years available in the "event" table + "period" table, both
   * from aggregate and tracker, with 5 years previous and future additions.
   *
   * <p>ie: [extra_5_previous_years, data_years, extra_5_future_year]
   *
   * @param source the source ({@link DataSource}) where the years wil be retrieved from.
   * @return an unmodifiable list of distinct years and the respective additions.
   */
  public List<Integer> getAvailableYears(DataSource source) {
    List<Integer> availableDataYears = new ArrayList<>();

    if (source == SYSTEM_DEFINED) {
      // Add default hard-coded years (keeps it backward compatible).
      for (int year = DEFAULT_FIRST_YEAR_SUPPORTED; year <= DEFAULT_LATEST_YEAR_SUPPORTED; year++) {
        availableDataYears.add(year);
      }
    } else {
      // Add years dynamically, based on the database.
      availableDataYears.addAll(fetchAvailableYears());
      addSafetyBuffer(availableDataYears, BEFORE_AND_AFTER_DATA_YEARS_SUPPORTED);
    }

    sort(availableDataYears);

    return unmodifiableList(availableDataYears);
  }

  /**
   * Adds some extra years (based on the buffer) to the given list of years. The extra years are
   * added at the end of the list.
   *
   * <p>Let's say that the given list contains [2021, 2024], and the buffer is 3. This will result
   * in a list like [2021, 2024, 2020, 2025, 2019, 2026, 2018, 2027].
   *
   * @param years the list of years to append new years as buffer.
   * @param buffer the buffer representing the amount of years to add.
   */
  void addSafetyBuffer(List<Integer> years, int buffer) {
    int firstYear = years.get(0);
    int lastYear = years.get(years.size() - 1);

    for (int i = 0; i < buffer; i++) {
      years.add(--firstYear);
      years.add(++lastYear);
    }
  }

  /**
   * Queries the database in order to fetch all years available in the "period" and "event" tables.
   * It does a distinct union of all years available in the "event" and "period" table, both from
   * aggregate and tracker. If nothing is found, the current year is returned (as default).
   *
   * @return the list of distinct years found in the database, or current year.
   */
  private List<Integer> fetchAvailableYears() {
    String dueDateOrExecutionDate =
        "(case when 'SCHEDULE' = ev.status then ev.scheduleddate else ev.occurreddate end)";

    String sql =
        "( select distinct (extract(year from pe.startdate)) as datayear from period pe )"
            + " union"
            + " ( select distinct (extract(year from pe.enddate)) as datayear from period pe )"
            + " union"
            + " ( select distinct (extract(year from "
            + dueDateOrExecutionDate
            + ")) as datayear"
            + " from event ev"
            + " where "
            + dueDateOrExecutionDate
            + " is not null"
            + " and ev.deleted is false ) order by datayear asc";

    List<Integer> years = jdbcTemplate.queryForList(sql, Integer.class);

    if (isEmpty(years)) {
      years.add(now().getYear());
    }

    return years;
  }
}
