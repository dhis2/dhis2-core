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
package org.hisp.dhis.webapi.controller;

import javax.annotation.Nonnull;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.system.util.HttpUtils;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * When returning {@link WebMessage} the message's {@link WebMessage#getHttpStatusCode()} is used to
 * set the HTTP response status code.
 *
 * <p>In case the response is a 4xx or 5xx caching is turned off.
 *
 * @author Jan Bernitt
 */
@ControllerAdvice
public class WebMessageControllerAdvice implements ResponseBodyAdvice<WebMessage> {
  @Autowired private ContextService contextService;

  @Override
  public boolean supports(
      MethodParameter returnType,
      @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType) {
    return WebMessage.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public WebMessage beforeBodyWrite(
      WebMessage body,
      @Nonnull MethodParameter returnType,
      @Nonnull MediaType selectedContentType,
      @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @Nonnull ServerHttpRequest request,
      @Nonnull ServerHttpResponse response) {
    if (body == null) return null;
    String location = body.getLocation();
    if (location != null) {
      response
          .getHeaders()
          .addIfAbsent(ContextUtils.HEADER_LOCATION, contextService.getApiPath() + location);
    }
    HttpStatus httpStatus = HttpUtils.resolve(body.getHttpStatusCode());
    if (httpStatus != null) {
      response.setStatusCode(httpStatus);
      if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
        response
            .getHeaders()
            .addIfAbsent("Cache-Control", CacheControl.noCache().cachePrivate().getHeaderValue());
      }
    }
    return body;
  }
}
