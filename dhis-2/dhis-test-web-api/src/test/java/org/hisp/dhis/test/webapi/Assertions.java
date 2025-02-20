/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.test.webapi;

import static org.hisp.dhis.test.utils.JavaToJson.singleToDoubleQuotes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.http.HttpClientAdapter.HttpResponse;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.http.HttpStatus.Series;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;

/**
 * Assertions contains web related assertions. General purpose assertions can be found and put into
 * {@link org.hisp.dhis.test.utils.Assertions}.
 */
public final class Assertions {
  private Assertions() {
    throw new UnsupportedOperationException("util");
  }

  public static JsonWebMessage assertWebMessage(HttpStatus expected, HttpResponse response) {
    JsonWebMessage actual = response.content(expected).as(JsonWebMessage.class);
    String expectedStatus = expected.series() == Series.SUCCESSFUL ? "OK" : "ERROR";
    assertWebMessageRequiredFields(
        expected.reasonPhrase(), expected.code(), expectedStatus, actual);
    return actual;
  }

  public static JsonWebMessage assertWebMessage(
      String httpStatus, int httpStatusCode, String status, String message, JsonMixed actual) {
    return assertWebMessage(
        httpStatus, httpStatusCode, status, message, actual.as(JsonWebMessage.class));
  }

  public static JsonWebMessage assertWebMessage(
      String httpStatus, int httpStatusCode, String status, String message, JsonWebMessage actual) {
    assertWebMessageRequiredFields(httpStatus, httpStatusCode, status, actual);
    assertEquals(message, actual.getMessage(), "unexpected message");
    return actual;
  }

  private static void assertWebMessageRequiredFields(
      String httpStatus, int httpStatusCode, String status, JsonWebMessage actual) {
    assertTrue(
        actual.has("httpStatusCode", "httpStatus", "status"),
        "response appears to be something other than a WebMessage: " + actual);
    assertEquals(httpStatusCode, actual.getHttpStatusCode(), "unexpected HTTP status code");
    assertEquals(httpStatus, actual.getHttpStatus(), "unexpected HTTP status");
    assertEquals(status, actual.getStatus(), "unexpected status");
  }

  public static void assertJson(String expected, HttpResponse actual) {
    assertEquals(singleToDoubleQuotes(expected), actual.content().toString());
  }
}
