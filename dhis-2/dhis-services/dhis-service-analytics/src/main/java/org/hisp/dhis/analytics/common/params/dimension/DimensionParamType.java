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
package org.hisp.dhis.analytics.common.params.dimension;

import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.fromFullDimensionId;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.common.AnalyticsDateFilter;

@Getter
@RequiredArgsConstructor
public enum DimensionParamType {
  DIMENSIONS(CommonRequestParams::getAllDimensions),
  FILTERS(CommonRequestParams::getFilter),
  HEADERS(CommonRequestParams::getHeaders),
  DATE_FILTERS(
      commonQueryRequest ->
          Arrays.stream(AnalyticsDateFilter.values())
              .map(analyticsDateFilter -> parseDate(commonQueryRequest, analyticsDateFilter))
              .filter(Objects::nonNull)
              .flatMap(Collection::stream)
              .collect(Collectors.toList())),

  /**
   * The function invoked on this enum, will return a collection made of:
   *
   * <ul>
   *   <li>commonQueryRequest.getAsc(), suffixed by ":asc"
   *   <li>commonQueryRequest.getDesc(), suffixed by ":desc"
   * </ul>
   */
  SORTING(
      commonQueryRequest ->
          Stream.concat(
                  commonQueryRequest.getAsc().stream().map(s -> s + ":" + ASC.getValue()),
                  commonQueryRequest.getDesc().stream().map(s -> s + ":" + DESC.getValue()))
              .collect(Collectors.toList()));

  private static List<String> parseDate(
      CommonRequestParams commonRequestParams, AnalyticsDateFilter analyticsDateFilter) {

    return SetUtils.emptyIfNull(
            analyticsDateFilter.getTrackedEntityExtractor().apply(commonRequestParams))
        .stream()
        .filter(StringUtils::isNotEmpty)
        .map(df -> df.split(";"))
        .flatMap(Arrays::stream)
        .map(dateFilterItem -> toDimensionParam(dateFilterItem, analyticsDateFilter))
        .toList();
  }

  /**
   * Transforms the given "dateItemFilter" into the default internal format for "pe" dimensions
   * based on the {@link AnalyticsDateFilter} provided.
   *
   * @param dateItemFilter the date item filter in the format "programUid.programStageUid.period"
   * @param analyticsDateFilter the {@link AnalyticsDateFilter}.
   * @return the string in the format "programUid.programStageUid.pe:period:analyticsDateFilter"
   */
  private static String toDimensionParam(
      String dateItemFilter, AnalyticsDateFilter analyticsDateFilter) {
    // Parsing the "programUid.programStageUid.period" to
    // programUid.programStageUid.pe:period:analyticsDateFilter.
    StringDimensionIdentifier parsedItem = fromFullDimensionId(dateItemFilter);

    String period = parsedItem.getDimension().getUid();

    StringDimensionIdentifier dimensionIdentifier =
        StringDimensionIdentifier.of(
            parsedItem.getProgram(), parsedItem.getProgramStage(), StringUid.of(PERIOD_DIM_ID));

    return String.join(":", dimensionIdentifier.toString(), period, analyticsDateFilter.name());
  }

  /**
   * Getter method that retrieves the dimensions or filters from the {@link CommonRequestParams}.
   */
  private final Function<CommonRequestParams, Collection<String>> uidsGetter;
}
