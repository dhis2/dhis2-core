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
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lombok.Data;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class OrderBindingTest {
  private static final String METHOD_PARAMETER_ENDPOINT = "/order/methodParameter";
  private static final String CLASS_PARAMETER_ENDPOINT = "/order/classParameter";
  private MockMvc mockMvc;
  private List<OrderCriteria> actual;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new OrderingController()).build();
  }

  @Test
  void shouldReturnDefaultSortDirectionWhenNoSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(METHOD_PARAMETER_ENDPOINT).param("order", "field"))
        .andExpect(status().isOk());

    assertEquals(List.of(OrderCriteria.of("field", SortDirection.ASC)), actual);
  }

  @Test
  void shouldReturnAscSortDirectionWhenAscSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(METHOD_PARAMETER_ENDPOINT).param("order", "field:asc"))
        .andExpect(status().isOk());

    assertEquals(List.of(OrderCriteria.of("field", SortDirection.ASC)), actual);
  }

  @Test
  void shouldReturnDescSortDirectionWhenDescSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(METHOD_PARAMETER_ENDPOINT).param("order", "field:desc"))
        .andExpect(status().isOk());

    assertEquals(List.of(OrderCriteria.of("field", SortDirection.DESC)), actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {METHOD_PARAMETER_ENDPOINT, CLASS_PARAMETER_ENDPOINT})
  void shouldHandleMultipleOrderComponentsGivenViaOneParameter(String endpoint) throws Exception {
    mockMvc
        .perform(get(endpoint).param("order", "field1:desc,field2:asc"))
        .andExpect(status().isOk());

    assertEquals(
        List.of(
            OrderCriteria.of("field1", SortDirection.DESC),
            OrderCriteria.of("field2", SortDirection.ASC)),
        actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {METHOD_PARAMETER_ENDPOINT, CLASS_PARAMETER_ENDPOINT})
  void shouldHandleMultipleOrderComponentsGivenViaMultipleParameters(String endpoint)
      throws Exception {
    mockMvc
        .perform(get(endpoint).param("order", "field1:desc").param("order", "field2:asc"))
        .andExpect(status().isOk());

    assertEquals(
        List.of(
            OrderCriteria.of("field1", SortDirection.DESC),
            OrderCriteria.of("field2", SortDirection.ASC)),
        actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {METHOD_PARAMETER_ENDPOINT, CLASS_PARAMETER_ENDPOINT})
  void shouldReturnABadRequestWhenMixingRepeatedParameterAndCommaSeparatedValues(String endpoint)
      throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get(endpoint)
                    .accept("application/json")
                    .param("order", "field1:desc")
                    .param("order", "field2:asc,field3:desc"))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith(
        "You likely repeated request parameter 'order' and used", message.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {METHOD_PARAMETER_ENDPOINT, CLASS_PARAMETER_ENDPOINT})
  void shouldReturnABadRequestWhenInvalidSortDirectionIsPassedAsParameter(String endpoint)
      throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get(endpoint).accept("application/json").param("order", "field:wrong"))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertContains(
        "'wrong' is not a valid sort direction. Valid values are: [ASC,", message.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {METHOD_PARAMETER_ENDPOINT, CLASS_PARAMETER_ENDPOINT})
  void shouldReturnABadRequestWhenInvalidSortDirectionIsPassedAsParameterInAListOfOrders(
      String endpoint) throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get(endpoint)
                    .accept("application/json")
                    .param("order", "field1:wrong")
                    .param("order", "field2:asc"))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith("Value 'field1:wrong' is not valid for parameter order", message.getMessage());
    assertContains(
        "'wrong' is not a valid sort direction. Valid values are: [ASC,", message.getMessage());
  }

  @Controller
  private class OrderingController extends CrudControllerAdvice {
    @Data
    static class RequestParams {
      List<OrderCriteria> order;
    }

    @GetMapping(value = CLASS_PARAMETER_ENDPOINT)
    public @ResponseBody WebMessage getOrderViaClassParameter(RequestParams params) {
      actual = params.order;
      return ok();
    }

    @GetMapping(value = METHOD_PARAMETER_ENDPOINT)
    public @ResponseBody WebMessage getOrderViaMethodParameter(
        @RequestParam List<OrderCriteria> order) {
      actual = order;
      return ok();
    }
  }
}
