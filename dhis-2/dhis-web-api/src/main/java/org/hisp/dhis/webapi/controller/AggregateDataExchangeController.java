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
package org.hisp.dhis.webapi.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchangeService;
import org.hisp.dhis.dataexchange.aggregate.SourceDataQueryParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aggregateDataExchanges")
public class AggregateDataExchangeController
    extends AbstractCrudController<AggregateDataExchange, GetObjectListParams> {
  private final AggregateDataExchangeService service;

  @PostMapping("/exchange")
  @ResponseStatus(value = HttpStatus.OK)
  public WebMessage runDataExchange(
      @RequestBody AggregateDataExchange exchange, @CurrentUser UserDetails userDetails) {
    return WebMessageUtils.importSummaries(
        service.exchangeData(userDetails, exchange, JobProgress.noop()));
  }

  @PostMapping("/{uid}/exchange")
  @ResponseStatus(value = HttpStatus.OK)
  public WebMessage runDataExchangeByUid(
      @PathVariable String uid, @CurrentUser UserDetails userDetails) {
    return WebMessageUtils.importSummaries(
        service.exchangeData(userDetails, uid, JobProgress.noop()));
  }

  @GetMapping("/{uid}/sourceData")
  @ResponseStatus(value = HttpStatus.OK)
  public List<Grid> getSourceData(
      @PathVariable String uid, SourceDataQueryParams params, @CurrentUser UserDetails userDetails)
      throws ForbiddenException {
    return service.getSourceData(userDetails, uid, params);
  }

  @GetMapping("/{uid}/sourceDataValueSets")
  @ResponseStatus(value = HttpStatus.OK)
  public List<DataValueSet> getSourceDataValueSets(
      @PathVariable String uid, SourceDataQueryParams params, @CurrentUser UserDetails userDetails)
      throws ForbiddenException {
    return service.getSourceDataValueSets(userDetails, uid, params);
  }
}
