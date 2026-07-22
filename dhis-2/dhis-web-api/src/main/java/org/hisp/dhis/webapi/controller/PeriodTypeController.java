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
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeParams;
import org.hisp.dhis.period.PeriodTypeResponse;
import org.hisp.dhis.period.PeriodTypeResponse.PeriodTypeEntry;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.security.RequiresAuthority;
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
    entity = PeriodType.class,
    classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequestMapping("/api/periodTypes")
@RequiredArgsConstructor
public class PeriodTypeController {

  private final I18nManager i18nManager;

  private final PeriodService periodService;

  @RequiresAuthority(anyOf = ALL)
  @PutMapping
  public WebMessage putPeriodType(@RequestBody PeriodTypeParams periodType) {
    periodService.updatePeriodTypeLabel(periodType.name(), periodType.label());

    return ok(periodType.name() + " updated successfully.");
  }

  @GetMapping
  public PeriodTypeResponse getPeriodTypes(@RequestParam(defaultValue = "*") String fields) {

    I18n i18n = i18nManager.getI18n();

    List<PeriodTypeEntry> periodTypes =
        PeriodType.getAvailablePeriodTypes().stream()
            .map(periodType -> new PeriodTypeEntry(periodType, null, i18n))
            .toList();

    return new PeriodTypeResponse(periodTypes);
  }

  @GetMapping(
      value = "/relativePeriodTypes",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public RelativePeriodEnum[] getRelativePeriodTypes() {
    return RelativePeriodEnum.values();
  }
}
