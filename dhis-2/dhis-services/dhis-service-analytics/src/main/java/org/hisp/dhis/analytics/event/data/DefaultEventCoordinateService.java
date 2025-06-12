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

import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
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

    if (COL_NAME_TRACKED_ENTITY_GEOMETRY.equals(coordinateField)) {
      return isRegistration;
    }

    if (COL_NAME_PROGRAM_NO_REGISTRATION_GEOMETRY_LIST.contains(coordinateField)) {
      return true;
    }

    DataElement dataElement = dataElementService.getDataElement(coordinateField);

    TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute(coordinateField);

    if (dataElement != null || attribute != null) {
      return true;
    }

    throwIllegalQueryEx(ErrorCode.E7232, coordinateField);

    return false;
  }

  @Override
  public List<String> getFallbackCoordinateFields(
      String program, String fallbackCoordinateField, boolean defaultCoordinateFallback) {
    Program pr = programService.getProgram(program);

    List<String> fallbackCoordinateFields = new ArrayList<>();

    if (fallbackCoordinateField != null) {
      if (!isFallbackCoordinateFieldValid(pr.isRegistration(), fallbackCoordinateField)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7232, fallbackCoordinateField));
      }

      fallbackCoordinateFields.add(fallbackCoordinateField);
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

  @Override
  public String validateCoordinateField(ValueType valueType, String field, ErrorCode errorCode) {
    if (ValueType.COORDINATE != valueType && ValueType.ORGANISATION_UNIT != valueType) {
      throwIllegalQueryEx(errorCode, field);
    }

    if (ValueType.ORGANISATION_UNIT == valueType) {
      // Append the "_geom" suffix to the field
      // so that the correct geometry column
      // is selected
      return field + OU_GEOMETRY_COL_SUFFIX;
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
