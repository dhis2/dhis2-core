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
package org.hisp.dhis.webapi.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class UIDBindingTest {

  private static final String VALID_UID = "bRNvL6NMQXb";

  private static final String INVALID_UID = "invalidUid";

  private static final String ERROR_MESSAGE =
      "Value 'invalidUid' is not valid for parameter uid. UID must be an alphanumeric string of 11 characters";

  private static final String ERROR_MESSAGE_FOR_PATH_PARAM =
      "Value 'invalidUid' is not valid for path parameter uid. UID must be an alphanumeric string of 11 characters";

  private static final String BINDING_OBJECT_ERROR_MESSAGE =
      "Value 'invalidUid' is not valid for parameter uidInRequestParams. UID must be an alphanumeric string of 11 characters";

  private static final String ENDPOINT = "/uid";

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new UIDController())
            .setControllerAdvice(new CrudControllerAdvice())
            .build();
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsPathVariable() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/" + VALID_UID))
        .andExpect(status().isOk())
        .andExpect(content().string(VALID_UID));
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsRequestParam() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("uid", VALID_UID))
        .andExpect(status().isOk())
        .andExpect(content().string(VALID_UID));
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsRequestParamObject() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/params").param("uidInRequestParams", VALID_UID))
        .andExpect(status().isOk())
        .andExpect(content().string(VALID_UID));
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsPathVariable() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/" + INVALID_UID))
        .andExpect(content().string(containsString(ERROR_MESSAGE_FOR_PATH_PARAM)));
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsRequestParam() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("uid", INVALID_UID))
        .andExpect(content().string(containsString(ERROR_MESSAGE)));
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsRequestParamObject() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/params").param("uidInRequestParams", INVALID_UID))
        .andExpect(content().string(containsString(BINDING_OBJECT_ERROR_MESSAGE)));
  }

  @Controller
  private static class UIDController extends CrudControllerAdvice {
    @GetMapping(value = ENDPOINT + "/{uid}")
    public @ResponseBody String getUIDValueFromPath(@PathVariable UID uid) {
      return uid.getValue();
    }

    @GetMapping(value = ENDPOINT)
    public @ResponseBody String getUIDValueFromRequestParam(@RequestParam UID uid) {
      return uid.getValue();
    }

    @GetMapping(value = ENDPOINT + "/params")
    public @ResponseBody String getUIDValueFromRequestObject(UIDRequestParams uidRequestParams) {
      return uidRequestParams.uidInRequestParams().getValue();
    }
  }

  private record UIDRequestParams(UID uidInRequestParams) {}
}
