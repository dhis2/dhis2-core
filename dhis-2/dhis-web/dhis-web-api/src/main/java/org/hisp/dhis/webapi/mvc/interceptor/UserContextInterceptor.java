package org.hisp.dhis.webapi.mvc.interceptor;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.common.UserContext.reset;
import static org.hisp.dhis.common.UserContext.setUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import lombok.RequiredArgsConstructor;

/**
 * This interceptor is ONLY responsible for setting the current user into the
 * current request cycle/thread. The intention is to leave it as simple as
 * possible. Any user settings or rules, if need to be set should be done
 * outside (in another interceptor or filter).
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class UserContextInterceptor extends HandlerInterceptorAdapter implements InitializingBean
{
    private static UserContextInterceptor instance;

    private final CurrentUserService currentUserService;

    @Override
    public void afterPropertiesSet()
    {
        instance = this;
    }

    public static UserContextInterceptor get()
    {
        return instance;
    }

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response,
        final Object handler )
    {
        setUser( currentUserService.getCurrentUser() );

        return true;
    }

    @Override
    public void afterCompletion( final HttpServletRequest request, final HttpServletResponse response,
        final Object handler, final Exception ex )
    {
        reset();
    }
}
