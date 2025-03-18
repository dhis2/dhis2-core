/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonRequestParamsParserTest {
  private SystemSettingsProvider settingsProvider = mock(SystemSettingsProvider.class);
  private SystemSettings settings = mock(SystemSettings.class);

  private DataQueryService dataQueryService = mock(DataQueryService.class);

  private EventDataQueryService eventDataQueryService = mock(EventDataQueryService.class);

  private ProgramService programService = mock(ProgramService.class);

  private DimensionIdentifierConverter dimensionIdentifierConverter =
      mock(DimensionIdentifierConverter.class);

  private final CommonRequestParamsParser commonRequestParamsParser =
      new CommonRequestParamsParser(
          settingsProvider,
          dataQueryService,
          eventDataQueryService,
          programService,
          dimensionIdentifierConverter);

  @BeforeEach
  void setUp() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
  }

  @Test
  void testPaginationPagingTruePageSizeHigherThanMaxLimit() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(1000);
    CommonRequestParams request = new CommonRequestParams().withPaging(true).withPageSize(10000);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertEquals(1000, parsed.getPagingParams().getPageSize());
    assertTrue(parsed.getPagingParams().getPaging());
  }

  @Test
  void testPaginationPagingTruePageSizeLowerThanMaxLimit() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(1000);
    CommonRequestParams request = new CommonRequestParams().withPaging(true).withPageSize(100);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertEquals(100, parsed.getPagingParams().getPageSize());
    assertTrue(parsed.getPagingParams().getPaging());
  }

  @Test
  void testUnlimitedMaxLimit0() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(0);
    CommonRequestParams request = new CommonRequestParams().withPaging(false);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertFalse(parsed.getPagingParams().isPaging());
  }

  @Test
  void testUnlimitedIgnoreLimit() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(100);
    CommonRequestParams request = new CommonRequestParams().withIgnoreLimit(true).withPaging(false);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertFalse(parsed.getPagingParams().isPaging());
  }

  @Test
  void testPagingFalseAndPageSizeGreaterThanMaxLimit() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(100);
    CommonRequestParams request = new CommonRequestParams().withPageSize(150).withPaging(false);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertFalse(parsed.getPagingParams().isPaging());
    assertFalse(parsed.getPagingParams().isUnlimited());
    assertEquals(100, parsed.getPagingParams().getPageSize());
  }

  @Test
  void testPagingFalseAndPageSizeLowerThanMaxLimit() {
    when(settings.getAnalyticsMaxLimit()).thenReturn(100);
    CommonRequestParams request = new CommonRequestParams().withPageSize(50).withPaging(false);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertFalse(parsed.getPagingParams().isPaging());
    assertFalse(parsed.getPagingParams().isUnlimited());
    assertEquals(100, parsed.getPagingParams().getPageSize());
  }

  @Test
  void testPagingFalseAndNoMaxLimit() {
    int unlimited = 0;

    when(settings.getAnalyticsMaxLimit()).thenReturn(unlimited);
    CommonRequestParams request = new CommonRequestParams().withPageSize(50).withPaging(false);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertFalse(parsed.getPagingParams().isPaging());
    assertTrue(parsed.getPagingParams().isUnlimited());
    assertEquals(50, parsed.getPagingParams().getPageSize());
  }

  @Test
  void testPagingTrueAndNoMaxLimit() {
    int unlimited = 0;

    when(settings.getAnalyticsMaxLimit()).thenReturn(unlimited);
    CommonRequestParams request = new CommonRequestParams().withPageSize(50).withPaging(true);
    CommonParsedParams parsed = commonRequestParamsParser.parse(request);
    assertTrue(parsed.getPagingParams().isPaging());
    assertFalse(parsed.getPagingParams().isUnlimited());
    assertEquals(50, parsed.getPagingParams().getPageSize());
  }

  @Test
  void testProgramStatusWrongFormat() {
    CommonRequestParams request = new CommonRequestParams().withProgramStatus(Set.of("COMPLETED"));
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));

    assertEquals(
        "Parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
        exception.getMessage());
  }

  @Test
  void testEnrollmentStatusWrongFormat() {
    CommonRequestParams request =
        new CommonRequestParams().withEnrollmentStatus(Set.of("COMPLETED"));
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));

    assertEquals(
        "Parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
        exception.getMessage());
  }

  @Test
  void testEventStatusWrongFormat() {
    for (String eventStatus : List.of("programUid.ACTIVE", "COMPLETED")) {
      CommonRequestParams request = new CommonRequestParams().withEventStatus(Set.of(eventStatus));
      IllegalQueryException exception =
          assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));
      assertEquals(
          "Parameter eventStatus must be of the form: [programUid].[programStageUid].[EVENT_STATUS]",
          exception.getMessage());
    }
  }

  @Test
  void testProgramStatusWrongEnum() {
    CommonRequestParams request =
        new CommonRequestParams().withProgramStatus(Set.of("programUid.WRONG_PROGRAM_STATUS"));
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));

    assertEquals(
        "Parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
        exception.getMessage());
  }

  @Test
  void testEnrollmentStatusStatusWrongEnum() {
    CommonRequestParams request =
        new CommonRequestParams().withEnrollmentStatus(Set.of("programUid.WRONG_PROGRAM_STATUS"));
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));

    assertEquals(
        "Parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
        exception.getMessage());
  }

  @Test
  void testEventStatusWrongEnum() {
    CommonRequestParams request =
        new CommonRequestParams()
            .withEventStatus(Set.of("programUid.programStageUid.WRONG_EVENT_STATUS"));
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> commonRequestParamsParser.parse(request));

    assertEquals(
        "Parameter eventStatus must be of the form: [programUid].[programStageUid].[EVENT_STATUS]",
        exception.getMessage());
  }

  @Test
  void testProgramStatusOK() {
    CommonRequestParams request =
        new CommonRequestParams().withProgramStatus(Set.of("programUid.COMPLETED"));

    String parsedDimension = request.getAllDimensions().iterator().next();

    assertEquals("programUid.ENROLLMENT_STATUS:COMPLETED", parsedDimension);
  }

  @Test
  void testEnrollmentStatusOK() {
    CommonRequestParams request =
        new CommonRequestParams().withEnrollmentStatus(Set.of("programUid.COMPLETED"));

    String parsedDimension = request.getAllDimensions().iterator().next();

    assertEquals("programUid.ENROLLMENT_STATUS:COMPLETED", parsedDimension);
  }

  @Test
  void testEventStatusOK() {
    CommonRequestParams request =
        new CommonRequestParams().withEventStatus((Set.of("programUid.programStageUid.COMPLETED")));

    String parsedDimension = request.getAllDimensions().iterator().next();

    assertEquals("programUid.programStageUid.EVENT_STATUS:COMPLETED", parsedDimension);
  }
}
