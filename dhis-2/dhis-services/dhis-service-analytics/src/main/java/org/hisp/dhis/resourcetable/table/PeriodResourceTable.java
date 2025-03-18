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
package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyAbstractPeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.DateTime;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
public class PeriodResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_periodstructure";

  private final Logged logged;

  private final List<Period> periods;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  @Override
  public Table getMainTable() {
    return new Table(TABLE_NAME, getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    List<Column> columns =
        Lists.newArrayList(
            new Column("periodid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("iso", DataType.VARCHAR_50, Nullable.NOT_NULL),
            new Column("daysno", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("startdate", DataType.DATE, Nullable.NOT_NULL),
            new Column("enddate", DataType.DATE, Nullable.NOT_NULL),
            new Column("periodtypeid", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("periodtypename", DataType.VARCHAR_50, Nullable.NOT_NULL),
            new Column("monthstartdate", DataType.DATE, Nullable.NOT_NULL),
            new Column("year", DataType.INTEGER, Nullable.NOT_NULL));

    for (PeriodType periodType : PeriodType.PERIOD_TYPES) {
      columns.add(new Column(periodType.getName().toLowerCase(), DataType.VARCHAR_50));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("periodid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_periodstructure_iso"))
            .tableName(toStaging(TABLE_NAME))
            .unique(Unique.UNIQUE)
            .columns(List.of("iso"))
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.PERIOD_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    Calendar calendar = PeriodType.getCalendar();

    List<Object[]> batchArgs = new ArrayList<>();

    Set<String> uniqueIsoDates = new HashSet<>();

    for (Period period : periods) {
      if (period != null && period.isValid()) {
        final String isoDate = period.getIsoDate();
        final PeriodType periodType = period.getPeriodType();
        final Date monthStartDate = DateUtils.dateTruncMonth(period.getStartDate());
        final int year = resolveYearFromPeriod(period);

        if (!uniqueIsoDates.add(isoDate)) {
          // Protect against duplicates produced by calendars

          log.warn("Duplicate ISO date for period: '{}', ignoring ISO date: '{}'", period, isoDate);
          continue;
        }

        List<Object> values = new ArrayList<>();

        values.add(period.getId());
        values.add(isoDate);
        values.add(period.getDaysInPeriod());
        values.add(period.getStartDate());
        values.add(period.getEndDate());
        values.add(periodType.getId());
        values.add(periodType.getName());
        values.add(monthStartDate);
        values.add(year);

        for (Period pe : PeriodType.getPeriodTypePeriods(period, calendar)) {
          values.add(
              pe != null ? IdentifiableObjectUtils.getLocalPeriodIdentifier(pe, calendar) : null);
        }

        batchArgs.add(values.toArray());
      }
    }

    return Optional.of(batchArgs);
  }

  /**
   * Resolves the year from the given period.
   *
   * <p>Weekly period types are treated differently from other period types. A week is considered to
   * belong to the year for which 4 days or more fall inside. In this logic, 3 days are added to the
   * week start day and the year of the modified start date is used as reference year for the
   * period.
   *
   * @param period the {@link Period}.
   * @return the year.
   */
  private int resolveYearFromPeriod(Period period) {
    DateTime dateTime = new DateTime(period.getStartDate().getTime());

    if (WeeklyAbstractPeriodType.class.isAssignableFrom(period.getPeriodType().getClass())) {
      return dateTime.plusDays(3).getYear();
    } else {
      return dateTime.getYear();
    }
  }
}
