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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
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
public class Period extends BaseDimensionalItemObject {
  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

  /**
   * Check if a date is within the date range as provided by a period.
   *
   * @param start inclusive, null is open to any time before end
   * @param end inclusive, null is open to any time after start
   * @param checked the date checked, maybe null
   * @return true, if the checked date is non-null and is between start and end date (ignoring time
   *     both ends inclusive)
   */
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

  /**
   * Check if a date is within the date range as provided by a period.
   *
   * @param start inclusive, null is open to any time before end
   * @param end inclusive, null is open to any time after start
   * @param checked the date checked, maybe null
   * @return true, if the checked date is non-null and is between start and end date. Exact times
   *     are considered.
   */
  public static boolean isDateWithTimeInTimeFrame(
      @CheckForNull Date start, @CheckForNull Date end, @CheckForNull Date checked) {
    if (checked == null) {
      return false;
    }
    return (start == null || !checked.before(start)) && (end == null || !checked.after(end));
  }

  /** Required. */
  private PeriodType periodType;

  /** Required. Must be unique together with endDate. */
  private Date startDate;

  /** Required. Must be unique together with startDate. */
  private Date endDate;

  /** Transient string holding the ISO representation of the period. */
  private transient String isoPeriod;

  /** date field this period refers to */
  @Getter @Setter private String dateField;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Period() {}

  /**
   * Creates a period that is not bound to the persistent layer. It represents a detached Period
   * that is mainly used for displaying purposes.
   *
   * @param isoRelativePeriod the ISO relative period
   */
  public Period(RelativePeriodEnum isoRelativePeriod) {
    this.isoPeriod = isoRelativePeriod.toString();
    this.name = isoRelativePeriod.toString();
    this.code = isoRelativePeriod.toString();
    this.setStartDate(new Date());
    this.setEndDate(new Date());
  }

  public Period(Period period) {
    this.id = period.getId();
    this.periodType = period.getPeriodType();
    this.startDate = period.getStartDate();
    this.endDate = period.getEndDate();
    this.name = period.getName();
    this.isoPeriod = period.getIsoDate();
    this.dateField = period.getDateField();
  }

  protected Period(PeriodType periodType, Date startDate, Date endDate) {
    this.periodType = periodType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isoPeriod = periodType.getIsoDate(this);
  }

  protected Period(PeriodType periodType, Date startDate, Date endDate, String isoPeriod) {
    this.periodType = periodType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isoPeriod = isoPeriod;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @Override
  public void setAutoFields() {}

  @Override
  public String getDimensionItem() {
    return getIsoDate();
  }

  @Override
  public String getUid() {
    return uid != null ? uid : getIsoDate();
  }

  public String getRealUid() {
    return uid;
  }

  @Override
  public String getCode() {
    return getIsoDate();
  }

  @Override
  public String getName() {
    return name != null ? name : getIsoDate();
  }

  @Override
  public String getShortName() {
    return shortName != null ? shortName : getIsoDate();
  }

  /**
   * Returns an ISO8601 formatted string version of the period.
   *
   * @return the period string
   */
  public String getIsoDate() {
    return isoPeriod != null ? isoPeriod : getPeriodTypeIsoDate();
  }

  /**
   * It returns the ISO date for the current periodType of "this" object.
   *
   * @return the ISO date.
   */
  private String getPeriodTypeIsoDate() {
    if (periodType != null) {
      return periodType.getIsoDate(this);
    }

    return "";
  }

  /**
   * Copies the transient properties (name) from the argument Period to this Period.
   *
   * @param other Period to copy from.
   * @return this Period.
   */
  public Period copyTransientProperties(Period other) {
    this.name = other.getName();
    this.shortName = other.getShortName();
    this.code = other.getCode();

    return this;
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

  /** Returns a unique key suitable for caching and lookups. */
  public String getCacheKey() {
    return periodType.getName() + "-" + startDate.toString() + "-" + endDate.toString();
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.PERIOD;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;

    result = result * prime + (startDate != null ? startDate.hashCode() : 0);
    result = result * prime + (endDate != null ? endDate.hashCode() : 0);
    result = result * prime + (getCode() != null ? getCode().hashCode() : 0);
    result = result * prime + (periodType != null ? periodType.hashCode() : 0);

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof Period period && objectEquals(period));
  }

  private boolean objectEquals(Period other) {
    return startDate.equals(other.getStartDate())
        && endDate.equals(other.getEndDate())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getIsoDate(), other.getIsoDate())
        && Objects.equals(periodType, other.periodType)
        && Objects.equals(dateField, other.getDateField());
  }

  @Override
  public String toString() {
    return getIsoDate() + (isDefault() ? "" : ":" + dateField);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  @JsonProperty
  @JsonSerialize(using = JacksonPeriodTypeSerializer.class)
  @JsonDeserialize(using = JacksonPeriodTypeDeserializer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.TEXT)
  public PeriodType getPeriodType() {
    return periodType;
  }

  public void setPeriodType(PeriodType periodType) {
    this.periodType = periodType;
  }

  public boolean isDefault() {
    return Objects.isNull(dateField);
  }
}
