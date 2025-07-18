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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.common.OpenApi.Response.Status;
import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.assertUserOrderableFieldsAreSupported;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateUnsupportedParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.FieldFilterRequestHandler.getRequestURL;
import static org.hisp.dhis.webapi.controller.tracker.export.MappingErrors.ensureNoMappingErrors;
import static org.hisp.dhis.webapi.controller.tracker.export.trackedentity.TrackedEntityRequestParams.DEFAULT_FIELDS_PARAM;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.tracker.RequestHandler;
import org.hisp.dhis.webapi.controller.tracker.export.ChangeLogRequestParams;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.ResponseHeader;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredEntity;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredPage;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.class)
@OpenApi.Document(
    entity = org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.class,
    classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/tracker/trackedEntities")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
class TrackedEntitiesExportController {

  protected static final String TRACKED_ENTITIES = "trackedEntities";

  /**
   * Fields we need to fetch from the DB to fulfill requests for CSV. CSV cannot be filtered by
   * field filtering so <code>fields</code> query parameter is ignored when CSV is requested. Make
   * sure this is kept in sync with the columns we promise to return in the CSV. See {@link
   * CsvTrackedEntity}.
   */
  private static final Fields CSV_FIELDS =
      FieldsParser.parse(
          "trackedEntity,trackedEntityType,createdAt,createdAtClient,updatedAt,updatedAtClient,orgUnit,inactive,deleted,potentialDuplicate,geometry,storedBy,createdBy,updatedBy,attributes");

  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private static final String TE_CSV_FILE = TRACKED_ENTITIES + ".csv";

  private static final String GZIP_EXT = ".gz";

  private static final String ZIP_EXT = ".zip";

  private static final TrackedEntityChangeLogMapper TRACKED_ENTITY_CHANGE_LOG_MAPPER =
      Mappers.getMapper(TrackedEntityChangeLogMapper.class);

  private final TrackedEntityService trackedEntityService;

  private final CsvService<TrackedEntity> entityCsvService;

  private final RequestHandler requestHandler;

  private final TrackedEntityChangeLogService trackedEntityChangeLogService;

  public TrackedEntitiesExportController(
      TrackedEntityService trackedEntityService,
      CsvService<TrackedEntity> csvEventService,
      RequestHandler requestHandler,
      TrackedEntityChangeLogService trackedEntityChangeLogService) {
    this.trackedEntityService = trackedEntityService;
    this.entityCsvService = csvEventService;
    this.requestHandler = requestHandler;
    this.trackedEntityChangeLogService = trackedEntityChangeLogService;

    assertUserOrderableFieldsAreSupported(
        "tracked entity",
        TrackedEntityMapper.ORDERABLE_FIELDS,
        trackedEntityService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  FilteredPage<TrackedEntity> getTrackedEntities(
      TrackedEntityRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws BadRequestException, ForbiddenException, NotFoundException, WebMessageException {
    validatePaginationParameters(requestParams);
    TrackedEntityOperationParams operationParams =
        TrackedEntityRequestParamsMapper.map(requestParams, currentUser);

    if (requestParams.isPaging()) {
      PageParams pageParams =
          PageParams.of(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
      org.hisp.dhis.tracker.Page<org.hisp.dhis.trackedentity.TrackedEntity> trackedEntitiesPage =
          trackedEntityService.findTrackedEntities(operationParams, pageParams);

      MappingErrors errors = new MappingErrors(idSchemeParams);
      org.hisp.dhis.tracker.Page<TrackedEntity> page =
          trackedEntitiesPage.withMappedItems(
              i -> TRACKED_ENTITY_MAPPER.map(idSchemeParams, errors, i));
      ensureNoMappingErrors(errors);

      return new FilteredPage<>(
          Page.withPager(TRACKED_ENTITIES, page, getRequestURL(request)),
          requestParams.getFields());
    }

    MappingErrors errors = new MappingErrors(idSchemeParams);
    List<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams).stream()
            .map(te -> TRACKED_ENTITY_MAPPER.map(idSchemeParams, errors, te))
            .toList();
    ensureNoMappingErrors(errors);

    return new FilteredPage<>(
        Page.withoutPager(TRACKED_ENTITIES, trackedEntities), requestParams.getFields());
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV, CONTENT_TYPE_TEXT_CSV})
  void getTrackedEntitiesAsCsv(
      TrackedEntityRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @CurrentUser UserDetails currentUser)
      throws IOException,
          BadRequestException,
          ForbiddenException,
          NotFoundException,
          WebMessageException {
    List<TrackedEntity> trackedEntities =
        getTrackedEntitiesForCsv(requestParams, idSchemeParams, currentUser);

    ResponseHeader.addContentDispositionAttachment(response, TE_CSV_FILE);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV);

    entityCsvService.write(response.getOutputStream(), trackedEntities, !skipHeader);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_ZIP})
  void getTrackedEntitiesAsCsvZip(
      TrackedEntityRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @CurrentUser UserDetails currentUser)
      throws IOException,
          BadRequestException,
          ForbiddenException,
          NotFoundException,
          WebMessageException {
    List<TrackedEntity> trackedEntities =
        getTrackedEntitiesForCsv(requestParams, idSchemeParams, currentUser);

    ResponseHeader.addContentDispositionAttachment(response, TE_CSV_FILE + ZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV_ZIP);

    entityCsvService.writeZip(
        response.getOutputStream(), trackedEntities, !skipHeader, TE_CSV_FILE);
  }

  @GetMapping(produces = {CONTENT_TYPE_CSV_GZIP})
  void getTrackedEntitiesAsCsvGZip(
      TrackedEntityRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @CurrentUser UserDetails currentUser)
      throws IOException,
          BadRequestException,
          ForbiddenException,
          NotFoundException,
          WebMessageException {
    List<TrackedEntity> trackedEntities =
        getTrackedEntitiesForCsv(requestParams, idSchemeParams, currentUser);

    ResponseHeader.addContentDispositionAttachment(response, TE_CSV_FILE + GZIP_EXT);
    ResponseHeader.addContentTransferEncodingBinary(response);
    response.setContentType(CONTENT_TYPE_CSV_GZIP);

    entityCsvService.writeGzip(response.getOutputStream(), trackedEntities, !skipHeader);
  }

  private List<TrackedEntity> getTrackedEntitiesForCsv(
      TrackedEntityRequestParams requestParams,
      TrackerIdSchemeParams idSchemeParams,
      UserDetails currentUser)
      throws BadRequestException, ForbiddenException, NotFoundException, WebMessageException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityRequestParamsMapper.map(requestParams, CSV_FIELDS, currentUser);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    List<TrackedEntity> result =
        trackedEntityService.findTrackedEntities(operationParams).stream()
            .map(te -> TRACKED_ENTITY_MAPPER.map(idSchemeParams, errors, te))
            .toList();
    ensureNoMappingErrors(errors);
    return result;
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping(value = "/{uid}")
  public FilteredEntity<TrackedEntity> getTrackedEntityByUid(
      @OpenApi.Param({UID.class, org.hisp.dhis.trackedentity.TrackedEntity.class}) @PathVariable
          UID uid,
      @OpenApi.Param({UID.class, Program.class}) @RequestParam(required = false) UID program,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          Fields fields,
      TrackerIdSchemeParams idSchemeParams)
      throws ForbiddenException, NotFoundException, WebMessageException {
    TrackedEntityFields trackedEntityFields =
        TrackedEntityFields.of(fields::includes, FieldPath.FIELD_PATH_SEPARATOR);
    MappingErrors errors = new MappingErrors(idSchemeParams);
    TrackedEntity trackedEntity =
        TRACKED_ENTITY_MAPPER.map(
            idSchemeParams,
            errors,
            trackedEntityService.getTrackedEntity(uid, program, trackedEntityFields));
    ensureNoMappingErrors(errors);

    return new FilteredEntity<>(trackedEntity, fields);
  }

  @GetMapping(
      value = "/{uid}",
      produces = {CONTENT_TYPE_CSV, CONTENT_TYPE_TEXT_CSV})
  void getTrackedEntityByUidAsCsv(
      @PathVariable UID uid,
      TrackerIdSchemeParams idSchemeParams,
      HttpServletResponse response,
      @RequestParam(required = false, defaultValue = "false") boolean skipHeader,
      @OpenApi.Param({UID.class, Program.class}) @RequestParam(required = false) UID program)
      throws IOException, ForbiddenException, NotFoundException, WebMessageException {
    TrackedEntityFields trackedEntityFields =
        TrackedEntityFields.of(CSV_FIELDS::includes, FieldPath.FIELD_PATH_SEPARATOR);

    MappingErrors errors = new MappingErrors(idSchemeParams);
    TrackedEntity trackedEntity =
        TRACKED_ENTITY_MAPPER.map(
            idSchemeParams,
            errors,
            trackedEntityService.getTrackedEntity(uid, program, trackedEntityFields));
    ensureNoMappingErrors(errors);

    OutputStream outputStream = response.getOutputStream();
    response.setContentType(CONTENT_TYPE_CSV);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trackedEntity.csv");
    entityCsvService.write(outputStream, List.of(trackedEntity), !skipHeader);
  }

  @GetMapping("/{trackedEntity}/attributes/{attribute}/file")
  ResponseEntity<InputStreamResource> getAttributeValueFile(
      @OpenApi.Param({UID.class, org.hisp.dhis.trackedentity.TrackedEntity.class}) @PathVariable
          UID trackedEntity,
      @OpenApi.Param({UID.class, Attribute.class}) @PathVariable UID attribute,
      @OpenApi.Param({UID.class, Program.class}) @RequestParam(required = false) UID program,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    validateUnsupportedParameter(
        request,
        "dimension",
        "Request parameter 'dimension' is only supported for images by API"
            + " /tracker/trackedEntities/attributes/{attribute}/image");

    return requestHandler.serve(
        request, trackedEntityService.getFileResource(trackedEntity, attribute, program));
  }

  @GetMapping("/{trackedEntity}/attributes/{attribute}/image")
  ResponseEntity<InputStreamResource> getAttributeValueImage(
      @OpenApi.Param({UID.class, org.hisp.dhis.trackedentity.TrackedEntity.class}) @PathVariable
          UID trackedEntity,
      @OpenApi.Param({UID.class, Attribute.class}) @PathVariable UID attribute,
      @OpenApi.Param({UID.class, Program.class}) @RequestParam(required = false) UID program,
      @RequestParam(required = false) ImageFileDimension dimension,
      HttpServletRequest request)
      throws NotFoundException, ConflictException, BadRequestException, ForbiddenException {
    return requestHandler.serve(
        request,
        trackedEntityService.getFileResourceImage(trackedEntity, attribute, program, dimension));
  }

  @OpenApi.EntityType(org.hisp.dhis.webapi.controller.tracker.view.TrackedEntityChangeLog.class)
  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping("/{trackedEntity}/changeLogs")
  FilteredPage<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntityChangeLog>
      getTrackedEntityChangeLog(
          @OpenApi.Param({UID.class, org.hisp.dhis.trackedentity.TrackedEntity.class}) @PathVariable
              UID trackedEntity,
          @OpenApi.Param({UID.class, Program.class}) @RequestParam(required = false) UID program,
          ChangeLogRequestParams requestParams,
          HttpServletRequest request)
          throws NotFoundException, BadRequestException, ForbiddenException {

    TrackedEntityChangeLogOperationParams operationParams =
        ChangeLogRequestParamsMapper.map(
            trackedEntityChangeLogService.getOrderableFields(),
            trackedEntityChangeLogService.getFilterableFields(),
            requestParams);

    PageParams pageParams =
        PageParams.of(requestParams.getPage(), requestParams.getPageSize(), false);
    org.hisp.dhis.tracker.Page<TrackedEntityChangeLog> page =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            trackedEntity, program, operationParams, pageParams);

    org.hisp.dhis.tracker.Page<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntityChangeLog>
        mappedPage = page.withMappedItems(TRACKED_ENTITY_CHANGE_LOG_MAPPER::map);

    return new FilteredPage<>(
        Page.withPager("changeLogs", mappedPage, getRequestURL(request)),
        requestParams.getFields());
  }
}
