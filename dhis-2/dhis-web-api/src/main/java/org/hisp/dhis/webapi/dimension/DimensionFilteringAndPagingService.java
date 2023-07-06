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
package org.hisp.dhis.webapi.dimension;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DimensionFilteringAndPagingService {

  @NonNull private final FieldFilterService fieldFilterService;

  private static final Comparator<DimensionResponse> DEFAULT_COMPARATOR =
      comparing(DimensionResponse::getCreated, nullsFirst(naturalOrder()));

  private static final Map<String, Comparator<DimensionResponse>> ORDERING_MAP =
      Map.of(
          "lastUpdated", comparing(DimensionResponse::getLastUpdated, nullsFirst(naturalOrder())),
          "code", comparing(DimensionResponse::getCode, nullsFirst(naturalOrder())),
          "uid", comparing(DimensionResponse::getUid, nullsFirst(naturalOrder())),
          "id", comparing(DimensionResponse::getId, nullsFirst(naturalOrder())),
          "name", comparing(DimensionResponse::getName, nullsFirst(naturalOrder())),
          "dimensionType",
              comparing(DimensionResponse::getDimensionType, nullsFirst(naturalOrder())),
          "displayShortName",
              comparing(DimensionResponse::getDisplayShortName, nullsFirst(naturalOrder())),
          "displayName", comparing(DimensionResponse::getDisplayName, nullsFirst(naturalOrder())));

  public AnalyticsDimensionsPagingWrapper<ObjectNode> pageAndFilter(
      Collection<DimensionResponse> dimensionResponses,
      DimensionsCriteria dimensionsCriteria,
      List<String> fields) {
    AnalyticsDimensionsPagingWrapper<ObjectNode> pagingWrapper =
        new AnalyticsDimensionsPagingWrapper<>();

    List<DimensionResponse> filteredDimensions =
        filterStream(dimensionResponses.stream(), dimensionsCriteria).collect(Collectors.toList());

    FieldFilterParams<DimensionResponse> filterParams =
        FieldFilterParams.of(
            sortedAndPagedStream(filteredDimensions.stream(), dimensionsCriteria)
                .collect(Collectors.toList()),
            fields);

    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(filterParams);

    pagingWrapper.setDimensions(objectNodes);

    if (dimensionsCriteria.isPaging()) {
      pagingWrapper.setPager(
          new Pager(
              Optional.ofNullable(dimensionsCriteria.getPage()).orElse(1),
              filteredDimensions.size(),
              dimensionsCriteria.getPageSize()));
    }

    return pagingWrapper;
  }

  private Stream<DimensionResponse> filterStream(
      Stream<DimensionResponse> dimensions, DimensionsCriteria criteria) {
    DimensionFilters dimensionFilters =
        Optional.of(criteria)
            .map(DimensionsCriteria::getFilter)
            .map(DimensionFilters::of)
            .orElse(DimensionFilters.EMPTY_DATA_DIMENSION_FILTER);

    return dimensions.filter(dimensionFilters);
  }

  private Stream<DimensionResponse> sortedAndPagedStream(
      Stream<DimensionResponse> dimensions, DimensionsCriteria pagingAndSortingCriteria) {
    if (Objects.nonNull(pagingAndSortingCriteria.getOrder())
        && !pagingAndSortingCriteria.getOrder().isEmpty()) {

      OrderCriteria orderCriteria = pagingAndSortingCriteria.getOrder().get(0);

      Comparator<DimensionResponse> comparator =
          ORDERING_MAP.keySet().stream()
              .filter(key -> key.equalsIgnoreCase(orderCriteria.getField()))
              .map(ORDERING_MAP::get)
              .findFirst()
              .orElse(DEFAULT_COMPARATOR);

      if (Objects.nonNull(orderCriteria.getDirection())
          && !orderCriteria.getDirection().isAscending()) {
        dimensions = dimensions.sorted(comparator.reversed());
      } else {
        dimensions = dimensions.sorted(comparator);
      }
    } else {
      dimensions = dimensions.sorted(DEFAULT_COMPARATOR);
    }

    if (pagingAndSortingCriteria.isPaging()) {
      dimensions =
          dimensions
              .skip(pagingAndSortingCriteria.getFirstResult())
              .limit(pagingAndSortingCriteria.getPageSize());
    }
    return dimensions;
  }
}
