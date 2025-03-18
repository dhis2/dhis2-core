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
package org.hisp.dhis.analytics.trackedentity.query;

import static java.util.Arrays.stream;
import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getCustomLabelOrFullName;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getCustomLabelOrHeaderColumnName;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.joinedWithPrefixesIfNeeded;
import static org.hisp.dhis.analytics.trackedentity.query.context.QueryContextConstants.TRACKED_ENTITY_ALIAS;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.TrackedEntityStaticField;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * This class provides methods responsible for extracting collections from different objects types,
 * like {@link TrackedEntityAttribute}, {@link ProgramTrackedEntityAttribute}, {@link GridHeader}
 * and {@link Field}.
 */
@NoArgsConstructor(access = PRIVATE)
public class TrackedEntityFields {

  private static final Comparator<TrackedEntityAttribute> CASE_INSENSITIVE_UID_COMPARATOR =
      (o1, o2) -> o1.getUid().compareToIgnoreCase(o2.getUid());

  /**
   * Retrieves all TEAs attributes from the given param encapsulating them into a stream of {@link
   * Field}.
   *
   * @param contextParams the {@link TrackedEntityQueryParams}.
   * @return a {@link Stream} of {@link Field}.
   */
  public static Stream<Field> getDimensionFields(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    TrackedEntityQueryParams trackedEntityQueryParams = contextParams.getTypedParsed();
    CommonParsedParams parsedParams = contextParams.getCommonParsed();

    return Stream.concat(
            getProgramAttributes(parsedParams.getPrograms()),
            getTrackedEntityAttributes(trackedEntityQueryParams.getTrackedEntityType()))
        .map(TrackedEntityAttribute::getUid)
        .distinct()
        .map(attr -> Field.of(TRACKED_ENTITY_ALIAS, () -> attr, attr));
  }

  /**
   * Extracts a stream of {@link TrackedEntityAttribute} found in the given list of {@link Program}.
   *
   * @param programs the list of {@link Program}.
   * @return a {@link Stream} of {@link TrackedEntityAttribute}.
   */
  public static Stream<TrackedEntityAttribute> getProgramAttributes(List<Program> programs) {
    // Attributes from Programs.
    return programs.stream()
        .map(Program::getProgramAttributes)
        .flatMap(List::stream)
        .map(ProgramTrackedEntityAttribute::getAttribute)
        .sorted(CASE_INSENSITIVE_UID_COMPARATOR);
  }

  /**
   * Extracts a stream of {@link TrackedEntityAttribute} from the given {@link TrackedEntityType}.
   *
   * @param trackedEntityType the {@link TrackedEntityType}.
   * @return a {@link Stream} of {@link TrackedEntityAttribute}, or empty.
   */
  public static Stream<TrackedEntityAttribute> getTrackedEntityAttributes(
      TrackedEntityType trackedEntityType) {
    if (trackedEntityType != null) {
      return trackedEntityType.getTrackedEntityAttributes().stream()
          .sorted(CASE_INSENSITIVE_UID_COMPARATOR);
    }

    return Stream.empty();
  }

  /**
   * Retrieves only static fields.
   *
   * @return the {@link Stream} of {@link Field}.
   */
  public static Stream<Field> getStaticFields() {
    return Stream.of(TrackedEntityStaticField.values())
        .map(TrackedEntityStaticField::getAlias)
        .map(a -> Field.of(TRACKED_ENTITY_ALIAS, () -> a, a));
  }

  /**
   * Returns a collection of all possible headers for the given {@link TrackedEntityQueryParams}. It
   * includes headers extracted for static and dynamic dimensions that matches the given list of
   * {@link Field}.
   *
   * <p>The static headers are also delivered on the top of the collection. The dynamic comes after
   * the static ones.
   *
   * @param contextParams the {@link ContextParams}.
   * @param fields list of {@link Field}.
   * @return a {@link Set} of {@link GridHeader}.
   */
  public static Set<GridHeader> getGridHeaders(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams,
      List<Field> fields) {
    CommonParsedParams commonParsed = contextParams.getCommonParsed();

    Map<String, GridHeader> headersMap = new HashMap<>();

    // Adding static and dynamic headers.
    stream(TrackedEntityStaticField.values())
        .forEach(
            f ->
                headersMap.put(
                    f.getAlias(),
                    new GridHeader(
                        f.getAlias(),
                        // should be a custom label instead of fullName in case the dimension is OU,
                        // however the ou label is stored in program definitions and  there are
                        // multiple programs in the query by design (it's cross program), so we
                        // can't  decide which program to use for the label. This is probably a bad
                        // design decision, but it's not something we can fix here, unless adopting
                        // some ugly workarounds. (DHIS2-16807)
                        f.getFullName(),
                        f.getType(),
                        false,
                        true)));

    // Adding dimension headers.
    fields.stream()
        .map(
            field ->
                findDimensionParamForField(
                    field,
                    Stream.concat(
                        commonParsed.getDimensionIdentifiers().stream(),
                        getEligibleParsedHeaders(commonParsed))))
        .filter(Objects::nonNull)
        .map(
            dimIdentifier ->
                Pair.of(dimIdentifier, getHeaderForDimensionParam(dimIdentifier, contextParams)))
        .map(pair -> withStageOffsetIfNecessary(pair.getLeft(), pair.getRight()))
        .filter(Objects::nonNull)
        .forEach(g -> headersMap.put(g.getName(), g));

    return reorder(headersMap, fields);
  }

  /**
   * Adds the stage offset to the given {@link GridHeader} if necessary.
   *
   * @param dimIdentifier the {@link DimensionIdentifier}.
   * @param gridHeader the {@link GridHeader}.
   * @return the updated {@link GridHeader}.
   */
  private static GridHeader withStageOffsetIfNecessary(
      DimensionIdentifier<DimensionParam> dimIdentifier, GridHeader gridHeader) {
    return Optional.of(dimIdentifier)
        .filter(DimensionIdentifier::hasProgramStage)
        .map(DimensionIdentifier::getProgramStage)
        .map(ElementWithOffset::getOffset)
        .map(offset -> gridHeader.withRepeatableStageParams(RepeatableStageParams.of(offset)))
        .orElse(gridHeader);
  }

  /**
   * Since TeiStaticFields are already added to the grid headers, we need to filter them out from
   * the list of parsed headers.
   *
   * @param commonParams the {@link CommonParsedParams}.
   * @return a {@link Stream} of {@link DimensionIdentifier}.
   */
  private static Stream<DimensionIdentifier<DimensionParam>> getEligibleParsedHeaders(
      CommonParsedParams commonParams) {
    return commonParams.getParsedHeaders().stream().filter(TrackedEntityFields::isEligible);
  }

  /**
   * Checks if the given {@link DimensionIdentifier} is eligible to be added as a header. It is
   * eligible if it is a static dimension, and it is either an event or enrollment dimension.
   *
   * @param parsedHeader the {@link DimensionIdentifier}.
   * @return true if it is eligible, false otherwise.
   */
  private static boolean isEligible(DimensionIdentifier<DimensionParam> parsedHeader) {
    return parsedHeader.getDimension().isStaticDimension()
        && (parsedHeader.isEventDimension() || parsedHeader.isEnrollmentDimension());
  }

  /**
   * Based on the given map of {@link GridHeader}, it will return a set of headers, reordering the
   * headers respecting the given fields ordering. Only elements inside the given map are returned.
   * The rest is ignored.
   *
   * <p>This is needed because the "fields" should drive the headers ordering. The "fields"
   * represent the columns selected from the DB, hence headers should reflect the same order of
   * columns.
   *
   * @param headersMap the map of {@link GridHeader}.
   * @param fields the list of {@link Field} to be respected.
   * @return the reordered set of {@link GridHeader}.
   */
  private static Set<GridHeader> reorder(Map<String, GridHeader> headersMap, List<Field> fields) {
    Set<GridHeader> headers = new LinkedHashSet<>();

    fields.forEach(
        field -> {
          if (headersMap.containsKey(field.getDimensionIdentifier())) {
            headers.add(headersMap.get(field.getDimensionIdentifier()));
          }
        });

    return headers;
  }

  /**
   * Creates a {@link GridHeader} for the given {@link DimensionParam} based on the given {@link
   * CommonParams}. The last is needed because of particular cases where we need a custom version of
   * the header.
   *
   * @param dimIdentifier the {@link DimensionIdentifier}.
   * @param contextParams the {@link ContextParams}.
   * @return the respective {@link GridHeader}.
   */
  private static GridHeader getHeaderForDimensionParam(
      DimensionIdentifier<DimensionParam> dimIdentifier,
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    DimensionParam dimensionParam = dimIdentifier.getDimension();
    QueryItem queryItem = dimensionParam.getQueryItem();
    DimensionalObject dimensionalObject = dimensionParam.getDimensionalObject();

    if (queryItem != null) {
      return getCustomGridHeaderForQueryItem(queryItem, contextParams, dimIdentifier);
    } else if (dimensionalObject != null) {
      return getCustomGridHeaderForDimensionalObject(
          dimIdentifier, d -> d.getDimensionalObject().getDimensionDisplayName());
    } else {
      // It is a static dimension.
      return getCustomGridHeaderForStaticDimension(dimIdentifier);
    }
  }

  private static GridHeader getCustomGridHeaderForStaticDimension(
      DimensionIdentifier<DimensionParam> dimId) {
    if (dimId.isEventDimension() || dimId.isEnrollmentDimension()) {
      return getGridHeader(
          dimensionId -> getCustomLabelOrHeaderColumnName(dimensionId, true, false), dimId);
    }
    return getGridHeader(dimensionId -> getCustomLabelOrFullName(dimensionId, false, false), dimId);
  }

  private static GridHeader getGridHeader(
      Function<DimensionIdentifier<DimensionParam>, String> labelProvider,
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return new GridHeader(
        dimensionIdentifier.getKey(),
        labelProvider.apply(dimensionIdentifier),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  private static GridHeader getCustomGridHeaderForDimensionalObject(
      DimensionIdentifier<DimensionParam> dimensionIdentifier,
      Function<DimensionParam, String> labelProvider) {
    return new GridHeader(
        dimensionIdentifier.getKey(),
        joinedWithPrefixesIfNeeded(
            dimensionIdentifier, labelProvider.apply(dimensionIdentifier.getDimension()), true),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  private static ValueType getValueType(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    /*
     * In some situations the DimensionalObject.valueType can be null. In
     * such cases, it defaults to ValueType.TEXT.
     */
    return Optional.of(dimensionIdentifier)
        .map(DimensionIdentifier::getDimension)
        .map(DimensionParam::getValueType)
        .orElse(ValueType.TEXT);
  }

  /**
   * Depending on the {@link QueryItem} we need to return specific headers. This method will
   * evaluate the given query item and take care of the particulars cases where we need a custom
   * {@link GridHeader} for the respective {@link QueryItem}.
   *
   * @param queryItem the {@link QueryItem}.
   * @param contextParams the {@link ContextParams}.
   * @return the correct {@link GridHeader} version.
   */
  private static GridHeader getCustomGridHeaderForQueryItem(
      QueryItem queryItem,
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams,
      DimensionIdentifier<DimensionParam> dimIdentifier) {
    CommonRequestParams requestParams = contextParams.getCommonRaw();
    CommonParsedParams commonParsed = contextParams.getCommonParsed();
    /*
     * If the request contains a query item of value type ORGANISATION_UNIT
     * and the item UID is linked to coordinates (coordinateField), then
     * create a header of ValueType COORDINATE.
     */
    if (queryItem.getValueType() == ORGANISATION_UNIT
        && commonParsed.getCoordinateFields().stream()
            .anyMatch(f -> f.equals(queryItem.getItem().getUid()))) {
      return new GridHeader(
          queryItem.getItem().getUid(),
          joinedWithPrefixesIfNeeded(
              dimIdentifier,
              queryItem.getItem().getDisplayProperty(requestParams.getDisplayProperty()),
              true),
          COORDINATE,
          false,
          true,
          queryItem.getOptionSet(),
          queryItem.getLegendSet());
    } else if (queryItem.hasNonDefaultRepeatableProgramStageOffset()) {
      String column = queryItem.getItem().getDisplayProperty(requestParams.getDisplayProperty());
      RepeatableStageParams repeatableStageParams = queryItem.getRepeatableStageParams();
      String dimName = repeatableStageParams.getDimension();
      ValueType valueType = queryItem.getValueType();

      return new GridHeader(
          dimName,
          joinedWithPrefixesIfNeeded(dimIdentifier, column, true),
          valueType,
          false,
          true,
          queryItem.getOptionSet(),
          queryItem.getLegendSet(),
          queryItem.getProgramStage().getUid(),
          repeatableStageParams);
    } else {
      String itemUid = dimIdentifier.toString();
      String column = queryItem.getItem().getDisplayProperty(requestParams.getDisplayProperty());

      return new GridHeader(
          itemUid,
          joinedWithPrefixesIfNeeded(dimIdentifier, column, true),
          queryItem.getValueType(),
          false,
          true,
          queryItem.getOptionSet(),
          queryItem.getLegendSet());
    }
  }

  /**
   * Finds the respective {@link DimensionParam}, from the given list of {@link
   * DimensionIdentifier}, that is associated with the given {@link Field}.
   *
   * @param field the {@link Field}.
   * @param dimensionIdentifiers the list of {@link DimensionIdentifier}.
   * @return the correct {@link DimensionIdentifier}
   * @throws IllegalStateException if nothing is found.
   */
  private static DimensionIdentifier<DimensionParam> findDimensionParamForField(
      Field field, Stream<DimensionIdentifier<DimensionParam>> dimensionIdentifiers) {
    return dimensionIdentifiers
        .filter(di -> di.toString().equals(field.getDimensionIdentifier()))
        .findFirst()
        .orElse(null);
  }
}
