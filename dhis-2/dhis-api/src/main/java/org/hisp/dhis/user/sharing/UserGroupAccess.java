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
import org.hisp.dhis.user.UserGroup;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@NoArgsConstructor
@JacksonXmlRootElement( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
public class UserGroupAccess
    extends AccessObject
{
    public UserGroupAccess( UserGroup userGroup, String access )
    {
        super( access, userGroup.getUid() );
    }

    /**
     * This is for backward compatibility with legacy
     * {@link org.hisp.dhis.user.UserGroupAccess}
     */
    public UserGroupAccess( org.hisp.dhis.user.UserGroupAccess userGroupAccess )
    {
        super( userGroupAccess.getAccess(), userGroupAccess.getUid() );
    }

    public UserGroupAccess( String access, String id )
    {
        super( access, id );
    }

    public void setUserGroup( UserGroup userGroup )
    {
        setId( userGroup.getUid() );
    }

    public org.hisp.dhis.user.UserGroupAccess toDtoObject()
    {
        org.hisp.dhis.user.UserGroupAccess userGroupAccess = new org.hisp.dhis.user.UserGroupAccess();
        userGroupAccess.setUid( getId() );
        userGroupAccess.setAccess( getAccess() );
        UserGroup userGroup = new UserGroup();
        userGroup.setUid( getId() );
        userGroupAccess.setUserGroup( userGroup );
        userGroupAccess.setUid( getId() );
        userGroupAccess.setDisplayName( getDisplayName() );

        return userGroupAccess;
    }

    @Override
    public UserGroupAccess copy()
    {
        return new UserGroupAccess( this.access, this.id );
    }
}
