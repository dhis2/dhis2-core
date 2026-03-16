/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.log;

/**
 * MDC key constants used across modules. These are the key names for SLF4J's {@link org.slf4j.MDC}
 * and log4j2's {@code %X{key}} pattern layout.
 *
 * @see org.slf4j.MDC
 */
public final class MdcKeys {
  private MdcKeys() {}

  /** Controller class name handling the request. Set by HandlerMethodInterceptor. */
  public static final String MDC_CONTROLLER = "controller";

  /** Handler method name. Set by HandlerMethodInterceptor. */
  public static final String MDC_METHOD = "method";

  /** Client-supplied X-Request-ID header value (sanitized). Set by RequestIdFilter. */
  public static final String MDC_REQUEST_ID = "requestId";

  /** Hashed session ID or job UID. Set by SessionIdFilter and the job scheduler. */
  public static final String MDC_SESSION_ID = "sessionId";

  /**
   * Deprecated alias of {@link #MDC_REQUEST_ID}. Kept for backward compatibility with custom log4j2
   * configs.
   */
  public static final String MDC_X_REQUEST_ID = "xRequestID";
}
