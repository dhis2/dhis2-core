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
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validatePaginationParameters;

import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParams;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
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
public class ProgramNotificationTemplateController
    extends AbstractCrudController<ProgramNotificationTemplate> {
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

  @RequiresAuthority(anyOf = ALL)
  @GetMapping(
      produces = {"application/json"},
      value = "/filter")
  public @ResponseBody Page<ProgramNotificationTemplate> getProgramNotificationTemplates(
      ProgramNotificationTemplateRequestParams requestParams)
      throws ConflictException, BadRequestException {
    validatePaginationParameters(requestParams);

    ProgramNotificationTemplateOperationParams params = requestParamsMapper.map(requestParams);

    List<ProgramNotificationTemplate> instances =
        programNotificationTemplateService.getProgramNotificationTemplates(params);

    if (params.isPaged()) {
      long total = programNotificationTemplateService.countProgramNotificationTemplates(params);
      return Page.withPager(
          ProgramNotificationTemplateSchemaDescriptor.PLURAL,
          org.hisp.dhis.tracker.export.Page.withTotals(
              instances, params.getPage(), params.getPageSize(), total));
    }

    return Page.withoutPager(ProgramNotificationTemplateSchemaDescriptor.PLURAL, instances);
  }
}
