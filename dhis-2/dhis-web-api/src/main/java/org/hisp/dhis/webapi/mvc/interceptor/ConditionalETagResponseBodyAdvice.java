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
package org.hisp.dhis.webapi.mvc.interceptor;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.webapi.service.ConditionalETagService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Attaches stored conditional ETag headers before Spring writes the response body.
 *
 * <p>This advice is intentionally broad and only acts when {@link ConditionalETagInterceptor}
 * marked the request as cacheable.
 *
 * @author Morten Svanæs
 */
@ControllerAdvice(basePackages = "org.hisp.dhis.webapi")
@RequiredArgsConstructor
public class ConditionalETagResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  private final ConditionalETagService conditionalETagService;

  @Override
  public boolean supports(
      @Nonnull MethodParameter returnType,
      @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType) {
    return conditionalETagService.isEnabled();
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      @Nonnull MethodParameter returnType,
      @Nonnull MediaType selectedContentType,
      @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @Nonnull ServerHttpRequest request,
      @Nonnull ServerHttpResponse response) {
    if (!(request instanceof ServletServerHttpRequest servletRequest)
        || !(response instanceof ServletServerHttpResponse servletResponse)) {
      return body;
    }

    String currentETag =
        ConditionalETagInterceptor.getStoredETag(servletRequest.getServletRequest());
    if (currentETag == null) {
      return body;
    }

    HttpServletResponse httpResponse = servletResponse.getServletResponse();
    if (httpResponse.isCommitted()
        || !isSuccessStatus(httpResponse.getStatus())
        || response.getHeaders().containsKey(HttpHeaders.ETAG)
        || httpResponse.containsHeader(HttpHeaders.ETAG)) {
      return body;
    }

    conditionalETagService.setETagHeaders(httpResponse, currentETag);
    return body;
  }

  private static boolean isSuccessStatus(int status) {
    return status >= 200 && status < 300;
  }
}
