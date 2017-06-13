package org.hisp.dhis.security.authority;

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

import com.opensymphony.xwork2.config.entities.ActionConfig;
import org.springframework.security.access.SecurityMetadataSource;

import java.util.Collection;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: RequiredAuthoritiesProvider.java 3160 2007-03-24 20:15:06Z torgeilo $
 */
public interface RequiredAuthoritiesProvider
{
    /**
     * Creates an SecurityMetadataSource based on the required authorities for
     * the action config. The specified action config is set as the secure
     * object. The SecurityMetadataSource may include additional attributes if
     * needed.
     *
     * @param actionConfig the secure actionConfig to get required authorities
     *                     from.
     */
    public SecurityMetadataSource createSecurityMetadataSource( ActionConfig actionConfig );

    /**
     * Creates an SecurityMetadataSource for a specified secure object based on
     * the required authorities for the action config. The
     * SecurityMetadataSource may include additional attributes if needed.
     *
     * @param actionConfig the actionConfig to get required authorities from.
     * @param object       the secure object.
     */
    public SecurityMetadataSource createSecurityMetadataSource( ActionConfig actionConfig, Object object );

    /**
     * Returns all authorities of an action configuration.
     */
    public Collection<String> getAllAuthorities( ActionConfig actionConfig );

    /**
     * Returns the required authorities of an action configuration.
     */
    public Collection<String> getRequiredAuthorities( ActionConfig actionConfig );
}
