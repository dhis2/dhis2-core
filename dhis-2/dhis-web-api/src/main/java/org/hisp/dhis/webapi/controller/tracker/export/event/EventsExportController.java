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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.common.OpenApi.Response.Status;
import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.assertUserOrderableFieldsAreSupported;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeGzip;
import static org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil.writeZip;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.export.event.EventRequestParams.DEFAULT_FIELDS_PARAM;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.ResponseEntityUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(Event.class)
@OpenApi.Tags("tracker")
@RestController
@RequestMapping(value = RESOURCE_PATH + "/" + EventsExportController.EVENTS)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@OpenApi.Ignore
class EventsExportController {
  protected static final String EVENTS = "events";

  private static final EventMapper EVENTS_MAPPER = Mappers.getMapper(EventMapper.class);

  private final EventService eventService;

  private final EventRequestParamsMapper eventParamsMapper;

  private final CsvService<Event> csvEventService;

  private final FieldFilterService fieldFilterService;

  private final EventFieldsParamMapper eventsMapper;

  private final ObjectMapper objectMapper;

  public EventsExportController(
      EventService eventService,
      EventRequestParamsMapper eventParamsMapper,
      CsvService<Event> csvEventService,
      FieldFilterService fieldFilterService,
      EventFieldsParamMapper eventsMapper,
      ObjectMapper objectMapper) {
    this.eventService = eventService;
    this.eventParamsMapper = eventParamsMapper;
    this.csvEventService = csvEventService;
    this.fieldFilterService = fieldFilterService;
    this.eventsMapper = eventsMapper;
    this.objectMapper = objectMapper;

    assertUserOrderableFieldsAreSupported(
        "event", EventMapper.ORDERABLE_FIELDS, eventService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(produces = "application/json")
  Page<ObjectNode> getEvents(EventRequestParams requestParams)
      throws BadRequestException, ForbiddenException {
    validatePaginationParameters(requestParams);
    EventOperationParams eventOperationParams = eventParamsMapper.map(requestParams);

    if (requestParams.isPaged()) {
      PageParams pageParams =
          new PageParams(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.getTotalPages());

      org.hisp.dhis.tracker.export.Page<org.hisp.dhis.program.Event> eventsPage =
          eventService.getEvents(eventOperationParams, pageParams);
      List<ObjectNode> objectNodes =
          fieldFilterService.toObjectNodes(
              EVENTS_MAPPER.fromCollection(eventsPage.getItems()), requestParams.getFields());

      return Page.withPager(EVENTS, objectNodes, eventsPage);
    }

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);
    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(
            EVENTS_MAPPER.fromCollection(events), requestParams.getFields());

    return Page.withoutPager(EVENTS, objectNodes);
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_GZIP)
  void getEventsAsJsonGzip(EventRequestParams eventRequestParams, HttpServletResponse response)
      throws BadRequestException, IOException, ForbiddenException {
    validatePaginationParameters(eventRequestParams);

    EventOperationParams eventOperationParams = eventParamsMapper.map(eventRequestParams);

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);

    String attachment = getAttachmentOrDefault(eventRequestParams.getAttachment(), "json", "gz");

    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION, getContentDispositionHeaderValue(attachment));
    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING,
        ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_JSON_GZIP);

    writeGzip(
        response.getOutputStream(), EVENTS_MAPPER.fromCollection(events), objectMapper.writer());
  }

  @GetMapping(produces = CONTENT_TYPE_JSON_ZIP)
  void getEventsAsJsonZip(EventRequestParams eventRequestParams, HttpServletResponse response)
      throws BadRequestException, ForbiddenException, IOException {
    validatePaginationParameters(eventRequestParams);

    EventOperationParams eventOperationParams = eventParamsMapper.map(eventRequestParams);

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);

    String attachment = getAttachmentOrDefault(eventRequestParams.getAttachment(), "json", "zip");

    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION, getContentDispositionHeaderValue(attachment));
    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING,
        ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_JSON_ZIP);

    writeZip(
        response.getOutputStream(),
        EVENTS_MAPPER.fromCollection(events),
        objectMapper.writer(),
        attachment);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV, CONTENT_TYPE_TEXT_CSV})
  void getEventsAsCsv(
      EventRequestParams eventRequestParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventOperationParams eventOperationParams = eventParamsMapper.map(eventRequestParams);

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);

    String attachment = getAttachmentOrDefault(eventRequestParams.getAttachment(), "csv");

    OutputStream outputStream = response.getOutputStream();
    response.setContentType(CONTENT_TYPE_CSV);
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, getContentDispositionHeaderValue(attachment));

    csvEventService.write(outputStream, EVENTS_MAPPER.fromCollection(events), !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_GZIP})
  void getEventsAsCsvGZip(
      EventRequestParams eventRequestParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventOperationParams eventOperationParams = eventParamsMapper.map(eventRequestParams);

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);

    String attachment = getAttachmentOrDefault(eventRequestParams.getAttachment(), "csv", "gz");

    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING,
        ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_CSV_GZIP);
    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION, getContentDispositionHeaderValue(attachment));

    csvEventService.writeGzip(
        response.getOutputStream(), EVENTS_MAPPER.fromCollection(events), !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_ZIP})
  void getEventsAsCsvZip(
      EventRequestParams eventRequestParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader)
      throws IOException, BadRequestException, ForbiddenException {
    EventOperationParams eventOperationParams = eventParamsMapper.map(eventRequestParams);

    List<org.hisp.dhis.program.Event> events = eventService.getEvents(eventOperationParams);

    String attachment = getAttachmentOrDefault(eventRequestParams.getAttachment(), "csv", "zip");

    response.addHeader(
        ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING,
        ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    response.setContentType(CONTENT_TYPE_CSV_ZIP);
    response.addHeader(
        ContextUtils.HEADER_CONTENT_DISPOSITION, getContentDispositionHeaderValue(attachment));

    csvEventService.writeZip(
        response.getOutputStream(), EVENTS_MAPPER.fromCollection(events), !skipHeader, attachment);
  }

  private String getAttachmentOrDefault(String filename, String type, String compression) {
    return Objects.toString(filename, String.join(".", EVENTS, type, compression));
  }

  private String getAttachmentOrDefault(String filename, String type) {
    return Objects.toString(filename, String.join(".", EVENTS, type));
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping("/{uid}")
  ResponseEntity<ObjectNode> getEventByUid(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields)
      throws NotFoundException, ForbiddenException {
    EventParams eventParams = eventsMapper.map(fields);
    Event event = EVENTS_MAPPER.from(eventService.getEvent(uid.getValue(), eventParams));

    return ResponseEntity.ok(fieldFilterService.toObjectNode(event, fields));
  }

  @GetMapping("/{event}/dataValues/{dataElement}/file")
  ResponseEntity<InputStreamResource> getEventDataValueFile(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException {
    return handleFileRequest(request, eventService.getFileResource(event, dataElement));
  }

  @GetMapping("/{event}/dataValues/{dataElement}/image")
  ResponseEntity<InputStreamResource> getEventDataValueImage(
      @OpenApi.Param({UID.class, Event.class}) @PathVariable UID event,
      @OpenApi.Param({UID.class, DataElement.class}) @PathVariable UID dataElement,
      @RequestParam(required = false) ImageFileDimension dimension,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException {
    return handleFileRequest(
        request, eventService.getFileResourceImage(event, dataElement, dimension));
  }

  private static ResponseEntity<InputStreamResource> handleFileRequest(
      HttpServletRequest request, FileResourceStream file)
      throws ConflictException, BadRequestException {
    FileResource fileResource = file.getFileResource();

    final String etag = fileResource.getContentMd5();
    if (ResponseEntityUtils.checkNotModified(etag, request)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
          .eTag(etag)
          .build();
    }

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
        .eTag(etag)
        .contentType(MediaType.valueOf(fileResource.getContentType()))
        .contentLength(fileResource.getContentLength())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            getContentDispositionHeaderValue(fileResource.getName()))
        .body(new InputStreamResource(file.getInputStreamSupplier().get()));
  }

  private static String getContentDispositionHeaderValue(String filename) {
    return "attachment; filename=" + filename;
  }
}
