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
package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
public class DatePeriodResourceTable extends ResourceTable<Period> {
  private final String tableType;

  public DatePeriodResourceTable(List<Period> objects, String tableType) {
    super(objects);
    this.tableType = tableType;
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATE_PERIOD_STRUCTURE;
  }

  @Override
  public String getCreateTempTableStatement() {
    String sql =
        "create "
            + tableType
            + " table "
            + getTempTableName()
            + " (dateperiod date not null primary key, year integer not null";

    for (PeriodType periodType : PeriodType.PERIOD_TYPES) {
      sql += ", " + quote(periodType.getName().toLowerCase()) + " varchar(15)";
    }

    sql += ")";

    return sql;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();

    List<Object[]> batchArgs = new ArrayList<>();

    // TODO Create a dynamic solution instead of having fixed dates

    Date startDate = new Cal(FIRST_YEAR_SUPPORTED, 1, 1, true).time();
    Date endDate = new Cal(LATEST_YEAR_SUPPORTED + 1, 1, 1, true).time();

    List<Period> dailyPeriods = new DailyPeriodType().generatePeriods(startDate, endDate);

    List<Date> days =
        new UniqueArrayList<>(
            dailyPeriods.stream().map(Period::getStartDate).collect(Collectors.toList()));

    Calendar calendar = PeriodType.getCalendar();

    for (Date day : days) {
      List<Object> values = new ArrayList<>();

      final int year = PeriodType.getCalendar().fromIso(day).getYear();

      values.add(day);
      values.add(year);

      for (PeriodType periodType : periodTypes) {
        values.add(periodType.createPeriod(day, calendar).getIsoDate());
      }

      batchArgs.add(values.toArray());
    }

    return Optional.of(batchArgs);
  }

  @Override
  public List<String> getCreateIndexStatements() {
    List<String> indexes = new ArrayList<>();

    for (PeriodType periodType : PeriodType.PERIOD_TYPES) {
      String colName = periodType.getName().toLowerCase();
      String indexName = "in" + getTableName() + "_" + colName + "_" + getRandomSuffix();
      String sql =
          "create index " + indexName + " on " + getTempTableName() + "(" + quote(colName) + ")";
      indexes.add(sql);
    }

    return indexes;
  }
}
