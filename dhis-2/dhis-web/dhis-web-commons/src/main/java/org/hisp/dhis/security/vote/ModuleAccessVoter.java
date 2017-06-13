package org.hisp.dhis.security.vote;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.opensymphony.xwork2.config.entities.ActionConfig;

/**
 * AccessDecisionVoter which grants access if one of the granted authorities
 * matches attribute prefix + module name. The module name is taken from an
 * <code>com.opensymphony.xwork.config.entities.ActionConfig</code> object,
 * which is the only type of object this voter supports.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: ModuleAccessVoter.java 6352 2008-11-20 15:49:52Z larshelg $
 */
public class ModuleAccessVoter
    extends AbstractPrefixedAccessDecisionVoter
{
    private static final Log LOG = LogFactory.getLog( ModuleAccessVoter.class );

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private Set<String> alwaysAccessible = Collections.emptySet();

    /**
     * Sets a set of names for modules which are always accessible.
     */
    public void setAlwaysAccessible( Set<String> alwaysAccessible )
    {
        this.alwaysAccessible = alwaysAccessible;
    }

    // -------------------------------------------------------------------------
    // AccessDecisionVoter implementation
    // -------------------------------------------------------------------------

    /**
     * Returns true if the class equals
     * <code>com.opensymphony.xwork.config.entities.ActionConfig</code>.
     * False otherwise.
     */
    @Override
    public boolean supports( Class<?> clazz )
    {
        boolean result = ActionConfig.class.equals( clazz );

        LOG.debug( "Supports class: " + clazz + ", " + result );

        return result;
    }

    /**
     * Votes. Votes ACCESS_ABSTAIN if the object class is not supported. Votes
     * ACCESS_GRANTED if there is a granted authority which equals attribute
     * prefix + module name, or the module name is in the always accessible set.
     * Otherwise votes ACCESS_DENIED.
     */
    @Override
    public int vote( Authentication authentication, Object object, Collection<ConfigAttribute> attributes )
    {
        if ( !supports( object.getClass() ) )
        {
            LOG.debug( "ACCESS_ABSTAIN [" + object.toString() + "]: Class not supported." );

            return ACCESS_ABSTAIN;
        }

        ActionConfig target = (ActionConfig) object;

        if ( alwaysAccessible.contains( target.getPackageName() ) )
        {
            LOG.debug( "ACCESS_GRANTED [" + target.getPackageName() + "] by configuration." );

            return ACCESS_GRANTED;
        }

        String requiredAuthority = attributePrefix + target.getPackageName();

        for ( GrantedAuthority grantedAuthority : authentication.getAuthorities() )
        {
            if ( grantedAuthority.getAuthority().equals( requiredAuthority ) )
            {
                LOG.debug( "ACCESS_GRANTED [" + target.getPackageName() + "]" );

                return ACCESS_GRANTED;
            }
        }

        LOG.debug( "ACCESS_DENIED [" + target.getPackageName() + "]" );

        return ACCESS_DENIED;
    }
}
