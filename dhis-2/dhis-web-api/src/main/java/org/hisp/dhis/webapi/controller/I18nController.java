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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.i18n.I18nInput;
import org.hisp.dhis.webapi.webdomain.i18n.I18nOutput;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping(value = "/i18n")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class I18nController {
  private final I18nManager i18nManager;

  private final ObjectMapper jsonMapper;

  public I18nController(I18nManager i18nManager, ObjectMapper jsonMapper) {
    this.i18nManager = i18nManager;
    this.jsonMapper = jsonMapper;
  }

  @PostMapping
  public @ResponseBody I18nOutput postI18n(
      @RequestParam(value = "package", required = false, defaultValue = "org.hisp.dhis")
          String searchPackage,
      HttpServletResponse response,
      InputStream inputStream)
      throws Exception {
    I18n i18n = i18nManager.getI18n(searchPackage);
    I18nOutput output = new I18nOutput();
    I18nInput input = jsonMapper.readValue(inputStream, I18nInput.class);

    for (String key : input) {
      String value = i18n.getString(key);

      if (value != null) {
        output.getTranslations().put(key, value);
      }
    }

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    return output;
  }
}
