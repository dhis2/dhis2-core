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
package org.hisp.dhis.http;

import static org.apache.commons.lang3.ArrayUtils.insert;
import static org.hisp.dhis.http.HttpAssertions.assertSeries;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpAssertions.exceptionAsFail;
import static org.hisp.dhis.http.HttpClientUtils.fileContent;
import static org.hisp.dhis.http.HttpClientUtils.requestComponentsIn;
import static org.hisp.dhis.http.HttpClientUtils.substitutePlaceholders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.intellij.lang.annotations.Language;

/**
 * The purpose of this interface is to allow mixin style addition of the convenience web API by
 * implementing this interface's essential method {@link #perform(HttpMethod, String, List, String,
 * Body)}.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
@SuppressWarnings("java:S100")
public interface HttpClientAdapter {

  /**
   * Execute the request with the provided parameters.
   *
   * <p>This is the adapter method to the underlying implementation to perform the actual request.
   *
   * @param method used method
   * @param url called URL
   * @param headers provided HTTP request headers
   * @param contentType of the request body content
   * @param content the request body
   * @return the HTTP response
   */
  @Nonnull
  HttpResponse perform(
      @Nonnull HttpMethod method,
      @Nonnull String url,
      @Nonnull List<Header> headers,
      @CheckForNull String contentType,
      @CheckForNull Body content);

  sealed interface RequestComponent permits Header, Body {}

  static Header Header(String name, Object value) {
    return new Header(name, value);
  }

  static Header ApiTokenHeader(String token) {
    return Header("Authorization", "ApiToken " + token);
  }

  static Header JwtTokenHeader(String token) {
    return Header("Authorization", "Bearer " + token);
  }

  static Header CookieHeader(String cookie) {
    return Header("Cookie", cookie);
  }

  static Header ContentType(Object mimeType) {
    return ContentType(mimeType.toString());
  }

  static Header ContentType(String mimeType) {
    return Header("Content-Type", mimeType);
  }

  static Header ContentType(Path file) {
    String name = file.toString();
    if (name.endsWith(".json")) return ContentType("application/json; charset=utf8");
    if (name.endsWith(".geojson")) return ContentType("application/geo+json; charset=utf8");
    if (name.endsWith(".xml")) return ContentType("application/xml; charset=utf8");
    return null;
  }

  static Header Accept(Object mimeType) {
    return Accept(mimeType.toString());
  }

  static Header Accept(String mimeType) {
    return Header("Accept", mimeType);
  }

  static Body Body(String text) {
    return new Body(text);
  }

  static Body Body(byte[] binary) {
    return new Body(null, binary);
  }

  static byte[] gzip(String text) {
    return gzip(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  static byte[] gzip(byte[] binary) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(bos)) {
      gz.write(binary);
      gz.finish();
      return bos.toByteArray();
    } catch (java.io.IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  record Header(String name, Object value) implements RequestComponent {}

  record Body(String text, byte[] binary) implements RequestComponent {
    public Body(String content) {
      this(content, null);
    }

    public Body {
      assertTrue(text != null || binary != null);
    }
  }

  @Nonnull
  default HttpResponse GET(String url, Object... args) {
    return perform(HttpMethod.GET, substitutePlaceholders(url, args), requestComponentsIn(args));
  }

  @Nonnull
  default HttpResponse POST(String url, Object... args) {
    return perform(HttpMethod.POST, substitutePlaceholders(url, args), requestComponentsIn(args));
  }

  @Nonnull
  default HttpResponse POST(String url, @Language("json5") String body) {
    return perform(HttpMethod.POST, url, new Body(body));
  }

  @Nonnull
  default HttpResponse POST(String url, Path body) {
    return exceptionAsFail(() -> POST(url, Body(fileContent(body.toString())), ContentType(body)));
  }

  @Nonnull
  default HttpResponse PATCH(String url, Object... args) {
    // Default mime-type is added as first element so that content type in
    // arguments does not override it
    return perform(
        HttpMethod.PATCH,
        substitutePlaceholders(url, args),
        insert(0, requestComponentsIn(args), ContentType("application/json-patch+json")));
  }

  @Nonnull
  default HttpResponse PATCH(String url, Path body) {
    return exceptionAsFail(
        () ->
            PATCH(
                url,
                Body(fileContent(body.toString())),
                ContentType("application/json-patch+json")));
  }

  @Nonnull
  default HttpResponse PATCH(String url, @Language("json5") String body) {
    return perform(HttpMethod.PATCH, url, ContentType("application/json-patch+json"), Body(body));
  }

  @Nonnull
  default HttpResponse PUT(String url, Object... args) {
    return perform(HttpMethod.PUT, substitutePlaceholders(url, args), requestComponentsIn(args));
  }

  @Nonnull
  default HttpResponse PUT(String url, Path body) {
    return exceptionAsFail(() -> PUT(url, Body(fileContent(body.toString())), ContentType(body)));
  }

  @Nonnull
  default HttpResponse PUT(String url, @Language("json5") String body) {
    return perform(HttpMethod.PUT, url, new Body(body));
  }

  @Nonnull
  default HttpResponse DELETE(String url, Object... args) {
    return perform(HttpMethod.DELETE, substitutePlaceholders(url, args), requestComponentsIn(args));
  }

  @Nonnull
  default HttpResponse DELETE(String url, @Language("json5") String body) {
    return perform(HttpMethod.DELETE, url, new Body(body));
  }

  @Nonnull
  default HttpResponse perform(HttpMethod method, String url, RequestComponent... components) {
    // configure headers
    String contentMediaType = null;
    String contentEncoding = null;
    List<Header> headers = new ArrayList<>();
    for (RequestComponent c : components) {
      if (c instanceof Header header) {
        if (header.name().equalsIgnoreCase("Content-Type")) {
          // last provided content type wins
          contentMediaType = header.value().toString();
        } else {
          if (header.name().equalsIgnoreCase("Content-Encoding"))
            contentEncoding = header.value().toString();
          headers.add(header);
        }
      }
    }
    // configure body
    Body bodyComponent = HttpClientUtils.getComponent(Body.class, components);
    if (bodyComponent != null) {
      String text = bodyComponent.text();
      if (text != null) {
        if (text.isEmpty()) return perform(method, url, headers, null, null);
        if (contentMediaType == null) contentMediaType = "application/json";
        // replace single quote with double quote (allowed for convenience)
        if (contentMediaType.startsWith("application/json")) text = text.replace('\'', '"');
        bodyComponent = new Body(text);
      } else {
        byte[] binary = bodyComponent.binary;
        if (binary == null || binary.length == 0) perform(method, url, headers, null, null);
        if (contentMediaType == null) contentMediaType = "application/json";
        if (contentEncoding == null) headers.add(Header("Content-Encoding", "gzip"));
      }
    }
    return perform(method, url, headers, contentMediaType, bodyComponent);
  }

  /** Implemented to adapt the {@link HttpClientAdapter} API to an actual implementation response */
  interface HttpResponseAdapter {

    /**
     * @return HTTP status code
     */
    int getStatus();

    /**
     * @return HTTP response body content
     */
    String getContent();

    /**
     * @return HTTP response error message
     */
    String getErrorMessage();

    /**
     * @param name of the header to read
     * @return HTTP response header value for the provided name
     */
    @CheckForNull
    String getHeader(String name);
  }

  final class HttpResponse {

    private final HttpResponseAdapter response;

    public HttpResponse(HttpResponseAdapter response) {
      this.response = response;
    }

    public HttpStatus status() {
      return HttpStatus.of(response.getStatus());
    }

    public HttpStatus.Series series() {
      return status().series();
    }

    public boolean success() {
      return series() == HttpStatus.Series.SUCCESSFUL;
    }

    /**
     * Access raw response body for non JSON responses
     *
     * @param contentType the expected content type
     * @return raw content body in UTF-8 encoding
     */
    public String content(String contentType) {
      if (contentType.equals("application/json")) {
        fail("Use one of the other content() methods for JSON");
      }
      String actualContentType = header("Content-Type");
      assertNotNull(actualContentType, "response content-type was not set");
      if (!actualContentType.startsWith(contentType)) assertEquals(contentType, actualContentType);
      return exceptionAsFail(response::getContent);
    }

    public JsonMixed content() {
      return content(HttpStatus.Series.SUCCESSFUL);
    }

    public JsonMixed content(HttpStatus.Series expected) {
      assertSeries(expected, this);
      return contentUnchecked();
    }

    public JsonMixed content(HttpStatus expected) {
      assertStatus(expected, this);
      return contentUnchecked();
    }

    public JsonError error() {
      assertTrue(series().value() >= 4, "not a client or server error");
      return errorInternal();
    }

    public JsonError error(HttpStatus expected) {
      assertStatus(expected, this);
      return errorInternal();
    }

    public JsonError error(HttpStatus.Series expected) {
      // OBS! cannot use assertSeries as it uses this method
      assertEquals(expected, series());
      return errorInternal();
    }

    private JsonError errorInternal() {
      if (!hasBody()) {
        String errorMessage = response.getErrorMessage();
        if (errorMessage != null) {
          errorMessage = '"' + errorMessage + '"';
        }
        String error =
            String.format(
                "{\"status\": \"error\",\"httpStatus\":\"%s\",\"httpStatusCode\":%d, \"message\":%s}",
                status().name(), response.getStatus(), errorMessage);
        return JsonValue.of(error).as(JsonError.class);
      }
      return contentUnchecked().as(JsonError.class);
    }

    public boolean hasBody() {
      return !response.getContent().isEmpty();
    }

    public JsonMixed contentUnchecked() {
      return exceptionAsFail(() -> JsonMixed.of(response.getContent()));
    }

    public String location() {
      return header("Location");
    }

    @CheckForNull
    public String header(String name) {
      return response.getHeader(name);
    }
  }
}
