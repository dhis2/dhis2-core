/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.better.FieldsPredicate;
import org.hisp.dhis.fieldfiltering.better.FieldsPredicateConverter;
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

class FieldsPredicateConverterTest {
  private MockMvc mockMvc;

  record ExpectField(boolean included, String dotPath) {}

  @BeforeEach
  void setUp() {
    List<ExpectField> expected =
        List.of(
            new ExpectField(true, "attributes"),
            new ExpectField(true, "attributes.attribute"),
            new ExpectField(true, "attributes.value"),
            new ExpectField(true, "deleted"));

    DefaultFormattingConversionService formattingConversionService =
        new DefaultFormattingConversionService();
    formattingConversionService.addConverter(new FieldsPredicateConverter());
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FieldsPredicateController(expected))
            .setConversionService(formattingConversionService)
            .build();
  }

  @Test
  void shouldConvertFieldsPredicateGivenASingleRequestParameter() throws Exception {
    mockMvc
        .perform(get("/test-fields").param("fields", "attributes[attribute,value],deleted"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldConvertFieldsPredicateGivenMultipleRequestParameters() throws Exception {
    mockMvc
        .perform(
            get("/test-fields")
                .param("fields", "attributes[attribute]")
                .param("fields", "attributes[value]")
                .param("fields", "deleted"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldConvertStarField() throws Exception {
    mockMvc.perform(get("/test-fields").param("fields", "*")).andExpect(status().isOk());
  }

  @Test
  void shouldConvertExclusionFields() throws Exception {
    mockMvc.perform(get("/test-fields").param("fields", "*,!deleted")).andExpect(status().isOk());
  }

  @Controller
  @RequiredArgsConstructor
  private static class FieldsPredicateController extends CrudControllerAdvice {
    private final List<ExpectField> expected;

    @GetMapping("/test-fields")
    public @ResponseBody String get(@RequestParam FieldsPredicate fields) {
      assertFields(expected, fields);
      return "";
    }
  }

  public static void assertFields(List<ExpectField> expectFields, FieldsPredicate predicates) {
    for (ExpectField expectField : expectFields) {
      assertField(expectField.included, expectField.dotPath, predicates);
    }
  }

  /**
   * Tests if the field represented by the full path as used by the current FieldFilterParser is
   * included in the parsed fields predicate.
   */
  private static void assertField(
      boolean expected, String expectedDotPath, FieldsPredicate predicate) {
    FieldsPredicate current = predicate;
    String[] segments = expectedDotPath.split("\\.");
    for (int i = 0; i < segments.length; i++) {
      if (i < segments.length - 1) {
        current = current.getChildren().get(segments[i]);
      } else {
        // TODO(ivo) expose a view or so of the effective set
        String what = expected ? "include" : "exclude";
        assertEquals(
            expected,
            current.test(segments[i]),
            "predicate with fields "
                + current.getIncludes()
                + " does not "
                + what
                + " "
                + expectedDotPath);
      }
    }
  }
}
