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

import static org.hamcrest.core.StringContains.containsString;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class SpringBindingTest {
  private static final String ENDPOINT = "/binding";

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new BindingController())
            .setControllerAdvice(new CrudControllerAdvice())
            .build();
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForEnumIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("simpleEnum", "INVALID"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value 'INVALID' is not valid for parameter simpleEnum. Valid values are: [YES, NO]")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForEnumInBindingObjectIsPassedAsParameter()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/criteria").param("simpleEnumInCriteria", "INVALID"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value 'INVALID' is not valid for parameter simpleEnumInCriteria. Valid values are: [YES, NO]")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForDoubleInBindingObjectIsPassedAsParameter()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/criteria").param("doubleNumber", "INVALID"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value 'INVALID' is not valid for parameter doubleNumber. It should be of type double")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForIntegerInBindingObjectIsPassedAsParameter()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/criteria").param("integerNumber", "10.5"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value '10.5' is not valid for parameter integerNumber. For input string: \"10.5\"")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForDateInBindingObjectIsPassedAsParameter()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/criteria").param("date", "INVALID"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value 'INVALID' is not valid for parameter date. Invalid format: \"INVALID\"")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidValueForBooleanInBindingObjectIsPassedAsParameter()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/criteria").param("booleanValue", "INVALID"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Value 'INVALID' is not valid for parameter booleanValue. Invalid boolean value [INVALID]")));
  }

  @Test
  void shouldReturnADefaultValueWhenDefaultParameterIsNotPassed() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/default"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("3")));
  }

  @Test
  void shouldReturnPassedValueWhenDefaultParameterIsPassed() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/default").param("defaultValue", "0"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("0")));
  }

  @Controller
  private class BindingController extends CrudControllerAdvice {
    @GetMapping(value = ENDPOINT)
    public @ResponseBody WebMessage getValue(@RequestParam SimpleEnum simpleEnum) {
      return ok(simpleEnum.name());
    }

    @GetMapping(value = ENDPOINT + "/criteria")
    public @ResponseBody WebMessage getValue(Criteria criteria) {
      return ok(criteria.toString());
    }

    @GetMapping(value = ENDPOINT + "/default")
    public @ResponseBody WebMessage getDefault(Criteria criteria) {
      return ok(criteria.getDefaultValue().toString());
    }
  }

  @NoArgsConstructor
  @Data
  private class Criteria {
    private SimpleEnum simpleEnumInCriteria;

    private Date date;

    private double doubleNumber;

    private Integer integerNumber;

    private Boolean booleanValue;

    private Integer defaultValue = 3;
  }

  private enum SimpleEnum {
    YES,
    NO
  }
}
