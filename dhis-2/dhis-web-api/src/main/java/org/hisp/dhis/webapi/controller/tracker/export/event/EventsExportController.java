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
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.event.EventChangeLog;
import org.hisp.dhis.tracker.export.event.EventChangeLogOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLogService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLogService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.webapi.controller.tracker.RequestHandler;
import org.hisp.dhis.webapi.controller.tracker.export.ChangeLogRequestParams;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.ResponseHeader;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(TrackerEvent.class)
@OpenApi.Document(classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/tracker/events")
class EventsExportController {
  protected static final String EVENTS = "events";

  private static final EventMapper EVENTS_MAPPER = Mappers.getMapper(EventMapper.class);

  private static final EventChangeLogMapper EVENT_CHANGE_LOG_MAPPER =
      Mappers.getMapper(EventChangeLogMapper.class);

  private static final String EVENT_CSV_FILE = EVENTS + ".csv";

  private static final String EVENT_JSON_FILE = EVENTS + ".json";

  private static final String GZIP_EXT = ".gz";

  private static final String ZIP_EXT = ".zip";

  private final TrackerEventService trackerEventService;

  private final TrackerEventRequestParamsMapper trackerEventParamsMapper;

  private final SingleEventService singleEventService;

  private final SingleEventRequestParamsMapper singleEventParamsMapper;

  private final CsvService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService;

  private final RequestHandler requestHandler;

  private final FieldFilterService fieldFilterService;

  private final ObjectMapper objectMapper;

  private final SingleEventChangeLogService singleEventChangeLogService;

  private final TrackerEventChangeLogService trackerEventChangeLogService;

  private final ProgramService programService;

  private final IdentifiableObjectManager manager;

  public EventsExportController(
      TrackerEventService trackerEventService,
      TrackerEventRequestParamsMapper trackerEventParamsMapper,
      SingleEventService singleEventService,
      SingleEventRequestParamsMapper singleEventParamsMapper,
      CsvService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService,
      RequestHandler requestHandler,
      FieldFilterService fieldFilterService,
      ObjectMapper objectMapper,
      SingleEventChangeLogService singleEventChangeLogService,
      TrackerEventChangeLogService trackerEventChangeLogService,
      ProgramService programService,
      IdentifiableObjectManager manager) {
    this.trackerEventService = trackerEventService;
    this.trackerEventParamsMapper = trackerEventParamsMapper;
    this.singleEventService = singleEventService;
    this.singleEventParamsMapper = singleEventParamsMapper;
    this.csvEventService = csvEventService;
    this.requestHandler = requestHandler;
    this.fieldFilterService = fieldFilterService;
    this.objectMapper = objectMapper;
    this.singleEventChangeLogService = singleEventChangeLogService;
    this.trackerEventChangeLogService = trackerEventChangeLogService;
    this.programService = programService;
    this.manager = manager;

    assertUserOrderableFieldsAreSupported(
        "event", EventMapper.ORDERABLE_FIELDS, trackerEventService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  ResponseEntity<Page<ObjectNode>> getEvents(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletRequest request,
      @RequestParam UID program)
      throws BadRequestException, ForbiddenException, WebMessageException {
    validatePaginationParameters(requestParams);
    Program eventProgram = getProgram(program);

    if (eventProgram.isRegistration()) {
      if (requestParams.isPaging()) {
        PageParams pageParams =
            PageParams.of(
                requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
        TrackerEventOperationParams trackerEventOperationParams =
            trackerEventParamsMapper.map(requestParams, idSchemeParams);
        org.hisp.dhis.tracker.Page<TrackerEvent> eventsPage =
            trackerEventService.findEvents(trackerEventOperationParams, pageParams);

        MappingErrors errors = new MappingErrors(idSchemeParams);
        org.hisp.dhis.tracker.Page<org.hisp.dhis.webapi.controller.tracker.view.Event> page =
            eventsPage.withMappedItems(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev));
        ensureNoMappingErrors(errors);

        return requestHandler.serve(request, EVENTS, page, requestParams);
      }

      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
          getTrackerEventsList(requestParams, idSchemeParams);

      return requestHandler.serve(EVENTS, events, requestParams);
    }

    if (requestParams.isPaging()) {
      PageParams pageParams =
          PageParams.of(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
      SingleEventOperationParams singleEventOperationParams =
          singleEventParamsMapper.map(requestParams, idSchemeParams);
      org.hisp.dhis.tracker.Page<SingleEvent> eventsPage =
          singleEventService.findEvents(singleEventOperationParams, pageParams);

      MappingErrors errors = new MappingErrors(idSchemeParams);
      org.hisp.dhis.tracker.Page<org.hisp.dhis.webapi.controller.tracker.view.Event> page =
          eventsPage.withMappedItems(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev));
      ensureNoMappingErrors(errors);

      return requestHandler.serve(request, EVENTS, page, requestParams);
    }

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        getSingleEventsList(requestParams, idSchemeParams);

    return requestHandler.serve(EVENTS, events, requestParams);
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_GZIP)
  void getEventsAsJsonGzip(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam UID program)
      throws BadRequestException, IOException, ForbiddenException, WebMessageException {
    validatePaginationParameters(requestParams);
    Program eventProgram = getProgram(program);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events;
    if (eventProgram.isRegistration()) {
      events = getTrackerEventsList(requestParams, idSchemeParams);
    } else {
      events = getSingleEventsList(requestParams, idSchemeParams);
    }

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
      HttpServletResponse response,
      @RequestParam UID program)
      throws BadRequestException, ForbiddenException, IOException, WebMessageException {
    validatePaginationParameters(requestParams);
    Program eventProgram = getProgram(program);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events;
    if (eventProgram.isRegistration()) {
      events = getTrackerEventsList(requestParams, idSchemeParams);
    } else {
      events = getSingleEventsList(requestParams, idSchemeParams);
    }

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
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @RequestParam UID program)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    Program eventProgram = getProgram(program);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events;
    if (eventProgram.isRegistration()) {
      events = getTrackerEventsList(requestParams, idSchemeParams);
    } else {
      events = getSingleEventsList(requestParams, idSchemeParams);
    }

    ResponseHeader.addContentDispositionAttachment(response, EVENT_CSV_FILE);
    response.setContentType(CONTENT_TYPE_CSV);

    csvEventService.write(response.getOutputStream(), events, !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_GZIP})
  void getEventsAsCsvGZip(
      EventRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @RequestParam UID program)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    Program eventProgram = getProgram(program);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events;
    if (eventProgram.isRegistration()) {
      events = getTrackerEventsList(requestParams, idSchemeParams);
    } else {
      events = getSingleEventsList(requestParams, idSchemeParams);
    }

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
      TrackerIdSchemeParams idSchemeParams,
      @RequestParam UID program)
      throws IOException, BadRequestException, ForbiddenException, WebMessageException {
    Program eventProgram = getProgram(program);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events;
    if (eventProgram.isRegistration()) {
      events = getTrackerEventsList(requestParams, idSchemeParams);
    } else {
      events = getSingleEventsList(requestParams, idSchemeParams);
    }

    ResponseHeader.addContentDispositionAttachment(response, EVENT_CSV_FILE + ZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV_ZIP);

    csvEventService.writeZip(response.getOutputStream(), events, !skipHeader, EVENT_CSV_FILE);
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping("/{uid}")
  ResponseEntity<ObjectNode> getEventByUid(
      @OpenApi.Param({UID.class, TrackerEvent.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields,
      TrackerIdSchemeParams idSchemeParams)
      throws NotFoundException, WebMessageException {
    MappingErrors errors = new MappingErrors(idSchemeParams);
    org.hisp.dhis.webapi.controller.tracker.view.Event eventView;
    Program program = getProgramFromEvent(uid);
    if (program.isRegistration()) {
      org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields eventFields =
          org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields.of(
              f ->
                  fieldFilterService.filterIncludes(
                      org.hisp.dhis.webapi.controller.tracker.view.Event.class, fields, f),
              FieldPath.FIELD_PATH_SEPARATOR);
      TrackerEvent event = trackerEventService.getEvent(uid, idSchemeParams, eventFields);
      eventView = EVENTS_MAPPER.map(idSchemeParams, errors, event);
    } else {
      org.hisp.dhis.tracker.export.singleevent.SingleEventFields eventFields =
          org.hisp.dhis.tracker.export.singleevent.SingleEventFields.of(
              f ->
                  fieldFilterService.filterIncludes(
                      org.hisp.dhis.webapi.controller.tracker.view.Event.class, fields, f),
              FieldPath.FIELD_PATH_SEPARATOR);
      SingleEvent event = singleEventService.getEvent(uid, idSchemeParams, eventFields);
      eventView = EVENTS_MAPPER.map(idSchemeParams, errors, event);
    }

    ensureNoMappingErrors(errors);

    return requestHandler.serve(eventView, fields);
  }

  @Nonnull
  private Program getProgram(UID programUID) throws BadRequestException {
    if (programUID == null) {
      throw new BadRequestException("Program is mandatory");
    }
    Program program = programService.getProgram(programUID.getValue());
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + programUID);
    }
    return program;
  }

  @Nonnull
  private Program getProgramFromEvent(@Nonnull UID eventUID) throws NotFoundException {
    TrackerEvent event = manager.get(TrackerEvent.class, eventUID);
    if (event == null) {
      throw new NotFoundException(Event.class, eventUID);
    }

    return event.getProgramStage().getProgram();
  }

  private List<org.hisp.dhis.webapi.controller.tracker.view.Event> getSingleEventsList(
      EventRequestParams requestParams, TrackerIdSchemeParams idSchemeParams)
      throws BadRequestException, ForbiddenException, WebMessageException {
    SingleEventOperationParams singleEventOperationParams =
        singleEventParamsMapper.map(requestParams, idSchemeParams);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        singleEventService.findEvents(singleEventOperationParams).stream()
            .map(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev))
            .toList();
    ensureNoMappingErrors(errors);
    return events;
  }

  private List<org.hisp.dhis.webapi.controller.tracker.view.Event> getTrackerEventsList(
      EventRequestParams requestParams, TrackerIdSchemeParams idSchemeParams)
      throws BadRequestException, ForbiddenException, WebMessageException {
    TrackerEventOperationParams trackerEventOperationParams =
        trackerEventParamsMapper.map(requestParams, idSchemeParams);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        trackerEventService.findEvents(trackerEventOperationParams).stream()
            .map(ev -> EVENTS_MAPPER.map(idSchemeParams, errors, ev))
            .toList();
    ensureNoMappingErrors(errors);
    return events;
  }

  @GetMapping("/{event}/dataValues/{dataElement}/file")
  ResponseEntity<InputStreamResource> getEventDataValueFile(
      @OpenApi.Param({UID.class, TrackerEvent.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    validateUnsupportedParameter(
        request,
        "dimension",
        "Request parameter 'dimension' is only supported for images by API"
            + " /tracker/event/dataValues/{dataElement}/image");

    FileResourceStream fileResource;
    Program program = getProgramFromEvent(event);
    if (program.isRegistration()) {
      fileResource = trackerEventService.getFileResource(event, dataElement);
    } else {
      fileResource = singleEventService.getFileResource(event, dataElement);
    }
    return requestHandler.serve(request, fileResource);
  }

  @GetMapping("/{event}/dataValues/{dataElement}/image")
  ResponseEntity<InputStreamResource> getEventDataValueImage(
      @OpenApi.Param({UID.class, TrackerEvent.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      @RequestParam(required = false) ImageFileDimension dimension,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    Program program = getProgramFromEvent(event);
    FileResourceStream fileResourceImage;
    if (program.isRegistration()) {
      fileResourceImage = trackerEventService.getFileResourceImage(event, dataElement, dimension);
    } else {
      fileResourceImage = singleEventService.getFileResourceImage(event, dataElement, dimension);
    }
    return requestHandler.serve(request, fileResourceImage);
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping("/{event}/changeLogs")
  ResponseEntity<Page<ObjectNode>> getEventChangeLogsByUid(
      @OpenApi.Param({UID.class, TrackerEvent.class}) @PathVariable UID event,
      ChangeLogRequestParams requestParams,
      HttpServletRequest request)
      throws NotFoundException, BadRequestException {
    Program program = getProgramFromEvent(event);
    if (program.isRegistration()) {
      EventChangeLogOperationParams operationParams =
          ChangeLogRequestParamsMapper.map(
              trackerEventChangeLogService.getOrderableFields(),
              trackerEventChangeLogService.getFilterableFields(),
              requestParams);

      PageParams pageParams =
          PageParams.of(requestParams.getPage(), requestParams.getPageSize(), false);
      org.hisp.dhis.tracker.Page<EventChangeLog> page =
          trackerEventChangeLogService.getEventChangeLog(event, operationParams, pageParams);

      return requestHandler.serve(
          request, "changeLogs", page.withMappedItems(EVENT_CHANGE_LOG_MAPPER::map), requestParams);
    } else {
      EventChangeLogOperationParams operationParams =
          ChangeLogRequestParamsMapper.map(
              singleEventChangeLogService.getOrderableFields(),
              singleEventChangeLogService.getFilterableFields(),
              requestParams);

      PageParams pageParams =
          PageParams.of(requestParams.getPage(), requestParams.getPageSize(), false);
      org.hisp.dhis.tracker.Page<EventChangeLog> page =
          singleEventChangeLogService.getEventChangeLog(event, operationParams, pageParams);

      return requestHandler.serve(
          request, "changeLogs", page.withMappedItems(EVENT_CHANGE_LOG_MAPPER::map), requestParams);
    }
  }
}
