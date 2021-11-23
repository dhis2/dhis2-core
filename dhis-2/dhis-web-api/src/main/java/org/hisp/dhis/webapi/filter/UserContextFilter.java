/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.common.UserContext.reset;
import static org.hisp.dhis.common.UserContext.setUser;
import static org.hisp.dhis.common.UserContext.setUserSetting;
import static org.hisp.dhis.user.UserSettingKey.DB_LOCALE;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This interceptor is ONLY responsible for setting the current user and its
 * related settings into the current request cycle/thread. The intention is to
 * leave it as simple as possible. Any business rules, if needed, should be
 * evaluated outside (in another interceptor or filter).
 *
 * @author maikel arabori
 * @author Morten Svanæs
 */
@Slf4j
@Component
@AllArgsConstructor
public class UserContextFilter extends OncePerRequestFilter
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    private static final String PARAM_TRANSLATE = "translate";

    private static final String PARAM_LOCALE = "locale";

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain )
        throws ServletException,
        IOException
    {
        boolean translate = !"false".equals( request.getParameter( PARAM_TRANSLATE ) );

        String locale = request.getParameter( PARAM_LOCALE );

        User user = currentUserService.getCurrentUserInTransaction();

        if ( user != null )
        {
            configureUserContext( user, new TranslateParams( translate, locale ) );
        }

        try
        {
            filterChain.doFilter( request, response );
        }
        finally
        {
            reset();
        }
    }

    private void configureUserContext( final User user, final TranslateParams translateParams )
    {
        final Locale dbLocale = getLocaleWithDefault( translateParams, user );

        setUser( user );
        setUserSetting( DB_LOCALE, dbLocale );
    }

    private Locale getLocaleWithDefault( final TranslateParams translateParams, final User user )
    {
        return translateParams.isTranslate()
            ? translateParams
                .getLocaleWithDefault( (Locale) userSettingService.getUserSetting( DB_LOCALE, user ) )
            : null;
    }

}
