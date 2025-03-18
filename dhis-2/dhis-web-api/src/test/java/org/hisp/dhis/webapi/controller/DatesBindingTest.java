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
import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
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
  private Date actualStartDateTime;
  private Date actualEndDateTime;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new BindingController()).build();
  }

  @Test
  void shouldReturnADateAtTheEndOfTheDayWhenAnEndDateIsPassedWithoutTime() throws Exception {
    mockMvc.perform(get(ENDPOINT).param("end", "2001-06-17")).andExpect(status().isOk());

    assertNull(actualStartDateTime);
    assertEquals("2001-06-17T23:59:59.999", DateUtils.toLongDateWithMillis(actualEndDateTime));
  }

  @Test
  void shouldReturnADateWithTimeWhenAnEndDateIsPassedWithTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("end", "2001-06-17T16:45:34.324"))
        .andExpect(status().isOk());

    assertNull(actualStartDateTime);
    assertEquals("2001-06-17T16:45:34.324", DateUtils.toLongDateWithMillis(actualEndDateTime));
  }

  @Test
  void shouldReturnADateAtTheStartOfTheDayWhenAnStartDateIsPassedWithoutTime() throws Exception {
    mockMvc.perform(get(ENDPOINT).param("start", "2001-06-17")).andExpect(status().isOk());

    assertEquals("2001-06-17T00:00:00.000", DateUtils.toLongDateWithMillis(actualStartDateTime));
    assertNull(actualEndDateTime);
  }

  @Test
  void shouldReturnADateWithTimeWhenAnStartDateIsPassedWithTime() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("start", "2001-06-17T16:45:34.324"))
        .andExpect(status().isOk());

    assertEquals("2001-06-17T16:45:34.324", DateUtils.toLongDateWithMillis(actualStartDateTime));
    assertNull(actualEndDateTime);
  }

  @Controller
  private class BindingController {
    @GetMapping(value = ENDPOINT)
    public @ResponseBody WebMessage getDefault(Params params) {
      actualStartDateTime = applyIfNotNull(params.getStart(), StartDateTime::toDate);
      actualEndDateTime = applyIfNotNull(params.getEnd(), EndDateTime::toDate);
      return ok();
    }
  }

  @NoArgsConstructor
  @Data
  private class Params {
    private StartDateTime start;

    private EndDateTime end;
  }
}
