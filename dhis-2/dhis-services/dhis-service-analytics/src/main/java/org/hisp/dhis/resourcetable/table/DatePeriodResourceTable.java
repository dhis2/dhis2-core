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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DatePeriodResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_dateperiodstructure";

  private final Logged logged;

  private final List<Integer> years;

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
            new Column("dateperiod", DataType.DATE, Nullable.NOT_NULL),
            new Column("year", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("monthstartdate", DataType.DATE, Nullable.NOT_NULL));

    for (PeriodType periodType : PeriodType.PERIOD_TYPES) {
      columns.add(new Column(periodType.getName().toLowerCase(), DataType.VARCHAR_50));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("dateperiod");
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATE_PERIOD_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();

    List<Object[]> batchArgs = new ArrayList<>();

    int firstYearSupported = years.get(0);
    int lastYearSupported = years.get(years.size() - 1);

    Date startDate = new Cal(firstYearSupported, 1, 1, true).time();
    Date endDate = new Cal(lastYearSupported + 1, 1, 1, true).time();

    List<Period> dailyPeriods = new DailyPeriodType().generatePeriods(startDate, endDate);

    List<Date> days =
        new UniqueArrayList<>(
            dailyPeriods.stream().map(Period::getStartDate).collect(Collectors.toList()));

    Calendar calendar = PeriodType.getCalendar();

    for (Date day : days) {
      final int year = PeriodType.getCalendar().fromIso(day).getYear();
      final Date monthStartDate = DateUtils.dateTruncMonth(day);

      List<Object> values = new ArrayList<>();

      values.add(day);
      values.add(year);
      values.add(monthStartDate);

      for (PeriodType periodType : periodTypes) {
        values.add(periodType.createPeriod(day, calendar).getIsoDate());
      }

      batchArgs.add(values.toArray());
    }

    return Optional.of(batchArgs);
  }
}
