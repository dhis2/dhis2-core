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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageBatch;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Zubair <rajazubair.asghar@gmail.com> */
@RestController
@RequestMapping(value = "/messages")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ProgramMessageController extends AbstractCrudController<ProgramMessage> {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private ProgramMessageService programMessageService;

  @Autowired private RenderService renderService;

  @Autowired
  @Qualifier("org.hisp.dhis.program.notification.ProgramNotificationInstanceStore")
  private IdentifiableObjectStore<ProgramNotificationInstance> programNotificationInstanceStore;

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @PreAuthorize("hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')")
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProgramMessage> getProgramMessages(
      @RequestParam(required = false) Set<String> ou,
      @RequestParam(required = false) String programInstance,
      @RequestParam(required = false) String programStageInstance,
      @RequestParam(required = false) ProgramMessageStatus messageStatus,
      @RequestParam(required = false) Date afterDate,
      @RequestParam(required = false) Date beforeDate,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize)
      throws WebMessageException {
    ProgramMessageQueryParams params =
        programMessageService.getFromUrl(
            ou,
            programInstance,
            programStageInstance,
            messageStatus,
            page,
            pageSize,
            afterDate,
            beforeDate);

    if (programInstance == null && programStageInstance == null) {
      throw new WebMessageException(
          conflict("ProgramInstance or ProgramStageInstance must be specified."));
    }

    return programMessageService.getProgramMessages(params);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')")
  @GetMapping(value = "/scheduled/sent", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProgramMessage> getScheduledSentMessage(
      @RequestParam(required = false) String programInstance,
      @RequestParam(required = false) String programStageInstance,
      @RequestParam(required = false) Date afterDate,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    ProgramMessageQueryParams params =
        programMessageService.getFromUrl(
            null, programInstance, programStageInstance, null, page, pageSize, afterDate, null);

    return programMessageService.getProgramMessages(params);
  }

  // -------------------------------------------------------------------------
  // POST
  // -------------------------------------------------------------------------

  @PreAuthorize("hasRole('ALL') or hasRole('F_MOBILE_SENDSMS') or hasRole('F_SEND_EMAIL')")
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public BatchResponseStatus saveMessages(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ProgramMessageBatch batch =
        renderService.fromJson(request.getInputStream(), ProgramMessageBatch.class);

    for (ProgramMessage programMessage : batch.getProgramMessages()) {
      programMessageService.validatePayload(programMessage);
    }

    return programMessageService.sendMessages(batch.getProgramMessages());
  }
}
