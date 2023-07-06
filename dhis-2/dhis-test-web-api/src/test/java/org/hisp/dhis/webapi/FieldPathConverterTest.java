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
package org.hisp.dhis.webapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPathConverter;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class FieldPathConverterTest {
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    List<FieldPath> expected =
        List.of(
            new FieldPath("attributes", Collections.emptyList()),
            new FieldPath("attribute", List.of("attributes")),
            new FieldPath("value", List.of("attributes")),
            new FieldPath("deleted", Collections.emptyList()));

    DefaultFormattingConversionService formattingConversionService =
        new DefaultFormattingConversionService();
    formattingConversionService.addConverter(new FieldPathConverter());
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FieldPathController(expected))
            .setConversionService(formattingConversionService)
            .build();
  }

  @Test
  void shouldConvertFieldPathGivenASingleRequestParameter() throws Exception {
    mockMvc
        .perform(get("/test").param("fields", "attributes[attribute,value],deleted"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldConvertFieldPathGivenMultipleRequestParameters() throws Exception {
    mockMvc
        .perform(
            get("/test")
                .param("fields", "attributes[attribute]")
                .param("fields", "attributes[value]")
                .param("fields", "deleted"))
        .andExpect(status().isOk());
  }

  @Controller
  @RequiredArgsConstructor
  private static class FieldPathController extends CrudControllerAdvice {
    private final List<FieldPath> expected;

    @GetMapping("/test")
    public @ResponseBody String get(@RequestParam List<FieldPath> fields) {
      assertEquals(expected, fields);
      return "";
    }
  }
}
