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

import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParams;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland
 */
@Controller
@RequestMapping("/api/programNotificationTemplates")
@ApiVersion(include = {DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@OpenApi.Document(classifiers = {"team:tracker", "purpose:metadata"})
public class ProgramNotificationTemplateController
    extends AbstractCrudController<ProgramNotificationTemplate, GetObjectListParams> {
  private final ProgramNotificationTemplateService programNotificationTemplateService;

  private final ProgramNotificationTemplateRequestParamsMapper requestParamsMapper;

  public ProgramNotificationTemplateController(
      ProgramNotificationTemplateService programNotificationTemplateService,
      ProgramNotificationTemplateRequestParamsMapper requestParamsMapper) {
    this.programNotificationTemplateService = programNotificationTemplateService;
    this.requestParamsMapper = requestParamsMapper;
  }

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @OpenApi.Response(GetObjectListResponse.class)
  @RequiresAuthority(anyOf = ALL)
  @GetMapping(
      produces = {"application/json"},
      value = "/filter")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<ProgramNotificationTemplate>>
      getProgramNotificationTemplates(ProgramNotificationTemplateRequestParams requestParams)
          throws ConflictException, BadRequestException {
    ProgramNotificationTemplateOperationParams params = requestParamsMapper.map(requestParams);

    List<ProgramNotificationTemplate> entities =
        programNotificationTemplateService.getProgramNotificationTemplates(params);

    Pager pager = null;
    if (params.isPaging()) {
      long totalCount =
          programNotificationTemplateService.countProgramNotificationTemplates(params);
      pager = new Pager(params.getPage(), totalCount, params.getPageSize());
      linkService.generatePagerLinks(pager, getEntityClass());
    }

    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            pager,
            getSchema().getCollectionName(),
            FieldFilterParams.of(entities, List.of("*")),
            false));
  }
}
