package org.hisp.dhis.interceptor;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import ognl.NoSuchPropertyException;
import ognl.Ognl;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Nguyen Dang Quang
 * @version $Id: WebWorkI18nInterceptor.java 6335 2008-11-20 11:11:26Z larshelg $
 */
public class I18nInterceptor
    implements Interceptor
{
    private static final String KEY_I18N = "i18n";
    private static final String KEY_I18N_FORMAT = "format";
    private static final String KEY_LOCALE = "locale";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private I18nManager i18nManager;

    public void setI18nManager( I18nManager manager )
    {
        i18nManager = manager;
    }

    private LocaleManager localeManager;

    public void setLocaleManager( LocaleManager localeManager )
    {
        this.localeManager = localeManager;
    }

    // -------------------------------------------------------------------------
    // AroundInterceptor implementation
    // -------------------------------------------------------------------------

    @Override
    public void destroy()
    {
    }

    @Override
    public void init()
    {
    }

    @Override
    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        Action action = (Action) invocation.getAction();

        I18n i18n = i18nManager.getI18n( action.getClass() );
        I18nFormat i18nFormat = i18nManager.getI18nFormat();
        Locale locale = localeManager.getCurrentLocale();

        // ---------------------------------------------------------------------
        // Make the objects available for web templates
        // ---------------------------------------------------------------------

        Map<String, Object> i18nMap = new HashMap<>( 3 );
        i18nMap.put( KEY_I18N, i18n );
        i18nMap.put( KEY_I18N_FORMAT, i18nFormat );
        i18nMap.put( KEY_LOCALE, locale );

        invocation.getStack().push( i18nMap );

        // ---------------------------------------------------------------------
        // Set the objects in the action class if the properties exist
        // ---------------------------------------------------------------------

        Map<?, ?> contextMap = invocation.getInvocationContext().getContextMap();

        try
        {
            Ognl.setValue( KEY_I18N, contextMap, action, i18n );
        }
        catch ( NoSuchPropertyException ignored )
        {
        }

        try
        {
            Ognl.setValue( KEY_I18N_FORMAT, contextMap, action, i18nFormat );
        }
        catch ( NoSuchPropertyException ignored )
        {
        }

        try
        {
            Ognl.setValue( KEY_LOCALE, contextMap, action, locale );
        }
        catch ( NoSuchPropertyException ignored )
        {
        }

        return invocation.invoke();
    }
}
