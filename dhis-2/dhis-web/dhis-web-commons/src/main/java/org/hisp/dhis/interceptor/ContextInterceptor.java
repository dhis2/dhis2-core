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

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.commons.util.TextUtils;

import java.util.HashMap;
import java.util.Map;

import static org.hisp.dhis.util.ContextUtils.getCookieValue;

/**
 * @author Lars Helge Overland
 */
public class ContextInterceptor
    implements Interceptor
{
    private static final String KEY_IN_MEMORY_DATABASE = "inMemoryDatabase";
    private static final String KEY_TEXT_UTILS = "dhisTextUtils";
    private static final String KEY_CURRENT_PAGE = "keyCurrentPage";
    private static final String KEY_CURRENT_KEY = "keyCurrentKey";

    private DatabaseInfoProvider databaseInfoProvider;

    public void setDatabaseInfoProvider( DatabaseInfoProvider databaseInfoProvider )
    {
        this.databaseInfoProvider = databaseInfoProvider;
    }

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
        Map<String, Object> map = new HashMap<>();

        map.put( KEY_IN_MEMORY_DATABASE, databaseInfoProvider.isInMemory() );
        map.put( KEY_TEXT_UTILS, TextUtils.INSTANCE );
        map.put( KEY_CURRENT_PAGE, getCookieValue( ServletActionContext.getRequest(), "currentPage" ) );
        map.put( KEY_CURRENT_KEY, getCookieValue( ServletActionContext.getRequest(), "currentKey" ) );

        invocation.getStack().push( map );

        return invocation.invoke();
    }
}
