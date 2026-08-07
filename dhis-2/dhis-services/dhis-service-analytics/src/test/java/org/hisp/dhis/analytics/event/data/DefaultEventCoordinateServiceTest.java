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

import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_GEOMETRY_LIST;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DefaultEventCoordinateService.
 *
 * @author Dusan Bernat
 */
@ExtendWith(MockitoExtension.class)
class DefaultEventCoordinateServiceTest {
  @Mock private ProgramService programService;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityAttributeService attributeService;

  @InjectMocks private DefaultEventCoordinateService service;

  @ParameterizedTest
  @ValueSource(strings = {"eventgeometry", "enrollmentgeometry", "ougeometry"})
  void testGetCoordinateFieldOrFail(String geometry) {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    assertEquals(geometry, service.validateCoordinateField("A", geometry, ErrorCode.E7232));
  }

  @Test
  void testGetFallbackCoordinateFieldsWithFallbackCoordinateFieldParam() {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    assertEquals(COL_NAME_GEOMETRY_LIST, service.getFallbackCoordinateFields("A", null, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"enrollmentgeometry", "eventgeometry", "tegeometry", "ougeometry"})
  void testGetFallbackCoordinateFieldsWithoutFallbackCoordinateFieldParam(String geometry) {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    assertEquals(List.of(geometry), service.getFallbackCoordinateFields("A", geometry, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"enrollmentgeometry", "eventgeometry", "tegeometry", "ougeometry"})
  void testVerifyFallbackCoordinateFieldWithRegistrationProgram(String geometry) {
    assertTrue(service.isFallbackCoordinateFieldValid(true, geometry));
  }

  @ParameterizedTest
  @ValueSource(strings = {"enrollmentgeometry", "eventgeometry", "ougeometry"})
  void testVerifyFallbackCoordinateFieldWithoutRegistrationProgram(String geometry) {
    assertTrue(service.isFallbackCoordinateFieldValid(false, geometry));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"badeventgeometry", "badenrollmentgeometry", "badtegeometry", "badougeometry"})
  void testVerifyBadFallbackCoordinateField(String geometry) {
    when(dataElementService.getDataElement(any(String.class))).thenReturn(null);
    when(attributeService.getTrackedEntityAttribute(any(String.class))).thenReturn(null);

    assertThrows(
        IllegalQueryException.class, () -> service.isFallbackCoordinateFieldValid(false, geometry));
  }

  @Test
  void testFallbackCoordinateFieldWithDataElementOfTypeOrgUnitAppendsGeomSuffix() {
    String uid = "deUid000001";
    DataElement de = new DataElement();
    de.setValueType(ValueType.ORGANISATION_UNIT);

    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));
    when(dataElementService.getDataElement(uid)).thenReturn(de);

    assertEquals(List.of(uid + "_geom"), service.getFallbackCoordinateFields("A", uid, false));
  }

  @Test
  void testFallbackCoordinateFieldWithDataElementOfTypeCoordinateIsReturnedUnchanged() {
    String uid = "deUid000002";
    DataElement de = new DataElement();
    de.setValueType(ValueType.COORDINATE);

    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));
    when(dataElementService.getDataElement(uid)).thenReturn(de);

    assertEquals(List.of(uid), service.getFallbackCoordinateFields("A", uid, false));
  }

  @Test
  void testFallbackCoordinateFieldWithDataElementOfUnsupportedTypeThrows() {
    String uid = "deUid000003";
    DataElement de = new DataElement();
    de.setValueType(ValueType.TEXT);

    when(dataElementService.getDataElement(uid)).thenReturn(de);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class, () -> service.isFallbackCoordinateFieldValid(false, uid));
    assertEquals(ErrorCode.E7219, ex.getErrorCode());
  }

  @Test
  void testFallbackCoordinateFieldWithAttributeOfTypeOrgUnitAppendsGeomSuffix() {
    String uid = "teaUid00001";
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setValueType(ValueType.ORGANISATION_UNIT);

    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));
    when(dataElementService.getDataElement(uid)).thenReturn(null);
    when(attributeService.getTrackedEntityAttribute(uid)).thenReturn(tea);

    assertEquals(List.of(uid + "_geom"), service.getFallbackCoordinateFields("A", uid, false));
  }

  @Test
  void testFallbackCoordinateFieldWithAttributeOfTypeCoordinateIsReturnedUnchanged() {
    String uid = "teaUid00002";
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setValueType(ValueType.COORDINATE);

    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));
    when(dataElementService.getDataElement(uid)).thenReturn(null);
    when(attributeService.getTrackedEntityAttribute(uid)).thenReturn(tea);

    assertEquals(List.of(uid), service.getFallbackCoordinateFields("A", uid, false));
  }

  @Test
  void testFallbackCoordinateFieldWithAttributeOfUnsupportedTypeThrows() {
    String uid = "teaUid00003";
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setValueType(ValueType.TEXT);

    when(dataElementService.getDataElement(uid)).thenReturn(null);
    when(attributeService.getTrackedEntityAttribute(uid)).thenReturn(tea);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class, () -> service.isFallbackCoordinateFieldValid(false, uid));
    assertEquals(ErrorCode.E7220, ex.getErrorCode());
  }
}
