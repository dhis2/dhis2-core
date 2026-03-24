/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import java.util.Optional;
import java.util.regex.Pattern;
import org.hisp.dhis.period.PeriodTypeEnum;

public class PostgreSqlAnalyticsSqlBuilder extends PostgreSqlBuilder
    implements AnalyticsSqlBuilder {
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /**
   * Returns a subquery that expand the event datavalue jsonb with two additional fields:
   *
   * <ul>
   *   <li>value_name: the name of the organisation unit that the datavalue is associated with
   *   <li>value_code: the code of the organisation unit that the datavalue is associated with
   * </ul>
   *
   * @return a SQL subquery.
   */
  @Override
  public String getEventDataValues() {
    return """
        (select json_object_agg(l2.keys, l2.datavalue) as value
        from (
            select l1.uid,
            l1.keys,
            json_strip_nulls(json_build_object(
            'value', l1.eventdatavalues -> l1.keys ->> 'value',
            'created', l1.eventdatavalues -> l1.keys ->> 'created',
            'storedBy', l1.eventdatavalues -> l1.keys ->> 'storedBy',
            'lastUpdated', l1.eventdatavalues -> l1.keys ->> 'lastUpdated',
            'providedElsewhere', l1.eventdatavalues -> l1.keys -> 'providedElsewhere',
            'value_name', (select ou.name
                from organisationunit ou
                where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'),
            'value_code', (select ou.code
                from organisationunit ou
                where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'))) as datavalue
            from (select inner_evt.*, jsonb_object_keys(inner_evt.eventdatavalues) keys
            from trackerevent inner_evt) as l1) as l2
        where l2.uid = ev.uid
        group by l2.uid)::jsonb
        """;
  }

  @Override
  public String renderTimestamp(String timestampAsString) {
    return timestampAsString;
  }

  @Override
  public Optional<String> renderDateFieldPeriodBucketDate(
      String dateColumn, PeriodTypeEnum periodType) {
    return Optional.ofNullable(
        switch (periodType) {
          case DAILY -> dateColumn + "::date";
          case YEARLY -> "date_trunc('year', " + dateColumn + ")::date";
          case WEEKLY -> "date_trunc('week', " + dateColumn + ")::date";
          case WEEKLY_WEDNESDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '5 days')::date - interval '5 days'";
          case WEEKLY_THURSDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '4 days')::date - interval '4 days'";
          case WEEKLY_FRIDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '3 days')::date - interval '3 days'";
          case WEEKLY_SATURDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '2 days')::date - interval '2 days'";
          case WEEKLY_SUNDAY ->
              "date_trunc('week', " + dateColumn + " + interval '1 day')::date - interval '1 day'";
          case BI_WEEKLY ->
              "date_trunc('week', "
                  + dateColumn
                  + ")::date - ((extract(week from "
                  + dateColumn
                  + ")::int - 1) % 2) * interval '7 days'";
          case MONTHLY -> "date_trunc('month', " + dateColumn + ")::date";
          case QUARTERLY -> "date_trunc('quarter', " + dateColumn + ")::date";
          case QUARTERLY_NOV ->
              "date_trunc('quarter', "
                  + dateColumn
                  + " - interval '1 month')::date + interval '1 month'";
          case BI_MONTHLY ->
              collapseWhitespace(
                  """
                  make_date(
                    extract(year from %1$s)::int,
                    ((extract(month from %1$s)::int - 1) / 2) * 2 + 1,
                    1
                  )
                  """
                      .formatted(dateColumn));
          case SIX_MONTHLY ->
              collapseWhitespace(
                  """
                  make_date(
                    extract(year from %1$s)::int,
                    case when extract(month from %1$s) <= 6 then 1 else 7 end,
                    1
                  )
                  """
                      .formatted(dateColumn));
          case SIX_MONTHLY_APRIL ->
              collapseWhitespace(
                  """
                  case
                    when extract(month from %1$s) between 4 and 9
                      then make_date(extract(year from %1$s)::int, 4, 1)
                    when extract(month from %1$s) >= 10
                      then make_date(extract(year from %1$s)::int, 10, 1)
                    else make_date(extract(year from %1$s)::int - 1, 10, 1)
                  end
                  """
                      .formatted(dateColumn));
          case SIX_MONTHLY_NOV ->
              collapseWhitespace(
                  """
                  case
                    when extract(month from %1$s) between 5 and 10
                      then make_date(extract(year from %1$s)::int, 5, 1)
                    when extract(month from %1$s) >= 11
                      then make_date(extract(year from %1$s)::int, 11, 1)
                    else make_date(extract(year from %1$s)::int - 1, 11, 1)
                  end
                  """
                      .formatted(dateColumn));
          case FINANCIAL_FEB -> renderFinancialYearStart(dateColumn, 2);
          case FINANCIAL_APRIL -> renderFinancialYearStart(dateColumn, 4);
          case FINANCIAL_JULY -> renderFinancialYearStart(dateColumn, 7);
          case FINANCIAL_AUG -> renderFinancialYearStart(dateColumn, 8);
          case FINANCIAL_SEP -> renderFinancialYearStart(dateColumn, 9);
          case FINANCIAL_OCT -> renderFinancialYearStart(dateColumn, 10);
          case FINANCIAL_NOV -> renderFinancialYearStart(dateColumn, 11);
          default -> null;
        });
  }

  private String renderFinancialYearStart(String dateColumn, int startMonth) {
    return collapseWhitespace(
        """
        make_date(
          case when extract(month from %1$s) >= %2$d
               then extract(year from %1$s)::int
               else extract(year from %1$s)::int - 1
          end, %2$d, 1
        )
        """
            .formatted(dateColumn, startMonth));
  }

  private static String collapseWhitespace(String input) {
    return WHITESPACE.matcher(input).replaceAll(" ").trim();
  }
}
