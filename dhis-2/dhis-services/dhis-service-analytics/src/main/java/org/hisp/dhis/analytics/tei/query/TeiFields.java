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
package org.hisp.dhis.analytics.tei.query;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getCustomLabelOrFullName;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getCustomLabelOrHeaderColumnName;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.REFERENCE;
import static org.hisp.dhis.commons.util.TextUtils.SPACE;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.query.context.TeiStaticField;
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
public class TeiFields {
  private static final String EVENT_COLUMN_PREFIX = "Event";

  private static final String ENROLLMENT_COLUMN_PREFIX = "Enrollment";

  /**
   * Retrieves all TEAs attributes from the given param encapsulating them into a stream of {@link
   * Field}.
   *
   * @param teiQueryParams the {@link TeiQueryParams}.
   * @return a {@link Stream} of {@link Field}.
   */
  public static Stream<Field> getDimensionFields(TeiQueryParams teiQueryParams) {
    return Stream.concat(
            teiQueryParams.getCommonParams().getPrograms().stream()
                .map(Program::getProgramAttributes)
                .flatMap(List::stream)
                .map(ProgramTrackedEntityAttribute::getAttribute)
                .map(TrackedEntityAttribute::getUid),
            teiQueryParams.getTrackedEntityType().getTrackedEntityAttributes().stream()
                .map(TrackedEntityAttribute::getUid))
        .distinct()
        .map(attr -> Field.of(TEI_ALIAS, () -> attr, attr));
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
        .map(ProgramTrackedEntityAttribute::getAttribute);
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
      return trackedEntityType.getTrackedEntityAttributes().stream();
    }

    return Stream.empty();
  }

  /**
   * Retrieves only static fields.
   *
   * @return the {@link Stream} of {@link Field}.
   */
  public static Stream<Field> getStaticFields() {
    return Stream.of(TeiStaticField.values())
        .map(TeiStaticField::getAlias)
        .map(a -> Field.of(TEI_ALIAS, () -> a, a));
  }

  /**
   * Returns a collection of all possible headers for the given {@link TeiQueryParams}. It includes
   * headers extracted for static and dynamic dimensions that matches the given list of {@link
   * Field}.
   *
   * <p>The static headers are also delivered on the top of the collection. The dynamic comes after
   * the static ones.
   *
   * @param teiQueryParams the {@link TeiQueryParams}.
   * @param fields the list of {@link Field}.
   * @return a {@link Set} of {@link GridHeader}.
   */
  public static Set<GridHeader> getGridHeaders(TeiQueryParams teiQueryParams, List<Field> fields) {
    Map<String, GridHeader> headersMap = new HashMap<>();

    // Adding static and dynamic headers.
    stream(TeiStaticField.values())
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
                        teiQueryParams.getCommonParams().getDimensionIdentifiers().stream(),
                        getEligibleParsedHeaders(teiQueryParams))))
        .filter(Objects::nonNull)
        .map(
            dimIdentifier ->
                Pair.of(
                    dimIdentifier,
                    getHeaderForDimensionParam(dimIdentifier, teiQueryParams.getCommonParams())))
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
        .map(
            offset ->
                gridHeader.withRepeatableStageParams(RepeatableStageParams.ofStartIndex(offset)))
        .orElse(gridHeader);
  }

  /**
   * Since TeiStaticFields are already added to the grid headers, we need to filter them out from
   * the list of parsed headers.
   *
   * @param teiQueryParams the {@link TeiQueryParams}.
   * @return a {@link Stream} of {@link DimensionIdentifier}.
   */
  private static Stream<DimensionIdentifier<DimensionParam>> getEligibleParsedHeaders(
      TeiQueryParams teiQueryParams) {
    return teiQueryParams.getCommonParams().getParsedHeaders().stream()
        .filter(TeiFields::isEligible);
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
   * @param commonParams the {@link CommonParams}.
   * @return the respective {@link GridHeader}.
   */
  private static GridHeader getHeaderForDimensionParam(
      DimensionIdentifier<DimensionParam> dimIdentifier, CommonParams commonParams) {
    DimensionParam dimensionParam = dimIdentifier.getDimension();
    QueryItem queryItem = dimensionParam.getQueryItem();
    DimensionalObject dimensionalObject = dimensionParam.getDimensionalObject();

    if (queryItem != null) {
      return getCustomGridHeaderForQueryItem(queryItem, commonParams, dimIdentifier);
    } else if (dimensionalObject != null) {
      return getCustomGridHeaderForDimensionalObject(
          dimIdentifier, d -> d.getDimensionalObject().getDimensionDisplayName());
    } else {
      // It is a static dimension.
      return getCustomGridHeaderForStaticDimension(dimIdentifier);
    }
  }

  private static GridHeader getCustomGridHeaderForStaticDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    if (dimensionIdentifier.hasProgramStage()) { // Event static dimension.
      return getCustomGridHeaderForStaticEventDimension(dimensionIdentifier);
    }
    if (dimensionIdentifier.hasProgram()) { // Enrollment static dimension.
      return getCustomGridHeaderForStaticEnrollmentDimension(dimensionIdentifier);
    }
    return getCustomGridHeaderForStaticTeiDimension(dimensionIdentifier);
  }

  @Nonnull
  private static GridHeader getCustomGridHeaderForStaticTeiDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return new GridHeader(
        dimensionIdentifier.getKey(),
        getCustomLabelOrFullName(dimensionIdentifier),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  @Nonnull
  private static GridHeader getCustomGridHeaderForStaticEnrollmentDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    String program = dimensionIdentifier.getProgram().getElement().getDisplayName();

    return new GridHeader(
        dimensionIdentifier.getKey(),
        joined(getCustomLabelOrHeaderColumnName(dimensionIdentifier), program),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  @Nonnull
  private static GridHeader getCustomGridHeaderForStaticEventDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    String program = dimensionIdentifier.getProgram().getElement().getDisplayName();
    String programStage = dimensionIdentifier.getProgramStage().getElement().getDisplayName();

    return new GridHeader(
        dimensionIdentifier.getKey(),
        joined(getCustomLabelOrHeaderColumnName(dimensionIdentifier), program, programStage),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  private static String joined(String... parts) {
    return join(", ", parts).trim();
  }

  private static GridHeader getCustomGridHeaderForDimensionalObject(
      DimensionIdentifier<DimensionParam> dimensionIdentifier,
      Function<DimensionParam, String> dimensionNameProvider) {
    return new GridHeader(
        dimensionIdentifier.getKey(),
        join(
                SPACE,
                getColumnPrefix(dimensionIdentifier),
                dimensionNameProvider.apply(dimensionIdentifier.getDimension()))
            .trim(),
        getValueType(dimensionIdentifier),
        false,
        true);
  }

  private static String getColumnPrefix(DimensionIdentifier<DimensionParam> dimIdentifier) {
    if (dimIdentifier.hasProgramStage()) {
      return EVENT_COLUMN_PREFIX;
    } else if (dimIdentifier.hasProgram()) {
      return ENROLLMENT_COLUMN_PREFIX;
    }
    return EMPTY;
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
   * @param commonParams the {@link CommonParams}.
   * @return the correct {@link GridHeader} version.
   */
  private static GridHeader getCustomGridHeaderForQueryItem(
      QueryItem queryItem,
      CommonParams commonParams,
      DimensionIdentifier<DimensionParam> dimIdentifier) {
    /*
     * If the request contains a query item of value type ORGANISATION_UNIT
     * and the item UID is linked to coordinates (coordinateField), then
     * create a header of ValueType COORDINATE.
     */
    if (queryItem.getValueType() == ORGANISATION_UNIT
        && commonParams.getCoordinateFields().stream()
            .anyMatch(f -> f.equals(queryItem.getItem().getUid()))) {
      return new GridHeader(
          queryItem.getItem().getUid(),
          queryItem.getItem().getDisplayProperty(commonParams.getDisplayProperty()),
          COORDINATE,
          false,
          true,
          queryItem.getOptionSet(),
          queryItem.getLegendSet());
    } else if (queryItem.hasNonDefaultRepeatableProgramStageOffset()) {
      String column = queryItem.getItem().getDisplayProperty(commonParams.getDisplayProperty());
      RepeatableStageParams repeatableStageParams = queryItem.getRepeatableStageParams();
      String dimName = repeatableStageParams.getDimension();
      ValueType valueType =
          repeatableStageParams.simpleStageValueExpected() ? queryItem.getValueType() : REFERENCE;

      return new GridHeader(
          dimName,
          column,
          valueType,
          false,
          true,
          queryItem.getOptionSet(),
          queryItem.getLegendSet(),
          queryItem.getProgramStage().getUid(),
          repeatableStageParams);
    } else {
      String itemUid = dimIdentifier.toString();
      String column = queryItem.getItem().getDisplayProperty(commonParams.getDisplayProperty());

      return new GridHeader(
          itemUid,
          column,
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
