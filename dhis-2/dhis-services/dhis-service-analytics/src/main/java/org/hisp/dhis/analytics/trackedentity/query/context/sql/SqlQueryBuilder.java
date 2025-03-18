/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.sql;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;

/** Provides the required methods to build {@link RenderableSqlQuery} objects. */
public interface SqlQueryBuilder {
  @Nonnull
  /**
   * Builds a {@link RenderableSqlQuery} based on the given arguments.
   *
   * @param queryContext the {@link QueryContext}.
   * @param acceptedHeaders the list of {@link DimensionIdentifier}.
   * @param acceptedDimensions the list of {@link DimensionIdentifier}.
   * @param acceptedSortingParams the list of {@link AnalyticsSortingParams}.
   */
  RenderableSqlQuery buildSqlQuery(
      @Nonnull QueryContext queryContext,
      @Nonnull List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      @Nonnull List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      @Nonnull List<AnalyticsSortingParams> acceptedSortingParams);

  /**
   * Provides the list of {@link Predicate} functions for {@link DimensionIdentifier} (headers).
   * They act as filters and are used to build the final {@link RenderableSqlQuery} query. By
   * default, it returns the same as {@link #getDimensionFilters()}.
   *
   * @return the list of filter dimensions or empty.
   */
  @Nonnull
  default List<Predicate<DimensionIdentifier<DimensionParam>>> getHeaderFilters() {
    return getDimensionFilters();
  }

  /**
   * Provides the list of {@link Predicate} functions for {@link DimensionIdentifier}. They act as
   * filters and are used to build the final {@link RenderableSqlQuery} query.
   *
   * @return the list of filter dimensions or empty.
   */
  @Nonnull
  default List<Predicate<DimensionIdentifier<DimensionParam>>> getDimensionFilters() {
    return List.of(unused -> false);
  }

  /**
   * Provides the list of {@link Predicate} functions for {@link DimensionIdentifier}. They are used
   * for sorting and are part of the final {@link RenderableSqlQuery} query.
   *
   * @return the list of sorting dimensions or empty.
   */
  @Nonnull
  default List<Predicate<AnalyticsSortingParams>> getSortingFilters() {
    return List.of(unused -> false);
  }

  default boolean alwaysRun() {
    return false;
  }

  default Stream<DimensionIdentifier<DimensionParam>> streamDimensions(
      List<DimensionIdentifier<DimensionParam>> headers,
      List<DimensionIdentifier<DimensionParam>> dimensions,
      List<AnalyticsSortingParams> sortingParams) {
    return Stream.of(
            headers.stream(),
            dimensions.stream(),
            sortingParams.stream().map(AnalyticsSortingParams::getOrderBy))
        .flatMap(Function.identity());
  }

  /**
   * Gets a grouped representation of the dimension identifiers, grouping them by key (the alias)
   *
   * @return the grouped dimensions
   */
  default GroupedDimensions getGroupedDimensions(
      List<DimensionIdentifier<DimensionParam>> headers,
      List<DimensionIdentifier<DimensionParam>> dimensions,
      List<AnalyticsSortingParams> sortingParams) {
    return GroupedDimensions.of(streamDimensions(headers, dimensions, sortingParams));
  }

  @Getter
  class GroupedDimensions {

    private final List<DimensionGroup> groupsByKey;

    private GroupedDimensions(Stream<DimensionIdentifier<DimensionParam>> dimensions) {
      groupsByKey =
          dimensions
              .collect(
                  groupingBy(
                      DimensionIdentifier::getKey,
                      // LinkedHashMap to keep the order
                      LinkedHashMap::new,
                      toList()))
              .entrySet()
              .stream()
              .map(entry -> new DimensionGroup(entry.getKey(), entry.getValue()))
              .toList();
    }

    public static GroupedDimensions of(
        Stream<DimensionIdentifier<DimensionParam>> dimensionIdentifierStream) {
      return new GroupedDimensions(dimensionIdentifierStream);
    }

    public Stream<DimensionIdentifier<DimensionParam>> streamOfFirstDimensionInEachGroup() {
      return groupsByKey.stream()
          .map(DimensionGroup::dimensions)
          .map(dimensions -> dimensions.get(0));
    }
  }

  record DimensionGroup(String key, List<DimensionIdentifier<DimensionParam>> dimensions) {}
}
