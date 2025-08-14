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
package org.hisp.dhis.webapi.controller.notification;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.export.FieldFilterRequestHandler.getRequestURL;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationInstanceSchemaDescriptor;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Zubair Asghar
 */
@OpenApi.EntityType(ProgramNotificationInstance.class)
@OpenApi.Document(
    entity = ProgramNotificationInstance.class,
    classifiers = {"team:tracker", "purpose:data"})
@Controller
@RequestMapping("/api/programNotificationInstances")
@RequiredArgsConstructor
public class ProgramNotificationInstanceController {
  private final ProgramNotificationInstanceService programNotificationInstanceService;

  private final EnrollmentService enrollmentService;

  private final TrackerEventService trackerEventService;

  private final SingleEventService singleEventService;

  private final IdentifiableObjectManager manager;

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @RequiresAuthority(anyOf = ALL)
  @GetMapping(produces = {"application/json"})
  public @ResponseBody ResponseEntity<Page<ProgramNotificationInstance>> getScheduledMessage(
      ProgramNotificationInstanceRequestParams requestParams, HttpServletRequest request)
      throws NotFoundException, BadRequestException {
    validatePaginationParameters(requestParams);
    validateRequestParams(requestParams);

    TrackerEvent storedTrackerEvent = null;
    SingleEvent storedSingleEvent = null;
    if (requestParams.getEvent() != null) {
      if (trackerEventService.exists(requestParams.getEvent())) {
        trackerEventService.getEvent(requestParams.getEvent());
        // TODO(tracker) jdbc-hibernate: check the impact on performance
        storedTrackerEvent = manager.get(TrackerEvent.class, requestParams.getEvent());
      } else {
        singleEventService.getEvent(requestParams.getEvent());
        // TODO(tracker) jdbc-hibernate: check the impact on performance
        storedSingleEvent = manager.get(SingleEvent.class, requestParams.getEvent());
      }
    }
    Enrollment storedEnrollment = null;
    if (requestParams.getEnrollment() != null) {
      enrollmentService.getEnrollment(requestParams.getEnrollment());
      // TODO(tracker) jdbc-hibernate: check the impact on performance
      storedEnrollment = manager.get(Enrollment.class, requestParams.getEnrollment());
    }

    if (requestParams.isPaging()) {
      PageParams pageParams =
          PageParams.of(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
      ProgramNotificationInstanceParam params =
          ProgramNotificationInstanceParam.builder()
              .enrollment(storedEnrollment)
              .trackerEvent(storedTrackerEvent)
              .singleEvent(storedSingleEvent)
              .scheduledAt(requestParams.getScheduledAt())
              .paging(true)
              .page(pageParams.getPage())
              .pageSize(pageParams.getPageSize())
              .build();
      List<ProgramNotificationInstance> instances =
          programNotificationInstanceService.getProgramNotificationInstancesPage(params);

      instances.forEach(this::setEvent);

      org.hisp.dhis.tracker.Page<ProgramNotificationInstance> page =
          new org.hisp.dhis.tracker.Page<>(
              instances,
              pageParams,
              () -> programNotificationInstanceService.countProgramNotificationInstances(params));

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Page.withPager(
                  ProgramNotificationInstanceSchemaDescriptor.PLURAL,
                  page,
                  getRequestURL(request)));
    }

    ProgramNotificationInstanceParam params =
        ProgramNotificationInstanceParam.builder()
            .enrollment(storedEnrollment)
            .trackerEvent(storedTrackerEvent)
            .singleEvent(storedSingleEvent)
            .scheduledAt(requestParams.getScheduledAt())
            .paging(false)
            .build();
    List<ProgramNotificationInstance> instances =
        programNotificationInstanceService.getProgramNotificationInstances(params);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(Page.withoutPager(ProgramNotificationInstanceSchemaDescriptor.PLURAL, instances));
  }

  private void setEvent(ProgramNotificationInstance programNotificationInstance) {
    TrackerEvent trackerEvent = programNotificationInstance.getTrackerEvent();
    SingleEvent singleEvent = programNotificationInstance.getSingleEvent();
    if (trackerEvent != null) {
      programNotificationInstance.setEvent(trackerEvent);
    } else if (singleEvent != null) {
      programNotificationInstance.setEvent(TrackerObjectsMapper.map(singleEvent));
    }
  }

  private void validateRequestParams(ProgramNotificationInstanceRequestParams requestParams)
      throws BadRequestException {
    if (requestParams.getEvent() == null && requestParams.getEnrollment() == null) {
      throw new BadRequestException("Enrollment or Event must be specified.");
    }
  }
}
