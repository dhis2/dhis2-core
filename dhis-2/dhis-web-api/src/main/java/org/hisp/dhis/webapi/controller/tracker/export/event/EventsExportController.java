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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.assertUserOrderableFieldsAreSupported;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateUnsupportedParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeGzip;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeZip;
import static org.hisp.dhis.webapi.controller.tracker.export.FieldFilterRequestHandler.getRequestURL;
import static org.hisp.dhis.webapi.controller.tracker.export.MappingErrors.ensureNoMappingErrors;
import static org.hisp.dhis.webapi.controller.tracker.export.event.EventRequestParams.DEFAULT_FIELDS_PARAM;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.Fields;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.event.EventChangeLogOperationParams;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.webapi.controller.tracker.RequestHandler;
import org.hisp.dhis.webapi.controller.tracker.export.ChangeLogRequestParams;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.ResponseHeader;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredPage;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(Event.class)
@OpenApi.Document(classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/tracker/events")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
class EventsExportController {
  protected static final String EVENTS = "events";

  private static final EventMapper EVENTS_MAPPER = Mappers.getMapper(EventMapper.class);

  private static final EventChangeLogMapper EVENT_CHANGE_LOG_MAPPER =
      Mappers.getMapper(EventChangeLogMapper.class);

  private static final String EVENT_CSV_FILE = EVENTS + ".csv";

  private static final String EVENT_JSON_FILE = EVENTS + ".json";

  private static final String GZIP_EXT = ".gz";

  private static final String ZIP_EXT = ".zip";

  private final EventService eventService;

  private final EventRequestParamsMapper eventParamsMapper;

  private final CsvService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService;

  private final RequestHandler requestHandler;

  private final FieldFilterService fieldFilterService;

  private final EventFieldsParamMapper eventsMapper;

  private final ObjectMapper objectMapper;

  private final EventChangeLogService eventChangeLogService;

  public EventsExportController(
      EventService eventService,
      EventRequestParamsMapper eventParamsMapper,
      CsvService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService,
      RequestHandler requestHandler,
      FieldFilterService fieldFilterService,
      EventFieldsParamMapper eventsMapper,
      ObjectMapper objectMapper,
      EventChangeLogService eventChangeLogService) {
    this.eventService = eventService;
    this.eventParamsMapper = eventParamsMapper;
    this.csvEventService = csvEventService;
    this.requestHandler = requestHandler;
    this.fieldFilterService = fieldFilterService;
    this.eventsMapper = eventsMapper;
    this.objectMapper = objectMapper;
    this.eventChangeLogService = eventChangeLogService;

    assertUserOrderableFieldsAreSupported(
        "event", EventMapper.ORDERABLE_FIELDS, eventService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  ResponseEntity<FilteredPage<org.hisp.dhis.webapi.controller.tracker.view.Event>> getEvents(
      EventRequestParams requestParams,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) Fields fields,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletRequest request)
      throws BadRequestException, ForbiddenException, WebMessageException {
    validatePaginationParameters(requestParams);

    if (requestParams.isPaging()) {
      PageParams pageParams =
          PageParams.of(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
      EventOperationParams eventOperationParams =
          eventParamsMapper.map(requestParams, idSchemeParams);
      org.hisp.dhis.tracker.Page<Event> eventsPage =
          eventService.findEvents(eventOperationParams, pageParams);

      MappingErrors errors = new MappingErrors(idSchemeParams);
      org.hisp.dhis.tracker.Page<org.hisp.dhis.webapi.controller.tracker.view.Event> page =
          eventsPage.withMappedItems(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev));
      ensureNoMappingErrors(errors);

      return ResponseEntity.ok(
          new FilteredPage<>(Page.withPager(EVENTS, page, getRequestURL(request)), fields));
    }

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    return ResponseEntity.ok(new FilteredPage<>(Page.withoutPager(EVENTS, events), fields));
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_GZIP)
  void getEventsAsJsonGzip(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response)
      throws BadRequestException, IOException, ForbiddenException, WebMessageException {
    validatePaginationParameters(requestParams);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    ResponseHeader.addContentDispositionAttachment(response, EVENT_JSON_FILE + GZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_JSON_GZIP);

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(events, requestParams.getFields());

    writeGzip(
        response.getOutputStream(), Page.withoutPager(EVENTS, objectNodes), objectMapper.writer());
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_ZIP)
  void getEventsAsJsonZip(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response)
      throws BadRequestException, ForbiddenException, IOException, WebMessageException {
    validatePaginationParameters(requestParams);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    ResponseHeader.addContentDispositionAttachment(response, EVENT_JSON_FILE + ZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_JSON_ZIP);

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(events, requestParams.getFields());

    writeZip(
        response.getOutputStream(),
        Page.withoutPager(EVENTS, objectNodes),
        objectMapper.writer(),
        EVENT_JSON_FILE);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV, CONTENT_TYPE_TEXT_CSV})
  void getEventsAsCsv(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    ResponseHeader.addContentDispositionAttachment(response, EVENT_CSV_FILE);
    response.setContentType(CONTENT_TYPE_CSV);

    csvEventService.write(response.getOutputStream(), events, !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_GZIP})
  void getEventsAsCsvGZip(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    ResponseHeader.addContentDispositionAttachment(response, EVENT_CSV_FILE + GZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV_GZIP);

    csvEventService.writeGzip(response.getOutputStream(), events, !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_ZIP})
  void getEventsAsCsvZip(
      EventRequestParams requestParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      TrackerIdSchemeParams idSchemeParams)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getEventsList(requestParams, idSchemeParams);

    ResponseHeader.addContentDispositionAttachment(response, EVENT_CSV_FILE + ZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV_ZIP);

    csvEventService.writeZip(response.getOutputStream(), events, !skipHeader, EVENT_CSV_FILE);
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping("/{uid}")
  ResponseEntity<ObjectNode> getEventByUid(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields,
      TrackerIdSchemeParams idSchemeParams)
      throws NotFoundException, WebMessageException {
    EventParams eventParams = eventsMapper.map(fields);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    org.hisp.dhis.webapi.controller.tracker.view.Event event =
        EVENTS_MAPPER.map(
            idSchemeParams, errors, eventService.getEvent(uid, idSchemeParams, eventParams));
    ensureNoMappingErrors(errors);

    return requestHandler.serve(event, fields);
  }

  private List<org.hisp.dhis.webapi.controller.tracker.view.Event> getEventsList(
      EventRequestParams requestParams, TrackerIdSchemeParams idSchemeParams)
      throws BadRequestException, ForbiddenException, WebMessageException {
    EventOperationParams eventOperationParams =
        eventParamsMapper.map(requestParams, idSchemeParams);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        eventService.findEvents(eventOperationParams).stream()
            .map(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev))
            .toList();
    ensureNoMappingErrors(errors);
    return events;
  }

  @GetMapping("/{event}/dataValues/{dataElement}/file")
  ResponseEntity<InputStreamResource> getEventDataValueFile(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    validateUnsupportedParameter(
        request,
        "dimension",
        "Request parameter 'dimension' is only supported for images by API"
            + " /tracker/event/dataValues/{dataElement}/image");

    return requestHandler.serve(request, eventService.getFileResource(event, dataElement));
  }

  @GetMapping("/{event}/dataValues/{dataElement}/image")
  ResponseEntity<InputStreamResource> getEventDataValueImage(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      @RequestParam(required = false) ImageFileDimension dimension,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    return requestHandler.serve(
        request, eventService.getFileResourceImage(event, dataElement, dimension));
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping("/{event}/changeLogs")
  ResponseEntity<Page<ObjectNode>> getEventChangeLogsByUid(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID event,
      ChangeLogRequestParams requestParams,
      HttpServletRequest request)
      throws NotFoundException, BadRequestException {
    EventChangeLogOperationParams operationParams =
        ChangeLogRequestParamsMapper.map(
            eventChangeLogService.getOrderableFields(),
            eventChangeLogService.getFilterableFields(),
            requestParams);

    PageParams pageParams =
        PageParams.of(requestParams.getPage(), requestParams.getPageSize(), false);
    org.hisp.dhis.tracker.Page<org.hisp.dhis.tracker.export.event.EventChangeLog> page =
        eventChangeLogService.getEventChangeLog(event, operationParams, pageParams);

    return requestHandler.serve(
        request, "changeLogs", page.withMappedItems(EVENT_CHANGE_LOG_MAPPER::map), requestParams);
  }
}
