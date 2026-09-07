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

import static org.hisp.dhis.analytics.table.ColumnPostfix.OU_GEOMETRY_COL_POSTFIX;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Service;

/**
 * @author Dusan Bernat
 */
@Service
@RequiredArgsConstructor
public class DefaultEventCoordinateService implements EventCoordinateService {
  public static final String COL_NAME_ENROLLMENT_GEOMETRY = "enrollmentgeometry";

  public static final String COL_NAME_EVENT_GEOMETRY = "eventgeometry";

  public static final String COL_NAME_TRACKED_ENTITY_GEOMETRY = "tegeometry";

  public static final String COL_NAME_OU_GEOMETRY = "ougeometry";

  public static final List<String> COL_NAME_GEOMETRY_LIST =
      List.of(
          COL_NAME_EVENT_GEOMETRY,
          COL_NAME_ENROLLMENT_GEOMETRY,
          COL_NAME_TRACKED_ENTITY_GEOMETRY,
          COL_NAME_OU_GEOMETRY);

  public static final List<String> COL_NAME_PROGRAM_NO_REGISTRATION_GEOMETRY_LIST =
      List.of(COL_NAME_EVENT_GEOMETRY, COL_NAME_ENROLLMENT_GEOMETRY, COL_NAME_OU_GEOMETRY);

  private final ProgramService programService;

  private final DataElementService dataElementService;

  private final TrackedEntityAttributeService attributeService;

  @Override
  public boolean isFallbackCoordinateFieldValid(boolean isRegistration, String coordinateField) {
    if (coordinateField == null) {
      return false;
    }

    resolveAndValidateFallbackColumn(isRegistration, coordinateField);
    return true;
  }

  @Override
  public List<String> getFallbackCoordinateFields(
      String program, String fallbackCoordinateField, boolean defaultCoordinateFallback) {
    Program pr = programService.getProgram(program);

    List<String> fallbackCoordinateFields = new ArrayList<>();

    if (fallbackCoordinateField != null) {
      fallbackCoordinateFields.add(
          resolveAndValidateFallbackColumn(pr.isRegistration(), fallbackCoordinateField));
    } else if (defaultCoordinateFallback) {
      List<String> items =
          new ArrayList<>(
              pr.isRegistration()
                  ? COL_NAME_GEOMETRY_LIST
                  : COL_NAME_PROGRAM_NO_REGISTRATION_GEOMETRY_LIST);

      fallbackCoordinateFields.addAll(items);
    }

    return fallbackCoordinateFields;
  }

  /**
   * Validates a fallback coordinate field and resolves it to the analytics table column to use in
   * SQL. Built-in geometry fields are returned unchanged, coordinate DE/TEA fields are returned
   * unchanged, and organisation unit DE/TEA fields are resolved to their generated geometry column.
   *
   * @param isRegistration true when the program has registration.
   * @param fallbackCoordinateField the requested fallback coordinate field.
   * @return the analytics table column name to use for the fallback field, or {@code null} when the
   *     fallback field is {@code null}.
   * @throws org.hisp.dhis.common.IllegalQueryException if the field is unknown, is not valid for
   *     the program type, or has an unsupported value type.
   */
  private String resolveAndValidateFallbackColumn(
      boolean isRegistration, String fallbackCoordinateField) {
    if (fallbackCoordinateField == null) {
      return null;
    }

    if (COL_NAME_TRACKED_ENTITY_GEOMETRY.equals(fallbackCoordinateField)) {
      if (!isRegistration) {
        throwIllegalQueryEx(ErrorCode.E7232, fallbackCoordinateField);
      }

      return fallbackCoordinateField;
    }

    if (COL_NAME_PROGRAM_NO_REGISTRATION_GEOMETRY_LIST.contains(fallbackCoordinateField)) {
      return fallbackCoordinateField;
    }

    DataElement dataElement = dataElementService.getDataElement(fallbackCoordinateField);
    if (dataElement != null) {
      return validateCoordinateField(
          dataElement.getValueType(), fallbackCoordinateField, ErrorCode.E7219);
    }

    TrackedEntityAttribute attribute =
        attributeService.getTrackedEntityAttribute(fallbackCoordinateField);
    if (attribute != null) {
      return validateCoordinateField(
          attribute.getValueType(), fallbackCoordinateField, ErrorCode.E7220);
    }

    throwIllegalQueryEx(ErrorCode.E7232, fallbackCoordinateField);

    return null;
  }

  @Override
  public String validateCoordinateField(ValueType valueType, String field, ErrorCode errorCode) {
    if (ValueType.COORDINATE != valueType && ValueType.ORGANISATION_UNIT != valueType) {
      throwIllegalQueryEx(errorCode, field);
    }

    if (ValueType.ORGANISATION_UNIT == valueType) {
      // Append the "_geom" suffix to the field
      // so that the correct geometry column
      // is selected
      return field + OU_GEOMETRY_COL_POSTFIX;
    }
    return field;
  }

  @Override
  public String validateCoordinateField(String program, String field, ErrorCode errorCode) {
    Program pr = programService.getProgram(program);

    if (COL_NAME_TRACKED_ENTITY_GEOMETRY.equals(field) && !pr.isRegistration()) {
      throwIllegalQueryEx(errorCode, field);
    }

    return field;
  }
}
