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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.objectReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.common.input.ReplaceTranslationsParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.TranslationsCheck;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeParams;
import org.hisp.dhis.period.PeriodTypes;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.translation.Translation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  private final PeriodService periodService;
  private final PeriodStore periodStore;

  @RequiresAuthority(anyOf = ALL)
  @PutMapping
  public WebMessage putLabel(@RequestBody PeriodTypeParams params) throws NotFoundException {
    // kept for API backwards compatibility with 43
    return putLabel(params.name(), params.locale(), params.label());
  }

  @RequiresAuthority(anyOf = ALL)
  @PutMapping("/{name}")
  public WebMessage putLabel(
      @PathVariable("name") String name,
      @RequestParam(required = false) Locale locale,
      @RequestParam(required = false) String value)
      throws NotFoundException {
    if (periodStore.updatePeriodTypeLabel(name, value, locale))
      return ok(name + " updated successfully.");
    throw new NotFoundException(PeriodType.class, name);
  }

  @RequiresAuthority(anyOf = ALL)
  @PutMapping("/{name}/translations")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public WebMessage replaceTranslations(
      @PathVariable("name") String name, @RequestBody ReplaceTranslationsParams params)
      throws NotFoundException {

    List<Translation> translations = params.translations();
    if (translations == null) translations = List.of();

    ObjectReport report = new ObjectReport(PeriodType.class, 0);
    TranslationsCheck.checkTranslations(translations, report::addErrorReport);
    if (!report.hasErrorReports()) {
      if (periodStore.updatePeriodTypeLabel(name, translations)) return null;
      throw new NotFoundException(PeriodType.class, name);
    }
    return objectReport(report);
  }

  @GetMapping
  public PeriodTypes getPeriodTypes(
      @RequestParam(required = false) Locale locale,
      @RequestParam(defaultValue = "*") String fields) {
    return periodService.getAllPeriodTypes(locale, Fields.of(fields));
  }

  @GetMapping("/{name}")
  public PeriodTypes.PeriodTypeEntry getPeriodType(
      @PathVariable("name") String name,
      @RequestParam(required = false) Locale locale,
      @RequestParam(defaultValue = "*") String fields)
      throws NotFoundException {
    PeriodTypes.PeriodTypeEntry entry =
        periodService.getAllPeriodTypes(locale, Fields.of(fields)).periodTypes().stream()
            .filter(pt -> name.equalsIgnoreCase(pt.name()))
            .findFirst()
            .orElse(null);
    if (entry == null) throw new NotFoundException(PeriodType.class, name);
    return entry;
  }

  @GetMapping(
      value = "/relativePeriodTypes",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public RelativePeriodEnum[] getRelativePeriodTypes() {
    return RelativePeriodEnum.values();
  }
}
