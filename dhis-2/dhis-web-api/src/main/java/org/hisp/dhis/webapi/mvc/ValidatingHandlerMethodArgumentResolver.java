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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.Validation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class ValidatingHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  private final ObjectMapper jsonMapper;

  /** A cache to remember which types have @{@link Validation} annotations */
  private static final Map<Class<?>, Boolean> VALIDATED = new ConcurrentHashMap<>();

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    Class<?> type = parameter.getParameterType();
    return VALIDATED.computeIfAbsent(
        type, ValidatingHandlerMethodArgumentResolver::hasValidationAnnotation);
  }

  private static boolean hasValidationAnnotation(Class<?> type) {
    for (Field f : type.getDeclaredFields())
      if (f.getAnnotatedType().isAnnotationPresent(Validation.class)) return true;
    Class<?> superclass = type.getSuperclass();
    return superclass != null && hasValidationAnnotation(superclass);
  }

  @Nonnull
  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      @Nonnull ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {

    // 1. Instantiate the target object (must have a default constructor)
    Object target = BeanUtils.instantiateClass(parameter.getParameterType());

    // 2. Get the underlying HttpServletRequest
    HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);

    // 3. Create a binder and bind request parameters
    WebDataBinder binder =
        binderFactory.createBinder(webRequest, target, parameter.getParameterName());
    // Wrap request parameters as PropertyValues
    PropertyValues propertyValues = new ServletRequestParameterPropertyValues(servletRequest);
    binder.bind(propertyValues);

    String json = jsonMapper.writeValueAsString(target);

    return target;
  }
}
