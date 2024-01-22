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

import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateParam;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland
 */
@OpenApi.Tags("tracker")
@Controller
@RequestMapping(value = ProgramNotificationTemplateSchemaDescriptor.API_ENDPOINT)
@ApiVersion(include = {DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ProgramNotificationTemplateController
    extends AbstractCrudController<ProgramNotificationTemplate> {
  private final ProgramService programService;

  private final ProgramStageService programStageService;

  private final ProgramNotificationTemplateService programNotificationTemplateService;

  public ProgramNotificationTemplateController(
      ProgramService programService,
      ProgramStageService programStageService,
      ProgramNotificationTemplateService programNotificationTemplateService) {
    this.programService = programService;
    this.programStageService = programStageService;
    this.programNotificationTemplateService = programNotificationTemplateService;
  }

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @PreAuthorize("hasRole('ALL')")
  @GetMapping(
      produces = {"application/json"},
      value = "/filter")
  public @ResponseBody Page<ProgramNotificationTemplate> getProgramNotificationTemplates(
      @RequestParam(required = false) String program,
      @RequestParam(required = false) String programStage,
      // @deprecated use {@code paging} instead
      @Deprecated(since = "2.41") @RequestParam(required = false) Boolean skipPaging,
      // TODO(tracker): set paging=true once skipPaging is removed. Both cannot have a default right
      // now. This would lead to invalid parameters if the user passes the other param i.e.
      // skipPaging==paging.
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestParam(required = false, defaultValue = "50") int pageSize)
      throws BadRequestException {
    if (paging != null && skipPaging != null && paging.equals(skipPaging)) {
      throw new BadRequestException(
          "Paging can either be enabled or disabled. Prefer 'paging' as 'skipPaging' will be removed.");
    }
    boolean isPaged = isPaged(paging, skipPaging);

    ProgramNotificationTemplateParam params =
        ProgramNotificationTemplateParam.builder()
            .program(programService.getProgram(program))
            .programStage(programStageService.getProgramStage(programStage))
            .skipPaging(!isPaged)
            .page(page)
            .pageSize(pageSize)
            .build();

    List<ProgramNotificationTemplate> instances =
        programNotificationTemplateService.getProgramNotificationTemplates(params);

    if (isPaged) {
      long total = programNotificationTemplateService.countProgramNotificationTemplates(params);

      Pager pager = new Pager(page, total, pageSize);
      pager.force(page, pageSize);
      return Page.withPager(
          ProgramNotificationTemplateSchemaDescriptor.PLURAL,
          instances,
          org.hisp.dhis.tracker.export.Page.of(instances, pager, true));
    }

    return Page.withoutPager(ProgramNotificationTemplateSchemaDescriptor.PLURAL, instances);
  }

  /**
   * Indicates whether to return a page of items or all items. By default, responses are paginated.
   *
   * <p>Note: this assumes {@code paging} and {@code skipPaging} have been validated. Preference is
   * given to {@code paging} as the other parameter is deprecated.
   */
  private static boolean isPaged(Boolean paging, Boolean skipPaging) {
    if (paging != null) {
      return Boolean.TRUE.equals(paging);
    }

    if (skipPaging != null) {
      return Boolean.FALSE.equals(skipPaging);
    }

    return true;
  }
}
