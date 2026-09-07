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
package org.hisp.dhis.analytics.event.data.aggregate;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.PeriodDimensionSplitter;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.DimensionalObject;

/**
 * Resolves aggregate enrollment static date headers to their underlying SQL source columns.
 *
 * <p>This resolver centralizes two related decisions used by the aggregate enrollment CTE:
 *
 * <ul>
 *   <li>When a visible header such as {@code incidentdate} or {@code completed} should be projected
 *       from a different enrollment analytics column (for example {@code occurreddate} or {@code
 *       completeddate}).
 *   <li>When a static date header is already satisfied by the active period dimension and should
 *       therefore not be added again as an extra aggregate column.
 * </ul>
 *
 * <p>{@code eventdate} remains a special case because it can be backed by the distinct-enrollment
 * event join rather than the enrollment analytics table.
 */
public final class AggregatedEnrollmentDateHeaderResolver {
  public static final String EVENT_DATE_HEADER = "eventdate";
  public static final String SCHEDULED_DATE_HEADER = "scheduleddate";
  public static final String EVENT_DATE_JOIN_ALIAS = "evf";
  public static final String EVENT_DATE_JOIN_COLUMN = "event_occurreddate";

  /**
   * Resolves a base CTE projection for the given header if the header should not be projected as a
   * raw enrollment table column.
   *
   * @param headerName normalized header key
   * @param usesAggregateEventJoin whether the base CTE uses the distinct-enrollment event join
   * @return an optional projection descriptor when a remapped projection is needed
   */
  public Optional<BaseAggregationHeaderProjection> resolveBaseProjection(
      String headerName, boolean usesAggregateEventJoin) {
    if (usesAggregateEventJoin && EVENT_DATE_HEADER.equalsIgnoreCase(headerName)) {
      return Optional.of(
          new BaseAggregationHeaderProjection(
              EVENT_DATE_JOIN_COLUMN, EVENT_DATE_JOIN_ALIAS, headerName));
    }

    return resolveEnrollmentStaticHeaderSourceColumn(headerName)
        .filter(sourceColumn -> !sourceColumn.equalsIgnoreCase(headerName))
        .map(sourceColumn -> new BaseAggregationHeaderProjection(sourceColumn, "ax", headerName));
  }

  /**
   * Indicates whether the given header corresponds to the current static date period dimension and
   * is therefore already represented by the derived period bucket.
   *
   * @param params current aggregate enrollment query parameters
   * @param headerName normalized header key
   * @return {@code true} when the header maps to the active static date period dimension
   */
  public boolean isDerivedStaticPeriodHeader(EventQueryParams params, String headerName) {
    String normalizedHeader = normalizeHeaderKey(headerName);
    return getStaticPeriodKeys(params).stream()
        .anyMatch(name -> name.equalsIgnoreCase(normalizedHeader));
  }

  /**
   * Normalizes a header key by stripping SQL quoting characters.
   *
   * @param headerName raw header key
   * @return the normalized key
   */
  public String normalizeHeaderKey(String headerName) {
    return headerName.replace("\"", "").replace("`", "");
  }

  private Set<String> getStaticPeriodKeys(EventQueryParams params) {
    DimensionalObject periodDimension = params.getDimension("pe");
    if (periodDimension == null || periodDimension.getItems().isEmpty()) {
      return Set.of();
    }

    return periodDimension.getItems().stream()
        .filter(org.hisp.dhis.period.PeriodDimension.class::isInstance)
        .map(org.hisp.dhis.period.PeriodDimension.class::cast)
        .map(org.hisp.dhis.period.PeriodDimension::getDateField)
        .filter(Objects::nonNull)
        .map(PeriodDimensionSplitter::toDateFieldKey)
        .collect(Collectors.toSet());
  }

  private Optional<String> resolveEnrollmentStaticHeaderSourceColumn(String headerName) {
    if (EVENT_DATE_HEADER.equalsIgnoreCase(headerName)
        || SCHEDULED_DATE_HEADER.equalsIgnoreCase(headerName)) {
      return Optional.empty();
    }

    String normalizedHeader = normalizeHeaderKey(headerName);

    return Stream.concat(
            Arrays.stream(AnalyticsDateFilter.values())
                .map(
                    dateFilter ->
                        new AbstractMap.SimpleEntry<>(
                            toDateFieldKey(dateFilter.name()),
                            dateFilter.getTimeField().getEnrollmentColumnName())),
            Arrays.stream(TimeField.values())
                .map(
                    timeField ->
                        new AbstractMap.SimpleEntry<>(
                            toDateFieldKey(timeField.name()), timeField.getEnrollmentColumnName())))
        .filter(mapping -> mapping.getKey().equalsIgnoreCase(normalizedHeader))
        .map(AbstractMap.SimpleEntry::getValue)
        .filter(StringUtils::isNotBlank)
        .findFirst();
  }

  private String toDateFieldKey(String dateField) {
    return dateField.toLowerCase().replace("_", "");
  }

  /** Describes a remapped base CTE projection for an aggregate enrollment header. */
  public record BaseAggregationHeaderProjection(
      String sourceColumn, String tableAlias, String alias) {}
}
