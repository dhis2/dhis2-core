/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.query.jsonextractor;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType;
import org.hisp.dhis.common.IllegalQueryException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * A {@link SqlRowSet} implementation that extracts values from a JSON string column in the wrapped
 * {@link SqlRowSet} and returns them as if they were columns in the row set.
 */
public class AggregatedJsonExtractingSqlRowSet extends DelegatingSqlRowSet {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final Comparator<JsonEnrollment> ENR_ENROLLMENT_DATE_COMPARATOR =
      comparing(JsonEnrollment::getEnrollmentDate, nullsFirst(naturalOrder())).reversed();

  public static final Comparator<JsonEnrollment.JsonEvent> EVT_EXECUTION_DATE_COMPARATOR =
      comparing(JsonEnrollment.JsonEvent::getExecutionDate, nullsFirst(naturalOrder())).reversed();

  private final transient Map<String, DimensionIdentifier<DimensionParam>> dimIdByKey;

  private final List<String> existingColumnsInRowSet;

  public AggregatedJsonExtractingSqlRowSet(
      SqlRowSet sqlRowSet, List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers) {
    super(sqlRowSet);
    this.dimIdByKey = new HashMap<>();
    for (DimensionIdentifier<DimensionParam> dimensionIdentifier : dimensionIdentifiers) {
      if (!dimIdByKey.containsKey(dimensionIdentifier.getKey())) {
        dimIdByKey.put(dimensionIdentifier.getKey(), dimensionIdentifier);
      }
    }
    // we need to know which columns are in the sqlrowset, so that when a column is not present, we
    // can check if it is present in the json string
    this.existingColumnsInRowSet = Arrays.asList(sqlRowSet.getMetaData().getColumnNames());
  }

  @Override
  @SneakyThrows
  public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
    // if the column is present in the rowset, we invoke the default behavior
    if (existingColumnsInRowSet.contains(columnLabel)) {
      return super.getObject(columnLabel);
    }
    // if the column is not present in the rowset, we check if it is present in the json string
    List<JsonEnrollment> enrollments =
        OBJECT_MAPPER.readValue(super.getString("enrollments"), new TypeReference<>() {});

    DimensionIdentifier<DimensionParam> dimensionIdentifier = dimIdByKey.get(columnLabel);

    if (dimensionIdentifier.isEnrollmentDimension()) {
      return enrollments.stream()
          // gets only enrollments whose program is the same as specified in the dimension
          .filter(
              jsonEnrollment ->
                  jsonEnrollment
                      .getProgramUid()
                      .equals(dimensionIdentifier.getProgram().getElement().getUid()))
          // sorts enrollments by enrollment date, descending
          .sorted(ENR_ENROLLMENT_DATE_COMPARATOR)
          // skips the number of enrollments specified in the dimension (offset)
          .skip(dimensionIdentifier.getProgram().getOffsetWithDefault())
          .findFirst()
          // extracts the value of the dimension from the enrollment
          .map(
              jsonEnrollment ->
                  getEnrollmentExtractor(dimensionIdentifier.getDimension()).apply(jsonEnrollment))
          .orElse(null);
    }

    if (dimensionIdentifier.isEventDimension()) {
      return enrollments.stream()
          // gets only enrollments whose program is the same as specified in the dimension
          .filter(
              jsonEnrollment ->
                  jsonEnrollment
                      .getProgramUid()
                      .equals(dimensionIdentifier.getProgram().getElement().getUid()))
          // sorts enrollments by enrollment date, descending
          .sorted(ENR_ENROLLMENT_DATE_COMPARATOR)
          // skips the number of enrollments specified in the dimension (offset)
          .skip(dimensionIdentifier.getProgram().getOffsetWithDefault())
          .findFirst()
          .map(JsonEnrollment::getEvents)
          .stream()
          .flatMap(Collection::stream)
          // gets only events whose program stage is the same as specified in the dimension
          .filter(
              jsonEvent ->
                  jsonEvent
                      .getProgramStageUid()
                      .equals(dimensionIdentifier.getProgramStage().getElement().getUid()))
          // sorts events by execution date, descending
          .sorted(EVT_EXECUTION_DATE_COMPARATOR)
          // skips the number of events specified in the dimension (offset)
          .skip(dimensionIdentifier.getProgramStage().getOffsetWithDefault())
          .findFirst()
          // extracts the value of the dimension from the event
          .map(jsonEvent -> getEventExtractor(dimensionIdentifier.getDimension()).apply(jsonEvent))
          .orElse(null);
    }
    throw new IllegalStateException("Unknown dimension identifier " + dimensionIdentifier);
  }

  /**
   * Returns a function that extracts the value of the dimension from the event.
   *
   * @param dimension the dimension
   * @return the function to extract the value of the dimension from the event
   */
  @SuppressWarnings("unchecked")
  private Function<JsonEnrollment.JsonEvent, Object> getEventExtractor(DimensionParam dimension) {
    if (dimension.isStaticDimension()) {
      return EventExtractor.byDimension(dimension.getStaticDimension()).getExtractor();
    }
    if (dimension.getDimensionParamObjectType().equals(DimensionParamObjectType.DATA_ELEMENT)) {
      // it is a data element dimension here
      String dataElementUid = dimension.getQueryItem().getItemId();
      return jsonEvent -> {
        Map<String, Object> dataValue =
            (Map<String, Object>) jsonEvent.getEventDataValues().get(dataElementUid);
        return Objects.nonNull(dataValue) ? dataValue.get("value") : null;
      };
    }
    throw new IllegalStateException("Unknown dimension identifier " + dimension);
  }

  /**
   * Returns a function that extracts the value of the dimension from the enrollment.
   *
   * @param dimension the dimension
   * @return the function to extract the value of the dimension from the enrollment
   */
  private Function<JsonEnrollment, Object> getEnrollmentExtractor(DimensionParam dimension) {
    if (dimension.isStaticDimension()) {
      return EnrollmentExtractor.byDimension(dimension.getStaticDimension()).getExtractor();
    }
    throw new IllegalQueryException("Unsupported enrollment dimension " + dimension);
  }
}
