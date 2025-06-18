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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.copy.CopyService;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:tracker", "purpose:metadata"})
public class ProgramController
    extends AbstractCrudController<Program, ProgramController.GetProgramObjectListParams> {

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class GetProgramObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        "Limit the results to programs accessible to the current user based on data sharing read access instead of metadata sharing read access.")
    boolean userFilter;
  }

  private final ProgramService programService;

  private final CopyService copyService;

  @Override
  protected void modifyGetObjectList(GetProgramObjectListParams params, Query<Program> query) {
    if (params.isUserFilter()) {
      query.setSkipSharing(true);
      query.setDataSharing(true);
    }
  }

  @GetMapping("/{uid}/metadata")
  public ResponseEntity<MetadataExportParams> getProgramWithDependencies(
      @PathVariable("uid") String pvUid,
      @RequestParam(required = false, defaultValue = "false") boolean download)
      throws WebMessageException {
    Program program = programService.getProgram(pvUid);

    if (program == null) {
      throw new WebMessageException(notFound("Program not found: " + pvUid));
    }

    MetadataExportParams exportParams =
        exportService.getParamsFromMap(contextService.getParameterValuesMap());
    exportService.validate(exportParams);
    exportParams.setObjectExportWithDependencies(program);

    return ResponseEntity.ok(exportParams);
  }

  @PostMapping("/{uid}/copy")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public WebMessage copyProgram(
      @PathVariable("uid") String uid,
      @RequestParam(required = false) Map<String, String> copyOptions)
      throws NotFoundException, ConflictException, ForbiddenException {
    Program programCopy = null;
    try {
      programCopy = copyService.copyProgram(uid, copyOptions);
    } catch (DataIntegrityViolationException dive) {
      String exceptionMessage = getRootCauseMessageIfPossible(dive);
      throw new ConflictException(exceptionMessage);
    }
    return created(("Program created: '%s'".formatted(programCopy.getUid())))
        .setLocation("/programs/" + programCopy.getUid());
  }

  @ResponseBody
  @GetMapping(value = "orgUnits")
  public Map<String, Collection<String>> getOrgUnitsAssociations(
      @RequestParam(value = "programs") Set<String> programs) {
    return Optional.ofNullable(programs)
        .filter(CollectionUtils::isNotEmpty)
        .map(programService::getProgramOrganisationUnitsAssociationsForCurrentUser)
        .map(SetValuedMap::asMap)
        .orElseThrow(
            () -> new IllegalArgumentException("At least one program uid must be specified"));
  }

  private String getRootCauseMessageIfPossible(DataIntegrityViolationException dive) {
    String exceptionMessage = "";
    if (null != dive.getRootCause()) {
      Throwable throwable = dive.getRootCause();
      if (null != throwable) {
        exceptionMessage = throwable.getMessage();
      }
    } else {
      exceptionMessage = dive.getMessage();
    }
    return exceptionMessage;
  }
}
