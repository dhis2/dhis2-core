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
package org.hisp.dhis.web;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * HTTP status codes and their names.
 *
 * <p>Mostly a copy of spring's type of the same name to avoid depending on spring web.
 *
 * @author Jan Bernitt
 */
@Getter
@Accessors(fluent = true)
public enum HttpStatus {
  CONTINUE(100, "Continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols"),
  PROCESSING(102, "Processing"),
  CHECKPOINT(103, "Checkpoint"),
  OK(200, "OK"),
  CREATED(201, "Created"),
  ACCEPTED(202, "Accepted"),
  NO_CONTENT(204, "No Content"),
  RESET_CONTENT(205, "Reset Content"),
  PARTIAL_CONTENT(206, "Partial Content"),
  MULTI_STATUS(207, "Multi-Status"),
  ALREADY_REPORTED(208, "Already Reported"),
  MOVED_PERMANENTLY(301, "Moved Permanently"),
  FOUND(302, "Found"),
  SEE_OTHER(303, "See Other"),
  NOT_MODIFIED(304, "Not Modified"),
  TEMPORARY_REDIRECT(307, "Temporary Redirect"),
  PERMANENT_REDIRECT(308, "Permanent Redirect"),
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  PAYMENT_REQUIRED(402, "Payment Required"),
  FORBIDDEN(403, "Forbidden"),
  NOT_FOUND(404, "Not Found"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable"),
  PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
  REQUEST_TIMEOUT(408, "Request Timeout"),
  CONFLICT(409, "Conflict"),
  GONE(410, "Gone"),
  LENGTH_REQUIRED(411, "Length Required"),
  PRECONDITION_FAILED(412, "Precondition Failed"),
  PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
  REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
  LOCKED(423, "Locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency"),
  TOO_EARLY(425, "Too Early"),
  UPGRADE_REQUIRED(426, "Upgrade Required"),
  PRECONDITION_REQUIRED(428, "Precondition Required"),
  TOO_MANY_REQUESTS(429, "Too Many Requests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
  UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  NOT_IMPLEMENTED(501, "Not Implemented"),
  BAD_GATEWAY(502, "Bad Gateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout"),
  HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
  LOOP_DETECTED(508, "Loop Detected"),
  BANDWIDTH_LIMIT_EXCEEDED(509, "Bandwidth Limit Exceeded"),
  NOT_EXTENDED(510, "Not Extended"),
  NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

  private static final Map<Integer, HttpStatus> VALUES =
      stream(values()).collect(toUnmodifiableMap(s -> s.code, Function.identity()));

  public static HttpStatus of(int code) {
    return VALUES.get(code);
  }

  private final int code;

  private final Series series;

  private final String reasonPhrase;

  HttpStatus(int code, String reasonPhrase) {
    this.code = code;
    this.series = Series.of(code);
    this.reasonPhrase = reasonPhrase;
  }

  public enum Series {
    INFORMATIONAL(1),
    SUCCESSFUL(2),
    REDIRECTION(3),
    CLIENT_ERROR(4),
    SERVER_ERROR(5);

    private static final Series[] VALUES = values();

    public static Series of(int code) {
      return VALUES[(code / 100) - 1];
    }

    private final int nxx;

    Series(int nxx) {

      this.nxx = nxx;
    }

    public int value() {
      return nxx;
    }
  }
}
