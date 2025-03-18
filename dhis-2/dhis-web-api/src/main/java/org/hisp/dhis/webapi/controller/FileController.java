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
import static org.hisp.dhis.security.Authorities.F_INSERT_CUSTOM_JS_CSS;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = SystemSetting.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/files")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class FileController {

  private final SystemSettingsService settingsService;
  private final ContextUtils contextUtils;

  // -------------------------------------------------------------------------
  // Custom script
  // -------------------------------------------------------------------------

  @OpenApi.Response(Serializable.class)
  @GetMapping("/script")
  public void getCustomScript(HttpServletResponse response, Writer writer, SystemSettings settings)
      throws IOException {
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JAVASCRIPT, CacheStrategy.CACHE_TWO_WEEKS);

    String content = settings.getCustomJs();

    if (content != null) {
      writer.write(content);
    }
  }

  @PostMapping(value = "/script", consumes = "application/javascript")
  @RequiresAuthority(anyOf = F_INSERT_CUSTOM_JS_CSS)
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  public WebMessage postCustomScript(@RequestBody String content) {
    if (content != null) {
      settingsService.put("keyCustomJs", content);
      return ok("Custom script created");
    }
    return null;
  }

  @DeleteMapping("/script")
  @RequiresAuthority(anyOf = F_INSERT_CUSTOM_JS_CSS)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeCustomScript() {
    settingsService.deleteAll(Set.of("keyCustomJs"));
  }

  // -------------------------------------------------------------------------
  // Custom style
  // -------------------------------------------------------------------------

  /**
   * The style/external mapping enables style to be reached from login page / before authentication.
   */
  @OpenApi.Response(Serializable.class)
  @GetMapping(value = {"/style", "/style/external"})
  public void getCustomStyle(HttpServletResponse response, Writer writer, SystemSettings settings)
      throws IOException {
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_CSS, CacheStrategy.CACHE_TWO_WEEKS);

    String content = settings.getCustomCss();

    if (content != null) {
      writer.write(content);
    }
  }

  @PostMapping(value = "/style", consumes = "text/css")
  @RequiresAuthority(anyOf = F_INSERT_CUSTOM_JS_CSS)
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  public WebMessage postCustomStyle(@RequestBody String content) {
    if (content != null) {
      settingsService.put("keyCustomCss", content);
      return ok("Custom style created");
    }
    return null;
  }

  @DeleteMapping("/style")
  @RequiresAuthority(anyOf = F_INSERT_CUSTOM_JS_CSS)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeCustomStyle() {
    settingsService.deleteAll(Set.of("keyCustomCss"));
  }
}
