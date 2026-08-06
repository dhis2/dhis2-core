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
package org.hisp.dhis.analytics.trackedentity;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getProgramAttributes;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getTrackedEntityAttributes;
import static org.hisp.dhis.feedback.ErrorCode.E7125;
import static org.hisp.dhis.feedback.ErrorCode.E7142;
import static org.hisp.dhis.feedback.ErrorCode.E7254;
import static org.hisp.dhis.feedback.ErrorCode.E7255;
import static org.hisp.dhis.feedback.ErrorCode.E7256;
import static org.hisp.dhis.feedback.ErrorCode.E7257;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper;
import org.hisp.dhis.analytics.common.params.dimension.StringDimensionIdentifier;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Component;

/**
 * This component maps creates a @{TrackedEntityQueryParams} query params objects. The query params
 * objects represents the preparation of dimensions and elements that are ready to be queried at
 * database level.
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityQueryRequestMapper {
  private final TrackedEntityTypeService trackedEntityTypeService;

  private final ProgramService programService;

  /** The aggregation functions a tracked entity aggregate query supports. */
  private static final Set<AggregationType> SUPPORTED_AGGREGATION_TYPES =
      EnumSet.of(
          AggregationType.COUNT,
          AggregationType.SUM,
          AggregationType.AVERAGE,
          AggregationType.MIN,
          AggregationType.MAX,
          AggregationType.STDDEV,
          AggregationType.VARIANCE);

  /**
   * Maps incoming query requests into a valid and usable {@link TrackedEntityQueryParams}. Be aware
   * that it changes the state of the given {@link CommonRequestParams} in specific cases.
   *
   * @param trackedEntityTypeUid the tracked entity type uid.
   * @param requestParams the {@link CommonRequestParams}.
   * @return the populated {@link TrackedEntityQueryParams}.
   * @throws IllegalQueryException if the current TrackedEntityType specified in the given request
   *     is invalid or non-existent.
   */
  public TrackedEntityQueryParams map(
      String trackedEntityTypeUid, CommonRequestParams requestParams) {
    TrackedEntityType trackedEntityType = getTrackedEntityType(trackedEntityTypeUid);

    if (!requestParams.hasPrograms()) {
      requestParams.setProgram(getProgramUidsFromTrackedEntityType(trackedEntityType));
      requestParams.getInternal().setRequestPrograms(false);
    } else {
      checkTetForPrograms(requestParams.getProgram(), trackedEntityType);
      requestParams.getInternal().setRequestPrograms(true);
    }

    // Adding tracked entity type attributes to the list of dimensions.
    requestParams
        .getInternal()
        .getEntityTypeAttributes()
        .addAll(
            getTrackedEntityAttributes(trackedEntityType).map(IdentifiableObject::getUid).toList());

    validateAggregationType(requestParams);

    List<Program> programs = List.copyOf(programService.getPrograms(requestParams.getProgram()));
    TrackedEntityAttribute attributeValue = null;
    EventValue eventValue = null;
    String valueUid = requestParams.getValue();
    if (valueUid != null) {
      if (isProgramStageDataElement(valueUid)) {
        eventValue = resolveEventValue(valueUid, programs);
      } else {
        attributeValue = resolveAttributeValue(valueUid, programs, trackedEntityType);
      }
    }

    return TrackedEntityQueryParams.builder()
        .trackedEntityType(trackedEntityType)
        .attributeValue(attributeValue)
        .eventValue(eventValue)
        .aggregationType(
            aggregationTypeFor(
                requestParams.getAggregationType(), attributeValue != null || eventValue != null))
        .build();
  }

  /**
   * A {@code value} referencing a program-stage data element is qualified with a program and a
   * program stage ({@code programUid.programStageUid.dataElementUid}); a bare UID references a
   * tracked entity attribute.
   */
  private static boolean isProgramStageDataElement(String valueUid) {
    return valueUid.indexOf(DimensionIdentifierHelper.DIMENSION_SEPARATOR) >= 0;
  }

  /**
   * Validates the requested aggregation type: it must be one this endpoint supports, and a
   * non-COUNT type requires a {@code value} to aggregate over.
   *
   * @param requestParams the {@link CommonRequestParams}.
   * @throws IllegalQueryException if the aggregation type is unsupported or requires a missing
   *     value.
   */
  private void validateAggregationType(CommonRequestParams requestParams) {
    AggregationType aggregationType = requestParams.getAggregationType();
    if (aggregationType == null) {
      return;
    }

    if (!SUPPORTED_AGGREGATION_TYPES.contains(aggregationType)) {
      throw new IllegalQueryException(E7254, aggregationType.name());
    }

    if (aggregationType != AggregationType.COUNT && requestParams.getValue() == null) {
      throw new IllegalQueryException(E7255, aggregationType.name());
    }
  }

  /**
   * Resolves a bare value UID into a numeric attribute to aggregate over. The candidates are the
   * tracked entity type's own attributes and the attributes of the requested programs — the same
   * set the analytics_te table flattens into columns, so both resolve to a bare {@code t_1."<uid>"}
   * column. Data elements and program indicators are not accepted here.
   *
   * @param valueUid the requested value UID.
   * @param programs the requested programs.
   * @param trackedEntityType the {@link TrackedEntityType}.
   * @return the resolved {@link TrackedEntityAttribute}.
   * @throws IllegalQueryException if the UID does not refer to a numeric attribute of the tracked
   *     entity type or its programs.
   */
  private TrackedEntityAttribute resolveAttributeValue(
      String valueUid, List<Program> programs, TrackedEntityType trackedEntityType) {
    return Stream.concat(
            getTrackedEntityAttributes(trackedEntityType), getProgramAttributes(programs))
        .filter(attribute -> valueUid.equals(attribute.getUid()))
        .filter(attribute -> attribute.getValueType().isNumeric())
        .findFirst()
        .orElseThrow(() -> new IllegalQueryException(E7256, valueUid, trackedEntityType.getUid()));
  }

  /**
   * Resolves a {@code programUid.programStageUid.dataElementUid} value into the numeric
   * program-stage data element to aggregate over. The program must be one of the requested
   * programs, the stage must belong to that program, and the data element must belong to that stage
   * and be numeric. The stage segment may carry an offset ({@code [n]}) selecting which occurrence
   * to read; an offset on the program segment is not allowed.
   *
   * @param valueUid the fully qualified value in {@code programUid.programStageUid.dataElementUid}
   *     form.
   * @param programs the requested programs.
   * @return the resolved {@link EventValue}.
   * @throws IllegalQueryException if the value does not reference a numeric data element of a
   *     program stage of the requested programs.
   */
  private EventValue resolveEventValue(String valueUid, List<Program> programs) {
    StringDimensionIdentifier parsed;
    try {
      parsed = DimensionIdentifierHelper.fromFullDimensionId(valueUid);
    } catch (IllegalArgumentException e) {
      throw new IllegalQueryException(E7257, valueUid);
    }

    if (!parsed.getProgram().isPresent()
        || !parsed.getProgramStage().isPresent()
        || parsed.getProgram().hasOffset()) {
      throw new IllegalQueryException(E7257, valueUid);
    }

    String programUid = parsed.getProgram().getElement().getUid();
    String programStageUid = parsed.getProgramStage().getElement().getUid();
    String dataElementUid = parsed.getDimension().getUid();
    int offset = parsed.getProgramStage().getOffsetWithDefault();

    ProgramStage programStage =
        programs.stream()
            .filter(program -> programUid.equals(program.getUid()))
            .map(Program::getProgramStages)
            .flatMap(Collection::stream)
            .filter(stage -> programStageUid.equals(stage.getUid()))
            .findFirst()
            .orElseThrow(() -> new IllegalQueryException(E7257, valueUid));

    DataElement dataElement =
        programStage.getDataElements().stream()
            .filter(de -> dataElementUid.equals(de.getUid()))
            .filter(de -> de.getValueType().isNumeric())
            .findFirst()
            .orElseThrow(() -> new IllegalQueryException(E7257, valueUid));

    return new EventValue(programStage, dataElement, offset);
  }

  /**
   * Returns the aggregation function to apply: the requested one, falling back to AVERAGE when a
   * value is present — matching the event/enrollment aggregate contract. Without a value the
   * aggregate query counts TEIs, so no function is materialized.
   */
  private AggregationType aggregationTypeFor(AggregationType requested, boolean hasValue) {
    if (requested != null) {
      return requested;
    }

    return hasValue ? AggregationType.AVERAGE : null;
  }

  private Set<String> getProgramUidsFromTrackedEntityType(TrackedEntityType trackedEntityType) {
    return programService.getAllPrograms().stream()
        .filter(program -> matchesTet(program, trackedEntityType))
        .map(Program::getUid)
        .collect(toSet());
  }

  /**
   * Checks if the given programs are valid for the given tracked entity type.
   *
   * @param programs the collection of programs.
   * @param trackedEntityType the {@link TrackedEntityType}.
   */
  private void checkTetForPrograms(Set<String> programs, TrackedEntityType trackedEntityType) {
    Set<String> nonMatchingProgramUids =
        programService.getPrograms(programs).stream()
            .filter(program -> !matchesTet(program, trackedEntityType))
            .map(program -> program.getName() + " (" + program.getUid() + ")")
            .collect(toSet());

    if (isNotEmpty(nonMatchingProgramUids)) {
      throw new IllegalQueryException(
          E7142,
          nonMatchingProgramUids,
          trackedEntityType.getName() + " (" + trackedEntityType.getUid() + ")");
    }
  }

  /**
   * Checks if the given program TET is equals to tracked entity type.
   *
   * @param program the program.
   * @param trackedEntityType the tracked entity type uid.
   * @return true if the program matches the tracked entity type.
   */
  private boolean matchesTet(Program program, TrackedEntityType trackedEntityType) {
    return Objects.nonNull(program.getTrackedEntityType())
        && Strings.CS.equals(program.getTrackedEntityType().getUid(), trackedEntityType.getUid());
  }

  /**
   * Simply loads the given tracked entity type. If nothing is found, it throws an exception.
   *
   * @param trackedEntityTypeUid the tracked entity type uid.
   * @throws IllegalQueryException if the tracked entity type specified is invalid or non-existent.
   */
  private TrackedEntityType getTrackedEntityType(String trackedEntityTypeUid) {
    return Optional.of(trackedEntityTypeUid)
        .map(trackedEntityTypeService::getTrackedEntityType)
        .orElseThrow(() -> new IllegalQueryException(E7125, trackedEntityTypeUid));
  }
}
