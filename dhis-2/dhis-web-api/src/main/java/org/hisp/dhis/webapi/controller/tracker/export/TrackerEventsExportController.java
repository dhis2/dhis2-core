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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeGzip;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeZip;
import static org.hisp.dhis.webapi.utils.ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventQueryParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.EventFieldsParamMapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Tags("tracker")
@RestController
@RequestMapping(value = RESOURCE_PATH + "/" + TrackerEventsExportController.EVENTS)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
@OpenApi.Ignore
public class TrackerEventsExportController {
  protected static final String EVENTS = "events";

  private static final String DEFAULT_FIELDS_PARAM = "*,!relationships";

  private static final EventMapper EVENTS_MAPPER = Mappers.getMapper(EventMapper.class);

  @Nonnull private final EventService eventService;

  @Nonnull private final TrackerEventCriteriaMapper requestToSearchParams;

  @Nonnull private final ProgramStageInstanceService programStageInstanceService;

  @Nonnull
  private final CsvEventService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService;

  @Nonnull private final FieldFilterService fieldFilterService;

  private final EventFieldsParamMapper eventsMapper;

  private final ObjectMapper objectMapper;
  private static final String EVENT_CSV_FILE = EVENTS + ".csv";
  private static final String EVENT_JSON_FILE = EVENTS + ".json";

  private static final String GZIP_EXT = ".gz";
  private static final String ZIP_EXT = ".zip";

  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  public ResponseEntity<PagingWrapper<ObjectNode>> getEvents(
      TrackerEventCriteria eventCriteria,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields)
      throws BadRequestException, ForbiddenException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    EventParams eventParams = eventsMapper.map(fields);

    eventQueryParams.setIncludeRelationships(eventParams.isIncludeRelationships());

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new PagingWrapper<ObjectNode>().withInstances(Collections.emptyList()));
    }

    Events events = eventService.getEvents(eventQueryParams);

    PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

    if (eventCriteria.isPagingRequest()) {
      pagingWrapper =
          pagingWrapper.withPager(PagingWrapper.Pager.fromLegacy(eventCriteria, events.getPager()));
    }

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(EVENTS_MAPPER.fromCollection(events.getEvents()), fields);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(pagingWrapper.withInstances(objectNodes));
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_GZIP)
  void getEventsAsGzip(
      TrackerEventCriteria eventCriteria,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields,
      HttpServletResponse response)
      throws BadRequestException, IOException, ForbiddenException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    EventParams eventParams = eventsMapper.map(fields);

    eventQueryParams.setIncludeRelationships(eventParams.isIncludeRelationships());

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return;
    }

    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION,
        getContentDispositionHeaderValue(EVENT_JSON_FILE + GZIP_EXT));
    response.addHeader(HEADER_CONTENT_TRANSFER_ENCODING, BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_JSON_GZIP);

    Events events = eventService.getEvents(eventQueryParams);

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(EVENTS_MAPPER.fromCollection(events.getEvents()), fields);
    writeGzip(
        response.getOutputStream(), PagingWrapper.withoutPager(objectNodes), objectMapper.writer());
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_ZIP)
  void getEventsAsZip(
      TrackerEventCriteria eventCriteria,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields,
      HttpServletResponse response)
      throws BadRequestException, ForbiddenException, IOException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    EventParams eventParams = eventsMapper.map(fields);

    eventQueryParams.setIncludeRelationships(eventParams.isIncludeRelationships());

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return;
    }

    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION,
        getContentDispositionHeaderValue(EVENT_JSON_FILE + ZIP_EXT));
    response.addHeader(HEADER_CONTENT_TRANSFER_ENCODING, BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_JSON_ZIP);

    Events events = eventService.getEvents(eventQueryParams);

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(EVENTS_MAPPER.fromCollection(events.getEvents()), fields);

    writeZip(
        response.getOutputStream(),
        PagingWrapper.withoutPager(objectNodes),
        objectMapper.writer(),
        EVENT_JSON_FILE);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV, CONTENT_TYPE_TEXT_CSV})
  void getEventsAsCsv(
      TrackerEventCriteria eventCriteria,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return;
    }

    Events events = eventService.getEvents(eventQueryParams);

    OutputStream outputStream = response.getOutputStream();
    response.setContentType(CONTENT_TYPE_CSV);
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, getContentDispositionHeaderValue(EVENT_CSV_FILE));

    csvEventService.writeEvents(
        outputStream, EVENTS_MAPPER.fromCollection(events.getEvents()), !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_GZIP})
  void getEventsAsCsvGZip(
      TrackerEventCriteria eventCriteria,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return;
    }

    Events events = eventService.getEvents(eventQueryParams);

    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_CSV_GZIP);
    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION,
        getContentDispositionHeaderValue(EVENT_CSV_FILE + GZIP_EXT));

    csvEventService.writeGzip(
        response.getOutputStream(), EVENTS_MAPPER.fromCollection(events.getEvents()), !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_ZIP})
  void getEventsAsCsvZip(
      TrackerEventCriteria eventCriteria,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventQueryParams eventQueryParams = requestToSearchParams.map(eventCriteria);

    if (areAllEnrollmentsInvalid(eventCriteria, eventQueryParams)) {
      return;
    }

    Events events = eventService.getEvents(eventQueryParams);

    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_CSV_ZIP);
    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION,
        getContentDispositionHeaderValue(EVENT_CSV_FILE + ZIP_EXT));

    csvEventService.writeZip(
        response.getOutputStream(),
        EVENTS_MAPPER.fromCollection(events.getEvents()),
        !skipHeader,
        EVENT_CSV_FILE);
  }

  public String getContentDispositionHeaderValue(String filename) {
    return "attachment; filename=" + filename;
  }

  private boolean areAllEnrollmentsInvalid(
      TrackerEventCriteria eventCriteria, EventQueryParams eventQueryParams) {
    return !CollectionUtils.isEmpty(eventCriteria.getEnrollments())
        && CollectionUtils.isEmpty(eventQueryParams.getProgramInstances());
  }

  @GetMapping("{uid}")
  public ResponseEntity<ObjectNode> getEvent(
      @PathVariable String uid,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields)
      throws NotFoundException {
    EventParams eventParams = eventsMapper.map(fields);
    Event event =
        eventService.getEvent(
            programStageInstanceService.getProgramStageInstance(uid), eventParams);
    if (event == null) {
      throw new NotFoundException(Event.class, uid);
    }

    return ResponseEntity.ok(fieldFilterService.toObjectNode(EVENTS_MAPPER.from(event), fields));
  }
}
