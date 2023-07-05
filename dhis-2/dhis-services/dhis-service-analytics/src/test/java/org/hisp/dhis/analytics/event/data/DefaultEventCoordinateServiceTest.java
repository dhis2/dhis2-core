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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_GEOMETRY_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @ValueSource(strings = {"psigeometry", "pigeometry", "ougeometry"})
  void testGetCoordinateFieldOrFail(String geometry) {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertEquals(geometry, service.getCoordinateField("A", geometry, ErrorCode.E7232));
  }

  @Test
  void testGetFallbackCoordinateFieldsWithFallbackCoordinateFieldParam() {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertEquals(COL_NAME_GEOMETRY_LIST, service.getFallbackCoordinateFields("A", null, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"psigeometry", "pigeometry", "teigeometry", "ougeometry"})
  void testGetFallbackCoordinateFieldsWithoutFallbackCoordinateFieldParam(String geometry) {
    when(programService.getProgram(any(String.class))).thenReturn(createProgram('A'));

    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertEquals(List.of(geometry), service.getFallbackCoordinateFields("A", geometry, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"psigeometry", "pigeometry", "teigeometry", "ougeometry"})
  void testVerifyFallbackCoordinateFieldWithRegistrationProgram(String geometry) {
    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertTrue(service.isFallbackCoordinateFieldValid(true, geometry));
  }

  @ParameterizedTest
  @ValueSource(strings = {"psigeometry", "pigeometry", "ougeometry"})
  void testVerifyFallbackCoordinateFieldWithoutRegistrationProgram(String geometry) {
    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertTrue(service.isFallbackCoordinateFieldValid(false, geometry));
  }

  @ParameterizedTest
  @ValueSource(strings = {"badpsigeometry", "badpigeometry", "badteigeometry", "badougeometry"})
  void testVerifyBadFallbackCoordinateField(String geometry) {
    when(dataElementService.getDataElement(any(String.class))).thenReturn(null);

    when(attributeService.getTrackedEntityAttribute(any(String.class))).thenReturn(null);

    EventCoordinateService service =
        new DefaultEventCoordinateService(programService, dataElementService, attributeService);

    assertThrows(
        IllegalQueryException.class, () -> service.isFallbackCoordinateFieldValid(false, geometry));
  }
}
