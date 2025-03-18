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
package org.hisp.dhis.webapi.common;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class UIDBindingTest {

  private static final String VALID_UID_STRING = "bRNvL6NMQXb";
  private static final UID VALID_UID = UID.of("bRNvL6NMQXb");

  private static final String INVALID_UID = "invalidUid";

  private static final String ERROR_MESSAGE =
      "Value 'invalidUid' is not valid for parameter uid. UID must be an alphanumeric string of 11 characters";

  private static final String ERROR_MESSAGE_FOR_PATH_PARAM =
      "Value 'invalidUid' is not valid for path parameter uid. UID must be an alphanumeric string of 11 characters";

  private static final String BINDING_OBJECT_ERROR_MESSAGE =
      "Value 'invalidUid' is not valid for parameter uid. UID must be an alphanumeric string of 11 characters";

  private static final String ENDPOINT = "/uid";

  private UID actual;
  private List<UID> actualCollection;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UIDController()).build();
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsPathVariable() throws Exception {
    mockMvc.perform(get(ENDPOINT + "/" + VALID_UID)).andExpect(status().isOk());

    assertEquals(VALID_UID, actual);
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsRequestParam() throws Exception {
    mockMvc.perform(get(ENDPOINT).param("uid", VALID_UID_STRING)).andExpect(status().isOk());

    assertEquals(VALID_UID, actual);
  }

  @Test
  void shouldReturnUIDValueWhenPassingValidUidAsRequestParamObject() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/params").param("uid", VALID_UID_STRING))
        .andExpect(status().isOk());

    assertEquals(VALID_UID, actual);
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsPathVariable() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get(ENDPOINT + "/" + INVALID_UID).accept("application/json"))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith(ERROR_MESSAGE_FOR_PATH_PARAM, message.getMessage());
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsRequestParam() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get(ENDPOINT).accept("application/json").param("uid", INVALID_UID))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith(ERROR_MESSAGE, message.getMessage());
  }

  @Test
  void shouldReturnBadRequestResponseWhenPassingInvalidUidAsRequestParamObject() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get(ENDPOINT + "/params").accept("application/json").param("uid", INVALID_UID))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith(BINDING_OBJECT_ERROR_MESSAGE, message.getMessage());
  }

  @Test
  void shouldHandleMultipleOrderComponentsGivenViaOneParameter() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/collection").param("uids", "PHB3x6n374I,Sq3QLSHij67"))
        .andExpect(status().isOk());

    assertEquals(List.of(UID.of("PHB3x6n374I"), UID.of("Sq3QLSHij67")), actualCollection);
  }

  @Test
  void shouldHandleMultipleOrderComponentsGivenViaMultipleParameters() throws Exception {
    mockMvc
        .perform(
            get(ENDPOINT + "/collection").param("uids", "PHB3x6n374I").param("uids", "Sq3QLSHij67"))
        .andExpect(status().isOk());

    assertEquals(List.of(UID.of("PHB3x6n374I"), UID.of("Sq3QLSHij67")), actualCollection);
  }

  @Test
  void shouldReturnABadRequestWhenMixingRepeatedParameterAndCommaSeparatedValues()
      throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get(ENDPOINT + "/collection")
                    .accept("application/json")
                    .param("uids", "PHB3x6n374I")
                    .param("uids", "Sq3QLSHij67,jEVhr5j6EaA"))
            .andReturn()
            .getResponse();

    JsonWebMessage message = JsonMixed.of(response.getContentAsString()).as(JsonWebMessage.class);
    assertEquals(400, message.getHttpStatusCode());
    assertStartsWith("You likely repeated request parameter 'uids' and used", message.getMessage());
  }

  @Controller
  private class UIDController extends CrudControllerAdvice {
    @GetMapping(value = ENDPOINT + "/{uid}")
    public @ResponseBody WebMessage getUIDValueFromPath(@PathVariable UID uid) {
      actual = uid;
      return ok();
    }

    @GetMapping(value = ENDPOINT)
    public @ResponseBody WebMessage getUIDValueFromRequestParam(@RequestParam UID uid) {
      actual = uid;
      return ok();
    }

    @GetMapping(value = ENDPOINT + "/collection")
    public @ResponseBody WebMessage getUIDValueFromRequestParamCollection(
        @RequestParam List<UID> uids) {
      actualCollection = uids;
      return ok();
    }

    @GetMapping(value = ENDPOINT + "/params")
    public @ResponseBody WebMessage getUIDValueFromRequestObject(UIDRequestParams requestParams) {
      actual = requestParams.uid;
      return ok();
    }
  }

  private record UIDRequestParams(UID uid) {}
}
