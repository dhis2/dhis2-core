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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.webapi.WebClient;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Base class for all Spring Mock MVC based controller tests.
 *
 * @author Jan Bernitt
 */
public abstract class DhisMockMvcControllerTest extends DhisConvenienceTest implements WebClient {

  public static JsonWebMessage assertWebMessage(
      String httpStatus, int httpStatusCode, String status, String message, JsonResponse actual) {
    return assertWebMessage(
        httpStatus, httpStatusCode, status, message, actual.as(JsonWebMessage.class));
  }

  public static JsonWebMessage assertWebMessage(
      String httpStatus, int httpStatusCode, String status, String message, JsonWebMessage actual) {
    assertTrue(
        actual.has("httpStatusCode", "httpStatus", "status"),
        "response appears to be something other than a WebMessage: " + actual.toString());
    assertEquals(httpStatusCode, actual.getHttpStatusCode(), "unexpected HTTP status code");
    assertEquals(httpStatus, actual.getHttpStatus(), "unexpected HTTP status");
    assertEquals(status, actual.getStatus(), "unexpected status");
    assertEquals(message, actual.getMessage(), "unexpected message");
    return actual;
  }

  public static ResponseAdapter toResponse(MockHttpServletResponse response) {
    return new MockMvcResponseAdapter(response);
  }

  @Override
  public HttpResponse webRequest(
      HttpMethod method, String url, List<Header> headers, MediaType contentType, String content) {
    return webRequest(buildMockRequest(method, url, headers, contentType, content));
  }

  protected MockHttpServletRequestBuilder buildMockRequest(
      HttpMethod method, String url, List<Header> headers, MediaType contentType, String content) {
    MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, url);
    for (Header header : headers) {
      request.header(header.getName(), header.getValue());
    }
    if (contentType != null) {
      request.contentType(contentType);
    }
    if (content != null) {
      request.content(content);
    }

    return request;
  }

  protected abstract HttpResponse webRequest(MockHttpServletRequestBuilder request);

  private static class MockMvcResponseAdapter implements ResponseAdapter {

    private final MockHttpServletResponse response;

    MockMvcResponseAdapter(MockHttpServletResponse response) {
      this.response = response;
    }

    @Override
    public int getStatus() {
      return response.getStatus();
    }

    @Override
    public String getContent() {
      try {
        return response.getContentAsString(StandardCharsets.UTF_8);
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public String getErrorMessage() {
      return response.getErrorMessage();
    }

    @Override
    public String getHeader(String name) {
      return response.getHeader(name);
    }
  }
}
