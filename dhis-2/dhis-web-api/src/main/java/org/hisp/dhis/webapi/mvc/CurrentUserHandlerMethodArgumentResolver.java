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
package org.hisp.dhis.webapi.mvc;

import lombok.AllArgsConstructor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserDetailsImpl;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Makes {@link CurrentUser} annotation have its effect.
 *
 * @author Morten Olav Hansen (initial version)
 * @author Jan Bernitt (annotation based version)
 */
@Component
@AllArgsConstructor
public class CurrentUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  private final UserService userService;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {

    Class<?> type = parameter.getParameterType();

    boolean isAssignable =
        type == String.class
            || type == CurrentUserDetailsImpl.class
            || User.class.isAssignableFrom(type);

    CurrentUser parameterAnnotation = parameter.getParameterAnnotation(CurrentUser.class);
    return parameterAnnotation != null && isAssignable;
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    Class<?> type = parameter.getParameterType();
    if (type == String.class) {
      return CurrentUserUtil.getCurrentUsername();
    }

    if (type == CurrentUserDetailsImpl.class) {
      return CurrentUserUtil.getCurrentUserDetails();
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
    if (currentUser == null && annotation != null && annotation.required()) {
      throw new NotAuthenticatedException();
    }
    if (User.class.isAssignableFrom(type)) {
      return currentUser;
    }
    throw new UnsupportedOperationException("Not yet supported @CurrentUser type: " + type);
  }
}
