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
package org.hisp.dhis.webapi.mvc;

import static org.hisp.dhis.common.input.InputUtils.decodeInput;
import static org.hisp.dhis.common.input.InputUtils.validateInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hisp.dhis.common.input.UrlParams;
import org.hisp.dhis.jsontree.JsonObject;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Adapter to map {@link Record}-classes endpoint method parameters that implement {@link
 * UrlParams}. This is so this is an opt-in features to not break existing usages of record types in
 * that position.
 *
 * @author Jan Bernitt
 * @since 2.44
 */
@Component
public class UrlParamsMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    Class<?> type = parameter.getParameterType();
    return type.isRecord() && UrlParams.class.isAssignableFrom(type);
  }

  @Nonnull
  @Override
  public Object resolveArgument(
      @Nonnull MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      @Nonnull NativeWebRequest request,
      @Nullable WebDataBinderFactory binderFactory)
      throws Exception {

    @SuppressWarnings("unchecked")
    Class<? extends Record> schema = (Class<? extends Record>) parameter.getParameterType();
    JsonObject params = decodeInput(schema, request::getParameterValues);
    validateInput(schema, params);
    return params.to(schema);
  }
}
