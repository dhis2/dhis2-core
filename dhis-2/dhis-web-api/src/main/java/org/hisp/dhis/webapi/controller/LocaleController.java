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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.i18n.I18nLocaleService;
import org.hisp.dhis.i18n.locale.I18nLocale;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.system.util.LocaleUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@OpenApi.Tags("ui")
@Controller
@RequestMapping(value = "/locales")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class LocaleController {
  @Autowired private LocaleManager localeManager;

  @Autowired private I18nLocaleService localeService;

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @GetMapping(value = "/ui")
  public @ResponseBody List<WebLocale> getUiLocales(Model model) {
    List<Locale> locales = localeManager.getAvailableLocales();
    List<WebLocale> webLocales =
        locales.stream().map(WebLocale::fromLocale).collect(Collectors.toList());

    return webLocales;
  }

  @GetMapping(value = "/db")
  public @ResponseBody List<WebLocale> getDbLocales() {
    List<Locale> locales = localeService.getAllLocales();
    List<WebLocale> webLocales =
        locales.stream().map(WebLocale::fromLocale).collect(Collectors.toList());
    return webLocales;
  }

  @GetMapping(value = "/languages", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody Map<String, String> getAvailableLanguages() {
    return localeService.getAvailableLanguages();
  }

  @GetMapping(value = "/countries", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody Map<String, String> getAvailableCountries() {
    return localeService.getAvailableCountries();
  }

  @GetMapping(value = "/dbLocales", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<I18nLocale> getDbLocalesWithId() {
    return localeService.getAllI18nLocales();
  }

  @GetMapping(value = "/dbLocales/{uid}", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody I18nLocale getObject(
      @PathVariable("uid") String uid, HttpServletResponse response) throws Exception {
    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
    I18nLocale locale = localeService.getI18nLocaleByUid(uid);

    if (locale == null) {
      throw new WebMessageException(notFound("Cannot find Locale with uid: " + uid));
    }

    return locale;
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_LOCALE_ADD')")
  @PostMapping(value = "/dbLocales")
  @ResponseBody
  public WebMessage addLocale(
      @RequestParam String country, @RequestParam String language, HttpServletResponse response) {
    if (StringUtils.isEmpty(country) || StringUtils.isEmpty(language)) {
      return conflict("Invalid country or language code.");
    }

    String localeCode = LocaleUtils.getLocaleString(language, country, null);

    Locale locale = LocaleUtils.getLocale(localeCode);

    if (locale != null) {
      I18nLocale i18nLocale = localeService.getI18nLocale(locale);

      if (i18nLocale != null) {
        return conflict("Locale code existed.");
      }
    }

    I18nLocale i18nLocale = localeService.addI18nLocale(language, country);

    return created("Locale created successfully").setLocation("/locales/" + i18nLocale.getUid());
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_LOCALE_DELETE')")
  @DeleteMapping(path = "/dbLocales/{uid}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String uid) throws Exception {
    I18nLocale i18nLocale = localeService.getI18nLocaleByUid(uid);

    if (i18nLocale == null) {
      throw new WebMessageException(notFound("Cannot find Locale with uid " + uid));
    }

    localeService.deleteI18nLocale(i18nLocale);
  }
}
