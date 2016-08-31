package org.hisp.dhis.security;

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
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class StrutsAuthorityUtils
{
    public static Collection<String> getAuthorities( ActionConfig actionConfig, String key )
    {
        final Map<String, String> staticParams = actionConfig.getParams();

        if ( staticParams == null || !staticParams.containsKey( key ) )
        {
            return Collections.emptySet();
        }

        final String param = staticParams.get( key );

        HashSet<String> keys = new HashSet<>();

        StringTokenizer t = new StringTokenizer( param, "\t\n\r ," );

        while ( t.hasMoreTokens() )
        {
            keys.add( t.nextToken() );
        }

        return keys;
    }

    public static Collection<ConfigAttribute> getConfigAttributes( ActionConfig actionConfig, String key )
    {
        return getConfigAttributes( getAuthorities( actionConfig, key ) );
    }

    public static Collection<ConfigAttribute> getConfigAttributes( Collection<String> authorities )
    {
        Collection<ConfigAttribute> configAttributes = new HashSet<>();

        for ( String authority : authorities )
        {
            configAttributes.add( new SecurityConfig( authority ) );
        }

        return configAttributes;
    }
}
