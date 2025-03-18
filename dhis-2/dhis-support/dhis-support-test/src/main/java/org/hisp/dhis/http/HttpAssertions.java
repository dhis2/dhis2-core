/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.json.domain.JsonError;

/**
 * Assertions for {@link HttpClientAdapter} API based tests.
 *
 * @author Jan Bernitt
 * @since 2.42 (extracted from existing utils)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpAssertions {
  /**
   * Asserts that the {@link HttpClientAdapter.HttpResponse} has the expected {@link HttpStatus}.
   *
   * <p>If status is {@link HttpStatus#CREATED} the method returns the UID of the created object in
   * case it is provided by the response. This is based on a convention used in DHIS2.
   *
   * @param expected status we should get
   * @param actual the response we actually got
   * @return UID of the created object (if available) or {@code null}
   */
  public static String assertStatus(HttpStatus expected, HttpClientAdapter.HttpResponse actual) {
    HttpStatus actualStatus = actual.status();
    if (expected != actualStatus) {
      // OBS! we use the actual state to not fail the check in error
      JsonError error = actual.error(actualStatus.series());
      String msg = error.getMessage();
      if (msg != null && expected.series() == actualStatus.series()) {
        assertEquals(expected, actualStatus, msg);
      } else {
        assertEquals(expected, actualStatus, error.summary());
      }
    }
    assertValidLocation(actual);
    return getCreatedId(actual);
  }

  /**
   * Asserts that the {@link HttpClientAdapter.HttpResponse} has the expected {@link
   * HttpStatus.Series}. This is useful on cases where it only matters that operation was {@link
   * HttpStatus.Series#SUCCESSFUL} or say {@link HttpStatus.Series#CLIENT_ERROR} but not which exact
   * code of the series.
   *
   * <p>If status is {@link HttpStatus#CREATED} the method returns the UID of the created object in
   * case it is provided by the response. This is based on a convention used in DHIS2.
   *
   * @param expected status {@link HttpStatus.Series} we should get
   * @param actual the response we actually got
   * @return UID of the created object (if available) or {@code null}
   */
  @CheckForNull
  public static String assertSeries(
      @Nonnull HttpStatus.Series expected, @Nonnull HttpClientAdapter.HttpResponse actual) {
    HttpStatus.Series actualSeries = actual.series();
    if (expected != actualSeries) {
      // OBS! we use the actual state to not fail the check in error
      String msg = actual.error(actualSeries).summary();
      assertEquals(expected, actualSeries, msg);
    }
    assertValidLocation(actual);
    return getCreatedId(actual);
  }

  public static void assertValidLocation(@Nonnull HttpClientAdapter.HttpResponse actual) {
    String location = actual.location();
    if (location == null) {
      return;
    }
    assertTrue(
        location.startsWith("http://") || location.startsWith("https://"),
        "Location header does not start with http or https");
    assertTrue(
        location.indexOf("http://", 4) < 0 && location.indexOf("https://", 4) < 0,
        "Location header does contain multiple protocol parts");
  }

  @CheckForNull
  private static String getCreatedId(HttpClientAdapter.HttpResponse response) {
    HttpStatus actual = response.status();
    if (actual == HttpStatus.CREATED) {
      JsonObject report = response.contentUnchecked().getObject("response");
      if (report.exists()) {
        return report.getString("uid").string();
      }
    }
    String location = response.location();
    return location == null ? null : location.substring(location.lastIndexOf('/') + 1);
  }

  public static <T> T exceptionAsFail(Callable<T> op) {
    try {
      return op.call();
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }
}
