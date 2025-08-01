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
package org.hisp.dhis.webapi.controller.message;

import static org.hisp.dhis.security.Authorities.F_MOBILE_SENDSMS;
import static org.hisp.dhis.security.Authorities.F_SEND_EMAIL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageBatch;
import org.hisp.dhis.program.message.ProgramMessageOperationParams;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Zubair <rajazubair.asghar@gmail.com> */
@RestController
@RequestMapping("/api/messages")
@OpenApi.Document(classifiers = {"team:tracker", "purpose:metadata"})
public class ProgramMessageController
    extends AbstractCrudController<ProgramMessage, GetObjectListParams> {
  @Autowired private ProgramMessageService programMessageService;

  @Autowired protected ProgramMessageRequestParamMapper requestParamMapper;

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProgramMessage> getProgramMessages(ProgramMessageRequestParams requestParams)
      throws BadRequestException, ConflictException, NotFoundException {
    ProgramMessageOperationParams params = requestParamMapper.map(requestParams);

    List<ProgramMessage> programMessages = programMessageService.getProgramMessages(params);
    programMessages.forEach(this::setEvent);
    return programMessages;
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(value = "/scheduled/sent", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProgramMessage> getScheduledSentMessage(ProgramMessageRequestParams params)
      throws BadRequestException, ConflictException, NotFoundException {
    params.setMessageStatus(ProgramMessageStatus.SENT);
    ProgramMessageOperationParams operationParams = requestParamMapper.map(params);

    List<ProgramMessage> programMessages =
        programMessageService.getProgramMessages(operationParams);
    programMessages.forEach(this::setEvent);
    return programMessages;
  }

  // -------------------------------------------------------------------------
  // POST
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = {F_MOBILE_SENDSMS, F_SEND_EMAIL})
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public BatchResponseStatus sendMessages(HttpServletRequest request) throws IOException {
    ProgramMessageBatch batch =
        renderService.fromJson(request.getInputStream(), ProgramMessageBatch.class);

    for (ProgramMessage programMessage : batch.getProgramMessages()) {
      programMessageService.validatePayload(programMessage);
    }

    return programMessageService.sendMessages(batch.getProgramMessages());
  }

  private void setEvent(ProgramMessage programMessage) {
    TrackerEvent trackerEvent = programMessage.getTrackerEvent();
    SingleEvent singleEvent = programMessage.getSingleEvent();
    if (trackerEvent != null) {
      programMessage.setEvent(trackerEvent);
    } else if (singleEvent != null) {
      programMessage.setEvent(TrackerObjectsMapper.map(singleEvent));
    }
  }
}
