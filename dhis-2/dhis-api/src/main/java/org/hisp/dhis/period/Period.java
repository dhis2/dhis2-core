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
package org.hisp.dhis.period;

import static java.util.Objects.requireNonNull;
import static org.hisp.dhis.period.PeriodTypeEnum.BI_MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.BI_WEEKLY;
import static org.hisp.dhis.period.PeriodTypeEnum.DAILY;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_APRIL;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_JULY;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_NOV;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_OCT;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_SEP;
import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY_NOV;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY_APRIL;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY_NOV;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_SATURDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_SUNDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_THURSDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_WEDNESDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.YEARLY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.joda.time.DateTime;
import org.joda.time.Days;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement(localName = "period", namespace = DxfNamespaces.DXF_2_0)
public class Period implements Serializable {
  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

  /**
   * Returns a period based on the given date string in ISO format. Returns null if the date string
   * cannot be parsed to a period.
   *
   * @param isoPeriod the date string in ISO format.
   * @return a period object for the input if it is valid
   * @throws IllegalArgumentException if the given ISO period is (formally or semantically) invalid
   * @implNote This got moved from {@code PeriodType#getPeriodFromIsoString}
   */
  @Nonnull
  public static Period of(@Nonnull String isoPeriod) {
    PeriodType type = PeriodType.getPeriodTypeFromIsoString(isoPeriod);
    try {
      Period p = type.createPeriod(isoPeriod);
      if (p == null)
        throw new IllegalArgumentException(
            "Invalid Period `%s`, failed to create date interval".formatted(isoPeriod));
      return p;
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Invalid Period `%s`, failed to create date interval".formatted(isoPeriod), ex);
    }
  }

  /**
   * A version of {@link #of(String)} for backwards compatibility where the old semantics of null
   * being allowed as input and being the output for illegal periods is required.
   *
   * @param isoPeriod the date string in ISO format.
   * @return a period or null in case the given {@link String} was not a valid period
   */
  @CheckForNull
  public static Period ofNullable(@CheckForNull String isoPeriod) {
    if (isoPeriod == null || isoPeriod.isEmpty()) return null;
    try {
      return of(isoPeriod);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Check if a date is within the date range as provided by a period.
   *
   * @param start inclusive, null is open to any time before end
   * @param end inclusive, null is open to any time after start
   * @param checked the date checked, maybe null
   * @return true, if the checked date is non-null and is between start and end date (ignoring time
   *     both ends inclusive)
   */
  public static boolean isDateInTimeFrame(
      @CheckForNull Date start, @CheckForNull Date end, @CheckForNull Date checked) {
    if (checked == null) {
      return false;
    }
    ZoneId zoneId = ZoneId.systemDefault();
    Function<Date, ZonedDateTime> toZonedDate =
        date -> ZonedDateTime.ofInstant(date.toInstant(), zoneId).with(LocalTime.MIN);
    ZonedDateTime from = start == null ? null : toZonedDate.apply(start);
    ZonedDateTime to = end == null ? null : toZonedDate.apply(end);
    ZonedDateTime sample = toZonedDate.apply(checked);
    return (from == null || !sample.isBefore(from)) && (to == null || !sample.isAfter(to));
  }

  @Getter @Setter private long id;
  @Setter private PeriodType periodType;
  @Setter private Date startDate;
  @Setter private Date endDate;
  private String isoPeriod;

  public Period() {}

  public Period(PeriodType periodType, Date startDate, Date endDate) {
    this.periodType = periodType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isoPeriod = periodType.getIsoDate(this);
  }

  public Period(PeriodType periodType, Date startDate, Date endDate, String isoPeriod) {
    this.periodType = periodType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isoPeriod = isoPeriod;
  }

  public Period next() {
    return periodType.getNextPeriod(this);
  }

  /**
   * Returns an ISO8601 formatted string version of the period.
   *
   * @return the period string
   */
  @Nonnull
  public String getIsoDate() {
    return isoPeriod != null ? isoPeriod : getPeriodTypeIsoDate();
  }

  public void setIsoDate(String iso) {
    // NOOP, just here for hibernate
  }

  /**
   * It returns the ISO date for the current periodType of "this" object.
   *
   * @return the ISO date.
   */
  @Nonnull
  private String getPeriodTypeIsoDate() {
    if (periodType != null) {
      return periodType.getIsoDate(this);
    }

    return "";
  }

  /**
   * Returns the frequency order of the period type of the period.
   *
   * @return the frequency order.
   */
  public int frequencyOrder() {
    return periodType != null ? periodType.getFrequencyOrder() : YearlyPeriodType.FREQUENCY_ORDER;
  }

  /**
   * Returns start date formatted as string.
   *
   * @return start date formatted as string.
   */
  public String getStartDateString() {
    return getMediumDateString(startDate);
  }

  /**
   * Returns end date formatted as string.
   *
   * @return end date formatted as string.
   */
  public String getEndDateString() {
    return getMediumDateString(endDate);
  }

  /**
   * Formats a Date to the format YYYY-MM-DD.
   *
   * @param date the Date to parse.
   * @return A formatted date string. Null if argument is null.
   */
  private String getMediumDateString(Date date) {
    final SimpleDateFormat format = new SimpleDateFormat();

    format.applyPattern(DEFAULT_DATE_FORMAT);

    return date != null ? format.format(date) : null;
  }

  /**
   * Returns the potential number of periods of the given period type which is spanned by this
   * period.
   *
   * @param type the period type.
   * @return the potential number of periods of the given period type spanned by this period.
   */
  public int getPeriodSpan(PeriodType type) {
    double no = (double) this.periodType.getFrequencyOrder() / type.getFrequencyOrder();

    return (int) Math.floor(no);
  }

  /**
   * Returns the number of days in the period, i.e. the days between the start and end date.
   *
   * @return number of days in period.
   */
  public int getDaysInPeriod() {
    Days days = Days.daysBetween(new DateTime(startDate), new DateTime(endDate));
    return days.getDays() + 1;
  }

  /** Validates this period. TODO Make more comprehensive. */
  public boolean isValid() {
    if (startDate == null || endDate == null || periodType == null) {
      return false;
    }

    if (PeriodTypeEnum.DAILY != periodType.getPeriodTypeEnum() && getDaysInPeriod() < 2) {
      return false;
    }

    return true;
  }

  /**
   * Indicates whether this period is after the given period. Bases the comparison on the end dates
   * of the periods. If the given period is null, false is returned.
   *
   * @param period the period to compare.
   * @return true if this period is after the given period.
   */
  public boolean isAfter(Period period) {
    if (period == null || period.getEndDate() == null) {
      return false;
    }

    return getEndDate().after(period.getEndDate());
  }

  @Override
  public int hashCode() {
    return getIsoDate().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof Period other && getIsoDate().equals(other.getIsoDate()));
  }

  @Override
  public String toString() {
    return getIsoDate();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getStartDate() {
    return startDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getEndDate() {
    return endDate;
  }

  @JsonProperty
  @JsonSerialize(using = JacksonPeriodTypeSerializer.class)
  @JsonDeserialize(using = JacksonPeriodTypeDeserializer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.TEXT)
  public PeriodType getPeriodType() {
    return periodType;
  }

  /**
   * A {@link Period} in its raw components based on what a user might have entered. This means, the
   * numbers are numerically valid but not necessarily semantically within a calendar system.
   *
   * @param type of the period entered
   * @param year year component
   * @param primaryInterval the 1st level interval within a year (for all but yearly types)
   * @param day day component (only for {@link PeriodTypeEnum#DAILY})
   */
  public record Input(
      @Nonnull PeriodTypeEnum type, int year, Integer primaryInterval, Integer day) {

    public Input {
      requireNonNull(type);
    }

    private Input(@Nonnull PeriodTypeEnum type, int year) {
      this(type, year, null, null);
    }

    private Input(@Nonnull PeriodTypeEnum type, int year, int secondary) {
      this(type, year, secondary, null);
    }

    /**
     * Parses a given ISO period {@link String} into its type and number components.
     *
     * <pre>
     * Daily: ~~~~~~~~ (8) or ~~~~-~~-~~ (10)
     * Weekly: ~~~~W~ (6) or ~~~~W~~ (7)
     * Weekly Wednesday: ~~~~WedW~ (9) or ~~~~WedW~~ (10)
     * Weekly Thursday: ~~~~ThuW~ (9) or ~~~~ThuW~~ (10)
     * Weekly Saturday: ~~~~SatW~ (9) or ~~~~SatW~~ (10)
     * Weekly Sunday: ~~~~SunW~ (9) or ~~~~SunW~~ (10)
     * Bi-weekly: ~~~~BiW~ (8) or ~~~~BiW~~ (9)
     * Monthly: ~~~~~~ (6) or ~~~~-~~ (7)
     * Bi-monthly: ~~~~~~B (7)
     * Quarterly: ~~~~Q~ (6)
     * Quarterly November: ~~~~NovQ~ (9)
     * Six-monthly: ~~~~S~ (6)
     * Six-monthly April: ~~~~AprilS~ (11)
     * Six-monthly November: ~~~~NovS~ (9)
     * Yearly: ~~~~ (4)
     * Yearly April: ~~~~April (9)
     * Yearly July: ~~~~July (8)
     * Yearly September: ~~~~Sep (7)
     * Yearly October: ~~~~Oct (7)
     * Yearly November: ~~~~Nov (7)
     * </pre>
     *
     * @param isoPeriod an ISO string
     * @return the period for the given string
     * @throws IllegalArgumentException if the given string cannot be parsed to a period
     */
    @Nonnull
    public static Period.Input of(@Nonnull String isoPeriod) {
      String p = isoPeriod;
      int len = p.length();
      if (len < 4 || len > 11)
        throw new IllegalArgumentException(
            "Period must be have between 4 and 11 characters but got: `%s`".formatted(p));
      int year = digits4(p, 0);
      if (len == 4) {
        return new Input(YEARLY, year);
      }
      if (len == 6) {
        int primary = digits1(p, 5);
        char c4 = p.charAt(4);
        if (c4 == 'W') return new Input(WEEKLY, year, primary);
        if (isDigit(c4)) return new Input(MONTHLY, year, digits2(p, 4));
        if (c4 == 'Q') return new Input(QUARTERLY, year, primary);
        if (c4 == 'S') return new Input(SIX_MONTHLY, year, primary);
        throw illegalInfix(p, "W", "Q", "S");
      }
      if (len == 7) {
        if (p.endsWith("Sep")) return new Input(FINANCIAL_SEP, year);
        if (p.endsWith("Oct")) return new Input(FINANCIAL_OCT, year);
        if (p.endsWith("Nov")) return new Input(FINANCIAL_NOV, year);
        if (p.endsWith("B")) return new Input(BI_MONTHLY, year, digits2(p, 4));
        int primary = digits2(p, 5);
        char c4 = p.charAt(4);
        if (c4 == 'W') return new Input(WEEKLY, year, primary);
        if (c4 == '-') return new Input(MONTHLY, year, primary);
        throw illegalInfix(p, "W", "-");
      }
      if (len == 8) {
        if (p.endsWith("July")) return new Input(FINANCIAL_JULY, year);
        if (p.indexOf("BiW") == 4) return new Input(BI_WEEKLY, year, digits1(p, 7));
        return new Input(DAILY, year, digits2(p, 4), digits2(p, 6));
      }
      if (len == 9) {
        if (p.endsWith("April")) return new Input(FINANCIAL_APRIL, year);
        if (p.indexOf("NovQ") == 4) return new Input(QUARTERLY_NOV, year, digits1(p, 8));
        if (p.indexOf("BiW") == 4) return new Input(BI_WEEKLY, year, digits2(p, 7));
        int primary = digits1(p, 8);
        if (p.indexOf("WedW") == 4) return new Input(WEEKLY_WEDNESDAY, year, primary);
        if (p.indexOf("ThuW") == 4) return new Input(WEEKLY_THURSDAY, year, primary);
        if (p.indexOf("SatW") == 4) return new Input(WEEKLY_SATURDAY, year, primary);
        if (p.indexOf("SunW") == 4) return new Input(WEEKLY_SUNDAY, year, primary);
        if (p.indexOf("NovS") == 4) return new Input(SIX_MONTHLY_NOV, year, primary);
        throw illegalInfix(p, "WedW", "ThuW", "SatW", "SunW", "NovS");
      }
      if (len == 10) {
        if (p.charAt(4) == '-') {
          if (p.charAt(7) != '-') throw illegalChar(p, "dash", 7);
          return new Input(DAILY, year, digits2(p, 5), digits2(p, 8));
        }
        int primary = digits2(p, 8);
        if (p.indexOf("WedW") == 4) return new Input(WEEKLY_WEDNESDAY, year, primary);
        if (p.indexOf("ThuW") == 4) return new Input(WEEKLY_THURSDAY, year, primary);
        if (p.indexOf("SatW") == 4) return new Input(WEEKLY_SATURDAY, year, primary);
        if (p.indexOf("SunW") == 4) return new Input(WEEKLY_SUNDAY, year, primary);
        throw illegalInfix(p, "WedW", "ThuW", "SatW", "SunW");
      }
      // if (len == 11)
      if (p.indexOf("AprilS") == 4) return new Input(SIX_MONTHLY_APRIL, year, digits1(p, 10));
      throw illegalInfix(p, "AprilS");
    }

    private static boolean isDigit(char c) {
      return c >= '0' && c <= '9';
    }

    private static void checkDigits(String period, int from, int to) {
      for (int i = from; i <= to; i++) checkDigit(period, i);
    }

    private static void checkDigit(String period, int index) {
      if (!isDigit(period.charAt(index))) throw illegalChar(period, "digit", index);
    }

    private static IllegalArgumentException illegalChar(String period, String expected, int index) {
      return new IllegalArgumentException(
          "Invalid Period `%s`, expected a %s at position %d but found: `%s`"
              .formatted(period, expected, index, period.charAt(index)));
    }

    private static IllegalArgumentException illegalInfix(String period, String... oneOf) {
      return new IllegalArgumentException(
          "Invalid Period `%s`, expected one of %s at index %d for a ISO period of length %d but found: `%s`"
              .formatted(period, List.of(oneOf), 4, period.length(), period.substring(4)));
    }

    private static int digits1(String p, int index) {
      checkDigit(p, index);
      return p.charAt(index) - '0';
    }

    private static int digits2(String p, int index) {
      checkDigits(p, index, index + 1);
      char d1 = p.charAt(index + 1);
      char d2 = p.charAt(index);
      // Main reason to not use parseInt is to avoid exception handling
      return 10 * (d2 - '0') + (d1 - '0');
    }

    private static int digits4(String p, int index) {
      checkDigits(p, index, index + 3);
      char d1 = p.charAt(index + 3);
      char d2 = p.charAt(index + 2);
      char d3 = p.charAt(index + 1);
      char d4 = p.charAt(index);
      // Main reason to not use parseInt is to avoid exception handling
      return 1000 * (d4 - '0') + 100 * (d3 - '0') + 10 * (d2 - '0') + (d1 - '0');
    }
  }
}
