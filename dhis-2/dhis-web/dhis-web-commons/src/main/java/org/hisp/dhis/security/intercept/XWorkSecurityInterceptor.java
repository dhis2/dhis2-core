package org.hisp.dhis.security.intercept;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.hisp.dhis.security.ActionAccessResolver;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.authority.RequiredAuthoritiesProvider;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: WebWorkSecurityInterceptor.java 5797 2008-10-02 15:40:29Z larshelg $
 */
public class XWorkSecurityInterceptor
    extends AbstractSecurityInterceptor
    implements Interceptor
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -3734961911452433836L;

    private static final String KEY_ACTION_ACCESS_RESOLVER = "auth";

    private static final String KEY_SECURITY_SERVICE = "security";

    private final ThreadLocal<SecurityMetadataSource> definitionSourceTag = new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private RequiredAuthoritiesProvider requiredAuthoritiesProvider;

    public void setRequiredAuthoritiesProvider( RequiredAuthoritiesProvider requiredAuthoritiesProvider )
    {
        this.requiredAuthoritiesProvider = requiredAuthoritiesProvider;
    }

    private ActionAccessResolver actionAccessResolver;

    public void setActionAccessResolver( ActionAccessResolver actionAccessResolver )
    {
        this.actionAccessResolver = actionAccessResolver;
    }

    private SecurityService securityService;

    public void setSecurityService( SecurityService securityService )
    {
        this.securityService = securityService;
    }

    // -------------------------------------------------------------------------
    // WebWork Interceptor
    // -------------------------------------------------------------------------

    @Override
    public void init()
    {
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        ActionConfig actionConfig = invocation.getProxy().getConfig();
        definitionSourceTag.set( requiredAuthoritiesProvider.createSecurityMetadataSource( actionConfig ) );

        InterceptorStatusToken token = beforeInvocation( actionConfig );

        addActionAccessResolver( invocation );

        Object result = null;
        try
        {
            result = invocation.invoke();
        }
        finally
        {
            result = afterInvocation( token, result );

            definitionSourceTag.remove();
        }

        if ( result != null )
        {
            return result.toString();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Spring Security Interceptor
    // -------------------------------------------------------------------------

    @Override
    public Class<?> getSecureObjectClass()
    {
        return ActionConfig.class;
    }

    @Override
    public SecurityMetadataSource obtainSecurityMetadataSource()
    {
        SecurityMetadataSource definitionSource = definitionSourceTag.get();

        if ( definitionSource != null )
        {
            return definitionSource;
        }

        // ---------------------------------------------------------------------
        // ObjectDefinitionSource required, but we are not inside an
        // invocation. Returning an empty dummy.
        // ---------------------------------------------------------------------

        return new SingleSecurityMetadataSource( new ActionConfig.Builder( "", "", "" ).build() );
    }

    // -------------------------------------------------------------------------
    // ActionAccessResolver
    // -------------------------------------------------------------------------

    private void addActionAccessResolver( ActionInvocation invocation )
    {
        Map<String, Object> accessResolverMap = new HashMap<>( 1 );

        accessResolverMap.put( KEY_ACTION_ACCESS_RESOLVER, actionAccessResolver );
        accessResolverMap.put( KEY_SECURITY_SERVICE, securityService );

        invocation.getStack().push( accessResolverMap );
    }
}
