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
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.webdomain.EndDate;
import org.hisp.dhis.webapi.webdomain.StartDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

class DatesBindingTest {
  private static final String ENDPOINT = "/binding";

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new BindingController()).build();
  }

  @Test
  void shouldReturnADateAtTheEndOfTheDayWhenAnEndDateIsPassedWithoutTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("endDate", "2001-06-17"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("2001-06-17T23:59:59.999")));
  }

  @Test
  void shouldReturnADateWithTimeWhenAnEndDateIsPassedWithTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("endDate", "2001-06-17T16:45:34"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("2001-06-17T16:45:34")));
  }

  @Test
  void shouldReturnADateAtTheStartOfTheDayWhenAnStartDateIsPassedWithoutTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("startDate", "2001-06-17"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("2001-06-17T00:00:00.000")));
  }

  @Test
  void shouldReturnADateWithTimeWhenAnStartDateIsPassedWithTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("startDate", "2001-06-17T16:45:34"))
        .andExpect(content().string(containsString("OK")))
        .andExpect(content().string(containsString("2001-06-17T16:45:34")));
  }

  @Controller
  private class BindingController {
    @GetMapping(value = ENDPOINT)
    public @ResponseBody WebMessage getDefault(Criteria criteria) {
      Date startDate = criteria.getStartDate() == null ? null : criteria.getStartDate().getDate();
      Date endDate = criteria.getEndDate() == null ? null : criteria.getEndDate().getDate();
      return ok(DateUtils.toIso8601NoTz(startDate) + " - " + DateUtils.toIso8601NoTz(endDate));
    }
  }

  @NoArgsConstructor
  @Data
  private class Criteria {
    private StartDate startDate;

    private EndDate endDate;
  }
}
