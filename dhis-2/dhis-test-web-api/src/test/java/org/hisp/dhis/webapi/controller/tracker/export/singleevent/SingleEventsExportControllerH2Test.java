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
package org.hisp.dhis.webapi.controller.tracker.export.singleevent;

import static org.hisp.dhis.webapi.controller.tracker.export.singleevent.SingleEventsExportControllerH2Test.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventMapper;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(classes = Config.class)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleEventsExportControllerH2Test extends H2ControllerIntegrationTestBase {

  static class Config {
    @Bean
    public SingleEventService singleEventService() {
      SingleEventService singleEventService = mock(SingleEventService.class);
      // Orderable fields are checked within the controller constructor
      when(singleEventService.getOrderableFields())
          .thenReturn(new HashSet<>(EventMapper.ORDERABLE_FIELDS.values()));
      return singleEventService;
    }

    @Bean
    public ProgramService programService() {
      return mock(ProgramService.class);
    }
  }

  @Autowired private SingleEventService singleEventService;

  @Autowired private ProgramService programService;

  static Stream<Arguments> callEventsEndpoint() {
    return Stream.of(
        arguments(
            "/tracker/events.json.zip?program=bMcwwoVnbSR",
            "application/json+zip",
            "attachment; filename=events.json.zip",
            "binary"),
        arguments(
            "/tracker/events.json.gz?program=bMcwwoVnbSR",
            "application/json+gzip",
            "attachment; filename=events.json.gz",
            "binary"),
        arguments(
            "/tracker/events.csv?program=bMcwwoVnbSR",
            "application/csv; charset=UTF-8",
            "attachment; filename=events.csv",
            null),
        arguments(
            "/tracker/events.csv.gz?program=bMcwwoVnbSR",
            "application/csv+gzip",
            "attachment; filename=events.csv.gz",
            "binary"),
        arguments(
            "/tracker/events.csv.zip?program=bMcwwoVnbSR",
            "application/csv+zip",
            "attachment; filename=events.csv.zip",
            "binary"));
  }

  @ParameterizedTest
  @MethodSource(value = "callEventsEndpoint")
  void
      shouldMatchContentTypeAndAttachment_whenEndpointForCompressedEventJsonIsInvokedForSingleEvent(
          String url, String expectedContentType, String expectedAttachment, String encoding)
          throws ForbiddenException, BadRequestException {

    Program program = new Program();
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    when(singleEventService.findEvents(any())).thenReturn(List.of());
    when(programService.getProgram(anyString())).thenReturn(program);

    HttpResponse res = GET(url);
    assertEquals(HttpStatus.OK, res.status());
    assertEquals(expectedContentType, res.header("Content-Type"));
    assertEquals(expectedAttachment, res.header(ContextUtils.HEADER_CONTENT_DISPOSITION));
    assertEquals(encoding, res.header(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING));
    assertNotNull(res.content(expectedContentType));
  }
}
