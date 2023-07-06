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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.REPORT_MODE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.webapi.controller.exception.InvalidEnumValueException;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackerImportParamsValidatorTest {
  @Mock private ContextService contextService;

  @Test
  void testRequestIsValidWhenNoIdSchemeIsPresent() {
    Mockito.when(contextService.getParameterValuesMap()).thenReturn(Map.of());

    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder().contextService(contextService).build();

    assertDoesNotThrow(() -> TrackerImportParamsValidator.validateRequest(trackerImportRequest));
  }

  @ParameterizedTest
  @ValueSource(strings = {"NAME", "CODE", "UID", "ATTRIBUTE:f3JrwRTeSSz"})
  void testRequestIsValidWhenIdSchemeIsValid(String idScheme) {
    Mockito.when(contextService.getParameterValuesMap())
        .thenReturn(Map.of(ID_SCHEME_KEY.getKey(), List.of(idScheme)));

    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder().contextService(contextService).build();

    assertDoesNotThrow(() -> TrackerImportParamsValidator.validateRequest(trackerImportRequest));
  }

  @Test
  void throwExceptionWhenIdSchemeIsInvalid() {
    Mockito.when(contextService.getParameterValuesMap())
        .thenReturn(Map.of(ID_SCHEME_KEY.getKey(), List.of("INVALID")));

    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder().contextService(contextService).build();

    InvalidEnumValueException ex =
        assertThrows(
            InvalidEnumValueException.class,
            () -> TrackerImportParamsValidator.validateRequest(trackerImportRequest));

    assertEquals("INVALID", ex.getInvalidValue());
    assertEquals(ID_SCHEME_KEY.getKey(), ex.getFieldName());
    assertEquals(TrackerIdScheme.class, ex.getEnumKlass());
  }

  @ParameterizedTest
  @ValueSource(strings = {"FULL", "ERRORS", "WARNINGS"})
  void testRequestIsValidWhenReportModeIsValid(String reportMode) {
    Mockito.when(contextService.getParameterValuesMap())
        .thenReturn(Map.of(REPORT_MODE.getKey(), List.of(reportMode)));

    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder().contextService(contextService).build();

    assertDoesNotThrow(() -> TrackerImportParamsValidator.validateRequest(trackerImportRequest));
  }

  @Test
  void throwExceptionWhenReportModeIsInvalid() {
    Mockito.when(contextService.getParameterValuesMap())
        .thenReturn(Map.of(REPORT_MODE.getKey(), List.of("INVALID")));

    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder().contextService(contextService).build();

    InvalidEnumValueException ex =
        assertThrows(
            InvalidEnumValueException.class,
            () -> TrackerImportParamsValidator.validateRequest(trackerImportRequest));

    assertEquals("INVALID", ex.getInvalidValue());
    assertEquals(REPORT_MODE.getKey(), ex.getFieldName());
    assertEquals(TrackerBundleReportMode.class, ex.getEnumKlass());
  }
}
