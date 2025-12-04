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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Component("org.hisp.dhis.analytics.QueryValidator")
@RequiredArgsConstructor
public class DefaultQueryValidator implements QueryValidator {
  @Override
  public void validate(DataQueryParams params) throws IllegalQueryException {
    ErrorMessage error = validateForErrorMessage(params);

    if (error != null) {
      log.warn(
          String.format(
              "Analytics validation failed, code: '%s', message: '%s'",
              error.getErrorCode(), error.getMessage()));

      throw new IllegalQueryException(error);
    }
  }

  @Override
  public ErrorMessage validateForErrorMessage(DataQueryParams params) {
    if (params == null) {
      throw new IllegalQueryException(ErrorCode.E7100);
    }

    ErrorMessage error;

    // Validate data dimensions
    error = validateDataDimensions(params);
    if (error != null) {
      return error;
    }

    // Validate periods
    error = validatePeriods(params);
    if (error != null) {
      return error;
    }

    // Validate indicators and filters
    error = validateIndicatorsAndFilters(params);
    if (error != null) {
      return error;
    }

    // Validate dimensions consistency
    error = validateDimensionsConsistency(params);
    if (error != null) {
      return error;
    }

    // Validate data elements
    error = validateDataElements(params);
    if (error != null) {
      return error;
    }

    error = validateIndicators(params);
    if (error != null) {
      return error;
    }

    // Validate output format requirements
    error = validateOutputFormat(params);
    if (error != null) {
      return error;
    }

    return null;
  }

  /**
   * Validates data dimension requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validateDataDimensions(DataQueryParams params) {
    if (params.isSkipDataDimensionValidation()) {
      return null;
    }

    if (params.getDimensions().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7101);
    }

    if (!params.isSkipData()
        && params.getDataDimensionAndFilterOptions().isEmpty()
        && params.getAllDataElementGroups().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7102);
    }

    if (!params.getDimensionsAsFilters().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7103, getDimensions(params.getDimensionsAsFilters()));
    }

    return null;
  }

  /**
   * Validates period-related requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validatePeriods(DataQueryParams params) {
    if (!params.hasPeriods() && !params.isSkipPartitioning() && !params.hasStartEndDate()) {
      return new ErrorMessage(ErrorCode.E7104);
    }

    if (params.hasPeriods() && params.hasStartEndDate()) {
      return new ErrorMessage(ErrorCode.E7105);
    }

    if (params.hasStartEndDate() && params.startDateAfterEndDate()) {
      return new ErrorMessage(ErrorCode.E7106);
    }

    if (params.hasStartEndDate() && !params.getReportingRates().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7107);
    }

    return null;
  }

  /**
   * Validates indicators and filter requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validateIndicatorsAndFilters(DataQueryParams params) {
    if ((!params.getFilterIndicators().isEmpty()
            || !params.getFilterProgramIndicators().isEmpty()
            || !params.getFilterExpressionDimensionItems().isEmpty())
        && params.getFilterOptions(DATA_X_DIM_ID).size() > 1) {
      return new ErrorMessage(ErrorCode.E7108);
    }

    if (!params.getFilterReportingRates().isEmpty()
        && params.getFilterOptions(DATA_X_DIM_ID).size() > 1) {
      return new ErrorMessage(ErrorCode.E7109);
    }

    if (params.getFilters().contains(new BaseDimensionalObject(CATEGORYOPTIONCOMBO_DIM_ID))) {
      return new ErrorMessage(ErrorCode.E7110);
    }

    return null;
  }

  /**
   * Validates dimension consistency requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validateDimensionsConsistency(DataQueryParams params) {
    if (!params.getDuplicateDimensions().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7111, getDimensions(params.getDuplicateDimensions()));
    }

    if (!params.getAllReportingRates().isEmpty()
        && !params.containsOnlyDimensionsAndFilters(COMPLETENESS_DIMENSION_TYPES)) {
      return new ErrorMessage(ErrorCode.E7112, COMPLETENESS_DIMENSION_TYPES);
    }

    if (params.hasDimensionOrFilter(CATEGORYOPTIONCOMBO_DIM_ID)
        && params.getAllDataElements().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7113);
    }

    if (params.hasDimensionOrFilter(CATEGORYOPTIONCOMBO_DIM_ID)
        && (params.getAllDataElements().size() != params.getAllDataDimensionItems().size())) {
      return new ErrorMessage(ErrorCode.E7114);
    }

    return null;
  }

  /**
   * Validates data element requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validateDataElements(DataQueryParams params) {
    List<DimensionalItemObject> dataElements =
        Lists.newArrayList(params.getDataElementsOperandsProgramDataElements());
    List<DataElement> nonAggDataElements =
        FilterUtils.inverseFilter(
            asTypedList(dataElements), AggregatableDataElementFilter.INSTANCE);

    if (!nonAggDataElements.isEmpty()) {
      return new ErrorMessage(ErrorCode.E7115, getUids(nonAggDataElements));
    }

    if (!params.getSkipTotalDataElements().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7134);
    }

    return null;
  }

  private ErrorMessage validateIndicators(DataQueryParams params) {
    List<DimensionalItemObject> indicators = params.getAllIndicators();
    if (indicators.isEmpty()) return null;

    boolean hasPeriodOffset =
        indicators.stream().anyMatch(i -> ((Indicator) i).numeratorContainsPeriodOffset());
    boolean hasPeriodAsFilter = params.hasFilter(PERIOD_DIM_ID);
    if (hasPeriodOffset && hasPeriodAsFilter) {
      // Indicators having a numerator that uses ".periodOffset" are only supported
      // if the period dimension is not used as filter
      return new ErrorMessage(ErrorCode.E7152);
    }
    return null;
  }

  /**
   * Validates output format-specific requirements.
   *
   * @param params the {@link DataQueryParams} to validate
   * @return an {@link ErrorMessage} if validation fails, null otherwise
   */
  private ErrorMessage validateOutputFormat(DataQueryParams params) {
    if (!params.isOutputFormat(OutputFormat.DATA_VALUE_SET)) {
      return null;
    }

    if (!params.hasDimension(DATA_X_DIM_ID)) {
      return new ErrorMessage(ErrorCode.E7117);
    }

    if (!params.hasDimension(PERIOD_DIM_ID)) {
      return new ErrorMessage(ErrorCode.E7118);
    }

    if (!params.hasDimension(ORGUNIT_DIM_ID)) {
      return new ErrorMessage(ErrorCode.E7119);
    }

    return null;
  }

  @Override
  public void validateTableLayout(DataQueryParams params, List<String> columns, List<String> rows) {
    ErrorMessage violation = null;

    if (columns != null) {
      for (String column : columns) {
        if (!params.hasDimension(column)) {
          violation = new ErrorMessage(ErrorCode.E7126, column);
        }
      }
    }

    if (rows != null) {
      for (String row : rows) {
        if (!params.hasDimension(row)) {
          violation = new ErrorMessage(ErrorCode.E7127, row);
        }
      }
    }

    if (violation != null) {
      log.warn(String.format("Validation failed: %s", violation));

      throw new IllegalQueryException(violation);
    }
  }
}
