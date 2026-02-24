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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.feedback.ErrorCode.E7229;
import static org.hisp.dhis.feedback.ErrorCode.E7234;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsComparable;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service("org.hisp.dhis.analytics.event.EventQueryValidator")
@RequiredArgsConstructor
public class DefaultEventQueryValidator implements EventQueryValidator {

  private final SystemSettingsProvider settingsProvider;

  // -------------------------------------------------------------------------
  // EventQueryValidator implementation
  // -------------------------------------------------------------------------

  @Override
  public void validate(EventQueryParams params) throws IllegalQueryException {
    ErrorMessage error = validateForErrorMessage(params);

    if (error != null) {
      log.warn(
          "Event analytics validation failed, code: '{}', message: '{}'",
          error.getErrorCode(),
          error.getMessage());

      throw new IllegalQueryException(error);
    }
  }

  @Override
  public ErrorMessage validateForErrorMessage(EventQueryParams params) {

    if (params == null) {
      throw new IllegalQueryException(ErrorCode.E7100);
    }
    if (!params.hasOrganisationUnits() && !params.hasEnrollmentOu()) {
      return new ErrorMessage(ErrorCode.E7200);
    }
    if (!params.getDuplicateDimensions().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7201, params.getDuplicateDimensions());
    }

    // Check for duplicate stage dimension identifiers (must be before E7202 check)
    // This applies to both EVENT and ENROLLMENT queries
    Set<String> duplicateStageDimensions = params.getDuplicateStageDimensionIdentifiers();
    if (!duplicateStageDimensions.isEmpty()) {
      return new ErrorMessage(ErrorCode.E7243, duplicateStageDimensions.iterator().next());
    }

    if (!params.getDuplicateQueryItems().isEmpty()) {
      return new ErrorMessage(ErrorCode.E7202, params.getDuplicateQueryItems());
    }
    if (params.hasValueDimension()
        && params.getDimensionalObjectItems().contains(params.getValue())) {
      return new ErrorMessage(ErrorCode.E7203);
    }
    if (params.hasAggregationType() && !(params.hasValueDimension() || params.isAggregateData())) {
      return new ErrorMessage(ErrorCode.E7204);
    }
    if (!params.hasPeriods()
        && (params.getStartDate() == null || params.getEndDate() == null)
        && !hasDateQueryItem(params)) {
      return new ErrorMessage(ErrorCode.E7205);
    }
    if (params.getStartDate() != null
        && params.getEndDate() != null
        && params.getStartDate().after(params.getEndDate())) {
      return new ErrorMessage(
          ErrorCode.E7206, toMediumDate(params.getStartDate()), toMediumDate(params.getEndDate()));
    }
    if (params.getPage() != null && params.getPage() <= 0) {
      return new ErrorMessage(ErrorCode.E7207, params.getPage());
    }
    if (params.getPageSize() != null && params.getPageSize() < 0) {
      return new ErrorMessage(ErrorCode.E7208, params.getPageSize());
    }
    if (params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit()) {
      return new ErrorMessage(ErrorCode.E7209, params.getLimit(), getMaxLimit());
    }
    if (params.hasTimeField() && !params.timeFieldIsValid()) {
      return new ErrorMessage(ErrorCode.E7210, params.getTimeField());
    }
    if (!params.orgUnitFieldIsValid()) {
      return new ErrorMessage(ErrorCode.E7211, params.getOrgUnitField());
    }
    if (params.hasClusterSize() && params.getClusterSize() <= 0) {
      return new ErrorMessage(ErrorCode.E7212, params.getClusterSize());
    }
    if (params.hasBbox() && !ValidationUtils.bboxIsValid(params.getBbox())) {
      return new ErrorMessage(ErrorCode.E7213, params.getBbox());
    }
    if ((params.hasBbox() || params.hasClusterSize()) && params.getCoordinateFields() == null) {
      return new ErrorMessage(ErrorCode.E7214);
    }

    // Stage parameter cannot be used with stage-specific dimension identifiers
    if (params.hasProgramStage() && params.hasStageSpecificItem()) {
      return new ErrorMessage(ErrorCode.E7241);
    }

    if (params.getEndpointItem() != null
        && params.getEndpointItem().equals(RequestTypeAware.EndpointItem.EVENT)) {
      // Stage-prefixed dimensions must all use the same stage for EVENT queries
      Set<ProgramStage> distinctStages = params.getDistinctStages();
      if (distinctStages.size() > 1) {
        String stages =
            distinctStages.stream()
                .map(ProgramStage::getUid)
                .sorted()
                .collect(Collectors.joining(", "));
        return new ErrorMessage(ErrorCode.E7244, stages);
      }
    }

    // Period dimension cannot be used with stage-specific date dimensions
    // Only applies when user explicitly requests period dimension (not default periods)
    if (hasExplicitPeriodDimension(params) && params.hasStageDateItem()) {
      return new ErrorMessage(ErrorCode.E7242);
    }

    // Validate stage-specific categories and COGS
    ErrorMessage stageSpecificDimError = validateStageSpecificDimensions(params);
    if (stageSpecificDimError != null) {
      return stageSpecificDimError;
    }

    for (QueryItem item : params.getItemsAndItemFilters()) {
      if (item.hasLegendSet() && item.hasOptionSet()) {
        return new ErrorMessage(ErrorCode.E7215, item.getItemId());
      } else if (params.isAggregateData() && !item.getAggregationType().isAggregatable()) {
        return new ErrorMessage(ErrorCode.E7216, item.getItemId());
      } else {
        for (QueryFilter filter : item.getFilters()) {
          ErrorMessage error = validateQueryFilter(filter, item.getValueType());
          if (error != null) {
            return error;
          }
        }
      }
    }

    // TODO validate coordinate field
    return null;
  }

  private boolean hasDateQueryItem(EventQueryParams params) {
    return params.getItems().stream()
        .anyMatch(
            item ->
                EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(item.getItemId())
                    || EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME.equals(item.getItemId())
                    || EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME.equals(
                        item.getItemId())
                    || EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME.equals(item.getItemId())
                    || EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME.equals(item.getItemId())
                    || EventAnalyticsColumnName.CREATED_DATE_COLUMN_NAME.equals(item.getItemId())
                    || EventAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME.equals(item.getItemId())
                    || EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME.equals(
                        item.getItemId())
                    || EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(
                        item.getItemId())
                    || EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME.equals(
                        item.getItemId())
                    || EnrollmentAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME.equals(
                        item.getItemId()));
  }

  /**
   * Checks if the query has an explicitly requested period dimension. Auto-added default periods
   * should not trigger the E7242 validation. Only returns true if user explicitly requested a
   * period dimension.
   *
   * <p>For enrollment queries, the system auto-adds a period with dateField=ENROLLMENT_DATE when
   * the user doesn't specify a period. This auto-added period should not trigger E7242.
   */
  private boolean hasExplicitPeriodDimension(EventQueryParams params) {
    // If no period dimension exists, it's not explicit
    if (!EventPeriodUtils.hasPeriodDimension(params)) {
      return false;
    }

    // Check if all periods are auto-added enrollment defaults
    // Only enrollment queries can have auto-added periods with ENROLLMENT_DATE
    return !hasAllEnrollmentAutoAddedPeriods(params);
  }

  /**
   * Checks if all period items are auto-added enrollment defaults. Auto-added periods for
   * enrollment queries have dateField=ENROLLMENT_DATE. Returns false if any period has a different
   * dateField (including null, which indicates user-specified period).
   */
  private boolean hasAllEnrollmentAutoAddedPeriods(EventQueryParams params) {
    DimensionalObject period = params.getDimension(PERIOD_DIM_ID);
    if (period == null) {
      return true;
    }

    for (DimensionalItemObject item : period.getItems()) {
      PeriodDimension p = (PeriodDimension) item;
      String dateField = p.getDateField();

      // Auto-added enrollment periods have dateField=ENROLLMENT_DATE
      // Any other value (including null) means user-specified
      if (!TimeField.ENROLLMENT_DATE.name().equals(dateField)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Validates stage-specific dimensions (categories and COGS). Checks that the program stage
   * belongs to the query's program.
   *
   * @param params the {@link EventQueryParams}.
   * @return an {@link ErrorMessage} if validation fails, null otherwise.
   */
  private ErrorMessage validateStageSpecificDimensions(EventQueryParams params) {
    if (!params.hasProgram()) {
      return null;
    }

    Program program = params.getProgram();

    // Check dimensions and filters for stage-specific categories/COGS
    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(
            Set.of(DimensionType.CATEGORY, DimensionType.CATEGORY_OPTION_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      if (dim.getProgramStage() != null) {
        ProgramStage dimProgramStage = dim.getProgramStage();

        // Validate that the stage belongs to the program
        if (!dimProgramStage.getProgram().equals(program)) {
          return new ErrorMessage(ErrorCode.E7245, dimProgramStage.getUid(), program.getUid());
        }
      }
    }

    return null;
  }

  /**
   * Validates the full {@link QueryFilter} based on the associated item's {@link ValueType}.
   *
   * @param filter the {@link QueryFilter}.
   * @param valueType the {@link ValueType}.
   * @return the validation {@link ErrorMessage}, or null if no error is found.
   */
  private ErrorMessage validateQueryFilter(QueryFilter filter, ValueType valueType) {
    String filterValue = trimToEmpty(filter.getFilter());
    ErrorMessage errorMessage = null;

    if (filter.getOperator().isIn()) {
      // A filter value may contain multiple options, ie.: 1;0;NV.
      Set<String> filterValues = Set.of(filterValue.split(";"));

      for (String f : filterValues) {
        errorMessage = validateFilterValue(IN, valueType, f);
        if (errorMessage != null) {
          return errorMessage;
        }
      }
    } else {
      errorMessage = validateFilterValue(filter.getOperator(), valueType, filterValue);
    }

    return errorMessage;
  }

  /**
   * Validates a single filter value based on its {@link ValueType} and {@link QueryOperator}.
   *
   * @param operator the {@link QueryOperator}.
   * @param valueType the {@link ValueType}.
   * @param filterValue the filter value.
   * @return the validation {@link ErrorMessage}, or null if no error is found.
   */
  private ErrorMessage validateFilterValue(
      QueryOperator operator, ValueType valueType, String filterValue) {
    if (!operator.isNullAllowed() && filterValue.contains(NV)) {
      return new ErrorMessage(E7229, operator.getValue());
    } else if (!filterValue.contains(NV)
        && !valueIsComparable(convertFilterValue(valueType, filterValue), valueType)) {
      return new ErrorMessage(E7234, filterValue, valueType);
    }

    return null;
  }

  /**
   * Some filter values may require some conversion, so they can be correctly evaluated and properly
   * validated. This method will provide the conversion needed for each {@link ValueType} if
   * applicable.
   *
   * @param valueType the {@link ValueType}.
   * @param filterValue the value to be converted.
   * @return the converted value or else the filter value provided.
   */
  private String convertFilterValue(ValueType valueType, String filterValue) {
    switch (valueType) {
      case TIME:
      case DATETIME:
        return replaceDateTimeSeparators(filterValue);
      default:
        return filterValue;
    }
  }

  /**
   * Based on the given input, this method will replace the first two ".", by ":". ie:
   *
   * <p>"12.02" -> "12:02", "2023-12-25T12.02.00" -> "2023-12-25T12:02:00"
   *
   * <p>This is required because of the URL params uses "." as separator, so it does not clash with
   * the character ":", used by dimensions. But internally, the date/time masks requires the
   * separator ":".
   *
   * @param dateTime time, or date/time.
   * @return the value with the correct separators.
   */
  private String replaceDateTimeSeparators(String dateTime) {
    return Strings.CS.replace(dateTime, ".", ":", 2);
  }

  @Override
  public void validateTableLayout(
      EventQueryParams params, List<String> columns, List<String> rows) {
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
      log.warn("Validation failed: {}", violation);

      throw new IllegalQueryException(violation);
    }
  }

  @Override
  public int getMaxLimit() {
    return settingsProvider.getCurrentSettings().getAnalyticsMaxLimit();
  }
}
