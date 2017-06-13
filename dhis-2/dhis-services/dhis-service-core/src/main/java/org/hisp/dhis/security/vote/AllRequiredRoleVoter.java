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

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * RoleVoter which requires all org.springframework.security.ConfigAttributes to
 * be granted authorities, given that the ConfigAttributes have the specified
 * prefix ("ROLE_" by default). If there are no supported ConfigAttributes it
 * abstains from voting.
 * 
 * @see org.springframework.security.access.vote.RoleVoter
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: AllRequiredRoleVoter.java 6070 2008-10-28 17:49:23Z larshelg $
 */
public class AllRequiredRoleVoter
    extends RoleVoter
{
    @Override
    public int vote( Authentication authentication, Object object, Collection<ConfigAttribute> attributes )
    {
        int supported = 0;

        for ( ConfigAttribute attribute : attributes )
        {
            if ( this.supports( attribute ) )
            {
                ++supported;
                boolean found = false;

                for ( GrantedAuthority authority : authentication.getAuthorities() )
                {
                    if ( attribute.getAttribute().equals( authority.getAuthority() ) )
                    {
                        found = true;
                        break;
                    }
                }

                if ( !found )
                {
                    return ACCESS_DENIED;
                }
            }
        }

        if ( supported > 0 )
        {
            return ACCESS_GRANTED;
        }

        return ACCESS_ABSTAIN;
    }
}
