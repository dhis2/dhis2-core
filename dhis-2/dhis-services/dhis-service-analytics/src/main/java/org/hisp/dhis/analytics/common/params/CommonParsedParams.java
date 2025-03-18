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
package org.hisp.dhis.analytics.common.params;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/** Responsible for consolidating all parsed objects and collections used across analytics flows. */
@Data
@Builder(toBuilder = true)
public class CommonParsedParams {
  /** The list of Program objects carried on by this object. */
  @Builder.Default private final List<Program> programs = new ArrayList<>();

  /**
   * Data structure containing dimensionParams, which can represent dimensions, filters, queryItems
   * or queryItemFilters.
   */
  @Builder.Default
  private final List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = new ArrayList<>();

  /**
   * Data structure containing parsed versions of the headers. If present, they will represent the
   * columns to be retrieved. Cannot be repeated and should keep ordering, hence a {@link
   * LinkedHashSet}.
   */
  @Builder.Default
  private final Set<DimensionIdentifier<DimensionParam>> parsedHeaders = new LinkedHashSet<>();

  /** The object that groups the paging and sorting parameters. */
  @Builder.Default
  private final AnalyticsPagingParams pagingParams = AnalyticsPagingParams.builder().build();

  /** List of sorting params. */
  @Builder.Default private final List<AnalyticsSortingParams> orderParams = Collections.emptyList();

  /** The user's organization unit. */
  @Builder.Default private final List<OrganisationUnit> userOrgUnit = Collections.emptyList();

  /**
   * The coordinate fields to use as basis for spatial event analytics. The list is built as
   * collection of coordinate field and fallback fields. The order defines priority of geometry
   * fields.
   */
  @Builder.Default private final List<String> coordinateFields = Collections.emptyList();

  public List<DimensionIdentifier<DimensionParam>> getDimensionIdentifiers() {
    return emptyIfNull(dimensionIdentifiers).stream().filter(Objects::nonNull).toList();
  }

  /**
   * Gets a new instance of the internal delegator object.
   *
   * @return an instance of {@link CommonParamsDelegator}.
   */
  public CommonParamsDelegator delegate() {
    return new CommonParamsDelegator(getDimensionIdentifiers());
  }

  /**
   * Gets all dimension identifiers, including parsed headers and order parameters, and removes
   * duplicates (by getting the first element of each group). Shouldn't be used when the order of
   * the dimension identifiers is important or when access to dimension identifiers restrictions is
   * needed.
   *
   * @return the list of dimension identifiers
   */
  public List<DimensionIdentifier<DimensionParam>> getAllDimensionIdentifiers() {
    return streamDimensions()
        .collect(groupingBy(DimensionIdentifier::getKeyNoOffset))
        .values()
        .stream()
        .map(identifiers -> identifiers.get(0))
        .toList();
  }

  /**
   * Gets a stream of all dimension identifiers, including parsed headers and order parameters.
   *
   * @return the stream of dimension identifiers
   */
  public Stream<DimensionIdentifier<DimensionParam>> streamDimensions() {
    return Stream.of(
            dimensionIdentifiers.stream(),
            parsedHeaders.stream(),
            orderParams.stream().map(AnalyticsSortingParams::getOrderBy))
        .flatMap(Function.identity());
  }
}
