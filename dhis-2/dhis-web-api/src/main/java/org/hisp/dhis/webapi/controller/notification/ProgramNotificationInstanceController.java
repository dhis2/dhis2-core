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
package org.hisp.dhis.webapi.controller.notification;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateDeprecatedParameter;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationInstanceSchemaDescriptor;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Zubair Asghar
 */
@OpenApi.Document(
    entity = ProgramNotificationInstance.class,
    classifiers = {"team:tracker", "purpose:metadata"})
@Controller
@RequestMapping("/api/programNotificationInstances")
@ApiVersion(include = {DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ProgramNotificationInstanceController {
  private final ProgramNotificationInstanceService programNotificationInstanceService;

  private final EnrollmentService enrollmentService;

  private final EventService eventService;

  public ProgramNotificationInstanceController(
      ProgramNotificationInstanceService programNotificationInstanceService,
      EnrollmentService enrollmentService,
      EventService eventService) {
    this.programNotificationInstanceService = programNotificationInstanceService;
    this.enrollmentService = enrollmentService;
    this.eventService = eventService;
  }

  @RequiresAuthority(anyOf = ALL)
  @GetMapping(produces = {"application/json"})
  public @ResponseBody Page<ProgramNotificationInstance> getScheduledMessage(
      @Deprecated(since = "2.41") @RequestParam(required = false) UID programInstance,
      @RequestParam(required = false) UID enrollment,
      @Deprecated(since = "2.41") @RequestParam(required = false) UID programStageInstance,
      @RequestParam(required = false) UID event,
      @RequestParam(required = false) Date scheduledAt,
      @RequestParam(required = false, defaultValue = "true") boolean paging,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestParam(required = false, defaultValue = "50") int pageSize)
      throws BadRequestException, ForbiddenException, NotFoundException {
    UID enrollmentUid =
        validateDeprecatedParameter("programInstance", programInstance, "enrollment", enrollment);
    UID eventUid =
        validateDeprecatedParameter("programStageInstance", programStageInstance, "event", event);

    Event storedEvent = null;
    if (eventUid != null) {
      storedEvent = eventService.getEvent(eventUid);
    }
    Enrollment storedEnrollment = null;
    if (enrollmentUid != null) {
      storedEnrollment = enrollmentService.getEnrollment(enrollmentUid);
    }
    ProgramNotificationInstanceParam params =
        ProgramNotificationInstanceParam.builder()
            .enrollment(storedEnrollment)
            .event(storedEvent)
            .page(page)
            .pageSize(pageSize)
            .paging(paging)
            .scheduledAt(scheduledAt)
            .build();

    List<ProgramNotificationInstance> instances =
        programNotificationInstanceService.getProgramNotificationInstances(params);

    if (paging) {
      long total = programNotificationInstanceService.countProgramNotificationInstances(params);
      return Page.withPager(
          ProgramNotificationInstanceSchemaDescriptor.PLURAL,
          org.hisp.dhis.tracker.Page.withTotals(instances, page, pageSize, total));
    }

    return Page.withoutPager(ProgramNotificationInstanceSchemaDescriptor.PLURAL, instances);
  }
}
