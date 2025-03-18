/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.analytics.EventOutputType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DATE_FILTERS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.FILTERS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.HEADERS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.SORTING;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getProgramAttributes;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.EventDataQueryRequest.ExtendedEventDataQueryRequestBuilder.DIMENSION_OR_SEPARATOR;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.feedback.ErrorCode.E7129;
import static org.hisp.dhis.feedback.ErrorCode.E7250;
import static org.hisp.dhis.feedback.ErrorCode.E7251;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilders;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;

/** Parser class for the common objects. */
@Component
@RequiredArgsConstructor
public class CommonRequestParamsParser implements Parser<CommonRequestParams, CommonParsedParams> {
  @Nonnull private final SystemSettingsProvider settingsProvider;

  @Nonnull private final DataQueryService dataQueryService;

  @Nonnull private final EventDataQueryService eventDataQueryService;

  @Nonnull private final ProgramService programService;

  @Nonnull private final DimensionIdentifierConverter dimensionIdentifierConverter;

  /**
   * Based on the given query request object {@link CommonRequestParams}, this method will
   * process/parse existing values in the request object, and populated all necessary attributes in
   * the return object {@link CommonParsedParams}. It appends program attributes to the dimension
   * list.
   *
   * @param request the {@link CommonRequestParams} to process.
   * @return the parsed {@link CommonParsedParams}.
   */
  @Nonnull
  @Override
  public CommonParsedParams parse(@Nonnull CommonRequestParams request) {
    List<OrganisationUnit> userOrgUnits =
        dataQueryService.getUserOrgUnits(null, request.getUserOrgUnit());
    List<Program> programs = getPrograms(request);

    // Adds all program attributes from all applicable programs as dimensions.
    request
        .getInternal()
        .getProgramAttributes()
        .addAll(getProgramAttributes(programs).map(IdentifiableObject::getUid).toList());

    return CommonParsedParams.builder()
        .programs(programs)
        .pagingParams(computePagingParams(request))
        .orderParams(getSortingParams(request, programs, userOrgUnits))
        .dimensionIdentifiers(retrieveDimensionParams(request, programs, userOrgUnits))
        .parsedHeaders(parseHeaders(request, programs, userOrgUnits))
        .userOrgUnit(userOrgUnits)
        .build();
  }

  /**
   * Apply paging parameters to the given {@link CommonRequestParams} object, taking into account
   * the system setting for the maximum limit and the ignoreLimit flag.
   *
   * @param commonRequestParams the {@link CommonRequestParams} used to compute the paging.
   * @return the computed {@link CommonRequestParams}.
   */
  private AnalyticsPagingParams computePagingParams(CommonRequestParams commonRequestParams) {
    int maxLimit = settingsProvider.getCurrentSettings().getAnalyticsMaxLimit();
    boolean unlimited = maxLimit == 0;
    boolean ignoreLimit = commonRequestParams.isIgnoreLimit();
    boolean hasMaxLimit = !unlimited && !ignoreLimit;
    int pageSize = commonRequestParams.getPageSize();

    if (commonRequestParams.isPaging()) {
      boolean pageSizeOverMaxLimit = commonRequestParams.getPageSize() > maxLimit;

      if (hasMaxLimit && pageSizeOverMaxLimit) {
        pageSize = maxLimit;
      }
    } else {
      if (unlimited) {
        ignoreLimit = true;
      } else {
        pageSize = maxLimit;
      }
    }

    return AnalyticsPagingParams.builder()
        .totalPages(commonRequestParams.isTotalPages())
        .paging(commonRequestParams.isPaging())
        .page(commonRequestParams.getPage())
        .pageSize(pageSize)
        .unlimited(ignoreLimit)
        .build();
  }

  private Set<DimensionIdentifier<DimensionParam>> parseHeaders(
      CommonRequestParams request, List<Program> programs, List<OrganisationUnit> userOrgUnits) {

    return HEADERS.getUidsGetter().apply(request).stream()
        .map(header -> toDimIdentifiers(header, HEADERS, request, programs, userOrgUnits))
        .collect(toSet());
  }

  /**
   * Based on the given arguments, it will extract a list of sorting objects params, if any. It will
   * return a list of {@link AnalyticsSortingParams}, where index of each element is the index of
   * the sorting param in the request.
   *
   * @param request the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return the {@link List} of sorting {@link AnalyticsSortingParams}.
   */
  private List<AnalyticsSortingParams> getSortingParams(
      CommonRequestParams request, List<Program> programs, List<OrganisationUnit> userOrgUnits) {
    return Streams.mapWithIndex(
            SORTING.getUidsGetter().apply(request).stream(),
            (sortRequest, index) ->
                toSortParam(index, sortRequest, request, programs, userOrgUnits))
        .toList();
  }

  /**
   * Based on the given arguments, it extracts the sort param object {@link AnalyticsSortingParams}.
   *
   * @param index the index of the sorting param in the request.
   * @param sortParam the representation in the format. uid1.uid2.uid3OrAttribute:ASC.
   * @param request the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return the built {@link AnalyticsSortingParams}.
   */
  private AnalyticsSortingParams toSortParam(
      long index,
      String sortParam,
      CommonRequestParams request,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {
    String[] parts = sortParam.split(":");
    return AnalyticsSortingParams.builder()
        .index(index)
        .sortDirection(SortDirection.of(parts[1]))
        .orderBy(toDimIdentifiers(parts[0], SORTING, request, programs, userOrgUnits))
        .build();
  }

  /**
   * Returns a {@link List} of {@link Program} objects based on the given {@link
   * CommonRequestParams}.
   *
   * @param queryRequest the {@link CommonRequestParams}.
   * @return the {@link List} of {@link Program} found.
   * @throws IllegalQueryException if the program(s) cannot be found.
   */
  private List<Program> getPrograms(CommonRequestParams queryRequest) {
    List<Program> programs =
        ImmutableList.copyOf(programService.getPrograms(queryRequest.getProgram()));
    boolean programsCouldNotBeRetrieved = programs.size() != queryRequest.getProgram().size();

    if (programsCouldNotBeRetrieved) {
      List<String> foundProgramUids = programs.stream().map(Program::getUid).toList();

      List<String> missingProgramUids =
          Optional.of(queryRequest).map(CommonRequestParams::getProgram).orElse(emptySet()).stream()
              .filter(uidFromRequest -> !foundProgramUids.contains(uidFromRequest))
              .toList();

      throw new IllegalQueryException(E7129, missingProgramUids);
    }

    return programs;
  }

  /**
   * Returns a {@link List} of {@link DimensionIdentifier} built from given arguments, params and
   * filter.
   *
   * @param queryRequest the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return a {@link List} of {@link DimensionIdentifier}.
   */
  private List<DimensionIdentifier<DimensionParam>> retrieveDimensionParams(
      CommonRequestParams queryRequest,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {

    return Stream.of(DIMENSIONS, FILTERS, DATE_FILTERS)
        .flatMap(type -> streamByType(type, queryRequest, programs, userOrgUnits))
        .toList();
  }

  /**
   * Streams the dimensions for the given {@link DimensionParamType} and maps them to {@link
   * DimensionIdentifier} objects.
   *
   * @param dimensionParamType the {@link DimensionParamType}.
   * @param queryRequest the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return the {@link Stream} of {@link DimensionIdentifier}.
   */
  private Stream<DimensionIdentifier<DimensionParam>> streamByType(
      DimensionParamType dimensionParamType,
      CommonRequestParams queryRequest,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {
    // A Collection of dimensions or filters coming from the request.
    Collection<String> dimensionString = dimensionParamType.getUidsGetter().apply(queryRequest);

    return dimensionString.stream()
        .map(CommonRequestParamsParser::splitOnOr)
        .map(dim -> toDimIdentifiers(dim, dimensionParamType, queryRequest, programs, userOrgUnits))
        .flatMap(Collection::stream);
  }

  /**
   * Returns a {@link List} of {@link DimensionIdentifier} built from given arguments, params and
   * filter, assigning a groupId to all dimensionIdentifier.
   *
   * @param dimensions the {@link List} of dimensions.
   * @param dimensionParamType the {@link DimensionParamType}.
   * @param queryRequest the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return a {@link List} of {@link DimensionIdentifier}.
   * @throws IllegalQueryException if "dimensionOrFilter" is not well-formed.
   */
  private List<DimensionIdentifier<DimensionParam>> toDimIdentifiers(
      List<String> dimensions,
      DimensionParamType dimensionParamType,
      CommonRequestParams queryRequest,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {
    return dimensions.stream()
        .map(
            dimensionAsString ->
                toDimIdentifiers(
                    dimensionAsString, dimensionParamType, queryRequest, programs, userOrgUnits))
        .map(this::withGroupId)
        .toList();
  }

  /**
   * Assigns a groupId to the given {@link DimensionIdentifier}. If the dimension is static or
   * period, it will assign a default groupId (the full dimensionIdentifier). Otherwise, it will
   * assign a random UUID as groupId.
   *
   * @param dimensionIdentifier the {@link DimensionIdentifier}.
   * @return the {@link DimensionIdentifier} with a groupId.
   */
  private DimensionIdentifier<DimensionParam> withGroupId(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    if (dimensionIdentifier.getDimension().isStaticDimension()
        || dimensionIdentifier.getDimension().isPeriodDimension()) {
      return dimensionIdentifier.withDefaultGroupId();
    }
    return dimensionIdentifier.withGroupId(UUID.randomUUID().toString());
  }

  /**
   * Splits the dimensions grouped by _OR_ into a {@link List} of String.
   *
   * @param dimensionAsString, in the format dim_OR_anotherDim.
   * @return the {@link List} of String.
   */
  private static List<String> splitOnOr(String dimensionAsString) {
    return stream(DIMENSION_OR_SEPARATOR.split(dimensionAsString)).toList();
  }

  /**
   * Returns a {@link DimensionIdentifier} built from given arguments, params and filter.
   *
   * @param dimensionOrFilter the uid of a dimension or filter.
   * @param dimensionParamType the {@link DimensionParamType}.
   * @param queryRequest the {@link CommonRequestParams}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return the {@link DimensionIdentifier}.
   * @throws IllegalQueryException if "dimensionOrFilter" is not well-formed.
   */
  private DimensionIdentifier<DimensionParam> toDimIdentifiers(
      String dimensionOrFilter,
      DimensionParamType dimensionParamType,
      CommonRequestParams queryRequest,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {
    return toDimIdentifiers(
        dimensionOrFilter,
        dimensionParamType,
        queryRequest.getRelativePeriodDate(),
        queryRequest.getDisplayProperty(),
        queryRequest.getOutputIdScheme(),
        programs,
        userOrgUnits);
  }

  /**
   * Returns a {@link DimensionIdentifier} built from given arguments, params and filter.
   *
   * @param dimensionOrFilter the uid of a dimension or filter.
   * @param dimensionParamType the {@link DimensionParamType}.
   * @param relativePeriodDate the {@link Date} used to compute the relative period.
   * @param displayProperty the {@link DisplayProperty}.
   * @param outputIdScheme the {@link IdScheme}.
   * @param programs the list of {@link Program}.
   * @param userOrgUnits the list of {@link OrganisationUnit}.
   * @return the {@link DimensionIdentifier}.
   * @throws IllegalQueryException if "dimensionOrFilter" is not well-formed.
   */
  public DimensionIdentifier<DimensionParam> toDimIdentifiers(
      String dimensionOrFilter,
      DimensionParamType dimensionParamType,
      Date relativePeriodDate,
      DisplayProperty displayProperty,
      IdScheme outputIdScheme,
      List<Program> programs,
      List<OrganisationUnit> userOrgUnits) {
    String dimensionId = getDimensionFromParam(dimensionOrFilter);

    // We first parse the dimensionId into <Program, ProgramStage, String>
    // to be able to operate on the string version (uid) of the dimension.
    DimensionIdentifier<StringUid> stringDimensionIdentifier =
        dimensionIdentifierConverter.fromString(programs, dimensionId);
    List<String> items = getDimensionItemsFromParam(dimensionOrFilter);

    Optional<StaticDimension> staticDimension =
        StaticDimension.of(stringDimensionIdentifier.getDimension().getUid());

    // Then we check if it's a static dimension.
    if (staticDimension.isPresent()) {
      return parseAsStaticDimension(
          dimensionParamType, stringDimensionIdentifier, outputIdScheme, items);
    }

    // Then we check if it's a DimensionalObject.
    DimensionalObject dimensionalObject =
        dataQueryService.getDimension(
            stringDimensionIdentifier.getDimension().getUid(),
            items,
            relativePeriodDate,
            userOrgUnits,
            true,
            displayProperty,
            UID);

    if (Objects.nonNull(dimensionalObject)) {
      DimensionParam dimensionParam =
          DimensionParam.ofObject(dimensionalObject, dimensionParamType, outputIdScheme, items);
      return DimensionIdentifier.of(
          stringDimensionIdentifier.getProgram(),
          stringDimensionIdentifier.getProgramStage(),
          dimensionParam);
    }

    QueryItem queryItem;

    if (!stringDimensionIdentifier.hasProgram() && !stringDimensionIdentifier.hasProgramStage()) {
      // If we reach here, it should be a trackedEntityAttribute.
      queryItem =
          eventDataQueryService.getQueryItem(
              stringDimensionIdentifier.getDimension().getUid(), null, TRACKED_ENTITY_INSTANCE);

      if (Objects.isNull(queryItem)) {
        throw new IllegalQueryException(E7250, dimensionId);
      }

    } else {
      // If we reach here, it should be a queryItem. In this case it can be either
      // a program indicator (with programUid prefix) or a Data Element
      // (both program and program stage prefixes).
      queryItem =
          eventDataQueryService.getQueryItem(
              stringDimensionIdentifier.getDimension().getUid(),
              stringDimensionIdentifier.getProgram().getElement(),
              TRACKED_ENTITY_INSTANCE);

      // TEA should only be specified without program prefix.
      if (queryItem.getItem() instanceof TrackedEntityAttribute) {
        throw new IllegalQueryException(E7250, dimensionId);
      }
    }

    // for queryItems, dimension string is in the format:
    // uid:OP:VAL1:OP2:VAL2:OPn:VALn
    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        DimensionIdentifier.of(
            stringDimensionIdentifier.getProgram(),
            stringDimensionIdentifier.getProgramStage(),
            DimensionParam.ofObject(
                queryItem,
                dimensionParamType,
                outputIdScheme,
                parseDimensionItems(dimensionOrFilter)));

    /* DHIS2-16732 missing support for ProgramIndicators */
    if (SqlQueryBuilders.isOfType(
        dimensionIdentifier, DimensionParamObjectType.PROGRAM_INDICATOR)) {
      throw new IllegalQueryException(E7251, stringDimensionIdentifier.toString());
    }

    return dimensionIdentifier;
  }

  /**
   * Parses the dimension items from the given dimension or filter into a {@link List} of String.
   * The format of the dimension or filter is: uid:OP:VAL1:OP2:VAL2:...:OPn:VALn The method will
   * skip the first element (the uid) and then return a list of all the remaining pairs of operator
   * and value. (example ["OP:VAL1", "OP2:VAL2", ..., "OPn:VALn"])
   *
   * @param dimensionOrFilter the dimension string.
   * @return the {@link List} of parsed dimension items.
   */
  private List<String> parseDimensionItems(String dimensionOrFilter) {
    final byte dimensionUid = 1;
    if (Objects.isNull(dimensionOrFilter) || !dimensionOrFilter.contains(":")) {
      return Collections.emptyList();
    }

    return toPairs(
            stream(dimensionOrFilter.split(":"))
                .skip(dimensionUid)
                .map(String::trim)
                .filter(StringUtils::isNotBlank))
        .map(CommonRequestParamsParser::toItem)
        .toList();
  }

  /**
   * Returns a string representation of the given pair by joining the left and right elements with a
   * colon.
   *
   * @param pair the pair.
   * @return the string representation.
   */
  private static String toItem(Pair<String, String> pair) {
    return Stream.of(pair.getLeft(), pair.getRight())
        .filter(Objects::nonNull)
        .collect(Collectors.joining(":"));
  }

  /**
   * Returns a {@link Stream} of pairs of elements from the given stream, where each pair is
   * composed of the current element and the next element. Example of input: [1, 2, 3, 4, 5] Example
   * of output: [(1, 2), (3, 4), (5, null)]
   *
   * @param stream the input stream.
   * @return the {@link Stream} of pairs.
   * @param <T> the type of the elements in the stream.
   */
  private <T> Stream<Pair<T, T>> toPairs(final Stream<T> stream) {
    final AtomicInteger counter = new AtomicInteger(0);
    return stream
        .collect(
            Collectors.groupingBy(
                item -> {
                  final int i = counter.getAndIncrement();
                  return (i % 2 == 0) ? i : i - 1;
                }))
        .values()
        .stream()
        .map(a -> Pair.of(a.get(0), (a.size() == 2 ? a.get(1) : null)));
  }

  private static DimensionIdentifier<DimensionParam> parseAsStaticDimension(
      DimensionParamType dimensionParamType,
      DimensionIdentifier<StringUid> dimensionIdentifier,
      IdScheme outputIdScheme,
      List<String> items) {
    return DimensionIdentifier.of(
        dimensionIdentifier.getProgram(),
        dimensionIdentifier.getProgramStage(),
        DimensionParam.ofObject(
            dimensionIdentifier.getDimension().getUid(),
            dimensionParamType,
            outputIdScheme,
            items));
  }
}
