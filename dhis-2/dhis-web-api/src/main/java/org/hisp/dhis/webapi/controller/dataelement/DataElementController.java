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
package org.hisp.dhis.webapi.controller.dataelement;

import static org.hisp.dhis.security.Authorities.F_DATA_ELEMENT_MERGE;
import static org.hisp.dhis.webapi.controller.CrudControllerAdvice.getHelpfulMessage;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Controller
@RequestMapping("/api/dataElements")
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class DataElementController
    extends AbstractCrudController<DataElement, GetObjectListParams> {

  private final DataElementService dataElementService;
  private final MergeService dataElementMergeService;

  @ResponseStatus(HttpStatus.OK)
  @RequiresAuthority(anyOf = F_DATA_ELEMENT_MERGE)
  @PostMapping(value = "/merge", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage mergeDataElements(@RequestBody MergeParams params)
      throws ConflictException {
    log.info("Data element merge received");

    MergeReport report;
    try {
      report = dataElementMergeService.processMerge(params);
    } catch (PersistenceException ex) {
      String helpfulMessage = getHelpfulMessage(ex);
      log.error("Error while processing Data element merge: {}", helpfulMessage);
      throw ex;
    }

    log.info("Data element merge processed with report: {}", report);
    return WebMessageUtils.mergeReport(report);
  }

  @Override
  protected void preCreateEntity(DataElement entity) {
    dataElementService.validateDateElement(entity);
  }

  @Override
  protected void preUpdateEntity(DataElement entity, DataElement newEntity) {
    dataElementService.validateDateElement(newEntity);
  }
}
