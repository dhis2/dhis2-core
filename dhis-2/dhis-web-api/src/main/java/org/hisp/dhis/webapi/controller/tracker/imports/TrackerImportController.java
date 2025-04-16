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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.responses.TrackerJobWebMessageResponse;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.TrackerBundleReportMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.note.NoteService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.locationtech.jts.io.ParseException;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = TrackedEntity.class,
    classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/tracker")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class TrackerImportController {
  static final String TRACKER_JOB_ADDED = "Tracker job added";
  public static final String OPENAPI_IMPORT_DESCRIPTION =
"""
Import tracker data.
"""
          + ImportRequestParams.OPENAPI_DESCRIPTION_ASYNC;

  private final TrackerImportService trackerImportService;

  private final CsvService<Event> csvEventService;

  private final Notifier notifier;

  private final JobExecutionService jobExecutionService;

  private final ObjectMapper jsonMapper;

  private final NoteService noteService;

  private final NoteMapper noteMapper = Mappers.getMapper(NoteMapper.class);

  @OpenApi.Description(OPENAPI_IMPORT_DESCRIPTION)
  @OpenApi.Response({ImportReport.class, WebMessage.class})
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> importJson(
      HttpServletRequest request,
      ImportRequestParams requestParams,
      @RequestBody Body body,
      @CurrentUser UserDetails currentUser)
      throws ConflictException, IOException {
    if (requestParams.isAsync()) {
      return importAsync(request, requestParams, body, currentUser, MediaType.APPLICATION_JSON);
    }

    return importSync(requestParams, body);
  }

  @OpenApi.Description(OPENAPI_IMPORT_DESCRIPTION)
  @OpenApi.Response({ImportReport.class, WebMessage.class})
  @PostMapping(
      consumes = {"application/csv", "text/csv"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> importCsv(
      HttpServletRequest request,
      ImportRequestParams requestParams,
      @RequestParam(required = false, defaultValue = "true") boolean skipFirst,
      @CurrentUser UserDetails currentUser)
      throws IOException, ParseException, ConflictException {
    InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat(request.getInputStream());
    List<Event> events = csvEventService.read(inputStream, skipFirst);
    Body body = Body.builder().events(events).build();

    if (requestParams.isAsync()) {
      return importAsync(
          request, requestParams, body, currentUser, MimeType.valueOf("application/csv"));
    }

    return importSync(requestParams, body);
  }

  private ResponseEntity<WebMessage> importAsync(
      HttpServletRequest request,
      ImportRequestParams requestParams,
      Body body,
      UserDetails user,
      MimeType contentType)
      throws IOException, ConflictException {
    TrackerImportParams importParams = TrackerImportParamsMapper.trackerImportParams(requestParams);

    JobConfiguration config = new JobConfiguration(JobType.TRACKER_IMPORT_JOB);
    config.setExecutedBy(user.getUid());
    config.setJobParameters(importParams);

    TrackerObjects trackerObjects =
        TrackerImportParamsMapper.trackerObjects(body, importParams.getIdSchemes());
    byte[] jsonInput = jsonMapper.writeValueAsBytes(trackerObjects);

    jobExecutionService.executeOnceNow(config, contentType, new ByteArrayInputStream(jsonInput));

    String jobId = config.getUid();
    String location = ContextUtils.getRootPath(request) + "/tracker/jobs/" + jobId;
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ok(TRACKER_JOB_ADDED)
                .setLocation("/tracker/jobs/" + jobId)
                .setResponse(
                    TrackerJobWebMessageResponse.builder().id(jobId).location(location).build()));
  }

  private ResponseEntity<ImportReport> importSync(
      ImportRequestParams importRequestParams, Body body) {
    TrackerImportParams params = TrackerImportParamsMapper.trackerImportParams(importRequestParams);
    TrackerObjects trackerObjects =
        TrackerImportParamsMapper.trackerObjects(body, params.getIdSchemes());
    ImportReport importReport =
        trackerImportService.buildImportReport(
            trackerImportService.importTracker(params, trackerObjects), params.getReportMode());

    ResponseEntity.BodyBuilder builder =
        importReport.getStatus() == Status.ERROR
            ? ResponseEntity.status(HttpStatus.CONFLICT)
            : ResponseEntity.ok();

    return builder.body(importReport);
  }

  @GetMapping(value = "/jobs/{uid}", produces = APPLICATION_JSON_VALUE)
  public Deque<Notification> getJob(@PathVariable String uid, HttpServletResponse response)
      throws HttpStatusCodeException {
    setNoStore(response);
    return notifier.getNotificationsByJobId(JobType.TRACKER_IMPORT_JOB, uid);
  }

  @GetMapping(value = "/jobs/{uid}/report", produces = APPLICATION_JSON_VALUE)
  public ImportReport getJobReport(
      @PathVariable String uid,
      @RequestParam(defaultValue = "errors", required = false) TrackerBundleReportMode reportMode,
      HttpServletResponse response)
      throws HttpStatusCodeException, NotFoundException, ConflictException {
    setNoStore(response);

    JsonValue report = notifier.getJobSummaryByJobId(JobType.TRACKER_IMPORT_JOB, uid);
    if (report == null) throw new NotFoundException("Summary for job " + uid + " does not exist");
    try {
      return trackerImportService.buildImportReport(
          jsonMapper.readValue(report.toJson(), ImportReport.class), reportMode);
    } catch (JsonProcessingException e) {
      throw new ConflictException("Failed to convert the import report: " + report);
    }
  }

  @PostMapping(value = "/enrollments/{uid}/note", consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<Note> addNoteToEnrollment(@RequestBody Note note, @PathVariable UID uid)
      throws ForbiddenException, NotFoundException, BadRequestException {

    noteService.addNoteForEnrollment(noteMapper.from(note), uid);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(note);
  }

  @PostMapping(value = "/events/{uid}/note", consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<Note> addNoteToEvent(@RequestBody Note note, @PathVariable UID uid)
      throws ForbiddenException, NotFoundException, BadRequestException {

    noteService.addNoteForEvent(noteMapper.from(note), uid);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(note);
  }
}
