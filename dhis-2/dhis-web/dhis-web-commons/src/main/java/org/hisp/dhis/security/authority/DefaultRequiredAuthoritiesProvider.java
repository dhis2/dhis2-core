package org.hisp.dhis.security.authority;

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

import com.opensymphony.xwork2.config.entities.ActionConfig;
import org.hisp.dhis.security.StrutsAuthorityUtils;
import org.hisp.dhis.security.intercept.SingleSecurityMetadataSource;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: DefaultRequiredAuthoritiesProvider.java 3160 2007-03-24 20:15:06Z torgeilo $
 */
public class DefaultRequiredAuthoritiesProvider
    implements RequiredAuthoritiesProvider
{
    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private String requiredAuthoritiesKey;

    @Required
    public void setRequiredAuthoritiesKey( String requiredAuthoritiesKey )
    {
        this.requiredAuthoritiesKey = requiredAuthoritiesKey;
    }

    private String anyAuthoritiesKey;

    @Required
    public void setAnyAuthoritiesKey( String anyAuthoritiesKey )
    {
        this.anyAuthoritiesKey = anyAuthoritiesKey;
    }

    private Set<String> globalAttributes = Collections.emptySet();

    public void setGlobalAttributes( Set<String> globalAttributes )
    {
        this.globalAttributes = globalAttributes;
    }

    // -------------------------------------------------------------------------
    // RequiredAuthoritiesProvider implementation
    // -------------------------------------------------------------------------

    @Override
    public SecurityMetadataSource createSecurityMetadataSource( ActionConfig actionConfig )
    {
        return createSecurityMetadataSource( actionConfig, actionConfig );
    }

    @Override
    public SecurityMetadataSource createSecurityMetadataSource( ActionConfig actionConfig, Object object )
    {
        Collection<ConfigAttribute> attributes = new ArrayList<>();
        attributes.addAll( StrutsAuthorityUtils.getConfigAttributes( getRequiredAuthorities( actionConfig ) ) );
        attributes.addAll( StrutsAuthorityUtils.getConfigAttributes( globalAttributes ) );

        return new SingleSecurityMetadataSource( object, attributes );
    }

    @Override
    public Collection<String> getAllAuthorities( ActionConfig actionConfig )
    {
        Collection<String> authorities = new HashSet<>();
        authorities.addAll( getRequiredAuthorities( actionConfig ) );
        authorities.addAll( getAnyAuthorities( actionConfig ) );

        return authorities;
    }

    @Override
    public Collection<String> getRequiredAuthorities( ActionConfig actionConfig )
    {
        return StrutsAuthorityUtils.getAuthorities( actionConfig, requiredAuthoritiesKey );
    }

    public Collection<String> getAnyAuthorities( ActionConfig actionConfig )
    {
        return StrutsAuthorityUtils.getAuthorities( actionConfig, anyAuthoritiesKey );
    }
}
