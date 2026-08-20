/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * Tests {@link SyncUtils#runSyncRequest(RestTemplate, RequestCallback, ResponseExtractor, String,
 * int)}.
 */
@ExtendWith(MockitoExtension.class)
class SyncUtilsTest {

  @Mock private RestTemplate restTemplate;

  private final RequestCallback requestCallback = request -> {};

  private final ResponseExtractor<String> responseExtractor =
      response -> new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

  @Test
  void shouldReturnResultWhenRequestSucceeds() {
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn("ok");

    String result =
        SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 3);

    assertEquals("ok", result);
    verify(restTemplate, times(1)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }

  @Test
  void shouldRetryServerErrorsUntilAttemptsExhaustedThenRethrow() {
    HttpServerErrorException serverError =
        HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            HttpHeaders.EMPTY,
            new byte[0],
            null);
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenThrow(serverError);

    assertThrows(
        HttpServerErrorException.class,
        () -> SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 3));

    verify(restTemplate, times(3)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }

  @Test
  void shouldSucceedAfterRetryingServerErrors() {
    HttpServerErrorException serverError =
        HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            HttpHeaders.EMPTY,
            new byte[0],
            null);
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenThrow(serverError)
        .thenReturn("ok");

    String result =
        SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 3);

    assertEquals("ok", result);
    verify(restTemplate, times(2)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }

  @Test
  void shouldParseClientErrorBodyWithoutRetrying() {
    HttpClientErrorException clientError =
        HttpClientErrorException.create(
            HttpStatus.CONFLICT,
            "Conflict",
            HttpHeaders.EMPTY,
            "already synced".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenThrow(clientError);

    String result =
        SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 3);

    assertEquals("already synced", result);
    verify(restTemplate, times(1)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }

  @Test
  void shouldRethrowResourceAccessExceptionWithoutRetrying() {
    ResourceAccessException connectionError = new ResourceAccessException("Connection refused");
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenThrow(connectionError);

    assertThrows(
        ResourceAccessException.class,
        () -> SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 3));

    verify(restTemplate, times(1)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }

  @Test
  void shouldMakeExactlyOneAttemptWhenMaxAttemptsIsZero() {
    when(restTemplate.<String>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn("ok");

    String result =
        SyncUtils.runSyncRequest(restTemplate, requestCallback, responseExtractor, "url", 0);

    assertEquals("ok", result);
    verify(restTemplate, times(1)).execute(anyString(), eq(HttpMethod.POST), any(), any());
  }
}
