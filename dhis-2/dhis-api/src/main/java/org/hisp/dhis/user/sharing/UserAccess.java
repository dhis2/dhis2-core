/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.user.sharing;

import lombok.NoArgsConstructor;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.sharing.AccessObject;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@NoArgsConstructor
@JacksonXmlRootElement( localName = "userAccess", namespace = DxfNamespaces.DXF_2_0 )
public class UserAccess
    extends AccessObject
{
    public UserAccess( String access, String id )
    {
        super( access, id );
    }

    public UserAccess( User user, String access )
    {
        super( access, user.getUid() );
    }

    /**
     * This is for backward compatibility with legacy
     * {@link org.hisp.dhis.user.UserAccess}
     */
    public UserAccess( org.hisp.dhis.user.UserAccess userAccess )
    {
        super( userAccess.getAccess(), userAccess.getUid() );
    }

    public void setUser( User user )
    {
        setId( user.getUid() );
    }

    public org.hisp.dhis.user.UserAccess toDtoObject()
    {
        org.hisp.dhis.user.UserAccess userAccess = new org.hisp.dhis.user.UserAccess();
        userAccess.setUid( getId() );
        userAccess.setAccess( getAccess() );
        User user = new User();
        user.setUid( getId() );
        userAccess.setUser( user );
        userAccess.setUid( getId() );
        userAccess.setDisplayName( getDisplayName() );

        return userAccess;
    }

    @Override
    public UserAccess copy()
    {
        return new UserAccess( this.access, this.id );
    }
}
