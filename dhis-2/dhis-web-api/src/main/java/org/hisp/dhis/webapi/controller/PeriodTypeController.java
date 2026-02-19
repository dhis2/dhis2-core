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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.jackson.domain.JsonRoot;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.webdomain.PeriodType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = Period.class,
    classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequestMapping("/api/periodTypes")
@RequiredArgsConstructor
public class PeriodTypeController {
  private final FieldFilterService fieldFilterService;

  private final I18nManager i18nManager;

  private final PeriodService periodService;

  @OpenApi.Response(
      status = Status.OK,
      object = {@OpenApi.Property(name = "periodType", value = PeriodType.class)})
  @PutMapping
  public WebMessage putPeriodType(@RequestBody PeriodType periodType) {
    periodService.updatePeriodTypeLabel(periodType.getName(), periodType.getLabel());

    return ok(periodType.getName() + " updated successfully.");
  }

  @OpenApi.Response(
      status = Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "periodTypes", value = PeriodType[].class)
      })
  @GetMapping
  @RequiresAuthority(anyOf = ALL)
  public ResponseEntity<JsonRoot> getPeriodTypes(
      @RequestParam(defaultValue = "*") List<String> fields) {
    I18n i18n = i18nManager.getI18n();

    var periodTypes =
        periodService.loadAllPeriodTypes().stream()
            .map(periodType -> new PeriodType(periodType, i18n))
            .collect(Collectors.toList());

    var params = FieldFilterParams.of(periodTypes, fields);
    var objectNodes = fieldFilterService.toObjectNodes(params);

    return ResponseEntity.ok(JsonRoot.of("periodTypes", objectNodes));
  }

  @GetMapping(
      value = "/relativePeriodTypes",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public RelativePeriodEnum[] getRelativePeriodTypes() {
    return RelativePeriodEnum.values();
  }
}
