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

import java.util.List;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class OrderBindingTest {
  private static final String ENDPOINT = "/ordering";

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new OrderingController())
            .setControllerAdvice(new CrudControllerAdvice())
            .build();
  }

  @Test
  void shouldReturnDefaultSortDirectionWhenNoSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("order", "field"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("field")))
        .andExpect(content().string(containsString("ASC")));
  }

  @Test
  void shouldReturnAscSortDirectionWhenAscSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("order", "field:asc"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("field")))
        .andExpect(content().string(containsString("ASC")));
  }

  @Test
  void shouldReturnDescSortDirectionWhenDescSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("order", "field:desc"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("field")))
        .andExpect(content().string(containsString("DESC")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidSortDirectionIsPassedAsParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("order", "field:wrong"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "'wrong' is not a valid sort direction. Valid values are: [ASC, DESC]")));
  }

  @Test
  void shouldReturnABadRequestWhenInvalidSortDirectionIsPassedAsParameterInAListOfOrders()
      throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("order", "field1:wrong").param("order", "field2:asc"))
        .andExpect(content().string(containsString("Bad Request")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "'wrong' is not a valid sort direction. Valid values are: [ASC, DESC]")));
  }

  @Controller
  private static class OrderingController extends CrudControllerAdvice {
    @GetMapping(value = ENDPOINT)
    public @ResponseBody WebMessage getOrder(@RequestParam List<OrderCriteria> order) {
      return ok(order.toString());
    }
  }
}
