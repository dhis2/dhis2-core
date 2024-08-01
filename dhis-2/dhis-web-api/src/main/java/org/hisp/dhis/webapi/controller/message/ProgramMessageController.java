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
package org.hisp.dhis.webapi.controller.message;

import static java.lang.String.format;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_MOBILE_SENDSMS;
import static org.hisp.dhis.security.Authorities.F_SEND_EMAIL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageOperationParams;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractFullReadOnlyController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Zubair <rajazubair.asghar@gmail.com> */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ProgramMessageController extends AbstractFullReadOnlyController<ProgramMessage> {
  private final ProgramMessageService programMessageService;
  private final ProgramMessageRequestParamsMapper paramsMapper;
  private final RenderService renderService;

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<List<ProgramMessage>> getProgramMessages(
      ProgramMessageRequestParams requestParams)
      throws BadRequestException, ConflictException, NotFoundException {
    ProgramMessageOperationParams params = paramsMapper.map(requestParams);

    return ResponseEntity.ok(programMessageService.getProgramMessages(params));
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(value = "/scheduled/sent", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProgramMessage> getScheduledSentMessage(ProgramMessageRequestParams requestParams)
      throws BadRequestException, ConflictException, NotFoundException {
    requestParams.setMessageStatus(ProgramMessageStatus.SENT);
    ProgramMessageOperationParams params = paramsMapper.map(requestParams);

    return programMessageService.getProgramMessages(params);
  }

  @RequiresAuthority(anyOf = {F_MOBILE_SENDSMS, F_SEND_EMAIL})
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public BatchResponseStatus sendMessages(HttpServletRequest request) throws IOException {
    ProgramMessagesImportBatch batch =
        renderService.fromJson(request.getInputStream(), ProgramMessagesImportBatch.class);

    for (ProgramMessage programMessage : batch.getProgramMessages()) {
      programMessageService.validatePayload(programMessage);
    }

    return programMessageService.sendMessages(batch.getProgramMessages());
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PutMapping(value = "/{uid}", produces = APPLICATION_JSON_VALUE)
  public WebMessage updateProgramMessage(
      @PathVariable UID uid, ProgramMessageUpdateRequest updatedRequest) throws NotFoundException {
    programMessageService.updateProgramMessage(uid, updatedRequest.getStatus());
    return ok(format("ProgramMessage with id %s updated", uid));
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @DeleteMapping(value = "/{uid}", produces = APPLICATION_JSON_VALUE)
  public WebMessage deleteProgramMessage(@PathVariable UID uid) throws NotFoundException {
    programMessageService.deleteProgramMessage(uid);
    return ok(format("ProgramMessage with id %s deleted", uid));
  }
}
